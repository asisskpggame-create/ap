package com.example.ui.theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Liquid Glass & Glassmorphism design tokens and components.
 * Creates an ultra-premium, tactile frosted glass look with specular rim reflections,
 * ambient liquid light orbs, and luminous depth.
 */
object GlassmorphismDefaults {

    // Liquid Glass Specular Rim Border Gradients (delegates to centralized GlassTheme)
    fun specularBorderBrush(
        accentColor: Color? = null,
        topIntensity: Float = 0.68f,
        bottomAlpha: Float = 0.10f
    ): Brush = GlassTheme.specularBorderBrush(accentColor, topIntensity, bottomAlpha)

    // Frosted Translucent Dark Glass Surface Brush (delegates to centralized GlassTheme)
    fun frostedGlassBrush(
        baseColor: Color = Color(0xFF0F1612),
        alphaTop: Float = 0.56f,
        alphaBottom: Float = 0.38f
    ): Brush = GlassTheme.frostedGlassBrush(baseColor, alphaTop, alphaBottom)

    // Top Gloss Sheen (Light catch reflection on the glass surface)
    val GlossSheenBrush: Brush get() = GlassTheme.GlossSheenBrush
}

/**
 * Atmospheric Liquid Glow Backdrop.
 * Renders a layered ambient mesh canvas with vibrant diffused light orbs and diagonal refraction bands
 * so translucent frosted glass cards visibly refract color and depth behind them.
 */
@Composable
fun LiquidGlassBackdrop(
    modifier: Modifier = Modifier,
    primaryGlow: Color = MaterialTheme.colorScheme.primary,
    secondaryGlow: Color = MaterialTheme.colorScheme.secondary,
    content: @Composable BoxScope.() -> Unit
) {
    val backdropBg = MaterialTheme.colorScheme.background
    val tertiaryGlow = MutedViolet
    val cyanGlow = SkyInfo

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backdropBg)
            .drawBehind {
                val canvasWidth = size.width
                val canvasHeight = size.height
                if (canvasWidth <= 1f || canvasHeight <= 1f) return@drawBehind

                val r1 = (canvasWidth * 0.95f).coerceAtLeast(1f)
                val r2 = (canvasWidth * 0.72f).coerceAtLeast(1f)
                val r3 = (canvasWidth * 0.75f).coerceAtLeast(1f)
                val r4 = (canvasWidth * 0.80f).coerceAtLeast(1f)
                val r5 = (canvasWidth * 0.60f).coerceAtLeast(1f)

                // Orb 1: Top-Center Emerald / Primary Aura
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryGlow.copy(alpha = 0.42f),
                            primaryGlow.copy(alpha = 0.20f),
                            secondaryGlow.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = Offset(canvasWidth * 0.52f, canvasHeight * 0.04f),
                        radius = r1
                    ),
                    radius = r1,
                    center = Offset(canvasWidth * 0.52f, canvasHeight * 0.04f)
                )

                // Orb 2: Upper-Left Cyan / Teal Glass Refraction Pool
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            cyanGlow.copy(alpha = 0.28f),
                            primaryGlow.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(canvasWidth * 0.12f, canvasHeight * 0.22f),
                        radius = r2
                    ),
                    radius = r2,
                    center = Offset(canvasWidth * 0.12f, canvasHeight * 0.22f)
                )

                // Orb 3: Mid-Right Violet / Magenta Contrast Orb (essential for visible glass refraction)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            tertiaryGlow.copy(alpha = 0.26f),
                            secondaryGlow.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(canvasWidth * 0.88f, canvasHeight * 0.46f),
                        radius = r3
                    ),
                    radius = r3,
                    center = Offset(canvasWidth * 0.88f, canvasHeight * 0.46f)
                )

                // Orb 4: Lower-Left Warm Emerald / Cyan Mesh
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryGlow.copy(alpha = 0.22f),
                            cyanGlow.copy(alpha = 0.10f),
                            Color.Transparent
                        ),
                        center = Offset(canvasWidth * 0.18f, canvasHeight * 0.72f),
                        radius = r5
                    ),
                    radius = r5,
                    center = Offset(canvasWidth * 0.18f, canvasHeight * 0.72f)
                )

                // Orb 5: Bottom Dock Luminous Reflection
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryGlow.copy(alpha = 0.30f),
                            tertiaryGlow.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(canvasWidth * 0.5f, canvasHeight * 0.96f),
                        radius = r4
                    ),
                    radius = r4,
                    center = Offset(canvasWidth * 0.5f, canvasHeight * 0.96f)
                )

                // Diagonal Glass Light Sheen across backdrop
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.04f),
                            Color.Transparent,
                            primaryGlow.copy(alpha = 0.03f),
                            Color.Transparent
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(canvasWidth, canvasHeight)
                    )
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
    borderWidth: Dp = 1.25.dp,
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

/**
 * Reusable Collapsible / Expandable Glass Drawer Section.
 * Makes expandable sections unmistakably obvious with:
 * - Icon badge + bold title + optional summary subtitle
 * - Interactive capsule badge ("باز" / "بسته" or custom badge)
 * - Circular glass button with 180-degree animated rotating chevron arrow
 * - Subtle bottom drawer grabber handle
 */
@Composable
fun CollapsibleGlassSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    badgeText: String? = null,
    icon: ImageVector? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable ColumnScope.() -> Unit
) {
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = GlassMotion.FluidSpring,
        label = "collapsible_arrow_rotation"
    )

    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        accentBorderColor = if (expanded) accentColor.copy(alpha = 0.7f) else accentColor.copy(alpha = 0.35f),
        borderWidth = if (expanded) 1.3.dp else 1.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Clickable Drawer Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = if (expanded) 0.16f else 0.08f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (icon != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = accentColor.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (!subtitle.isNullOrBlank()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Clear Expand / Collapse Indicator Pill + Rotating Chevron Circle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = accentColor.copy(alpha = if (expanded) 0.22f else 0.12f),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = badgeText ?: if (expanded) "بستن" else "باز کردن",
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = accentColor.copy(alpha = if (expanded) 0.24f else 0.14f),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.55f)),
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = if (expanded) "بستن بخش" else "باز کردن بخش",
                                tint = accentColor,
                                modifier = Modifier
                                    .size(20.dp)
                                    .rotate(arrowRotation)
                            )
                        }
                    }
                }
            }

            // Expandable Body
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow)) + fadeIn(),
                exit = shrinkVertically(animationSpec = spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow)) + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                    content()
                }
            }

            // Subtle Drawer Grabber Handle at Bottom so user immediately sees it is a pull/collapsible drawer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(bottom = 6.dp, top = if (expanded) 0.dp else 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(34.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(50))
                        .background(accentColor.copy(alpha = 0.35f))
                )
            }
        }
    }
}

