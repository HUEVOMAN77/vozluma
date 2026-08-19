# VozLuma

**VozLuma** es un prototipo de Android escrito en Kotlin que anuncia en voz alta notificaciones seleccionadas y llamadas entrantes mediante el motor nativo de Text-to-Speech. Está pensado para funcionar de forma local, sin cuentas, servidores ni conexión a Internet durante el uso.

## Funcionalidades

La aplicación incluye un interruptor para activar o desactivar el asistente, un acceso directo a la pantalla del sistema para conceder acceso a notificaciones y un botón para probar el audio. La primera versión procesa notificaciones de WhatsApp, Messenger, Facebook, Instagram y aplicaciones de SMS comunes. Ignora notificaciones persistentes, resúmenes de grupos, progreso, servicios y eventos repetidos durante una ventana breve para reducir lecturas duplicadas.

Cuando llega un mensaje, VozLuma utiliza el formato «[remitente] en [aplicación] dice: [mensaje]». Para una llamada entrante intenta resolver el nombre desde los contactos; si no puede hacerlo, anuncia el número o «número desconocido».

> VozLuma no envía notificaciones, llamadas ni contactos a ningún servidor. El procesamiento se realiza en el dispositivo.

## Requisitos

| Componente | Versión |
|---|---|
| Android Studio | Hedgehog o posterior |
| JDK | 17 |
| Android Gradle Plugin | 8.6.1 |
| Kotlin | 2.0.21 |
| Compile SDK | 35 |
| Android mínimo | API 26 / Android 8.0 |

## Compilación

Abre la carpeta raíz en Android Studio, espera a que termine la sincronización de Gradle y ejecuta la variante `debug` en un dispositivo o emulador. Desde una terminal con el SDK de Android configurado también puedes usar:

```bash
./gradlew assembleDebug
```

El APK de depuración se genera en `app/build/outputs/apk/debug/app-debug.apk`.

## Configuración en el teléfono

Después de instalar la aplicación, abre VozLuma y concede los permisos de teléfono y contactos cuando Android los solicite. Pulsa **Conceder acceso a notificaciones**, activa VozLuma en la pantalla del sistema y confirma el aviso de seguridad de Android. Finalmente, deja encendido el interruptor **Asistente activo** y pulsa **Probar la voz**.

El motor de voz depende de que el dispositivo tenga instalado y configurado un servicio TTS compatible con español. Si la prueba no produce audio, revisa los ajustes de texto a voz del sistema y el volumen multimedia.

## Estructura principal

```text
app/src/main/
├── AndroidManifest.xml
├── java/com/vozluma/app/
│   ├── BootReceiver.kt
│   ├── IncomingCallReceiver.kt
│   ├── MainActivity.kt
│   ├── NotificationService.kt
│   ├── PreferencesStore.kt
│   └── TTSManager.kt
└── res/
    ├── drawable/ic_vozluma.xml
    ├── layout/activity_main.xml
    └── values/{colors,strings,themes}.xml
```

## Privacidad y limitaciones del prototipo

El acceso a notificaciones y llamadas es sensible y siempre debe concederse manualmente. El comportamiento de algunos fabricantes puede variar porque Android puede detener o restringir servicios en segundo plano mediante sus ajustes de batería. La lectura de mensajes se limita deliberadamente a paquetes conocidos y puede requerir añadir el identificador de una aplicación si se desea cubrir otro cliente de mensajería.
