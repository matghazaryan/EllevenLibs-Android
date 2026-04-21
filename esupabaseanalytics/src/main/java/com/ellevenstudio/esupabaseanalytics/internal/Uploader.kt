package com.ellevenstudio.esupabaseanalytics.internal

import com.ellevenstudio.esupabaseanalytics.ESupabaseAnalyticsConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * POSTs event batches to Supabase PostgREST using plain [HttpURLConnection],
 * so this library stays dependency-free.
 */
internal class Uploader(
    private val config: ESupabaseAnalyticsConfig,
    @Volatile var authToken: String? = null,
) {
    /** Returns true on 2xx, false on any transport or server error. */
    fun upload(events: List<JSONObject>): Boolean {
        if (events.isEmpty()) return true

        val base = config.supabaseUrl.trimEnd('/')
        val url = URL("$base/rest/v1/${config.tableName}")

        val conn = url.openConnection() as HttpURLConnection
        return try {
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.doInput = true
            conn.connectTimeout = 15_000
            conn.readTimeout = 15_000
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("apikey", config.anonKey)
            conn.setRequestProperty("Authorization", "Bearer ${authToken ?: config.anonKey}")
            conn.setRequestProperty("Prefer", "return=minimal")

            val array = JSONArray()
            events.forEach { array.put(it) }

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { w ->
                w.write(array.toString())
                w.flush()
            }

            val status = conn.responseCode
            if (status in 200..299) {
                true
            } else {
                val body = try {
                    (conn.errorStream ?: conn.inputStream)?.let { s ->
                        BufferedReader(InputStreamReader(s)).use { it.readText() }
                    }
                } catch (_: Exception) {
                    null
                }
                android.util.Log.w(TAG, "upload failed status=$status body=$body")
                false
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "upload failed: ${e.message}")
            false
        } finally {
            conn.disconnect()
        }
    }

    companion object {
        private const val TAG = "ESupabaseAnalytics"
    }
}
