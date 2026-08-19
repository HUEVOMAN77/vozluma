package com.vozluma.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Comandos personalizados completamente locales: frase de activación -> respuesta hablada.
 */
object CustomCommandStore {
    private const val FILE_NAME = "vozluma_custom_commands"
    private const val KEY_COMMANDS = "commands"

    data class Command(val phrase: String, val response: String)

    private fun preferences(context: Context) =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun all(context: Context): List<Command> {
        val raw = preferences(context).getString(KEY_COMMANDS, "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(Command(item.optString("phrase"), item.optString("response")))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun save(context: Context, phrase: String, response: String) {
        val cleanPhrase = phrase.trim().lowercase()
        val cleanResponse = response.trim()
        if (cleanPhrase.length < 2 || cleanResponse.isBlank()) return
        val commands = all(context).filterNot { it.phrase == cleanPhrase }.toMutableList()
        commands.add(Command(cleanPhrase, cleanResponse))
        val array = JSONArray().apply {
            commands.forEach {
                put(JSONObject().put("phrase", it.phrase).put("response", it.response))
            }
        }
        preferences(context).edit().putString(KEY_COMMANDS, array.toString()).apply()
    }

    fun deleteAll(context: Context) {
        preferences(context).edit().remove(KEY_COMMANDS).apply()
    }

    fun match(context: Context, input: String): String? {
        val normalized = input.lowercase().trim()
        return all(context).firstOrNull { normalized.contains(it.phrase) }?.response
    }
}
