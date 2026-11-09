# Spec Delta

## MODIFIED Requirements

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

## ADDED Requirements

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
