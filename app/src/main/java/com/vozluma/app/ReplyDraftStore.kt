package com.vozluma.app

import android.content.Context

/**
 * Ubicación: app/src/main/java/com/vozluma/app/ReplyDraftStore.kt
 * Guarda solo el último borrador local para que el usuario lo revise.
 */
object ReplyDraftStore {
    private const val FILE_NAME = "vozluma_reply_draft"
    private const val KEY_TEXT = "draft_text"

    fun save(context: Context, text: String) {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_TEXT, text).apply()
    }

    fun get(context: Context): String = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
        .getString(KEY_TEXT, "") ?: ""

    fun clear(context: Context) {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_TEXT).apply()
    }
}
