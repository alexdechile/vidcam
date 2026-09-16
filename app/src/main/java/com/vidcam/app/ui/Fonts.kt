package com.vidcam.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.googlefonts.isAvailableOnDevice
import com.vidcam.app.R
import androidx.compose.ui.text.googlefonts.Font as GoogleFontResource

/** Fuentes descargables de Google Fonts disponibles para las capas de texto. */
val GOOGLE_FONT_NAMES: List<String> = listOf(
    "Roboto",
    "Lobster",
    "Pacifico",
    "Bebas Neue",
    "Montserrat",
    "Playfair Display",
    "Oswald",
    "Raleway",
)

private val googleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

/** Indica si el proveedor de fuentes de Google Play Services está disponible. */
@Composable
fun rememberGoogleFontsAvailable(): Boolean {
    val context = LocalContext.current
    return remember {
        runCatching { isAvailableOnDevice(googleFontProvider, context) }.getOrDefault(false)
    }
}

/**
 * Familia tipográfica para [name]; si el proveedor no está disponible (por
 * ejemplo sin conexión o sin Google Play Services) cae a la fuente del sistema.
 */
@Composable
fun rememberGoogleFontFamily(name: String?): FontFamily {
    val available = rememberGoogleFontsAvailable()
    return remember(available, name) {
        if (name.isNullOrBlank() || !available) {
            FontFamily.Default
        } else {
            FontFamily(GoogleFontResource(GoogleFont(name), googleFontProvider))
        }
    }
}
