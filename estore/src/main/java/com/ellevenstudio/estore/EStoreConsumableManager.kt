package com.ellevenstudio.estore

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages local consumable balances (coins, credits, etc.).
 */
internal object EStoreConsumableManager {
    private const val PREFS_NAME = "estore_consumables"
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun balance(productId: String): Int {
        return prefs?.getInt(productId, 0) ?: 0
    }

    fun increment(productId: String, amount: Int) {
        val current = balance(productId)
        prefs?.edit()?.putInt(productId, current + amount)?.apply()
    }

    fun deduct(productId: String, amount: Int): Boolean {
        val current = balance(productId)
        if (current < amount) return false
        prefs?.edit()?.putInt(productId, current - amount)?.apply()
        return true
    }

    fun reset(productId: String) {
        prefs?.edit()?.putInt(productId, 0)?.apply()
    }
}
