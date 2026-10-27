package com.example.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Work
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Task Categories with distinct color identities, icons, and Persian localization
 * for visual organization within GlassCard components.
 */
enum class TaskCategory(
    val code: String,
    val titleFa: String,
    val color: Color,
    val icon: ImageVector
) {
    PERSONAL("PERSONAL", "شخصی", Color(0xFF00E5FF), Icons.Default.Person),
    WORK("WORK", "کار و شغل", Color(0xFFFFB300), Icons.Default.Work),
    HEALTH("HEALTH", "سلامت و تندرستی", Color(0xFF10B981), Icons.Default.Favorite),
    FINANCE("FINANCE", "مالی و خرید", Color(0xFF34D399), Icons.Default.Payments),
    LEARNING("LEARNING", "آموزش و رشد", Color(0xFFA78BFA), Icons.Default.School),
    GENERAL("GENERAL", "عمومی", Color(0xFF818CF8), Icons.Default.Category);

    companion object {
        fun fromCode(code: String?): TaskCategory {
            if (code.isNullOrBlank()) return GENERAL
            return entries.firstOrNull {
                it.code.equals(code, ignoreCase = true) ||
                it.titleFa.equals(code, ignoreCase = true) ||
                (code.contains("کار") && it == WORK) ||
                (code.contains("شخص") && it == PERSONAL) ||
                ((code.contains("سلامت") || code.contains("ورزش") || code.contains("بهداشت") || code.contains("دکتر")) && it == HEALTH) ||
                ((code.contains("مال") || code.contains("پول") || code.contains("خرج") || code.contains("خرید") || code.contains("بانک")) && it == FINANCE) ||
                ((code.contains("آموزش") || code.contains("درس") || code.contains("کتاب") || code.contains("مطالعه")) && it == LEARNING)
            } ?: GENERAL
        }
    }
}

/**
 * Visual Colored Category Indicator Pill for GlassCard components.
 */
@androidx.compose.runtime.Composable
fun TaskCategoryIndicator(
    category: TaskCategory,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    androidx.compose.material3.Surface(
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
        color = category.color.copy(alpha = 0.16f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            GlassmorphismDefaults.specularBorderBrush(category.color, topIntensity = 0.55f)
        )
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = androidx.compose.ui.Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(5.dp)
        ) {
            // Glowing color dot indicator
            androidx.compose.foundation.layout.Box(
                modifier = androidx.compose.ui.Modifier
                    .size(7.dp)
                    .background(category.color, androidx.compose.foundation.shape.CircleShape)
            )
            androidx.compose.material3.Text(
                text = category.titleFa,
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                color = category.color,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        }
    }
}

