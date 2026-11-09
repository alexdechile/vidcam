# Tasks

## 1. Modelo de datos y matemática de etapas

- [x] 1.1 Crear modelo `UsageScheme`, `SchemeSlot`, `SchemeBackground`, `SchemeAudio` en `com.vidcam.app.model.scheme` y verificar compilación y serialización `@Serializable`.
- [x] 1.2 Implementar `SchemeTiming.kt` para conversión de etapas modulares (`stagesCount`, `stageStart`, `stageEnd`) a milisegundos (`startMs`, `endMs`) y adaptadores entre `UsageScheme` y `Project`.
- [x] 1.3 Crear pruebas unitarias `SchemeTimingTest` y `UsageSchemeSerializationTest` verificando el cálculo de las 5 etapas (15s) y la serialización JSON.

## 2. Almacén de esquemas y catálogo de fábrica

- [x] 2.1 Implementar `SchemeStore` en `com.vidcam.app.data` para persistencia atómica en disco y recuperación tolerante a versiones futuras.
- [x] 2.2 Implementar catálogo de fábrica `FactorySchemes` con el preset "5 Etapas en Cascada (15s)" y esquemas recomendados.
- [x] 2.3 Crear pruebas unitarias `SchemeStoreTest` para guardado, carga, listado y eliminación de esquemas en JVM sin dependencias de Android.

## 3. Integración en ViewModel y UI del Editor

- [x] 3.1 Agregar soporte en `EditorViewModel` para `applyScheme(scheme)`, `saveCurrentAsScheme(name)` y carga reactiva de esquemas.
- [x] 3.2 Crear hoja modal `SchemesSheet.kt` en Jetpack Compose para explorar esquemas de fábrica y guardados, con previsualización visual de sus etapas.
- [x] 3.3 Conectar acceso a esquemas en `EditorScreen.kt` (menú/botón de plantilla) y acciones de aplicar y guardar.

## 4. Documentación y Verificación

- [x] 4.1 Actualizar `alex.md` y `README.md` documentando la arquitectura de esquemas y el formato JSON.
- [x] 4.2 Ejecutar `openspec validate` y verificar build/tests en CI vía GitHub Actions.
