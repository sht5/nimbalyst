package com.tvremote.samsung.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Samsung's own brand blue, used sparingly as the accent color.
private val SamsungBlue = Color(0xFF1428A0)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF6E85FF),
    secondary = Color(0xFF9AA5FF),
    background = Color(0xFF0A0E27),
    surface = Color(0xFF14183A),
)

private val LightColors = lightColorScheme(
    primary = SamsungBlue,
    secondary = Color(0xFF3D52D5),
    background = Color(0xFFF5F6FB),
    surface = Color(0xFFFFFFFF),
)

@Composable
fun SamsungTvRemoteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
