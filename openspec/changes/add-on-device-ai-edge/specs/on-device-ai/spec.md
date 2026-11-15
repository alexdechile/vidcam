# Spec Delta

## Purpose

Permite ejecutar modelos de lenguaje locales en el dispositivo (formato `.task` / `.litertlm`) dentro de VidCam para asistir al creador en la redacción de titulares, subtítulos y guiones de voice-over sin conexión a internet.

## ADDED Requirements

### Requirement: Selección y registro de modelo local externo
El sistema SHALL permitir al usuario seleccionar un archivo de modelo compatible (`.task` o `.litertlm`) desde el almacenamiento del dispositivo y recordar su ubicación sin incluir pesos dentro de la APK.

#### Scenario: Selección de modelo válido
- **WHEN** el usuario selecciona un archivo `.task` o `.litertlm` válido desde el selector de archivos
- **THEN** el sistema copia o referencia el archivo en almacenamiento interno privado de la app y lo muestra como modelo activo disponible

#### Scenario: Archivo inválido o inexistente
- **WHEN** el usuario selecciona un archivo corrupto o con extensión no soportada, o el archivo previamente registrado fue eliminado
- **THEN** el sistema informa el error de forma clara y mantiene deshabilitada la inferencia hasta registrar un modelo válido

### Requirement: Asistencia creativa de texto offline en el editor
El sistema SHALL ofrecer generación de texto offline a partir de un prompt del usuario para crear o mejorar titulares de capa de texto y guiones de voice-over.

#### Scenario: Generación de titular o guion exitosa
- **WHEN** hay un modelo válido configurado y el usuario solicita generar texto para un titular o guion
- **THEN** el sistema ejecuta la inferencia localmente y permite insertar el resultado en una capa de texto o copiarlo al guion de voice-over

#### Scenario: Sin modelo configurado
- **WHEN** el usuario abre el asistente offline sin haber configurado un modelo local
- **THEN** el sistema muestra un estado vacío guiado con acceso directo para seleccionar un archivo `.task` o `.litertlm`

### Requirement: Gestión de memoria y liberación bajo demanda
El sistema SHALL cargar el motor de inferencia bajo demanda y liberar la memoria del modelo cuando el usuario cierre el asistente o inicie una exportación de vídeo.

#### Scenario: Liberación antes de exportar
- **WHEN** el modelo está cargado en memoria y el usuario inicia la exportación de vídeo MP4 o PNG
- **THEN** el sistema descarga el motor de IA de memoria antes de iniciar el renderizado para evitar presión de RAM
