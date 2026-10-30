## Purpose

Permite crear y exportar un vídeo sin ningún clip: el lienzo compuesto por
fondos, capas PNG, textos, GIF y música se convierte en un MP4 vertical de la
duración fijada.

## ADDED Requirements

### Requirement: Exportar sin clips
VidCam SHALL permitir exportar un proyecto sin clips cuando tenga al menos un
fondo o una capa, usando la duración fijada del proyecto como duración del
vídeo.

#### Scenario: Proyecto solo con PNG y texto
- **WHEN** el usuario agrega un fondo, una capa PNG y un texto, y pulsa "Exportar"
- **THEN** la app produce un MP4 de la duración fijada con ese lienzo

#### Scenario: Proyecto vacío
- **WHEN** el proyecto no tiene clips, fondos ni capas
- **THEN** "Exportar" está deshabilitado y no se genera ningún archivo

#### Scenario: GIF sobre lienzo de imágenes
- **WHEN** el proyecto de solo imágenes tiene una capa GIF
- **THEN** el MP4 muestra el GIF animado durante su ventana en pantalla

### Requirement: Lienzo de fondo negro cuando no hay fondo elegido
Si el proyecto no tiene fondos, el vídeo de imágenes SHALL usar un lienzo negro
del tamaño del fotograma de salida, de modo que las capas se vean igual que en
la vista previa.

#### Scenario: Solo una capa PNG
- **WHEN** el usuario agrega una capa PNG sin fondo y exporta
- **THEN** el MP4 muestra la capa sobre fondo negro, a pantalla completa vertical

### Requirement: Música en el vídeo de imágenes
La música activa SHALL acompañar al vídeo sin clips, repitiéndose en bucle si
es más corta que la duración fijada.

#### Scenario: Música más corta que el vídeo
- **WHEN** el usuario elige una pista de 3 s y un vídeo de imágenes de 10 s
- **THEN** el MP4 dura 10 s y la música se repite durante todo el vídeo

### Requirement: Vista previa del lienzo sin clips
Sin clips, la vista previa SHALL mostrar el lienzo de imágenes (fondo, capas y
GIF) con el movimiento de las capas y de los GIF, y SHALL permitir mover las
capas con los gestos de siempre.

#### Scenario: Ver el lienzo antes de exportar
- **WHEN** el usuario agrega un fondo y una capa sobre un proyecto sin clips
- **THEN** la vista previa muestra el lienzo compuesto en vez de la cámara

#### Scenario: Mover una capa sin clips
- **WHEN** el usuario arrastra una capa en el lienzo sin clips
- **THEN** la capa se mueve igual que con un vídeo en la línea de tiempo

### Requirement: Resumen de exportación sin clips
El diálogo de vista previa de exportación SHALL mostrar el lienzo compuesto y
la duración del vídeo cuando el proyecto no tiene clips, sin quedarse esperando
un fotograma que no existe.

#### Scenario: Abrir el resumen
- **WHEN** el usuario pulsa "Exportar" en un proyecto solo con imágenes
- **THEN** el diálogo muestra el fondo, las capas visibles y la duración fijada
