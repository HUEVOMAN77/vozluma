package com.vozluma.app

import android.os.Bundle
import android.widget.Button
import com.google.android.material.textfield.TextInputEditText
import androidx.appcompat.app.AppCompatActivity

/**
 * Ubicación: app/src/main/java/com/vozluma/app/PriorityContactsActivity.kt
 * Los contactos se guardan como nombres o fragmentos de nombre, solo localmente.
 */
class PriorityContactsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_priority_contacts)

        val input = findViewById<TextInputEditText>(R.id.input_priority_contacts)
        input.setText(PreferencesStore.priorityContacts(this).joinToString("\n"))

        findViewById<Button>(R.id.button_save_priority_contacts).setOnClickListener {
            val contacts = input.text?.toString()
                .orEmpty()
                .split(Regex("[,\\n]"))
                .map { it.trim() }
                .filter { it.length >= 2 }
                .toSet()
            PreferencesStore.setPriorityContacts(this, contacts)
            finish()
        }
    }
}
