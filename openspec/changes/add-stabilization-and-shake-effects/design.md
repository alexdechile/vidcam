# Design — Estabilizador de imagen y efecto de temblor (Shake) en VidCam

## Context

VidCam utiliza **CameraX** para la captura de vídeo y **Media3** para la vista previa (`ExoPlayer`) y la exportación (`Transformer`).
El usuario solicitó dos capacidades de movimiento de cámara complementarias:
1. Poder estabilizar la imagen para tomas suaves y sin sacudidas involuntarias.
2. Aplicar el efecto inverso: hacer que la toma tiemble de forma controlada y dinámica (temblor / cámara temblorosa).

## Decisions

### D1: Estabilización de imagen nativa en cámara mediante Camera2Interop
- **Razón**: La estabilización de vídeo digital en post-producción sin sensores giroscópicos ni bibliotecas pesadas de visión artificial (como OpenCV) provocaría artefactos o degradación de rendimiento en dispositivos móviles. Por el contrario, la HAL de Android ofrece soporte de hardware para **EIS** (`CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE`) y **OIS** (`CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE`) de alto rendimiento y cero latencia.
- **Implementación**: Usar `Camera2CameraControl.from(controller.cameraControl)` en `CameraRecorder` para aplicar las opciones de captura de forma segura, ignorando de manera tolerante dispositivos que no cuenten con dicho soporte.

### D2: Efecto de temblor (Shake) implementado con Media3 MatrixTransformation
- **Razón**: Media3 1.5.1 proporciona la interfaz `MatrixTransformation`, cuyo método `getMatrix(presentationTimeUs: Long): Matrix` se evalúa frame a frame tanto en `videoPlayer.setVideoEffects` (preview de ExoPlayer) como en `Transformer` (exportación).
- **Matemática del temblor**:
  - Para lograr un temblor natural tipo cámara en mano o sacudida enérgica sin saltos bruscos artificiales, se combinan ondas sinusoidales de frecuencias no armónicas:
    - $dx(t) = A_x \cdot (\sin(2\pi \cdot f_1 \cdot t) \cdot 0.6 + \sin(2\pi \cdot f_2 \cdot t + 1.2) \cdot 0.4)$
    - $dy(t) = A_y \cdot (\cos(2\pi \cdot f_3 \cdot t) \cdot 0.7 + \sin(2\pi \cdot f_4 \cdot t + 2.1) \cdot 0.3)$
    - $rot(t) = A_\theta \cdot \sin(2\pi \cdot f_5 \cdot t + 0.5)$
  - Se aplica un zoom de compensación proporcional a la amplitud ($scale = 1.05\times$ a $1.15\times$) para que al desplazarse o rotar el fotograma, los bordes nunca revelen franjas negras.
- **Niveles de intensidad (`ShakeIntensity`)**:
  - `NINGUNO`: sin transformación.
  - `SUAVE`: leve balanceo orgánico estilo cámara en mano ($A \approx 0.02$, escala $1.05$).
  - `MEDIO`: movimiento evidente de pasos o agitación ($A \approx 0.05$, escala $1.10$).
  - `FUERTE`: sacudida intensa de impacto o temblor fuerte ($A \approx 0.09$, escala $1.16$).

### D3: Retrocompatibilidad y modelo de datos
- `ShakeIntensity` enum serializable con `@Serializable`.
- Añadido a `VideoClip` con valor por defecto `ShakeIntensity.NINGUNO`.
- Cualquier proyecto JSON previo se deserializa sin errores manteniendo los valores por defecto.

### D4: Deshacer y rehacer
- Toda modificación de temblor en un clip se registra en `UndoHistory` bajo la clave `"clip-shake-$id"`.
