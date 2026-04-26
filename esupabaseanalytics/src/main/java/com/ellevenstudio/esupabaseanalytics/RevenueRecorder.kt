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
 * Records authoritative purchase events to the `revenue` table for the same
 * Supabase project that [ESupabaseAnalytics] is configured against.
 *
 * Two write paths feed this table in production:
 *   - **Client** (this recorder): writes a `'purchase'` row immediately on
 *     successful Play Billing transaction. Has `session_id` so it joins to
 *     user behavior in the timeline view.
 *   - **Server** (your Edge Function webhooks): writes `'renewal'` /
 *     `'refund'` / `'cancellation'` rows. Authoritative for MRR/churn but
 *     has no `session_id`. Outside the scope of this recorder.
 *
 * The `revenue` table has `unique(external_provider, external_id, kind)` so
 * retries from this recorder *and* your server webhooks are idempotent —
 * duplicate writes are silently dropped by Postgres.
 */
object ESupabaseRevenueRecorder {
    private const val PREFS_KEY = "revenue_queue"

    @Volatile var isEnabled: Boolean = true
        private set

    private var queue: EventQueue? = null
    private var flushJob: Job? = null

    private val isoFormatter: SimpleDateFormat by lazy {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    /**
     * Record a purchase / renewal / refund / cancellation event.
     *
     * @param userId Supabase user id this purchase belongs to. **Required** —
     *               anonymous revenue rows are not allowed by the schema.
     * @param productId Product identifier from the platform.
     * @param productType `"subscription"` / `"consumable"` / `"non_consumable"`.
     * @param period `"monthly"` / `"yearly"` / `"lifetime"`, or null for one-time products.
     * @param amountCents Price in **integer cents** of the local currency. Never floats.
     * @param currency ISO 4217 code (e.g. `"USD"`, `"EUR"`).
     * @param kind `"purchase"` / `"renewal"` / `"refund"` / `"upgrade"` / `"downgrade"`
     *             / `"cancellation"` / `"restore"` / `"trial_start"` / `"trial_convert"`.
     * @param status `"succeeded"` (default) / `"pending"` / `"failed"`.
     * @param externalProvider `"google"` for Play Billing, `"stripe"` for web Stripe,
     *                         `"apple"` for StoreKit (iOS).
     * @param externalId Provider's authoritative ID. Google `purchase_token` /
     *                   Stripe charge id / Apple `original_transaction_id`.
     */
    fun recordPurchase(
        userId: String,
        productId: String,
        productType: String = "subscription",
        period: String? = null,
        amountCents: Int,
        currency: String,
        kind: String = "purchase",
        status: String = "succeeded",
        externalProvider: String = "google",
        externalId: String,
        errorCode: String? = null,
        properties: Map<String, Any?>? = null,
    ) {
        if (!isEnabled) return
        val config = ESupabaseAnalytics.sharedConfig() ?: return
        val ctx = ESupabaseAnalytics.sharedDeviceContext() ?: return
        val q = ensureQueue() ?: return

        val now = System.currentTimeMillis()
        val sessionId = ESupabaseAnalytics.sharedTouchSession(now)

        val row = JSONObject()
        row.put("occurred_at", isoFormatter.format(Date(now)))
        row.put("device_id", ctx.deviceId)
        row.put("user_id", userId)
        row.put("platform", "android")
        row.put("product_id", productId)
        row.put("product_type", productType)
        row.put("amount_cents", amountCents)
        row.put("currency", currency)
        row.put("kind", kind)
        row.put("status", status)
        row.put("external_provider", externalProvider)
        row.put("external_id", externalId)
        sessionId?.let { row.put("session_id", it) }
        ctx.appVersion?.let { row.put("app_version", it) }
        period?.let { row.put("period", it) }
        errorCode?.let { row.put("error_code", it) }
        PIIScrubber.scrub(properties)?.takeIf { it.length() > 0 }?.let { row.put("properties", it) }

        ESupabaseAnalytics.scope.launch { q.enqueue(row) }
        ensureFlushLoop()
    }

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

    private fun ensureQueue(): EventQueue? {
        val existing = queue
        if (existing != null) return existing
        val prefs = ESupabaseAnalytics.sharedPrefs() ?: return null
        val fresh = EventQueue(prefs, PREFS_KEY)
        queue = fresh
        return fresh
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

        val revenueConfig = ESupabaseAnalyticsConfig(
            supabaseUrl = config.supabaseUrl,
            anonKey = config.anonKey,
            tableName = "revenue",
            flushIntervalMs = config.flushIntervalMs,
            sessionTimeoutMs = config.sessionTimeoutMs,
        )
        val uploader = Uploader(revenueConfig, ESupabaseAnalytics.sharedAuthToken())
        val ok = uploader.upload(batch)
        if (ok) q.remove(batch.size)
    }
}
