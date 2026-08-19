# Alcance del asistente offline

Fecha: 2026-08-19.

## Control de reproducción

La documentación oficial de Android indica que MediaSession es el mecanismo universal para que un reproductor exponga controles y reciba comandos externos. Un MediaController puede enviar comandos a una sesión activa; esto permite play, pause, siguiente y anterior cuando el reproductor expone correctamente una MediaSession. Fuente: https://developer.android.com/media/media3/session/control-playback

Consecuencia para VozLuma: se puede implementar control multimedia offline con `MediaSessionManager`/`MediaController` y comandos de transporte. El control universal depende de que el reproductor tenga una sesión activa y acepte esos comandos. Cambiar a una canción concreta, buscar dentro de una biblioteca o iniciar una app específica puede requerir integración propia del reproductor o intents de esa aplicación.

## Asistente del sistema

La API oficial `VoiceInteractionService` es el servicio principal de un interactuador de voz global y puede dar soporte a hotwording e interacciones. Para actuar como asistente predeterminado, la app debe implementar y registrar ese servicio y el usuario debe seleccionarlo en la configuración de asistentes. Fuente: https://developer.android.com/reference/android/service/voice/VoiceInteractionService

Consecuencia para VozLuma: se puede avanzar desde un servicio de micrófono propio hacia el rol de asistente predeterminado, pero es una integración de Android con permisos y configuración explícita. No se debe prometer control absoluto de todas las aplicaciones: Android y cada app limitan qué acciones se pueden ejecutar.

## Alcance recomendado

1. Implementar control multimedia offline: reproducir/pausar, siguiente/anterior, detener, subir/bajar volumen y abrir controles multimedia.
2. Añadir intents seguros para abrir reproductor, ajustes, temporizador, calendario y cámara cuando existan en el dispositivo.
3. Mantener comandos locales y conversaciones de contexto limitado.
4. Añadir modo asistente predeterminado opcional en una fase posterior, con explicación clara de permisos.
5. Para acciones en aplicaciones que no exponen MediaSession o intents públicos, no simular éxito; mostrar que la acción no está disponible para ese reproductor.
