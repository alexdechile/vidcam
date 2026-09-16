## Purpose

Permite editar de forma no destructiva la composición: recortar vídeo y audio, silenciar o mezclar el audio original, y superponer capas PNG transparentes y textos con Google Fonts.

## ADDED Requirements

### Requirement: Recorte de vídeo no destructivo
La aplicación DEBE (MUST) permitir ajustar el inicio y el fin de cada clip de vídeo con precisión de al menos una décima de segundo, sin alterar el archivo original.

#### Scenario: Recortar el inicio y el fin
- **WHEN** el usuario arrastra los controles de inicio y fin de un clip
- **THEN** la vista previa y la duración total reflejan el segmento recortado y el archivo original no se modifica

#### Scenario: Duración final mayor a 30 segundos
- **WHEN** la suma de la línea de tiempo supera los 30 segundos tras un recorte
- **THEN** la aplicación avisa y exige recortar antes de exportar

### Requirement: Ajuste de audio con silencio y mezcla
La aplicación DEBE (MUST) permitir silenciar el audio original del vídeo y DEBE (MUST) mezclarlo con la pista de música elegida a lo largo de la línea de tiempo.

#### Scenario: Silenciar el audio original
- **WHEN** el usuario activa la opción de silencio
- **THEN** el audio original deja de oírse en la vista previa y no se incluye en la exportación

#### Scenario: Mezclar música con el vídeo
- **WHEN** el usuario elige una pista de música y activa su uso
- **THEN** la vista previa reproduce la música alineada al inicio del proyecto y se conserva al exportar

#### Scenario: Recorte de la pista de música
- **WHEN** la pista musical es más larga que la línea de tiempo
- **THEN** la aplicación usa solo el segmento necesario y permite elegir el punto de inicio de la música

### Requirement: Capas de PNG con transparencia
La aplicación DEBE (MUST) permitir agregar una o más imágenes PNG con transparencia como capas superpuestas, con control de posición, escala y duración, y DEBE (MUST) respetar el canal alfa en la vista previa y en la exportación.

#### Scenario: Agregar un sticker PNG transparente
- **WHEN** el usuario agrega un PNG desde el conjunto integrado o desde la galería
- **THEN** el PNG aparece sobre el vídeo, se puede mover y escalar, y las zonas transparentes dejan ver el vídeo de fondo

#### Scenario: Cambiar la duración de una capa
- **WHEN** el usuario ajusta la duración de una capa PNG
- **THEN** la capa aparece solo durante el intervalo configurado en la vista previa

### Requirement: Textos con Google Fonts
La aplicación DEBE (MUST) permitir agregar uno o más textos superpuestos con contenido, tamaño, color y posición editables, y DEBE (MUST) ofrecer una selección de fuentes de Google Fonts descargadas y almacenadas en caché.

#### Scenario: Agregar un texto con una fuente de Google
- **WHEN** el usuario agrega un texto y elige una fuente de Google Fonts
- **THEN** el texto se muestra sobre el vídeo con la fuente elegida y la vista previa refleja tamaño, color y posición

#### Scenario: Uso sin conexión
- **WHEN** el usuario elige una fuente de Google Fonts sin conexión y no está en caché
- **THEN** la aplicación usa una fuente local predeterminada y avisa que la fuente se descargará cuando haya conexión
