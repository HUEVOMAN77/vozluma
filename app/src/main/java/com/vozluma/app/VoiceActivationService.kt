package com.vozluma.app

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService

/**
 * Ubicación: app/src/main/java/com/vozluma/app/VoiceActivationService.kt
 * Escucha localmente la palabra «Hola» usando el modelo español incluido en assets.
 * Android muestra una notificación permanente mientras el micrófono está activo.
 */
class VoiceActivationService : Service(), RecognitionListener {
    private var model: Model? = null
    private var speechService: SpeechService? = null
    private lateinit var ttsManager: TTSManager
    private val handler = Handler(Looper.getMainLooper())
    private var waitingForCommand = false
    private var shuttingDown = false

    override fun onCreate() {
        super.onCreate()
        ttsManager = TTSManager(this)
        createNotificationChannel()
        startMicrophoneForegroundNotification()
        loadLocalModel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!PreferencesStore.isVoiceActivationEnabled(this) || !hasRecordAudioPermission()) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        shuttingDown = true
        handler.removeCallbacksAndMessages(null)
        stopListening()
        model?.close()
        model = null
        if (::ttsManager.isInitialized) ttsManager.shutdown()
        super.onDestroy()
    }

    override fun onPartialResult(hypothesis: String?) {
        val text = extractText(hypothesis, "partial") ?: return
        if (!waitingForCommand && containsWakeWord(text)) {
            beginConversation()
        }
    }

    override fun onResult(hypothesis: String?) {
        val text = extractText(hypothesis, "text") ?: return
        if (!waitingForCommand && containsWakeWord(text)) {
            beginConversation()
        } else if (waitingForCommand && text.isNotBlank()) {
            handleCommand(text)
        }
    }

    override fun onFinalResult(hypothesis: String?) {
        // SpeechService vuelve a entregar resultados parciales y finales automáticamente.
    }

    override fun onError(exception: Exception?) {
        if (shuttingDown) return
        restartListeningWithDelay(1_000L)
    }

    override fun onTimeout() {
        if (shuttingDown) return
        restartListeningWithDelay(300L)
    }

    private fun loadLocalModel() {
        StorageService.unpack(
            this,
            "model-es",
            "model-es",
            { unpackedModel ->
                if (shuttingDown) {
                    unpackedModel.close()
                    return@unpack
                }
                model = unpackedModel
                startListening()
            },
            { exception ->
                ttsManager.speak("No pude cargar el modelo de voz local")
                stopSelf()
            }
        )
    }

    private fun startListening() {
        if (shuttingDown || speechService != null || model == null) return
        try {
            val recognizer = Recognizer(model, SAMPLE_RATE)
            speechService = SpeechService(recognizer, SAMPLE_RATE)
            speechService?.startListening(this)
        } catch (_: Exception) {
            restartListeningWithDelay(1_500L)
        }
    }

    private fun stopListening() {
        speechService?.stop()
        speechService?.shutdown()
        speechService = null
    }

    private fun beginConversation() {
        if (waitingForCommand || shuttingDown) return
        waitingForCommand = true
        stopListening()
        ttsManager.speak("Hola, ¿en qué puedo ayudarte?")
        restartListeningWithDelay(GREETING_DELAY_MS)
    }

    private fun handleCommand(command: String) {
        waitingForCommand = false
        stopListening()
        val normalized = command.lowercase()
        val answer = when {
            normalized.contains("última notificación") || normalized.contains("ultima notificacion") ||
                normalized.contains("lee las notificaciones") || normalized.contains("lee mis notificaciones") ->
                latestNotificationAnswer()
            normalized.contains("qué hora es") || normalized.contains("que hora es") ->
                "Son las ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}"
            normalized.contains("silencia") || normalized.contains("modo silencio") -> {
                PreferencesStore.setAssistantEnabled(this, false)
                "Listo, silencié el asistente"
            }
            normalized.contains("activa") || normalized.contains("enciende") -> {
                PreferencesStore.setAssistantEnabled(this, true)
                "Listo, activé el asistente"
            }
            normalized.contains("desactiva") || normalized.contains("apaga") -> {
                PreferencesStore.setAssistantEnabled(this, false)
                "Listo, desactivé el asistente"
            }
            normalized.contains("modo coche") || normalized.contains("modo auto") -> {
                PreferencesStore.setCarModeEnabled(this, true)
                "Activé el modo coche"
            }
            normalized.contains("estado") -> {
                if (PreferencesStore.isAssistantEnabled(this)) "El asistente está encendido" else "El asistente está apagado"
            }
            normalized.contains("qué puedes hacer") || normalized.contains("que puedes hacer") ->
                "Puedo anunciar tus notificaciones y llamadas, leer la última notificación y activar o desactivar el asistente"
            else -> "Todavía estoy aprendiendo. Puedo ayudarte con tus notificaciones, llamadas y ajustes de voz"
        }
        ttsManager.speak(answer)
        restartListeningWithDelay(COMMAND_DELAY_MS)
    }

    private fun latestNotificationAnswer(): String {
        val latest = HistoryStore.getAll(this).firstOrNull()
            ?: return "Todavía no tengo notificaciones guardadas"
        return "La última notificación fue de ${latest.sender} en ${latest.appName}: ${latest.message}"
    }

    private fun restartListeningWithDelay(delayMillis: Long) {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            if (!shuttingDown) startListening()
        }, delayMillis)
    }

    private fun extractText(json: String?, key: String): String? = try {
        json?.let { JSONObject(it).optString(key).trim() }?.takeIf { it.isNotEmpty() }
    } catch (_: Exception) {
        null
    }

    private fun containsWakeWord(text: String): Boolean =
        text.lowercase().split(Regex("\\s+")).any { it.trim(',', '.', '!', '?', '¿', '¡') == WAKE_WORD }

    private fun hasRecordAudioPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
            checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun startMicrophoneForegroundNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_vozluma)
            .setContentTitle("VozLuma está escuchando")
            .setContentText("Di «Hola» para hablar con VozLuma")
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Activación por voz",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Indica que VozLuma está escuchando la palabra de activación"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_START = "com.vozluma.app.action.START_VOICE"
        const val ACTION_STOP = "com.vozluma.app.action.STOP_VOICE"
        private const val CHANNEL_ID = "vozluma_voice_activation"
        private const val NOTIFICATION_ID = 1201
        private const val SAMPLE_RATE = 16_000.0f
        private const val WAKE_WORD = "hola"
        private const val GREETING_DELAY_MS = 1_800L
        private const val COMMAND_DELAY_MS = 3_000L
    }
}
