## Purpose

Aplica a cada clip filtros de color clásicos reconocibles: sepia, blanco y
negro, vintage y alto contraste — además de "ninguno" — que transforman la
toma con apariencia "producida", de forma idéntica en la vista previa del
editor, en el selector al grabar, y en el MP4 exportado, y que se conservan
al guardar y reabrir el proyecto.

## ADDED Requirements

### Requirement: Selección de filtro clásico por clip

Cada clip SHALL tener exactamente un `colorFilter` seleccionable entre
`NINGUNO`, `SEPIA`, `BLANCO_Y_NEGRO`, `VINTAGE` y `ALTO_CONTRASTE`, con
`NINGUNO` como valor por defecto. La selección SHALL hacerse en dos momentos:

1. Al grabar (selector sobre la cámara, junto a velocidad y lente ancho),
   que queda aplicada al clip que se acaba de agregar.
2. A posteriori sobre cualquier clip ya en la línea de tiempo (cabecera del
   clip), como parte del mismo flujo de edición con deshacer/rehacer.

El filtro no SHALL modificar la duración efectiva del clip ni el presupuesto
de 30 s del proyecto: la duración del timeline se sigue midiendo como
recorte × velocidad.

#### Scenario: Seleccionar filtro al grabar

- **WHEN** el usuario elige "Sepia" en el selector de grabación y graba un
  clip de 6 s a velocidad 1x
- **THEN** el clip entra a la línea de tiempo con `colorFilter = SEPIA`, y el
  resto de clips conservan su filtro

#### Scenario: Cambiar filtro por clip sin regrabar

- **WHEN** el usuario abre la cabecera de un clip grabado con sepia y elige
  "Blanco y negro"
- **THEN** el filtro del clip cambia a `BLANCO_Y_NEGRO` y la operación queda
  registrada en la línea de deshacer como un único paso

#### Scenario: Ninguno por defecto

- **WHEN** se graba un clip sin tocar el selector de filtros
- **THEN** el clip queda con `colorFilter = NINGUNO`

#### Scenario: El filtro no cambia la duración efectiva

- **WHEN** un clip de 10 s con `trimStartMs = 1000` y `trimEndMs = 9000` a
  1x recibe un filtro de `SEPIA`
- **THEN** su duración efectiva sigue siendo 8000 ms y el presupuesto total
  del proyecto no cambia

### Requirement: Vista previa fiel del filtro

La vista previa de la línea de tiempo SHALL mostrar cada clip con su filtro
de color aplicado, usando el mismo mecanismo de Media3 `/effect` que usa la
exportación, de modo que el encuadre, el color y la velocidad que el usuario
ve al reproducir coincidan con el MP4 exportado. La cámara en vivo (grabando)
SHALL mostrar el sensor crudo sin el filtro, igual que hoy: el chip se limita
a marcar qué filtro se aplicará, como ya ocurre con la velocidad.

#### Scenario: Filtro visible en la vista previa

- **WHEN** la línea de tiempo reproduce un clip con `colorFilter = SEPIA`
- **THEN** la imagen mostrada en la vista previa aparece con tono sepia
  idéntico al que tendrá el MP4 exportado

#### Scenario: Cámara en vivo sin filtro

- **WHEN** el usuario está grabando y ha seleccionado "Vintage"
- **THEN** la cámara muestra el sensor crudo sin filtro, y la marca
  "Vintage" se aplica al clip recién grabado

### Requirement: Exportación con el filtro del clip

La exportación SHALL aplicar a cada `EditedMediaItem` el filtro de color de
su clip (matriz RGB/HSL de media3-effect, igual a la vista previa) **antes**
de la composición de capas, música y velocidad, sin cambiar el orden de
composición, la resolución de salida ni el límite de 30 s. Un clip con
`colorFilter = NINGUNO` SHALL exportarse sin alteración de color.

#### Scenario: Exportar un clip con filtro

- **WHEN** el usuario exporta un proyecto cuyo único clip tiene
  `colorFilter = ALTO_CONTRASTE`
- **THEN** el MP4 de salida muestra ese clip con alto contraste aplicado,
  conservando la resolución de exportación y la duración efectiva

#### Scenario: Exportar sin filtro

- **WHEN** el proyecto tiene clips con `colorFilter = NINGUNO`
- **THEN** el MP4 sale sin alteración de color y con la misma duración
  efectiva que si no existiera la función de filtros

### Requirement: Persistencia del filtro por clip

El filtro de color de cada clip SHALL serializarse en el JSON del proyecto y
restaurarse al abrirlo: un clip con `colorFilter = SEPIA` guardado y reabierto
SHALL conservar sepia en su cabecera, su vista previa y su exportación. Un
proyecto guardado por una versión anterior (sin el campo `colorFilter`)
SHALL abrirse con todos sus clips en `NINGUNO`, sin necesidad de migración.

#### Scenario: Guardar y reabrir con filtro

- **WHEN** el usuario guarda un proyecto con un clip en `VINTAGE` y lo abre
  de nuevo
- **THEN** el clip conserva `VINTAGE` en la cabecera, la vista previa y la
  exportación

#### Scenario: Proyecto antiguo sin el campo filtro

- **WHEN** el usuario abre un proyecto guardado antes de esta función
- **THEN** todos sus clips se muestran con `colorFilter = NINGUNO`
