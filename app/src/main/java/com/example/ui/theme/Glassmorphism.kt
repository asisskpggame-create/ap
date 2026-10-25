package com.example.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Liquid Glass & Glassmorphism design tokens and components.
 * Creates an ultra-premium, tactile frosted glass look with specular rim reflections,
 * ambient liquid light orbs, and luminous depth over pitch OLED black.
 */
object GlassmorphismDefaults {

    // Liquid Glass Specular Rim Border Gradients (delegates to centralized GlassTheme)
    fun specularBorderBrush(
        accentColor: Color? = null,
        topIntensity: Float = 0.55f,
        bottomAlpha: Float = 0.05f
    ): Brush = GlassTheme.specularBorderBrush(accentColor, topIntensity, bottomAlpha)

    // Frosted Translucent Dark Glass Surface Brush (delegates to centralized GlassTheme)
    fun frostedGlassBrush(
        baseColor: Color = Color(0xFF0F1612),
        alphaTop: Float = 0.72f,
        alphaBottom: Float = 0.55f
    ): Brush = GlassTheme.frostedGlassBrush(baseColor, alphaTop, alphaBottom)

    // Top Gloss Sheen (Light catch reflection on the glass surface)
    val GlossSheenBrush: Brush get() = GlassTheme.GlossSheenBrush
}

/**
 * Atmospheric Liquid Glow Backdrop.
 * Renders an OLED Pure Black canvas with soft diffused fluid mesh gradients
 * that refract and glow through translucent liquid glass cards as the user scrolls.
 */
@Composable
fun LiquidGlassBackdrop(
    modifier: Modifier = Modifier,
    primaryGlow: Color = MaterialTheme.colorScheme.primary,
    secondaryGlow: Color = MaterialTheme.colorScheme.secondary,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .drawBehind {
                val canvasWidth = size.width
                val canvasHeight = size.height
                if (canvasWidth <= 1f || canvasHeight <= 1f) return@drawBehind

                val r1 = (canvasWidth * 1.05f).coerceAtLeast(1f)
                val r2 = (canvasWidth * 0.70f).coerceAtLeast(1f)
                val r3 = (canvasWidth * 0.65f).coerceAtLeast(1f)
                val r4 = (canvasWidth * 0.75f).coerceAtLeast(1f)

                // Fluid Dome 1: Top Volumetric Aura (Directly inspired by the luxury hero screenshot)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryGlow.copy(alpha = 0.32f),
                            primaryGlow.copy(alpha = 0.18f),
                            secondaryGlow.copy(alpha = 0.07f),
                            Color.Transparent
                        ),
                        center = Offset(canvasWidth * 0.5f, 0f),
                        radius = r1
                    ),
                    radius = r1,
                    center = Offset(canvasWidth * 0.5f, 0f)
                )

                // Fluid Orb 2: Top-Left Refractive Mesh
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryGlow.copy(alpha = 0.22f),
                            secondaryGlow.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = Offset(canvasWidth * 0.15f, canvasHeight * 0.18f),
                        radius = r2
                    ),
                    radius = r2,
                    center = Offset(canvasWidth * 0.15f, canvasHeight * 0.18f)
                )

                // Fluid Orb 3: Mid-Right Secondary Light Pool
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            secondaryGlow.copy(alpha = 0.14f),
                            primaryGlow.copy(alpha = 0.04f),
                            Color.Transparent
                        ),
                        center = Offset(canvasWidth * 0.88f, canvasHeight * 0.52f),
                        radius = r3
                    ),
                    radius = r3,
                    center = Offset(canvasWidth * 0.88f, canvasHeight * 0.52f)
                )

                // Fluid Orb 4: Bottom Ambient Glow (Luminous Dock Reflection)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryGlow.copy(alpha = 0.16f),
                            secondaryGlow.copy(alpha = 0.06f),
                            Color.Transparent
                        ),
                        center = Offset(canvasWidth * 0.5f, canvasHeight * 0.98f),
                        radius = r4
                    ),
                    radius = r4,
                    center = Offset(canvasWidth * 0.5f, canvasHeight * 0.98f)
                )
            },
        content = content
    )
}

/**
 * Ultra-Polished Liquid Glass Card.
 * Combines translucent frosted glass tinting, a 3D specular rim border,
 * and a subtle refractive gloss highlight.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    accentBorderColor: Color? = null,
    borderWidth: Dp = 1.2.dp,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    GlassCard(
        modifier = modifier,
        shape = shape,
        accentBorderColor = accentBorderColor,
        borderWidth = borderWidth,
        onClick = onClick,
        onLongClick = onLongClick,
        content = content
    )
}

/**
 * Liquid Glass Pill / Chip for badges, buttons, and status indicators.
 * Delegates to centralized GlassPill.
 */
@Composable
fun LiquidGlassPill(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(50),
    accentGlow: Color? = null,
    borderWidth: Dp = 1.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    GlassPill(
        modifier = modifier,
        shape = shape,
        accentGlow = accentGlow,
        borderWidth = borderWidth,
        onClick = onClick,
        content = content
    )
}
