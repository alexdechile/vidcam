## ADDED Requirements

### Requirement: Abrir el vídeo exportado en la galería
Después de guardar el vídeo exportado en la galería, la aplicación DEBE (MUST)
abrirlo en el visor de vídeo del sistema; si ninguna aplicación puede abrirlo,
DEBE (MUST) conservar el archivo guardado e informar por el aviso de la
aplicación.

#### Scenario: Apertura tras exportar
- **WHEN** la exportación termina correctamente y el vídeo se guarda en la galería
- **THEN** la aplicación abre el vídeo guardado en el visor del sistema y muestra un aviso de que quedó guardado

#### Scenario: Sin visor disponible
- **WHEN** la exportación termina y ningún visor puede abrir el vídeo guardado
- **THEN** el vídeo permanece en la galería y la aplicación solo informa que se guardó
