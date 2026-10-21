# Clip Shake Effect Specification

## Requirements

### Requirement: Efecto de temblor procedimental por clip
La aplicación DEBE (MUST) permitir seleccionar un efecto de temblor dinámico para cada clip, con intensidades `NINGUNO`, `SUAVE`, `MEDIO` y `FUERTE`. El efecto DEBE reflejarse de forma continua y sincronizada tanto en la vista previa del editor como en el vídeo MP4 exportado.

#### Scenario: Aplicar temblor a un clip en el editor
- **GIVEN** un clip existente en la línea de tiempo
- **WHEN** el usuario selecciona una intensidad de temblor distinta de NINGUNO
- **THEN** la reproducción en la vista previa y el archivo exportado muestran la vibración de la imagen con la escala de compensación apropiada

#### Scenario: Conservación y deserialización
- **GIVEN** un proyecto guardado con intensidades de temblor configuradas
- **WHEN** el proyecto se abre nuevamente
- **THEN** cada clip conserva su nivel de temblor configurado
- **AND** los proyectos anteriores sin este campo cargan con intensidad NINGUNO
