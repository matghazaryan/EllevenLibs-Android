package com.ellevenstudio.estore

import com.android.billingclient.api.ProductDetails

/** How an introductory offer charges the subscriber. */
enum class EStoreIntroPaymentMode {
    /** User gets the intro period free (e.g. "7 days free, then $9.99/month"). */
    FREE_TRIAL,
    /** User pays one discounted lump sum for the entire intro period, then
     *  auto-renews at the standard price (e.g. "$39.99 for year 1, then $59.99/year"). */
    PAY_UP_FRONT,
    /** User pays a reduced per-period price for N cycles, then switches to
     *  the standard price (e.g. "$4.99/mo for 3 months, then $9.99/mo"). */
    PAY_AS_YOU_GO,
}

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
    /** True iff this product currently has an active introductory offer (any payment mode). */
    val hasIntroductoryOffer: Boolean = false,
    /** Localized intro price, e.g. "$39.99" or "Free". Null if no intro offer. */
    val introductoryDisplayPrice: String? = null,
    /** Raw intro price in micros (e.g. 39_990_000). Null if no intro offer. */
    val introductoryPriceAmountMicros: Long? = null,
    /** Whether the intro is free, pay-up-front, or pay-as-you-go. Null if no intro offer. */
    val introductoryPaymentMode: EStoreIntroPaymentMode? = null,
    /** ISO 8601 intro period (e.g. "P1Y", "P3D"). Null if no intro offer. */
    val introductoryPeriod: String? = null,
    /** Approximate intro period in days. 0 if no intro offer. */
    val introductoryPeriodDays: Int = 0,
    internal val productDetails: ProductDetails? = null,
    internal val config: EStoreProductConfig,
) {
    companion object {
        // BillingClient recurrence modes — kept as constants so we don't have
        // to import BillingClient.ProductDetails.RecurrenceMode at every site.
        private const val INFINITE_RECURRING = 1
        private const val FINITE_RECURRING = 2
        private const val NON_RECURRING = 3

        fun fromSubscription(details: ProductDetails, config: EStoreProductConfig): EStoreProduct {
            val offer = details.subscriptionOfferDetails?.firstOrNull()
            val pricingPhases = offer?.pricingPhases?.pricingPhaseList ?: emptyList()

            // The recurring/standard phase is the one that repeats forever.
            // Any non-INFINITE phase that comes before it is an intro phase.
            val recurringPhase = pricingPhases.firstOrNull { it.recurrenceMode == INFINITE_RECURRING }
                ?: pricingPhases.lastOrNull { it.priceAmountMicros > 0L }
            val introPhase = pricingPhases.firstOrNull {
                it.recurrenceMode == FINITE_RECURRING || it.recurrenceMode == NON_RECURRING
            }

            val hasIntro = introPhase != null
            val introMode: EStoreIntroPaymentMode? = when {
                introPhase == null -> null
                introPhase.priceAmountMicros == 0L -> EStoreIntroPaymentMode.FREE_TRIAL
                // FINITE with billingCycleCount > 1 means N recurring discounted cycles.
                introPhase.recurrenceMode == FINITE_RECURRING && introPhase.billingCycleCount > 1 ->
                    EStoreIntroPaymentMode.PAY_AS_YOU_GO
                // Otherwise a single discounted cycle covering the whole intro period.
                else -> EStoreIntroPaymentMode.PAY_UP_FRONT
            }

            // Legacy trial fields stay populated for free-trial intros so the
            // existing paywall trial-badge code keeps working.
            val isFreeTrial = introMode == EStoreIntroPaymentMode.FREE_TRIAL
            val trialPeriodStr = if (isFreeTrial) introPhase?.billingPeriod else null
            val trialDays = if (isFreeTrial) parsePeriodToDays(trialPeriodStr) else 0

            return EStoreProduct(
                id = details.productId,
                type = config.type,
                displayName = details.name,
                localizedTitle = config.title(),
                localizedDescription = config.description(),
                displayPrice = recurringPhase?.formattedPrice ?: "",
                priceAmountMicros = recurringPhase?.priceAmountMicros ?: 0,
                currencyCode = recurringPhase?.priceCurrencyCode,
                subscriptionPeriod = recurringPhase?.billingPeriod,
                trialPeriod = trialPeriodStr,
                trialDays = trialDays,
                hasIntroductoryOffer = hasIntro,
                introductoryDisplayPrice = introPhase?.formattedPrice,
                introductoryPriceAmountMicros = introPhase?.priceAmountMicros,
                introductoryPaymentMode = introMode,
                introductoryPeriod = introPhase?.billingPeriod,
                introductoryPeriodDays = parsePeriodToDays(introPhase?.billingPeriod),
                productDetails = details,
                config = config,
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
