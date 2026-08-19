package com.vozluma.app

import android.content.Context

/**
 * Ubicación: app/src/main/java/com/vozluma/app/PreferencesStore.kt
 * Preferencias mínimas y locales; VozLuma no necesita servidor ni cuenta.
 */
object PreferencesStore {
    private const val FILE_NAME = "vozluma_preferences"
    private const val KEY_ENABLED = "assistant_enabled"

    private fun preferences(context: Context) =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun isAssistantEnabled(context: Context): Boolean =
        preferences(context).getBoolean(KEY_ENABLED, true)

    fun setAssistantEnabled(context: Context, enabled: Boolean) {
        preferences(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }
}
