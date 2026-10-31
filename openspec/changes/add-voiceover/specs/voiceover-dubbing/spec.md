## Purpose

Permite doblar un vídeo de VidCam con la propia voz: se graba la narración
después del montaje, se limpia el ruido de entorno y la toma se mezcla al
exportar y al ver la vista previa.

## ADDED Requirements

### Requirement: Grabar una voz en off sobre el montaje
VidCam SHALL permitir grabar una toma de voz sobre un proyecto que ya tiene
contenido, reproduciendo el vídeo como guía y deteniendo la grabación sola al
terminar el vídeo.

#### Scenario: Grabar el doblaje de un vídeo montado
- **WHEN** el usuario abre "Voz" y presiona "Grabar"
- **THEN** la vista previa vuelve al principio, el vídeo se reproduce con el
  monitoreo bajo y la toma se graba desde el segundo cero

#### Scenario: La toma se corta sola
- **WHEN** el vídeo llega a su final mientras se graba
- **THEN** la grabación se detiene y la toma queda asociada al proyecto

#### Scenario: Regrabar
- **WHEN** el usuario presiona "Regrabar" después de una toma
- **THEN** la toma anterior se reemplaza por la nueva y "Deshacer" recupera la
  anterior

### Requirement: Aislar el entorno de la toma
VidCam SHALL procesar la toma cruda con RNNoise para reducir el ruido de entorno,
y SHALL permitir activar o desactivar esa limpieza sin volver a grabar.

#### Scenario: Activar la aislación
- **WHEN** el usuario activa "Aislar entorno (RNNoise)"
- **THEN** la toma se reprocesa y la vista previa y la exportación usan la
  versión limpia

#### Scenario: Comparar contra la toma cruda
- **WHEN** el usuario desactiva "Aislar entorno"
- **THEN** suena la toma original, sin perder la versión limpia

#### Scenario: La limpieza falla
- **WHEN** el procesamiento no puede completarse
- **THEN** el proyecto conserva la toma cruda, la aislación queda desactivada y
  se informa por aviso

### Requirement: Colocar la toma en el tiempo
La toma SHALL poder moverse dentro del vídeo, con valores negativos que recortan
su comienzo y positivos que la retrasan, y SHALL recortarse al final del vídeo.

#### Scenario: Retrasar la voz
- **WHEN** el usuario sube la entrada a +1,5 s
- **THEN** la voz empieza 1,5 s después del inicio del vídeo, en la vista previa
  y en el MP4

#### Scenario: Adelantar la voz
- **WHEN** el usuario baja la entrada a −0,8 s
- **THEN** los primeros 800 ms de la toma no se usan y la voz entra al inicio

#### Scenario: La toma es más larga que el vídeo
- **WHEN** la toma dura más que el vídeo
- **THEN** lo que sobra no se exporta

### Requirement: La voz suena en la vista previa
La vista previa SHALL reproducir la voz sincronizada con el vídeo, respetando
entrada, recorte y volumen, y SHALL silenciarla mientras se graba una nueva toma
para no realimentar el micrófono.

#### Scenario: Reproducir con voz
- **WHEN** el proyecto tiene una toma y la vista previa se reproduce
- **THEN** la voz suena alineada con el vídeo y se corrige si se desfasa

#### Scenario: Grabar sin realimentación
- **WHEN** se está grabando una toma sobre otra existente
- **THEN** la toma anterior no suena y el monitoreo del vídeo está al 25 %

### Requirement: La voz se exporta en el MP4
La exportación SHALL incluir la toma (cruda o limpia, según el interruptor) como
una pista más de audio, mezclada con el audio original y la música.

#### Scenario: Exportar con voz en off
- **WHEN** el usuario exporta un proyecto con toma de voz
- **THEN** el MP4 contiene la voz mezclada con el audio original y la música,
  en el mismo segundo que en la vista previa

#### Scenario: Proyecto sin voz
- **WHEN** el proyecto no tiene toma
- **THEN** la exportación funciona exactamente como antes
