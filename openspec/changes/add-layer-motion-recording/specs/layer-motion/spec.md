## Purpose

Permite animar las capas del proyecto grabando su movimiento en el tiempo, verlo
en la vista previa y conservarlo en el vídeo exportado.

## ADDED Requirements

### Requirement: Keyframes de transformación por capa

La aplicación DEBE (MUST) permitir que cada capa tenga cero o más fotogramas
clave a lo largo de la línea de tiempo, y cada fotograma clave DEBE (MUST)
almacenar posición, escala y rotación en un instante concreto. Cuando una capa
tiene fotogramas clave, la vista previa y la exportación DEBEN (MUST) interpolar
suavemente el transform entre claves consecutivas; antes del primer fotograma
clave y después del último, la capa DEBE (MUST) mantener el valor del extremo
correspondiente.

#### Scenario: Interpolación entre dos fotogramas clave

- **WHEN** una capa tiene un fotograma clave en t=0 s y otro en t=4 s con
  posiciones distintas
- **THEN** en t=2 s la capa se dibuja en una posición intermedia entre ambas,
  tanto en la vista previa como en el vídeo exportado

#### Scenario: Capa sin fotogramas clave

- **WHEN** el usuario no ha grabado ni añadido fotogramas clave a una capa
- **THEN** la capa conserva su posición, escala y rotación fijas durante todo el
  intervalo en que está visible, como hasta ahora

### Requirement: Grabación de movimiento en tiempo real

La aplicación DEBE (MUST) ofrecer un modo de grabación de movimiento en el que,
mientras el vídeo se reproduce, los gestos que el usuario aplica sobre una capa
se registren como fotogramas clave en el tiempo de reproducción correspondiente.
Durante la grabación DEBE (MUST) mostrarse un indicador visible de que se está
grabando, y el usuario DEBE (MUST) poder detenerla en cualquier momento.

#### Scenario: Grabar un arrastre durante la reproducción

- **WHEN** el usuario activa el modo de grabación, el vídeo se reproduce y
  arrastra una capa a lo largo de la pantalla
- **THEN** al detener la grabación la capa tiene fotogramas clave repartidos en
  el tiempo que reproducen ese recorrido

#### Scenario: Escalar y rotar mientras se graba

- **WHEN** el usuario pellizca una capa mientras la grabación de movimiento está
  activa
- **THEN** los cambios de escala y rotación quedan registrados como fotogramas
  clave en el tiempo en que ocurrieron

#### Scenario: Detener la grabación

- **WHEN** el usuario detiene la grabación de movimiento
- **THEN** la grabación termina, el indicador desaparece y los gestos posteriores
  ya no crean fotogramas clave

### Requirement: Reproducción animada en la vista previa

La aplicación DEBE (MUST) reproducir en la vista previa la animación de cada capa
sincronizada con el cabezal de reproducción, de forma que el movimiento grabado
se vea tal como quedará en el vídeo.

#### Scenario: Vista previa del movimiento grabado

- **WHEN** una capa tiene fotogramas clave y el usuario reproduce la línea de
  tiempo
- **THEN** la capa se mueve, escala y rota siguiendo la animación al ritmo del
  vídeo

### Requirement: Exportación fiel del movimiento

La exportación DEBE (MUST) evaluar la animación de cada capa en cada fotograma
del vídeo de salida, de modo que el movimiento grabado aparezca en el MP4 con la
misma trayectoria, escala y rotación que en la vista previa.

#### Scenario: Movimiento presente en el archivo exportado

- **WHEN** el usuario graba el movimiento de una capa y exporta el proyecto
- **THEN** el MP4 resultante muestra la capa recorriendo la misma trayectoria y
  con la misma variación de tamaño y rotación que en la vista previa

### Requirement: Gestión de la animación de una capa

La aplicación DEBE (MUST) permitir eliminar todos los fotogramas clave de una
capa para volver a su estado fijo, y DEBE (MUST) permitir borrar fotogramas clave
individuales. Cuando una capa ya tiene animación y el usuario ajusta su posición,
escala o rotación fuera del modo de grabación, la aplicación DEBE (MUST) aplicar
el cambio al fotograma clave del tiempo actual sin destruir el resto de la
animación.

#### Scenario: Borrar la animación de una capa

- **WHEN** el usuario pulsa la acción de borrar la animación de una capa animada
- **THEN** la capa vuelve a mostrarse fija en su última posición y deja de
  animarse en la vista previa y en la exportación

#### Scenario: Ajuste manual con animación existente

- **WHEN** una capa tiene fotogramas clave y el usuario la mueve con el cabezal
  detenido en t=3 s
- **THEN** el fotograma clave correspondiente a t=3 s refleja la nueva posición y
  el resto de la animación se conserva

#### Scenario: Borrar un fotograma clave concreto

- **WHEN** el usuario elimina una de las marcas de fotograma clave de la línea
  de tiempo
- **THEN** la capa deja de pasar por ese punto y la animación se recalcula con
  los fotogramas clave restantes
