## Purpose

Permite grabar y editar vídeos en cámara rápida o cámara lenta eligiendo la
velocidad de reproducción de cada clip, con efecto en la duración de la línea
de tiempo, la vista previa, la exportación MP4 y los proyectos guardados.

## ADDED Requirements

### Requirement: Velocidad por clip
Cada clip de la línea de tiempo SHALL tener una velocidad de reproducción
seleccionable entre 0.25x (cámara lenta), 0.5x, 1x (normal), 2x y 4x (cámara
rápida). La velocidad por defecto de un clip nuevo SHALL ser 1x. La duración
que el clip ocupa en la línea de tiempo SHALL ser su duración recortada
dividida por la velocidad, y el límite de 30 segundos SHALL medirse sobre esa
duración efectiva.

#### Scenario: Velocidad al grabar
- **WHEN** el usuario elige 2x y graba un clip de 6 segundos
- **THEN** el clip entra a la línea de tiempo con velocidad 2x y ocupa 3
  segundos de timeline

#### Scenario: Velocidad al importar
- **WHEN** el usuario importa un clip de 10 segundos y le asigna 0.5x
- **THEN** el clip ocupa 20 segundos de timeline

#### Scenario: Duración efectiva dentro del límite
- **WHEN** la suma de duraciones efectivas de todos los clips supera los 30
  segundos al cambiar la velocidad de un clip
- **THEN** el recorte del clip se ajusta al final para que la duración
  efectiva total no supere 30 segundos

#### Scenario: Clip normal por defecto
- **WHEN** se graba o importa un clip sin tocar la velocidad
- **THEN** el clip se reproduce a 1x y ocupa exactamente su duración
  recortada

### Requirement: Selector de velocidad en cámara
Cuando la línea de tiempo está vacía y la cámara está disponible, el selector
de velocidad SHALL mostrarse junto a los controles de grabación y el valor
elegido SHALL aplicarse al clip grabado a continuación.

#### Scenario: Grabar en cámara lenta
- **WHEN** el usuario selecciona 0.5x y pulsa Grabar
- **THEN** al detener, el clip grabado queda en la línea de tiempo con
  velocidad 0.5x

### Requirement: Vista previa a la velocidad del clip
La vista previa SHALL reproducir cada clip a su velocidad, de modo que un clip
lento se reproduzca ralentizado y uno rápido acelerado, con la música y las
capas sincronizadas sobre la duración efectiva de la línea de tiempo.

#### Scenario: Reproducción de un clip rápido
- **WHEN** el timeline reproduce un clip con velocidad 4x
- **THEN** la imagen del clip avanza 4 veces más rápido que el tiempo real

#### Scenario: Reproducción de un clip lento
- **WHEN** el timeline reproduce un clip con velocidad 0.25x
- **THEN** la imagen del clip avanza 4 veces más lento que el tiempo real

### Requirement: Exportación con cámara rápida y lenta
El MP4 exportado SHALL reproducir cada clip a su velocidad, incluido el audio,
conservándose el efecto en la duración total del vídeo de salida.

#### Scenario: Exportar un proyecto con cámara rápida
- **WHEN** el usuario exporta un proyecto cuyo único clip es de 10 s a 2x
- **THEN** el MP4 de salida dura 5 s y el contenido avanza a cámara rápida

#### Scenario: Exportar capas animadas
- **WHEN** el usuario exporta un proyecto con capas animadas y clips con
  velocidad distinta de 1x
- **THEN** las capas siguen sincronizadas con el timeline del proyecto

### Requirement: Persistencia de la velocidad
La velocidad de cada clip SHALL guardarse en el JSON del proyecto y
restaurarse al abrirlo. Un proyecto guardado por una versión anterior, sin
información de velocidad, SHALL abrirse con todos los clips a 1x.

#### Scenario: Guardar y volver a abrir
- **WHEN** el usuario guarda un proyecto con un clip a 0.25x y lo abre de nuevo
- **THEN** el clip conserva la velocidad 0.25x

#### Scenario: Proyecto antiguo sin velocidad
- **WHEN** el usuario abre un proyecto guardado antes de esta función
- **THEN** todos sus clips se interpretan a velocidad 1x