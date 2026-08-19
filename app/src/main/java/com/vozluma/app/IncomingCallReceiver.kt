package com.vozluma.app

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.telephony.TelephonyManager

/**
 * Ubicación: app/src/main/java/com/vozluma/app/IncomingCallReceiver.kt
 * Anuncia el inicio de una llamada entrante sin contestarla ni grabarla.
 */
class IncomingCallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        if (!PreferencesStore.isAssistantEnabled(context)) return

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        if (state != TelephonyManager.EXTRA_STATE_RINGING) return

        val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
        val caller = resolveCaller(context, number)
        val pendingResult = goAsync()
        val tts = TTSManager(context)
        val handler = Handler(Looper.getMainLooper())

        handler.postDelayed({
            tts.speak("Llamada entrante de $caller")
            handler.postDelayed({
                tts.shutdown()
                pendingResult.finish()
            }, SPEECH_CLEANUP_DELAY_MS)
        }, TTS_INITIALIZATION_DELAY_MS)
    }

    private fun resolveCaller(context: Context, number: String?): String {
        val normalizedNumber = number?.trim().takeUnless { it.isNullOrEmpty() }
            ?: return "número desconocido"

        if (context.checkSelfPermission(Manifest.permission.READ_CONTACTS) !=
            PackageManager.PERMISSION_GRANTED
        ) return normalizedNumber

        val lookupUri = ContactsContract.PhoneLookup.CONTENT_FILTER_URI.buildUpon()
            .appendPath(normalizedNumber)
            .build()

        return context.contentResolver.query(
            lookupUri,
            arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getString(0).takeUnless { it.isNullOrBlank() } ?: normalizedNumber
            } else {
                normalizedNumber
            }
        } ?: normalizedNumber
    }

    companion object {
        private const val TTS_INITIALIZATION_DELAY_MS = 500L
        private const val SPEECH_CLEANUP_DELAY_MS = 6_000L
    }
}
