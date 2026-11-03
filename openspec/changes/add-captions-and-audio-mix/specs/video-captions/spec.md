## Purpose

Permite poner los subtítulos del vídeo encima de la imagen, a mano o a partir de
la voz en off, con un estilo que se quema en el MP4 y se ve igual en la vista
previa.

## ADDED Requirements

### Requirement: Subtítulos con tiempos
VidCam SHALL permitir crear, editar y borrar subtítulos, cada uno con su texto y
su tramo de tiempo dentro del vídeo.

#### Scenario: Agregar un subtítulo a mano
- **WHEN** el usuario presiona "Agregar" en la hoja de subtítulos y escribe el texto
- **THEN** el subtítulo aparece en la vista previa cuando el vídeo pasa por su tramo

#### Scenario: Editar tiempos
- **WHEN** el usuario cambia la entrada o la salida de un subtítulo
- **THEN** el subtítulo se ve en el nuevo tramo, sin solaparse con el anterior

#### Scenario: Quitar un subtítulo
- **WHEN** el usuario presiona "Quitar" en un subtítulo
- **THEN** deja de verse en la vista previa y no se exporta

### Requirement: Estilo común de los subtítulos
Los subtítulos SHALL compartir un estilo con tamaño, altura en pantalla,
mayúsculas y fondo, ajustable y aplicado al instante.

#### Scenario: Cambiar el tamaño
- **WHEN** el usuario mueve el control de tamaño
- **THEN** el texto cambia de tamaño en la vista previa y ese mismo tamaño se
  quema en el MP4

#### Scenario: Subir o bajar el bloque
- **WHEN** el usuario mueve el control de altura
- **THEN** el bloque de texto se mueve, sin salirse del fotograma

### Requirement: Transcribir la voz en off
VidCam SHALL poder convertir la toma de voz en subtítulos con sus tiempos,
usando un servicio de transcripción configurable.

#### Scenario: Transcribir con el servicio configurado
- **WHEN** el usuario presiona "Transcribir voz" con una toma y un servicio configurado
- **THEN** se crean subtítulos con el texto y los tiempos que devuelve el servicio

#### Scenario: Sin servicio configurado
- **WHEN** el usuario presiona "Transcribir voz" sin URL configurada
- **THEN** se le avisa que configure el servicio y el proyecto no cambia

#### Scenario: El servicio solo devuelve texto
- **WHEN** la respuesta no trae tiempos
- **THEN** el texto se reparte en tramos proporcionales a la duración de la toma

#### Scenario: La toma está adelantada o retrasada
- **WHEN** la voz tiene entrada o recorte configurados
- **THEN** los subtítulos se desplazan con ella, no se quedan en el segundo cero

### Requirement: Los subtítulos se exportan
El MP4 SHALL incluir los subtítulos activos en cada instante, con el mismo
tamaño y posición que la vista previa.

#### Scenario: Exportar con subtítulos
- **WHEN** el usuario exporta un proyecto con subtítulos
- **THEN** el MP4 muestra cada subtítulo durante su tramo

#### Scenario: Proyecto sin subtítulos
- **WHEN** el proyecto no tiene subtítulos
- **THEN** la exportación funciona y no se dibuja nada encima
