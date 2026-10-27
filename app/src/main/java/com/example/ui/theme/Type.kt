package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.sp
import com.example.R

val VazirmatnFontFamily = FontFamily(
    Font(R.font.vazirmatn, FontWeight.Normal),
    Font(R.font.vazirmatn, FontWeight.Medium),
    Font(R.font.vazirmatn, FontWeight.SemiBold),
    Font(R.font.vazirmatn, FontWeight.Bold)
)

val JetBrainsMonoFontFamily = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Bold),
    Font(R.font.vazirmatn, FontWeight.Normal),
    Font(R.font.vazirmatn, FontWeight.Bold)
)

// OpenType Tabular Figures (tnum) enabled across all typography styles so Persian numerals (۰-۹)
// in financial tables, Safe-to-Spend counters, timers, and task lists remain strictly aligned without horizontal jitter.
const val TABULAR_NUMS_FEATURE = "tnum"

private val PersianPlatformTextStyle = PlatformTextStyle(
    includeFontPadding = false
)

private val PersianLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None
)

private fun persianTextStyle(
    fontFamily: FontFamily = VazirmatnFontFamily,
    fontWeight: FontWeight,
    fontSizeSp: Int,
    lineHeightSp: Int,
    letterSpacingSp: Float = 0f
): TextStyle = TextStyle(
    fontFamily = fontFamily,
    fontWeight = fontWeight,
    fontSize = fontSizeSp.sp,
    lineHeight = lineHeightSp.sp,
    letterSpacing = letterSpacingSp.sp,
    fontFeatureSettings = TABULAR_NUMS_FEATURE,
    textDirection = TextDirection.ContentOrRtl,
    platformStyle = PersianPlatformTextStyle,
    lineHeightStyle = PersianLineHeightStyle
)

val TabularNumericTextStyle = persianTextStyle(
    fontFamily = VazirmatnFontFamily,
    fontWeight = FontWeight.Bold,
    fontSizeSp = 16,
    lineHeightSp = 24
)

fun TextStyle.withTabularFigures(): TextStyle = copy(
    fontFeatureSettings = TABULAR_NUMS_FEATURE,
    textDirection = TextDirection.ContentOrRtl
)

val Typography = Typography(
    displayLarge = persianTextStyle(
        fontWeight = FontWeight.Bold,
        fontSizeSp = 32,
        lineHeightSp = 44
    ),
    displayMedium = persianTextStyle(
        fontWeight = FontWeight.Bold,
        fontSizeSp = 28,
        lineHeightSp = 38
    ),
    displaySmall = persianTextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSizeSp = 24,
        lineHeightSp = 34
    ),
    headlineLarge = persianTextStyle(
        fontWeight = FontWeight.Bold,
        fontSizeSp = 26,
        lineHeightSp = 36
    ),
    headlineMedium = persianTextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSizeSp = 22,
        lineHeightSp = 30
    ),
    headlineSmall = persianTextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSizeSp = 19,
        lineHeightSp = 28
    ),
    titleLarge = persianTextStyle(
        fontWeight = FontWeight.Bold,
        fontSizeSp = 18,
        lineHeightSp = 26
    ),
    titleMedium = persianTextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSizeSp = 16,
        lineHeightSp = 24
    ),
    titleSmall = persianTextStyle(
        fontWeight = FontWeight.Medium,
        fontSizeSp = 14,
        lineHeightSp = 20
    ),
    bodyLarge = persianTextStyle(
        fontWeight = FontWeight.Normal,
        fontSizeSp = 15,
        lineHeightSp = 24
    ),
    bodyMedium = persianTextStyle(
        fontWeight = FontWeight.Normal,
        fontSizeSp = 13,
        lineHeightSp = 20
    ),
    bodySmall = persianTextStyle(
        fontWeight = FontWeight.Normal,
        fontSizeSp = 12,
        lineHeightSp = 18
    ),
    labelLarge = persianTextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSizeSp = 13,
        lineHeightSp = 18
    ),
    labelMedium = persianTextStyle(
        fontWeight = FontWeight.Medium,
        fontSizeSp = 11,
        lineHeightSp = 16
    ),
    labelSmall = persianTextStyle(
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSizeSp = 10,
        lineHeightSp = 14
    )
)
