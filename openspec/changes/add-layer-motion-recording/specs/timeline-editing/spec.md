## MODIFIED Requirements

### Requirement: Capas de PNG con transparencia

La aplicación DEBE (MUST) permitir agregar una o más imágenes PNG con
transparencia como capas superpuestas, con control de posición, escala y
duración, y DEBE (MUST) respetar el canal alfa en la vista previa y en la
exportación. Los gestos de mover, escalar y rotar DEBEN (MUST) reconocerse sobre
el lienzo completo —no solo sobre el área ocupada por la capa—, de modo que un
dedo arrastre la capa seleccionada, dos dedos la escalen y roten con
independencia de dónde caigan los dedos, y un toque seleccione la capa bajo el
dedo.

#### Scenario: Agregar un sticker PNG transparente

- **WHEN** el usuario agrega un PNG desde el conjunto integrado o desde la galería
- **THEN** el PNG aparece sobre el vídeo, se puede mover y escalar, y las zonas transparentes dejan ver el vídeo de fondo

#### Scenario: Cambiar la duración de una capa

- **WHEN** el usuario ajusta la duración de una capa PNG
- **THEN** la capa aparece solo durante el intervalo configurado en la vista previa

#### Scenario: Posición consistente entre edición y exportación

- **WHEN** el usuario coloca una capa y exporta el vídeo
- **THEN** la capa aparece en la misma posición relativa del fotograma que en la vista previa, aunque la relación de aspecto del área de vista previa sea distinta a la del vídeo

#### Scenario: Pellizco con los dedos fuera de la capa

- **WHEN** el usuario apoya dos dedos sobre el lienzo y al menos uno de ellos cae fuera del área ocupada por la capa seleccionada
- **THEN** la capa igualmente se escala y rota siguiendo el movimiento de ambos dedos

#### Scenario: Seleccionar una capa con un toque

- **WHEN** el usuario toca una capa sin desplazar el dedo
- **THEN** esa capa queda seleccionada y resaltada, y pasa a ser la capa que responden los gestos posteriores
