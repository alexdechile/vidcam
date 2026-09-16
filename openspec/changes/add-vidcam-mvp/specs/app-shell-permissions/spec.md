## Purpose

Define el arranque de la aplicación, la experiencia de primer uso y la solicitud única de permisos de cámara, micrófono y acceso a medios.

## ADDED Requirements

### Requirement: Solicitud única de permisos al primer arranque
La aplicación DEBE (MUST) solicitar los permisos de cámara, micrófono y lectura de medios en un único flujo la primera vez que se abre, y DEBE (MUST) permitir llegar al editor únicamente cuando los permisos necesarios para la acción elegida estén concedidos.

#### Scenario: Primer arranque concede todos los permisos
- **WHEN** el usuario abre la aplicación por primera vez y acepta cámara, micrófono y acceso a medios
- **THEN** la aplicación muestra el editor minimalista y no vuelve a pedir esos permisos en arranques posteriores

#### Scenario: Permiso denegado con opción de reintento
- **WHEN** el usuario deniega uno o más permisos
- **THEN** la aplicación muestra un estado que explica qué función queda deshabilitada y ofrece reintentar o abrir los ajustes del sistema

#### Scenario: Permiso denegado permanentemente
- **WHEN** el usuario marcó no volver a preguntar y vuelve a abrir la aplicación
- **THEN** la aplicación muestra un enlace directo a los ajustes del sistema para habilitar el permiso manualmente

### Requirement: Lectura de medios sin permiso adicional en versiones compatibles
La aplicación DEBE (MUST) usar el selector de medios del sistema para importar vídeos e imágenes cuando el sistema operativo lo provea, de modo que no se solicite permiso de almacenamiento adicional.

#### Scenario: Importar con el selector del sistema
- **WHEN** el usuario toca Importar en un dispositivo con selector de medios del sistema
- **THEN** se abre el selector y la aplicación accede solo al archivo elegido, sin solicitar permiso de lectura global

### Requirement: Interfaz de arranque minimalista
La pantalla de arranque DEBE (MUST) mostrar el estado de permisos, el nombre de la aplicación y una única acción principal para continuar.

#### Scenario: Interfaz de arranque
- **WHEN** el usuario abre la aplicación
- **THEN** ve una pantalla simple en español con el estado de permisos y una acción principal para continuar al editor
