package com.example.personallifeos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class ExtendedColors(
    val elevatedSurface: Color,
    val border: Color,
    val textMuted: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val cardHover: Color,
    val primaryGlow: Color
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        elevatedSurface = DarkElevatedSurface,
        border = DarkBorder,
        textMuted = DarkTextMuted,
        success = DarkSuccess,
        warning = DarkWarning,
        danger = DarkDanger,
        cardHover = DarkCardHover,
        primaryGlow = DarkPrimaryGlow
    )
}

private val DarkColorScheme: ColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkBg,
    primaryContainer = DarkElevatedSurface,
    onPrimaryContainer = DarkPrimaryGlow,
    secondary = BlueSecondary,
    onSecondary = DarkTextPrimary,
    tertiary = DarkSuccess,
    onTertiary = DarkBg,
    background = DarkBg,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkElevatedSurface,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    outlineVariant = DarkBorder.copy(alpha = 0.5f),
    error = DarkDanger,
    onError = DarkTextPrimary
)

private val LightColorScheme: ColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F7FA),
    onPrimaryContainer = LightPrimary,
    secondary = BlueSecondary,
    onSecondary = Color.White,
    tertiary = LightSuccess,
    onTertiary = Color.White,
    background = LightBg,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightCardHover,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    outlineVariant = LightBorder.copy(alpha = 0.6f),
    error = LightDanger,
    onError = Color.White
)

@Composable
fun PersonalLifeOSTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) {
        ExtendedColors(
            elevatedSurface = DarkElevatedSurface,
            border = DarkBorder,
            textMuted = DarkTextMuted,
            success = DarkSuccess,
            warning = DarkWarning,
            danger = DarkDanger,
            cardHover = DarkCardHover,
            primaryGlow = DarkPrimaryGlow
        )
    } else {
        ExtendedColors(
            elevatedSurface = LightElevatedSurface,
            border = LightBorder,
            textMuted = LightTextMuted,
            success = LightSuccess,
            warning = LightWarning,
            danger = LightDanger,
            cardHover = LightCardHover,
            primaryGlow = LightPrimaryGlow
        )
    }

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

object AppTheme {
    val extendedColors: ExtendedColors
        @Composable
        get() = LocalExtendedColors.current
}
