# VozLuma Premium

**VozLuma Premium 3.0.0** es un asistente Android offline en Kotlin con una interfaz premium, activación local por voz y controles de privacidad. Su objetivo es anunciar notificaciones y llamadas, permitir comandos hablados y mantener el procesamiento sensible en el propio teléfono.

## Experiencia principal

Di **«Hola»** mientras la activación por voz está encendida. VozLuma responderá **«Hola, ¿en qué puedo ayudarte?»** y escuchará una orden breve. El reconocimiento usa el modelo español local de Vosk, que se descarga una sola vez desde la app. No se requiere una cuenta de VozLuma ni una conexión a Internet durante la escucha.

La edición premium mantiene una separación segura entre comandos informativos y acciones sensibles. Puede preparar un borrador de respuesta dictada, pero no envía mensajes automáticamente: muestra una pantalla de revisión y el usuario decide si quiere editarlo y compartirlo mediante una aplicación del teléfono.

## Funciones premium

| Área | Funciones |
|---|---|
| Activación por voz | Palabra «Hola», respuesta inicial, modo conversación breve y reconocimiento offline en español. |
| Comandos | Hora, fecha, estado, salud, batería, almacenamiento, dispositivo, conectividad, cálculos, recordatorios locales, resúmenes, modo coche, temporizadores, ajustes de sonido/Bluetooth y ayuda. |
| Música offline | Reproducir, pausar, detener, siguiente, anterior, abrir reproductor y subir/bajar volumen mediante MediaSession y teclas multimedia del sistema. |
| Suite de teléfono | Llamadas con marcador y confirmación, SMS preparados para revisar, cámara, mapas, alarmas, calendario y ajustes. |
| Accesibilidad | Lectura de pantalla y acciones globales opcionales, siempre activadas manualmente por el usuario. |
| Rutinas | Modo dormir, estudio, trabajo, viaje, coche, casa y normal con preferencias locales. |
| Respuestas | Dictado local de un borrador, revisión, edición y selector de aplicación; nunca hay envío automático. |
| Notificaciones | WhatsApp, Messenger, Facebook, Instagram, Mensajes de Google, SMS y Mensajes Samsung, con selección individual, deduplicación y filtros inteligentes. |
| Prioridades | Contactos prioritarios que pueden superar el horario silencioso; alertas con palabras de emergencia también reciben tratamiento especial. |
| Llamadas | Anuncia el nombre del contacto cuando existe permiso y el número disponible cuando no hay coincidencia. |
| Perfiles y modos | Horario silencioso, modo coche, solo auriculares/Bluetooth, tema oscuro y velocidad de voz ajustable. |
| Privacidad | Centro de privacidad, historial local opcional, borrado de datos locales, permisos visibles y procesamiento offline. |
| Acceso rápido | Widget para pausar o activar la escucha cuando el micrófono ya tiene permiso. |
| Conversación offline | Después de «Hola», mantiene una ventana de conversación de 20 segundos para varias preguntas sin repetir la activación. |
| Personalización | Biblioteca para crear frases y respuestas personalizadas completamente locales. |
| Control del sistema | Intents seguros para abrir ajustes, temporizadores, reproductor de música y otras funciones disponibles en el teléfono. |
| Dashboard | Estado de permisos, estado de escucha, diagnóstico de voz, salud de batería, almacenamiento libre y estado del modelo offline. |

## Interfaz

La interfaz fue rediseñada con Material 3, tarjetas redondeadas, jerarquía visual, gradientes azul-violeta-cian, estados de escucha visibles, modo oscuro y adaptación edge-to-edge. Los controles importantes tienen texto y no dependen únicamente del color, para conservar legibilidad y accesibilidad. Android 15 aplica edge-to-edge automáticamente cuando una aplicación apunta a SDK 35, por lo que VozLuma aplica insets a su contenido desplazable [1].

## Modelo local

La app descarga una sola vez `vosk-model-small-es-0.42` desde la tarjeta **Modelo de voz offline**. El catálogo oficial de Vosk lo describe como un modelo español ligero para Android y Raspberry Pi, con un tamaño comprimido aproximado de 39 MB [2]. Después de la descarga, el reconocimiento funciona sin Internet y la APK inicial queda aproximadamente en 45 MB.

## Requisitos

| Componente | Versión |
|---|---|
| Android Studio | Hedgehog o posterior |
| JDK | 17 |
| Android Gradle Plugin | 8.6.1 |
| Kotlin | 2.0.21 |
| Vosk Android | 0.3.75 |
| Compile SDK | 35 |
| Android mínimo | API 26 / Android 8.0 |
| Application ID | `com.vozluma.premium` |
| Modelo inicial | Descarga única desde la app; Internet solo durante la configuración |
| Diagnóstico | Estado visible de modelo, micrófono, escucha y detección de «Hola» |

## Compilación

Abre la carpeta raíz en Android Studio y espera la sincronización de Gradle. También puedes generar el APK de depuración con:

```bash
./gradlew assembleDebug
```

El APK se crea en `app/build/outputs/apk/debug/app-debug.apk`. El repositorio incluye una copia en `artifacts/VozLuma-Suite-3.0.0.apk`.

Esta edición usa el identificador `com.vozluma.premium` para evitar conflictos de firma con prototipos anteriores. Puede instalarse junto a versiones antiguas; si quieres conservar una sola app, desinstala primero el prototipo anterior y luego instala esta edición.

## Configuración inicial

Instala el APK y concede teléfono/contactos cuando Android lo solicite. Abre VozLuma con Internet disponible y pulsa **Descargar modelo español**. Espera a que aparezca **Modelo instalado**. Después abre **Conceder acceso a notificaciones**, activa VozLuma en los ajustes del sistema y confirma el aviso. Activa **Asistente activo**, habilita **Activación por voz: «Hola»** y concede el permiso de micrófono. El botón **Probar la voz** inicia una prueba guiada y te indica cuándo decir «Hola».

Mientras el micrófono está activo, Android muestra una notificación permanente de servicio. Esto es deliberado: permite al usuario saber que la aplicación está escuchando. Android impone requisitos de tipo y permisos para servicios de primer plano que usan micrófono, y puede restringir el inicio de ese servicio desde segundo plano [3]. Por eso algunos fabricantes pueden detener la escucha tras reiniciar o aplicar ahorro de batería; si ocurre, abre la aplicación y vuelve a activar el interruptor.

## Privacidad y seguridad

> VozLuma no envía tus notificaciones, llamadas, contactos, borradores ni audio a un servidor de VozLuma.

Los comandos que pueden producir efectos externos se mantienen deliberadamente seguros. La respuesta dictada se guarda como borrador local y requiere revisión explícita; el selector del sistema permite elegir la aplicación destino. El centro de privacidad permite borrar historial, contactos prioritarios y borradores. El usuario también puede abrir directamente los ajustes de permisos de Android.

El reconocimiento offline puede equivocarse con ruido, acentos o frases cortas. La palabra «Hola» funciona como disparador, pero la escucha debe activarse explícitamente y el servicio permanece visible mediante la notificación del sistema. La aplicación no promete escucha oculta ni reactivación automática universal después de un reinicio. El dashboard muestra el diagnóstico técnico actual: modelo pendiente, micrófono activo, error de escucha o «Hola detectado». Las consultas generales que no estén en la biblioteca local responden con claridad que requieren Internet; no se simula una respuesta de IA. El control de música funciona cuando el reproductor tiene una sesión multimedia activa o acepta teclas multimedia del sistema; seleccionar una canción concreta depende de las capacidades de cada reproductor. El control de pantalla requiere activar manualmente el servicio de accesibilidad desde Ajustes; Android reserva ese mecanismo para herramientas de asistencia general [4].

## Estructura principal

```text
app/src/main/
├── AndroidManifest.xml
├── filesDir/model-es/                    # Modelo descargado en el dispositivo
├── java/com/vozluma/app/
│   ├── AudioRouteChecker.kt
│   ├── CustomCommandStore.kt
│   ├── CustomCommandsActivity.kt
│   ├── BootReceiver.kt
│   ├── HistoryActivity.kt
│   ├── HistoryStore.kt
│   ├── IncomingCallReceiver.kt
│   ├── MainActivity.kt
│   ├── MediaControlManager.kt
│   ├── NotificationService.kt
│   ├── PreferencesStore.kt
│   ├── PriorityContactsActivity.kt
│   ├── PrivacyActivity.kt
│   ├── ReplyDraftActivity.kt
│   ├── ReplyDraftStore.kt
│   ├── TTSManager.kt
│   ├── VoiceActivationService.kt
│   ├── VozLumaAccessibilityService.kt
│   └── VozLumaWidgetProvider.kt
└── res/
    ├── drawable/
    ├── layout/
    ├── values/
    ├── values-night/
    └── xml/vozluma_widget_info.xml
```

## Referencias

[1]: https://developer.android.com/develop/ui/views/layout/edge-to-edge "Display content edge-to-edge in views — Android Developers"
[2]: https://alphacephei.com/vosk/models "Vosk Models"
[3]: https://developer.android.com/develop/background-work/services/fgs/service-types "Foreground service types — Android Developers"
[4]: https://developer.android.com/guide/topics/ui/accessibility/service "Create an accessibility service — Android Developers"
[4]: https://developer.android.com/guide/topics/ui/accessibility/service "Create an accessibility service — Android Developers"
