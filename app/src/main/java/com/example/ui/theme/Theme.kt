package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
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

enum class AppThemePalette(
    val code: String,
    val titleFa: String,
    val subtitleFa: String,
    val previewPrimary: Color,
    val previewBg: Color,
    val isDark: Boolean = true
) {
    OBSIDIAN_TEAL(
        code = "OBSIDIAN_TEAL",
        titleFa = "مشکی OLED و لیمویی نئون",
        subtitleFa = "کنتراست مطلق ذغالی/مشکی با نقاط تمرکز نئونی (پیشنهادی)",
        previewPrimary = NeonLime,
        previewBg = ObsidianBg,
        isDark = true
    ),
    EMERALD_MATRIX(
        code = "EMERALD_MATRIX",
        titleFa = "زمردی و فیروزه‌ای OLED",
        subtitleFa = "مشکی خالص با سیگنال‌های فیروزه‌ای و زمردی",
        previewPrimary = ElectricTeal,
        previewBg = MatrixBg,
        isDark = true
    ),
    ROYAL_AMETHYST(
        code = "ROYAL_AMETHYST",
        titleFa = "بنفش کهکشانی (Raycast)",
        subtitleFa = "ترکیب بنفش آمیتیست و رز کوارتز روی پس‌زمینه OLED",
        previewPrimary = AmethystPrimary,
        previewBg = AmethystBg,
        isDark = true
    ),
    AMBER_CYBER(
        code = "AMBER_CYBER",
        titleFa = "طلایی و کهربایی گرم",
        subtitleFa = "طیف گرم اسپرسو و طلایی بدون نور آبی",
        previewPrimary = AmberCyberPrimary,
        previewBg = AmberCyberBg,
        isDark = true
    ),
    NORDIC_FROST(
        code = "NORDIC_FROST",
        titleFa = "آبی قطبی (Nordic)",
        subtitleFa = "مشکی سرمه‌ای با سیگنال‌های آبی یخی",
        previewPrimary = NordicPrimary,
        previewBg = NordicBg,
        isDark = true
    ),
    MINIMAL_DAYLIGHT(
        code = "MINIMAL_DAYLIGHT",
        titleFa = "روشن کاغذی (Things 3)",
        subtitleFa = "پوسته روشن مینیمال مناسب محیط‌های پرنور",
        previewPrimary = LightPrimaryTeal,
        previewBg = LightBg,
        isDark = false
    );

    companion object {
        fun fromCode(code: String): AppThemePalette =
            entries.firstOrNull { it.code == code } ?: OBSIDIAN_TEAL
    }
}

enum class AppLayoutStyle(
    val code: String,
    val titleFa: String,
    val subtitleFa: String
) {
    BENTO_GRID(
        code = "BENTO_GRID",
        titleFa = "مومنتوم بنتو (Momentum)",
        subtitleFa = "کارت هوشمند، نوار پیشرفت کپسولی و گرید ۲ ستونی مدرن"
    ),
    COMPACT_PRO(
        code = "COMPACT_PRO",
        titleFa = "فشرده حرفه‌ای (Raycast)",
        subtitleFa = "نمایش حداکثری اطلاعات در یک نگاه و اولویت تسک‌ها در بالا"
    ),
    MINIMAL_ZEN(
        code = "MINIMAL_ZEN",
        titleFa = "خلوت و تمرکزگرا (Things 3 Zen)",
        subtitleFa = "حذف شلوغی‌ها؛ فقط بودجه امروز، ثبت سریع و تسک‌های اصلی"
    ),
    KANBAN_BOARD(
        code = "KANBAN_BOARD",
        titleFa = "برد دسته‌بندی (Superlist Board)",
        subtitleFa = "تفکیک ستونی بخش‌های مالی، سلامت و تسک‌ها"
    );

    companion object {
        fun fromCode(code: String): AppLayoutStyle =
            entries.firstOrNull { it.code == code } ?: BENTO_GRID
    }
}

// Strict OLED-Optimized Dark Color Scheme with Charcoal/Black Background & High-Contrast Neon Lime / Teal Accents
private val ObsidianTealColorScheme = darkColorScheme(
    primary = NeonLime,
    onPrimary = OnNeonLime,
    primaryContainer = NeonLimeContainer,
    onPrimaryContainer = NeonLime,
    secondary = ElectricTeal,
    onSecondary = OnElectricTeal,
    secondaryContainer = ElectricTealContainer,
    onSecondaryContainer = ElectricTeal,
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

private val EmeraldMatrixColorScheme = darkColorScheme(
    primary = ElectricTeal,
    onPrimary = OnElectricTeal,
    primaryContainer = ElectricTealContainer,
    onPrimaryContainer = ElectricTeal,
    secondary = EmeraldSuccess,
    onSecondary = Color(0xFF002114),
    secondaryContainer = MatrixContainer,
    onSecondaryContainer = Color(0xFFD1FAE5),
    tertiary = NeonLime,
    background = MatrixBg,
    onBackground = TextPrimaryDark,
    surface = MatrixSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = MatrixCardSurface,
    onSurfaceVariant = Color(0xFFA7F3D0),
    surfaceContainerHigh = ElevatedCardSurface,
    outline = SubtleBorderDark,
    error = RoseError,
    onError = Color.White
)

private val RoyalAmethystColorScheme = darkColorScheme(
    primary = AmethystPrimary,
    onPrimary = Color(0xFF1F003B),
    primaryContainer = AmethystContainer,
    onPrimaryContainer = Color(0xFFE9D5FF),
    secondary = AmethystSecondary,
    onSecondary = Color(0xFF3B0764),
    secondaryContainer = Color(0xFF4A044E),
    onSecondaryContainer = Color(0xFFFCE7F3),
    tertiary = SkyInfo,
    background = AmethystBg,
    onBackground = TextPrimaryDark,
    surface = AmethystSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = AmethystCardSurface,
    onSurfaceVariant = Color(0xFFD8B4FE),
    surfaceContainerHigh = ElevatedCardSurface,
    outline = Color(0x22D8B4FE),
    error = RoseError,
    onError = Color.White
)

private val AmberCyberColorScheme = darkColorScheme(
    primary = AmberCyberPrimary,
    onPrimary = Color(0xFF271300),
    primaryContainer = AmberCyberContainer,
    onPrimaryContainer = Color(0xFFFDE68A),
    secondary = AmberCyberSecondary,
    onSecondary = Color(0xFF271300),
    secondaryContainer = Color(0xFF431407),
    onSecondaryContainer = Color(0xFFFFEDD5),
    tertiary = EmeraldSuccess,
    background = AmberCyberBg,
    onBackground = Color(0xFFFEF3C7),
    surface = AmberCyberSurface,
    onSurface = Color(0xFFFEF3C7),
    surfaceVariant = AmberCyberCardSurface,
    onSurfaceVariant = Color(0xFFFCD34D),
    surfaceContainerHigh = ElevatedCardSurface,
    outline = Color(0x22FCD34D),
    error = RoseError,
    onError = Color.White
)

private val NordicFrostColorScheme = darkColorScheme(
    primary = NordicPrimary,
    onPrimary = Color(0xFF001E2F),
    primaryContainer = NordicContainer,
    onPrimaryContainer = Color(0xFFBAE6FD),
    secondary = NordicSecondary,
    onSecondary = Color(0xFF1E1B4B),
    secondaryContainer = Color(0xFF1E1B4B),
    onSecondaryContainer = Color(0xFFE0E7FF),
    tertiary = ElectricTeal,
    background = NordicBg,
    onBackground = TextPrimaryDark,
    surface = NordicSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = NordicCardSurface,
    onSurfaceVariant = Color(0xFF93C5FD),
    surfaceContainerHigh = ElevatedCardSurface,
    outline = Color(0x2293C5FD),
    error = RoseError,
    onError = Color.White
)

private val MinimalDaylightColorScheme = lightColorScheme(
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

fun resolveColorScheme(themePalette: String, darkTheme: Boolean): ColorScheme {
    val palette = AppThemePalette.fromCode(themePalette)
    if (!darkTheme && palette == AppThemePalette.MINIMAL_DAYLIGHT) {
        return MinimalDaylightColorScheme
    }
    return when (palette) {
        AppThemePalette.OBSIDIAN_TEAL -> ObsidianTealColorScheme
        AppThemePalette.EMERALD_MATRIX -> EmeraldMatrixColorScheme
        AppThemePalette.ROYAL_AMETHYST -> RoyalAmethystColorScheme
        AppThemePalette.AMBER_CYBER -> AmberCyberColorScheme
        AppThemePalette.NORDIC_FROST -> NordicFrostColorScheme
        AppThemePalette.MINIMAL_DAYLIGHT -> MinimalDaylightColorScheme
    }
}

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

/**
 * Configures MaterialTheme with strict OLED-optimized dark theme by default,
 * explicit RTL layout direction (LayoutDirection.Rtl for Persian-first rendering),
 * high-contrast accents, and Vazirmatn + Tabular Figures (tnum) Typography.
 */
@Composable
fun PersonalLifeOsTheme(
    darkTheme: Boolean = true,
    themePalette: String = AppThemePalette.OBSIDIAN_TEAL.code,
    isRtl: Boolean = true,
    layoutDirection: LayoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
    content: @Composable () -> Unit
) {
    val colorScheme = resolveColorScheme(themePalette = themePalette, darkTheme = darkTheme)

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

