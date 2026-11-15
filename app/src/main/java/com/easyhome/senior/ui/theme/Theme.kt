package com.easyhome.senior.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.easyhome.senior.domain.model.AppTheme

@Immutable
data class SeniorColors(
    val background: Color,
    val onBackground: Color,
    val topBar: Color,
    val onTopBar: Color,
    val border: Color,
    val shadow: Color,
    val keyBackground: Color,
    val onKey: Color,
    val keyboardBackground: Color,
    val danger: Color,
    val onDanger: Color,
    val success: Color,
    val onSuccess: Color,
    val warning: Color,
    val onWarning: Color,
    val isHighContrast: Boolean,
    val isDark: Boolean,
)

val LightSeniorColors = SeniorColors(
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF000000),
    topBar = Color(0xFF0D47A1),
    onTopBar = Color(0xFFFFFFFF),
    border = Color(0xFF000000),
    shadow = Color(0xFF000000),
    keyBackground = Color(0xFFFFFFFF),
    onKey = Color(0xFF000000),
    keyboardBackground = Color(0xFF37474F),
    danger = Color(0xFFB71C1C),
    onDanger = Color(0xFFFFFFFF),
    success = Color(0xFF1B5E20),
    onSuccess = Color(0xFFFFFFFF),
    warning = Color(0xFFFFD600),
    onWarning = Color(0xFF000000),
    isHighContrast = false,
    isDark = false,
)

val DarkSeniorColors = SeniorColors(
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    topBar = Color(0xFF212121),
    onTopBar = Color(0xFFFFFFFF),
    border = Color(0xFFFFFFFF),
    shadow = Color(0xFF616161),
    keyBackground = Color(0xFF303030),
    onKey = Color(0xFFFFFFFF),
    keyboardBackground = Color(0xFF000000),
    danger = Color(0xFFD32F2F),
    onDanger = Color(0xFFFFFFFF),
    success = Color(0xFF2E7D32),
    onSuccess = Color(0xFFFFFFFF),
    warning = Color(0xFFFFD600),
    onWarning = Color(0xFF000000),
    isHighContrast = false,
    isDark = true,
)

val HighContrastSeniorColors = SeniorColors(
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFF00),
    topBar = Color(0xFF000000),
    onTopBar = Color(0xFFFFFF00),
    border = Color(0xFFFFFF00),
    shadow = Color(0xFF7A7A00),
    keyBackground = Color(0xFF000000),
    onKey = Color(0xFFFFFF00),
    keyboardBackground = Color(0xFF000000),
    danger = Color(0xFFFF1744),
    onDanger = Color(0xFF000000),
    success = Color(0xFF00E676),
    onSuccess = Color(0xFF000000),
    warning = Color(0xFFFFFF00),
    onWarning = Color(0xFF000000),
    isHighContrast = true,
    isDark = true,
)

val LocalSeniorColors = staticCompositionLocalOf { LightSeniorColors }

object SeniorTheme {
    val colors: SeniorColors
        @Composable
        @ReadOnlyComposable
        get() = LocalSeniorColors.current
}

fun seniorColorsFor(theme: AppTheme): SeniorColors = when (theme) {
    AppTheme.LIGHT -> LightSeniorColors
    AppTheme.DARK -> DarkSeniorColors
    AppTheme.HIGH_CONTRAST -> HighContrastSeniorColors
}

private val SeniorTypography = Typography(
    bodyLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontSize = 18.sp, lineHeight = 24.sp),
    titleLarge = TextStyle(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
    labelLarge = TextStyle(fontSize = 18.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold),
)

/**
 * Schema Material 3 COMPLETO: ogni ruolo (container, surfaceContainer*, outline...) è
 * esplicito. I valori di default di Material 3 sono toni pastello lilla/rosa a basso
 * contrasto, inadatti a chi vede poco: qui solo colori pieni, neutri e bordi scuri.
 */
private fun strongColorScheme(
    dark: Boolean,
    primary: Color,
    onPrimary: Color,
    background: Color,
    onBackground: Color,
    surfaceLow: Color,
    surfaceHigh: Color,
    outline: Color,
    error: Color,
    onError: Color,
): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primary,
        onPrimaryContainer = onPrimary,
        inversePrimary = primary,
        secondary = primary,
        onSecondary = onPrimary,
        // Slider, chip, badge: grigio neutro invece del lilla di default.
        secondaryContainer = surfaceHigh,
        onSecondaryContainer = onBackground,
        tertiary = primary,
        onTertiary = onPrimary,
        tertiaryContainer = primary,
        onTertiaryContainer = onPrimary,
        background = background,
        onBackground = onBackground,
        surface = background,
        onSurface = onBackground,
        surfaceVariant = surfaceHigh,
        onSurfaceVariant = onBackground,
        // Nessuna tinta colorata sulle superfici elevate.
        surfaceTint = background,
        inverseSurface = onBackground,
        inverseOnSurface = background,
        error = error,
        onError = onError,
        errorContainer = error,
        onErrorContainer = onError,
        outline = outline,
        outlineVariant = outline,
        scrim = Color.Black,
        surfaceBright = background,
        surfaceContainer = surfaceLow,
        surfaceContainerHigh = surfaceLow,
        surfaceContainerHighest = surfaceHigh,
        surfaceContainerLow = background,
        surfaceContainerLowest = background,
        surfaceDim = surfaceHigh,
    )
}

@Composable
fun EasyHomeTheme(theme: AppTheme, content: @Composable () -> Unit) {
    val colors = seniorColorsFor(theme)
    val scheme = when (theme) {
        AppTheme.LIGHT -> strongColorScheme(
            dark = false,
            primary = Color(0xFF0D47A1), onPrimary = Color.White,
            background = Color.White, onBackground = Color.Black,
            surfaceLow = Color.White, surfaceHigh = Color(0xFFE0E0E0),
            outline = Color.Black, error = colors.danger, onError = colors.onDanger,
        )
        AppTheme.DARK -> strongColorScheme(
            dark = true,
            primary = Color.White, onPrimary = Color.Black,
            background = Color.Black, onBackground = Color.White,
            surfaceLow = Color(0xFF212121), surfaceHigh = Color(0xFF424242),
            outline = Color.White, error = colors.danger, onError = colors.onDanger,
        )
        AppTheme.HIGH_CONTRAST -> strongColorScheme(
            dark = true,
            primary = Color(0xFFFFFF00), onPrimary = Color.Black,
            background = Color.Black, onBackground = Color(0xFFFFFF00),
            surfaceLow = Color.Black, surfaceHigh = Color(0xFF333300),
            outline = Color(0xFFFFFF00), error = colors.danger, onError = colors.onDanger,
        )
    }
    CompositionLocalProvider(LocalSeniorColors provides colors) {
        MaterialTheme(colorScheme = scheme, typography = SeniorTypography, content = content)
    }
}

/** Tema nitido (niente pastello) per il configuratore dei familiari. */
@Composable
fun AdminTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = strongColorScheme(
            dark = false,
            primary = Color(0xFF0D47A1), onPrimary = Color.White,
            background = Color.White, onBackground = Color.Black,
            surfaceLow = Color(0xFFF5F5F5), surfaceHigh = Color(0xFFE0E0E0),
            outline = Color(0xFF424242), error = Color(0xFFB71C1C), onError = Color.White,
        ),
        typography = Typography(),
        content = content,
    )
}

/** Colori (sfondo, testo) di un pulsante della Home in base al tema attivo. */
@Composable
@ReadOnlyComposable
fun homeButtonColors(colorArgb: Long): Pair<Color, Color> {
    val colors = SeniorTheme.colors
    if (colors.isHighContrast) return Color.Black to Color(0xFFFFFF00)
    val bg = Color(colorArgb)
    val fg = if (bg.luminance() > 0.45f) Color.Black else Color.White
    return bg to fg
}
