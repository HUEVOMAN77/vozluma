# VozLuma

**VozLuma** es un asistente Android en Kotlin que anuncia notificaciones seleccionadas y llamadas entrantes mediante Text-to-Speech. También incorpora un modo manos libres: cuando el usuario activa la opción, el teléfono escucha localmente la palabra **«Hola»** y responde **«Hola, ¿en qué puedo ayudarte?»** sin enviar el audio a un servidor.

## Funciones incluidas

| Área | Funcionalidad |
|---|---|
| Voz local | Modelo español Vosk incluido en el APK, detección de «Hola», respuesta hablada y comandos básicos offline. |
| Notificaciones | WhatsApp, Messenger, Facebook, Instagram, Mensajes de Google, SMS y Mensajes Samsung; filtro por aplicación, deduplicación y filtros inteligentes. |
| Llamadas | Anuncia el nombre del contacto cuando existe permiso de contactos; si no, anuncia el número o «número desconocido». |
| Perfiles | Horario silencioso configurable, modo coche y lectura solo con auriculares o Bluetooth. |
| Privacidad | Historial opcional exclusivamente local, borrado manual y procesamiento local de audio y mensajes. |
| Acceso rápido | Widget de pantalla de inicio para activar o pausar la escucha. |
| Personalización | Tema oscuro, velocidad de voz en español y selección individual de aplicaciones. |

## Conversación por voz

Con la activación por voz encendida, di «Hola». VozLuma responderá «Hola, ¿en qué puedo ayudarte?» y abrirá una ventana breve para escuchar una orden. La versión actual entiende comandos como:

```text
Hola
¿Qué hora es?
Lee la última notificación
¿Qué puedes hacer?
Activa el asistente
Desactiva el asistente
Activa el modo coche
¿Cuál es el estado?
```

El reconocimiento local se realiza con Vosk y el modelo español ligero `vosk-model-small-es-0.42`, incluido en `app/src/main/assets/model-es`. La distribución oficial describe este modelo como ligero para Android y Raspberry Pi y publica un tamaño comprimido aproximado de 39 MB [1]. La aplicación termina con un APK más grande porque también contiene la biblioteca de reconocimiento y sus bibliotecas nativas.

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

## Compilación

Abre la carpeta raíz en Android Studio, espera a que termine la sincronización de Gradle y ejecuta la variante `debug` en un teléfono o emulador. Desde una terminal con el SDK de Android configurado puedes usar:

```bash
./gradlew assembleDebug
```

El APK se genera en `app/build/outputs/apk/debug/app-debug.apk`. En este repositorio también se incluye una copia lista para instalar en `artifacts/VozLuma-debug.apk`.

## Configuración en el teléfono

Después de instalar la aplicación, concede los permisos de teléfono y contactos cuando Android los solicite. Pulsa **Conceder acceso a notificaciones**, activa VozLuma en la pantalla del sistema y confirma el aviso de seguridad. Después activa **Asistente activo**.

Para usar el modo manos libres, activa **Activación por voz: «Hola»** y concede el permiso de micrófono. Mientras el micrófono esté activo, Android mostrará una notificación permanente de servicio; esto es intencional y permite saber cuándo VozLuma está escuchando. La primera activación puede tardar unos segundos porque el modelo local se copia al almacenamiento privado de la aplicación.

El motor TTS depende de que el teléfono tenga un servicio de texto a voz configurado para español. Si la prueba no produce audio, revisa los ajustes de texto a voz y el volumen multimedia.

## Privacidad y limitaciones

> VozLuma no envía tus notificaciones, llamadas, contactos ni audio a ningún servidor. La activación y el reconocimiento de voz se procesan en el dispositivo.

Android impone restricciones a los servicios de micrófono en segundo plano. Un servicio de primer plano que usa micrófono debe declarar su tipo y permisos correspondientes, y el usuario debe iniciar la escucha desde la aplicación o desde el widget después de conceder el permiso [2]. Por esta razón, no se promete que todos los fabricantes mantengan la escucha después de reiniciar el teléfono o tras aplicar restricciones agresivas de batería. Si el servicio se detiene, abre VozLuma y vuelve a activar el interruptor.

El modelo español ligero puede tener errores con acentos, ruido o frases muy cortas. La palabra «Hola» funciona como disparador, pero el usuario debe activar explícitamente el modo de voz; el micrófono no queda activo de forma oculta.

## Estructura principal

```text
app/src/main/
├── AndroidManifest.xml
├── assets/model-es/                    # Modelo Vosk español offline
├── java/com/vozluma/app/
│   ├── AudioRouteChecker.kt
│   ├── BootReceiver.kt
│   ├── HistoryActivity.kt
│   ├── HistoryStore.kt
│   ├── IncomingCallReceiver.kt
│   ├── MainActivity.kt
│   ├── NotificationService.kt
│   ├── PreferencesStore.kt
│   ├── TTSManager.kt
│   ├── VoiceActivationService.kt
│   └── VozLumaWidgetProvider.kt
└── res/
    ├── drawable/
    ├── layout/
    ├── values/
    └── xml/vozluma_widget_info.xml
```

## Referencias

[1]: https://alphacephei.com/vosk/models "Vosk Models"
[2]: https://developer.android.com/develop/background-work/services/fgs/service-types "Foreground service types — Android Developers"
