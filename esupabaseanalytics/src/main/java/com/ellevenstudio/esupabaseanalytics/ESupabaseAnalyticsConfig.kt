package com.ellevenstudio.esupabaseanalytics

/**
 * Configuration for [ESupabaseAnalytics].
 *
 * Pass the same Supabase URL and anon key the consumer app already uses for its
 * own project — events land in the same project, in the [tableName] table
 * (default `"analytics"`, created by the shipped SQL migration).
 *
 * @param supabaseUrl Base URL of the Supabase project (e.g. `https://xyz.supabase.co`).
 * @param anonKey The project's anon/publishable key. User JWTs can be layered on top via `setAuthToken`.
 * @param tableName Target table. Default `"analytics"`.
 * @param flushIntervalMs How often the background flush runs while foregrounded. Default `5_000`.
 * @param sessionTimeoutMs Inactivity after which a new session is rolled. Default `30 * 60_000` (30 min).
 */
data class ESupabaseAnalyticsConfig(
    val supabaseUrl: String,
    val anonKey: String,
    val tableName: String = "analytics",
    val flushIntervalMs: Long = 5_000L,
    val sessionTimeoutMs: Long = 30L * 60L * 1000L,
) {
    init {
        require(supabaseUrl.isNotBlank()) { "[ESupabaseAnalytics] supabaseUrl must not be blank." }
        require(anonKey.isNotBlank()) { "[ESupabaseAnalytics] anonKey must not be blank." }
        require(tableName.isNotBlank()) { "[ESupabaseAnalytics] tableName must not be blank." }
    }
}
