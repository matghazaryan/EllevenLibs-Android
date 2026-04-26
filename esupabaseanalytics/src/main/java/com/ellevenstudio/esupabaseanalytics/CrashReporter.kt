package com.ellevenstudio.esupabaseanalytics

import android.content.Context
import com.ellevenstudio.esupabaseanalytics.internal.EventQueue
import com.ellevenstudio.esupabaseanalytics.internal.PIIScrubber
import com.ellevenstudio.esupabaseanalytics.internal.Uploader
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

/**
 * Captures uncaught JVM exceptions and explicitly-reported errors to the
 * `crashes` table for the same Supabase project that [ESupabaseAnalytics] is
 * configured against.
 *
 * **Lifecycle**
 *
 * 1. Consumer calls `ESupabaseCrashReporter.install(context)` once at app
 *    startup (after `ESupabaseAnalytics.configure(...)`).
 * 2. `install()` first scans the on-disk crash directory and uploads any
 *    pending records from the previous run (the *previous* crash is what
 *    we're shipping — we couldn't upload it during the crash itself).
 * 3. `install()` registers a `Thread.setDefaultUncaughtExceptionHandler`.
 *    When a crash hits, the handler serializes a JSON record to
 *    `<cacheDir>/ESupabaseAnalytics/pending_crashes/`, then chains to the
 *    previous handler (so other crash reporters in the app still fire and
 *    the process actually terminates as the OS expects).
 * 4. On the next launch, step 2 picks up the file, ships it, deletes it.
 *
 * **Caveats**
 *
 * - Native crashes (NDK / signal-level) are NOT captured. JVM uncaught
 *   exceptions and explicitly-reported errors only.
 * - ANRs are NOT captured (would need a watchdog thread). Skipped for now.
 * - Call [report] directly to log a non-fatal error (e.g. inside a `catch`
 *   block) without triggering process termination.
 */
object ESupabaseCrashReporter {
    private const val PREFS_KEY = "crash_queue"
    private const val CRASH_DIR_NAME = "ESupabaseAnalytics/pending_crashes"

    @Volatile var isInstalled: Boolean = false
        private set

    @Volatile var isEnabled: Boolean = true
        private set

    private var queue: EventQueue? = null
    private var flushJob: Job? = null
    private var crashDir: File? = null

    private val isoFormatter: SimpleDateFormat by lazy {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    /**
     * Install the JVM uncaught-exception handler and ship any pending crash
     * from the previous run. Idempotent — safe to call multiple times.
     */
    fun install(context: Context) {
        if (isInstalled) return
        isInstalled = true

        val app = context.applicationContext
        crashDir = File(app.cacheDir, CRASH_DIR_NAME).apply { mkdirs() }
        flushPendingCrashes()
        installUncaughtHandler()
        ensureFlushLoop()
    }

    /**
     * Report a non-fatal error — runs the same upload path as a crash but
     * without terminating the process. Use inside `catch` blocks where you
     * want analytics on the failure but the app keeps running.
     */
    fun report(
        throwable: Throwable,
        kind: String = "caught_error",
        screen: String? = null,
        properties: Map<String, Any?>? = null,
    ) {
        if (!isEnabled) return
        val q = ensureQueue() ?: return
        val record = buildRecord(
            throwable = throwable,
            kind = kind,
            isFatal = false,
            screen = screen,
            sessionId = ESupabaseAnalytics.sharedReadSessionWithoutTouch(),
            userId = ESupabaseAnalytics.sharedUserId(),
            properties = properties,
        )
        ESupabaseAnalytics.scope.launch { q.enqueue(record) }
        ensureFlushLoop()
    }

    fun flush() {
        ESupabaseAnalytics.scope.launch { performFlush() }
    }

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        if (!enabled) {
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

    /**
     * Build a JSON record from a throwable + identity context. Used by both
     * [report] (non-fatal) and the uncaught-exception handler (fatal).
     */
    private fun buildRecord(
        throwable: Throwable,
        kind: String,
        isFatal: Boolean,
        screen: String?,
        sessionId: String?,
        userId: String?,
        properties: Map<String, Any?>?,
    ): JSONObject {
        val ctx = ESupabaseAnalytics.sharedDeviceContext()
        val stack = StringWriter().also { sw ->
            throwable.printStackTrace(PrintWriter(sw))
        }.toString()

        val row = JSONObject()
        row.put("occurred_at", isoFormatter.format(Date()))
        if (ctx != null) row.put("device_id", ctx.deviceId) else row.put("device_id", "unknown")
        row.put("platform", "android")
        row.put("kind", kind)
        row.put("is_fatal", isFatal)
        row.put("crash_signature", signature(throwable.javaClass.name, kind, stack))
        ctx?.appVersion?.let { row.put("app_version", it) } ?: row.put("app_version", "unknown")
        ctx?.appBuild?.let { row.put("app_build", it) } ?: row.put("app_build", "unknown")
        ctx?.osVersion?.let { row.put("os_version", it) }
        ctx?.deviceModel?.let { row.put("device_model", it) }
        sessionId?.let { row.put("session_id", it) }
        userId?.let { row.put("user_id", it) }
        row.put("exception_class", throwable.javaClass.name)
        row.put("exception_message", throwable.message ?: "<no message>")
        row.put("stack_trace", stack)
        screen?.let { row.put("screen", it) }
        PIIScrubber.scrub(properties)?.takeIf { it.length() > 0 }?.let { row.put("properties", it) }
        return row
    }

    /** Stable hash for grouping identical crashes. */
    private fun signature(exceptionClass: String, kind: String, stackTrace: String): String {
        // Hash exceptionClass + kind + the first 3 stack frames. Truncating to
        // 3 frames means the same bug from different call paths still groups.
        val topFrames = stackTrace.lineSequence().take(3).joinToString("|")
        val raw = "$exceptionClass|$kind|$topFrames"
        var h = 0xcbf29ce484222325uL
        for (b in raw.toByteArray()) {
            h = h xor (b.toUByte().toULong())
            h *= 0x100000001b3uL
        }
        return "%016x".format(h.toLong())
    }

    /**
     * Persist a record to disk synchronously. Called from the uncaught
     * handler so the process can be torn down right after.
     */
    private fun persistRecordToDisk(record: JSONObject) {
        val dir = crashDir ?: return
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "crash_${System.currentTimeMillis()}_${UUID.randomUUID()}.json")
        try {
            file.writeText(record.toString())
        } catch (_: Exception) {
            // Best-effort — if we can't write, the crash is lost.
        }
    }

    /** Read every pending crash file, enqueue them, delete the files. */
    private fun flushPendingCrashes() {
        val dir = crashDir ?: return
        val q = ensureQueue() ?: return
        val files = dir.listFiles { f -> f.extension == "json" } ?: return
        for (f in files) {
            try {
                val text = f.readText()
                val obj = JSONObject(text)
                runBlocking { q.enqueue(obj) }
            } catch (_: Exception) {
                // skip malformed file
            } finally {
                try { f.delete() } catch (_: Exception) {}
            }
        }
    }

    private fun installUncaughtHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // We're already in a dying-process state. Foundation is fine here
            // — JVM exceptions don't have the async-signal-safety constraints
            // that POSIX signals do.
            try {
                val record = buildRecord(
                    throwable = throwable,
                    kind = "uncaught_exception",
                    isFatal = true,
                    screen = null,
                    sessionId = ESupabaseAnalytics.sharedReadSessionWithoutTouch(),
                    userId = ESupabaseAnalytics.sharedUserId(),
                    properties = mapOf("thread_name" to thread.name),
                )
                persistRecordToDisk(record)
            } catch (_: Throwable) {
                // Don't let crash-recording itself crash the crash handler.
            }
            // Chain to the previous handler so other reporters fire and the OS
            // gets the standard crash flow (process terminates).
            previous?.uncaughtException(thread, throwable)
        }
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

        val crashConfig = ESupabaseAnalyticsConfig(
            supabaseUrl = config.supabaseUrl,
            anonKey = config.anonKey,
            tableName = "crashes",
            flushIntervalMs = config.flushIntervalMs,
            sessionTimeoutMs = config.sessionTimeoutMs,
        )
        val uploader = Uploader(crashConfig, ESupabaseAnalytics.sharedAuthToken())
        val ok = uploader.upload(batch)
        if (ok) q.remove(batch.size)
    }
}
