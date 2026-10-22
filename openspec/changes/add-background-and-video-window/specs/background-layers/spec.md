## Purpose

Permite usar imágenes PNG como fondo a pantalla completa en tramos de la línea de tiempo,
por debajo del vídeo y de las capas, tanto en la vista previa como en el MP4 exportado.

## ADDED Requirements

### Requirement: Fondo PNG a pantalla completa

El sistema SHALL permitir agregar una imagen PNG de la galería como fondo del proyecto.
El fondo MUST cubrir todo el lienzo recortando la imagen (*cover*), sin deformarla ni dejar
franjas vacías, y MUST dibujarse por debajo de la ventana de vídeo y de todas las capas
PNG/texto. En los instantes sin fondo el lienzo MUST verse negro, como hasta ahora.

#### Scenario: Agregar un fondo
- **WHEN** el usuario toca **Fondo**, elige una imagen PNG y confirma el tramo
- **THEN** la imagen cubre todo el lienzo en la vista previa durante ese tramo
- **AND** las capas PNG y textos existentes se siguen viendo por encima

#### Scenario: Imagen de fondo con otra proporción
- **WHEN** el PNG elegido no tiene la misma proporción que el lienzo
- **THEN** el sistema lo recorta por el centro para llenar el lienzo, sin deformarlo

#### Scenario: Fondo ilegible
- **WHEN** el archivo elegido no se puede decodificar como imagen
- **THEN** el sistema muestra un mensaje de error y el proyecto queda sin ese fondo

### Requirement: Varios fondos por tramos

El proyecto SHALL admitir varios fondos, cada uno con un tramo `[inicio, fin)` dentro de la
duración total. Los tramos MUST NOT solaparse: al agregar o mover un fondo sobre otro, el
sistema MUST recortar el tramo existente para eliminar el solapamiento. Un tramo MUST
tener duración mayor que cero.

#### Scenario: Dos fondos consecutivos
- **WHEN** el usuario agrega un fondo de 0 a 3 s y otro de 3 a 6 s
- **THEN** la vista previa muestra el primero hasta 3 s y el segundo desde 3 s

#### Scenario: Fondo que se solapa con otro
- **WHEN** el usuario agrega un fondo de 2 a 8 s sobre uno existente de 0 a 5 s
- **THEN** el fondo existente queda de 0 a 2 s y no hay solapamiento

#### Scenario: Tramo sin fondo
- **WHEN** la posición de reproducción cae en un instante sin fondo
- **THEN** el lienzo se muestra negro y el vídeo se dibuja encima según la ventana de vídeo

### Requirement: Edición de los tramos de fondo

El sistema SHALL permitir cambiar el inicio y el fin de cada fondo y eliminarlo. Al
eliminar un fondo, el tramo MUST quedar negro y el resto de fondos MUST conservarse.

#### Scenario: Cambiar el fin de un fondo
- **WHEN** el usuario edita el fin de un fondo y lo confirma
- **THEN** la vista previa y la lista de fondos reflejan el nuevo tramo

#### Scenario: Eliminar un fondo
- **WHEN** el usuario elimina un fondo
- **THEN** ese tramo deja de mostrar la imagen y los demás fondos no cambian

### Requirement: Persistencia y deshacer

Los fondos MUST guardarse en el JSON del proyecto y MUST participar de deshacer/rehacer.
Un proyecto guardado sin fondos MUST cargarse sin errores y sin fondos.

#### Scenario: Guardar y abrir
- **WHEN** el usuario guarda un proyecto con dos fondos y lo vuelve a abrir
- **THEN** ambos fondos conservan imagen y tramos

#### Scenario: Deshacer
- **WHEN** el usuario deshace después de agregar, editar o eliminar un fondo
- **THEN** el proyecto recupera el estado anterior de los fondos

#### Scenario: Proyecto anterior sin fondos
- **WHEN** se abre un JSON de proyecto creado antes de esta función
- **THEN** el proyecto carga sin fondos y el editor funciona como antes

### Requirement: Fondo en la exportación

La exportación MUST incluir los fondos con la misma imagen, tramo y recorte que la vista
previa. Cuando el proyecto no tiene fondos, la exportación MUST producir el mismo
resultado que antes de esta función.

#### Scenario: Exportar con fondos
- **WHEN** el usuario exporta un proyecto con fondos
- **THEN** el MP4 muestra cada fondo en su tramo, por debajo del vídeo y de las capas

#### Scenario: Exportar sin fondos
- **WHEN** el usuario exporta un proyecto sin fondos y con la ventana de vídeo por defecto
- **THEN** el MP4 es equivalente al que producía la versión anterior
