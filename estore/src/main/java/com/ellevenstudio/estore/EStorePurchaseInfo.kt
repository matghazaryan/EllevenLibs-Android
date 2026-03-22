package com.ellevenstudio.estore

import com.android.billingclient.api.Purchase
import java.util.Date

/**
 * Information about an active purchase.
 */
data class EStorePurchaseInfo(
    val productIds: List<String>,
    val type: EStoreProductType,
    val purchaseDate: Date,
    val purchaseToken: String,
    val orderId: String?,
    val isAutoRenewing: Boolean,
    val expirationDate: Date? = null
) {
    companion object {
        fun fromPurchase(purchase: Purchase, type: EStoreProductType): EStorePurchaseInfo {
            return EStorePurchaseInfo(
                productIds = purchase.products,
                type = type,
                purchaseDate = Date(purchase.purchaseTime),
                purchaseToken = purchase.purchaseToken,
                orderId = purchase.orderId,
                isAutoRenewing = purchase.isAutoRenewing
            )
        }
    }
}
