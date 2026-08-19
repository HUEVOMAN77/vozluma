# Diseño de VozLuma Premium

Fecha: 2026-08-19.

## Decisiones

Material 3 organiza los componentes en acciones, contención, navegación, selección y entrada de texto. La edición premium usará una jerarquía clara: un encabezado con estado de escucha, una acción principal de activación, tarjetas de configuración agrupadas y navegación inferior o accesos destacados para inicio, automatizaciones, historial y privacidad. Fuente: https://developer.android.com/design/ui/mobile/guides/components/material-overview

Como el proyecto apunta a SDK 35, Android 15 aplica edge-to-edge automáticamente. La interfaz debe aplicar insets a elementos interactivos para que no queden debajo de la barra de estado o navegación, y debe mantener suficiente contraste en modo claro y oscuro. Fuente: https://developer.android.com/develop/ui/views/layout/edge-to-edge

## Dirección visual

Se usará una estética premium de fondo oscuro azul noche, gradientes violeta/cian, superficies redondeadas, tarjetas con jerarquía, chips de estado, un indicador visual del micrófono y microinteracciones discretas. No se debe depender de texto pequeño ni de color como único indicador; cada estado tendrá texto, icono y contraste accesible.

## Alcance funcional premium

La edición incluirá respuestas por voz con confirmación, comandos locales para ajustes del teléfono, contactos prioritarios, resumen de notificaciones, modo conversación continuo, comandos personalizados guardados localmente, detección de alertas importantes, protección para acciones sensibles, historial y privacidad explicable, panel de batería y almacenamiento, perfiles, modo conducción, widget y configuración de velocidad TTS.

## Límite honesto

Las acciones que impliquen enviar mensajes, llamar, borrar información o modificar permisos deben pedir confirmación explícita. El reconocimiento offline podrá ejecutar comandos predefinidos sin red, pero no será un modelo conversacional general ilimitado. Android puede restringir micrófono y servicios en segundo plano; la app debe mostrar el servicio activo y permitir detenerlo.
