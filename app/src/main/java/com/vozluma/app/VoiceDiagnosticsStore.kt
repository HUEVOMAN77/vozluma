package com.vozluma.app

import android.content.Context

/**
 * Guarda únicamente el estado técnico más reciente de la escucha para poder diagnosticar el teléfono.
 */
object VoiceDiagnosticsStore {
    private const val FILE_NAME = "vozluma_voice_diagnostics"
    private const val KEY_STATE = "state"
    private const val KEY_DETAIL = "detail"
    private const val KEY_TIME = "time"

    fun set(context: Context, state: String, detail: String = "") {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_STATE, state)
            .putString(KEY_DETAIL, detail)
            .putLong(KEY_TIME, System.currentTimeMillis())
            .apply()
    }

    fun message(context: Context): String {
        val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
        val state = preferences.getString(KEY_STATE, "sin iniciar").orEmpty()
        val detail = preferences.getString(KEY_DETAIL, "").orEmpty()
        return if (detail.isBlank()) state else "$state · $detail"
    }
}
