## Purpose

Orienta al usuario hacia fuentes de música que puede descargar legalmente y usar
en sus vídeos, sin que la aplicación redistribuya audio de terceros.

## ADDED Requirements

### Requirement: Guía de fuentes de música gratis
La aplicación DEBE (MUST) ofrecer, dentro del selector de música, enlaces a
fuentes de música de uso libre que el usuario pueda descargar por su cuenta, y NO
DEBE (MUST NOT) incluir pistas de audio de terceros dentro de la aplicación.

#### Scenario: Abrir una fuente de música
- **WHEN** el usuario toca una de las fuentes del selector
- **THEN** la aplicación abre esa página en el navegador del dispositivo

#### Scenario: Fuentes ofrecidas
- **WHEN** el usuario abre el selector de música
- **THEN** ve al menos la Biblioteca de audio de YouTube, Pixabay, Free Music Archive, ccMixter y Musopen, cada una con una nota sobre su licencia

#### Scenario: Sin navegador disponible
- **WHEN** el dispositivo no puede abrir el enlace
- **THEN** la aplicación no falla y el resto del selector sigue funcionando

### Requirement: Actualizar la música del dispositivo
La aplicación DEBE (MUST) permitir volver a leer la biblioteca de audio del
dispositivo sin cerrar el selector, para que una pista descargada aparezca sin
reiniciar la aplicación.

#### Scenario: Pista descargada durante la sesión
- **WHEN** el usuario descarga una pista y toca "Actualizar" en el selector
- **THEN** la pista aparece en la lista de música del dispositivo

#### Scenario: Sin música en el dispositivo
- **WHEN** el dispositivo no tiene pistas de audio
- **THEN** la aplicación explica que no encontró música e indica que descargue una de las fuentes y toque "Actualizar"
