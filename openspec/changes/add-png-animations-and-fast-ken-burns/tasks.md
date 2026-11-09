# Tasks

- [x] Añadir enums `@Serializable` `PngAnimationPreset`, `AnimationSpeed` y `LayerTransitionPreset` en `Project.kt` y extender `OverlayLayer` y `BackgroundLayer` manteniendo retrocompatibilidad JSON
- [x] Implementar la matemática pura de Ken Burns rápido, loops (`PULSO`, `FLOTAR`, `VAIVEN`, `TEMBLOR`), transiciones (`POP`, `DESLIZAR`, `FUNDIDO`), opacidad (`resolvedAlphaAt`) y cobertura de fondos (`resolvedTransformAt`) en `LayerAnimation.kt`
- [x] Integrar la opacidad animada en `LayerBitmapOverlay` (`Overlays.kt`) y la transformación de fondos (`MatrixTransformation`) en `VideoExporter.kt`
- [x] Añadir acciones de actualización con soporte Undo/Redo en `EditorViewModel.kt` (`setLayerAnimation`, `setLayerAnimationSpeed`, `setLayerTransition`, `setBackgroundAnimation`, `setBackgroundAnimationSpeed`)
- [x] Actualizar `EditorScreen.kt` para renderizar la opacidad de capas y la animación de fondos en `TimelinePreview` y `ExportPreviewDialog`, exponer los selectores de chips en `LayerRow`, `LayerEditDialog`, `BackgroundRow` y `BackgroundSegmentDialog`, y habilitar `"Grabar movimiento"` sin clips
- [x] Añadir tests unitarios en `LayerAnimationTest.kt` y `ProjectStoreTest.kt`
- [x] Actualizar `alex.md` con el resumen de cambios
