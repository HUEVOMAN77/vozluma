package com.vozluma.app

import android.os.Bundle
import android.text.format.DateFormat
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Date

/**
 * Ubicación: app/src/main/java/com/vozluma/app/HistoryActivity.kt
 * Consulta exclusivamente el historial guardado en el almacenamiento local.
 */
class HistoryActivity : AppCompatActivity() {
    private lateinit var entriesContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        entriesContainer = findViewById(R.id.history_entries)
        findViewById<Button>(R.id.button_clear_history).setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.clear_history_title)
                .setMessage(R.string.clear_history_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.clear_history) { _, _ ->
                    HistoryStore.clear(this)
                    renderHistory()
                }
                .show()
        }
        renderHistory()
    }

    private fun renderHistory() {
        entriesContainer.removeAllViews()
        val entries = HistoryStore.getAll(this)
        if (entries.isEmpty()) {
            entriesContainer.addView(TextView(this).apply {
                setText(R.string.history_empty)
                setTextColor(getColor(R.color.text_secondary))
                textSize = 16f
                setPadding(0, 24, 0, 24)
            })
            return
        }

        entries.forEach { entry ->
            entriesContainer.addView(TextView(this).apply {
                text = "${entry.sender} · ${entry.appName}\n${entry.message}\n${DateFormat.format("dd/MM/yyyy HH:mm", Date(entry.timestamp))}"
                setTextColor(getColor(R.color.text_primary))
                textSize = 15f
                setPadding(0, 14, 0, 14)
            })
        }
    }
}
