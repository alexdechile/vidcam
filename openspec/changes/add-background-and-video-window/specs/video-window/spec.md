## Purpose

Define una ventana de vídeo global, móvil, escalable, girable y con forma 1:1, 9:16 u
original, que se compone encima del fondo del proyecto y por debajo de las capas.

## ADDED Requirements

### Requirement: Forma de la ventana de vídeo

El sistema SHALL ofrecer tres formas para la ventana de vídeo: `ORIGINAL` (proporción del
primer clip), `CUADRADO` (1:1) y `VERTICAL` (9:16). El vídeo MUST recortarse por el centro
(*cover*) para llenar la forma elegida, sin deformarse. Con `ORIGINAL` y escala 1 la ventana
MUST ocupar el lienzo completo, como antes de esta función.

#### Scenario: Cambiar a 1:1
- **WHEN** el usuario elige la forma 1:1
- **THEN** el vídeo se muestra recortado en un cuadrado centrado, sin deformación

#### Scenario: Cambiar a 9:16
- **WHEN** el usuario elige la forma 9:16
- **THEN** el vídeo se muestra recortado en un rectángulo vertical centrado

#### Scenario: Volver a original
- **WHEN** el usuario elige la forma original con escala 1 y posición centrada
- **THEN** el vídeo vuelve a ocupar todo el lienzo

### Requirement: Posición, escala y rotación de la ventana

La ventana SHALL tener posición normalizada `(x, y)` con origen en la esquina superior
izquierda, escala entre 0.2 y 1 (donde 1 es el mayor tamaño que cabe en el lienzo para la
forma elegida) y rotación en grados. El centro MUST quedar dentro del lienzo. La ventana
MUST poder moverse arrastrando, escalarse con pellizco y girarse con dos dedos, y MUST
poder restablecerse a centrada, escala 1 y sin giro.

#### Scenario: Arrastrar la ventana
- **WHEN** el usuario arrastra el vídeo cuando no hay una capa en ese punto
- **THEN** la ventana se mueve siguiendo el dedo dentro del lienzo

#### Scenario: Pellizcar y girar
- **WHEN** el usuario hace pellizco o gira dos dedos sobre la ventana
- **THEN** la ventana cambia de escala o de rotación en tiempo real

#### Scenario: Restablecer
- **WHEN** el usuario toca restablecer la ventana
- **THEN** vuelve a estar centrada, con escala 1 y sin rotación

### Requirement: Prioridad entre ventana y capas

Los gestos sobre una capa PNG o texto MUST seguir actuando sobre la capa. La ventana de
vídeo MUST recibir el gesto solo cuando el toque no cae sobre una capa visible.

#### Scenario: Tocar una capa sobre el vídeo
- **WHEN** el usuario arrastra una capa PNG que está encima de la ventana
- **THEN** se mueve la capa y la ventana no cambia

#### Scenario: Tocar el vídeo sin capas
- **WHEN** el usuario arrastra en una zona del vídeo sin capas
- **THEN** se mueve la ventana de vídeo

### Requirement: Coherencia entre vista previa y exportación

La vista previa y la exportación MUST usar la misma geometría normalizada para posición,
escala, rotación y forma de la ventana, y el mismo recorte *cover* del vídeo. El tamaño y
la proporción del fotograma exportado MUST NOT cambiar por usar la ventana: siguen siendo
los del primer clip.

#### Scenario: Exportar con ventana 1:1 girada
- **WHEN** el usuario exporta un proyecto con la ventana en 1:1, desplazada y girada
- **THEN** el MP4 muestra la ventana en la misma posición, tamaño y giro que la vista previa

#### Scenario: Fondo detrás de la ventana
- **WHEN** hay un fondo y la ventana no cubre todo el lienzo
- **THEN** el fondo se ve alrededor de la ventana, delante de nada más y detrás de las capas

### Requirement: Persistencia y deshacer de la ventana

La posición, escala, rotación y forma de la ventana MUST guardarse en el JSON del proyecto,
MUST participar de deshacer/rehacer y MUST cargarse con los valores por defecto
(original, centrada, escala 1, sin giro) cuando el proyecto no las tenga.

#### Scenario: Guardar y abrir con ventana modificada
- **WHEN** el usuario guarda un proyecto con la ventana en 1:1, movida y girada, y lo abre
- **THEN** la ventana conserva forma, posición, escala y giro

#### Scenario: Deshacer un cambio de ventana
- **WHEN** el usuario deshace después de mover o cambiar la forma de la ventana
- **THEN** la ventana vuelve al estado anterior

#### Scenario: Proyecto anterior sin ventana
- **WHEN** se abre un JSON creado antes de esta función
- **THEN** la ventana queda en original, centrada, escala 1 y sin giro
