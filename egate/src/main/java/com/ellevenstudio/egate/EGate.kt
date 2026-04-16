package com.ellevenstudio.egate

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import com.ellevenstudio.eads.EAdsRewarded
import com.ellevenstudio.estore.EStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Central manager for the play-limit gate system.
 * Tracks how many games a user has played and triggers a gate UI when the limit is reached.
 *
 * Usage:
 *     // Configure (call once at app launch, after EStore and EAds)
 *     EGate.configure(context, EGateConfig(maxPlays = 5))
 *
 *     // After each game
 *     EGate.recordPlay()
 *     if (EGate.shouldShowGate.value) { ... }
 *
 *     // Or use the Composable
 *     EGateOverlay(activity = activity)
 */
object EGate {
    private const val PREFS_NAME = "egate_prefs"

    private val _currentCount = MutableStateFlow(0)
    val currentCount: StateFlow<Int> = _currentCount.asStateFlow()

    private val _shouldShowGate = MutableStateFlow(false)
    val shouldShowGate: StateFlow<Boolean> = _shouldShowGate.asStateFlow()

    var config: EGateConfig = EGateConfig()
        private set

    private var prefs: SharedPreferences? = null

    /**
     * Configure EGate. Call once at app launch, after EStore and EAds are configured.
     */
    fun configure(context: Context, config: EGateConfig = EGateConfig()) {
        this.config = config
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _currentCount.value = prefs?.getInt(config.storageKey, 0) ?: 0
        updateGateState()
    }

    /**
     * Record a play. Call this after each game round ends.
     * Returns true if the gate should now be shown.
     */
    fun recordPlay(): Boolean {
        _currentCount.value += 1
        prefs?.edit()?.putInt(config.storageKey, _currentCount.value)?.apply()
        updateGateState()
        return _shouldShowGate.value
    }

    /**
     * Reset the play counter to zero.
     */
    fun reset() {
        _currentCount.value = 0
        prefs?.edit()?.putInt(config.storageKey, 0)?.apply()
        updateGateState()
    }

    /**
     * Called when user watches an ad. Resets count if configured.
     */
    fun onAdWatched() {
        if (config.resetAfterAd) {
            reset()
        }
    }

    /**
     * Called when user becomes premium. Gate will no longer show.
     */
    fun onPremiumPurchased() {
        _shouldShowGate.value = false
    }

    /**
     * Show a rewarded ad using EAds.
     */
    fun showRewardedAd(activity: Activity) {
        EAdsRewarded.show(
            activity = activity,
            onReward = { onAdWatched() },
            onDismiss = null
        )
    }

    /**
     * Dismiss the gate without taking action.
     */
    fun dismiss() {
        _shouldShowGate.value = false
    }

    private fun updateGateState() {
        if (EStore.isPremium.value) {
            _shouldShowGate.value = false
            return
        }
        _shouldShowGate.value = _currentCount.value >= config.maxPlays
    }
}
