package com.mokostudio.baselog.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Orange500,
    onPrimary = White,
    secondary = Orange300,
    background = Navy900,
    onBackground = White,
    surface = SurfaceDark,
    onSurface = White,
    surfaceVariant = Navy800,
    onSurfaceVariant = White
)

private val LightColorScheme = lightColorScheme(
    primary = Orange500,
    onPrimary = White,
    secondary = Navy900,
    onSecondary = White,
    background = White,
    onBackground = Navy900,
    surface = SurfaceLight,
    onSurface = Navy900,
    surfaceVariant = BorderLight,
    onSurfaceVariant = TextMuted
)

@Composable
fun BaseLogTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
