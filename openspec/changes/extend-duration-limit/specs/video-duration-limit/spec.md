# Spec Delta: video-duration-limit

## Purpose

Gestiona los límites de duración del proyecto permitiendo grabaciones y vídeos de imágenes de hasta 5 minutos, respetando la duración completa del archivo cuando se importa o comparte un vídeo, y permitiendo exportar fotogramas compuestos en PNG de máxima calidad.

## ADDED Requirements

### Requirement: Límite base de duración de 5 minutos
El sistema SHALL limitar las grabaciones de cámara y los proyectos de imágenes fijas (sin clips de vídeo) a una duración máxima base de 5 minutos (300_000 ms).

#### Scenario: Grabación respeta el presupuesto de 5 minutos
- **WHEN** el usuario inicia una grabación de cámara en un proyecto con tiempo disponible
- **THEN** la grabación se programa para detenerse al alcanzar la duración fijada o el restante para completar 5 minutos, lo que ocurra primero

#### Scenario: Tope alcanzado para nueva grabación
- **WHEN** la duración total de clips en el proyecto alcanza o supera los 5 minutos (300_000 ms)
- **THEN** el sistema no permite iniciar una nueva grabación y notifica "Límite de 5 min alcanzado"

#### Scenario: Proyecto sin clips acota la duración fijada a 5 minutos
- **WHEN** un proyecto no tiene clips de vídeo
- **THEN** su duración de salida (`outputDurationMs`) se acota entre 1.000 ms y 300.000 ms según el valor de `targetDurationMs`

### Requirement: Duración completa para vídeos importados o compartidos
El sistema SHALL admitir que los vídeos importados desde el almacenamiento del dispositivo o recibidos compartidos desde otras aplicaciones ingresen a la línea de tiempo con su duración completa sin ser recortados forzosamente al límite base de 5 minutos.

#### Scenario: Importación de vídeo extenso
- **WHEN** el usuario importa un vídeo cuya duración excede los 5 minutos (por ejemplo 7 minutos)
- **THEN** el clip ingresa con su duración completa (`sourceDurationMs`) y la duración total del proyecto se extiende para alojarlo

#### Scenario: Recepción de vídeo compartido extenso
- **WHEN** otra aplicación comparte con VidCam un vídeo cuya duración excede los 5 minutos
- **THEN** el vídeo se incorpora a la línea de tiempo sin recortar su tramo final

#### Scenario: Recorte y velocidad en clips importados extensos
- **WHEN** el usuario ajusta el recorte (`updateTrim`) o la velocidad (`setClipSpeed`) de un clip cuya duración efectiva excede los 5 minutos
- **THEN** el sistema permite ajustar el rango hasta la duración completa del archivo sin forzar un recorte defensivo a 5 minutos

### Requirement: Selector de duración objetivo ampliado
El editor SHALL ofrecer chips de duración predefinida que incluyan duraciones de 5 s, 10 s, 15 s, 30 s, 1 min y 5 min para proyectos vacíos y grabaciones.

#### Scenario: Visualización de etiquetas legibles
- **WHEN** el editor muestra las opciones de duración fijada
- **THEN** las opciones de 60.000 ms y 300.000 ms se muestran con etiquetas legibles en minutos ("1 min" y "5 min")

### Requirement: Exportación de fotograma compuesto en formato PNG
El sistema SHALL permitir exportar un fotograma compuesto del proyecto en formato PNG con resolución completa y compresión sin pérdidas.

#### Scenario: Renderizado y guardado de fotograma en galería
- **WHEN** el usuario pulsa "Exportar fotograma (PNG)" en el diálogo de vista previa de exportación
- **THEN** el sistema genera una imagen PNG en `ARGB_8888` que combina el fotograma del vídeo, fondos, capas y ventana en resolución completa (1080x1920) y la almacena en la galería de imágenes del dispositivo bajo `Pictures/VidCam`

#### Scenario: Notificación y opción de compartir fotograma
- **WHEN** el fotograma PNG se guarda exitosamente
- **THEN** el sistema notifica "Fotograma PNG guardado en la galería" y permite compartir la imagen generada
