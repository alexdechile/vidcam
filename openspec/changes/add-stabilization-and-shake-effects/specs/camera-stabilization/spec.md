# Camera Stabilization Specification

## Requirements

### Requirement: Estabilización de vídeo en grabación de cámara
La aplicación DEBE (MUST) permitir activar o desactivar la estabilización de imagen al grabar vídeo con la cámara, aplicando los modos de estabilización electrónica (EIS) y óptica (OIS) compatibles con el hardware del dispositivo.

#### Scenario: Activar estabilizador de cámara
- **GIVEN** que la cámara está lista para grabar
- **WHEN** el usuario activa la opción de estabilizador
- **THEN** la grabación se realiza con la estabilización de vídeo del dispositivo habilitada

#### Scenario: Desactivar estabilizador de cámara
- **GIVEN** que el estabilizador está activado
- **WHEN** el usuario desactiva la opción
- **THEN** la cámara vuelve a su modo estándar sin estabilización forzada
