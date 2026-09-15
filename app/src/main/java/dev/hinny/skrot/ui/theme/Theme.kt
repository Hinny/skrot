package dev.hinny.skrot.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import dev.hinny.skrot.data.model.ColorTheme
import dev.hinny.skrot.data.model.ThemeMode

/**
 * One accent family in both appearances. Only the roles the app actually
 * leans on are pinned; everything else comes from Material's defaults for a
 * dark or light scheme, which is what the original amber palette did too.
 */
private class Palette(val dark: ColorScheme, val light: ColorScheme)

private val Amber = Palette(
    dark = darkColorScheme(
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
    ),
    light = lightColorScheme(
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
    ),
)

private val Moss = Palette(
    dark = darkColorScheme(
        primary = Color(0xFFA1D39A),
        onPrimary = Color(0xFF0F3910),
        primaryContainer = Color(0xFF275026),
        onPrimaryContainer = Color(0xFFBCF0B4),
        secondary = Color(0xFFBACCB3),
        onSecondary = Color(0xFF253423),
        tertiary = Color(0xFFA0CFD0),
        background = Color(0xFF11140F),
        onBackground = Color(0xFFE1E4DC),
        surface = Color(0xFF11140F),
        onSurface = Color(0xFFE1E4DC),
        surfaceVariant = Color(0xFF424940),
        onSurfaceVariant = Color(0xFFC2C9BD),
    ),
    light = lightColorScheme(
        primary = Color(0xFF386A20),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFB7F397),
        onPrimaryContainer = Color(0xFF042100),
        secondary = Color(0xFF55624C),
        onSecondary = Color(0xFFFFFFFF),
        tertiary = Color(0xFF386666),
        background = Color(0xFFF8FAF0),
        onBackground = Color(0xFF1A1C18),
        surface = Color(0xFFF8FAF0),
        onSurface = Color(0xFF1A1C18),
    ),
)

private val Sky = Palette(
    dark = darkColorScheme(
        primary = Color(0xFF9FCAFF),
        onPrimary = Color(0xFF003258),
        primaryContainer = Color(0xFF00497D),
        onPrimaryContainer = Color(0xFFD1E4FF),
        secondary = Color(0xFFBBC7DB),
        onSecondary = Color(0xFF253140),
        tertiary = Color(0xFFD6BEE4),
        background = Color(0xFF101418),
        onBackground = Color(0xFFE0E2E8),
        surface = Color(0xFF101418),
        onSurface = Color(0xFFE0E2E8),
        surfaceVariant = Color(0xFF43474E),
        onSurfaceVariant = Color(0xFFC3C7CF),
    ),
    light = lightColorScheme(
        primary = Color(0xFF0061A4),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFD1E4FF),
        onPrimaryContainer = Color(0xFF001D36),
        secondary = Color(0xFF535F70),
        onSecondary = Color(0xFFFFFFFF),
        tertiary = Color(0xFF6B5778),
        background = Color(0xFFF8F9FF),
        onBackground = Color(0xFF191C1E),
        surface = Color(0xFFF8F9FF),
        onSurface = Color(0xFF191C1E),
    ),
)

private val Rose = Palette(
    dark = darkColorScheme(
        primary = Color(0xFFFFB1C8),
        onPrimary = Color(0xFF5E1133),
        primaryContainer = Color(0xFF7B294A),
        onPrimaryContainer = Color(0xFFFFD9E2),
        secondary = Color(0xFFE3BDC6),
        onSecondary = Color(0xFF422931),
        tertiary = Color(0xFFEFBD94),
        background = Color(0xFF191113),
        onBackground = Color(0xFFEFDFE1),
        surface = Color(0xFF191113),
        onSurface = Color(0xFFEFDFE1),
        surfaceVariant = Color(0xFF514346),
        onSurfaceVariant = Color(0xFFD5C2C6),
    ),
    light = lightColorScheme(
        primary = Color(0xFF984062),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFD9E2),
        onPrimaryContainer = Color(0xFF3E001D),
        secondary = Color(0xFF74565F),
        onSecondary = Color(0xFFFFFFFF),
        tertiary = Color(0xFF7C5635),
        background = Color(0xFFFFF8F8),
        onBackground = Color(0xFF22191C),
        surface = Color(0xFFFFF8F8),
        onSurface = Color(0xFF22191C),
    ),
)

private val Violet = Palette(
    dark = darkColorScheme(
        primary = Color(0xFFCFBCFF),
        onPrimary = Color(0xFF381E72),
        primaryContainer = Color(0xFF4F378B),
        onPrimaryContainer = Color(0xFFEADDFF),
        secondary = Color(0xFFCCC2DC),
        onSecondary = Color(0xFF332D41),
        tertiary = Color(0xFFEFB8C8),
        background = Color(0xFF141218),
        onBackground = Color(0xFFE6E0E9),
        surface = Color(0xFF141218),
        onSurface = Color(0xFFE6E0E9),
        surfaceVariant = Color(0xFF49454F),
        onSurfaceVariant = Color(0xFFCAC4D0),
    ),
    light = lightColorScheme(
        primary = Color(0xFF6750A4),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFEADDFF),
        onPrimaryContainer = Color(0xFF21005D),
        secondary = Color(0xFF625B71),
        onSecondary = Color(0xFFFFFFFF),
        tertiary = Color(0xFF7D5260),
        background = Color(0xFFFEF7FF),
        onBackground = Color(0xFF1D1B20),
        surface = Color(0xFFFEF7FF),
        onSurface = Color(0xFF1D1B20),
    ),
)

private fun ColorTheme.palette(): Palette = when (this) {
    ColorTheme.AMBER -> Amber
    ColorTheme.MOSS -> Moss
    ColorTheme.SKY -> Sky
    ColorTheme.ROSE -> Rose
    ColorTheme.VIOLET -> Violet
}

/** The accent colour of a theme, for the swatch in Settings. */
@Composable
fun ColorTheme.accent(dark: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f): Color =
    with(palette()) { if (dark) this.dark.primary else this.light.primary }

@Composable
fun SkrotTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    colorTheme: ColorTheme = ColorTheme.AMBER,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val palette = colorTheme.palette()
    MaterialTheme(
        colorScheme = if (dark) palette.dark else palette.light,
        content = content,
    )
}
