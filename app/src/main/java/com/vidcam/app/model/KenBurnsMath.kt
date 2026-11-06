package com.vidcam.app.model

/**
 * Cálculo puro del efecto Ken Burns sobre imágenes fijas.
 *
 * El efecto es un zoom lento, con un desplazamiento lateral pequeño, que recorre
 * toda la ventana visible de la imagen. La animación se reparte sobre el tramo en
 * que la imagen está en pantalla, así que el mismo zoom se nota rápido en un
 * tramo corto y lento en uno largo: la velocidad queda determinada por la
 * duración del vídeo, sin configuración adicional.
 *
 * El desplazamiento se acota siempre a lo que el zoom permite
 * (`|offset| <= (scale - 1) / 2`), de modo que el borde de la imagen nunca entra
 * en el encuadre y no aparecen franjas negras.
 *
 * Vive fuera de Compose y de Media3 para que la vista previa y la exportación
 * usen exactamente las mismas cuentas y para poder probarla en la JVM.
 */
object KenBurnsMath {

    /** Zoom total de la animación, como fracción del encuadre (0.14 = 14 %). */
    const val ZOOM_AMOUNT = 0.14f

    /**
     * Transformación de una imagen en un instante: [scale] es el factor de zoom
     * (1 = imagen quieta a encuadre completo) y [offsetX]/[offsetY] el
     * desplazamiento en unidades normalizadas del lienzo (1 = ancho o alto).
     * El desplazamiento positivo mueve la imagen hacia la derecha o hacia abajo.
     */
    data class KenBurnsTransform(
        val scale: Float = 1f,
        val offsetX: Float = 0f,
        val offsetY: Float = 0f,
    )

    /** Imagen quieta, sin zoom ni desplazamiento. */
    val SIN_EFECTO = KenBurnsTransform()

    /** `true` si [mode] mueve la imagen en algún momento. */
    fun isActive(mode: KenBurnsMode): Boolean = mode != KenBurnsMode.NINGUNO

    /**
     * Modo efectivo de una imagen: [KenBurnsMode.AUTOMATICO] alterna acercar y
     * alejar según el índice de la imagen en la línea de tiempo, para que dos
     * imágenes consecutivas no repitan el mismo movimiento.
     */
    fun resolveMode(mode: KenBurnsMode, index: Int): KenBurnsMode = when (mode) {
        KenBurnsMode.AUTOMATICO ->
            if (index.coerceAtLeast(0) % 2 == 0) KenBurnsMode.ACERCAR else KenBurnsMode.ALEJAR
        else -> mode
    }

    /**
     * Posición de la imagen dentro del proyecto (orden por inicio), que decide
     * qué dirección usa [KenBurnsMode.AUTOMATICO]. Devuelve 0 si [id] no existe.
     */
    fun indexOf(backgrounds: List<BackgroundLayer>, id: String?): Int {
        if (id == null) return 0
        val index = backgrounds.sortedBy { it.startMs }.indexOfFirst { it.id == id }
        return index.coerceAtLeast(0)
    }

    /**
     * Progreso `[0, 1]` de la animación en [timeMs], con suavizado de entrada y
     * salida para que el zoom no arranque ni termine de golpe.
     */
    fun progressAt(startMs: Long, endMs: Long, timeMs: Long): Float {
        val span = endMs - startMs
        if (span <= 0L) return 0f
        val linear = ((timeMs - startMs).toFloat() / span.toFloat()).coerceIn(0f, 1f)
        return linear * linear * (3f - 2f * linear)
    }

    /**
     * Transformación de la imagen en [timeMs] para un tramo `[startMs, endMs)`.
     * Fuera del tramo se devuelve el primer o el último fotograma del efecto, así
     * que la imagen nunca salta al entrar o salir.
     */
    fun transformAt(
        mode: KenBurnsMode,
        index: Int,
        startMs: Long,
        endMs: Long,
        timeMs: Long,
    ): KenBurnsTransform {
        val resolved = resolveMode(mode, index)
        if (resolved == KenBurnsMode.NINGUNO) return SIN_EFECTO
        if (endMs - startMs <= 0L) return SIN_EFECTO
        val progress = progressAt(startMs, endMs, timeMs)
        val scale: Float
        val drift: Float
        if (resolved == KenBurnsMode.ALEJAR) {
            scale = 1f + ZOOM_AMOUNT * (1f - progress)
            // Empieza desplazada a la derecha y vuelve al centro mientras se aleja.
            drift = 1f - 2f * progress
        } else {
            scale = 1f + ZOOM_AMOUNT * progress
            // Recorre de izquierda a derecha mientras se acerca.
            drift = -1f + 2f * progress
        }
        // El desplazamiento se acota a lo que el zoom permite: con escala 1 no hay
        // margen y con la escala máxima el margen es `(scale - 1) / 2`.
        val maxOffset = (scale - 1f) / 2f
        return KenBurnsTransform(
            scale = scale,
            offsetX = drift * maxOffset,
            offsetY = 0f,
        )
    }

    /**
     * Cambio de zoom por segundo que implica un tramo de `[startMs, endMs)`.
     * Sirve para documentar y probar que un vídeo más largo mueve la imagen más
     * despacio.
     */
    fun zoomPerSecond(startMs: Long, endMs: Long): Float {
        val spanMs = (endMs - startMs).coerceAtLeast(1L)
        return ZOOM_AMOUNT / (spanMs.toFloat() / 1000f)
    }
}
