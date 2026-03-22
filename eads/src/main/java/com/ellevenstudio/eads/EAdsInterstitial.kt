package com.ellevenstudio.eads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * Manages interstitial ads with automatic preloading.
 *
 * Usage:
 *     EAdsInterstitial.show(activity)
 *     EAdsInterstitial.show(activity) { println("Ad dismissed") }
 */
object EAdsInterstitial {
    private const val TAG = "EAds"
    private var interstitialAd: InterstitialAd? = null

    /**
     * Preloads an interstitial ad.
     */
    fun load(context: Context) {
        val adUnitId = EAds.interstitialAdUnitId ?: return
        InterstitialAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Interstitial failed to load: ${error.message}")
                    interstitialAd = null
                }
            }
        )
    }

    /**
     * Shows the interstitial ad if one is loaded.
     * A new ad is preloaded automatically after dismissal.
     */
    fun show(activity: Activity, onDismiss: (() -> Unit)? = null) {
        val ad = interstitialAd
        if (ad == null) {
            Log.w(TAG, "Interstitial not ready")
            onDismiss?.invoke()
            load(activity)
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                onDismiss?.invoke()
                load(activity)
            }

            override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                Log.w(TAG, "Interstitial failed to show: ${error.message}")
                interstitialAd = null
                onDismiss?.invoke()
                load(activity)
            }
        }
        ad.show(activity)
    }
}
