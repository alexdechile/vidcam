## Purpose

Permite descartar el proyecto en edición y empezar uno vacío desde el propio
editor, sin persistencia, evitando perder trabajo por accidente.

## ADDED Requirements

### Requirement: Empezar un proyecto nuevo sin guardar
La aplicación DEBE (MUST) ofrecer una acción para descartar el proyecto actual y
dejar el editor vacío, y NO DEBE (MUST NOT) guardar el proyecto descartado
mientras no exista persistencia.

#### Scenario: Empezar de cero con trabajo en curso
- **WHEN** el usuario toca "Nuevo" con clips, capas o música en el proyecto y confirma el descarte
- **THEN** el editor queda vacío, sin clips, capas ni música, y el proyecto anterior no se guarda

#### Scenario: Confirmación antes de descartar
- **WHEN** el usuario toca "Nuevo" con trabajo en el proyecto
- **THEN** la aplicación pide confirmación y, si el usuario cancela, conserva el proyecto intacto

#### Scenario: Proyecto ya vacío
- **WHEN** el usuario toca "Nuevo" sin clips, capas ni música
- **THEN** el editor permanece vacío sin pedir confirmación

#### Scenario: Nuevo proyecto durante la exportación
- **WHEN** hay una exportación en curso
- **THEN** la aplicación no descarta el proyecto hasta que la exportación termina o se cancela
