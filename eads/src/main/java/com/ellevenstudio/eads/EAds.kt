package com.ellevenstudio.eads

import android.content.Context
import android.content.pm.ApplicationInfo
import com.google.android.gms.ads.MobileAds

/**
 * Ad unit ID pair for debug and production environments.
 */
data class EAdUnitId(
    val debug: String,
    val production: String
)

/**
 * Central manager for Google AdMob integration.
 * You MUST provide your own ad unit IDs (both debug and production).
 * The library automatically selects the correct one based on build type.
 *
 * Usage:
 *     EAds.configure(
 *         context = this,
 *         banner = EAdUnitId(
 *             debug = "ca-app-pub-3940256099942544/6300978111",
 *             production = "ca-app-pub-YOUR_ID/YOUR_BANNER"
 *         ),
 *         interstitial = EAdUnitId(
 *             debug = "ca-app-pub-3940256099942544/1033173712",
 *             production = "ca-app-pub-YOUR_ID/YOUR_INTERSTITIAL"
 *         )
 *     )
 */
object EAds {
    var bannerAdUnitId: String? = null
        private set
    var interstitialAdUnitId: String? = null
        private set
    var rewardedAdUnitId: String? = null
        private set
    var openAppAdUnitId: String? = null
        private set
    var isInitialized: Boolean = false
        private set

    internal var appContext: Context? = null

    private fun isDebug(context: Context): Boolean {
        return (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    /**
     * Configure EAds with your ad unit IDs. At least one ad type must be provided.
     * Both debug and production IDs are required for each ad type you use.
     *
     * @param context Application context
     * @param banner Banner ad unit IDs (debug + production)
     * @param interstitial Interstitial ad unit IDs (debug + production)
     * @param rewarded Rewarded ad unit IDs (debug + production)
     * @param openApp Open app ad unit IDs (debug + production)
     */
    fun configure(
        context: Context,
        banner: EAdUnitId? = null,
        interstitial: EAdUnitId? = null,
        rewarded: EAdUnitId? = null,
        openApp: EAdUnitId? = null
    ) {
        require(banner != null || interstitial != null || rewarded != null || openApp != null) {
            "[EAds] ERROR: You must provide at least one ad unit ID. Pass EAdUnitId(debug, production) for each ad type you want to use."
        }

        appContext = context.applicationContext
        val debug = isDebug(context)

        bannerAdUnitId = banner?.let { if (debug) it.debug else it.production }
        interstitialAdUnitId = interstitial?.let { if (debug) it.debug else it.production }
        rewardedAdUnitId = rewarded?.let { if (debug) it.debug else it.production }
        openAppAdUnitId = openApp?.let { if (debug) it.debug else it.production }

        MobileAds.initialize(context) {
            isInitialized = true
            interstitialAdUnitId?.let { EAdsInterstitial.load(context) }
            rewardedAdUnitId?.let { EAdsRewarded.load(context) }
            openAppAdUnitId?.let { EAdsOpenApp.load(context) }
        }
    }
}
