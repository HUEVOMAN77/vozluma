package com.vozluma.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Ubicación: app/src/main/java/com/vozluma/app/BootReceiver.kt
 * NotificationListenerService se vuelve a enlazar por Android tras el arranque;
 * este receptor no inicia procesos en segundo plano ni consume batería.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Android restaura automáticamente el enlace del NotificationListenerService.
    }
}
