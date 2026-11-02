package com.example.personallifeos.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personallifeos.ui.theme.AppTheme
import com.example.personallifeos.util.PersianUtils

/**
 * Premium Hybrid AppCard: Consistent surface card with border and elevation.
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    borderWidth: Dp = 1.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
            onClick = onClick
        )
    } else Modifier

    Surface(
        modifier = modifier
            .clip(shape)
            .border(borderWidth, borderColor, shape)
            .then(clickableModifier),
        shape = shape,
        color = backgroundColor,
        tonalElevation = 2.dp
    ) {
        content()
    }
}

/**
 * Filter Chip for tabs & categories (e.g., همه, امروز, فردا, عقب‌افتاده)
 */
@Composable
fun AppFilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    badgeCount: Int? = null
) {
    val bg = if (isSelected) activeColor else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
    val borderCol = if (isSelected) activeColor else MaterialTheme.colorScheme.outline

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .border(1.dp, borderCol, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = text,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
            if (badgeCount != null && badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color.Black.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = PersianUtils.formatPersianNumber(badgeCount.toLong()),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.Black else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * Circular Progress Ring with animated sweep angle.
 */
@Composable
fun AppCircularProgressRing(
    progressPercent: Int,
    size: Dp = 100.dp,
    strokeWidth: Dp = 8.dp,
    modifier: Modifier = Modifier,
    ringColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
    centerContent: (@Composable () -> Unit)? = null
) {
    val safePercent = progressPercent.coerceIn(0, 100)
    val sweepAngle by animateFloatAsState(
        targetValue = safePercent * 3.6f,
        animationSpec = tween(durationMillis = 800),
        label = "progressSweep"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            // Background track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke
            )
            // Progress arc
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = stroke
            )
        }

        if (centerContent != null) {
            centerContent()
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${PersianUtils.formatPersianNumber(safePercent.toLong())}٪",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Section Header with Title and optional Action (مشاهده همه)
 */
@Composable
fun AppSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (actionText != null && onActionClick != null) {
            Text(
                text = actionText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onActionClick)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

/**
 * Pill Quick Action Button (as seen on Home: + وظیفه, یادداشت, هزینه, هدف)
 */
@Composable
fun AppPillActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeColor: Color = MaterialTheme.colorScheme.primary
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = badgeColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Primary Button with cyan gradient
 */
@Composable
fun AppPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        modifier = modifier.height(48.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Secondary Outline Button
 */
@Composable
fun AppSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = Brush.horizontalGradient(
                listOf(MaterialTheme.colorScheme.outline, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            )
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        modifier = modifier.height(44.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/**
 * Reusable Status / Priority Badge
 */
@Composable
fun AppStatusBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 11.sp
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = color,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Master 5-Pillar Bottom Navigation Bar
 * 1. خانه (Home)
 * 2. وظایف (Tasks)
 * 3. تاپیک‌ها (Topics)
 * 4. مالی (Finance)
 * 5. بیشتر (More)
 */
data class NavDestinationItem(
    val id: String,
    val titleFa: String,
    val icon: ImageVector,
    val activeColor: Color
)

@Composable
fun AppBottomNavBar(
    items: List<NavDestinationItem>,
    selectedId: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = item.id == selectedId
                val tint = if (isSelected) item.activeColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)

                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = item.activeColor.copy(alpha = 0.2f)),
                            onClick = { onItemSelected(item.id) }
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("nav_tab_${item.id}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.titleFa,
                        tint = tint,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = item.titleFa,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = tint
                    )
                    if (isSelected) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .width(16.dp)
                                .height(2.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(item.activeColor)
                        )
                    }
                }
            }
        }
    }
}
