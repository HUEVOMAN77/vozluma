package com.vozluma.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import androidx.appcompat.app.AppCompatActivity

/**
 * Ubicación: app/src/main/java/com/vozluma/app/ReplyDraftActivity.kt
 * Nunca envía mensajes automáticamente: el usuario revisa y elige compartir.
 */
class ReplyDraftActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reply_draft)

        val input = findViewById<TextInputEditText>(R.id.input_reply_draft)
        input.setText(ReplyDraftStore.get(this))

        findViewById<Button>(R.id.button_share_draft).setOnClickListener {
            val text = input.text?.toString().orEmpty().trim()
            if (text.isNotBlank()) {
                ReplyDraftStore.save(this, text)
                val shareIntent = Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }, getString(R.string.share_reply_title))
                runCatching { startActivity(shareIntent) }
                    .onFailure {
                        Toast.makeText(
                            this,
                            R.string.share_reply_unavailable,
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
        }

        findViewById<Button>(R.id.button_discard_draft).setOnClickListener {
            ReplyDraftStore.clear(this)
            finish()
        }
    }
}
