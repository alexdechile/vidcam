# Tasks

- [x] Añadir enum `ColorFilterPreset` (NINGUNO, SEPIA, BLANCO_Y_NEGRO,
  VINTAGE, ALTO_CONTRASTE) al modelo y campos `colorFilter` + `wideLens` en
  `VideoClip` con defaults seguros
- [x] Crear `ClassicEffects.kt`: mapeo preset → matriz RGBA 4x5 (`RgbMatrix`
  de media3-effect) y `RgbAdjustment`
- [x] En `EditorViewModel`: `setClipFilter(id, preset)` y `setClipWide(id,
  bool)` con historial deshacer/rehacer
- [x] Extender `VideoExporter.buildSequences` para aplicar por clip el filtro
  de color y el encuadre ancho (escala 1/0.65) antes de capas/música
- [x] En `TimelinePreview`: aplicar filtro (ColorFilter Compose) y encuadre
  ancho (escala 1/0.65) por clip al reproducir
- [x] En la pantalla de grabación: chips de filtro + botón "Grupo" junto al
  selector de velocidad, aplicando el filtro/lente al clip grabado
- [x] Poner filtro/lente en el JSON del proyecto y leer proyecto antiguo con
  defaults (sin migración)
- [x] Guardar y reabrir proyecto conservando filtro y lente ancho por clip
- [x] Tests: matrices de cada preset contra valor esperado; `TimelineMath`
  (la duración efectiva NO cambia con filtro/lente)
- [x] Compilar con lint + tests + androidTest hasta verde en CI
- [ ] Tag `v1.2.0` → release → descargar APK → enviarlo por WhatsApp y por
  Telegram con el aviso
