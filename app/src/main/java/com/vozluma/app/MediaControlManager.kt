package com.vozluma.app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.view.KeyEvent

/**
 * Control multimedia universal de Android.
 * Funciona sin Internet cuando el reproductor expone una sesión multimedia activa.
 */
object MediaControlManager {
    private fun activeController(context: Context): MediaController? = try {
        val manager = context.getSystemService(MediaSessionManager::class.java) ?: return null
        val listener = ComponentName(context, NotificationService::class.java)
        manager.getActiveSessions(listener)
            .firstOrNull { it.playbackState?.state != android.media.session.PlaybackState.STATE_NONE }
    } catch (_: SecurityException) {
        null
    } catch (_: Exception) {
        null
    }

    fun play(context: Context): String {
        val controller = activeController(context)
        if (controller != null) {
            controller.transportControls.play()
            return "Reproduciendo la música"
        }
        if (dispatchMediaKey(context, KeyEvent.KEYCODE_MEDIA_PLAY)) {
            return "Envié la orden de reproducir al reproductor activo"
        }
        openMusicApp(context)
        return "Abrí el reproductor de música. Pulsa reproducir allí y luego podré controlarlo"
    }

    fun pause(context: Context): String {
        val controller = activeController(context)
        if (controller != null) {
            controller.transportControls.pause()
            return "Pausé la música"
        }
        return if (dispatchMediaKey(context, KeyEvent.KEYCODE_MEDIA_PAUSE)) "Pausé la música" else notReadyMessage()
    }

    fun stop(context: Context): String {
        val controller = activeController(context)
        if (controller != null) {
            controller.transportControls.stop()
            return "Detuve la música"
        }
        return if (dispatchMediaKey(context, KeyEvent.KEYCODE_MEDIA_STOP)) "Detuve la música" else notReadyMessage()
    }

    fun next(context: Context): String {
        val controller = activeController(context)
        if (controller != null) {
            controller.transportControls.skipToNext()
            return "Cambié a la siguiente canción"
        }
        return if (dispatchMediaKey(context, KeyEvent.KEYCODE_MEDIA_NEXT)) "Cambié a la siguiente canción" else notReadyMessage()
    }

    fun previous(context: Context): String {
        val controller = activeController(context)
        if (controller != null) {
            controller.transportControls.skipToPrevious()
            return "Volví a la canción anterior"
        }
        return if (dispatchMediaKey(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS)) "Volví a la canción anterior" else notReadyMessage()
    }

    fun volume(context: Context, increase: Boolean): String {
        val audio = context.getSystemService(AudioManager::class.java)
            ?: return "No pude controlar el volumen"
        val direction = if (increase) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
        audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
        return if (increase) "Subí el volumen" else "Bajé el volumen"
    }

    fun openMusicApp(context: Context) {
        val intent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MUSIC)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    private fun dispatchMediaKey(context: Context, keyCode: Int): Boolean = runCatching {
        val audio = context.getSystemService(AudioManager::class.java) ?: return false
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        true
    }.getOrDefault(false)

    private fun notReadyMessage(): String =
        "No encontré un reproductor activo. Abre tu reproductor de música y vuelve a intentarlo"
}
