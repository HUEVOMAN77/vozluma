# Diagnóstico de instalación Android 14/15

Fecha: 2026-08-19.

## Hallazgos del APK actual

El APK `VozLuma-Premium-2.0.1.apk` tiene `minSdk 26`, `targetSdk 35`, firma APK Signature Scheme v2 verificada, alineación ZIP válida con `zipalign`, y contiene código nativo de Vosk y JNA para varias arquitecturas (`arm64-v8a`, `armeabi-v7a`, `x86_64`, además de arquitecturas heredadas). No hay un dispositivo Android conectado al entorno, por lo que no se pudo obtener el código exacto `INSTALL_FAILED_*` mediante Package Manager.

## Posible causa de Android 15

La documentación oficial de Android indica que los dispositivos con páginas de memoria de 16 KB requieren revisar las bibliotecas nativas, su alineación ELF y el empaquetado del APK. El proyecto incluye `libvosk.so`, una biblioteca nativa precompilada de terceros, por lo que debe comprobarse su compatibilidad con 16 KB y el APK debe usar empaquetado/alineación de 16 KB cuando corresponda. Fuente: https://developer.android.com/guide/practices/page-sizes

La documentación oficial también indica que Android verifica la firma del APK al instalarlo y que una firma diferente es relevante cuando se intenta actualizar una aplicación existente. La APK actual usa una firma de depuración válida, pero la aplicación se cambió a `com.vozluma.premium` para evitar conflictos con prototipos anteriores. Fuente: https://source.android.com/docs/security/features/apksigning

## Plan técnico

1. Generar una variante arm64-v8a enfocada a teléfonos Android modernos y otra universal si resulta necesario.
2. Configurar empaquetado de bibliotecas nativas sin compresión y alineación de 16 KB.
3. Revisar la alineación ELF de `libvosk.so` y `libjnidispatch.so`.
4. Validar firma, zipalign, badging, lint y tamaño.
5. Publicar el APK corregido y pedir al usuario el código exacto de instalación si el teléfono sigue mostrando únicamente «No se instaló la app».
