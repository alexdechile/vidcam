## Purpose

Da dinámica al sonido del vídeo: la música y la voz entran y salen con fundidos,
y la música baja sola mientras hay voz para que la narración se entienda.

## ADDED Requirements

### Requirement: Fundidos de entrada y salida
VidCam SHALL permitir fundir la música y la voz al entrar y al salir, con una
duración configurable, y SHALL aplicar el mismo resultado en la vista previa y
en el MP4.

#### Scenario: Fundir la música
- **WHEN** el usuario pone 800 ms de fundido de entrada en la música
- **THEN** la música arranca en silencio y sube hasta su volumen durante el
  primer 0,8 s, en la vista previa y en el MP4

#### Scenario: Fundir la voz
- **WHEN** el usuario pone un fundido de salida de 400 ms en la voz
- **THEN** la toma se apaga suavemente en sus últimos 0,4 s

#### Scenario: Fundidos en cero
- **WHEN** los fundidos están en 0
- **THEN** el audio suena como antes, de golpe

### Requirement: Ducking de la música
VidCam SHALL bajar automáticamente el volumen de la música mientras suena la voz
y SHALL devolverlo en los silencios, con el nivel de atenuación configurable.

#### Scenario: La música baja con la voz
- **WHEN** el usuario activa el ducking al 70 % y hay una toma de voz
- **THEN** la música baja mientras se habla y vuelve a su volumen en los silencios

#### Scenario: Sin voz grabada
- **WHEN** el proyecto no tiene toma de voz
- **THEN** el ducking no tiene efecto

#### Scenario: Vista previa coherente con el MP4
- **WHEN** el usuario reproduce la vista previa con fundidos y ducking
- **THEN** el cambio de volumen coincide con el del vídeo exportado

### Requirement: Compatibilidad con proyectos anteriores
Los proyectos guardados antes de este cambio SHALL cargar con fundidos en 0 y
ducking apagado, y exportarse igual que antes.

#### Scenario: Abrir un proyecto viejo
- **WHEN** el usuario abre un proyecto guardado con la versión anterior
- **THEN** el audio no tiene fundidos ni ducking y suena como antes
