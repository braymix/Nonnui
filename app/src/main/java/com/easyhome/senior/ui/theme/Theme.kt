package com.easyhome.senior.ui.theme

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
    background = Color(0xFFF5F5F5),
    onBackground = Color(0xFF000000),
    topBar = Color(0xFFFFFFFF),
    onTopBar = Color(0xFF000000),
    border = Color(0xFF000000),
    shadow = Color(0xFF000000),
    keyBackground = Color(0xFFFFFFFF),
    onKey = Color(0xFF000000),
    keyboardBackground = Color(0xFFD6D6D6),
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
    topBar = Color(0xFF1C1C1C),
    onTopBar = Color(0xFFFFFFFF),
    border = Color(0xFFFFFFFF),
    shadow = Color(0xFF5A5A5A),
    keyBackground = Color(0xFF2B2B2B),
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

@Composable
fun EasyHomeTheme(theme: AppTheme, content: @Composable () -> Unit) {
    val colors = seniorColorsFor(theme)
    val scheme = if (colors.isDark) {
        darkColorScheme(
            primary = colors.onBackground,
            onPrimary = colors.background,
            background = colors.background,
            onBackground = colors.onBackground,
            surface = colors.topBar,
            onSurface = colors.onTopBar,
            error = colors.danger,
            onError = colors.onDanger,
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF0D47A1),
            onPrimary = Color.White,
            background = colors.background,
            onBackground = colors.onBackground,
            surface = colors.topBar,
            onSurface = colors.onTopBar,
            error = colors.danger,
            onError = colors.onDanger,
        )
    }
    CompositionLocalProvider(LocalSeniorColors provides colors) {
        MaterialTheme(colorScheme = scheme, typography = SeniorTypography, content = content)
    }
}

/** Tema sobrio e leggibile per il configuratore dei familiari. */
@Composable
fun AdminTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF0D47A1),
            onPrimary = Color.White,
            secondary = Color(0xFF1B5E20),
            onSecondary = Color.White,
            error = Color(0xFFB71C1C),
            background = Color(0xFFFAFAFA),
            surface = Color.White,
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
