package com.vidcam.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val VidCamColors = darkColorScheme(
    primary = VidCamPurple,
    onPrimary = Color.White,
    primaryContainer = VidCamPurpleDark,
    onPrimaryContainer = Color.White,
    secondary = Accent,
    onSecondary = Background,
    background = Background,
    onBackground = OnSurface,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurface,
    error = Danger,
    onError = Color.White,
    outline = Muted,
)

@Composable
fun VidCamTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
    }
    MaterialTheme(
        colorScheme = VidCamColors,
        typography = VidCamTypography,
        content = content,
    )
}
