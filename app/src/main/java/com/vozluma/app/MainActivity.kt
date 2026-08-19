package com.vozluma.app

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlin.math.roundToInt

/**
 * Ubicación: app/src/main/java/com/vozluma/app/MainActivity.kt
 * Pantalla de control completa del asistente VozLuma.
 */
class MainActivity : AppCompatActivity() {
    private lateinit var assistantSwitch: SwitchMaterial
    private lateinit var voiceActivationSwitch: SwitchMaterial
    private lateinit var accessStatusText: TextView
    private lateinit var quietHoursText: TextView
    private lateinit var modelStatusText: TextView
    private lateinit var modelProgress: ProgressBar
    private lateinit var modelDownloadButton: Button
    private lateinit var ttsManager: TTSManager
    private var waitingToStartVoice = false

    override fun onCreate(savedInstanceState: Bundle?) {
        applySavedTheme()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root_content)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(view.paddingLeft, systemBars.top + 8, view.paddingRight, systemBars.bottom + 28)
            insets
        }

        assistantSwitch = findViewById(R.id.switch_assistant)
        voiceActivationSwitch = findViewById(R.id.switch_voice_activation)
        accessStatusText = findViewById(R.id.text_access_status)
        quietHoursText = findViewById(R.id.text_quiet_hours)
        modelStatusText = findViewById(R.id.text_model_status)
        modelProgress = findViewById(R.id.progress_model_download)
        modelDownloadButton = findViewById(R.id.button_download_model)
        ttsManager = TTSManager(this)

        configurePrimarySwitches()
        configureFeatureSwitches()
        configureAppSwitches()
        configureButtons()
        configureModelDownload()
        requestRuntimePermissionsIfNeeded()
        updateStatusText()
        updateDeviceInsights()
    }

    override fun onResume() {
        super.onResume()
        updateStatusText()
        updateDeviceInsights()
        updateModelStatus()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != VOICE_PERMISSIONS_REQUEST_CODE) return

        val recordAudioGranted = checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (waitingToStartVoice && recordAudioGranted) {
            startVoiceActivationService()
        } else if (waitingToStartVoice) {
            voiceActivationSwitch.isChecked = false
            Toast.makeText(this, R.string.microphone_permission_required, Toast.LENGTH_LONG).show()
        }
        waitingToStartVoice = false
    }

    override fun onDestroy() {
        ttsManager.shutdown()
        super.onDestroy()
    }

    private fun configurePrimarySwitches() {
        assistantSwitch.isChecked = PreferencesStore.isAssistantEnabled(this)
        assistantSwitch.setOnCheckedChangeListener { _, isChecked ->
            PreferencesStore.setAssistantEnabled(this, isChecked)
            updateStatusText()
        }

        val voiceEnabled = PreferencesStore.isVoiceActivationEnabled(this) && ModelManager.isReady(this)
        if (!voiceEnabled) PreferencesStore.setVoiceActivationEnabled(this, false)
        voiceActivationSwitch.isChecked = voiceEnabled
        voiceActivationSwitch.setOnCheckedChangeListener { _, isChecked ->
            PreferencesStore.setVoiceActivationEnabled(this, isChecked)
            if (isChecked && !ModelManager.isReady(this)) {
                PreferencesStore.setVoiceActivationEnabled(this, false)
                voiceActivationSwitch.isChecked = false
                downloadModel()
                return@setOnCheckedChangeListener
            }
            if (isChecked) requestVoicePermissionAndStart() else stopVoiceActivationService()
            updateStatusText()
        }
    }

    private fun configureFeatureSwitches() {
        configureSwitch(R.id.switch_smart_filters, PreferencesStore.areSmartFiltersEnabled(this)) {
            PreferencesStore.setSmartFiltersEnabled(this, it)
        }
        configureSwitch(R.id.switch_only_headphones, PreferencesStore.onlyWithHeadphones(this)) {
            PreferencesStore.setOnlyWithHeadphones(this, it)
        }
        configureSwitch(R.id.switch_car_mode, PreferencesStore.isCarModeEnabled(this)) {
            PreferencesStore.setCarModeEnabled(this, it)
        }
        configureSwitch(R.id.switch_history, PreferencesStore.isHistoryEnabled(this)) {
            PreferencesStore.setHistoryEnabled(this, it)
        }
        configureSwitch(R.id.switch_quiet_hours, PreferencesStore.isQuietHoursEnabled(this)) {
            PreferencesStore.setQuietHoursEnabled(this, it)
            updateQuietHoursText()
        }
        val speechRateSeekBar = findViewById<SeekBar>(R.id.seek_speech_rate)
        speechRateSeekBar.progress = (((PreferencesStore.speechRate(this) - 0.6f) / 0.8f) * 80f)
            .roundToInt().coerceIn(0, 80)
        updateSpeechRateLabel(speechRateSeekBar.progress)
        speechRateSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                PreferencesStore.setSpeechRate(this@MainActivity, 0.6f + progress / 80f * 0.8f)
                updateSpeechRateLabel(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })

        val darkThemeSwitch = findViewById<SwitchMaterial>(R.id.switch_dark_theme)
        darkThemeSwitch.isChecked = PreferencesStore.themeMode(this) == 2
        darkThemeSwitch.setOnCheckedChangeListener { _, isChecked ->
            PreferencesStore.setThemeMode(this, if (isChecked) 2 else 1)
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }
        updateQuietHoursText()
    }

    private fun configureAppSwitches() {
        configureSwitch(R.id.switch_whatsapp, PreferencesStore.isPackageEnabled(this, "com.whatsapp")) {
            PreferencesStore.setPackageEnabled(this, "com.whatsapp", it)
        }
        configureSwitch(R.id.switch_messenger, PreferencesStore.isPackageEnabled(this, "com.facebook.orca")) {
            PreferencesStore.setPackageEnabled(this, "com.facebook.orca", it)
        }
        configureSwitch(R.id.switch_facebook, PreferencesStore.isPackageEnabled(this, "com.facebook.katana")) {
            PreferencesStore.setPackageEnabled(this, "com.facebook.katana", it)
        }
        configureSwitch(R.id.switch_instagram, PreferencesStore.isPackageEnabled(this, "com.instagram.android")) {
            PreferencesStore.setPackageEnabled(this, "com.instagram.android", it)
        }
        configureSwitch(R.id.switch_google_messages, PreferencesStore.isPackageEnabled(this, "com.google.android.apps.messaging")) {
            PreferencesStore.setPackageEnabled(this, "com.google.android.apps.messaging", it)
        }
        configureSwitch(R.id.switch_sms, PreferencesStore.isPackageEnabled(this, "com.android.mms")) {
            PreferencesStore.setPackageEnabled(this, "com.android.mms", it)
        }
        configureSwitch(R.id.switch_samsung_messages, PreferencesStore.isPackageEnabled(this, "com.samsung.android.messaging")) {
            PreferencesStore.setPackageEnabled(this, "com.samsung.android.messaging", it)
        }
    }

    private fun configureModelDownload() {
        modelDownloadButton.setOnClickListener { downloadModel() }
        updateModelStatus()
    }

    private fun updateModelStatus() {
        if (ModelManager.isReady(this)) {
            modelStatusText.setText(R.string.model_ready)
            modelDownloadButton.isEnabled = false
            modelProgress.visibility = View.GONE
        } else {
            modelStatusText.setText(R.string.model_card_description)
            modelDownloadButton.isEnabled = true
        }
    }

    private fun downloadModel() {
        if (ModelManager.isReady(this)) return
        modelDownloadButton.isEnabled = false
        modelProgress.progress = 0
        modelProgress.visibility = View.VISIBLE
        ModelManager.download(
            this,
            onProgress = { progress ->
                runOnUiThread {
                    modelProgress.progress = progress
                    modelStatusText.text = getString(R.string.model_downloading, progress)
                }
            },
            onComplete = { result ->
                runOnUiThread {
                    modelProgress.visibility = View.GONE
                    if (result.isSuccess) {
                        modelStatusText.setText(R.string.model_ready)
                        modelDownloadButton.isEnabled = false
                    } else {
                        val detail = result.exceptionOrNull()?.message?.takeIf { it.isNotBlank() }
                            ?: getString(R.string.model_download_error)
                        modelStatusText.text = getString(R.string.model_download_error_details, detail)
                        modelDownloadButton.isEnabled = true
                    }
                }
            }
        )
    }

    private fun configureButtons() {
        findViewById<Button>(R.id.button_notification_access).setOnClickListener {
            openSystemSettingsSafely(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        findViewById<Button>(R.id.button_test_voice).setOnClickListener {
            when {
                !assistantSwitch.isChecked -> ttsManager.speak("Activa primero el asistente VozLuma")
                !ModelManager.isReady(this) -> {
                    ttsManager.speak("Descarga primero el modelo español")
                    downloadModel()
                }
                !voiceActivationSwitch.isChecked -> {
                    voiceActivationSwitch.isChecked = true
                    Toast.makeText(this, R.string.voice_test_prompt, Toast.LENGTH_LONG).show()
                }
                else -> {
                    startVoiceActivationService()
                    Toast.makeText(this, R.string.voice_test_prompt, Toast.LENGTH_LONG).show()
                }
            }
        }
        findViewById<Button>(R.id.button_history).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
        findViewById<Button>(R.id.button_accessibility_settings).setOnClickListener {
            openSystemSettingsSafely(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        findViewById<Button>(R.id.button_custom_commands).setOnClickListener {
            startActivity(Intent(this, CustomCommandsActivity::class.java))
        }
        findViewById<Button>(R.id.button_privacy_center).setOnClickListener {
            startActivity(Intent(this, PrivacyActivity::class.java))
        }
        findViewById<Button>(R.id.button_priority_contacts).setOnClickListener {
            startActivity(Intent(this, PriorityContactsActivity::class.java))
        }
        findViewById<Button>(R.id.button_quiet_hours).setOnClickListener {
            showTimePicker(isStart = true)
        }
    }

    private fun configureSwitch(id: Int, checked: Boolean, onChanged: (Boolean) -> Unit) {
        findViewById<SwitchMaterial>(id).apply {
            isChecked = checked
            setOnCheckedChangeListener { _, value -> onChanged(value) }
        }
    }

    private fun requestVoicePermissionAndStart() {
        val missing = buildList {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                add(Manifest.permission.RECORD_AUDIO)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (missing.isEmpty()) {
            startVoiceActivationService()
        } else {
            waitingToStartVoice = true
            requestPermissions(missing.toTypedArray(), VOICE_PERMISSIONS_REQUEST_CODE)
        }
    }

    private fun startVoiceActivationService() {
        if (!PreferencesStore.isAssistantEnabled(this)) {
            assistantSwitch.isChecked = true
            PreferencesStore.setAssistantEnabled(this, true)
        }
        val serviceIntent = Intent(this, VoiceActivationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(serviceIntent)
        else startService(serviceIntent)
        Toast.makeText(this, R.string.voice_activation_started, Toast.LENGTH_SHORT).show()
    }

    private fun stopVoiceActivationService() {
        stopService(Intent(this, VoiceActivationService::class.java))
        Toast.makeText(this, R.string.voice_activation_stopped, Toast.LENGTH_SHORT).show()
    }

    private fun updateStatusText() {
        val notificationAccessGranted = NotificationManagerCompat
            .getEnabledListenerPackages(this)
            .contains(packageName)
        val assistantState = if (PreferencesStore.isAssistantEnabled(this)) {
            getString(R.string.status_assistant_on)
        } else getString(R.string.status_assistant_off)
        val accessState = if (notificationAccessGranted) {
            getString(R.string.status_access_granted)
        } else getString(R.string.status_access_missing)
        val voiceState = if (PreferencesStore.isVoiceActivationEnabled(this)) {
            getString(R.string.status_voice_on)
        } else getString(R.string.status_voice_off)
        val modelState = if (ModelManager.isReady(this)) "modelo instalado" else "modelo pendiente"
        val diagnostics = VoiceDiagnosticsStore.message(this)
        accessStatusText.text = "$assistantState\n$accessState\n$voiceState\n$modelState\n${getString(R.string.voice_diagnostics, diagnostics)}"
    }

    private fun updateDeviceInsights() {
        val batteryManager = getSystemService(android.os.BatteryManager::class.java)
        val battery = batteryManager?.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
            ?.takeIf { it in 0..100 } ?: 0
        val stats = android.os.StatFs(filesDir.absolutePath)
        val freeGb = stats.availableBytes / (1024L * 1024L * 1024L)
        val modelState = if (ModelManager.isReady(this)) "instalado" else "pendiente"
        findViewById<TextView>(R.id.text_device_insights).text =
            getString(R.string.insights_value, battery, freeGb, modelState)
    }

    private fun updateSpeechRateLabel(progress: Int) {
        val percentage = (60 + progress).coerceIn(60, 140)
        findViewById<TextView>(R.id.text_speech_rate).text =
            getString(R.string.speech_rate_label, percentage)
    }

    private fun updateQuietHoursText() {
        val enabled = PreferencesStore.isQuietHoursEnabled(this)
        quietHoursText.text = if (enabled) {
            getString(
                R.string.quiet_hours_value,
                PreferencesStore.getQuietStart(this),
                PreferencesStore.getQuietEnd(this)
            )
        } else getString(R.string.quiet_hours_disabled)
    }

    private fun showTimePicker(isStart: Boolean) {
        val current = if (isStart) PreferencesStore.getQuietStart(this) else PreferencesStore.getQuietEnd(this)
        val parts = current.split(":").map { it.toIntOrNull() ?: 0 }
        TimePickerDialog(this, { _, hour, minute ->
            val value = "%02d:%02d".format(hour, minute)
            if (isStart) {
                PreferencesStore.setQuietHours(this, value, PreferencesStore.getQuietEnd(this))
                showTimePicker(isStart = false)
            } else {
                PreferencesStore.setQuietHours(this, PreferencesStore.getQuietStart(this), value)
                updateQuietHoursText()
            }
        }, parts.getOrElse(0) { 22 }, parts.getOrElse(1) { 0 }, true).apply {
            setTitle(if (isStart) R.string.quiet_start_title else R.string.quiet_end_title)
            show()
        }
    }

    private fun requestRuntimePermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val permissionsToRequest = buildList {
            if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                add(Manifest.permission.READ_PHONE_STATE)
            }
            if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                add(Manifest.permission.READ_CONTACTS)
            }
        }
        if (permissionsToRequest.isNotEmpty()) requestPermissions(
            permissionsToRequest.toTypedArray(), BASIC_PERMISSIONS_REQUEST_CODE
        )
    }

    private fun openSystemSettingsSafely(intent: Intent) {
        runCatching {
            if (intent.resolveActivity(packageManager) == null) error("Ajustes no disponibles")
            startActivity(intent)
        }.onFailure {
            Toast.makeText(this, R.string.settings_unavailable, Toast.LENGTH_LONG).show()
        }
    }

    private fun applySavedTheme() {
        when (PreferencesStore.themeMode(this)) {
            1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    companion object {
        private const val BASIC_PERMISSIONS_REQUEST_CODE = 1001
        private const val VOICE_PERMISSIONS_REQUEST_CODE = 2002
    }
}
