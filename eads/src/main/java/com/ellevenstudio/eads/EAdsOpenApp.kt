package com.ellevenstudio.eads

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd

/**
 * Manages open app ads that show when the app comes to the foreground.
 *
 * Usage:
 *     // Manual
 *     EAdsOpenApp.show(activity)
 *
 *     // Automatic: attach to app lifecycle in Application.onCreate()
 *     EAdsOpenApp.attachToAppLifecycle(activityProvider = { currentActivity })
 */
object EAdsOpenApp {
    private const val TAG = "EAds"
    private var appOpenAd: AppOpenAd? = null
    private var isShowingAd = false
    private var loadTime: Long = 0
    private var isAttached = false

    /**
     * Preloads an open app ad.
     */
    fun load(context: Context) {
        val adUnitId = EAds.openAppAdUnitId ?: return
        if (appOpenAd != null) return // Already loaded

        AppOpenAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    loadTime = System.currentTimeMillis()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Open app ad failed to load: ${error.message}")
                }
            }
        )
    }

    /**
     * Attaches to the app lifecycle to automatically show open app ads
     * when the app comes to the foreground.
     * @param activityProvider Returns the current foreground activity.
     */
    fun attachToAppLifecycle(activityProvider: () -> Activity?) {
        if (isAttached) return
        isAttached = true

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                activityProvider()?.let { show(it) }
            }
        })
    }

    /**
     * Shows the open app ad if one is loaded and not expired (4 hours).
     */
    fun show(activity: Activity) {
        if (isShowingAd) return
        val ad = appOpenAd
        if (ad == null || isAdExpired()) {
            appOpenAd = null
            load(activity)
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                isShowingAd = false
                appOpenAd = null
                load(activity)
            }

            override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                Log.w(TAG, "Open app ad failed to show: ${error.message}")
                isShowingAd = false
                appOpenAd = null
                load(activity)
            }
        }

        isShowingAd = true
        ad.show(activity)
    }

    private fun isAdExpired(): Boolean {
        return System.currentTimeMillis() - loadTime > 4 * 3600 * 1000
    }
}
