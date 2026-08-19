package com.vozluma.app

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Modo de asistencia opcional. Android exige que el usuario lo active manualmente en Ajustes.
 */
class VozLumaAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        current = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    fun readVisibleScreen(): String {
        val root = rootInActiveWindow ?: return "No puedo leer la pantalla actual"
        val output = StringBuilder()
        collectText(root, output)
        return output.toString().trim().takeIf { it.isNotBlank() }
            ?: "No encontré texto legible en la pantalla"
    }

    private fun collectText(node: AccessibilityNodeInfo, output: StringBuilder) {
        node.text?.toString()?.trim()?.takeIf { it.isNotBlank() }?.let {
            if (output.length < MAX_SCREEN_TEXT) output.append(it).append(". ")
        }
        node.contentDescription?.toString()?.trim()?.takeIf { it.isNotBlank() }?.let {
            if (output.length < MAX_SCREEN_TEXT) output.append(it).append(". ")
        }
        for (index in 0 until node.childCount) {
            node.getChild(index)?.let { child ->
                collectText(child, output)
            }
        }
    }

    companion object {
        private const val MAX_SCREEN_TEXT = 700
        @Volatile
        var current: VozLumaAccessibilityService? = null
            private set
    }

    override fun onDestroy() {
        if (current === this) current = null
        super.onDestroy()
    }
}
