package com.ellevenstudio.eads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * Manages rewarded ads with automatic preloading.
 *
 * Usage:
 *     EAdsRewarded.show(activity) { reward ->
 *         println("Earned: ${reward.amount} ${reward.type}")
 *     }
 */
object EAdsRewarded {
    private const val TAG = "EAds"
    private var rewardedAd: RewardedAd? = null

    /**
     * Preloads a rewarded ad.
     */
    fun load(context: Context) {
        val adUnitId = EAds.rewardedAdUnitId ?: return
        RewardedAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Rewarded ad failed to load: ${error.message}")
                    rewardedAd = null
                }
            }
        )
    }

    /**
     * Shows the rewarded ad if one is loaded.
     * @param onReward Called when the user earns a reward.
     * @param onDismiss Called when the ad is dismissed.
     */
    fun show(
        activity: Activity,
        onReward: (EAdReward) -> Unit,
        onDismiss: (() -> Unit)? = null
    ) {
        val ad = rewardedAd
        if (ad == null) {
            Log.w(TAG, "Rewarded ad not ready")
            onDismiss?.invoke()
            load(activity)
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                onDismiss?.invoke()
                load(activity)
            }

            override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                Log.w(TAG, "Rewarded ad failed to show: ${error.message}")
                rewardedAd = null
                onDismiss?.invoke()
                load(activity)
            }
        }

        ad.show(activity) { rewardItem ->
            onReward(EAdReward(amount = rewardItem.amount, type = rewardItem.type))
        }
    }
}

/**
 * Reward data from a rewarded ad.
 */
data class EAdReward(
    val amount: Int,
    val type: String
)
