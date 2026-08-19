package com.vozluma.app

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText

class CustomCommandsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_commands)

        val phraseInput = findViewById<TextInputEditText>(R.id.input_custom_phrase)
        val responseInput = findViewById<TextInputEditText>(R.id.input_custom_response)
        val listText = findViewById<TextView>(R.id.text_custom_commands_list)

        fun refreshList() {
            val commands = CustomCommandStore.all(this)
            listText.text = if (commands.isEmpty()) {
                getString(R.string.custom_commands_empty)
            } else {
                commands.joinToString("\n") { "«${it.phrase}» → ${it.response}" }
            }
        }
        refreshList()

        findViewById<Button>(R.id.button_save_custom_command).setOnClickListener {
            val phrase = phraseInput.text?.toString().orEmpty()
            val response = responseInput.text?.toString().orEmpty()
            if (phrase.trim().length < 2 || response.isBlank()) {
                Toast.makeText(this, R.string.custom_command_required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            CustomCommandStore.save(this, phrase, response)
            phraseInput.text?.clear()
            responseInput.text?.clear()
            refreshList()
            Toast.makeText(this, R.string.custom_command_saved, Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.button_clear_custom_commands).setOnClickListener {
            CustomCommandStore.deleteAll(this)
            refreshList()
        }
    }
}
