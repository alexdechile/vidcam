## Purpose

Permite decidir durante cuántos segundos y desde qué instante se ve cada capa
(PNG o texto) del proyecto, tanto en la vista previa como en el vídeo exportado.

## ADDED Requirements

### Requirement: Duración en pantalla configurable por capa

La aplicación DEBE (MUST) permitir fijar con un control deslizante cuántos
segundos permanece visible cada capa, expresado en segundos. El valor DEBE
(MUST) estar acotado entre un mínimo de 0,3 s y el tiempo disponible del proyecto
a partir del inicio de la capa, de modo que nunca se pueda pedir una capa más
larga que el vídeo.

#### Scenario: Ajustar la duración de un PNG

- **WHEN** el usuario añade un PNG y mueve el slider "En pantalla" hasta 3 s
- **THEN** la capa se muestra durante 3 s y se oculta el resto del vídeo, igual
  en la vista previa y en el MP4 exportado

#### Scenario: La capa no puede durar más que el vídeo

- **WHEN** el proyecto dura 4 s y el usuario arrastra el slider al máximo
- **THEN** la duración queda en 4 s (o menos, si el inicio ya no está en 0) y el
  slider no permite valores mayores

#### Scenario: Proyecto sin clips todavía

- **WHEN** el usuario añade un PNG antes de grabar o importar ningún clip
- **THEN** el slider se acota con el límite de 30 s del proyecto y sigue
  respondiendo

### Requirement: Instante de aparición configurable por capa

La aplicación DEBE (MUST) permitir fijar con un control deslizante en qué
segundo aparece la capa. Al cambiar el instante de aparición, la duración en
pantalla DEBE (MUST) conservarse, y el inicio DEBE (MUST) quedar acotado para que
la capa no termine después del final del proyecto.

#### Scenario: Colocar un sticker a mitad del vídeo

- **WHEN** el proyecto dura 10 s, la capa dura 3 s y el usuario mueve
  "Aparece en" hasta 5 s
- **THEN** la capa se ve de 5 s a 8 s y sigue durando 3 s

#### Scenario: El inicio no empuja la capa fuera del vídeo

- **WHEN** el proyecto dura 10 s, la capa dura 4 s y el usuario arrastra el
  inicio hasta el máximo
- **THEN** el inicio se detiene en 6 s para que la capa termine en 10 s

### Requirement: Duración y aparición visibles mientras se edita

La aplicación DEBE (MUST) mostrar el valor actual de duración y de aparición en
segundos junto a cada control, y DEBE (MUST) reflejar cada cambio en la vista
previa inmediatamente, sin necesidad de exportar.

#### Scenario: Ver el cambio en la vista previa

- **WHEN** el usuario reduce la duración de una capa mientras el vídeo se
  reproduce en la vista previa
- **THEN** la capa desaparece del lienzo al pasar su nuevo instante final

### Requirement: Ventana conservada en el proyecto guardado

Las capas guardadas en un proyecto DEBEN (MUST) conservar su duración y su
instante de aparición al reabrir el proyecto. Una capa sin ventana explícita
DEBE (MUST) seguir mostrándose durante todo el vídeo, como antes de esta
capacidad.

#### Scenario: Reabrir un proyecto con capas de distinta duración

- **WHEN** el usuario guarda un proyecto con un PNG de 2 s y un texto que dura
  todo el vídeo, y lo vuelve a abrir
- **THEN** ambos conservan su tiempo en pantalla y el slider muestra cada valor

### Requirement: La animación de la capa respeta su ventana

Cuando una capa tiene fotogramas clave, la animación DEBE (MUST) reproducirse
solo dentro de su ventana visible. Los fotogramas clave fuera de la ventana DEBEN
(MUST) conservarse en el proyecto y volver a aplicarse si el usuario alarga de
nuevo la duración.

#### Scenario: Alargar de nuevo una capa animada

- **WHEN** una capa animada de 8 s se acorta a 2 s y después se vuelve a alargar
  a 8 s
- **THEN** la animación original vuelve a verse completa, sin perder fotogramas
  clave
