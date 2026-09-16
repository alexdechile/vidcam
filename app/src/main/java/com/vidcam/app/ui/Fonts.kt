package com.vidcam.app.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.googlefonts.GoogleFont
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

/**
 * Heurística de disponibilidad de Google Fonts: si no hay red asumimos que las
 * fuentes no podrán descargarse y avisamos al usuario.
 */
@Composable
fun rememberGoogleFontsAvailable(): Boolean {
    val context = LocalContext.current
    return remember {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            as? ConnectivityManager
        val network = manager?.activeNetwork
        val capabilities = network?.let { manager.getNetworkCapabilities(it) }
        capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }
}

/**
 * Familia tipográfica para [name]; si no hay red la resolución cae a la fuente
 * del sistema (`bestEffort`), evitando errores en tiempo de ejecución.
 */
@Composable
fun rememberGoogleFontFamily(name: String?): FontFamily {
    val available = rememberGoogleFontsAvailable()
    return remember(available, name) {
        if (name.isNullOrBlank()) {
            FontFamily.Default
        } else {
            FontFamily(
                GoogleFontResource(GoogleFont(name, bestEffort = true), googleFontProvider),
            )
        }
    }
}
