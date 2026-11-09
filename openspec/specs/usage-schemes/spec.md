# usage-schemes Specification

## Purpose
Permite definir, serializar, almacenar y aplicar esquemas de uso y plantillas temporales divididas en etapas modulares para componer vídeos dinámicos en VidCam.

## Requirements

### Requirement: Stage-based Timing Calculation
The system MUST calculate start (`startMs`) and end (`endMs`) timestamps for each element based on total scheme duration, number of stages, and stage interval (`stageStart` to `stageEnd`).

#### Scenario: Cálculo de tiempos para esquema de 5 etapas y 15 segundos
- **WHEN** se define un esquema con `totalDurationSec = 15` y `stagesCount = 5` (3 segundos por etapa)
- **THEN** la duración de cada etapa es de 3000 ms
- **THEN** un elemento con `stageStart = 1` y `stageEnd = 2` obtiene `startMs = 0` y `endMs = 6000`
- **THEN** un elemento con `stageStart = 2` y `stageEnd = 4` obtiene `startMs = 3000` y `endMs = 12000`
- **THEN** un elemento con `stageStart = 3` y `stageEnd = 5` obtiene `startMs = 6000` y `endMs = 15000`
- **THEN** el fondo con `stageStart = 1` y `stageEnd = 5` cubre de 0 a 15000 ms

#### Scenario: Rango de etapas inválido o fuera de límites
- **WHEN** un elemento declara `stageStart < 1` o `stageEnd > stagesCount` o `stageStart > stageEnd`
- **THEN** el sistema acota los valores al rango válido `1..stagesCount` garantizando una duración mínima de al menos una etapa

### Requirement: JSON Serialization and Portability
The system MUST serialize and deserialize usage schemes to and from portable JSON format without absolute device-specific local paths.

#### Scenario: Serialización y deserialización de esquema
- **WHEN** se serializa un `UsageScheme` con fondo animado, slots de imagen con transiciones y animación procedimental, y ajustes de audio
- **THEN** el JSON resultante contiene los atributos de etapas, transiciones (`LayerTransitionPreset`), presets de animación (`PngAnimationPreset`, `KenBurnsMode`) y velocidad (`AnimationSpeed`)
- **THEN** la deserialización recupera exactamente la misma configuración sin perder campos

#### Scenario: Esquema con claves desconocidas o futuras
- **WHEN** se deserializa un JSON con propiedades adicionales de versiones futuras
- **THEN** el parser las ignora de forma segura y carga el esquema correctamente

### Requirement: Schemes Persistence and Catalog
The system MUST provide a catalog of factory templates and allow listing, loading, saving, and deleting custom user schemes.

#### Scenario: Listado de plantillas de fábrica
- **WHEN** el usuario consulta las plantillas disponibles
- **THEN** la lista incluye la plantilla de fábrica "5 Etapas en Cascada (15s)" con slots para intro, desarrollo y cierre

#### Scenario: Guardado y borrado de plantilla personalizada
- **WHEN** el usuario guarda un nuevo esquema con un nombre descriptivo
- **THEN** el archivo JSON se persiste en la carpeta de plantillas del almacenamiento de la app
- **WHEN** el usuario solicita eliminar el esquema guardado
- **THEN** el archivo correspondiente se elimina y la lista se actualiza

### Requirement: Applying Scheme to Editor Project
The system MUST allow applying an existing scheme onto the active editor project, configuring target duration, layers, and background according to defined slots, binding actual media assets (images, backgrounds, texts) and preserving existing assets through intelligent mapping.

#### Scenario: Aplicar esquema a proyecto vacío
- **WHEN** el usuario selecciona un esquema de 15 segundos y 5 etapas en un proyecto sin capas
- **THEN** el proyecto fija `targetDurationMs = 15000`
- **THEN** se crean las capas correspondientes a cada slot con sus tiempos (`startMs`, `endMs`), etiquetas, animaciones y transiciones
- **THEN** se añade el fondo con el modo Ken Burns y velocidad configurados en el esquema

#### Scenario: Aplicar esquema conservando clips de vídeo existentes
- **WHEN** el usuario aplica un esquema sobre un proyecto que ya contiene clips grabados
- **THEN** los clips de vídeo se conservan intactos
- **THEN** la ventana de vídeo y las capas decorativas se adaptan a la coreografía del esquema

#### Scenario: Aplicar esquema vinculando objetos reales vía SchemeBinding
- **WHEN** el usuario aplica un esquema proporcionando una vinculación con URIs de fondo y capas PNG
- **THEN** las capas creadas reciben sus URIs correspondientes vinculadas a los archivos locales reales
- **THEN** el fondo adopta la URI asignada manteniendo la coreografía y modo Ken Burns del esquema

### Requirement: Create Scheme from Current Project
The system MUST allow generating a new usage scheme from the current editor project composition, sanitizing private device paths.

#### Scenario: Guardar proyecto actual como esquema
- **WHEN** el usuario elige "Guardar como esquema" en un proyecto con capas animadas y fondos
- **THEN** el sistema calcula la cantidad de etapas más adecuada o usa la duración fijada
- **THEN** convierte las capas en slots de plantilla eliminando URIs privados de archivos locales
- **THEN** genera y guarda el JSON de esquema listo para ser reutilizado

### Requirement: Interactive Slot Binding and Batch Fill
The system MUST provide an interactive binding workflow that auto-maps existing project assets to scheme slots, supports individual assignment or batch multi-image selection, and offers an optional post-application voiceover step.

#### Scenario: Auto-mapeo inteligente con capas y fondos existentes
- **WHEN** el usuario abre la vinculación de un esquema en un proyecto que ya contiene un fondo y capas PNG
- **THEN** el fondo existente se pre-asigna automáticamente al slot de fondo del esquema
- **THEN** las capas PNG existentes se asignan en orden cronológico a los slots de imagen del esquema
- **THEN** el usuario puede reasignar o modificar cada slot individualmente

#### Scenario: Selección múltiple en lote para slots de imagen
- **WHEN** el usuario utiliza la acción de selección en lote y escoge múltiples imágenes desde el selector del sistema
- **THEN** las imágenes seleccionadas se asignan secuencialmente a los slots de imagen pendientes del esquema

#### Scenario: Paso opcional de doblaje de voz en off
- **WHEN** el usuario marca la opción de grabar voz en off al completar la vinculación y aplica el esquema
- **THEN** el proyecto se actualiza con la coreografía y elementos vinculados
- **THEN** el sistema abre inmediatamente la interfaz de grabación de voz en off sincronizada con la vista previa
