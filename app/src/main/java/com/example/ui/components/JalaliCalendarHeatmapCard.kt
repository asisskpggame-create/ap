package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConsumptionEntry
import com.example.data.model.ExpenseEntry
import com.example.data.model.HabitRecord
import com.example.data.model.Task
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ElectricTeal
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LiquidGlassPill
import com.example.ui.theme.MutedViolet
import com.example.ui.theme.RoseError
import com.example.ui.theme.SkyInfo
import com.example.util.PersianDateAndUnitUtils
import com.example.util.WorldClassOsUtils

/**
 * Interactive Solar Hijri (Jalali) Monthly Calendar Heatmap Card.
 * Displays all days of the selected Jalali month in a 7-column grid (Saturday to Friday),
 * renders color-coded activity dots (Tasks, Expenses, Habits) for every day,
 * and shows a rich interactive day-detail panel when any day is tapped.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JalaliCalendarHeatmapCard(
    todayEpochDay: Long,
    defaultCurrency: String,
    tasks: List<Task>,
    expenses: List<ExpenseEntry>,
    consumption: List<ConsumptionEntry>,
    habitRecords: List<HabitRecord>,
    onQuickCreateTaskForDay: (title: String, dueEpochDay: Long) -> Unit,
    onCompleteTask: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val todayJalali = remember(todayEpochDay) {
        PersianDateAndUnitUtils.epochDayToJalali(todayEpochDay)
    }

    // Month offset relative to current Jalali month (0 = current month, -1 = previous month, +1 = next month)
    var monthOffset by rememberSaveable { mutableIntStateOf(0) }
    var selectedEpochDay by rememberSaveable { mutableLongStateOf(todayEpochDay) }
    var isExpanded by rememberSaveable { mutableStateOf(true) }
    var quickTaskTitle by remember { mutableStateOf("") }

    // Calculate target Jalali (year, month) and the epochDay of the 1st of that Jalali month
    val monthMeta = remember(todayEpochDay, monthOffset) {
        calculateJalaliMonthGridMeta(todayEpochDay, todayJalali.year, todayJalali.month, monthOffset)
    }

    val weekdayHeaders = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")

    LiquidGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("jalali_calendar_heatmap_card"),
        shape = RoundedCornerShape(24.dp),
        accentBorderColor = ElectricTeal
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Header: Title + Month Navigation + Collapse Toggle
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
                        shape = CircleShape,
                        color = ElectricTeal.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, ElectricTeal.copy(alpha = 0.45f)),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "تقویم شمسی",
                                tint = ElectricTeal,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "تقویم شمسی و نقشه فعالیت",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${monthMeta.monthNameFa} ${PersianDateAndUnitUtils.toPersianDigits(monthMeta.year.toString())}",
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricTeal,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = {
                            WorldClassOsUtils.HapticSignature.crispTick(context)
                            monthOffset -= 1
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("calendar_prev_month_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "ماه قبل",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (monthOffset != 0) {
                        LiquidGlassPill(
                            accentGlow = ElectricTeal,
                            onClick = {
                                WorldClassOsUtils.HapticSignature.crispTick(context)
                                monthOffset = 0
                                selectedEpochDay = todayEpochDay
                            }
                        ) {
                            Text(
                                text = "امروز",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricTeal,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            WorldClassOsUtils.HapticSignature.crispTick(context)
                            monthOffset += 1
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("calendar_next_month_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "ماه بعد",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = {
                            WorldClassOsUtils.HapticSignature.crispTick(context)
                            isExpanded = !isExpanded
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "بستن تقویم" else "باز کردن تقویم",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Legend Row
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        CalendarLegendItem(color = EmeraldSuccess, label = "تسک")
                        CalendarLegendItem(color = AmberWarning, label = "هزینه مالی")
                        CalendarLegendItem(color = MutedViolet, label = "عادت تکمیل‌شده")
                    }

                    // 7-Column Weekday Header (شنبه تا جمعه)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        weekdayHeaders.forEachIndexed { index, title ->
                            Text(
                                text = title,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (index == 6) RoseError else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Calendar Grid Rows
                    val totalCells = monthMeta.leadingEmptyCells + monthMeta.daysInMonth
                    val rowCount = (totalCells + 6) / 7

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (row in 0 until rowCount) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for (col in 0 until 7) {
                                    val cellIndex = row * 7 + col
                                    val dayNumber = cellIndex - monthMeta.leadingEmptyCells + 1
                                    if (dayNumber in 1..monthMeta.daysInMonth) {
                                        val cellEpochDay = monthMeta.firstDayEpochDay + (dayNumber - 1)
                                        val isToday = cellEpochDay == todayEpochDay
                                        val isSelected = cellEpochDay == selectedEpochDay
                                        val dayTasks = tasks.filter { it.status != "ARCHIVED" && it.dueDateEpochDay == cellEpochDay }
                                        val hasExpenses = expenses.any { !it.isIncome && it.localEpochDay == cellEpochDay }
                                        val hasHabits = habitRecords.any { it.localEpochDay == cellEpochDay }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    when {
                                                        isSelected -> ElectricTeal.copy(alpha = 0.26f)
                                                        isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                                        else -> Color.White.copy(alpha = 0.04f)
                                                    }
                                                )
                                                .border(
                                                    width = if (isSelected || isToday) 1.4.dp else 0.6.dp,
                                                    color = when {
                                                        isSelected -> ElectricTeal
                                                        isToday -> MaterialTheme.colorScheme.primary
                                                        else -> Color.White.copy(alpha = 0.08f)
                                                    },
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .clickable {
                                                    WorldClassOsUtils.HapticSignature.crispTick(context)
                                                    selectedEpochDay = cellEpochDay
                                                }
                                                .testTag("jalali_day_cell_$dayNumber"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = PersianDateAndUnitUtils.toPersianDigits(dayNumber.toString()),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = if (isSelected || isToday) FontWeight.Black else FontWeight.Medium,
                                                    color = when {
                                                        isSelected -> ElectricTeal
                                                        col == 6 -> RoseError.copy(alpha = 0.9f)
                                                        else -> MaterialTheme.colorScheme.onSurface
                                                    }
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                // Heatmap Dots Row
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    if (dayTasks.isNotEmpty()) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(5.dp)
                                                                .background(
                                                                    if (dayTasks.all { it.status == "COMPLETED" }) EmeraldSuccess else ElectricTeal,
                                                                    CircleShape
                                                                )
                                                        )
                                                    }
                                                    if (hasExpenses) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(5.dp)
                                                                .background(AmberWarning, CircleShape)
                                                        )
                                                    }
                                                    if (hasHabits) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(5.dp)
                                                                .background(MutedViolet, CircleShape)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        Spacer(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                    // Selected Day Summary Detail Panel
                    val selectedTasks = tasks.filter { it.status != "ARCHIVED" && it.dueDateEpochDay == selectedEpochDay }
                    val selectedExpensesSum = expenses
                        .filter { !it.isIncome && it.localEpochDay == selectedEpochDay && it.currency.equals(defaultCurrency, ignoreCase = true) }
                        .sumOf { it.amount }
                    val selectedWaterMl = PersianDateAndUnitUtils.sumCompatibleConsumption(
                        consumption.filter { it.localEpochDay == selectedEpochDay && it.category == "WATER" },
                        "ml"
                    ).toInt()
                    val selectedHabitsCount = habitRecords.count { it.localEpochDay == selectedEpochDay }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = PersianDateAndUnitUtils.formatEpochDay(selectedEpochDay, "JALALI", true),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = ElectricTeal
                            )
                            Text(
                                text = PersianDateAndUnitUtils.formatEpochDay(selectedEpochDay, "GREGORIAN", false),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Quick Metrics for Selected Day
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DayMetricBadge(
                                icon = Icons.Default.CheckCircle,
                                text = "${PersianDateAndUnitUtils.toPersianDigits(selectedTasks.size.toString())} تسک",
                                color = EmeraldSuccess
                            )
                            DayMetricBadge(
                                icon = Icons.Default.Payments,
                                text = PersianDateAndUnitUtils.formatCurrencyAmount(selectedExpensesSum, defaultCurrency),
                                color = AmberWarning
                            )
                            DayMetricBadge(
                                icon = Icons.Default.LocalDrink,
                                text = "${PersianDateAndUnitUtils.toPersianDigits(selectedWaterMl.toString())} ml آب",
                                color = SkyInfo
                            )
                            if (selectedHabitsCount > 0) {
                                DayMetricBadge(
                                    icon = Icons.Default.CheckCircle,
                                    text = "${PersianDateAndUnitUtils.toPersianDigits(selectedHabitsCount.toString())} عادت",
                                    color = MutedViolet
                                )
                            }
                        }

                        // Show tasks for the selected day
                        if (selectedTasks.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                selectedTasks.take(4).forEach { t ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color.White.copy(alpha = 0.04f))
                                            .clickable {
                                                if (t.status != "COMPLETED") {
                                                    WorldClassOsUtils.HapticSignature.crispTick(context)
                                                    onCompleteTask(t)
                                                }
                                            }
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = t.title,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (t.status == "COMPLETED") MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = if (t.status == "COMPLETED") "انجام شد ✓" else "تکمیل",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (t.status == "COMPLETED") EmeraldSuccess else ElectricTeal,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Quick Add Task for Selected Date
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = quickTaskTitle,
                                onValueChange = { quickTaskTitle = it },
                                placeholder = { Text("افزودن تسک در این تاریخ...", fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("calendar_quick_task_input")
                            )
                            Button(
                                onClick = {
                                    val trimmed = quickTaskTitle.trim()
                                    if (trimmed.isNotEmpty()) {
                                        WorldClassOsUtils.HapticSignature.crispTick(context)
                                        onQuickCreateTaskForDay(trimmed, selectedEpochDay)
                                        quickTaskTitle = ""
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal),
                                modifier = Modifier.testTag("calendar_quick_task_add_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "ثبت تسک در تاریخ انتخابی",
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarLegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(color, CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DayMetricBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(0.8.dp, color.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
            Text(text = text, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold)
        }
    }
}

private data class JalaliMonthGridMeta(
    val year: Int,
    val month: Int,
    val monthNameFa: String,
    val daysInMonth: Int,
    val firstDayEpochDay: Long,
    val leadingEmptyCells: Int // 0 = Saturday .. 6 = Friday
)

private fun jalaliMonthLength(year: Int, month: Int): Int {
    return when (month) {
        in 1..6 -> 31
        in 7..11 -> 30
        12 -> if (PersianDateAndUnitUtils.isJalaliLeapYear(year)) 30 else 29
        else -> 30
    }
}

private fun calculateJalaliMonthGridMeta(
    todayEpochDay: Long,
    currentJalaliYear: Int,
    currentJalaliMonth: Int,
    monthOffset: Int
): JalaliMonthGridMeta {
    val todayJ = PersianDateAndUnitUtils.epochDayToJalali(todayEpochDay)
    var firstOfCurrentMonthEpoch = todayEpochDay - (todayJ.day - 1)
    var targetYear = currentJalaliYear
    var targetMonth = currentJalaliMonth

    if (monthOffset > 0) {
        repeat(monthOffset) {
            val len = jalaliMonthLength(targetYear, targetMonth)
            firstOfCurrentMonthEpoch += len
            targetMonth += 1
            if (targetMonth > 12) {
                targetMonth = 1
                targetYear += 1
            }
        }
    } else if (monthOffset < 0) {
        repeat(-monthOffset) {
            targetMonth -= 1
            if (targetMonth < 1) {
                targetMonth = 12
                targetYear -= 1
            }
            val len = jalaliMonthLength(targetYear, targetMonth)
            firstOfCurrentMonthEpoch -= len
        }
    }

    val daysInTarget = jalaliMonthLength(targetYear, targetMonth)
    // ISO dayOfWeek: 1=Mon .. 5=Fri, 6=Sat, 7=Sun
    // Map to Iranian week index: 0=Sat, 1=Sun, 2=Mon, 3=Tue, 4=Wed, 5=Thu, 6=Fri
    val isoDow = PersianDateAndUnitUtils.dayOfWeekIso(firstOfCurrentMonthEpoch)
    val iranianWeekIndex = when (isoDow) {
        6 -> 0 // شنبه
        7 -> 1 // یکشنبه
        1 -> 2 // دوشنبه
        2 -> 3 // سه‌شنبه
        3 -> 4 // چهارشنبه
        4 -> 5 // پنج‌شنبه
        5 -> 6 // جمعه
        else -> 0
    }

    val monthName = PersianDateAndUnitUtils.jalaliMonthNamesFa.getOrElse(targetMonth - 1) { "$targetMonth" }
    return JalaliMonthGridMeta(
        year = targetYear,
        month = targetMonth,
        monthNameFa = monthName,
        daysInMonth = daysInTarget,
        firstDayEpochDay = firstOfCurrentMonthEpoch,
        leadingEmptyCells = iranianWeekIndex
    )
}
