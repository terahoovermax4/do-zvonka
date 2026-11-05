package ru.dozvonka.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Цвета дизайна «Чистый». */
data class AppColors(
    val bg: Color,
    val card: Color,
    val text: Color,
    val muted: Color,
    val track: Color,
    val divider: Color,
    val blue: Color,
    val blueTint: Color,
    val orange: Color,
    val orangeTint: Color,
    val onAccent: Color,
    val ok: Color,
)

val LightColors = AppColors(
    bg = Color(0xFFF5F6F8),
    card = Color(0xFFFFFFFF),
    text = Color(0xFF111318),
    muted = Color(0xFF5B616E),
    track = Color(0xFFE3E6EC),
    divider = Color(0xFFECEEF2),
    blue = Color(0xFF2F5BEA),
    blueTint = Color(0xFFEAF0FF),
    orange = Color(0xFFC2410C),
    orangeTint = Color(0xFFFFF1E8),
    onAccent = Color(0xFFFFFFFF),
    ok = Color(0xFF15803D),
)

val DarkColors = AppColors(
    bg = Color(0xFF0F1115),
    card = Color(0xFF1A1D23),
    text = Color(0xFFF2F3F5),
    muted = Color(0xFFA7ADB8),
    track = Color(0xFF2C3039),
    divider = Color(0xFF2C3039),
    blue = Color(0xFF8CA6FF),
    blueTint = Color(0xFF1E2740),
    orange = Color(0xFFFF9A5C),
    orangeTint = Color(0xFF3A2418),
    onAccent = Color(0xFF0F1115),
    ok = Color(0xFF4ADE80),
)

val LocalAppColors = staticCompositionLocalOf { LightColors }

@Composable
fun DoZvonkaTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val c = if (dark) DarkColors else LightColors
    val scheme = if (dark) {
        darkColorScheme(primary = c.blue, background = c.bg, surface = c.card, onSurface = c.text)
    } else {
        lightColorScheme(primary = c.blue, background = c.bg, surface = c.card, onSurface = c.text)
    }
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(LocalAppColors provides c, content = content)
    }
}
