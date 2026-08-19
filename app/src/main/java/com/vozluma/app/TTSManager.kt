package com.vozluma.app

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * Ubicación: app/src/main/java/com/vozluma/app/TTSManager.kt
 * Encapsula la inicialización y el uso del TTS nativo de Android.
 */
class TTSManager(context: Context) : TextToSpeech.OnInitListener {
    private val appContext = context.applicationContext
    private var textToSpeech: TextToSpeech? = TextToSpeech(appContext, this)
    private var isReady = false

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) return

        val engine = textToSpeech ?: return
        val languageResult = engine.setLanguage(Locale("es", "ES"))
        isReady = languageResult != TextToSpeech.LANG_MISSING_DATA &&
            languageResult != TextToSpeech.LANG_NOT_SUPPORTED

        if (isReady) {
            engine.setSpeechRate(PreferencesStore.speechRate(appContext))
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit
                override fun onDone(utteranceId: String?) = Unit
                override fun onError(utteranceId: String?) = Unit
            })
        }
    }

    @Synchronized
    fun speak(text: String) {
        val cleanText = text.trim()
        if (!isReady || cleanText.isEmpty()) return

        textToSpeech?.setSpeechRate(PreferencesStore.speechRate(appContext))
        textToSpeech?.speak(
            cleanText,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "vozluma_${System.currentTimeMillis()}"
        )
    }

    @Synchronized
    fun stop() {
        textToSpeech?.stop()
    }

    @Synchronized
    fun shutdown() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        isReady = false
    }
}
