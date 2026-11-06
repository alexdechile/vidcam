## 1. Modelo y cálculo

- [x] 1.1 Agregar el enum serializable `KenBurnsMode` (`NINGUNO`, `ACERCAR`, `ALEJAR`,
      `AUTOMATICO`) en `model/Project.kt`
- [x] 1.2 Agregar `BackgroundLayer.kenBurns` con valor por defecto `NINGUNO` (retrocompatible)
- [x] 1.3 Crear `model/KenBurnsMath.kt` con `transformAt`, `progressAt`, `resolveMode`, `indexOf`
      y `zoomPerSecond`, sin dependencias de Android ni de Media3
- [x] 1.4 Tests JVM en `app/src/test/.../model/KenBurnsMathTest.kt`: dirección, acotado del
      desplazamiento, velocidad según la duración, alternancia automática, tramos inválidos y
      desplazamiento nulo sin efecto

## 2. Exportación

- [x] 2.1 Guardar en `PreparedBackdrop` el modo, la ventana del Ken Burns y el índice de la imagen
- [x] 2.2 Resolver el transform del fondo activo por fotograma en `WindowCompositorSettings` y
      entregarlo como `OverlaySettings` (escala y ancla en NDC)
- [x] 2.3 Configurar el compositor también en los proyectos sin clips, donde la secuencia de fondos
      es la entrada 0, y aplicar la ventana de vídeo solo cuando hay clips
- [x] 2.4 Verificar que sin efecto los ajustes equivalen a los anteriores (escala 1, ancla 0)
- [x] 2.5 Usar la ventana del fondo original (no el tramo recortado por solapes) para el zoom

## 3. Editor

- [x] 3.1 `EditorViewModel.setBackgroundKenBurns(id, mode)` con clave de deshacer
      `background-ken-burns:<id>`
- [x] 3.2 Los fondos nuevos entran con `AUTOMATICO` desde `addBackground`
- [x] 3.3 Chips `Sin zoom / Zoom in / Zoom out / Ken Burns auto` en la fila de cada fondo
- [x] 3.4 `KenBurnsBackground` en la vista previa con `positionMs` y en `ExportPreviewDialog` con el
      instante del resumen
- [x] 3.5 El resumen de exportación indica el efecto activo del fondo

## 4. Documentación

- [x] 4.1 `openspec/changes/add-ken-burns-effect` (proposal, design, tasks y spec)
- [x] 4.2 `README.md` con la sección del efecto y el nuevo change en la lista
- [x] 4.3 `alex.md` con la entrada de "Último Realizado"

## 5. Verificación

- [ ] 5.1 Compilar y correr los tests en GitHub Actions (`lint`, `test`, `assembleDebug`,
      `assembleRelease`) y corregir lo que falle
- [ ] 5.2 Probar en dispositivo: fondo único con automático, dos fondos alternando, `Zoom in` y
      `Zoom out` forzados, `Sin zoom`, y comparar el MP4 con la vista previa
- [ ] 5.3 Probar con tramos de distinta duración (4 s y 20 s) para confirmar que el movimiento es
      más lento en el vídeo más largo
- [ ] 5.4 Probar un vídeo solo con imágenes (sin clips) de 10 s con un fondo en `Ken Burns auto`
