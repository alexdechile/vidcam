## Purpose

Permite guardar el proyecto en el dispositivo, volver a abrirlo más adelante y
borrarlo, de forma local y sin cuenta ni conexión.

## ADDED Requirements

### Requirement: Guardar el proyecto en un archivo JSON local
La aplicación DEBE (MUST) guardar el proyecto actual como un archivo JSON en el
almacenamiento propio de la aplicación, con un nombre y la fecha de
actualización, y DEBE (MUST) conservar la fecha de creación al sobrescribirlo.

#### Scenario: Guardar por primera vez
- **WHEN** el usuario guarda un proyecto que todavía no tiene nombre
- **THEN** la aplicación pide un nombre y crea el archivo con el proyecto completo

#### Scenario: Guardar cambios sobre el proyecto abierto
- **WHEN** el usuario guarda un proyecto ya guardado
- **THEN** la aplicación sobrescribe ese mismo archivo y actualiza la fecha de modificación, sin duplicarlo

#### Scenario: Guardar una copia
- **WHEN** el usuario elige "Guardar como" y da un nombre distinto
- **THEN** la aplicación crea un proyecto nuevo y la lista muestra ambos

#### Scenario: Proyecto vacío
- **WHEN** el usuario intenta guardar sin clips, capas ni música
- **THEN** la aplicación no guarda nada e informa que no hay nada que guardar

### Requirement: Abrir y eliminar proyectos guardados
La aplicación DEBE (MUST) mostrar la lista de proyectos guardados, del más
reciente al más antiguo, permitir abrir cualquiera de ellos y eliminarlo.

#### Scenario: Abrir un proyecto
- **WHEN** el usuario elige un proyecto de la lista
- **THEN** el editor carga sus clips, capas, música y recortes en el estado en que se guardaron

#### Scenario: Eliminar un proyecto
- **WHEN** el usuario elimina un proyecto de la lista
- **THEN** el archivo desaparece del almacenamiento y ya no aparece en la lista

#### Scenario: Sin proyectos guardados
- **WHEN** el usuario abre la lista y no hay proyectos
- **THEN** la aplicación muestra un estado vacío explicando que todavía no hay proyectos guardados

#### Scenario: Archivo ilegible
- **WHEN** uno de los archivos guardados está corrupto
- **THEN** la aplicación lo ignora y sigue mostrando el resto de la lista

### Requirement: Aviso de cambios sin guardar
La aplicación DEBE (MUST) detectar si el proyecto actual difiere de la última
versión guardada y DEBE (MUST) pedir confirmación antes de descartarlo.

#### Scenario: Crear un proyecto nuevo con cambios sin guardar
- **WHEN** el usuario toca "Nuevo" con cambios sin guardar
- **THEN** la aplicación pide confirmación y, si el usuario cancela, conserva el proyecto intacto

#### Scenario: Abrir otro proyecto con cambios sin guardar
- **WHEN** el usuario elige abrir un proyecto distinto con cambios sin guardar
- **THEN** la aplicación pide confirmación antes de descartar los cambios

#### Scenario: Deshacer hasta el estado guardado
- **WHEN** el usuario deshace todos los cambios posteriores al último guardado
- **THEN** la aplicación ya no considera que haya cambios sin guardar

### Requirement: Medios del proyecto persistidos
La aplicación DEBE (MUST) almacenar en su propio espacio persistente los clips
importados, las grabaciones y las imágenes agregadas desde la galería, de modo
que un proyecto guardado siga funcionando aunque se limpie la caché o caduque el
permiso del selector del sistema.

#### Scenario: Reabrir un proyecto después de limpiar la caché
- **WHEN** el usuario abre un proyecto guardado después de que el sistema haya limpiado la caché
- **THEN** los clips y las imágenes siguen disponibles
