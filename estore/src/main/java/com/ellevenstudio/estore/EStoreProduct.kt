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
    val currencyCode: String?,
    val subscriptionPeriod: String?,
    /** Trial period ISO 8601 (e.g., "P2W" for 2 weeks, "P3D" for 3 days). Null if no trial. */
    val trialPeriod: String?,
    /** Number of trial days. 0 if no trial. */
    val trialDays: Int,
    internal val productDetails: ProductDetails?,
    internal val config: EStoreProductConfig
) {
    companion object {
        fun fromSubscription(details: ProductDetails, config: EStoreProductConfig): EStoreProduct {
            val offer = details.subscriptionOfferDetails?.firstOrNull()
            val pricingPhases = offer?.pricingPhases?.pricingPhaseList ?: emptyList()

            // First phase with price > 0 is the main price; free phase is trial
            val trialPhase = pricingPhases.firstOrNull { it.priceAmountMicros == 0L }
            val paidPhase = pricingPhases.firstOrNull { it.priceAmountMicros > 0L }

            val trialPeriodStr = trialPhase?.billingPeriod
            val trialDays = parsePeriodToDays(trialPeriodStr)

            return EStoreProduct(
                id = details.productId,
                type = config.type,
                displayName = details.name,
                localizedTitle = config.title(),
                localizedDescription = config.description(),
                displayPrice = paidPhase?.formattedPrice ?: "",
                priceAmountMicros = paidPhase?.priceAmountMicros ?: 0,
                currencyCode = paidPhase?.priceCurrencyCode,
                subscriptionPeriod = paidPhase?.billingPeriod,
                trialPeriod = trialPeriodStr,
                trialDays = trialDays,
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
                currencyCode = offer?.priceCurrencyCode,
                subscriptionPeriod = null,
                trialPeriod = null,
                trialDays = 0,
                productDetails = details,
                config = config
            )
        }

        fun fromTestConfig(
            config: EStoreProductConfig,
            displayPrice: String,
            priceAmountMicros: Long,
            subscriptionPeriod: String? = null,
            trialPeriod: String? = null,
            trialDays: Int = 0
        ): EStoreProduct {
            return EStoreProduct(
                id = config.id,
                type = config.type,
                displayName = config.title(),
                localizedTitle = config.title(),
                localizedDescription = config.description(),
                displayPrice = displayPrice,
                priceAmountMicros = priceAmountMicros,
                currencyCode = "USD",
                subscriptionPeriod = subscriptionPeriod,
                trialPeriod = trialPeriod,
                trialDays = trialDays,
                productDetails = null,
                config = config
            )
        }

        /** Parse ISO 8601 period to approximate days. */
        private fun parsePeriodToDays(period: String?): Int {
            if (period == null) return 0
            return when {
                period.contains("D") -> period.replace("P", "").replace("D", "").toIntOrNull() ?: 0
                period.contains("W") -> (period.replace("P", "").replace("W", "").toIntOrNull() ?: 0) * 7
                period.contains("M") -> (period.replace("P", "").replace("M", "").toIntOrNull() ?: 0) * 30
                period.contains("Y") -> (period.replace("P", "").replace("Y", "").toIntOrNull() ?: 0) * 365
                else -> 0
            }
        }
    }
}
