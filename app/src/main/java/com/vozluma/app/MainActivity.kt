package com.vozluma.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import com.google.android.material.switchmaterial.SwitchMaterial

/**
 * Ubicación: app/src/main/java/com/vozluma/app/MainActivity.kt
 * Pantalla de control del asistente VozLuma.
 */
class MainActivity : AppCompatActivity() {
    private lateinit var assistantSwitch: SwitchMaterial
    private lateinit var accessStatusText: TextView
    private lateinit var ttsManager: TTSManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        assistantSwitch = findViewById(R.id.switch_assistant)
        accessStatusText = findViewById(R.id.text_access_status)
        ttsManager = TTSManager(this)

        assistantSwitch.isChecked = PreferencesStore.isAssistantEnabled(this)
        assistantSwitch.setOnCheckedChangeListener { _, isChecked ->
            PreferencesStore.setAssistantEnabled(this, isChecked)
            updateStatusText()
        }

        findViewById<Button>(R.id.button_notification_access).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        findViewById<Button>(R.id.button_test_voice).setOnClickListener {
            if (!assistantSwitch.isChecked) {
                ttsManager.speak("Activa primero el asistente VozLuma")
            } else {
                ttsManager.speak("Hola. VozLuma está funcionando correctamente")
            }
        }

        requestRuntimePermissionsIfNeeded()
        updateStatusText()
    }

    override fun onResume() {
        super.onResume()
        updateStatusText()
    }

    override fun onDestroy() {
        ttsManager.shutdown()
        super.onDestroy()
    }

    private fun updateStatusText() {
        val notificationAccessGranted = NotificationManagerCompat
            .getEnabledListenerPackages(this)
            .contains(packageName)

        val assistantState = if (PreferencesStore.isAssistantEnabled(this)) {
            getString(R.string.status_assistant_on)
        } else {
            getString(R.string.status_assistant_off)
        }

        val accessState = if (notificationAccessGranted) {
            getString(R.string.status_access_granted)
        } else {
            getString(R.string.status_access_missing)
        }

        accessStatusText.text = "$assistantState\n$accessState"
    }

    private fun requestRuntimePermissionsIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) return

        val permissionsToRequest = buildList {
            if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) !=
                PackageManager.PERMISSION_GRANTED
            ) add(Manifest.permission.READ_PHONE_STATE)
            if (checkSelfPermission(Manifest.permission.READ_CONTACTS) !=
                PackageManager.PERMISSION_GRANTED
            ) add(Manifest.permission.READ_CONTACTS)
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissions(permissionsToRequest.toTypedArray(), PERMISSIONS_REQUEST_CODE)
        }
    }

    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 1001
    }
}
