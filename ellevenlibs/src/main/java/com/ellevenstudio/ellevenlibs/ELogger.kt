package com.ellevenstudio.ellevenlibs

import android.util.Log

/**
 * A simple tagged logger for consistent logging across your apps.
 *
 * Usage:
 * ```kotlin
 * val logger = ELogger("Network")
 * logger.debug("Request started")
 * logger.info("Response received")
 * logger.warning("Slow response: 3.2s")
 * logger.error("Request failed: timeout")
 * ```
 */
class ELogger(private val tag: String) {

    fun debug(message: String) {
        Log.d(tag, message)
    }

    fun info(message: String) {
        Log.i(tag, message)
    }

    fun warning(message: String) {
        Log.w(tag, message)
    }

    fun error(message: String) {
        Log.e(tag, message)
    }

    fun error(message: String, throwable: Throwable) {
        Log.e(tag, message, throwable)
    }
}
