package com.vidcam.app.ui

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

fun googleFontFamily(name: String?): FontFamily {
    if (name.isNullOrBlank()) return FontFamily.Default
    return FontFamily(GoogleFontResource(GoogleFont(name), googleFontProvider))
}
