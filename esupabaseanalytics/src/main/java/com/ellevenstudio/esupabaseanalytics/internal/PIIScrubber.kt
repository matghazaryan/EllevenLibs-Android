package com.ellevenstudio.esupabaseanalytics.internal

import org.json.JSONArray
import org.json.JSONObject

/**
 * Scrubs PII / secrets from request and response bodies before they're
 * uploaded to the observability tables.
 *
 * Conservative deny list: any key whose lowercase contains one of the deny
 * substrings is replaced with the literal string `"<redacted>"`. Recurses
 * through nested JSONObjects and JSONArrays.
 *
 * This intentionally errs on the side of dropping too much rather than too
 * little. False positives (e.g. a column literally called `password_hint`)
 * will lose readable values; the tradeoff is worth it for compliance.
 */
internal object PIIScrubber {
    /** Substrings checked case-insensitively against each key. */
    private val denyList: Set<String> = setOf(
        "password",
        "passwd",
        "secret",
        "token",
        "apikey",
        "api_key",
        "authorization",
        "auth",
        "bearer",
        "session",
        "cookie",
        "x-api-key",
        "credit_card",
        "creditcard",
        "card_number",
        "cvv",
        "ssn",
    )

    /** Returns a deep-cleaned copy of [json]. Original input is not mutated. */
    fun scrub(json: JSONObject?): JSONObject? {
        if (json == null) return null
        val out = JSONObject()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val raw = json.opt(key) ?: continue
            if (isSensitive(key)) {
                out.put(key, "<redacted>")
                continue
            }
            when (raw) {
                is JSONObject -> out.put(key, scrub(raw))
                is JSONArray -> out.put(key, scrubArray(raw))
                else -> out.put(key, raw)
            }
        }
        return out
    }

    /** Convenience overload for `Map<String, Any?>`. */
    fun scrub(map: Map<String, Any?>?): JSONObject? {
        if (map == null) return null
        val json = JSONObject()
        for ((k, v) in map) {
            if (v != null) json.put(k, v)
        }
        return scrub(json)
    }

    private fun scrubArray(array: JSONArray): JSONArray {
        val out = JSONArray()
        for (i in 0 until array.length()) {
            when (val item = array.opt(i)) {
                is JSONObject -> out.put(scrub(item))
                is JSONArray -> out.put(scrubArray(item))
                else -> out.put(item)
            }
        }
        return out
    }

    private fun isSensitive(key: String): Boolean {
        val lower = key.lowercase()
        return denyList.any { lower.contains(it) }
    }
}
