package com.vozluma.app

import android.content.Context
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Ubicación: app/src/main/java/com/vozluma/app/PreferencesStore.kt
 * Preferencias locales para que VozLuma funcione sin cuenta ni servidor.
 */
object PreferencesStore {
    private const val FILE_NAME = "vozluma_preferences"
    private const val KEY_ENABLED = "assistant_enabled"
    private const val KEY_VOICE_ACTIVATION = "voice_activation_enabled"
    private const val KEY_ONLY_HEADPHONES = "only_headphones"
    private const val KEY_CAR_MODE = "car_mode"
    private const val KEY_HISTORY = "history_enabled"
    private const val KEY_SMART_FILTERS = "smart_filters_enabled"
    private const val KEY_QUIET_HOURS = "quiet_hours_enabled"
    private const val KEY_QUIET_START = "quiet_start"
    private const val KEY_QUIET_END = "quiet_end"
    private const val KEY_THEME = "theme_mode"
    private const val KEY_SPEECH_RATE = "speech_rate"
    private const val KEY_APPS = "enabled_apps"

    val supportedPackages = linkedMapOf(
        "com.whatsapp" to "WhatsApp",
        "com.facebook.orca" to "Messenger",
        "com.facebook.katana" to "Facebook",
        "com.instagram.android" to "Instagram",
        "com.google.android.apps.messaging" to "Mensajes de Google",
        "com.android.mms" to "SMS",
        "com.samsung.android.messaging" to "Mensajes Samsung"
    )

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    private fun preferences(context: Context) =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun isAssistantEnabled(context: Context): Boolean =
        preferences(context).getBoolean(KEY_ENABLED, true)

    fun setAssistantEnabled(context: Context, enabled: Boolean) {
        preferences(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun isVoiceActivationEnabled(context: Context): Boolean =
        preferences(context).getBoolean(KEY_VOICE_ACTIVATION, false)

    fun setVoiceActivationEnabled(context: Context, enabled: Boolean) {
        preferences(context).edit().putBoolean(KEY_VOICE_ACTIVATION, enabled).apply()
    }

    fun onlyWithHeadphones(context: Context): Boolean =
        preferences(context).getBoolean(KEY_ONLY_HEADPHONES, false)

    fun setOnlyWithHeadphones(context: Context, enabled: Boolean) {
        preferences(context).edit().putBoolean(KEY_ONLY_HEADPHONES, enabled).apply()
    }

    fun isCarModeEnabled(context: Context): Boolean =
        preferences(context).getBoolean(KEY_CAR_MODE, false)

    fun setCarModeEnabled(context: Context, enabled: Boolean) {
        preferences(context).edit().putBoolean(KEY_CAR_MODE, enabled).apply()
    }

    fun isHistoryEnabled(context: Context): Boolean =
        preferences(context).getBoolean(KEY_HISTORY, true)

    fun setHistoryEnabled(context: Context, enabled: Boolean) {
        preferences(context).edit().putBoolean(KEY_HISTORY, enabled).apply()
    }

    fun areSmartFiltersEnabled(context: Context): Boolean =
        preferences(context).getBoolean(KEY_SMART_FILTERS, true)

    fun setSmartFiltersEnabled(context: Context, enabled: Boolean) {
        preferences(context).edit().putBoolean(KEY_SMART_FILTERS, enabled).apply()
    }

    fun isQuietHoursEnabled(context: Context): Boolean =
        preferences(context).getBoolean(KEY_QUIET_HOURS, false)

    fun setQuietHoursEnabled(context: Context, enabled: Boolean) {
        preferences(context).edit().putBoolean(KEY_QUIET_HOURS, enabled).apply()
    }

    fun getQuietStart(context: Context): String =
        preferences(context).getString(KEY_QUIET_START, "22:00") ?: "22:00"

    fun getQuietEnd(context: Context): String =
        preferences(context).getString(KEY_QUIET_END, "07:00") ?: "07:00"

    fun setQuietHours(context: Context, start: String, end: String) {
        preferences(context).edit()
            .putString(KEY_QUIET_START, start)
            .putString(KEY_QUIET_END, end)
            .apply()
    }

    fun isQuietNow(context: Context): Boolean {
        if (!isQuietHoursEnabled(context)) return false
        return try {
            val now = LocalTime.now()
            val start = LocalTime.parse(getQuietStart(context), timeFormatter)
            val end = LocalTime.parse(getQuietEnd(context), timeFormatter)
            if (start <= end) now >= start && now < end else now >= start || now < end
        } catch (_: Exception) {
            false
        }
    }

    fun themeMode(context: Context): Int =
        preferences(context).getInt(KEY_THEME, 0)

    fun setThemeMode(context: Context, mode: Int) {
        preferences(context).edit().putInt(KEY_THEME, mode).apply()
    }

    fun speechRate(context: Context): Float =
        preferences(context).getFloat(KEY_SPEECH_RATE, 0.95f)

    fun setSpeechRate(context: Context, value: Float) {
        preferences(context).edit().putFloat(KEY_SPEECH_RATE, value.coerceIn(0.6f, 1.4f)).apply()
    }

    fun isPackageEnabled(context: Context, packageName: String): Boolean {
        val stored = preferences(context).getStringSet(KEY_APPS, null)
        return stored?.contains(packageName) ?: supportedPackages.containsKey(packageName)
    }

    fun setPackageEnabled(context: Context, packageName: String, enabled: Boolean) {
        val current = preferences(context).getStringSet(
            KEY_APPS,
            supportedPackages.keys.toSet()
        )?.toMutableSet() ?: mutableSetOf()
        if (enabled) current.add(packageName) else current.remove(packageName)
        preferences(context).edit().putStringSet(KEY_APPS, current).apply()
    }
}
