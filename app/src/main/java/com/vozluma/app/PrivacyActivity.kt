package com.vozluma.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.appcompat.app.AppCompatActivity

/**
 * Ubicación: app/src/main/java/com/vozluma/app/PrivacyActivity.kt
 * Centro de control de datos locales y permisos de VozLuma.
 */
class PrivacyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy)

        findViewById<TextView>(R.id.text_privacy_mic_status).text = if (
            checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        ) getString(R.string.privacy_mic_granted) else getString(R.string.privacy_mic_missing)

        findViewById<Button>(R.id.button_clear_all_local_data).setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.clear_all_local_data_title)
                .setMessage(R.string.clear_all_local_data_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.clear_all_local_data) { _, _ ->
                    HistoryStore.clear(this)
                    ReplyDraftStore.clear(this)
                    CustomCommandStore.deleteAll(this)
                    PreferencesStore.setPriorityContacts(this, emptySet())
                    PreferencesStore.clearLastReminder(this)
                    findViewById<TextView>(R.id.text_privacy_action_status)
                        .setText(R.string.privacy_data_cleared)
                }
                .show()
        }
        findViewById<Button>(R.id.button_open_app_settings).setOnClickListener {
            runCatching {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                })
            }.onFailure {
                Toast.makeText(this, R.string.settings_unavailable, Toast.LENGTH_LONG).show()
            }
        }
    }
}
