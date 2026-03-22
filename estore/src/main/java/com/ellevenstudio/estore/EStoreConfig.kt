package com.ellevenstudio.estore

/**
 * Configuration for EStore. Products are REQUIRED.
 *
 * Usage:
 *     EStore.configure(
 *         context = this,
 *         config = EStoreConfig(
 *             products = listOf(
 *                 EStoreProductConfig(
 *                     id = "com.app.monthly",
 *                     type = EStoreProductType.Subscription,
 *                     localizedTitles = mapOf("en" to "Monthly Pro", "es" to "Pro Mensual"),
 *                     localizedDescriptions = mapOf("en" to "Full access", "es" to "Acceso completo")
 *                 ),
 *                 EStoreProductConfig(
 *                     id = "com.app.coins100",
 *                     type = EStoreProductType.Consumable(amount = 100),
 *                     localizedTitles = mapOf("en" to "100 Coins"),
 *                     localizedDescriptions = mapOf("en" to "Buy 100 coins")
 *                 )
 *             ),
 *             theme = EStoreTheme(primaryColor = Color.Blue)
 *         )
 *     )
 */
/**
 * A premium feature displayed in paywalls.
 *
 * @param icon Emoji or icon text (e.g., "✓", "⚡", "🔒")
 * @param title Feature title (e.g., "Ad Free")
 * @param subtitle Optional description
 */
data class EStoreFeature(
    val icon: String = "✓",
    val title: String,
    val subtitle: String? = null
)

data class EStoreConfig(
    val products: List<EStoreProductConfig>,
    val features: List<EStoreFeature> = emptyList(),
    val theme: EStoreTheme = EStoreTheme()
) {
    init {
        require(products.isNotEmpty()) { "[EStore] ERROR: You must provide at least one product configuration." }
    }

    internal val subscriptionIds get() = products.filter { it.type is EStoreProductType.Subscription }.map { it.id }
    internal val oneTimeIds get() = products.filter { it.type is EStoreProductType.OneTime }.map { it.id }
    internal val consumableIds get() = products.filter { it.type is EStoreProductType.Consumable }.map { it.id }
}

/**
 * Configuration for a single product.
 */
data class EStoreProductConfig(
    val id: String,
    val type: EStoreProductType,
    val localizedTitles: Map<String, String>,
    val localizedDescriptions: Map<String, String>,
    val iconName: String? = null
) {
    init {
        require(localizedTitles.isNotEmpty()) { "[EStore] ERROR: Product '$id' must have at least one localized title." }
        require(localizedDescriptions.isNotEmpty()) { "[EStore] ERROR: Product '$id' must have at least one localized description." }
    }

    /** Returns the title for the current device locale, falling back to "en", then first available. */
    fun title(locale: String? = null): String {
        val lang = locale ?: java.util.Locale.getDefault().language
        return localizedTitles[lang] ?: localizedTitles["en"] ?: localizedTitles.values.firstOrNull() ?: id
    }

    /** Returns the description for the current device locale. */
    fun description(locale: String? = null): String {
        val lang = locale ?: java.util.Locale.getDefault().language
        return localizedDescriptions[lang] ?: localizedDescriptions["en"] ?: localizedDescriptions.values.firstOrNull() ?: ""
    }
}

/** The type of a store product. */
sealed class EStoreProductType {
    data object Subscription : EStoreProductType()
    data object OneTime : EStoreProductType()
    data class Consumable(val amount: Int) : EStoreProductType()
}
