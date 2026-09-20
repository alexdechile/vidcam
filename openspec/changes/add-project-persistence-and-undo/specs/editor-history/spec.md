## Purpose

Permite deshacer y rehacer las ediciones del proyecto dentro de la sesión, con
una entrada de historial por gesto o edición puntual.

## ADDED Requirements

### Requirement: Deshacer y rehacer ediciones
La aplicación DEBE (MUST) permitir deshacer y rehacer los cambios del proyecto
actual y DEBE (MUST) indicar visualmente cuándo no hay nada que deshacer o
rehacer.

#### Scenario: Deshacer una edición puntual
- **WHEN** el usuario agrega una capa y toca deshacer
- **THEN** la capa desaparece y el proyecto vuelve al estado anterior

#### Scenario: Rehacer una edición deshecha
- **WHEN** el usuario toca rehacer después de deshacer
- **THEN** el cambio se vuelve a aplicar

#### Scenario: Sin historial disponible
- **WHEN** no hay cambios que deshacer o rehacer
- **THEN** las acciones correspondientes aparecen deshabilitadas

### Requirement: Agrupación de ediciones continuas
La aplicación DEBE (MUST) tratar una edición continua (arrastrar un recorte,
mover o pellizcar una capa, desplazar un control) como una sola entrada de
historial, de modo que deshacer retroceda al estado previo al gesto.

#### Scenario: Deshacer un gesto completo
- **WHEN** el usuario arrastra una capa y toca deshacer
- **THEN** la capa vuelve a la posición, escala y rotación que tenía antes de empezar el gesto, no a un punto intermedio

#### Scenario: Deshacer un recorte
- **WHEN** el usuario ajusta el inicio o el fin de un clip con el control deslizante y toca deshacer
- **THEN** el recorte vuelve al valor que tenía antes de empezar a arrastrar

### Requirement: Historial acotado y por sesión de proyecto
La aplicación DEBE (MUST) limitar el tamaño del historial y DEBE (MUST) vaciarlo
al crear o abrir otro proyecto.

#### Scenario: Historial vacío tras abrir un proyecto
- **WHEN** el usuario abre un proyecto guardado
- **THEN** no se puede deshacer hacia el proyecto que estaba editando antes
