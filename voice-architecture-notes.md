# Notas de arquitectura de voz

Fecha de consulta: 2026-08-19.

## Hallazgos

La documentación oficial de Android indica que `SpeechRecognizer` requiere `RECORD_AUDIO` y que su implementación puede transmitir audio a servidores remotos; por ello no debe asumirse como reconocimiento offline ni como solución de escucha continua. Android además advierte que la escucha continua consume batería y ancho de banda. Fuente: https://developer.android.com/reference/android/speech/SpeechRecognizer

Para una experiencia por voz realmente offline y con palabra de activación, la opción más confiable es incorporar un motor local de reconocimiento de voz o de detección de palabra clave dentro de la aplicación. La aplicación debe indicar claramente el uso del micrófono y ofrecer un interruptor visible para activarlo.

Desde Android 14/API 34, un servicio de primer plano que usa micrófono debe declarar `android:foregroundServiceType="microphone"`, solicitar `FOREGROUND_SERVICE` y `FOREGROUND_SERVICE_MICROPHONE`, y contar con `RECORD_AUDIO`. Android también aplica restricciones de permisos while-in-use y limita iniciar este servicio directamente desde segundo plano o desde `BOOT_COMPLETED`. Fuente: https://developer.android.com/develop/background-work/services/fgs/service-types

## Decisión pendiente

Hay dos rutas viables:

1. Reconocimiento local integrado: permite una palabra «Hola» auténticamente offline, pero aumenta el tamaño del APK, consumo de batería y complejidad de distribución del modelo.
2. `SpeechRecognizer` del sistema: requiere menos código y tamaño, pero el modo offline depende del motor instalado y no garantiza una escucha continua fiable en segundo plano.

La implementación debe evitar prometer que el micrófono estará escuchando permanentemente en todos los teléfonos. La interfaz debe explicar las limitaciones y usar un servicio de primer plano visible cuando la escucha esté activa.

## Vosk

El repositorio de ejemplo de Vosk para Android describe reconocimiento de voz offline y una integración Android basada en su biblioteca. Fuente: https://github.com/alphacep/vosk-android-demo

El catálogo oficial de modelos de Vosk incluye `vosk-model-small-es-0.42`, con aproximadamente 39 MB, descrito como modelo ligero de banda ancha para Android y Raspberry Pi. Fuente: https://alphacephei.com/vosk/models

## Implicación para VozLuma

El modelo español debe distribuirse dentro de `app/src/main/assets/model-es` o descargarse una sola vez con una pantalla explícita de configuración. Para cumplir el objetivo offline desde la primera ejecución, se integrará dentro del APK o en un paquete de modelo descargable incluido como artefacto del repositorio. Vosk procesa el audio localmente, pero el modelo consume almacenamiento y memoria; se debe mostrar este requisito en la interfaz.
