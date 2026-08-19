package com.vozluma.app

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.text.TextUtils
import java.util.LinkedHashMap
import java.util.Locale

/**
 * Ubicación: app/src/main/java/com/vozluma/app/NotificationService.kt
 * Servicio autorizado por el usuario desde Ajustes > Acceso a notificaciones.
 */
class NotificationService : NotificationListenerService() {
    private lateinit var ttsManager: TTSManager

    private val recentlyRead = object : LinkedHashMap<String, Long>(100, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?): Boolean =
            size > MAX_RECENT_ITEMS
    }

    override fun onCreate() {
        super.onCreate()
        ttsManager = TTSManager(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!PreferencesStore.isAssistantEnabled(this)) return
        if (sbn.packageName == packageName) return
        if (!isSupportedPackage(sbn.packageName)) return

        val notification = sbn.notification ?: return
        if (notification.flags and Notification.FLAG_ONGOING_EVENT != 0) return
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        if (notification.category == Notification.CATEGORY_PROGRESS ||
            notification.category == Notification.CATEGORY_SERVICE ||
            notification.category == Notification.CATEGORY_TRANSPORT) return

        val extras = notification.extras ?: return
        val title = firstNonBlank(
            extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
            extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()
        )
        val message = extractMessage(extras)
        if (message.isBlank()) return

        val normalizedMessage = message.replace(Regex("\\s+"), " ").trim()
        val deduplicationKey = listOf(sbn.packageName, sbn.tag, title, normalizedMessage).joinToString("|")
        if (isDuplicate(deduplicationKey)) return

        val applicationName = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(sbn.packageName, 0)
            ).toString()
        } catch (_: Exception) {
            friendlyApplicationName(sbn.packageName)
        }

        val sender = title.takeUnless { it.isNullOrBlank() } ?: "Alguien"
        ttsManager.speak("$sender en $applicationName dice: $normalizedMessage")
    }

    override fun onDestroy() {
        if (::ttsManager.isInitialized) ttsManager.shutdown()
        super.onDestroy()
    }

    private fun extractMessage(extras: android.os.Bundle): String {
        val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.joinToString(". ") { it.toString() }
            .orEmpty()
        return firstNonBlank(
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString(),
            lines,
            extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        ).orEmpty()
    }

    private fun isDuplicate(key: String): Boolean {
        val now = System.currentTimeMillis()
        synchronized(recentlyRead) {
            recentlyRead.entries.removeIf { now - it.value > DEDUPLICATION_WINDOW_MS }
            if (recentlyRead.containsKey(key)) return true
            recentlyRead[key] = now
            return false
        }
    }

    private fun isSupportedPackage(packageName: String): Boolean {
        val supportedPackages = setOf(
            "com.whatsapp",
            "com.facebook.orca",
            "com.facebook.katana",
            "com.instagram.android",
            "com.google.android.apps.messaging",
            "com.android.mms",
            "com.samsung.android.messaging"
        )
        return packageName in supportedPackages
    }

    private fun friendlyApplicationName(packageName: String): String = when (packageName) {
        "com.whatsapp" -> "WhatsApp"
        "com.facebook.orca" -> "Messenger"
        "com.facebook.katana" -> "Facebook"
        "com.instagram.android" -> "Instagram"
        else -> packageName.substringAfterLast('.').replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
    }

    private fun firstNonBlank(vararg values: String?): String? =
        values.firstOrNull { !it.isNullOrBlank() && !TextUtils.equals(it, "null") }

    companion object {
        private const val MAX_RECENT_ITEMS = 100
        private const val DEDUPLICATION_WINDOW_MS = 30_000L
    }
}
