package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Habit
import com.example.data.model.HabitRecord
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GlassCard
import com.example.ui.theme.GlassTheme
import com.example.ui.theme.GlassmorphismDefaults
import com.example.ui.theme.MutedViolet
import com.example.util.PersianDateAndUnitUtils
import com.example.util.WorldClassOsUtils

/**
 * Interactive Habit Tracking Module integrated with GlassTheme.
 * Allows users to log daily habits with a simple 1-tap toggle interface, spring-based animations,
 * and satisfying haptic feedback upon completion.
 */
@Composable
fun HabitTrackingGlassModule(
    habits: List<Habit>,
    habitRecords: List<HabitRecord>,
    todayEpochDay: Long,
    weekStartDay: Int,
    onToggleHabitDay: (Habit, Long) -> Unit,
    onNavigateToHabits: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val activeHabits = habits.filter { !it.archived }
    val completedTodayIds = habitRecords
        .filter { it.localEpochDay == todayEpochDay && it.completedCount > 0 }
        .map { it.habitId }
        .toSet()

    val completionRatio = if (activeHabits.isEmpty()) {
        0f
    } else {
        (completedTodayIds.size.toFloat() / activeHabits.size.toFloat()).coerceIn(0f, 1f)
    }

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("habit_tracking_glass_module"),
        shape = RoundedCornerShape(24.dp),
        accentBorderColor = if (completionRatio >= 1f && activeHabits.isNotEmpty()) {
            EmeraldSuccess.copy(alpha = 0.65f)
        } else {
            MutedViolet.copy(alpha = 0.45f)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MutedViolet.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, MutedViolet.copy(alpha = 0.4f)),
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = MutedViolet,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "ثبت سریع عادت‌های روزانه",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${PersianDateAndUnitUtils.toPersianDigits(completedTodayIds.size.toString())} از ${PersianDateAndUnitUtils.toPersianDigits(activeHabits.size.toString())} عادت تکمیل شده",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "مدیریت عادت‌ها ←",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { onNavigateToHabits() }
                        .testTag("navigate_habits_from_module")
                )
            }

            if (activeHabits.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToHabits() }
                ) {
                    Text(
                        text = "✨ هنوز عادت روزانه‌ای تعریف نکرده‌اید. برای افزودن اولین عادت اینجا را لمس کنید.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            } else {
                activeHabits.take(5).forEach { habit ->
                    val completedDays = habitRecords
                        .filter { it.habitId == habit.id && it.completedCount > 0 }
                        .map { it.localEpochDay }
                        .toSet()

                    val isCompletedToday = todayEpochDay in completedDays
                    val streak = PersianDateAndUnitUtils.calculateHabitStreak(
                        habit = habit,
                        completedEpochDays = completedDays,
                        todayEpochDay = todayEpochDay,
                        weekStartDay = weekStartDay
                    )

                    val animatedScale by animateFloatAsState(
                        targetValue = if (isCompletedToday) 1.015f else 1f,
                        animationSpec = com.example.ui.theme.GlassMotion.FluidSpring,
                        label = "habitToggleScale"
                    )

                    val rowBorderColor by animateColorAsState(
                        targetValue = if (isCompletedToday) {
                            EmeraldSuccess.copy(alpha = 0.65f)
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                        },
                        label = "habitBorderColor"
                    )

                    val rowContainerColor by animateColorAsState(
                        targetValue = if (isCompletedToday) {
                            EmeraldSuccess.copy(alpha = 0.12f)
                        } else {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
                        },
                        label = "habitContainerColor"
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = rowContainerColor,
                        border = BorderStroke(1.dp, rowBorderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = animatedScale
                                scaleY = animatedScale
                            }
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                // Satisfying multi-layered haptic feedback upon habit toggle
                                if (!isCompletedToday) {
                                    WorldClassOsUtils.HapticSignature.successThud(context)
                                    WorldClassOsUtils.HapticSignature.doublePulse(context)
                                } else {
                                    WorldClassOsUtils.HapticSignature.crispTick(context)
                                }
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onToggleHabitDay(habit, todayEpochDay)
                            }
                            .testTag("dashboard_habit_toggle_${habit.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Custom Glass Toggle Pill Indicator
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isCompletedToday) EmeraldSuccess else Color.Transparent
                                        )
                                        .padding(2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isCompletedToday) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "تکمیل شده",
                                            tint = Color(0xFF06150A),
                                            modifier = Modifier.size(17.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.RadioButtonUnchecked,
                                            contentDescription = "ثبت انجام عادت",
                                            tint = MutedViolet,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Text(
                                        text = habit.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCompletedToday) EmeraldSuccess else MaterialTheme.colorScheme.onSurface,
                                        textDecoration = if (isCompletedToday) TextDecoration.LineThrough else TextDecoration.None,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (isCompletedToday) "انجام شد • لمس برای لغو" else "لمس برای ثبت انجام امروز",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Streak Pill Badge
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = AmberWarning.copy(alpha = 0.14f),
                                border = BorderStroke(
                                    1.dp,
                                    GlassmorphismDefaults.specularBorderBrush(AmberWarning, topIntensity = 0.45f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = AmberWarning,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "${PersianDateAndUnitUtils.toPersianDigits(streak.toString())} روز",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AmberWarning,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
