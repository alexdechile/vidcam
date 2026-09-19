package com.vidcam.app.util

import java.util.Locale

fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}

/** Tiempo corto con décimas, para marcas de fotogramas clave. */
fun formatShortTime(ms: Long): String =
    String.format(Locale.US, "%.1fs", ms.coerceAtLeast(0L) / 1000f)
