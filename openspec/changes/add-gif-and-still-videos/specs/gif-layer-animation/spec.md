## Purpose

Permite usar GIF animados como capa: el GIF conserva su movimiento en la vista
previa y en el MP4 exportado, sin importar cómo se llamó al archivo al
importarlo ni desde qué aplicación se compartió.

## ADDED Requirements

### Requirement: Capa de GIF animado
VidCam SHALL reconocer un GIF por el contenido del archivo (no por su nombre) al
agregar una imagen como capa, y SHALL tratarlo como capa animada
(`LayerKind.GIF`).

#### Scenario: Compartir un GIF desde otra app
- **WHEN** el usuario comparte un GIF con VidCam desde WhatsApp o la galería
- **THEN** el GIF entra como capa animada sobre el lienzo

#### Scenario: Elegir un GIF desde el selector de imágenes
- **WHEN** el usuario elige un GIF con el botón "PNG"
- **THEN** la capa queda animada, aunque el archivo guardado se llame `.png`

#### Scenario: Una imagen fija sigue siendo fija
- **WHEN** el usuario agrega una foto JPEG
- **THEN** la capa es estática, como antes

### Requirement: El GIF se reproduce en la vista previa
La capa GIF SHALL mostrar el fotograma que corresponde al instante que se está
viendo, de modo que el movimiento se aprecie al reproducir la vista previa y en
el lienzo de solo imágenes.

#### Scenario: Reproducción del movimiento
- **WHEN** el usuario reproduce la vista previa con una capa GIF visible
- **THEN** la capa cambia de fotograma siguiendo el tiempo del vídeo

### Requirement: El GIF se reproduce en el MP4 exportado
El MP4 exportado SHALL contener la animación del GIF, con el mismo fotograma
que la vista previa para cada instante.

#### Scenario: Exportar con GIF
- **WHEN** el usuario exporta un proyecto con una capa GIF
- **THEN** el vídeo resultante muestra el GIF animado durante toda la ventana
  en pantalla de la capa

### Requirement: Recursos acotados
La rasterización del GIF SHALL acotar el número de fotogramas y su tamaño, y
SHALL reutilizar el resultado entre la vista previa y la exportación.

#### Scenario: GIF grande
- **WHEN** el usuario agrega un GIF de gran resolución
- **THEN** los fotogramas se reducen a un lado máximo de 384 px y a 24
  fotogramas, y la app sigue respondiendo
