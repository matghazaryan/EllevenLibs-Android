package com.ellevenstudio.esupabaseanalytics.internal

import android.content.SharedPreferences
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

/**
 * Disk-backed FIFO queue of event JSON objects, stored as a single JSON array
 * string in [SharedPreferences] under [storageKey] (default "queue"). The
 * sister observability recorders (`ESupabaseNetworkRecorder`,
 * `ESupabaseRevenueRecorder`, `ESupabaseCrashReporter`) instantiate their own
 * EventQueue with a distinct key so the four streams don't clobber each other.
 *
 * All access goes through a [Mutex] so writes from the public `track(...)`
 * path and reads from the flush loop can't race.
 */
internal class EventQueue(
    private val prefs: SharedPreferences,
    private val storageKey: String = DEFAULT_KEY,
) {
    private val mutex = Mutex()

    suspend fun enqueue(event: JSONObject) {
        mutex.withLock {
            val current = loadLocked()
            current.put(event)
            saveLocked(current)
        }
    }

    /** Snapshot up to [limit] events for the next upload attempt. */
    suspend fun snapshot(limit: Int = 200): List<JSONObject> {
        mutex.withLock {
            val current = loadLocked()
            val take = minOf(limit, current.length())
            val out = ArrayList<JSONObject>(take)
            for (i in 0 until take) {
                out.add(current.getJSONObject(i))
            }
            return out
        }
    }

    /** Drops the first [count] events after a successful upload. */
    suspend fun remove(count: Int) {
        if (count <= 0) return
        mutex.withLock {
            val current = loadLocked()
            val remaining = JSONArray()
            for (i in count until current.length()) {
                remaining.put(current.getJSONObject(i))
            }
            saveLocked(remaining)
        }
    }

    suspend fun clear() {
        mutex.withLock {
            prefs.edit().remove(storageKey).apply()
        }
    }

    suspend fun count(): Int {
        mutex.withLock {
            return loadLocked().length()
        }
    }

    // Must be called with [mutex] held.
    private fun loadLocked(): JSONArray {
        val raw = prefs.getString(storageKey, null) ?: return JSONArray()
        return try {
            JSONArray(raw)
        } catch (_: Exception) {
            JSONArray()
        }
    }

    private fun saveLocked(array: JSONArray) {
        if (array.length() == 0) {
            prefs.edit().remove(storageKey).apply()
        } else {
            prefs.edit().putString(storageKey, array.toString()).apply()
        }
    }

    companion object {
        private const val DEFAULT_KEY = "queue"
    }
}
