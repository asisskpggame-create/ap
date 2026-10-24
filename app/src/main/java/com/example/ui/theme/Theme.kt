package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val PersonalLifeOsDarkColorScheme = darkColorScheme(
    primary = ElectricTeal,
    onPrimary = OnElectricTeal,
    primaryContainer = ElectricTealContainer,
    onPrimaryContainer = ElectricTeal,
    secondary = MutedViolet,
    onSecondary = OnMutedViolet,
    secondaryContainer = MutedVioletContainer,
    onSecondaryContainer = Color(0xFFDDD6FE),
    tertiary = SkyInfo,
    onTertiary = Color(0xFF001F2A),
    background = ObsidianBg,
    onBackground = TextPrimaryDark,
    surface = CharcoalSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = LayeredCardSurface,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerHigh = ElevatedCardSurface,
    outline = SubtleBorderDark,
    error = RoseError,
    onError = Color.White
)

private val PersonalLifeOsLightColorScheme = lightColorScheme(
    primary = LightPrimaryTeal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB2DFDB),
    onPrimaryContainer = Color(0xFF004D40),
    secondary = LightSecondaryViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE7F6),
    onSecondaryContainer = Color(0xFF311B92),
    tertiary = Color(0xFF0284C7),
    background = LightBg,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = Color(0xFFCBD5E1),
    error = RoseError,
    onError = Color.White
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/**
 * Configures MaterialTheme with explicit RTL layout direction (defaulting to LayoutDirection.Rtl
 * for Persian-first rendering) and applies the Vazirmatn + Tabular Figures (tnum) Typography.
 */
@Composable
fun PersonalLifeOsTheme(
    darkTheme: Boolean = true,
    isRtl: Boolean = true,
    layoutDirection: LayoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) PersonalLifeOsDarkColorScheme else PersonalLifeOsLightColorScheme

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    layoutDirection: LayoutDirection = LayoutDirection.Rtl,
    content: @Composable () -> Unit
) {
    PersonalLifeOsTheme(
        darkTheme = darkTheme,
        isRtl = layoutDirection == LayoutDirection.Rtl,
        layoutDirection = layoutDirection,
        content = content
    )
}
