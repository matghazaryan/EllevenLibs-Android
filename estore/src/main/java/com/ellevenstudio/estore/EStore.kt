package com.ellevenstudio.estore

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Central manager for Google Play Billing.
 * Handles subscriptions, one-time purchases, and consumables.
 *
 * Usage:
 *     EStore.configure(context, EStoreConfig(
 *         products = listOf(
 *             EStoreProductConfig(id = "monthly", type = EStoreProductType.Subscription,
 *                 localizedTitles = mapOf("en" to "Monthly"), localizedDescriptions = mapOf("en" to "Monthly access")),
 *         )
 *     ))
 *
 *     // Observe: val isPremium by EStore.isPremium.collectAsState()
 *     // Purchase: EStore.purchase(activity, "monthly")
 */
object EStore {
    private const val TAG = "EStore"
    private const val PREFS_NAME = "estore_prefs"
    private const val KEY_IS_PREMIUM = "is_premium"
    private const val KEY_TEST_PURCHASES = "test_purchases"

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _products = MutableStateFlow<List<EStoreProduct>>(emptyList())
    val products: StateFlow<List<EStoreProduct>> = _products.asStateFlow()

    private val _purchaseInfo = MutableStateFlow<EStorePurchaseInfo?>(null)
    val purchaseInfo: StateFlow<EStorePurchaseInfo?> = _purchaseInfo.asStateFlow()

    private val _allPurchaseInfos = MutableStateFlow<List<EStorePurchaseInfo>>(emptyList())
    val allPurchaseInfos: StateFlow<List<EStorePurchaseInfo>> = _allPurchaseInfos.asStateFlow()

    private val _loadingState = MutableStateFlow<EStoreLoadingState>(EStoreLoadingState.Idle)
    val loadingState: StateFlow<EStoreLoadingState> = _loadingState.asStateFlow()

    var config: EStoreConfig? = null
        private set
    val theme: EStoreTheme get() = config?.theme ?: EStoreTheme()

    private var billingClient: BillingClient? = null
    private var prefs: SharedPreferences? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isTestMode = false
    private val testPurchases = mutableListOf<EStorePurchaseInfo>()
    private var purchaseCallback: ((EStorePurchaseResult) -> Unit)? = null
    private var pendingPurchaseProduct: EStoreProduct? = null

    /**
     * Configure with product definitions. MUST be called before using EStore.
     */
    fun configure(context: Context, config: EStoreConfig) {
        this.config = config
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _isPremium.value = prefs?.getBoolean(KEY_IS_PREMIUM, false) ?: false
        EStoreConsumableManager.init(context)

        // Test mode in debug with test config
        if (isDebug(context) && EStoreTestConfig.hasTestConfig(context)) {
            isTestMode = true
            _loadingState.value = EStoreLoadingState.Loading
            Log.i(TAG, "Test mode enabled — using estore_test_products.json")
            _products.value = EStoreTestConfig.loadTestProducts(context, config)
            loadTestPurchasesFromPrefs()
            _loadingState.value = EStoreLoadingState.Loaded
            return
        }

        isTestMode = false
        billingClient = BillingClient.newBuilder(context)
            .setListener { billingResult, purchases ->
                val product = pendingPurchaseProduct
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    scope.launch {
                        for (purchase in purchases) {
                            handlePurchase(purchase)
                        }
                        // Fire callback with rich result
                        if (product != null && purchases.isNotEmpty()) {
                            val p = purchases.first()
                            purchaseCallback?.invoke(EStorePurchaseResult(
                                status = EStorePurchaseStatus.SUCCESS,
                                productId = product.id,
                                displayPrice = product.displayPrice,
                                priceAmountMicros = product.priceAmountMicros,
                                currencyCode = product.currencyCode,
                                type = product.type,
                                subscriptionPeriod = product.subscriptionPeriod,
                                trialPeriod = product.trialPeriod,
                                trialDays = product.trialDays,
                                purchaseDate = java.util.Date(p.purchaseTime),
                                orderId = p.orderId,
                                purchaseToken = p.purchaseToken
                            ))
                        }
                        purchaseCallback = null
                        pendingPurchaseProduct = null
                    }
                } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                    purchaseCallback?.invoke(EStorePurchaseResult(
                        status = EStorePurchaseStatus.CANCELLED,
                        productId = product?.id ?: ""
                    ))
                    purchaseCallback = null
                    pendingPurchaseProduct = null
                } else {
                    purchaseCallback?.invoke(EStorePurchaseResult(
                        status = EStorePurchaseStatus.FAILED,
                        productId = product?.id ?: ""
                    ))
                    purchaseCallback = null
                    pendingPurchaseProduct = null
                }
            }
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .enablePrepaidPlans()
                    .build()
            )
            .build()

        connectAndQuery()
    }

    private fun isDebug(context: Context): Boolean {
        return (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    private fun connectAndQuery() {
        _loadingState.value = EStoreLoadingState.Loading
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    scope.launch {
                        queryProducts()
                        refreshPurchases()
                    }
                } else {
                    val code = billingResult.responseCode
                    val msg = billingResult.debugMessage
                    Log.e(TAG, "Billing setup failed (code $code): $msg")
                    _loadingState.value = EStoreLoadingState.Failed(
                        "Billing setup failed (code $code): $msg", code
                    )
                }
            }
            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing disconnected, reconnecting...")
                connectAndQuery()
            }
        })
    }

    private suspend fun queryProducts() {
        val cfg = config ?: run {
            _loadingState.value = EStoreLoadingState.Failed("EStore not configured. Call configure() first.")
            return
        }
        val allProducts = mutableListOf<EStoreProduct>()

        if (cfg.subscriptionIds.isNotEmpty()) {
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(cfg.subscriptionIds.map {
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(it)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                }).build()
            val result = billingClient?.queryProductDetails(params)
            if (result != null && result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                val code = result.billingResult.responseCode
                val msg = result.billingResult.debugMessage
                Log.e(TAG, "Failed to query subscriptions (code $code): $msg")
            }
            result?.productDetailsList?.forEach { details ->
                val pc = cfg.products.first { it.id == details.productId }
                allProducts.add(EStoreProduct.fromSubscription(details, pc))
            }
        }

        val inAppIds = cfg.oneTimeIds + cfg.consumableIds
        if (inAppIds.isNotEmpty()) {
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(inAppIds.map {
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(it)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                }).build()
            val result = billingClient?.queryProductDetails(params)
            if (result != null && result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                val code = result.billingResult.responseCode
                val msg = result.billingResult.debugMessage
                Log.e(TAG, "Failed to query in-app products (code $code): $msg")
            }
            result?.productDetailsList?.forEach { details ->
                val pc = cfg.products.first { it.id == details.productId }
                allProducts.add(EStoreProduct.fromInApp(details, pc))
            }
        }

        _products.value = allProducts.sortedBy { it.priceAmountMicros }

        if (allProducts.isEmpty()) {
            val totalConfigured = cfg.subscriptionIds.size + inAppIds.size
            val msg = "No products loaded. $totalConfigured product(s) configured but none returned from Play Store. Check your product IDs in Google Play Console."
            Log.w(TAG, msg)
            _loadingState.value = EStoreLoadingState.Failed(msg)
        } else {
            Log.i(TAG, "Loaded ${allProducts.size} products")
            _loadingState.value = EStoreLoadingState.Loaded
        }
    }

    private suspend fun refreshPurchases() {
        val cfg = config ?: return
        val infos = mutableListOf<EStorePurchaseInfo>()

        billingClient?.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        )?.purchasesList?.forEach { purchase ->
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                acknowledgePurchaseIfNeeded(purchase)
                val pc = cfg.products.firstOrNull { it.id in purchase.products }
                infos.add(EStorePurchaseInfo.fromPurchase(purchase, pc?.type ?: EStoreProductType.Subscription))
            }
        }

        billingClient?.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        )?.purchasesList?.forEach { purchase ->
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                val pc = cfg.products.firstOrNull { it.id in purchase.products }
                val type = pc?.type ?: EStoreProductType.OneTime
                if (type is EStoreProductType.Consumable) {
                    // Consume and increment balance
                    val consumeParams = ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
                    billingClient?.consumePurchase(consumeParams)
                    EStoreConsumableManager.increment(purchase.products.first(), type.amount)
                } else {
                    acknowledgePurchaseIfNeeded(purchase)
                    infos.add(EStorePurchaseInfo.fromPurchase(purchase, type))
                }
            }
        }

        updatePurchaseState(infos)
    }

    private suspend fun acknowledgePurchaseIfNeeded(purchase: Purchase) {
        if (!purchase.isAcknowledged) {
            billingClient?.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
            )
        }
    }

    private suspend fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            val cfg = config ?: return
            val pc = cfg.products.firstOrNull { it.id in purchase.products }
            if (pc?.type is EStoreProductType.Consumable) {
                val consumeParams = ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
                billingClient?.consumePurchase(consumeParams)
                EStoreConsumableManager.increment(purchase.products.first(), (pc.type as EStoreProductType.Consumable).amount)
            } else {
                acknowledgePurchaseIfNeeded(purchase)
            }
            refreshPurchases()
        }
    }

    private fun updatePurchaseState(infos: List<EStorePurchaseInfo>) {
        _allPurchaseInfos.value = infos
        _purchaseInfo.value = infos.firstOrNull()
        // Premium = subscription or oneTime, NOT consumable
        val premium = infos.any { it.type !is EStoreProductType.Consumable }
        _isPremium.value = premium
        prefs?.edit()?.putBoolean(KEY_IS_PREMIUM, premium)?.apply()
    }

    /**
     * Launch the purchase flow.
     * @param onResult Optional callback with rich purchase result containing price, trial, dates, etc.
     */
    fun purchase(activity: Activity, productId: String, onResult: ((EStorePurchaseResult) -> Unit)? = null) {
        val product = _products.value.find { it.id == productId } ?: run {
            Log.w(TAG, "Product not found: $productId")
            onResult?.invoke(EStorePurchaseResult(status = EStorePurchaseStatus.FAILED, productId = productId))
            return
        }

        if (isTestMode) {
            Log.i(TAG, "[TEST] Simulating purchase: $productId")
            val info = EStoreTestConfig.createTestPurchaseInfo(product)
            if (product.type is EStoreProductType.Consumable) {
                EStoreConsumableManager.increment(productId, (product.type as EStoreProductType.Consumable).amount)
            } else {
                testPurchases.add(info)
                saveTestPurchasesToPrefs()
                updatePurchaseState(testPurchases.toList())
            }
            val result = EStorePurchaseResult(
                status = EStorePurchaseStatus.SUCCESS,
                productId = productId,
                displayPrice = product.displayPrice,
                priceAmountMicros = product.priceAmountMicros,
                currencyCode = product.currencyCode,
                type = product.type,
                subscriptionPeriod = product.subscriptionPeriod,
                trialPeriod = product.trialPeriod,
                trialDays = product.trialDays,
                purchaseDate = info.purchaseDate,
                expirationDate = info.expirationDate,
                orderId = info.orderId,
                purchaseToken = info.purchaseToken
            )
            onResult?.invoke(result)
            return
        }

        pendingPurchaseProduct = product
        purchaseCallback = onResult

        val productDetails = product.productDetails ?: return
        val paramsList = if (product.type is EStoreProductType.Subscription) {
            val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: return
            listOf(BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails).setOfferToken(offerToken).build())
        } else {
            listOf(BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails).build())
        }
        billingClient?.launchBillingFlow(activity, BillingFlowParams.newBuilder().setProductDetailsParamsList(paramsList).build())
    }

    fun restore() {
        if (isTestMode) { loadTestPurchasesFromPrefs(); return }
        scope.launch { refreshPurchases() }
    }

    /** Force verify premium status against Play Billing. */
    fun verifyPremiumStatus() { scope.launch { refreshPurchases() } }

    // Consumable balance
    fun consumableBalance(productId: String): Int = EStoreConsumableManager.balance(productId)
    fun deductConsumable(productId: String, amount: Int): Boolean = EStoreConsumableManager.deduct(productId, amount)
    fun addConsumable(productId: String, amount: Int) = EStoreConsumableManager.increment(productId, amount)

    fun clearTestPurchases() {
        if (!isTestMode) return
        testPurchases.clear()
        prefs?.edit()?.remove(KEY_TEST_PURCHASES)?.apply()
        updatePurchaseState(emptyList())
    }

    // Test purchase persistence
    private fun saveTestPurchasesToPrefs() {
        val json = org.json.JSONArray()
        testPurchases.forEach { info ->
            json.put(org.json.JSONObject().apply {
                put("productIds", org.json.JSONArray(info.productIds))
                put("type", when(info.type) { is EStoreProductType.Subscription -> "subscription"; is EStoreProductType.OneTime -> "oneTime"; is EStoreProductType.Consumable -> "consumable" })
                put("purchaseDate", info.purchaseDate.time)
                put("purchaseToken", info.purchaseToken)
                put("orderId", info.orderId)
                put("isAutoRenewing", info.isAutoRenewing)
                put("expirationDate", info.expirationDate?.time ?: -1)
            })
        }
        prefs?.edit()?.putString(KEY_TEST_PURCHASES, json.toString())?.apply()
    }

    private fun loadTestPurchasesFromPrefs() {
        testPurchases.clear()
        val raw = prefs?.getString(KEY_TEST_PURCHASES, null) ?: run { updatePurchaseState(emptyList()); return }
        try {
            val json = org.json.JSONArray(raw)
            for (i in 0 until json.length()) {
                val obj = json.getJSONObject(i)
                val pIds = mutableListOf<String>()
                val arr = obj.getJSONArray("productIds")
                for (j in 0 until arr.length()) pIds.add(arr.getString(j))
                val type = when(obj.getString("type")) {
                    "subscription" -> EStoreProductType.Subscription
                    "oneTime" -> EStoreProductType.OneTime
                    else -> EStoreProductType.OneTime
                }
                val expTime = obj.optLong("expirationDate", -1)
                testPurchases.add(EStorePurchaseInfo(
                    productIds = pIds, type = type,
                    purchaseDate = java.util.Date(obj.getLong("purchaseDate")),
                    purchaseToken = obj.getString("purchaseToken"),
                    orderId = obj.optString("orderId", null),
                    isAutoRenewing = obj.getBoolean("isAutoRenewing"),
                    expirationDate = if (expTime > 0) java.util.Date(expTime) else null
                ))
            }
        } catch (e: Exception) { Log.w(TAG, "Failed to load test purchases: ${e.message}") }
        updatePurchaseState(testPurchases.toList())
    }
}
