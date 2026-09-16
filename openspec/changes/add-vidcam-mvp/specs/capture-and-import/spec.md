## Purpose

Permite grabar vídeos verticales de hasta 30 segundos, importar vídeos existentes del teléfono y elegir una pista de música desde la biblioteca de medios del dispositivo.

## ADDED Requirements

### Requirement: Grabación de vídeo de hasta 30 segundos
La aplicación DEBE (MUST) grabar vídeo en orientación vertical (9:16) con la cámara trasera o frontal y DEBE (MUST) detener automáticamente la grabación al alcanzar 30 segundos.

#### Scenario: Grabación completa dentro del límite
- **WHEN** el usuario inicia la grabación y la detiene antes de 30 segundos
- **THEN** se crea un clip con la duración grabada y se agrega a la línea de tiempo

#### Scenario: Grabación que alcanza el límite
- **WHEN** la grabación llega a 30 segundos
- **THEN** la captura se detiene sola, se conserva el clip completo y la interfaz avisa que se alcanzó el máximo

#### Scenario: Cambio de cámara frontal a trasera
- **WHEN** el usuario alterna la cámara antes o durante la grabación
- **THEN** la vista previa cambia a la cámara seleccionada y la grabación usa esa cámara

### Requirement: Importación de vídeos del teléfono
La aplicación DEBE (MUST) permitir seleccionar uno o más vídeos existentes del dispositivo y agregarlos a la línea de tiempo.

#### Scenario: Importar un vídeo
- **WHEN** el usuario elige un vídeo desde el selector de medios
- **THEN** el vídeo se agrega como clip a la línea de tiempo con su duración visible

#### Scenario: Vídeo importado mayor a 30 segundos
- **WHEN** el usuario importa un vídeo de duración mayor a 30 segundos
- **THEN** la aplicación lo incorpora y notifica que será recortado a 30 segundos en la línea de tiempo

### Requirement: Biblioteca de música del dispositivo
La aplicación DEBE (MUST) listar las pistas de audio disponibles en la biblioteca de medios del dispositivo con título, artista y duración, y DEBE (MUST) permitir elegir una como pista musical del proyecto.

#### Scenario: Elegir una canción
- **WHEN** el usuario abre la biblioteca de música y selecciona una pista
- **THEN** la pista queda asignada como audio del proyecto y se refleja en la línea de tiempo

#### Scenario: Biblioteca sin pistas
- **WHEN** el dispositivo no tiene pistas de audio accesibles
- **THEN** la aplicación muestra un mensaje vacío explicativo y permite continuar en silencio
