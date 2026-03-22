package com.ellevenstudio.estore

import android.content.Context
import android.content.pm.ApplicationInfo
import org.json.JSONObject
import java.util.Calendar
import java.util.Date

internal object EStoreTestConfig {
    private const val CONFIG_FILE = "estore_test_products.json"

    fun isDebug(context: Context) = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    fun hasTestConfig(context: Context): Boolean {
        return try { context.assets.open(CONFIG_FILE).close(); true } catch (_: Exception) { false }
    }

    fun loadTestProducts(context: Context, storeConfig: EStoreConfig): List<EStoreProduct> {
        val products = mutableListOf<EStoreProduct>()
        try {
            val json = context.assets.open(CONFIG_FILE).bufferedReader().use { it.readText() }
            val root = JSONObject(json)

            val subs = root.optJSONArray("subscriptions")
            if (subs != null) {
                for (i in 0 until subs.length()) {
                    val item = subs.getJSONObject(i)
                    val id = item.getString("productId")
                    val config = storeConfig.products.firstOrNull { it.id == id } ?: continue
                    products.add(EStoreProduct.fromTestConfig(
                        config = config,
                        displayPrice = item.getString("displayPrice"),
                        priceAmountMicros = item.getLong("priceAmountMicros"),
                        subscriptionPeriod = item.optString("period", null)
                    ))
                }
            }

            val inApp = root.optJSONArray("products")
            if (inApp != null) {
                for (i in 0 until inApp.length()) {
                    val item = inApp.getJSONObject(i)
                    val id = item.getString("productId")
                    val config = storeConfig.products.firstOrNull { it.id == id } ?: continue
                    products.add(EStoreProduct.fromTestConfig(
                        config = config,
                        displayPrice = item.getString("displayPrice"),
                        priceAmountMicros = item.getLong("priceAmountMicros")
                    ))
                }
            }
        } catch (e: Exception) { android.util.Log.w("EStore", "Failed to load test products: ${e.message}") }
        return products.sortedBy { it.priceAmountMicros }
    }

    fun createTestPurchaseInfo(product: EStoreProduct): EStorePurchaseInfo {
        val now = Date()
        val exp = if (product.type is EStoreProductType.Subscription) {
            Calendar.getInstance().apply {
                time = now
                when (product.subscriptionPeriod) {
                    "P1W" -> add(Calendar.WEEK_OF_YEAR, 1)
                    "P1M" -> add(Calendar.MONTH, 1)
                    "P3M" -> add(Calendar.MONTH, 3)
                    "P6M" -> add(Calendar.MONTH, 6)
                    "P1Y" -> add(Calendar.YEAR, 1)
                    else -> add(Calendar.MONTH, 1)
                }
            }.time
        } else null
        return EStorePurchaseInfo(
            productIds = listOf(product.id), type = product.type,
            purchaseDate = now, purchaseToken = "test-${product.id}-${System.currentTimeMillis()}",
            orderId = "GPA.TEST-${product.id}", isAutoRenewing = product.type is EStoreProductType.Subscription,
            expirationDate = exp
        )
    }
}
