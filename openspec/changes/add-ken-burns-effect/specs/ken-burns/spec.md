## Purpose

Da vida a las imágenes fijas del proyecto con el efecto Ken Burns: un zoom lento, con un
desplazamiento mínimo, que recorre toda la imagen mientras está en pantalla. La animación se
reparte sobre el tramo visible, así que su velocidad depende de la duración del vídeo, y el
resultado es idéntico en la vista previa y en el MP4 exportado, con clips o en un vídeo hecho solo
con imágenes.

## ADDED Requirements

### Requirement: Efecto Ken Burns por imagen

El sistema SHALL permitir elegir, para cada fondo PNG, uno de cuatro modos de Ken Burns:
`Sin zoom`, `Zoom in`, `Zoom out` y `Ken Burns auto`. La elección MUST guardarse en el JSON del
proyecto y MUST participar de deshacer/rehacer. Un fondo sin elección explícita MUST quedarse
quieto, como antes de esta función.

#### Scenario: Activar el efecto en un fondo
- **WHEN** el usuario toca `Zoom in` en la fila de un fondo
- **THEN** la vista previa muestra esa imagen ampliándose durante su tramo
- **AND** el ajuste se conserva al guardar y volver a abrir el proyecto

#### Scenario: Desactivar el efecto
- **WHEN** el usuario toca `Sin zoom` en un fondo
- **THEN** la imagen vuelve a verse quieta y encuadrada, como antes de la función

#### Scenario: Proyecto anterior sin el ajuste
- **WHEN** se abre un proyecto guardado antes de esta función
- **THEN** sus fondos cargan como `Sin zoom` y el proyecto se ve igual que antes

#### Scenario: Deshacer el cambio de efecto
- **WHEN** el usuario cambia el modo de un fondo y luego deshace
- **THEN** el fondo recupera el modo anterior

### Requirement: Zoom automático alternado

El modo `Ken Burns auto` MUST alternar entre acercar y alejar según el orden de la imagen en la
línea de tiempo, de modo que dos imágenes consecutivas no repitan el mismo movimiento. Un fondo
nuevo MUST entrar con `Ken Burns auto` activado.

#### Scenario: Un solo fondo
- **WHEN** el proyecto tiene un único fondo con `Ken Burns auto`
- **THEN** la imagen se acerca durante su tramo y termina ampliada

#### Scenario: Dos fondos consecutivos
- **WHEN** el proyecto tiene dos fondos consecutivos con `Ken Burns auto`
- **THEN** el primero se acerca y el segundo se aleja

### Requirement: La velocidad depende de la duración

El zoom total MUST repartirse sobre el tramo visible de la imagen, de modo que un tramo más largo
produzca un movimiento más lento y uno más corto un movimiento más rápido, sin controles de
velocidad adicionales. El movimiento MUST empezar y terminar con suavidad.

#### Scenario: Mismo efecto en vídeos de distinta duración
- **WHEN** el mismo fondo de 4 s y el mismo fondo de 20 s se comparan a los 2 s de su tramo
- **THEN** el de 4 s ya recorrió la mitad del zoom y el de 20 s solo una pequeña parte

#### Scenario: Inicio y fin suaves
- **WHEN** empieza o termina el tramo de una imagen con Ken Burns
- **THEN** el zoom arranca y termina sin saltos

### Requirement: La imagen siempre cubre el lienzo

El efecto MUST NOT dejar franjas negras ni descubrir el borde de la imagen: el desplazamiento MUST
quedar acotado al margen que deja el propio zoom. Con zoom 1 el desplazamiento MUST ser nulo.

#### Scenario: Desplazamiento al máximo zoom
- **WHEN** la imagen está en su zoom máximo
- **THEN** el desplazamiento no supera `(escala - 1) / 2` y el encuadre sigue cubierto

#### Scenario: Fin del alejamiento
- **WHEN** un `Zoom out` termina
- **THEN** la imagen queda encuadrada, sin desplazamiento

### Requirement: Exportación equivalente a la vista previa

La exportación MUST aplicar el mismo zoom y el mismo desplazamiento que la vista previa, evaluados
por fotograma, tanto en proyectos con clips como en vídeos hechos solo con imágenes. Cuando ninguna
imagen tiene efecto, el MP4 MUST ser equivalente al de la versión anterior.

#### Scenario: Exportar con Ken Burns
- **WHEN** el usuario exporta un proyecto con fondos en `Zoom in`, `Zoom out` o `Ken Burns auto`
- **THEN** el MP4 muestra el mismo movimiento que la vista previa

#### Scenario: Exportar sin efecto
- **WHEN** el usuario exporta un proyecto cuyos fondos están en `Sin zoom`
- **THEN** el MP4 se ve como antes de esta función

#### Scenario: Vídeo solo con imágenes
- **WHEN** el proyecto no tiene clips y su fondo está en `Ken Burns auto`
- **THEN** el MP4 de la duración fijada muestra el zoom sobre el fondo

#### Scenario: Fondo con tramo recortado por un solape
- **WHEN** un fondo comparte parte de su tramo con otro de mayor apilamiento
- **THEN** el zoom se calcula sobre el tramo del fondo, igual que en la vista previa
