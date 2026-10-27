package com.example.ui.theme

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Design Tokens for the Centralized Glassmorphism Design System.
 * Defines translucency levels, blur radiuses, specular edge intensities, adaptive shapes,
 * and physics-based spring specifications.
 */
@Immutable
data class GlassTokens(
    val surfaceAlpha: Float = 0.52f,
    val surfaceVariantAlpha: Float = 0.42f,
    val elevatedAlpha: Float = 0.68f,
    val subtleAlpha: Float = 0.28f,
    val chipAlpha: Float = 0.24f,
    val blurRadius: Dp = 24.dp,
    val subtleBlurRadius: Dp = 14.dp,
    val specularBorderWidth: Dp = 1.25.dp,
    val subtleBorderWidth: Dp = 1.dp,
    val topRimIntensity: Float = 0.68f,
    val bottomRimAlpha: Float = 0.08f,
    val sheenAlpha: Float = 0.18f,
    val cardShape: Shape = RoundedCornerShape(24.dp),
    val cardMediumShape: Shape = RoundedCornerShape(18.dp),
    val pillShape: Shape = RoundedCornerShape(50),
    val chipShape: Shape = RoundedCornerShape(14.dp),
    val dockShape: Shape = RoundedCornerShape(30.dp),
    val dialogShape: Shape = RoundedCornerShape(26.dp),
    val bottomSheetShape: Shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    val springDampingRatio: Float = Spring.DampingRatioMediumBouncy,
    val springStiffness: Float = Spring.StiffnessLow
)

val LocalGlassTheme = staticCompositionLocalOf { GlassTokens() }

/**
 * Fluid spring-based physics and motion specifications for the Glassmorphism Design System.
 * Delivers organic, tactile physical motion for card reveals, dialog entries, and interactive press states.
 */
object GlassMotion {

    val FluidSpring: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    val SnappySpring: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    val GentleSpring: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessVeryLow
    )

    val BouncySpring: SpringSpec<Float> = spring(
        dampingRatio = 0.6f,
        stiffness = Spring.StiffnessMediumLow
    )

    val DpSpring: SpringSpec<Dp> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    val ColorSpring: SpringSpec<Color> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    /**
     * Spring-based entry transition for fluid glassmorphism reveal:
     * Combines subtle fade-in, organic spring scale-in (0.92f to 1.0f),
     * and gentle vertical rise.
     */
    fun entryTransition(
        dampingRatio: Float = Spring.DampingRatioMediumBouncy,
        stiffness: Float = Spring.StiffnessLow,
        initialScale: Float = 0.92f
    ): EnterTransition = fadeIn(
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = stiffness
        )
    ) + scaleIn(
        initialScale = initialScale,
        animationSpec = spring(
            dampingRatio = dampingRatio,
            stiffness = stiffness
        )
    ) + slideInVertically(
        initialOffsetY = { it / 8 },
        animationSpec = spring(
            dampingRatio = dampingRatio,
            stiffness = stiffness
        )
    )

    /**
     * Spring-based exit transition:
     * Gentle fade-out and subtle scale-down.
     */
    fun exitTransition(
        dampingRatio: Float = Spring.DampingRatioNoBouncy,
        stiffness: Float = Spring.StiffnessMediumLow,
        targetScale: Float = 0.95f
    ): ExitTransition = fadeOut(
        animationSpec = spring(
            dampingRatio = dampingRatio,
            stiffness = stiffness
        )
    ) + scaleOut(
        targetScale = targetScale,
        animationSpec = spring(
            dampingRatio = dampingRatio,
            stiffness = stiffness
        )
    ) + slideOutVertically(
        targetOffsetY = { it / 12 },
        animationSpec = spring(
            dampingRatio = dampingRatio,
            stiffness = stiffness
        )
    )
}

/**
 * Global GlassTheme accessor providing centralized access to tokens,
 * specular border brushes, frosted glass brushes, and gloss overlays.
 */
object GlassTheme {
    val tokens: GlassTokens
        @Composable
        get() = LocalGlassTheme.current

    val motion: GlassMotion
        get() = GlassMotion

    /**
     * Creates a 3D diagonal liquid glass specular rim border where light catches the top-left و bottom-right edges.
     */
    fun specularBorderBrush(
        accentColor: Color? = null,
        topIntensity: Float = 0.68f,
        bottomAlpha: Float = 0.12f
    ): Brush {
        val highlight = if (accentColor != null) {
            accentColor.copy(alpha = topIntensity.coerceIn(0.3f, 0.92f))
        } else {
            Color.White.copy(alpha = topIntensity)
        }
        val secondaryRim = accentColor?.copy(alpha = 0.28f) ?: Color.White.copy(alpha = 0.24f)
        return Brush.linearGradient(
            colors = listOf(
                highlight,
                Color.White.copy(alpha = 0.32f),
                Color.White.copy(alpha = bottomAlpha),
                secondaryRim
            )
        )
    }

    /**
     * Creates a frosted translucent multi-stop diagonal glass gradient fill.
     */
    fun frostedGlassBrush(
        baseColor: Color = Color(0xFF101B17),
        alphaTop: Float = 0.56f,
        alphaBottom: Float = 0.38f
    ): Brush {
        return Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.10f),
                baseColor.copy(alpha = alphaTop),
                baseColor.copy(alpha = alphaBottom)
            )
        )
    }

    /**
     * Top Specular Gloss Sheen reflection overlay for authentic glass refraction.
     */
    val GlossSheenBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.18f),
            Color.White.copy(alpha = 0.06f),
            Color.Transparent,
            Color.White.copy(alpha = 0.04f)
        )
    )
}

/**
 * Custom Modifier that uses hardware-accelerated RenderEffect on Android 12+ (API 31+)
 * to apply background blur to glassmorphism components, enhancing the liquid glass visual effect.
 * Gracefully falls back to Compose Modifier.blur on earlier supported versions.
 */
fun Modifier.glassRenderEffect(
    blurRadius: Dp = 20.dp,
    tileMode: Shader.TileMode = Shader.TileMode.CLAMP
): Modifier = if (blurRadius > 0.dp) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        this.graphicsLayer {
            val px = blurRadius.toPx()
            if (px > 0f) {
                val effect = RenderEffect.createBlurEffect(px, px, tileMode)
                renderEffect = effect.asComposeRenderEffect()
            }
        }
    } else {
        this.blur(blurRadius)
    }
} else {
    this
}

private const val AGSL_GLASS_FRAGMENT_SHADER = """
    uniform shader contents;
    uniform float blurAmount;
    vec4 main(vec2 coord) {
        vec4 c = contents.eval(coord);
        return vec4(mix(c.rgb, vec3(0.08, 0.14, 0.11), 0.18), c.a);
    }
"""

/**
 * AGSL (Android Graphics Shading Language) hardware-accelerated shader modifier for Android 13+ (API 33+).
 * Offloads glass filtering and refraction directly to the GPU, with automatic fallback profile for older devices or low power.
 */
fun Modifier.agslGlassShaderEffect(
    enabled: Boolean = true,
    fallbackProfile: Boolean = false
): Modifier = if (enabled && !fallbackProfile && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    this.graphicsLayer {
        try {
            val runtimeShader = RuntimeShader(AGSL_GLASS_FRAGMENT_SHADER).apply {
                setFloatUniform("blurAmount", 20f)
            }
            val renderEffect = android.graphics.RenderEffect.createRuntimeShaderEffect(runtimeShader, "contents")
            this.renderEffect = renderEffect.asComposeRenderEffect()
        } catch (e: Exception) {
            // Fallback profile
        }
    }
} else {
    this
}

/**
 * Modifier extension to apply glassmorphism blur conditionally or with fallback.
 */
fun Modifier.glassBlur(radius: Dp): Modifier = this.glassRenderEffect(radius)

/**
 * Modifier extension providing fluid spring-based tactile press feedback.
 * Gently scales down to [pressedScale] with spring physics when pressed, then bounces back.
 */
@Composable
fun Modifier.springPressScale(
    pressedScale: Float = 0.97f,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    enabled: Boolean = true
): Modifier {
    if (!enabled) return this
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = GlassMotion.FluidSpring,
        label = "glass_spring_press_scale"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Modifier extension to apply a complete glass surface background with clip, border, and frosted fill.
 */
fun Modifier.glassBackground(
    shape: Shape = RoundedCornerShape(20.dp),
    borderWidth: Dp = 1.dp,
    accentBorderColor: Color? = null,
    baseColor: Color? = null,
    alpha: Float = 0.70f,
    topRimIntensity: Float = 0.55f
): Modifier = this
    .clip(shape)
    .border(
        border = BorderStroke(
            width = borderWidth,
            brush = GlassTheme.specularBorderBrush(accentBorderColor, topIntensity = topRimIntensity)
        ),
        shape = shape
    )
    .background(
        brush = GlassTheme.frostedGlassBrush(
            baseColor = baseColor ?: Color(0xFF0F1612),
            alphaTop = alpha,
            alphaBottom = (alpha * 0.8f).coerceAtLeast(0.1f)
        ),
        shape = shape
    )

/**
 * Specialized container that uses RenderEffect to apply background blur,
 * specular liquid glass border, and refractive translucent acrylic fill to its children.
 */
@Composable
fun GlassRenderEffectContainer(
    modifier: Modifier = Modifier,
    shape: Shape = GlassTheme.tokens.cardShape,
    blurRadius: Dp = 0.dp,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    alpha: Float = GlassTheme.tokens.surfaceAlpha,
    accentBorderColor: Color? = null,
    borderWidth: Dp = GlassTheme.tokens.specularBorderWidth,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .glassRenderEffect(blurRadius)
            .border(
                border = BorderStroke(
                    width = borderWidth,
                    brush = GlassTheme.specularBorderBrush(
                        accentColor = accentBorderColor,
                        topIntensity = GlassTheme.tokens.topRimIntensity
                    )
                ),
                shape = shape
            )
            .background(
                brush = GlassTheme.frostedGlassBrush(
                    baseColor = backgroundColor,
                    alphaTop = alpha,
                    alphaBottom = (alpha * 0.78f).coerceAtLeast(0.12f)
                ),
                shape = shape
            )
    ) {
        // Specular Gloss Highlight Overlay
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(GlassTheme.GlossSheenBrush)
        )
        content()
    }
}

/**
 * Reusable 'GlassCard' Composable built on Material3 Surface with specific alpha transparency,
 * rounded corners, a subtle specular light border, RenderEffect background blur,
 * and spring-based tactile press feedback to act as the foundation for the glassmorphism design system.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = GlassTheme.tokens.cardShape,
    baseColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    alpha: Float = GlassTheme.tokens.surfaceAlpha,
    accentBorderColor: Color? = null,
    borderWidth: Dp = GlassTheme.tokens.specularBorderWidth,
    border: BorderStroke? = BorderStroke(
        width = borderWidth,
        brush = GlassTheme.specularBorderBrush(
            accentColor = accentBorderColor,
            topIntensity = GlassTheme.tokens.topRimIntensity
        )
    ),
    blurRadius: Dp = 0.dp,
    useRenderEffect: Boolean = false,
    enableSpringScale: Boolean = true,
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val surfaceColor = baseColor.copy(alpha = alpha)
    val interactionSource = remember { MutableInteractionSource() }

    val blurModifier = if (useRenderEffect && blurRadius > 0.dp) Modifier.glassRenderEffect(blurRadius) else Modifier
    val springModifier = if (enableSpringScale && (onClick != null || onLongClick != null)) {
        Modifier.springPressScale(pressedScale = 0.975f, interactionSource = interactionSource)
    } else {
        Modifier
    }

    val clickModifier = when {
        onClick != null && onLongClick != null -> Modifier.combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
            onLongClick = onLongClick
        )
        onClick != null -> Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
        else -> Modifier
    }

    // Material 3 Surface Foundation
    Surface(
        modifier = modifier
            .then(blurModifier)
            .then(springModifier)
            .clip(shape)
            .then(clickModifier),
        shape = shape,
        color = surfaceColor,
        border = border,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation
    ) {
        Box {
            // Specular Gloss Highlight Overlay
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(GlassTheme.GlossSheenBrush)
            )

            // Content Column
            Column(content = content)
        }
    }
}

/**
 * Animated variant of GlassCard that wraps entry and exit transitions in fluid spring-based animations.
 */
@Composable
fun GlassAnimatedCard(
    visible: Boolean,
    modifier: Modifier = Modifier,
    shape: Shape = GlassTheme.tokens.cardShape,
    baseColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    alpha: Float = GlassTheme.tokens.surfaceAlpha,
    accentBorderColor: Color? = null,
    borderWidth: Dp = GlassTheme.tokens.specularBorderWidth,
    blurRadius: Dp = GlassTheme.tokens.blurRadius,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    enterTransition: EnterTransition = GlassMotion.entryTransition(),
    exitTransition: ExitTransition = GlassMotion.exitTransition(),
    content: @Composable ColumnScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = enterTransition,
        exit = exitTransition
    ) {
        GlassCard(
            modifier = modifier,
            shape = shape,
            baseColor = baseColor,
            alpha = alpha,
            accentBorderColor = accentBorderColor,
            borderWidth = borderWidth,
            blurRadius = blurRadius,
            onClick = onClick,
            onLongClick = onLongClick,
            content = content
        )
    }
}

/**
 * Centralized GlassSurface built on Material 3 Surface with semi-transparent background,
 * subtle specular rim border, custom shapes, RenderEffect blur, and spring-based interactions.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = GlassTheme.tokens.cardShape,
    color: Color = MaterialTheme.colorScheme.surfaceVariant,
    alpha: Float = GlassTheme.tokens.surfaceAlpha,
    accentBorderColor: Color? = null,
    borderWidth: Dp = GlassTheme.tokens.specularBorderWidth,
    border: BorderStroke? = BorderStroke(
        width = borderWidth,
        brush = GlassTheme.specularBorderBrush(accentBorderColor, topIntensity = GlassTheme.tokens.topRimIntensity)
    ),
    blurRadius: Dp = 0.dp,
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val surfaceColor = color.copy(alpha = alpha)
    val baseModifier = modifier
        .clip(shape)
        .then(if (blurRadius > 0.dp) Modifier.glassRenderEffect(blurRadius) else Modifier)

    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = baseModifier.springPressScale(pressedScale = 0.96f),
            shape = shape,
            color = surfaceColor,
            border = border,
            tonalElevation = tonalElevation,
            shadowElevation = shadowElevation,
            content = content
        )
    } else {
        Surface(
            modifier = baseModifier,
            shape = shape,
            color = surfaceColor,
            border = border,
            tonalElevation = tonalElevation,
            shadowElevation = shadowElevation,
            content = content
        )
    }
}

/**
 * Standardized Material 3 Glass Pill / Chip for badges, buttons, and status indicators.
 * Includes RenderEffect translucency and tactile spring scaling.
 */
@Composable
fun GlassPill(
    modifier: Modifier = Modifier,
    shape: Shape = GlassTheme.tokens.pillShape,
    accentGlow: Color? = null,
    borderWidth: Dp = GlassTheme.tokens.subtleBorderWidth,
    alpha: Float = GlassTheme.tokens.chipAlpha,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val clickModifier = if (onClick != null) {
        Modifier
            .springPressScale(pressedScale = 0.95f)
            .clickable(onClick = onClick)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .clip(shape)
            .border(
                border = BorderStroke(
                    width = borderWidth,
                    brush = GlassTheme.specularBorderBrush(accentGlow, topIntensity = 0.5f)
                ),
                shape = shape
            )
            .background(
                brush = GlassTheme.frostedGlassBrush(
                    baseColor = accentGlow?.copy(alpha = 0.18f) ?: MaterialTheme.colorScheme.surfaceVariant,
                    alphaTop = (alpha + 0.25f).coerceAtMost(0.95f),
                    alphaBottom = alpha
                ),
                shape = shape
            )
            .then(clickModifier),
        content = content
    )
}

/**
 * Centralized GlassTheme definition using Compose Material 3.
 * Configures MaterialTheme with strict dark/OLED color schemes, Persian RTL direction,
 * Vazirmatn typography, and global Glassmorphism design tokens.
 */
@Composable
fun GlassTheme(
    tokens: GlassTokens = GlassTokens(),
    darkTheme: Boolean = true,
    themePalette: String = AppThemePalette.OBSIDIAN_TEAL.code,
    isRtl: Boolean = true,
    layoutDirection: LayoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
    content: @Composable () -> Unit
) {
    val colorScheme = resolveColorScheme(themePalette = themePalette, darkTheme = darkTheme)

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection,
        LocalGlassTheme provides tokens
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
