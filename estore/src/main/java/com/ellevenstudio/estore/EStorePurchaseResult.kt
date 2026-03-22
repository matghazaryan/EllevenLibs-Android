package com.ellevenstudio.estore

import java.util.Date

/**
 * Rich result returned after a purchase attempt. Contains all transaction data.
 *
 * Usage:
 *     val result = EStore.purchase(activity, "com.app.monthly")
 *     if (result.status == EStorePurchaseStatus.SUCCESS) {
 *         println("Purchased ${result.productId} for ${result.displayPrice}")
 *         println("Trial: ${result.trialPeriod}")
 *         println("Expires: ${result.expirationDate}")
 *     }
 */
data class EStorePurchaseResult(
    /** The outcome of the purchase. */
    val status: EStorePurchaseStatus,
    /** The product ID that was purchased. */
    val productId: String,
    /** Formatted price string (e.g., "$4.99"). */
    val displayPrice: String? = null,
    /** Raw price in micros (e.g., 4990000). */
    val priceAmountMicros: Long? = null,
    /** Currency code (e.g., "USD"). */
    val currencyCode: String? = null,
    /** Product type (Subscription, OneTime, Consumable). */
    val type: EStoreProductType? = null,
    /** Subscription period ISO 8601 (e.g., "P1M"). Null for non-subscriptions. */
    val subscriptionPeriod: String? = null,
    /** Trial period (e.g., "P2W" for 2 weeks). Null if no trial. */
    val trialPeriod: String? = null,
    /** Number of trial days. 0 if no trial. */
    val trialDays: Int = 0,
    /** When the purchase was made. */
    val purchaseDate: Date? = null,
    /** When the subscription expires. Null for lifetime/consumable. */
    val expirationDate: Date? = null,
    /** Order/transaction ID from the store. */
    val orderId: String? = null,
    /** Purchase token (Android-specific, useful for server verification). */
    val purchaseToken: String? = null
)

enum class EStorePurchaseStatus {
    SUCCESS, CANCELLED, PENDING, FAILED
}
