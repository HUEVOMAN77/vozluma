package com.vozluma.app

import android.Manifest
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.RemoteViews
import androidx.core.content.ContextCompat

/**
 * Ubicación: app/src/main/java/com/vozluma/app/VozLumaWidgetProvider.kt
 * Widget simple para controlar la escucha sin abrir la aplicación completa.
 */
class VozLumaWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateWidget(context, manager, it) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_TOGGLE) return

        if (PreferencesStore.isVoiceActivationEnabled(context)) {
            PreferencesStore.setVoiceActivationEnabled(context, false)
            context.stopService(Intent(context, VoiceActivationService::class.java))
        } else if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            PreferencesStore.setAssistantEnabled(context, true)
            PreferencesStore.setVoiceActivationEnabled(context, true)
            val serviceIntent = Intent(context, VoiceActivationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } else {
            context.startActivity(Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
        updateAllWidgets(context)
    }

    private fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_vozluma)
        val enabled = PreferencesStore.isVoiceActivationEnabled(context)
        views.setTextViewText(
            R.id.widget_status,
            if (enabled) context.getString(R.string.widget_listening)
            else context.getString(R.string.widget_paused)
        )
        views.setTextViewText(
            R.id.widget_toggle,
            if (enabled) context.getString(R.string.widget_stop)
            else context.getString(R.string.widget_start)
        )
        val intent = Intent(context, VozLumaWidgetProvider::class.java).setAction(ACTION_TOGGLE)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            widgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_toggle, pendingIntent)
        manager.updateAppWidget(widgetId, views)
    }

    private fun updateAllWidgets(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, VozLumaWidgetProvider::class.java)
        val ids = manager.getAppWidgetIds(component)
        onUpdate(context, manager, ids)
    }

    companion object {
        private const val ACTION_TOGGLE = "com.vozluma.app.action.WIDGET_TOGGLE"
    }
}
