# Capacidades de la suite offline

Fecha: 2026-08-19.

## Intents oficiales

La guía oficial de Android documenta intents para alarmas, temporizadores, cámara, llamadas, SMS, mapas y música. Estos intents abren o preparan acciones en aplicaciones compatibles, pero no garantizan que una app de terceros acepte una operación interna específica. Fuente: https://developer.android.com/guide/components/intents-common

## Accesibilidad

Android permite crear un `AccessibilityService` que inspecciona contenido de pantalla y actúa en nombre del usuario, pero advierte que debe usarse para herramientas de asistencia general y requiere que el usuario lo active manualmente. La configuración debe declarar `BIND_ACCESSIBILITY_SERVICE`; `canRetrieveWindowContent` habilita leer la jerarquía y `canPerformGestures` habilita gestos. Fuente: https://developer.android.com/guide/topics/ui/accessibility/service

## Alcance técnico de la suite

Se pueden implementar offline y sin APIs: control de reproducción multimedia, intents de alarma/temporizador/cámara/mapas/llamadas/SMS, rutinas locales, perfiles y recordatorios. Las acciones que escriben o llaman deben tener confirmación. El control de interfaces arbitrarias de otras apps requiere accesibilidad y una activación manual del usuario; no debe activarse de forma oculta ni prometer funcionamiento universal.
