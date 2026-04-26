package com.ellevenstudio.esupabaseanalytics

import com.ellevenstudio.esupabaseanalytics.internal.EventQueue
import com.ellevenstudio.esupabaseanalytics.internal.PIIScrubber
import com.ellevenstudio.esupabaseanalytics.internal.Uploader
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Logs every HTTP / Edge Function call to the `network_requests` table for
 * the same Supabase project that [ESupabaseAnalytics] is configured against.
 * Reads device + session + identity context from `ESupabaseAnalytics` so it
 * doesn't need its own configure call — the consumer just calls
 * `ESupabaseAnalytics.configure(...)` once, then logs requests via
 * `ESupabaseNetworkRecorder.log(...)`.
 *
 * The record-on-success vs record-on-error policy is up to the caller: this
 * API records *whatever you log*. The recommended consumer pattern is to
 * always call `log(...)` after every meaningful HTTP call, but pass
 * `requestBody` / `responseBody` only on failure (or sample 1% of successes)
 * so the table doesn't balloon.
 *
 * Bodies are PII-scrubbed via [PIIScrubber] before insert.
 */
object ESupabaseNetworkRecorder {
    private const val PREFS_KEY = "network_queue"

    @Volatile var isEnabled: Boolean = true
        private set

    private var queue: EventQueue? = null
    private var flushJob: Job? = null

    private val isoFormatter: SimpleDateFormat by lazy {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    // MARK: - Public API

    /**
     * Log a network request. Call this from your HTTP wrapper / interceptor
     * after the response (or transport failure) lands.
     */
    fun log(
        endpoint: String,
        httpMethod: String,
        httpStatus: Int?,
        durationMs: Int,
        screen: String? = null,
        requestSizeBytes: Int? = null,
        requestBody: Map<String, Any?>? = null,
        responseSizeBytes: Int? = null,
        responseBody: Map<String, Any?>? = null,
        errorMessage: String? = null,
        errorSignature: String? = null,
        properties: Map<String, Any?>? = null,
    ) {
        if (!isEnabled) return
        val config = ESupabaseAnalytics.sharedConfig() ?: return
        val ctx = ESupabaseAnalytics.sharedDeviceContext() ?: return
        val q = ensureQueue() ?: return

        val now = System.currentTimeMillis()
        val sessionId = ESupabaseAnalytics.sharedTouchSession(now)

        val isError = httpStatus?.let { it >= 400 } ?: true
        val signature = errorSignature ?: if (isError) makeSignature(endpoint, httpStatus) else null

        val row = JSONObject()
        row.put("occurred_at", isoFormatter.format(Date(now)))
        row.put("device_id", ctx.deviceId)
        row.put("platform", "android")
        row.put("endpoint", endpoint)
        row.put("http_method", httpMethod.uppercase())
        row.put("duration_ms", durationMs)
        sessionId?.let { row.put("session_id", it) }
        ESupabaseAnalytics.sharedUserId()?.let { row.put("user_id", it) }
        ctx.appVersion?.let { row.put("app_version", it) }
        ctx.appBuild?.let { row.put("app_build", it) }
        row.put("os_version", ctx.osVersion)
        screen?.let { row.put("screen", it) }
        httpStatus?.let { row.put("http_status", it) }
        requestSizeBytes?.let { row.put("request_size_bytes", it) }
        responseSizeBytes?.let { row.put("response_size_bytes", it) }
        PIIScrubber.scrub(requestBody)?.takeIf { it.length() > 0 }?.let { row.put("request_body", it) }
        PIIScrubber.scrub(responseBody)?.takeIf { it.length() > 0 }?.let { row.put("response_body", it) }
        errorMessage?.let { row.put("error_message", it) }
        signature?.let { row.put("error_signature", it) }
        PIIScrubber.scrub(properties)?.takeIf { it.length() > 0 }?.let { row.put("properties", it) }

        ESupabaseAnalytics.scope.launch { q.enqueue(row) }
        ensureFlushLoop()
    }

    /** Force an upload attempt now. */
    fun flush() {
        ESupabaseAnalytics.scope.launch { performFlush() }
    }

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        if (enabled) {
            ensureFlushLoop()
        } else {
            flushJob?.cancel()
            flushJob = null
            queue?.let { q -> ESupabaseAnalytics.scope.launch { q.clear() } }
        }
    }

    // MARK: - Internals

    private fun ensureQueue(): EventQueue? {
        val existing = queue
        if (existing != null) return existing
        val prefs = ESupabaseAnalytics.sharedPrefs() ?: return null
        val fresh = EventQueue(prefs, PREFS_KEY)
        queue = fresh
        return fresh
    }

    private fun makeSignature(endpoint: String, httpStatus: Int?): String {
        // FNV-1a 64-bit; stable across launches and devices.
        val raw = "$endpoint|${httpStatus?.toString() ?: "no-response"}"
        var h = 0xcbf29ce484222325uL
        for (b in raw.toByteArray()) {
            h = h xor (b.toUByte().toULong())
            h *= 0x100000001b3uL
        }
        return "%012x".format(h.toLong())
    }

    private fun ensureFlushLoop() {
        if (flushJob?.isActive == true) return
        if (!isEnabled) return
        val interval = ESupabaseAnalytics.sharedConfig()?.flushIntervalMs ?: return
        flushJob = ESupabaseAnalytics.scope.launch {
            while (isActive) {
                delay(interval)
                performFlush()
            }
        }
    }

    private suspend fun performFlush() {
        val q = queue ?: return
        val config = ESupabaseAnalytics.sharedConfig() ?: return
        val batch = q.snapshot()
        if (batch.isEmpty()) return

        val networkConfig = ESupabaseAnalyticsConfig(
            supabaseUrl = config.supabaseUrl,
            anonKey = config.anonKey,
            tableName = "network_requests",
            flushIntervalMs = config.flushIntervalMs,
            sessionTimeoutMs = config.sessionTimeoutMs,
        )
        val uploader = Uploader(networkConfig, ESupabaseAnalytics.sharedAuthToken())
        val ok = uploader.upload(batch)
        if (ok) q.remove(batch.size)
    }
}
