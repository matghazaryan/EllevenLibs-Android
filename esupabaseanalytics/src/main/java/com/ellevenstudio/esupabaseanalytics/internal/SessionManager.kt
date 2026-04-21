package com.ellevenstudio.esupabaseanalytics.internal

import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

/**
 * Tracks the current session id, the idle timeout, and per-calendar-day first-open
 * dedup (so `daily_open` fires at most once per day).
 */
internal class SessionManager(
    private val prefs: SharedPreferences,
    private val timeoutMs: Long,
) {
    data class Roll(
        val previousSessionId: String,
        val previousDurationMs: Long,
        val newSessionId: String,
    )

    private val lock = Any()
    private val dayFormatter: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getDefault()
    }

    fun currentSessionId(now: Long = System.currentTimeMillis()): String = synchronized(lock) {
        val existing = prefs.getString(KEY_ID, null)
        val last = prefs.getLong(KEY_LAST_EVENT, 0L)
        if (existing != null && last != 0L && now - last < timeoutMs) {
            return existing
        }
        val fresh = UUID.randomUUID().toString()
        prefs.edit()
            .putString(KEY_ID, fresh)
            .putLong(KEY_STARTED_AT, now)
            .putLong(KEY_LAST_EVENT, now)
            .apply()
        fresh
    }

    fun rollIfNeeded(now: Long = System.currentTimeMillis()): Roll? = synchronized(lock) {
        val existing = prefs.getString(KEY_ID, null)
        val last = prefs.getLong(KEY_LAST_EVENT, 0L)

        if (existing == null || last == 0L) {
            val fresh = UUID.randomUUID().toString()
            prefs.edit()
                .putString(KEY_ID, fresh)
                .putLong(KEY_STARTED_AT, now)
                .putLong(KEY_LAST_EVENT, now)
                .apply()
            return null
        }

        if (now - last < timeoutMs) return null

        val startedAt = prefs.getLong(KEY_STARTED_AT, last)
        val duration = (last - startedAt).coerceAtLeast(0)
        val fresh = UUID.randomUUID().toString()
        prefs.edit()
            .putString(KEY_ID, fresh)
            .putLong(KEY_STARTED_AT, now)
            .putLong(KEY_LAST_EVENT, now)
            .apply()
        return Roll(existing, duration, fresh)
    }

    fun touchEvent(now: Long = System.currentTimeMillis()) = synchronized(lock) {
        prefs.edit().putLong(KEY_LAST_EVENT, now).apply()
    }

    /** Returns true exactly once per calendar day. */
    fun touchDay(now: Long = System.currentTimeMillis()): Boolean = synchronized(lock) {
        val today = dayFormatter.format(Date(now))
        if (prefs.getString(KEY_LAST_DAY, null) == today) return false
        prefs.edit().putString(KEY_LAST_DAY, today).apply()
        true
    }

    /** Returns (sessionId, durationMs) for the currently open session, used when backgrounding. */
    fun snapshotForBackgrounding(now: Long = System.currentTimeMillis()): Pair<String, Long>? = synchronized(lock) {
        val id = prefs.getString(KEY_ID, null) ?: return null
        val startedAt = prefs.getLong(KEY_STARTED_AT, 0L)
        if (startedAt == 0L) return null
        id to (now - startedAt).coerceAtLeast(0)
    }

    companion object {
        private const val KEY_ID = "session_id"
        private const val KEY_STARTED_AT = "session_started_at"
        private const val KEY_LAST_EVENT = "session_last_event"
        private const val KEY_LAST_DAY = "session_last_day"
    }
}
