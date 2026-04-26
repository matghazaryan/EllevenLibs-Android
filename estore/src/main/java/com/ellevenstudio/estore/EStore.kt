package com.ellevenstudio.estore

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest

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
     * Diagnostic snapshot captured at configure() time. Useful when prices don't
     * load and you need to tell sideload/signature/package issues apart.
     */
    private var diagnostics: String = ""

    /**
     * Configure with product definitions. MUST be called before using EStore.
     */
    fun configure(context: Context, config: EStoreConfig) {
        this.config = config
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _isPremium.value = prefs?.getBoolean(KEY_IS_PREMIUM, false) ?: false
        EStoreConsumableManager.init(context)

        diagnostics = buildDiagnostics(context)
        Log.i(TAG, "configure() called. $diagnostics")
        Log.i(TAG, "Configured ${config.products.size} product(s): " +
            config.products.joinToString { "${it.id} (${productTypeLabel(it.type)})" })

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

    private fun productTypeLabel(type: EStoreProductType): String = when (type) {
        is EStoreProductType.Subscription -> "subs"
        is EStoreProductType.OneTime -> "inapp"
        is EStoreProductType.Consumable -> "inapp/consumable"
    }

    private fun responseCodeName(code: Int): String = when (code) {
        BillingClient.BillingResponseCode.OK -> "OK"
        BillingClient.BillingResponseCode.USER_CANCELED -> "USER_CANCELED"
        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> "SERVICE_UNAVAILABLE"
        BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> "BILLING_UNAVAILABLE"
        BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> "ITEM_UNAVAILABLE"
        BillingClient.BillingResponseCode.DEVELOPER_ERROR -> "DEVELOPER_ERROR"
        BillingClient.BillingResponseCode.ERROR -> "ERROR"
        BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> "ITEM_ALREADY_OWNED"
        BillingClient.BillingResponseCode.ITEM_NOT_OWNED -> "ITEM_NOT_OWNED"
        BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED -> "FEATURE_NOT_SUPPORTED"
        BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> "SERVICE_DISCONNECTED"
        BillingClient.BillingResponseCode.NETWORK_ERROR -> "NETWORK_ERROR"
        else -> "UNKNOWN"
    }

    private fun buildDiagnostics(context: Context): String {
        val pkg = context.packageName
        val debuggable = isDebug(context)
        val installer = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.packageManager.getInstallSourceInfo(pkg).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getInstallerPackageName(pkg)
            }
        } catch (e: Exception) { null }
        val installerLabel = when (installer) {
            null -> "sideload (null)"
            "com.android.vending" -> "Play Store"
            "com.google.android.packageinstaller",
            "com.android.packageinstaller" -> "sideload (pm)"
            else -> installer
        }
        val sigShort = try {
            val pm = context.packageManager
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val info = pm.getPackageInfo(pkg, PackageManager.GET_SIGNING_CERTIFICATES)
                info.signingInfo?.apkContentsSigners ?: emptyArray()
            } else {
                @Suppress("DEPRECATION")
                val info = pm.getPackageInfo(pkg, PackageManager.GET_SIGNATURES)
                @Suppress("DEPRECATION")
                info.signatures ?: emptyArray()
            }
            val first = signatures.firstOrNull()
            if (first == null) {
                "none"
            } else {
                val digest = MessageDigest.getInstance("SHA-256").digest(first.toByteArray())
                // Short 8-byte prefix is enough to identify which key was used
                digest.take(8).joinToString(":") { "%02X".format(it) }
            }
        } catch (e: Exception) { "unknown(${e.javaClass.simpleName})" }
        return "package=$pkg, installer=$installerLabel, debuggable=$debuggable, signatureSha256Prefix=$sigShort"
    }

    private fun connectAndQuery() {
        _loadingState.value = EStoreLoadingState.Loading
        Log.i(TAG, "Starting BillingClient connection...")
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                val code = billingResult.responseCode
                val msg = billingResult.debugMessage.ifEmpty { "(no debug message)" }
                Log.i(TAG, "onBillingSetupFinished: code=$code ${responseCodeName(code)}, debugMessage=$msg")
                if (code == BillingClient.BillingResponseCode.OK) {
                    scope.launch {
                        queryProducts()
                        refreshPurchases()
                    }
                } else {
                    Log.e(TAG, "Billing setup failed. $diagnostics")
                    _loadingState.value = EStoreLoadingState.Failed(
                        "Billing setup failed (code $code ${responseCodeName(code)}): $msg",
                        code
                    )
                }
            }
            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing disconnected, reconnecting...")
                connectAndQuery()
            }
        })
    }

    /**
     * Wrap queryProductDetailsAsync in a coroutine without going through
     * billing-ktx's suspend extension. Real-world reports (and Crashlytics
     * traces) showed the suspend bridge silently dropping the response under
     * R8/proguard-android-optimize.txt. This direct callback path is the
     * minimum surface area: BillingClient -> SAM listener -> resume.
     */
    private suspend fun queryProductDetailsDirect(
        params: QueryProductDetailsParams,
        timeoutMs: Long = 15_000
    ): Pair<BillingResult?, List<ProductDetails>?> {
        val client = billingClient ?: return null to null
        val result = kotlinx.coroutines.withTimeoutOrNull(timeoutMs) {
            kotlin.coroutines.suspendCoroutine<Pair<BillingResult, List<ProductDetails>>> { cont ->
                client.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
                    cont.resumeWith(Result.success(billingResult to productDetailsList))
                }
            }
        }
        return result?.first to result?.second
    }

    private suspend fun queryProducts() {
        val cfg = config ?: run {
            _loadingState.value = EStoreLoadingState.Failed("EStore not configured. Call configure() first.")
            return
        }
        val allProducts = mutableListOf<EStoreProduct>()
        var lastErrorCode: Int? = null
        var lastErrorMessage: String? = null
        val missingIds = mutableListOf<String>()

        if (cfg.subscriptionIds.isNotEmpty()) {
            Log.i(TAG, "Querying SUBS: ${cfg.subscriptionIds}")
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(cfg.subscriptionIds.map {
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(it)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                }).build()
            val (billingResult, productDetailsList) = queryProductDetailsDirect(params)
            if (billingResult == null) {
                Log.e(TAG, "SUBS query timed out or BillingClient not ready (15s). $diagnostics")
                lastErrorCode = BillingClient.BillingResponseCode.SERVICE_TIMEOUT
                lastErrorMessage = "Subscription query timed out after 15s. Play Services may be stalled — try Play Store cache clear or reboot."
                missingIds.addAll(cfg.subscriptionIds)
            } else {
                val code = billingResult.responseCode
                val dbg = billingResult.debugMessage.ifEmpty { "(no debug message)" }
                val returned = productDetailsList?.map { it.productId } ?: emptyList()
                Log.i(TAG, "SUBS query response: code=$code ${responseCodeName(code)}, debugMessage=$dbg, returnedIds=$returned")
                if (code != BillingClient.BillingResponseCode.OK) {
                    lastErrorCode = code
                    lastErrorMessage = dbg
                }
                productDetailsList?.forEach { details ->
                    val pc = cfg.products.firstOrNull { it.id == details.productId }
                    if (pc == null) {
                        Log.w(TAG, "Play returned product '${details.productId}' that is not in EStoreConfig — ignoring")
                    } else {
                        allProducts.add(EStoreProduct.fromSubscription(details, pc))
                    }
                }
                cfg.subscriptionIds.filter { it !in returned }.forEach { missingIds.add(it) }
            }
        }

        val inAppIds = cfg.oneTimeIds + cfg.consumableIds
        if (inAppIds.isNotEmpty()) {
            Log.i(TAG, "Querying INAPP: $inAppIds")
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(inAppIds.map {
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(it)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                }).build()
            val (billingResult, productDetailsList) = queryProductDetailsDirect(params)
            if (billingResult == null) {
                Log.e(TAG, "INAPP query timed out or BillingClient not ready (15s). $diagnostics")
                lastErrorCode = BillingClient.BillingResponseCode.SERVICE_TIMEOUT
                lastErrorMessage = "In-app product query timed out after 15s."
                missingIds.addAll(inAppIds)
            } else {
                val code = billingResult.responseCode
                val dbg = billingResult.debugMessage.ifEmpty { "(no debug message)" }
                val returned = productDetailsList?.map { it.productId } ?: emptyList()
                Log.i(TAG, "INAPP query response: code=$code ${responseCodeName(code)}, debugMessage=$dbg, returnedIds=$returned")
                if (code != BillingClient.BillingResponseCode.OK) {
                    lastErrorCode = code
                    lastErrorMessage = dbg
                }
                productDetailsList?.forEach { details ->
                    val pc = cfg.products.firstOrNull { it.id == details.productId }
                    if (pc == null) {
                        Log.w(TAG, "Play returned product '${details.productId}' that is not in EStoreConfig — ignoring")
                    } else {
                        allProducts.add(EStoreProduct.fromInApp(details, pc))
                    }
                }
                inAppIds.filter { it !in returned }.forEach { missingIds.add(it) }
            }
        }

        _products.value = allProducts.sortedBy { it.priceAmountMicros }

        if (allProducts.isEmpty()) {
            val totalConfigured = cfg.subscriptionIds.size + inAppIds.size
            val reason = when {
                lastErrorCode != null ->
                    "Play Billing returned ${responseCodeName(lastErrorCode!!)} (code $lastErrorCode): $lastErrorMessage"
                else ->
                    "Play Billing returned OK but no ProductDetails for the configured IDs."
            }
            val hint = buildHint()
            val msg = "No products loaded. $totalConfigured configured, 0 returned.\n" +
                "Missing IDs: ${missingIds.joinToString()}\n" +
                "Reason: $reason\n" +
                "Hint: $hint\n" +
                "Device: $diagnostics"
            Log.e(TAG, msg)
            _loadingState.value = EStoreLoadingState.Failed(msg, lastErrorCode)
        } else {
            if (missingIds.isNotEmpty()) {
                Log.w(TAG, "Partial product load. Loaded ${allProducts.size}, missing: $missingIds. " +
                    "Reason: ${lastErrorMessage ?: "not returned by Play"}. Device: $diagnostics")
            } else {
                Log.i(TAG, "Loaded ${allProducts.size} products: ${allProducts.map { it.id }}")
            }
            _loadingState.value = EStoreLoadingState.Loaded
        }
    }

    private fun buildHint(): String {
        val d = diagnostics
        val sideload = !d.contains("installer=Play Store")
        val debug = d.contains("debuggable=true")
        return when {
            sideload -> "APK not installed from Play Store. Play Billing returns empty lists for sideloaded APKs. " +
                "Install the app from the Play Console testing track link on a tester device."
            debug -> "Debuggable build detected. If the signing certificate doesn't match what Play has " +
                "for this package, queryProductDetails returns empty. Build a release-signed APK or install from Play."
            else -> "Verify: (1) product IDs match Google Play Console exactly, (2) the subscription/base plan is Active, " +
                "(3) the app has an active release on a testing track, (4) the signed-in Google account is on the tester list."
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
