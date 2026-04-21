package com.ellevenstudio.esupabaseanalytics.internal

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.os.Build
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

internal class DeviceContext(
    val deviceId: String,
    val osVersion: String,
    val deviceModel: String,
    val appVersion: String?,
    val appBuild: String?,
    val locale: String,
    val timezone: String,
    val isDebug: Boolean,
) {
    companion object {
        fun from(context: Context, prefs: SharedPreferences): DeviceContext {
            val deviceId = prefs.getString(KEY_DEVICE_ID, null) ?: run {
                val fresh = UUID.randomUUID().toString()
                prefs.edit().putString(KEY_DEVICE_ID, fresh).apply()
                fresh
            }

            val pm = context.packageManager
            val pkg = try {
                pm.getPackageInfo(context.packageName, 0)
            } catch (_: Exception) {
                null
            }
            val versionName = pkg?.versionName
            @Suppress("DEPRECATION")
            val versionCode: String? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkg?.longVersionCode?.toString()
            } else {
                pkg?.versionCode?.toString()
            }

            val isDebug = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

            return DeviceContext(
                deviceId = deviceId,
                osVersion = Build.VERSION.RELEASE ?: "",
                deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
                appVersion = versionName,
                appBuild = versionCode,
                locale = Locale.getDefault().toLanguageTag(),
                timezone = TimeZone.getDefault().id,
                isDebug = isDebug,
            )
        }

        private const val KEY_DEVICE_ID = "device_id"
    }
}
