package com.ellevenstudio.esupabaseanalytics

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.ellevenstudio.esupabaseanalytics.internal.DeviceContext
import com.ellevenstudio.esupabaseanalytics.internal.EventQueue
import com.ellevenstudio.esupabaseanalytics.internal.SessionManager
import com.ellevenstudio.esupabaseanalytics.internal.Uploader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Lightweight analytics client that writes events to a Supabase Postgres table
 * via PostgREST. Replaces Firebase Analytics for simple use cases.
 *
 * Usage:
 *     ESupabaseAnalytics.configure(
 *         context = this,
 *         config = ESupabaseAnalyticsConfig(
 *             supabaseUrl = "https://xyz.supabase.co",
 *             anonKey = "..."
 *         )
 *     )
 *     ESupabaseAnalytics.setUserId("user_123")
 *     ESupabaseAnalytics.track("button_tap", mapOf("name" to "subscribe"))
 *     ESupabaseAnalytics.trackScreen("Home")
 */
object ESupabaseAnalytics {
    private const val PREFS_NAME = "esupabaseanalytics_prefs"

    @Volatile var isConfigured: Boolean = false
        private set

    @Volatile var isEnabled: Boolean = true
        private set

    private var config: ESupabaseAnalyticsConfig? = null
    private var queue: EventQueue? = null
    private var uploader: Uploader? = null
    private var session: SessionManager? = null
    private var deviceContext: DeviceContext? = null

    private var prefs: SharedPreferences? = null
    internal val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var flushJob: Job? = null

    @Volatile private var userId: String? = null
    @Volatile private var authToken: String? = null

    // Internal accessors for sister recorders (NetworkRecorder, CrashReporter,
    // RevenueRecorder). Read-only — public mutators are still the only writers.
    internal fun sharedConfig(): ESupabaseAnalyticsConfig? = config
    internal fun sharedUserId(): String? = userId
    internal fun sharedAuthToken(): String? = authToken
    internal fun sharedDeviceContext(): DeviceContext? = deviceContext
    internal fun sharedPrefs(): SharedPreferences? = prefs

    /** Touches the session (extends activity timer) and returns the current id. */
    internal fun sharedTouchSession(now: Long = System.currentTimeMillis()): String? {
        return session?.currentSessionId(now)
    }

    /** Reads the in-flight session id without touching `lastEventAt`. */
    internal fun sharedReadSessionWithoutTouch(): String? = session?.peekSessionId()

    private var lifecycleObserver: DefaultLifecycleObserver? = null

    private val isoFormatter: SimpleDateFormat by lazy {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    /** Configure with a Supabase URL + anon key. MUST be called before any `track`. */
    fun configure(context: Context, config: ESupabaseAnalyticsConfig) {
        val app = context.applicationContext
        this.config = config
        val prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        this.prefs = prefs
        this.queue = EventQueue(prefs)
        this.uploader = Uploader(config, authToken)
        this.session = SessionManager(prefs, config.sessionTimeoutMs)
        this.deviceContext = DeviceContext.from(app, prefs)
        this.isConfigured = true

        installLifecycleObserver()
        handleForeground()
        startFlushLoop()
    }

    // MARK: - Identity

    fun setUserId(userId: String?) {
        this.userId = userId
    }

    fun setAuthToken(token: String?) {
        this.authToken = token
        uploader?.authToken = token
    }

    // MARK: - Tracking

    fun track(name: String, properties: Map<String, Any?>? = null) {
        if (!isEnabled) return
        val session = session ?: return
        val queue = queue ?: return

        val now = System.currentTimeMillis()

        session.rollIfNeeded(now)?.let { roll ->
            enqueueInternal(
                queue,
                name = "session_end",
                atEpochMs = now,
                sessionId = roll.previousSessionId,
                screenName = null,
                extraProperties = mapOf("duration_ms" to roll.previousDurationMs),
            )
            enqueueInternal(
                queue,
                name = "session_start",
                atEpochMs = now,
                sessionId = roll.newSessionId,
                screenName = null,
                extraProperties = null,
            )
        }

        val sessionId = session.currentSessionId(now)

        if (session.touchDay(now)) {
            enqueueInternal(
                queue,
                name = "daily_open",
                atEpochMs = now,
                sessionId = sessionId,
                screenName = null,
                extraProperties = null,
            )
        }

        enqueueInternal(
            queue,
            name = name,
            atEpochMs = now,
            sessionId = sessionId,
            screenName = properties?.get("screen_name") as? String,
            extraProperties = properties,
        )

        session.touchEvent(now)
    }

    fun trackScreen(name: String, properties: Map<String, Any?>? = null) {
        val merged = (properties ?: emptyMap()).toMutableMap()
        merged["screen_name"] = name
        track("screen_view", merged)
    }

    /** Force an upload attempt now. */
    fun flush() {
        scope.launch { performFlush() }
    }

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        if (enabled) {
            startFlushLoop()
        } else {
            flushJob?.cancel()
            flushJob = null
            queue?.let { q -> scope.launch { q.clear() } }
        }
    }

    // MARK: - Internal

    private fun enqueueInternal(
        queue: EventQueue,
        name: String,
        atEpochMs: Long,
        sessionId: String,
        screenName: String?,
        extraProperties: Map<String, Any?>?,
    ) {
        val ctx = deviceContext ?: return
        val json = JSONObject()
        json.put("event_name", name)
        json.put("occurred_at", isoFormatter.format(Date(atEpochMs)))
        json.put("device_id", ctx.deviceId)
        json.put("session_id", sessionId)
        json.put("platform", "android")
        json.put("os_version", ctx.osVersion)
        json.put("device_model", ctx.deviceModel)
        json.put("locale", ctx.locale)
        json.put("timezone", ctx.timezone)
        json.put("is_debug", ctx.isDebug)
        ctx.appVersion?.let { json.put("app_version", it) }
        ctx.appBuild?.let { json.put("app_build", it) }
        userId?.let { json.put("user_id", it) }
        screenName?.let { json.put("screen_name", it) }

        if (!extraProperties.isNullOrEmpty()) {
            val reserved = setOf(
                "event_name", "occurred_at", "device_id", "session_id", "platform",
                "os_version", "device_model", "locale", "timezone", "is_debug",
                "app_version", "app_build", "icloud_id", "user_id", "screen_name",
            )
            val props = JSONObject()
            for ((k, v) in extraProperties) {
                if (k in reserved) continue
                if (v == null) continue
                try {
                    props.put(k, v)
                } catch (_: Exception) {
                    // skip non-JSON-serializable values
                }
            }
            if (props.length() > 0) {
                json.put("properties", props)
            }
        }

        scope.launch { queue.enqueue(json) }
    }

    private fun startFlushLoop() {
        flushJob?.cancel()
        val interval = config?.flushIntervalMs ?: return
        if (!isEnabled) return
        flushJob = scope.launch {
            while (isActive) {
                delay(interval)
                performFlush()
            }
        }
    }

    private suspend fun performFlush() {
        val queue = queue ?: return
        val uploader = uploader ?: return
        val batch = queue.snapshot()
        if (batch.isEmpty()) return
        val ok = uploader.upload(batch)
        if (ok) {
            queue.remove(batch.size)
        }
        // else: leave the batch in the queue; next tick retries.
    }

    // MARK: - Lifecycle

    private fun installLifecycleObserver() {
        if (lifecycleObserver != null) return
        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) { handleForeground() }
            override fun onStop(owner: LifecycleOwner) { handleBackground() }
        }
        lifecycleObserver = observer
        ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
    }

    private fun handleForeground() {
        if (!isEnabled) return
        val session = session ?: return
        val queue = queue ?: return
        val now = System.currentTimeMillis()
        val roll = session.rollIfNeeded(now)
        if (roll != null) {
            enqueueInternal(
                queue,
                name = "session_end",
                atEpochMs = now,
                sessionId = roll.previousSessionId,
                screenName = null,
                extraProperties = mapOf("duration_ms" to roll.previousDurationMs),
            )
            enqueueInternal(
                queue,
                name = "session_start",
                atEpochMs = now,
                sessionId = roll.newSessionId,
                screenName = null,
                extraProperties = null,
            )
        } else {
            val sessionId = session.currentSessionId(now)
            scope.launch {
                if (queue.count() == 0) {
                    enqueueInternal(
                        queue,
                        name = "session_start",
                        atEpochMs = now,
                        sessionId = sessionId,
                        screenName = null,
                        extraProperties = null,
                    )
                }
            }
        }
        session.touchEvent(now)
    }

    private fun handleBackground() {
        if (!isEnabled) return
        val session = session ?: return
        val queue = queue ?: return
        val now = System.currentTimeMillis()
        val snapshot = session.snapshotForBackgrounding(now) ?: return
        enqueueInternal(
            queue,
            name = "session_end",
            atEpochMs = now,
            sessionId = snapshot.first,
            screenName = null,
            extraProperties = mapOf("duration_ms" to snapshot.second),
        )
        session.touchEvent(now)
    }
}
