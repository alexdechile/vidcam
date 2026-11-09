# Spec Delta

## Purpose

Proporciona capacidades de doblaje automático de voz en off (ADT), transposición de tono y generación de murmullo multi-voz con control total del usuario para vídeos verticales en VidCam.

## ADDED Requirements

### Requirement: Presets de doblaje y murmullo
El sistema SHALL permitir al usuario seleccionar entre presets rápidos de doblaje automático para su toma de voz en off: Desactivado, Doble Grave, Doble Agudo, Estudio ADT y Murmullo de multitud.

#### Scenario: Selección de preset Doble Grave
- **WHEN** el usuario selecciona el preset "Doble Grave" en la hoja de voz en off
- **THEN** el sistema configura un clon de voz a -12 semitonos con micro-desfase temporal y ganancia equilibrada sin alterar la grabación cruda original

#### Scenario: Selección de preset Murmullo
- **WHEN** el usuario selecciona el preset "Murmullo"
- **THEN** el sistema configura entre 3 y 5 voces clonadas con desfases escalonados entre 20 ms y 70 ms y variaciones sutiles de tono para simular un ambiente de multitud

#### Scenario: Desactivación de doblaje
- **WHEN** el usuario elige el preset "Desactivado"
- **THEN** el sistema reproduce y exporta exclusivamente la toma de voz principal sin pistas adicionales de doblaje

### Requirement: Control granular de parámetros de voz
El sistema SHALL permitir al usuario personalizar libremente la cantidad de voces clonadas (1 a 5), el desplazamiento de tono (-12 a +12 semitonos), el retardo temporal (10 a 80 ms) y la proporción de mezcla (0 % a 100 %).

#### Scenario: Modificación manual de tono y número de voces
- **WHEN** el usuario ajusta el selector a 3 voces y el tono a +4 semitonos
- **THEN** el sistema cambia el modo a Personalizado y genera 3 capas duplicadas transpuestas con el intervalo especificado

#### Scenario: Ajuste de mezcla de doblaje
- **WHEN** el usuario reduce el nivel de mezcla al 30 %
- **THEN** las voces secundarias de doblaje o murmullo suenan atenuadas respecto a la voz principal

### Requirement: Generación y persistencia no destructiva
El sistema SHALL procesar el doblaje a partir del audio crudo o aislado de la toma sin sobreescribir el archivo de grabación original, y persistir la configuración de doblaje en el archivo del proyecto.

#### Scenario: Persistencia y soporte de deshacer
- **WHEN** el usuario modifica un parámetro de doblaje y luego pulsa Deshacer
- **THEN** el proyecto restaura el estado previo de configuración de doblaje y actualiza la pista de audio correspondiente

#### Scenario: Compatibilidad con proyectos existentes
- **WHEN** se carga un proyecto antiguo que no contiene configuración de doblaje
- **THEN** el sistema asume por defecto el estado Desactivado sin generar errores

### Requirement: Vista previa interactiva y exportación a MP4
El sistema SHALL reproducir la voz con su doblaje o murmullo aplicado durante la vista previa del editor y codificar la pista combinada en el archivo MP4 exportado.

#### Scenario: Reproducción en vista previa
- **WHEN** el usuario inicia la reproducción con el doblaje activo
- **THEN** el reproductor emite la voz principal junto a las voces secundarias dobladas en perfecta sincronía con la línea de tiempo del vídeo

#### Scenario: Exportación con música y ducking
- **WHEN** se exporta el vídeo con doblaje activo y música con ducking
- **THEN** la pista de voz procesada combinada activa la atenuación de la música y se mezcla limpiamente en el contenedor MP4
