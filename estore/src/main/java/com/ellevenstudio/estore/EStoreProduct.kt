package com.ellevenstudio.estore

import com.android.billingclient.api.ProductDetails

/**
 * A product available for purchase, enriched with store data and config.
 */
data class EStoreProduct(
    val id: String,
    val type: EStoreProductType,
    val displayName: String,
    val localizedTitle: String,
    val localizedDescription: String,
    val displayPrice: String,
    val priceAmountMicros: Long,
    val subscriptionPeriod: String?,
    internal val productDetails: ProductDetails?,
    internal val config: EStoreProductConfig
) {
    companion object {
        fun fromSubscription(details: ProductDetails, config: EStoreProductConfig): EStoreProduct {
            val offer = details.subscriptionOfferDetails?.firstOrNull()
            val pricingPhase = offer?.pricingPhases?.pricingPhaseList?.firstOrNull()
            return EStoreProduct(
                id = details.productId,
                type = config.type,
                displayName = details.name,
                localizedTitle = config.title(),
                localizedDescription = config.description(),
                displayPrice = pricingPhase?.formattedPrice ?: "",
                priceAmountMicros = pricingPhase?.priceAmountMicros ?: 0,
                subscriptionPeriod = pricingPhase?.billingPeriod,
                productDetails = details,
                config = config
            )
        }

        fun fromInApp(details: ProductDetails, config: EStoreProductConfig): EStoreProduct {
            val offer = details.oneTimePurchaseOfferDetails
            return EStoreProduct(
                id = details.productId,
                type = config.type,
                displayName = details.name,
                localizedTitle = config.title(),
                localizedDescription = config.description(),
                displayPrice = offer?.formattedPrice ?: "",
                priceAmountMicros = offer?.priceAmountMicros ?: 0,
                subscriptionPeriod = null,
                productDetails = details,
                config = config
            )
        }

        /** Create a test product from config (for debug mode). */
        fun fromTestConfig(config: EStoreProductConfig, displayPrice: String, priceAmountMicros: Long, subscriptionPeriod: String? = null): EStoreProduct {
            return EStoreProduct(
                id = config.id,
                type = config.type,
                displayName = config.title(),
                localizedTitle = config.title(),
                localizedDescription = config.description(),
                displayPrice = displayPrice,
                priceAmountMicros = priceAmountMicros,
                subscriptionPeriod = subscriptionPeriod,
                productDetails = null,
                config = config
            )
        }
    }
}
