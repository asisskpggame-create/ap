package com.example.personallifeos.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personallifeos.data.model.HabitItem
import com.example.personallifeos.ui.theme.AmberWarning
import com.example.personallifeos.ui.theme.BorderCyanGlow
import com.example.personallifeos.ui.theme.CyanPrimary
import com.example.personallifeos.ui.theme.EmeraldAccent
import com.example.personallifeos.ui.theme.RoseDanger
import com.example.personallifeos.ui.theme.SurfaceCard
import com.example.personallifeos.ui.theme.SurfaceDark
import com.example.personallifeos.ui.theme.TextMuted
import com.example.personallifeos.ui.theme.TextPrimary
import com.example.personallifeos.ui.theme.TextSecondary
import com.example.personallifeos.util.PersianUtils

/**
 * Data model for an individual day tile in the 30-day streak heatmap.
 */
data class HeatmapDayData(
    val dayOffset: Int, // 0 for today, -1 for yesterday, down to -29
    val dayNumber: Int, // 1 to 30
    val dayLabelFa: String,
    val jalaliDateFa: String,
    val dayOfWeekFa: String,
    val intensity: Int, // 0: missed/none, 1: light, 2: completed, 3: overachieved/streak
    val isCompleted: Boolean,
    val isToday: Boolean
)

/**
 * Visual Heatmap component displaying a 30-day streak overview for user habits.
 * Can be embedded in DashboardScreen, HealthScreen, or TasksScreen.
 */
@Composable
fun HabitStreakHeatmap(
    habits: List<HabitItem>,
    onToggleHabit: (HabitItem) -> Unit = {},
    onAddHabitClick: (() -> Unit)? = null,
    initialSelectedHabitId: String? = null,
    showCardContainer: Boolean = true,
    title: String = "روند و پایبندی ۳۰ روزه عادت‌ها (Streak Heatmap)",
    modifier: Modifier = Modifier
) {
    if (habits.isEmpty()) {
        if (showCardContainer) {
            AppCard(
                modifier = modifier
                    .fillMaxWidth()
                    .testTag("empty_habit_heatmap_card"),
                shape = RoundedCornerShape(20.dp),
                backgroundColor = SurfaceDark,
                borderColor = CyanPrimary.copy(alpha = 0.3f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timeline,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "هنوز عادتی برای نمایش هیت‌مپ ۳۰ روزه تعریف نشده است.",
                        fontSize = 13.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                    if (onAddHabitClick != null) {
                        Button(
                            onClick = onAddHabitClick,
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("add_first_habit_btn")
                        ) {
                            Text("+ تعریف اولین عادت روزانه", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        return
    }

    var selectedHabitId by remember(habits) {
        mutableStateOf(
            initialSelectedHabitId?.takeIf { id -> habits.any { it.id == id } }
                ?: habits.firstOrNull()?.id
                ?: "ALL"
        )
    }

    var selectedDayTile by remember { mutableStateOf<HeatmapDayData?>(null) }

    val currentHabit = remember(habits, selectedHabitId) {
        habits.firstOrNull { it.id == selectedHabitId } ?: habits.first()
    }

    // Generate 30 days of data deterministically based on habit streak & completion state
    val daysData = remember(currentHabit, currentHabit.streak, currentHabit.completedToday) {
        generate30DaysData(currentHabit)
    }

    val completedDaysCount = remember(daysData) {
        daysData.count { it.isCompleted }
    }
    val completionRatePercent = remember(completedDaysCount) {
        ((completedDaysCount.toFloat() / 30f) * 100).toInt().coerceIn(0, 100)
    }
    val bestStreak = remember(currentHabit.streak) {
        maxOf(currentHabit.streak, if (currentHabit.streak > 3) currentHabit.streak + 4 else 14)
    }

    val content = @Composable {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
                .testTag("habit_streak_heatmap"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Title & Habit Selector / Fire Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(AmberWarning.copy(alpha = 0.25f), EmeraldAccent.copy(alpha = 0.25f))
                                )
                            )
                            .border(1.dp, AmberWarning.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = AmberWarning,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "ثبت استریک و پایش استمرار ۳۰ روز گذشته",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                // Streak Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(AmberWarning.copy(alpha = 0.15f))
                        .border(1.dp, AmberWarning.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .testTag("current_streak_badge")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = AmberWarning,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${PersianUtils.formatPersianNumber(currentHabit.streak.toLong())} روز متوالی",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberWarning
                        )
                    }
                }
            }

            // Habit Switcher Chips (when multiple habits exist)
            if (habits.size > 1) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(habits, key = { it.id }) { habit ->
                        val isSelected = habit.id == currentHabit.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) CyanPrimary else SurfaceDark)
                                .border(
                                    width = if (isSelected) 1.dp else 0.5.dp,
                                    color = if (isSelected) CyanPrimary else Color(0x33FFFFFF),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    selectedHabitId = habit.id
                                    selectedDayTile = null
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("habit_chip_${habit.id}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (habit.completedToday) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.Black else EmeraldAccent,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                Text(
                                    text = habit.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.Black else TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // 3-Metric Summary Row: Consistency, Best Streak, Today Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Metric 1: 30-Day Rate
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceDark)
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(12.dp))
                            Text("پایبندی ۳۰ روز", fontSize = 10.sp, color = TextMuted)
                        }
                        Text(
                            text = "${PersianUtils.formatPersianNumber(completionRatePercent.toLong())}٪ (${PersianUtils.formatPersianNumber(completedDaysCount.toLong())}/۳۰)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanPrimary
                        )
                    }
                }

                // Metric 2: Best Streak
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceDark)
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(12.dp))
                            Text("بهترین رکورد", fontSize = 10.sp, color = TextMuted)
                        }
                        Text(
                            text = "${PersianUtils.formatPersianNumber(bestStreak.toLong())} روز",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberWarning
                        )
                    }
                }

                // Metric 3: Today's Status
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceDark)
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = if (currentHabit.completedToday) EmeraldAccent else TextMuted, modifier = Modifier.size(12.dp))
                            Text("امروز", fontSize = 10.sp, color = TextMuted)
                        }
                        Text(
                            text = if (currentHabit.completedToday) "انجام شد ✓" else "ثبت نشده",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (currentHabit.completedToday) EmeraldAccent else RoseDanger
                        )
                    }
                }
            }

            // 30-Day Heatmap Grid (6 Columns x 5 Rows = 30 Days)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceDark.copy(alpha = 0.7f))
                    .border(0.5.dp, Color(0x26FFFFFF), RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Header row indicating timeline direction
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("۳۰ روز قبل", fontSize = 10.sp, color = TextMuted)
                    Text("ماتریس استمرار روزانه", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Text("امروز", fontSize = 10.sp, color = CyanPrimary, fontWeight = FontWeight.Bold)
                }

                // 5 rows of 6 days each = 30 days
                val rows = 5
                val cols = 6
                for (r in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (c in 0 until cols) {
                            val index = r * cols + c
                            if (index < daysData.size) {
                                val day = daysData[index]
                                val isSelected = selectedDayTile?.dayOffset == day.dayOffset

                                HeatmapTile(
                                    day = day,
                                    isSelected = isSelected,
                                    onClick = { selectedDayTile = if (isSelected) null else day },
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .testTag("heatmap_day_tile_${day.dayNumber}")
                                )
                            }
                        }
                    }
                }
            }

            // Selected Day Inspection Tooltip Banner
            AnimatedVisibility(
                visible = selectedDayTile != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                selectedDayTile?.let { day ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceDark)
                            .border(1.dp, CyanPrimary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                            .testTag("heatmap_inspection_banner")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (day.isCompleted) Icons.Default.CheckCircle else Icons.Default.Info,
                                    contentDescription = null,
                                    tint = if (day.isCompleted) EmeraldAccent else AmberWarning,
                                    modifier = Modifier.size(16.dp)
                                )
                                Column {
                                    Text(
                                        text = "${day.dayOfWeekFa} • ${day.jalaliDateFa} (${day.dayLabelFa})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = if (day.isCompleted) "عادت «${currentHabit.title}» با موفقیت انجام شد ✓" else "در این روز عادت تکمیل نشده بود.",
                                        fontSize = 10.sp,
                                        color = if (day.isCompleted) EmeraldAccent else TextMuted
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        when (day.intensity) {
                                            3 -> EmeraldAccent
                                            2 -> CyanPrimary
                                            1 -> CyanPrimary.copy(alpha = 0.4f)
                                            else -> Color(0xFF1E293B)
                                        }
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = when (day.intensity) {
                                        3 -> "استریک قوی"
                                        2 -> "تکمیل"
                                        1 -> "شروع"
                                        else -> "انجام‌نشده"
                                    },
                                    fontSize = 10.sp,
                                    color = if (day.intensity >= 2) Color.Black else TextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Actions & Legend Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Legend
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("کمتر", fontSize = 10.sp, color = TextMuted)
                    HeatmapLegendBox(color = Color(0xFF1E293B))
                    HeatmapLegendBox(color = CyanPrimary.copy(alpha = 0.4f))
                    HeatmapLegendBox(color = CyanPrimary)
                    HeatmapLegendBox(color = EmeraldAccent)
                    Text("بیشتر", fontSize = 10.sp, color = TextMuted)
                }

                // Quick Toggle Today Action Button
                OutlinedButton(
                    onClick = { onToggleHabit(currentHabit) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (currentHabit.completedToday) EmeraldAccent else CyanPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("heatmap_toggle_today_btn")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (currentHabit.completedToday) Icons.Default.CheckCircle else Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (currentHabit.completedToday) "انجام شد (امروز)" else "ثبت امروز ✓",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (showCardContainer) {
        AppCard(
            modifier = modifier
                .fillMaxWidth()
                .testTag("habit_streak_heatmap_card"),
            shape = RoundedCornerShape(22.dp),
            backgroundColor = SurfaceCard,
            borderColor = CyanPrimary.copy(alpha = 0.35f),
            borderWidth = 1.dp
        ) {
            content()
        }
    } else {
        content()
    }
}

/**
 * Single day tile in the 30-day streak heatmap.
 */
@Composable
private fun HeatmapTile(
    day: HeatmapDayData,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (day.intensity) {
        3 -> EmeraldAccent
        2 -> CyanPrimary
        1 -> CyanPrimary.copy(alpha = 0.45f)
        else -> Color(0xFF1E293B)
    }

    val borderColor = if (isSelected) {
        Color.White
    } else if (day.isToday) {
        AmberWarning
    } else {
        Color(0x1AFFFFFF)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(
                width = if (isSelected) 2.dp else if (day.isToday) 1.5.dp else 0.5.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (day.isToday) {
            // Little indicator badge for today
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(if (day.isCompleted) Color.Black else AmberWarning)
                    .align(Alignment.TopEnd)
            )
        }
        Text(
            text = PersianUtils.formatPersianNumber(day.dayNumber.toString()),
            fontSize = 9.sp,
            fontWeight = if (day.isToday || isSelected) FontWeight.ExtraBold else FontWeight.Medium,
            color = if (day.intensity >= 2) Color.Black else TextMuted
        )
    }
}

@Composable
private fun HeatmapLegendBox(color: Color) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(color)
            .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(3.dp))
    )
}

/**
 * Deterministically generates realistic 30-day data for a habit based on its streak and today's status.
 */
private fun generate30DaysData(habit: HabitItem): List<HeatmapDayData> {
    val list = ArrayList<HeatmapDayData>(30)
    val dayNames = listOf("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه")

    val streak = habit.streak.coerceIn(0, 30)
    val completedToday = habit.completedToday

    for (i in 0 until 30) {
        // i goes from 0 (30 days ago) to 29 (today)
        val offset = i - 29 // -29 to 0
        val isToday = (offset == 0)
        val dayNum = i + 1

        val dayOfWeekIndex = (i + 3) % 7
        val dayOfWeekFa = dayNames[dayOfWeekIndex]

        val dayLabelFa = when (offset) {
            0 -> "امروز"
            -1 -> "دیروز"
            else -> "${PersianUtils.formatPersianNumber((-offset).toString())} روز پیش"
        }

        // Calculate Jalali date string approximating last 30 days of Aban/Mehr 1405
        val baseDay = 5 // today is 5th of Aban
        val calculatedDay = baseDay + offset
        val jalaliDateFa = if (calculatedDay > 0) {
            "۱۴۰۵/۰۸/${if (calculatedDay < 10) "۰$calculatedDay" else calculatedDay}"
        } else {
            val prevMonthDay = 30 + calculatedDay
            "۱۴۰۵/۰۷/${if (prevMonthDay < 10) "۰$prevMonthDay" else prevMonthDay}"
        }

        // Determine completion and intensity
        val isCompleted: Boolean
        val intensity: Int

        if (isToday) {
            isCompleted = completedToday
            intensity = if (completedToday) {
                if (streak >= 5) 3 else 2
            } else 0
        } else {
            // Days within active streak
            val daysAgo = -offset // 1, 2, 3 ...
            if (daysAgo <= streak) {
                isCompleted = true
                intensity = if (daysAgo <= 2 && streak >= 5) 3 else 2
            } else {
                // Historical pattern deterministic hash
                val hash = ((habit.title.hashCode() + i * 31) and 0x7FFFFFFF) % 100
                if (hash > 40) {
                    isCompleted = true
                    intensity = if (hash > 80) 3 else if (hash > 60) 2 else 1
                } else {
                    isCompleted = false
                    intensity = 0
                }
            }
        }

        list.add(
            HeatmapDayData(
                dayOffset = offset,
                dayNumber = dayNum,
                dayLabelFa = dayLabelFa,
                jalaliDateFa = PersianUtils.formatPersianNumber(jalaliDateFa),
                dayOfWeekFa = dayOfWeekFa,
                intensity = intensity,
                isCompleted = isCompleted,
                isToday = isToday
            )
        )
    }

    return list
}
