# Proposal

## Why

Los esquemas de uso (templates) definen coreografías temporales avanzadas por etapas (Ken Burns en fondos, transiciones y animaciones procedimentales en capas PNG), pero actualmente al aplicarse generan capas con `uri = ""` vacías o descartan los elementos que el usuario ya tenía en el lienzo. Para que las plantillas sean inmediatamente útiles y prácticas, se necesita un puente interactivo y flexible que conecte cada slot del esquema con objetos reales (fondos de galería, capas PNG existentes o nuevas, textos y grabación opcional de voz en off).

## What Changes

- **Vinculación flexible de slots (`SchemeBinding`)**: Estructura de datos que asocia cada slot de imagen/texto/fondo del esquema a recursos reales (`Uri` de imagen, textos personalizados, o capas existentes del proyecto).
- **Auto-mapeo inteligente inicial**: Si el proyecto ya contiene un fondo o capas PNG, se pre-asignan automáticamente a los slots del esquema en orden cronológico respetando la coreografía y tiempos de la plantilla.
- **Asistente interactivo de vinculación (`SchemeBindingSheet`)**: Interfaz modal donde el usuario visualiza los slots requeridos, puede reasignar fondos/PNGs individualmente o utilizar el atajo de **selección múltiple en lote** (`PickMultipleVisualMedia`) para rellenar todos los slots de imagen de un solo toque.
- **Voz en off opcional**: Opción para saltar directamente a la hoja de doblaje (`VoiceoverSheet`) una vez aplicado el esquema, manteniendo el flujo centrado en vídeo y simplificando la locución sincronizada.
- **Soporte de Deshacer (`UndoHistory`)**: La aplicación de un esquema con vinculación se puede deshacer de forma atómica con un solo toque.

## Capabilities

### Modified Capabilities
- `usage-schemes`: Se amplía el requisito de aplicación de esquemas para incorporar la vinculación interactiva y auto-mapeo de slots (`SchemeBinding`), permitiendo enlazar fondos y capas reales de forma individual o en lote, con opción de voz en off.

## Impact

- **Modelos y Lógica**: `com.vidcam.app.model.scheme.SchemeBinding`, extensión de `SchemeTiming.kt` para calcular y aplicar el binding al `Project`.
- **UI Compose**: Nuevo componente `SchemeBindingSheet.kt`, integración en `SchemesSheet.kt` y `EditorScreen.kt`.
- **ViewModel**: Nuevo método `applySchemeWithBinding` en `EditorViewModel.kt` con soporte para abrir la grabadora de voz de forma opcional tras la aplicación.
