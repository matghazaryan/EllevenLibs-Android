package com.ellevenstudio.estore.paywalls

import com.ellevenstudio.estore.*

internal class EPaywallData(
    customTheme: EStoreTheme? = null
) {
    val theme: EStoreTheme = customTheme ?: EStore.theme
    val products: List<EStoreProduct> get() = EStore.products.value
    val isPremium: Boolean get() = EStore.isPremium.value

    val subscriptions get() = products.filter { it.type is EStoreProductType.Subscription }
    val oneTimeProducts get() = products.filter { it.type is EStoreProductType.OneTime }
    val consumables get() = products.filter { it.type is EStoreProductType.Consumable }
    val premiumProducts get() = subscriptions + oneTimeProducts

    val features: List<EStoreFeature> get() {
        val configured = EStore.config?.features ?: emptyList()
        return configured.ifEmpty {
            listOf(
                EStoreFeature(icon = "✓", title = "Unlimited Access"),
                EStoreFeature(icon = "✕", title = "No Ads"),
                EStoreFeature(icon = "⚡", title = "Premium Features"),
            )
        }
    }
}
