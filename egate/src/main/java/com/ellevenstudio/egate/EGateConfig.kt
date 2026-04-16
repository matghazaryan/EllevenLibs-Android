package com.ellevenstudio.egate

/**
 * Configuration for EGate play-limit system.
 *
 * Usage:
 *     EGate.configure(context, EGateConfig(
 *         maxPlays = 5,
 *         storageKey = "my_game_gate",
 *         localizedTitles = mapOf("en" to "Play Limit Reached", "hy" to "..."),
 *         localizedMessages = mapOf("en" to "Upgrade to premium or watch an ad to continue playing."),
 *         localizedPremiumButtonTexts = mapOf("en" to "Go Premium"),
 *         localizedAdButtonTexts = mapOf("en" to "Watch Ad to Continue"),
 *         localizedDismissButtonTexts = mapOf("en" to "Later")
 *     ))
 *
 * @param maxPlays Number of plays before gate is shown. Default is 5.
 * @param storageKey Unique key for persisting play count. Default is "egate_play_count".
 * @param localizedTitles Title text by language code.
 * @param localizedMessages Message text by language code.
 * @param localizedPremiumButtonTexts Premium button text by language code.
 * @param localizedAdButtonTexts Ad button text by language code.
 * @param localizedDismissButtonTexts Dismiss button text by language code.
 * @param theme Visual theme for the gate UI.
 * @param showAdButton Whether to show the "Watch Ad" button. Default true.
 * @param showPremiumButton Whether to show the "Go Premium" button. Default true.
 * @param showDismissButton Whether to show the "Later" dismiss button. Default true.
 * @param resetAfterAd Whether to reset play count after watching an ad. Default true.
 */
data class EGateConfig(
    val maxPlays: Int = 5,
    val storageKey: String = "egate_play_count",
    val localizedTitles: Map<String, String> = mapOf("en" to "Play Limit Reached"),
    val localizedMessages: Map<String, String> = mapOf("en" to "Upgrade to premium or watch an ad to continue playing."),
    val localizedPremiumButtonTexts: Map<String, String> = mapOf("en" to "Go Premium"),
    val localizedAdButtonTexts: Map<String, String> = mapOf("en" to "Watch Ad to Continue"),
    val localizedDismissButtonTexts: Map<String, String> = mapOf("en" to "Later"),
    val theme: EGateTheme = EGateTheme(),
    val showAdButton: Boolean = true,
    val showPremiumButton: Boolean = true,
    val showDismissButton: Boolean = true,
    val resetAfterAd: Boolean = true
) {
    private val currentLanguage: String
        get() = java.util.Locale.getDefault().language

    val title: String
        get() = localizedTitles[currentLanguage] ?: localizedTitles["en"] ?: localizedTitles.values.firstOrNull() ?: "Play Limit Reached"

    val message: String
        get() = localizedMessages[currentLanguage] ?: localizedMessages["en"] ?: localizedMessages.values.firstOrNull() ?: ""

    val premiumButtonText: String
        get() = localizedPremiumButtonTexts[currentLanguage] ?: localizedPremiumButtonTexts["en"] ?: localizedPremiumButtonTexts.values.firstOrNull() ?: "Go Premium"

    val adButtonText: String
        get() = localizedAdButtonTexts[currentLanguage] ?: localizedAdButtonTexts["en"] ?: localizedAdButtonTexts.values.firstOrNull() ?: "Watch Ad to Continue"

    val dismissButtonText: String
        get() = localizedDismissButtonTexts[currentLanguage] ?: localizedDismissButtonTexts["en"] ?: localizedDismissButtonTexts.values.firstOrNull() ?: "Later"
}
