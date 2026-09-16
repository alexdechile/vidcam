## Purpose

Permite renderizar la composición a un archivo MP4 vertical, guardarlo en la galería del dispositivo y compartirlo con aplicaciones de redes sociales.

## ADDED Requirements

### Requirement: Exportación a MP4 vertical
La aplicación DEBE (MUST) renderizar la composición a un archivo MP4 con vídeo H.264 y audio AAC en proporción 9:16, incluyendo recortes, mezcla de audio y capas superpuestas.

#### Scenario: Exportación exitosa
- **WHEN** el usuario confirma exportar una composición válida
- **THEN** la aplicación genera un archivo MP4 reproducible con las capas, textos y audio mezclado aplicados

#### Scenario: Progreso y cancelación
- **WHEN** la exportación está en curso
- **THEN** la aplicación muestra una barra de progreso y permite cancelar; si se cancela, no deja un archivo parcial en la galería

#### Scenario: Error de exportación
- **WHEN** la exportación falla
- **THEN** la aplicación informa el error en español, conserva el proyecto y permite reintentar

### Requirement: Guardado en la galería
La aplicación DEBE (MUST) guardar el archivo exportado en la galería de vídeos del dispositivo, accesible desde otras aplicaciones.

#### Scenario: Archivo visible en la galería
- **WHEN** la exportación termina correctamente
- **THEN** el vídeo aparece en la galería del dispositivo con nombre y marca de tiempo

### Requirement: Compartir a redes sociales
La aplicación DEBE (MUST) ofrecer la hoja de compartir del sistema para el vídeo exportado con tipo `video/mp4`, permitiendo publicarlo en redes sociales como historias o TikTok.

#### Scenario: Compartir el vídeo exportado
- **WHEN** el usuario toca Compartir después de exportar
- **THEN** se abre la hoja de compartir del sistema con las aplicaciones instaladas y el vídeo se envía a la aplicación elegida
