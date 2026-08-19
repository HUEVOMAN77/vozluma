package com.vozluma.app

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.app.NotificationCompat
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService

/**
 * Ubicación: app/src/main/java/com/vozluma/app/VoiceActivationService.kt
 * Escucha localmente la palabra «Hola» usando el modelo español instalado en filesDir.
 * Android muestra una notificación permanente mientras el micrófono está activo.
 */
class VoiceActivationService : Service(), RecognitionListener {
    private var model: Model? = null
    private var speechService: SpeechService? = null
    private lateinit var ttsManager: TTSManager
    private val handler = Handler(Looper.getMainLooper())
    private var waitingForCommand = false
    private var waitingForReply = false
    private var conversationMode = false
    private var shuttingDown = false

    override fun onCreate() {
        super.onCreate()
        ttsManager = TTSManager(this)
        VoiceDiagnosticsStore.set(this, "iniciando")
        createNotificationChannel()
        startMicrophoneForegroundNotification()
        loadLocalModel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!PreferencesStore.isVoiceActivationEnabled(this)) {
            VoiceDiagnosticsStore.set(this, "apagado", "activa el interruptor de voz")
            stopSelf()
            return START_NOT_STICKY
        }
        if (!hasRecordAudioPermission()) {
            VoiceDiagnosticsStore.set(this, "sin permiso", "falta micrófono")
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
        VoiceDiagnosticsStore.set(this, "escuchando", text)
        if (!waitingForCommand && containsWakeWord(text)) {
            beginConversation()
        }
    }

    override fun onResult(hypothesis: String?) {
        val text = extractText(hypothesis, "text") ?: return
        VoiceDiagnosticsStore.set(this, "resultado", text)
        if (!waitingForCommand && containsWakeWord(text)) {
            beginConversation()
        } else if ((waitingForCommand || conversationMode) && text.isNotBlank()) {
            handleCommand(text)
        }
    }

    override fun onFinalResult(hypothesis: String?) {
        // SpeechService vuelve a entregar resultados parciales y finales automáticamente.
    }

    override fun onError(exception: Exception?) {
        if (shuttingDown) return
        VoiceDiagnosticsStore.set(this, "error de escucha", exception?.message ?: "reiniciando")
        restartListeningWithDelay(1_000L)
    }

    override fun onTimeout() {
        if (shuttingDown) return
        restartListeningWithDelay(300L)
    }

    private fun loadLocalModel() {
        if (!ModelManager.isReady(this)) {
            VoiceDiagnosticsStore.set(this, "modelo pendiente", "descárgalo desde la pantalla principal")
            ttsManager.speak("Primero descarga el modelo de voz desde la pantalla principal")
            stopSelf()
            return
        }
        try {
            model = Model(ModelManager.modelDirectory(this).absolutePath)
            VoiceDiagnosticsStore.set(this, "modelo listo", "iniciando micrófono")
            startListening()
        } catch (exception: Exception) {
            VoiceDiagnosticsStore.set(this, "error de modelo", exception.message ?: "modelo inválido")
            ttsManager.speak("No pude cargar el modelo de voz local")
            stopSelf()
        }
    }

    private fun startListening() {
        if (shuttingDown || speechService != null || model == null) return
        try {
            val recognizer = Recognizer(model, SAMPLE_RATE)
            speechService = SpeechService(recognizer, SAMPLE_RATE)
            speechService?.startListening(this)
            VoiceDiagnosticsStore.set(this, "micrófono activo", "di Hola")
        } catch (exception: Exception) {
            VoiceDiagnosticsStore.set(this, "error de micrófono", exception.message ?: "no se pudo iniciar")
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
        conversationMode = true
        stopListening()
        handler.removeCallbacksAndMessages(null)
        VoiceDiagnosticsStore.set(this, "Hola detectado", "respondiendo")
        ttsManager.speak("Hola, ¿en qué puedo ayudarte?")
        handler.postDelayed({ conversationMode = false }, CONVERSATION_TIMEOUT_MS)
        restartListeningWithDelay(GREETING_DELAY_MS)
    }

    private fun handleCommand(command: String) {
        waitingForCommand = false
        stopListening()
        val normalized = command.lowercase()
        if (waitingForReply) {
            waitingForReply = false
            ReplyDraftStore.save(this, command.trim())
            ttsManager.speak("Preparé tu respuesta. Revisa la pantalla y confirma antes de compartirla")
            startActivity(Intent(this, ReplyDraftActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            restartListeningWithDelay(COMMAND_DELAY_MS)
            return
        }
        val answer = CustomCommandStore.match(this, normalized) ?: when {
            normalized.startsWith("hola") || normalized.contains("buenos días") || normalized.contains("buenos dias") ||
                normalized.contains("buenas tardes") || normalized.contains("buenas noches") ->
                "Hola de nuevo. Estoy lista para ayudarte"
            normalized.contains("gracias") || normalized.contains("muchas gracias") ->
                "De nada. Estoy aquí para ayudarte"
            normalized.contains("quién eres") || normalized.contains("quien eres") || normalized.contains("qué eres") ||
                normalized.contains("que eres") ->
                "Soy VozLuma, tu asistente local. Puedo ayudarte con comandos, notificaciones, llamadas y tareas del teléfono"
            normalized.contains("resumen") || normalized.contains("cuántas notificaciones") ||
                normalized.contains("cuantas notificaciones") -> HistoryStore.summary(this)
            normalized.contains("responder") || normalized.contains("contestar") -> {
                waitingForReply = true
                "Claro. Dime el texto de la respuesta"
            }
            normalized.contains("última notificación") || normalized.contains("ultima notificacion") ||
                normalized.contains("lee las notificaciones") || normalized.contains("lee mis notificaciones") ->
                latestNotificationAnswer()
            normalized.contains("qué hora es") || normalized.contains("que hora es") ->
                "Son las ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}"
            normalized.contains("cómo estás") || normalized.contains("como estas") ->
                "Estoy funcionando y lista para ayudarte"
            normalized.contains("batería") || normalized.contains("bateria") -> batteryAnswer()
            normalized.contains("espacio libre") || normalized.contains("almacenamiento") -> storageAnswer()
            normalized.contains("qué teléfono tengo") || normalized.contains("que telefono tengo") ||
                normalized.contains("qué dispositivo tengo") || normalized.contains("que dispositivo tengo") -> deviceAnswer()
            normalized.contains("tengo internet") || normalized.contains("estoy conectado") ||
                normalized.contains("hay conexión") || normalized.contains("hay conexion") -> connectivityAnswer()
            normalized.contains("reproduc") || normalized.contains("pon música") || normalized.contains("pon musica") ->
                MediaControlManager.play(this)
            normalized.contains("pausa") || normalized.contains("pausar") -> MediaControlManager.pause(this)
            normalized.contains("detén la música") || normalized.contains("deten la musica") ||
                normalized.contains("detener música") || normalized.contains("detener musica") ->
                MediaControlManager.stop(this)
            normalized.contains("cambia la canción") || normalized.contains("cambia la cancion") ||
                normalized.contains("siguiente canción") || normalized.contains("siguiente cancion") ||
                normalized.contains("siguiente pista") || normalized == "siguiente" ->
                MediaControlManager.next(this)
            normalized.contains("canción anterior") || normalized.contains("cancion anterior") ||
                normalized.contains("pista anterior") || normalized == "anterior" ->
                MediaControlManager.previous(this)
            normalized.contains("sube el volumen") || normalized.contains("subir el volumen") ||
                normalized.contains("más volumen") || normalized.contains("mas volumen") ->
                MediaControlManager.volume(this, increase = true)
            normalized.contains("baja el volumen") || normalized.contains("bajar el volumen") ||
                normalized.contains("menos volumen") ->
                MediaControlManager.volume(this, increase = false)
            normalized.contains("abre el reproductor") || normalized.contains("abre la música") ||
                normalized.contains("abre la musica") -> {
                MediaControlManager.openMusicApp(this)
                "Abrí el reproductor de música"
            }
            normalized.contains("llama a") || normalized.contains("llamar a") -> callContact(command)
            normalized.contains("envía un mensaje") || normalized.contains("envia un mensaje") ||
                normalized.contains("manda un mensaje") || normalized.contains("manda un sms") -> prepareSms(command)
            normalized.contains("abre la cámara") || normalized.contains("abre la camara") ||
                normalized.contains("toma una foto") || normalized.contains("saca una foto") -> openCamera()
            normalized.contains("navega a") || normalized.contains("llévame a") || normalized.contains("llevame a") ||
                normalized.contains("abre el mapa") -> openMap(command)
            normalized.contains("crea una alarma") || normalized.contains("pon una alarma") -> createAlarm(command)
            normalized.contains("calendario") || normalized.contains("crea un evento") || normalized.contains("crear un evento") ->
                openCalendar(command)
            normalized.contains("lee la pantalla") || normalized.contains("qué hay en pantalla") ||
                normalized.contains("que hay en pantalla") -> readScreen()
            normalized == "volver" || normalized.contains("retrocede") -> globalAccessibilityAction("back")
            normalized.contains("ir al inicio") || normalized.contains("pantalla de inicio") -> globalAccessibilityAction("home")
            normalized.contains("abre las notificaciones") -> globalAccessibilityAction("notifications")
            normalized.contains("ajustes del teléfono") || normalized.contains("ajustes del telefono") -> {
                openSystemSettings(Settings.ACTION_SETTINGS)
                "Abrí los ajustes del teléfono"
            }
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
            normalized.contains("modo dormir") || normalized.contains("modo sueño") || normalized.contains("modo sueno") -> {
                PreferencesStore.setQuietHours(this, "22:00", "07:00")
                PreferencesStore.setQuietHoursEnabled(this, true)
                PreferencesStore.setCarModeEnabled(this, false)
                "Activé el modo dormir: reduciré los avisos entre las diez de la noche y las siete de la mañana"
            }
            normalized.contains("modo estudio") || normalized.contains("modo trabajo") -> {
                PreferencesStore.setQuietHours(this, "08:00", "18:00")
                PreferencesStore.setQuietHoursEnabled(this, true)
                PreferencesStore.setCarModeEnabled(this, false)
                "Activé el modo estudio con avisos reducidos durante el horario configurado"
            }
            normalized.contains("modo viaje") || normalized.contains("modo conducción") || normalized.contains("modo conduccion") -> {
                PreferencesStore.setCarModeEnabled(this, true)
                PreferencesStore.setQuietHoursEnabled(this, false)
                "Activé el modo viaje y coche"
            }
            normalized.contains("modo normal") || normalized.contains("modo casa") -> {
                PreferencesStore.setCarModeEnabled(this, false)
                PreferencesStore.setQuietHoursEnabled(this, false)
                "Volví al modo normal"
            }
            normalized.contains("modo coche") || normalized.contains("modo auto") -> {
                PreferencesStore.setCarModeEnabled(this, true)
                "Activé el modo coche"
            }
            normalized.contains("ajustes de sonido") || normalized.contains("configuración de sonido") -> {
                openSystemSettings(Settings.ACTION_SOUND_SETTINGS)
                "Abrí los ajustes de sonido"
            }
            normalized.contains("ajustes de bluetooth") || normalized.contains("configuración de bluetooth") -> {
                openSystemSettings(Settings.ACTION_BLUETOOTH_SETTINGS)
                "Abrí los ajustes de Bluetooth"
            }
            normalized.contains("temporizador") -> {
                val minutes = parseMinutes(normalized)
                if (minutes != null && minutes in 1..720) {
                    startActivity(Intent(AlarmClock.ACTION_SET_TIMER).apply {
                        putExtra(AlarmClock.EXTRA_LENGTH, minutes * 60)
                        putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                    "Preparé un temporizador de $minutes minutos"
                } else {
                    "Dime el número de minutos, por ejemplo: temporizador de diez minutos"
                }
            }
            normalized.contains("estado") -> {
                if (PreferencesStore.isAssistantEnabled(this)) "El asistente está encendido" else "El asistente está apagado"
            }
            normalized.contains("cuánto es") || normalized.contains("cuanto es") || normalized.contains("calcula") ||
                normalized.contains("calcular") -> calculateAnswer(normalized)
            normalized.contains("qué día es") || normalized.contains("que dia es") ||
                normalized.contains("qué fecha es") || normalized.contains("que fecha es") ->
                dateAnswer()
            normalized.contains("recordatorio") || normalized.contains("recuérdame") || normalized.contains("recuerdame") -> {
                PreferencesStore.setLastReminder(this, command)
                "Guardé este recordatorio en VozLuma: ${command.substringAfter("recordatorio", command).trim()}"
            }
            normalized.contains("borra el recordatorio") || normalized.contains("elimina el recordatorio") -> {
                PreferencesStore.clearLastReminder(this)
                "Eliminé el recordatorio local"
            }
            normalized.contains("qué tengo pendiente") || normalized.contains("que tengo pendiente") ||
                normalized.contains("mis recordatorios") -> PreferencesStore.lastReminder(this)
            normalized.contains("qué puedes hacer") || normalized.contains("que puedes hacer") ||
                normalized.contains("ayuda") || normalized.contains("comandos") -> helpAnswer()
            else -> "No tengo una respuesta general sin Internet, pero sí puedo ayudarte offline con comandos, notificaciones, llamadas, cálculos, recordatorios, temporizadores, ajustes y frases personalizadas. Di ayuda para escuchar la lista"
        }
        ttsManager.speak(answer)
        restartListeningWithDelay(COMMAND_DELAY_MS)
    }

    private fun callContact(command: String): String {
        val query = command.substringAfter("llama a", command.substringAfter("llamar a", "")).trim()
        if (query.isBlank()) return "Dime el nombre del contacto al que quieres llamar"
        val number = findContactNumber(query)
            ?: return "No encontré a $query en tus contactos o falta el permiso de contactos"
        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(number)}")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
        return "Abrí el marcador para llamar a $query. Revisa el número y confirma la llamada"
    }

    private fun prepareSms(command: String): String {
        val normalized = command.lowercase()
        val recipientText = command.substringAfter(" a ", "")
            .substringBefore(" diciendo ")
            .substringBefore(" que diga ")
            .trim()
        val body = when {
            normalized.contains(" diciendo ") -> command.substringAfter(" diciendo ").trim()
            normalized.contains(" que diga ") -> command.substringAfter(" que diga ").trim()
            else -> ""
        }
        if (recipientText.isBlank() || body.isBlank()) {
            return "Di: envía un mensaje a mamá diciendo llego en diez minutos"
        }
        val number = findContactNumber(recipientText)
            ?: return "No encontré a $recipientText en tus contactos o falta el permiso de contactos"
        startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(number)}")).apply {
            putExtra("sms_body", body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
        return "Preparé el mensaje para $recipientText. Revísalo y pulsa enviar"
    }

    private fun findContactNumber(query: String): String? {
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) return null
        val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val args = arrayOf("%${query.trim()}%")
        return contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            selection,
            args,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }

    private fun openCamera(): String {
        return try {
            startActivity(Intent(MediaStore.ACTION_IMAGE_CAPTURE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            "Abrí la cámara"
        } catch (_: Exception) {
            "No encontré una aplicación de cámara"
        }
    }

    private fun openMap(command: String): String {
        val destination = command.substringAfter("navega a", command.substringAfter("llévame a", command.substringAfter("llevame a", ""))).trim()
        if (destination.isBlank()) return "Dime a qué lugar quieres navegar"
        return try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(destination)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            "Abrí el mapa para $destination"
        } catch (_: Exception) {
            "No encontré una aplicación de mapas"
        }
    }

    private fun createAlarm(command: String): String {
        val match = Regex("(\\d{1,2})(?:[:.](\\d{1,2}))?").find(command)
            ?: return "Dime la hora, por ejemplo: crea una alarma a las 7:30"
        val hour = match.groupValues[1].toIntOrNull() ?: return "No entendí la hora"
        val minute = match.groupValues.getOrNull(2)?.toIntOrNull() ?: 0
        if (hour !in 0..23 || minute !in 0..59) return "La hora no es válida"
        startActivity(Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
        return "Preparé una alarma para las %02d:%02d. Confirma en la aplicación de reloj".format(hour, minute)
    }

    private fun openCalendar(command: String): String {
        val title = command.substringAfter("evento", "").trim().ifBlank { "Evento de VozLuma" }
        return try {
            startActivity(Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI).apply {
                putExtra(CalendarContract.Events.TITLE, title)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            "Abrí el calendario para preparar el evento. Revísalo y guárdalo"
        } catch (_: Exception) {
            "No encontré una aplicación de calendario"
        }
    }

    private fun readScreen(): String =
        VozLumaAccessibilityService.current?.readVisibleScreen()
            ?: "Activa primero el control avanzado de pantalla desde los ajustes de accesibilidad"

    private fun globalAccessibilityAction(action: String): String {
        val service = VozLumaAccessibilityService.current
            ?: return "Activa primero el control avanzado de pantalla desde los ajustes de accesibilidad"
        val performed = when (action) {
            "back" -> service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
            "home" -> service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME)
            else -> service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
        }
        return if (performed) "Listo" else "Android no permitió esa acción"
    }

    private fun calculateAnswer(text: String): String {
        val expression = text.substringAfter("cuánto es", text.substringAfter("cuanto es", text.substringAfter("calcula", text.substringAfter("calcular", ""))))
            .replace("por", "*")
            .replace("x", "*")
            .replace("más", "+")
            .replace("mas", "+")
            .replace("menos", "-")
            .replace("dividido entre", "/")
            .replace("dividido por", "/")
            .replace(Regex("[^0-9+*/.\\-]"), "")
        return runCatching {
            val numbers = Regex("(-?\\d+(?:\\.\\d+)?)").findAll(expression).map { it.value.toDouble() }.toList()
            val operator = Regex("[+*/-]").find(expression)?.value
            if (numbers.size < 2 || operator == null) return@runCatching "Puedo calcular operaciones sencillas como 12 más 8"
            val result = when (operator) {
                "+" -> numbers[0] + numbers[1]
                "-" -> numbers[0] - numbers[1]
                "*" -> numbers[0] * numbers[1]
                "/" -> if (numbers[1] == 0.0) return@runCatching "No se puede dividir por cero" else numbers[0] / numbers[1]
                else -> return@runCatching "Puedo calcular operaciones sencillas"
            }
            "El resultado es ${"%.2f".format(java.util.Locale.getDefault(), result).trimEnd('0').trimEnd('.') }"
        }.getOrElse { "Puedo calcular operaciones sencillas como 12 más 8" }
    }

    private fun dateAnswer(): String =
        "Hoy es ${java.text.SimpleDateFormat("EEEE d 'de' MMMM", java.util.Locale("es", "ES")).format(java.util.Date())}"

    private fun batteryAnswer(): String {
        val manager = getSystemService(android.os.BatteryManager::class.java)
        val percentage = manager?.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
            ?.takeIf { it in 0..100 } ?: return "No pude consultar la batería"
        return "La batería está al $percentage por ciento"
    }

    private fun storageAnswer(): String {
        val stats = android.os.StatFs(filesDir.absolutePath)
        val freeGb = stats.availableBytes / (1024L * 1024L * 1024L)
        return "Tienes aproximadamente $freeGb gigabytes libres"
    }

    private fun deviceAnswer(): String =
        "Estás usando un ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"

    private fun connectivityAnswer(): String {
        val manager = getSystemService(android.net.ConnectivityManager::class.java)
        val connected = manager?.activeNetwork != null
        return if (connected) "El teléfono indica que tiene una conexión activa" else "No detecto una conexión activa"
    }

    private fun helpAnswer(): String =
        "Puedo controlar música, volumen, llamadas y mensajes con confirmación, abrir cámara y mapas, crear alarmas y eventos, leer la pantalla si activas accesibilidad, cambiar rutinas y responder a tus comandos personalizados"

    private fun parseMinutes(text: String): Int? {
        Regex("\\d+").find(text)?.value?.toIntOrNull()?.let { return it }
        return mapOf(
            "uno" to 1, "dos" to 2, "tres" to 3, "cinco" to 5,
            "diez" to 10, "quince" to 15, "veinte" to 20,
            "treinta" to 30, "cuarenta" to 40, "sesenta" to 60
        ).entries.firstOrNull { text.contains(it.key) }?.value
    }

    private fun openSystemSettings(action: String) {
        startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
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

    private fun containsWakeWord(text: String): Boolean {
        val normalized = text.lowercase()
            .replace('á', 'a')
            .replace('é', 'e')
            .replace('í', 'i')
            .replace('ó', 'o')
            .replace('ú', 'u')
        return Regex("(^|\\s)hola($|\\s|[,.!?¿¡])").containsMatchIn(normalized)
    }

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
        private const val CONVERSATION_TIMEOUT_MS = 20_000L
    }
}
