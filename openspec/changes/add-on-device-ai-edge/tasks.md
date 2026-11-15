# Tasks

## 1. Dependencias y configuración Gradle

- [x] 1.1 Agregar `com.google.mediapipe:tasks-genai` en `gradle/libs.versions.toml` y `app/build.gradle.kts`, verificando que la sincronización Gradle resuelva la dependencia sin conflictos con `compileSdk = 35` y `minSdk = 26`.

## 2. Gestor de modelos locales y motor de inferencia

- [x] 2.1 Implementar `LocalAiModelStore` para registrar, validar extensión (`.task` / `.litertlm`), copiar a almacenamiento interno privado (`filesDir/models/`) y eliminar modelos locales, cubierto por pruebas unitarias de validación y persistencia de ruta.
- [x] 2.2 Implementar `OnDeviceAiEngine` envolviendo `LlmInference` en `Dispatchers.IO` con carga bajo demanda, generación de texto, manejo seguro de errores en ABIs no compatibles y método `release()` para liberar RAM; verificar con pruebas unitarias de estado y ciclo de vida.

## 3. Integración en el Editor y UI Compose

- [x] 3.1 Exponer estado y acciones del asistente offline en `EditorViewModel` (`selectAiModelUri`, `generateText`, `clearAiModel`, `releaseAiEngine`) y asegurar la llamada a `releaseAiEngine()` al iniciar exportación MP4 o PNG.
- [x] 3.2 Crear el componente Compose `OnDeviceAiSheet` (selector de archivo `.task`/`.litertlm`, prompt rápido para titular o guion de voice-over, vista previa de respuesta y botones para insertar como capa de texto o copiar) conectado desde la barra del editor.

## 4. Verificación en la nube y documentación

- [x] 4.1 Ejecutar build y tests unitarios en GitHub Actions (`gh run watch`) verificando que la APK `arm64-v8a` compile y las pruebas pasen sin regresiones.
- [x] 4.2 Actualizar `alex.md` de VidCam documentando la integración de Google AI Edge on-device y cómo cargar un archivo `.task`/`.litertlm` en el dispositivo.
