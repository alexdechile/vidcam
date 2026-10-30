## Purpose

Permite que VidCam reciba archivos desde otras aplicaciones —vídeo, imagen y
audio— por la hoja de compartir del sistema o abriéndolos con VidCam, y los
incorpore al proyecto en curso como clips, capas PNG y música, sin perder el
trabajo actual ni el material recibido.

## ADDED Requirements

### Requirement: VidCam aparece en la hoja de compartir del sistema
La app SHALL declarar en su actividad principal la recepción de
`android.intent.action.SEND` y `android.intent.action.SEND_MULTIPLE` para los
tipos `video/*`, `image/*` y `audio/*`, y de `android.intent.action.VIEW` para
`video/*`, de modo que el sistema ofrezca VidCam al compartir archivos
multimedia o al abrir un vídeo.

#### Scenario: Compartir un vídeo desde la galería
- **WHEN** el usuario comparte un vídeo desde otra aplicación y elige VidCam
- **THEN** VidCam se abre con ese vídeo agregado como clip de la línea de tiempo

#### Scenario: Abrir un vídeo con VidCam
- **WHEN** el usuario elige "Abrir con VidCam" para un archivo de vídeo
- **THEN** VidCam se abre con esa misma incorporación como clip

#### Scenario: Compartir varios archivos
- **WHEN** el usuario comparte dos vídeos y una imagen en una sola operación
- **THEN** los dos vídeos entran como clips y la imagen como capa PNG

### Requirement: Clasificación de los archivos recibidos
Cada archivo recibido SHALL clasificarse como vídeo, imagen o audio usando el
tipo MIME resuelto por el `ContentResolver` del sistema; si el remitente no
declara un tipo utilizable, SHALL usarse la extensión del nombre visible.
Un archivo de cualquier otro tipo SHALL ignorarse y contarse como omitido.

#### Scenario: Remitente sin tipo MIME
- **WHEN** una aplicación comparte un archivo llamado `clip.mp4` sin declarar
  su tipo MIME
- **THEN** VidCam lo trata como vídeo y lo agrega a la línea de tiempo

#### Scenario: Tipo no soportado
- **WHEN** el usuario comparte un documento PDF
- **THEN** VidCam no modifica el proyecto y avisa que ese tipo de archivo no se
  puede usar

### Requirement: Incorporación al proyecto
Los archivos recibidos SHALL copiarse al almacenamiento propio de la app antes
de usarse y SHALL incorporarse al proyecto en curso sin reemplazarlo: los
vídeos como clips (respetando el límite de 30 s y el recorte automático), las
imágenes como capas PNG sobre el lienzo y el audio como pista de música del
proyecto. Cada incorporación SHALL poder deshacerse.

#### Scenario: Compartir con un proyecto ya abierto
- **WHEN** el usuario comparte una imagen mientras edita un proyecto con clips
- **THEN** la imagen se agrega como capa nueva y los clips existentes se
  conservan

#### Scenario: Deshacer una incorporación compartida
- **WHEN** el usuario toca "Deshacer" después de recibir un vídeo compartido
- **THEN** el clip agregado desaparece y el proyecto vuelve a su estado anterior

#### Scenario: Proyecto guardado después de recibir
- **WHEN** el usuario guarda el proyecto después de recibir un archivo
  compartido y lo vuelve a abrir más tarde
- **THEN** el archivo sigue disponible, porque la copia vive en el
  almacenamiento propio de la app

### Requirement: Recepción sin duplicar la actividad
Si VidCam ya está abierta, compartir un archivo SHALL entregarlo a la instancia
existente en lugar de abrir una segunda copia de la aplicación.

#### Scenario: Compartir con VidCam en segundo plano
- **WHEN** el usuario comparte un vídeo estando VidCam abierta en segundo plano
- **THEN** VidCam vuelve al frente con el vídeo agregado al proyecto que ya
  tenía abierto

### Requirement: Archivos pendientes cuando faltan permisos
Si la app todavía no tiene los permisos de medios, los archivos recibidos SHALL
mantenerse pendientes y la pantalla de permisos SHALL indicarlo. Al conceder
los permisos, los archivos SHALL incorporarse automáticamente.

#### Scenario: Compartir en una instalación nueva
- **WHEN** el usuario comparte un vídeo con VidCam sin haber concedido nunca
  los permisos
- **THEN** la app muestra la pantalla de permisos con el aviso de que hay un
  archivo esperando y, al concederlos, agrega el vídeo a la línea de tiempo

### Requirement: Aviso del resultado
Después de procesar los archivos, la app SHALL informar en un solo mensaje
cuántos vídeos, imágenes y pistas de audio se agregaron, y cuántos se omitieron,
conservando el motivo cuando la incorporación haya fallado.

#### Scenario: Resultado mixto
- **WHEN** el usuario comparte un vídeo válido y un PDF
- **THEN** la app agrega el clip y avisa que un archivo quedó sin agregar
