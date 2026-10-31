## Purpose

Permite ajustar el volumen de cada pista del montaje (voz en off, audio original
y música) en la vista previa y en el MP4 exportado, en vez de solo silenciar o
dejar el audio a volumen completo.

## ADDED Requirements

### Requirement: Volumen del audio original
VidCam SHALL permitir ajustar el volumen del audio de los clips entre 0 y 1, y
SHALL aplicarlo en la vista previa y en la exportación.

#### Scenario: Bajar el audio original bajo la voz
- **WHEN** el usuario baja "Volumen del audio original" a 0,4
- **THEN** la vista previa y el MP4 reproducen ese audio al 40 %

#### Scenario: Silenciar sigue funcionando
- **WHEN** el usuario silencia el audio original
- **THEN** el MP4 se exporta sin pista de audio del clip, como antes

### Requirement: Volumen de la música
VidCam SHALL permitir ajustar el volumen de la música entre 0 y 1, y SHALL
aplicarlo en la vista previa y en la exportación.

#### Scenario: Música de fondo
- **WHEN** el usuario baja la música a 0,3
- **THEN** la música suena al 30 % en la vista previa y en el MP4

#### Scenario: Música en cero
- **WHEN** el usuario deja la música en 0
- **THEN** la música no entra a la mezcla exportada

### Requirement: Volumen de la voz en off
VidCam SHALL permitir ajustar el volumen de la toma de voz entre 0 y 1, y SHALL
aplicarlo en la vista previa y en la exportación.

#### Scenario: Subir la voz sobre la música
- **WHEN** el usuario pone la voz a 1 y la música a 0,3
- **THEN** en el MP4 la voz se impone sobre la música

#### Scenario: Voz en cero
- **WHEN** el usuario deja el volumen de la voz en 0
- **THEN** la toma no entra a la mezcla exportada

### Requirement: Valores por defecto compatibles
Los proyectos guardados antes de este cambio SHALL cargar con volumen 1 en todas
las pistas y SHALL exportarse igual que antes.

#### Scenario: Abrir un proyecto viejo
- **WHEN** el usuario abre un proyecto guardado con la versión anterior
- **THEN** los volúmenes aparecen al máximo y el audio suena como antes
