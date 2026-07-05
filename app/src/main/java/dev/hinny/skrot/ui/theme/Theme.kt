package dev.hinny.skrot.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import dev.hinny.skrot.data.model.ThemeMode

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB74D),
    onPrimary = Color(0xFF442B00),
    primaryContainer = Color(0xFF624000),
    onPrimaryContainer = Color(0xFFFFDDB3),
    secondary = Color(0xFFDDC2A1),
    onSecondary = Color(0xFF3E2D16),
    tertiary = Color(0xFFB8CEA1),
    background = Color(0xFF17130E),
    onBackground = Color(0xFFEBE1D9),
    surface = Color(0xFF17130E),
    onSurface = Color(0xFFEBE1D9),
    surfaceVariant = Color(0xFF4F4539),
    onSurfaceVariant = Color(0xFFD3C4B4),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF815512),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDDB3),
    onPrimaryContainer = Color(0xFF2A1800),
    secondary = Color(0xFF705B41),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFF52643F),
    background = Color(0xFFFFF8F3),
    onBackground = Color(0xFF201B13),
    surface = Color(0xFFFFF8F3),
    onSurface = Color(0xFF201B13),
)

@Composable
fun SkrotTheme(themeMode: ThemeMode = ThemeMode.DARK, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        content = content,
    )
}
