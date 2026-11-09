# Proposal: Esquemas de Uso y Plantillas por Etapas (Usage Schemes)

## Why

VidCam permite actualmente guardar y cargar proyectos completos (`SavedProject`). Sin embargo, un proyecto guarda rutas y URIs locales absolutos (`content://...` o rutas internas del dispositivo) ligados a archivos específicos de vídeo y audio.

Los creadores de contenido para redes sociales (Reels, TikTok, Stories) necesitan reutilizar **estructuras y coreografías de edición** (p. ej. "un Reel de 15 segundos con fondo en paneo, un gancho visual en los primeros 6 segundos, un desarrollo central entre el segundo 3 y 12, y un remate o llamada a la acción en los últimos 9 segundos"). Configurar manualmente los tiempos, transiciones y animaciones de cada capa en cada nuevo vídeo resulta repetitivo y propenso a errores.

Hacerlo modelando una **grilla modular de etapas (stages)** permite a los usuarios pensar en compases temporales (ej. 5 etapas de 3 segundos) y generar composiciones limpias, portables en JSON y reutilizables sobre cualquier nuevo conjunto de medios.

## What Changes

- **Modelo de Esquemas de Uso (`UsageScheme` / `VidCamTemplate`)**:
  - Definición de esquema portable en JSON (`kotlinx.serialization`) con duración total (`totalDurationSec`) y cantidad de etapas (`stagesCount`).
  - Slots modulares temporales asociados a rangos de etapas (`stageStart` a `stageEnd`), con cálculo automático de milisegundos (`startMs`, `endMs`).
  - Soporte de configuración de fondo con animación continua o Ken Burns (`KenBurnsMode`, `AnimationSpeed`).
  - Soporte de slots de capas superpuestas (imagen/sticker, texto) con sus transiciones de entrada/salida (`LayerTransitionPreset`), presets de animación (`PngAnimationPreset`, `AnimationSpeed`) y posicionamiento.
  - Soporte de configuración de ventana de vídeo (`VideoWindow`), audio/música y subtítulos (`CaptionStyle`).
- **Almacenamiento y Gestión de Esquemas (`SchemeStore`)**:
  - Carpeta de esquemas de usuario (`templates/`) con operaciones de listado, guardado, carga y borrado.
  - Catálogo de esquemas de fábrica (presets embebidos en la app, incluyendo el esquema de 5 etapas en cascada de 15 s).
  - Exportación e importación de esquemas como JSON portable (compatible con compartir por WhatsApp/Telegram y copiar/pegar).
- **Integración en el Editor (`EditorViewModel` y UI)**:
  - Acción "Guardar como esquema": extrae la estructura temporal y visual del proyecto actual limpiando referencias a archivos privados.
  - Diálogo / Hoja modal "Plantillas y Esquemas": selector visual para aplicar un esquema sobre el proyecto actual o iniciar uno nuevo.
  - Asistente de slots: mapeo asistido para colocar imágenes o textos en cada slot predefinido del esquema.

## Capabilities

### New Capabilities
- `usage-schemes`: Definición, serialización, cálculo de tiempos por etapas, almacenamiento y aplicación de esquemas de uso (templates) en VidCam.

## Impact

- **Modelo**: Nuevas clases `UsageScheme`, `SchemeSlot`, `SchemeBackground`, etc., en paquete `com.vidcam.app.model.scheme`.
- **Persistencia**: Nuevo `SchemeStore` en `com.vidcam.app.data`, catálogo de esquemas por defecto en assets o código.
- **Editor**: Nuevos métodos en `EditorViewModel` para aplicar un esquema al `Project` activo y exportar/guardar esquemas.
- **UI**: Hoja modal de esquemas en Compose (`SchemesSheet.kt`).
- **Tests**: Pruebas unitarias de cálculo de tiempos por etapa, serialización/deserialización JSON y aplicación de esquemas a proyectos.
