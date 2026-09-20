## Context

- `Project` es un `data class` inmutable; toda la edición pasa por
  `EditorViewModel.update { }`, que reemplaza el estado completo. Eso hace que
  tanto el historial como la persistencia sean operaciones sobre instantáneas.
- Los clips importados se copiaban a `cacheDir/imports` y las grabaciones a
  `cacheDir/captures`; los PNG de la galería se guardaban como `content://` del
  selector de fotos. Ninguna de las tres rutas sobrevive a una limpieza de caché
  ni a la caducidad del permiso del selector.
- `MusicSheet` lee `MediaStore.Audio` a través de `MusicRepository`, sin caché
  propia entre aperturas.
- El catálogo de la Biblioteca de audio de YouTube no se puede redistribuir: su
  licencia estándar se limita a vídeos de YouTube y prohíbe entregar las pistas
  como archivos sueltos.

## Goals / Non-Goals

**Goals:**
- Deshacer/rehacer fiable y barato, con una entrada por gesto.
- Proyectos que sobrevivan al cierre de la app y se puedan reabrir y borrar.
- Dejar claro cuándo hay cambios sin guardar.
- Ofrecer una vía legal y cómoda para conseguir música.

**Non-Goals:**
- Historial persistente entre sesiones (el historial se pierde al abrir o crear
  un proyecto; no se guarda en disco).
- Empaquetar música en el APK o descargarla automáticamente.
- Autoguardado en segundo plano o recuperación tras muerte del proceso.
- Sincronización en la nube, exportar/importar proyectos o compartirlos.
- Control de versiones del proyecto guardado (cada guardado sobrescribe).

## Decisions

### 1. Historial de instantáneas con agrupación por clave

`UndoHistory` (sin dependencias de Android, en `editor/UndoHistory.kt`) guarda
pilas de `Project` con un límite de 60 entradas:

```kotlin
fun record(before: Project, key: String? = null, nowMs: Long)
fun undo(current: Project): Project?
fun redo(current: Project): Project?
```

`update` en el ViewModel registra el estado previo **solo si el proyecto cambió**
(`if (next == before) return`), de modo que un toque sin efecto no ensucia el
historial. Cada llamada pasa una clave:

| Edición | Clave |
|---|---|
| Recorte de un clip | `trim:<id>` |
| Deslizador de inicio de música | `music-start` |
| Diálogo de edición de capa | `layer-edit:<id>` |
| Gesto sobre una capa | `gesture:<id>` |
| Alta/baja de clips, capas o pistas | sin clave |

Si la clave coincide con la última y no pasaron más de 700 ms, no se apila una
instantánea nueva: se conserva la del inicio del grupo. Un gesto continuo emite
cada ~16 ms, así que todo el gesto se deshace de una vez. Sin clave, cada edición
es una entrada independiente.

**Alternativa descartada**: botones explícitos de "empezar/terminar gesto" desde
la UI. Requiere tocar la superficie de puntero y sincronizarla con el ViewModel;
la agrupación por tiempo y clave da el mismo resultado sin acoplar las capas.
**Coste**: una `Project` de referencia por entrada; con el límite de 60 es
despreciable (las listas se comparten entre instantáneas inmutables).

### 2. Persistencia: un JSON por proyecto, con la carpeta inyectada

`ProjectStore(rootDir: File)` no depende de `Context`, así que se prueba en la
JVM con un directorio temporal. El formato es:

```json
{
  "id": "proj-1737000000000",
  "name": "Mi proyecto",
  "createdAtMs": 0,
  "updatedAtMs": 0,
  "project": { "clips": [], "music": null, "layers": [] }
}
```

- `Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }`:
  legible a mano y tolerante a campos nuevos, lo que da margen para evolucionar
  el modelo sin migraciones.
- Escritura a un `.tmp` y `renameTo` final: si el proceso muere a mitad, el JSON
  anterior sigue siendo válido.
- `list()` ignora los archivos que no parsean (un JSON corrupto no rompe la
  lista) y ordena por `updatedAtMs` descendente.
- El `id` es `proj-<epochMs>`; el nombre por defecto se genera con la fecha
  (`Proyecto 20 sep 14:32`). `createdAtMs` se conserva al sobrescribir.

**Alternativa descartada**: Room. El proyecto no necesita consultas ni índices y
el JSON es inspeccionable, exportable a mano y sin dependencias nuevas más allá
de la serialización.

### 3. Medios en `filesDir`, no en la caché

Si un proyecto guardado referencia `cacheDir/imports/clip-123.mp4`, el sistema
puede borrarlo en cualquier momento y el proyecto abre roto. Por eso los clips
importados y los PNG de la galería se copian a `filesDir/media`, y las
grabaciones a `filesDir/captures`. Los stickers integrados siguen siendo
`asset://` porque viven en el APK y son estables por definición.

**Trade-off**: se duplica el archivo importado en el almacenamiento propio y no
se limpia al descartar el proyecto. Aceptado por ahora: la alternativa (copiar
solo al guardar y mantener un registro de referencias) es más compleja y puede
perder medios si el usuario guarda justo después de importar.

### 4. Estado "sin guardar" calculado por comparación

No hay bandera manual de suciedad: el ViewModel conserva `savedSnapshot: Project`
(la última versión que coincide con disco) y `dirty` es
`_project.value != savedSnapshot`. Como `Project` es un `data class`, la igualdad
estructural es exacta, así que deshacer hasta el estado guardado vuelve a marcar
el proyecto como limpio. Es más honesto que una bandera que solo se apaga al
guardar.

### 5. Fuentes de música: enlaces, nunca audio empaquetado

`MusicSheet` añade una sección con cinco fuentes y un botón "Actualizar" que
vuelve a consultar `MediaStore`. No se descarga ni se empaqueta ninguna pista:
la Biblioteca de audio de YouTube limita su licencia estándar a vídeos de
YouTube y prohíbe redistribuir las pistas, y empaquetarlas expondría la app a
una retirada por propiedad intelectual en Google Play. El texto de cada fuente
recuerda que hay que revisar la licencia (algunas piden crédito).

## Risks / Trade-offs

- **[Crecimiento del almacenamiento por medios persistentes]** → Los clips ya se
  copiaban a la caché; ahora persisten. Se acepta y se documenta; una limpieza de
  huérfanos queda como trabajo futuro.
- **[Un proyecto guardado apunta a un archivo que el usuario borró]** → La capa o
  el clip se muestran como no disponibles (el `decodePng`/`durationMs` devuelve
  nulo o 0) y no rompen la apertura del proyecto.
- **[Historial de 60 entradas con proyectos grandes]** → Las instantáneas
  comparten las listas inmutables, así que solo se copian las referencias.
- **[Nombre por defecto poco descriptivo]** → El usuario puede renombrar con
  "Guardar como…"; no hay rename aparte.
- **[Google Fonts en proyectos guardados]** → El nombre de la fuente se guarda,
  pero la descarga se resuelve de nuevo al abrir; sin conexión cae a la fuente
  local, que es el comportamiento ya existente.

## Migration Plan

No hay datos persistidos de versiones anteriores, así que no hay migración: la
primera ejecución crea `filesDir/projects` al guardar. El JSON lleva
`ignoreUnknownKeys = true` para tolerar cambios futuros del modelo.

## Open Questions

- Ninguna que bloquee. Queda para después la limpieza de medios huérfanos y un
  autoguardado periódico.
