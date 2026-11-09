# Tasks

## 1. Modelo y Lógica de Vinculación (`SchemeBinding` y `SchemeTiming`)

- [x] 1.1 Crear `SchemeBinding.kt` en `com.vidcam.app.model.scheme` definiendo los modelos para la vinculación de fondos, slots de capas y banderas de flujo (como `openVoiceoverOnApply`).
- [x] 1.2 Implementar en `SchemeTiming.kt` las funciones `prepareInitialBinding` (auto-mapeo de fondos y capas existentes) y `applyBindingToProject` (inyección de URIs y textos en el `Project`).
- [x] 1.3 Crear pruebas unitarias JVM en `SchemeBindingTest.kt` verificando auto-mapeo de fondos y capas PNG existentes, resolución de slots de texto e inyección de tiempos por etapas.

## 2. ViewModel e Integración en Editor

- [x] 2.1 Agregar `applySchemeWithBinding` en `EditorViewModel.kt` con soporte de `UndoHistory` ("apply_scheme_binding") y mensaje informativo.
- [x] 2.2 Integrar la señal de activación opcional de voz en off en `EditorScreen.kt` para abrir automáticamente `VoiceoverSheet` al completar la aplicación del esquema.

## 3. Interfaz Compose `SchemeBindingSheet` y Experiencia de Usuario

- [x] 3.1 Crear la hoja modal `SchemeBindingSheet.kt` en Jetpack Compose mostrando resumen de etapas, acción destacada de selección en lote (`PickMultipleVisualMedia`), lista de slots (fondos, PNGs, textos) y switch opcional de doblaje de voz.
- [x] 3.2 Conectar `SchemesSheet.kt` con `SchemeBindingSheet.kt` en `EditorScreen.kt`, abriendo el asistente al presionar "Usar esquema" en lugar de aplicar capas vacías a ciegas.
- [x] 3.3 Actualizar la bitácora maestra en `alex.md` con el nuevo flujo de vinculación interactiva de esquemas.
