package com.vozluma.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Ubicación: app/src/main/java/com/vozluma/app/HistoryStore.kt
 * Guarda únicamente un historial local limitado y se puede borrar desde la app.
 */
object HistoryStore {
    private const val FILE_NAME = "vozluma_history"
    private const val KEY_ENTRIES = "entries"
    private const val MAX_ENTRIES = 50

    data class Entry(
        val appName: String,
        val sender: String,
        val message: String,
        val timestamp: Long
    )

    fun add(context: Context, appName: String, sender: String, message: String) {
        if (!PreferencesStore.isHistoryEnabled(context)) return
        val entries = read(context).toMutableList()
        entries.add(0, Entry(appName, sender, message, System.currentTimeMillis()))
        write(context, entries.take(MAX_ENTRIES))
    }

    fun getAll(context: Context): List<Entry> = read(context)

    fun clear(context: Context) {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_ENTRIES).apply()
    }

    private fun read(context: Context): List<Entry> {
        val raw = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ENTRIES, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        Entry(
                            item.optString("app"),
                            item.optString("sender"),
                            item.optString("message"),
                            item.optLong("timestamp")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun write(context: Context, entries: List<Entry>) {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(
                JSONObject().apply {
                    put("app", entry.appName)
                    put("sender", entry.sender)
                    put("message", entry.message)
                    put("timestamp", entry.timestamp)
                }
            )
        }
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_ENTRIES, array.toString()).apply()
    }
}
