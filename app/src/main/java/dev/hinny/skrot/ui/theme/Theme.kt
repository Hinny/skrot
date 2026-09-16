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

/*
 * The accents below are deliberately saturated mid-tones rather than the
 * pastel tone-80 primaries Material hands out for dark schemes: the pastels
 * read as a children's book on a black background. Each dark primary still
 * clears 4.5:1 against both its onPrimary and the background, and the light
 * primaries are deep enough (navy, blood, forest) to carry white text.
 */

private val Moss = Palette(
    dark = darkColorScheme(
        primary = Color(0xFF5E9E4A),
        onPrimary = Color(0xFF06210A),
        primaryContainer = Color(0xFF1F4A1E),
        onPrimaryContainer = Color(0xFFC4E8B8),
        secondary = Color(0xFF9DB58F),
        onSecondary = Color(0xFF1A2A18),
        tertiary = Color(0xFF7FB3A8),
        background = Color(0xFF0D120C),
        onBackground = Color(0xFFE0E4DA),
        surface = Color(0xFF0D120C),
        onSurface = Color(0xFFE0E4DA),
        surfaceVariant = Color(0xFF313A2E),
        onSurfaceVariant = Color(0xFFBBC6B3),
    ),
    light = lightColorScheme(
        primary = Color(0xFF2E5C1E),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFC0E0AE),
        onPrimaryContainer = Color(0xFF072100),
        secondary = Color(0xFF4E5E46),
        onSecondary = Color(0xFFFFFFFF),
        tertiary = Color(0xFF2F5F58),
        background = Color(0xFFF5F7F0),
        onBackground = Color(0xFF181C16),
        surface = Color(0xFFF5F7F0),
        onSurface = Color(0xFF181C16),
    ),
)

private val Ocean = Palette(
    dark = darkColorScheme(
        primary = Color(0xFF3E86D8),
        onPrimary = Color(0xFF031B36),
        primaryContainer = Color(0xFF123A6B),
        onPrimaryContainer = Color(0xFFBBD4F5),
        secondary = Color(0xFF93A9C4),
        onSecondary = Color(0xFF172636),
        tertiary = Color(0xFF6FA8B8),
        background = Color(0xFF0B1119),
        onBackground = Color(0xFFDEE3EA),
        surface = Color(0xFF0B1119),
        onSurface = Color(0xFFDEE3EA),
        surfaceVariant = Color(0xFF2E3742),
        onSurfaceVariant = Color(0xFFB8C2CE),
    ),
    light = lightColorScheme(
        primary = Color(0xFF0F3F86),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFBBD2F0),
        onPrimaryContainer = Color(0xFF001A3A),
        secondary = Color(0xFF465A72),
        onSecondary = Color(0xFFFFFFFF),
        tertiary = Color(0xFF2A5F70),
        background = Color(0xFFF3F6FA),
        onBackground = Color(0xFF161A20),
        surface = Color(0xFFF3F6FA),
        onSurface = Color(0xFF161A20),
    ),
)

private val Blood = Palette(
    dark = darkColorScheme(
        primary = Color(0xFFC93B3B),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFF6E1414),
        onPrimaryContainer = Color(0xFFF5C4C0),
        secondary = Color(0xFFC49A97),
        onSecondary = Color(0xFF331B19),
        tertiary = Color(0xFFD09A5E),
        background = Color(0xFF140D0D),
        onBackground = Color(0xFFE9DEDD),
        surface = Color(0xFF140D0D),
        onSurface = Color(0xFFE9DEDD),
        surfaceVariant = Color(0xFF3F3131),
        onSurfaceVariant = Color(0xFFCCBAB8),
    ),
    light = lightColorScheme(
        primary = Color(0xFF8E1B1B),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFF0C4C0),
        onPrimaryContainer = Color(0xFF3F0000),
        secondary = Color(0xFF6E4E4C),
        onSecondary = Color(0xFFFFFFFF),
        tertiary = Color(0xFF7A5230),
        background = Color(0xFFFAF4F3),
        onBackground = Color(0xFF1F1818),
        surface = Color(0xFFFAF4F3),
        onSurface = Color(0xFF1F1818),
    ),
)

private val Iron = Palette(
    dark = darkColorScheme(
        primary = Color(0xFF9AA5B1),
        onPrimary = Color(0xFF1A2027),
        primaryContainer = Color(0xFF3A434D),
        onPrimaryContainer = Color(0xFFDCE3EA),
        secondary = Color(0xFFAAB0B8),
        onSecondary = Color(0xFF23282E),
        tertiary = Color(0xFFB8A58C),
        background = Color(0xFF0F1113),
        onBackground = Color(0xFFE2E4E7),
        surface = Color(0xFF0F1113),
        onSurface = Color(0xFFE2E4E7),
        surfaceVariant = Color(0xFF33383E),
        onSurfaceVariant = Color(0xFFC0C6CD),
    ),
    light = lightColorScheme(
        primary = Color(0xFF3A4653),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFD3DBE3),
        onPrimaryContainer = Color(0xFF0F161D),
        secondary = Color(0xFF555C64),
        onSecondary = Color(0xFFFFFFFF),
        tertiary = Color(0xFF6B5C48),
        background = Color(0xFFF4F5F7),
        onBackground = Color(0xFF191B1E),
        surface = Color(0xFFF4F5F7),
        onSurface = Color(0xFF191B1E),
    ),
)

private val Violet = Palette(
    dark = darkColorScheme(
        primary = Color(0xFF9B7BE0),
        onPrimary = Color(0xFF1E0B4A),
        primaryContainer = Color(0xFF3F2A7A),
        onPrimaryContainer = Color(0xFFDCD0FF),
        secondary = Color(0xFFB0A6C6),
        onSecondary = Color(0xFF272138),
        tertiary = Color(0xFFC98CA6),
        background = Color(0xFF100E16),
        onBackground = Color(0xFFE3DFEA),
        surface = Color(0xFF100E16),
        onSurface = Color(0xFFE3DFEA),
        surfaceVariant = Color(0xFF36313F),
        onSurfaceVariant = Color(0xFFC2BBCC),
    ),
    light = lightColorScheme(
        primary = Color(0xFF4B2E9A),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFD9CCF5),
        onPrimaryContainer = Color(0xFF1B0060),
        secondary = Color(0xFF5B5470),
        onSecondary = Color(0xFFFFFFFF),
        tertiary = Color(0xFF7A4B5F),
        background = Color(0xFFF6F4FA),
        onBackground = Color(0xFF1B181F),
        surface = Color(0xFFF6F4FA),
        onSurface = Color(0xFF1B181F),
    ),
)

private fun ColorTheme.palette(): Palette = when (this) {
    ColorTheme.AMBER -> Amber
    ColorTheme.MOSS -> Moss
    ColorTheme.OCEAN -> Ocean
    ColorTheme.BLOOD -> Blood
    ColorTheme.IRON -> Iron
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
