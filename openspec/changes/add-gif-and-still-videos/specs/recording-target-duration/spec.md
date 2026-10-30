## Purpose

Permite fijar de antemano cuánto dura la grabación: la cámara se detiene sola
en el tiempo elegido (5, 10, 15 o 30 s) y nunca se pasa de lo que queda libre
del proyecto de 30 s.

## ADDED Requirements

### Requirement: Elegir la duración de la grabación
La pantalla del editor SHALL ofrecer las duraciones de 5, 10, 15 y 30 segundos
para fijar la duración de la próxima grabación, y SHALL recordar la elegida en
el proyecto.

#### Scenario: Elegir 10 segundos
- **WHEN** el usuario selecciona "10 s" y empieza a grabar
- **THEN** la grabación se detiene automáticamente al llegar a 10 segundos

#### Scenario: La elección se guarda con el proyecto
- **WHEN** el usuario guarda un proyecto con duración fijada de 15 s y lo abre
  más tarde
- **THEN** el selector muestra 15 s y la próxima grabación se corta ahí

### Requirement: La grabación no supera el espacio libre
La parada automática SHALL respetar el tope de 30 s del proyecto: si ya hay
material grabado, la grabación se corta en lo que queda libre, y si no cabe
nada no empieza.

#### Scenario: Quedan 5 segundos libres
- **WHEN** el proyecto tiene 25 s grabados y la duración fijada es de 10 s
- **THEN** la nueva grabación se detiene a los 5 segundos

#### Scenario: Sin espacio
- **WHEN** el proyecto ya ocupa 30 segundos
- **THEN** la app avisa "Límite de 30 s alcanzado" y no empieza a grabar

### Requirement: Cronómetro durante la grabación
Mientras graba, la app SHALL mostrar el tiempo transcurrido y el límite de esa
grabación.

#### Scenario: Grabación en curso
- **WHEN** el usuario graba con un límite de 10 s
- **THEN** la pantalla muestra el avance, por ejemplo "Grabando 0:03 de 0:10"

### Requirement: La duración fijada define el vídeo de imágenes
En un proyecto sin clips, la duración fijada SHALL ser la duración del MP4
exportado y del lienzo de la vista previa, y las capas y fondos que llegaban al
final SHALL ajustarse al cambiarla.

#### Scenario: Vídeo de solo imágenes de 5 segundos
- **WHEN** el usuario fija 5 s y exporta un proyecto solo con PNG
- **THEN** el MP4 dura 5 segundos

#### Scenario: Alargar la duración
- **WHEN** el usuario pasa de 5 s a 15 s en un proyecto solo con imágenes
- **THEN** las capas y fondos que llegaban al final se estiran hasta los 15 s
