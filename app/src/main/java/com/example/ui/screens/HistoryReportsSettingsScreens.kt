package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.ActivityEvent
import com.example.data.model.ConsumptionEntry
import com.example.data.model.ExerciseRecord
import com.example.data.model.ExpenseEntry
import com.example.data.model.Habit
import com.example.data.model.HabitRecord
import com.example.data.model.SleepRecord
import com.example.data.model.Task
import com.example.data.model.UserPreferencesDoc
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ElectricTeal
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LiquidGlassPill
import com.example.ui.theme.MutedViolet
import com.example.ui.theme.RoseError
import com.example.ui.theme.SkyInfo
import com.example.util.PersianDateAndUnitUtils
import com.example.util.WorldClassOsUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryScreen(
    prefs: UserPreferencesDoc,
    activityEvents: List<ActivityEvent>
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedEntityFilter by remember { mutableStateOf("ALL") }
    var selectedSourceFilter by remember { mutableStateOf("ALL") }

    val filteredEvents = remember(activityEvents, searchQuery, selectedEntityFilter, selectedSourceFilter) {
        activityEvents.filter { ev ->
            val matchesQuery = searchQuery.isBlank() ||
                ev.humanSummary.contains(searchQuery, ignoreCase = true) ||
                ev.recognizedTranscript.contains(searchQuery, ignoreCase = true) ||
                ev.eventType.contains(searchQuery, ignoreCase = true) ||
                ev.afterSummary.contains(searchQuery, ignoreCase = true)

            val matchesEntity = when (selectedEntityFilter) {
                "TASK" -> ev.entityType.contains("TASK", ignoreCase = true) || ev.eventType.contains("TASK", ignoreCase = true)
                "AI" -> ev.eventType.startsWith("AI_") || ev.entityType == "AI_COMMAND"
                "LOG" -> ev.entityType in listOf("CONSUMPTION", "EXPENSE", "AddConsumptionEntry", "AddExpense")
                "HEALTH" -> ev.entityType in listOf("HABIT", "SLEEP", "EXERCISE", "AddSleepRecord", "AddExerciseRecord")
                else -> true
            }

            val matchesSource = when (selectedSourceFilter) {
                "VOICE" -> ev.inputSource == "VOICE"
                "AI_ASSISTANT" -> ev.inputSource == "AI_ASSISTANT"
                "MANUAL" -> ev.inputSource in listOf("MANUAL", "QUICK_ADD")
                else -> true
            }

            matchesQuery && matchesEntity && matchesSource
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("history_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.History, contentDescription = null, tint = ElectricTeal)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "تاریخچه فعالیت‌ها و گزارش ممیزی هوش مصنوعی",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "ثبت غیرقابل‌حذف (Append-Only) تمامی تغییرات دستی، صوتی و عملیات Gemini",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("جستجو در خلاصه عملیات، متن صوتی یا نوع رویداد...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("history_search_input"),
                shape = RoundedCornerShape(14.dp)
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "ALL" to "همه بخش‌ها",
                        "AI" to "عملیات Gemini",
                        "TASK" to "تسک‌ها",
                        "LOG" to "مصرف و هزینه",
                        "HEALTH" to "عادت، خواب و ورزش"
                    ).forEach { (code, label) ->
                        FilterChip(
                            selected = selectedEntityFilter == code,
                            onClick = { selectedEntityFilter = code },
                            label = { Text(label) }
                        )
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "ALL" to "همه ورودی‌ها",
                        "VOICE" to "فقط صوتی",
                        "AI_ASSISTANT" to "چت هوش مصنوعی",
                        "MANUAL" to "ثبت دستی / سریع"
                    ).forEach { (code, label) ->
                        FilterChip(
                            selected = selectedSourceFilter == code,
                            onClick = { selectedSourceFilter = code },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }

        if (filteredEvents.isEmpty()) {
            item {
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    accentBorderColor = ElectricTeal.copy(alpha = 0.3f)
                ) {
                    Text(
                        text = "هیچ رویدادی مطابق فیلترهای انتخابی یافت نشد.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        }

        items(filteredEvents, key = { it.id }) { event ->
            val statusColor = when (event.operationStatus) {
                "EXECUTED" -> EmeraldSuccess
                "PROPOSED" -> AmberWarning
                "REJECTED", "FAILED" -> RoseError
                else -> SkyInfo
            }

            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                accentBorderColor = statusColor.copy(alpha = 0.45f),
                borderWidth = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Text(
                                text = event.humanSummary,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    val eventEpochDay = PersianDateAndUnitUtils.currentLocalEpochDay(
                        if (event.timestampMillis > 0L) event.timestampMillis else System.currentTimeMillis()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${PersianDateAndUnitUtils.formatEpochDay(eventEpochDay, prefs.calendarDisplay)} • منشأ: ${
                                when (event.inputSource) {
                                    "VOICE" -> "فرمان صوتی"
                                    "AI_ASSISTANT" -> "دستیار Gemini"
                                    "QUICK_ADD" -> "ثبت سریع"
                                    else -> "دستی"
                                }
                            }",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        LiquidGlassPill(
                            shape = RoundedCornerShape(6.dp),
                            accentGlow = statusColor
                        ) {
                            Text(
                                text = when (event.operationStatus) {
                                    "EXECUTED" -> "اجرا شده"
                                    "PROPOSED" -> "پیشنهاد شده"
                                    "REJECTED" -> "لغو توسط کاربر"
                                    "FAILED" -> "ناموفق"
                                    else -> event.operationStatus
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (event.recognizedTranscript.isNotBlank()) {
                        GlassSurface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF141F1A),
                            alpha = 0.65f,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null, tint = MutedViolet, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "متن صوتی: «${event.recognizedTranscript}»",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    if (event.beforeSummary.isNotBlank() || event.afterSummary.isNotBlank()) {
                        Text(
                            text = buildString {
                                if (event.beforeSummary.isNotBlank()) append("قبل: ${event.beforeSummary} ← ")
                                if (event.afterSummary.isNotBlank()) append("بعد: ${event.afterSummary}")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportsScreen(
    prefs: UserPreferencesDoc,
    tasks: List<Task>,
    consumption: List<ConsumptionEntry>,
    expenses: List<ExpenseEntry>,
    habits: List<Habit>,
    habitRecords: List<HabitRecord>,
    sleepRecords: List<SleepRecord>,
    exerciseRecords: List<ExerciseRecord>
) {
    val todayEpochDay = PersianDateAndUnitUtils.currentLocalEpochDay()
    var rangeDays by remember { mutableStateOf(7) } // 7-day weekly vs 30-day monthly
    val startEpochDay = todayEpochDay - (rangeDays - 1)

    // Real persisted metrics calculation
    val activeTasks = tasks.filter { it.status != "ARCHIVED" }
    val completedInRange = activeTasks.count {
        it.status == "COMPLETED" && it.dueDateEpochDay in startEpochDay..todayEpochDay
    }
    val pendingInRange = activeTasks.count {
        it.status != "COMPLETED" && it.dueDateEpochDay in startEpochDay..todayEpochDay
    }
    val overdueTotal = activeTasks.count {
        it.status != "COMPLETED" && it.dueDateEpochDay in 0 until todayEpochDay
    }
    val totalFocusMinutes = activeTasks.sumOf { it.actualDurationMinutes }
    val taskCompletionRatio = if (completedInRange + pendingInRange > 0) {
        completedInRange.toFloat() / (completedInRange + pendingInRange).toFloat()
    } else 0f

    // 7-day water array for bar chart
    val last7Days = (6 downTo 0).map { todayEpochDay - it }
    val waterPerDayMl = last7Days.map { day ->
        val dayWaterEntries = consumption.filter { it.localEpochDay == day && it.category == "WATER" }
        PersianDateAndUnitUtils.sumCompatibleConsumption(dayWaterEntries, "ml").toFloat()
    }

    val totalCoffeeCupsInRange = consumption
        .filter { it.localEpochDay in startEpochDay..todayEpochDay && it.category == "COFFEE" }
        .count()

    val expensesInRange = expenses.filter { it.localEpochDay in startEpochDay..todayEpochDay }
    val expensesByCurrency: Map<String, Double> = PersianDateAndUnitUtils.aggregateExpensesByCurrency(expensesInRange)
    val incomesByCurrency: Map<String, Double> = PersianDateAndUnitUtils.aggregateIncomesByCurrency(expensesInRange)
    val netBalanceByCurrency: Map<String, Double> = PersianDateAndUnitUtils.aggregateNetBalanceByCurrency(expensesInRange)

    val sleepInRange = sleepRecords.filter { it.localEpochDay in startEpochDay..todayEpochDay }
    val avgSleepHours = if (sleepInRange.isNotEmpty()) {
        sleepInRange.map { it.durationMinutes }.average() / 60.0
    } else 0.0

    val exerciseMinutesInRange = exerciseRecords
        .filter { it.localEpochDay in startEpochDay..todayEpochDay }
        .sumOf { it.durationMinutes }

    val activeHabits = habits.filter { !it.archived }
    val habitCheckInsInRange = habitRecords.count {
        it.localEpochDay in startEpochDay..todayEpochDay && it.completedCount > 0
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("reports_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Analytics, contentDescription = null, tint = ElectricTeal)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "گزارش‌ها و تحلیل عملکرد",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        1 to "روزانه (امروز)",
                        7 to "هفتگی (۷ روز)",
                        30 to "ماهانه (۳۰ روز)",
                        365 to "سالانه (۱ سال)"
                    ).forEach { (days, label) ->
                        FilterChip(
                            selected = rangeDays == days,
                            onClick = { rangeDays = days },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }

        // Factual AI Summary Card
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                accentBorderColor = ElectricTeal,
                borderWidth = 1.2.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ElectricTeal, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "جمع‌بندی تحلیلی مبتنی بر داده‌های واقعی",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ElectricTeal
                        )
                    }
                    Text(
                        text = buildString {
                            append("در بازه ${PersianDateAndUnitUtils.toPersianDigits(rangeDays.toString())} روز گذشته، ")
                            append("${PersianDateAndUnitUtils.toPersianDigits(completedInRange.toString())} تسک تکمیل شده ")
                            append("و ${PersianDateAndUnitUtils.toPersianDigits(habitCheckInsInRange.toString())} ثبت عادت انجام شده است. ")
                            if (totalFocusMinutes > 0) {
                                append("مجموع زمان تمرکز عمیق و پومودورو روی تسک‌ها ${PersianDateAndUnitUtils.toPersianDigits(totalFocusMinutes.toString())} دقیقه بوده است. ")
                            }
                            if (avgSleepHours > 0) {
                                append("میانگین خواب ثبت‌شده ${PersianDateAndUnitUtils.toPersianDigits(String.format("%.1f", avgSleepHours))} ساعت ")
                            }
                            if (exerciseMinutesInRange > 0) {
                                append("و مجموع ورزش ${PersianDateAndUnitUtils.toPersianDigits(exerciseMinutesInRange.toString())} دقیقه بوده است.")
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Task Productivity Card
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                accentBorderColor = EmeraldSuccess.copy(alpha = 0.5f),
                borderWidth = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("بهره‌وری تسک‌ها و تایمر تمرکز (Focus Mode)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    LinearProgressIndicator(
                        progress = { taskCompletionRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        color = EmeraldSuccess,
                        trackColor = MaterialTheme.colorScheme.surface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "تکمیل‌شده: ${PersianDateAndUnitUtils.toPersianDigits(completedInRange.toString())}",
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldSuccess
                        )
                        Text(
                            text = "در انتظار: ${PersianDateAndUnitUtils.toPersianDigits(pendingInRange.toString())}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SkyInfo
                        )
                        Text(
                            text = "عقب‌افتاده: ${PersianDateAndUnitUtils.toPersianDigits(overdueTotal.toString())}",
                            style = MaterialTheme.typography.bodySmall,
                            color = RoseError
                        )
                    }
                    Text(
                        text = "🎯 مجموع زمان تمرکز واقعی ثبت‌شده روی تسک‌ها: ${PersianDateAndUnitUtils.toPersianDigits(totalFocusMinutes.toString())} دقیقه",
                        style = MaterialTheme.typography.labelMedium,
                        color = ElectricTeal,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // 7-Day Hydration Trend Canvas Chart
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                accentBorderColor = SkyInfo.copy(alpha = 0.5f),
                borderWidth = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("نمودار مصرف آب (۷ روز اخیر)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = "هدف روزانه: ${PersianDateAndUnitUtils.toPersianDigits(prefs.dailyWaterTargetMl.toString())} ml",
                            style = MaterialTheme.typography.labelMedium,
                            color = SkyInfo
                        )
                    }

                    val maxWater = (waterPerDayMl.maxOrNull() ?: 0f).coerceAtLeast(prefs.dailyWaterTargetMl.toFloat()).coerceAtLeast(1000f)

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    ) {
                        val barCount = waterPerDayMl.size
                        val spacing = size.width / (barCount * 2f)
                        val barWidth = spacing

                        waterPerDayMl.forEachIndexed { index, valueMl ->
                            val ratio = (valueMl / maxWater).coerceIn(0.04f, 1f)
                            val barHeight = size.height * ratio
                            val x = spacing / 2f + index * (barWidth + spacing)
                            val y = size.height - barHeight
                            drawRoundRect(
                                color = if (valueMl >= prefs.dailyWaterTargetMl) EmeraldSuccess else SkyInfo,
                                topLeft = Offset(x, y),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(10f, 10f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        last7Days.forEachIndexed { idx, day ->
                            val jalali = PersianDateAndUnitUtils.epochDayToJalali(day)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = PersianDateAndUnitUtils.toPersianDigits("${jalali.month}/${jalali.day}"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = PersianDateAndUnitUtils.toPersianDigits(waterPerDayMl[idx].toInt().toString()),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SkyInfo
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    Text(
                        text = "مجموع قهوه و نوشیدنی کافئین‌دار در این بازه: ${PersianDateAndUnitUtils.toPersianDigits(totalCoffeeCupsInRange.toString())} نوبت",
                        style = MaterialTheme.typography.bodySmall,
                        color = AmberWarning
                    )
                }
            }
        }

        // Multi-Currency Expense & Income Analytics (Daily, Weekly, Monthly, Yearly)
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                accentBorderColor = EmeraldSuccess.copy(alpha = 0.5f),
                borderWidth = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "گزارش جامع خرج‌ها و درآمدها (روزانه، هفتگی، ماهانه و سالانه)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Quick 4-period comparison rows
                    listOf(
                        "روزانه (امروز)" to expenses.filter { it.localEpochDay == todayEpochDay },
                        "هفتگی (۷ روز اخیر)" to expenses.filter { it.localEpochDay in (todayEpochDay - 6)..todayEpochDay },
                        "ماهانه (۳۰ روز اخیر)" to expenses.filter { it.localEpochDay in (todayEpochDay - 29)..todayEpochDay },
                        "سالانه (۱ سال اخیر)" to expenses.filter { it.localEpochDay in (todayEpochDay - 364)..todayEpochDay }
                    ).forEach { (periodLabel, list) ->
                        val expMap = PersianDateAndUnitUtils.aggregateExpensesByCurrency(list)
                        val incMap = PersianDateAndUnitUtils.aggregateIncomesByCurrency(list)
                        val expStr = expMap.entries.joinToString(" | ") {
                            PersianDateAndUnitUtils.formatCurrencyAmount(it.value, it.key)
                        }.ifBlank { "۰" }
                        val incStr = incMap.entries.joinToString(" | ") {
                            PersianDateAndUnitUtils.formatCurrencyAmount(it.value, it.key)
                        }.ifBlank { "۰" }

                        GlassSurface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF141F1A),
                            alpha = 0.65f,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = periodLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = "خرج: $expStr", style = MaterialTheme.typography.bodySmall, color = RoseError, fontWeight = FontWeight.Bold)
                                    Text(text = "درآمد: $incStr", style = MaterialTheme.typography.labelSmall, color = EmeraldSuccess)
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                    if (expensesByCurrency.isEmpty() && incomesByCurrency.isEmpty()) {
                        Text(
                            text = "در بازه انتخاب‌شده، خرج یا درآمدی ثبت نشده است.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        expensesByCurrency.forEach { (currency, totalAmount) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "مجموع خرج ($currency):", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = PersianDateAndUnitUtils.formatCurrencyAmount(totalAmount, currency),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = RoseError
                                )
                            }
                        }
                        incomesByCurrency.forEach { (currency, totalAmount) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "مجموع درآمد ($currency):", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = PersianDateAndUnitUtils.formatCurrencyAmount(totalAmount, currency),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldSuccess
                                )
                            }
                        }
                        netBalanceByCurrency.forEach { (currency, netAmount) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "تراز خالص ($currency):", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    text = PersianDateAndUnitUtils.formatCurrencyAmount(netAmount, currency),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricTeal
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        Text("تفکیک دسته‌بندی‌ها و دلایل:", style = MaterialTheme.typography.labelMedium)
                        val byCatAndCurr = expensesInRange.groupBy {
                            val tLabel = if (it.isIncome) "درآمد" else "خرج"
                            "$tLabel - ${it.category} (${it.currency})"
                        }
                        byCatAndCurr.forEach { (catKey, list) ->
                            val sum = list.sumOf { it.amount }
                            val curr = list.first().currency
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = catKey, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = PersianDateAndUnitUtils.formatCurrencyAmount(sum, curr),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }

        // Sleep, Exercise & Habit Summary Card
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                accentBorderColor = MutedViolet.copy(alpha = 0.5f),
                borderWidth = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("خواب، ورزش و استمرار عادت‌ها", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "• میانگین خواب شبانه: ${PersianDateAndUnitUtils.toPersianDigits(String.format("%.1f", avgSleepHours))} ساعت (${PersianDateAndUnitUtils.toPersianDigits(sleepInRange.size.toString())} شب ثبت‌شده)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "• مجموع زمان ورزش: ${PersianDateAndUnitUtils.toPersianDigits(exerciseMinutesInRange.toString())} دقیقه",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "• عادت‌های فعال: ${PersianDateAndUnitUtils.toPersianDigits(activeHabits.size.toString())} مورد | مجموع تیک‌های ثبت‌شده در بازه: ${PersianDateAndUnitUtils.toPersianDigits(habitCheckInsInRange.toString())}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    userEmail: String?,
    userId: String,
    prefs: UserPreferencesDoc,
    pendingSyncCount: Int = 0,
    lastSyncMillis: Long = 0L,
    isSyncing: Boolean = false,
    onTriggerSixHourSync: () -> Unit = {},
    onConnectOptionalGoogleAccount: () -> Unit = {},
    onUpdatePrefs: (UserPreferencesDoc) -> Unit,
    onClearConversations: () -> Unit,
    onExportJson: () -> String,
    onDeleteAllData: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val isLocalOnlyMode = userId == "local_device_user"
    var waterInput by remember(prefs.dailyWaterTargetMl) { mutableStateOf(prefs.dailyWaterTargetMl.toString()) }
    var sleepInput by remember(prefs.dailySleepTargetMinutes) { mutableStateOf((prefs.dailySleepTargetMinutes / 60).toString()) }
    var exerciseInput by remember(prefs.dailyExerciseTargetMinutes) { mutableStateOf(prefs.dailyExerciseTargetMinutes.toString()) }
    var monthlyBudgetInput by remember(prefs.monthlyBudgetTarget) { mutableStateOf(prefs.monthlyBudgetTarget.toLong().toString()) }
    var vaultPinInput by remember(prefs.vaultPinCode) { mutableStateOf(prefs.vaultPinCode) }
    var exportedJsonPreview by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = ElectricTeal)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "تنظیمات، پردازش محلی و انتقال ۶ ساعته",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // On-Device Processing & 6-Hour Periodic Data Transfer Card
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                accentBorderColor = EmeraldSuccess,
                borderWidth = 1.2.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = EmeraldSuccess)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "پردازش روی گوشی + انتقال خودکار هر ۶ ساعت",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = if (isLocalOnlyMode) {
                            "وضعیت فعلی: فعال بدون نیاز به اکانت (تمامی داده‌ها و پردازش‌ها مستقیماً روی حافظه گوشی انجام می‌شود)"
                        } else {
                            "وضعیت فعلی: پردازش روی گوشی + متصل به حساب ابری (${userEmail ?: "حساب گوگل"})"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = EmeraldSuccess
                    )
                    Text(
                        text = "رکوردهای آماده انتقال در چرخه ۶ ساعته: ${PersianDateAndUnitUtils.toPersianDigits(pendingSyncCount.toString())} مورد",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val lastSyncLabel = if (lastSyncMillis > 0L) {
                        val day = PersianDateAndUnitUtils.currentLocalEpochDay(lastSyncMillis)
                        PersianDateAndUnitUtils.formatEpochDay(day, prefs.calendarDisplay)
                    } else {
                        "در انتظار اولین چرخه ۶ ساعته"
                    }
                    Text(
                        text = "آخرین انتقال اطلاعات: $lastSyncLabel",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = onTriggerSixHourSync,
                        enabled = !isSyncing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sync_now_button")
                    ) {
                        Text(if (isSyncing) "در حال انتقال اطلاعات..." else "انتقال و پشتیبان‌گیری اطلاعات الان (چرخه ۶ ساعته)")
                    }

                    // Anti-Filtering Encrypted Local Backup & Direct Share to Telegram Saved Messages
                    OutlinedButton(
                        onClick = {
                            val json = onExportJson()
                            WorldClassOsUtils.EncryptedBackupManager.shareBackupToTelegramOrSystem(context, json)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("share_encrypted_telegram_backup_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = ElectricTeal, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("پشتیبان رمزنگاری‌شده AES-256 (ارسال به تلگرام / فایل)")
                    }

                    if (isLocalOnlyMode) {
                        OutlinedButton(
                            onClick = onConnectOptionalGoogleAccount,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("connect_optional_google_button")
                        ) {
                            Text("اتصال اختیاری به حساب گوگل (جهت همگام‌سازی ابری)")
                        }
                    } else {
                        OutlinedButton(
                            onClick = onSignOut,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sign_out_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("خروج از حساب ابری (ادامه روی گوشی بدون اکانت)")
                        }
                    }
                }
            }
        }

        // Appearance, Themes, Layout Architecture, Calendar & Currency Preferences
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                accentBorderColor = ElectricTeal.copy(alpha = 0.5f),
                borderWidth = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("تم‌های رنگی، نوع چیدمان، تقویم و واحد پول", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    Text("انتخاب تم رنگی (۶ پوسته استاندارد):", style = MaterialTheme.typography.labelMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        com.example.ui.theme.AppThemePalette.entries.forEach { palette ->
                            FilterChip(
                                selected = prefs.themePalette == palette.code,
                                onClick = {
                                    onUpdatePrefs(
                                        prefs.copy(
                                            themePalette = palette.code,
                                            darkTheme = palette.isDark
                                        )
                                    )
                                },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .background(palette.previewPrimary, CircleShape)
                                    )
                                },
                                label = { Text(palette.titleFa) },
                                modifier = Modifier.testTag("settings_theme_chip_${palette.code}")
                            )
                        }
                    }

                    Text("نوع چیدمان و معماری داشبورد (۴ حالت چیدمان):", style = MaterialTheme.typography.labelMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        com.example.ui.theme.AppLayoutStyle.entries.forEach { styleOption ->
                            FilterChip(
                                selected = prefs.layoutStyle == styleOption.code,
                                onClick = { onUpdatePrefs(prefs.copy(layoutStyle = styleOption.code)) },
                                label = { Text(styleOption.titleFa) },
                                modifier = Modifier.testTag("settings_layout_chip_${styleOption.code}")
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("پوسته تاریک OLED (Dark Theme)", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = prefs.darkTheme,
                            onCheckedChange = {
                                val nextPalette = if (it && prefs.themePalette == "MINIMAL_DAYLIGHT") {
                                    "OBSIDIAN_TEAL"
                                } else if (!it) {
                                    "MINIMAL_DAYLIGHT"
                                } else {
                                    prefs.themePalette
                                }
                                onUpdatePrefs(prefs.copy(darkTheme = it, themePalette = nextPalette))
                            },
                            modifier = Modifier.testTag("dark_theme_switch")
                        )
                    }

                    Text("نمایش تقویم:", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "JALALI" to "شمسی (جلالی)",
                            "GREGORIAN" to "میلادی",
                            "BOTH" to "هر دو تقویم"
                        ).forEach { (code, label) ->
                            FilterChip(
                                selected = prefs.calendarDisplay == code,
                                onClick = { onUpdatePrefs(prefs.copy(calendarDisplay = code)) },
                                label = { Text(label) }
                            )
                        }
                    }

                    Text("روز شروع هفته:", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            6 to "شنبه",
                            1 to "دوشنبه",
                            7 to "یکشنبه"
                        ).forEach { (code, label) ->
                            FilterChip(
                                selected = prefs.weekStartDay == code,
                                onClick = { onUpdatePrefs(prefs.copy(weekStartDay = code)) },
                                label = { Text(label) }
                            )
                        }
                    }

                    Text("واحد پول پیش‌فرض:", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "TOMAN" to "تومان",
                            "IRR" to "ریال",
                            "EUR" to "یورو (€)",
                            "USD" to "دلار ($)"
                        ).forEach { (code, label) ->
                            FilterChip(
                                selected = prefs.defaultCurrency == code,
                                onClick = { onUpdatePrefs(prefs.copy(defaultCurrency = code)) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        }

        // Daily Targets & Monthly Safe-to-Spend Budget Configuration
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                accentBorderColor = EmeraldSuccess.copy(alpha = 0.5f),
                borderWidth = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("اهداف روزانه و سقف بودجه ماهانه (Safe-to-Spend)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = monthlyBudgetInput,
                        onValueChange = { monthlyBudgetInput = PersianDateAndUnitUtils.toEnglishDigits(it) },
                        label = { Text("سقف بودجه ماهانه (${prefs.defaultCurrency})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = waterInput,
                        onValueChange = { waterInput = PersianDateAndUnitUtils.toEnglishDigits(it) },
                        label = { Text("هدف روزانه آب (میلی‌لیتر)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = sleepInput,
                        onValueChange = { sleepInput = PersianDateAndUnitUtils.toEnglishDigits(it) },
                        label = { Text("هدف روزانه خواب (ساعت)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = exerciseInput,
                        onValueChange = { exerciseInput = PersianDateAndUnitUtils.toEnglishDigits(it) },
                        label = { Text("هدف روزانه ورزش (دقیقه)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val w = waterInput.toIntOrNull()?.coerceIn(500, 10000) ?: prefs.dailyWaterTargetMl
                            val sHours = sleepInput.toIntOrNull()?.coerceIn(3, 14) ?: (prefs.dailySleepTargetMinutes / 60)
                            val exMin = exerciseInput.toIntOrNull()?.coerceIn(5, 300) ?: prefs.dailyExerciseTargetMinutes
                            val budget = monthlyBudgetInput.toDoubleOrNull()?.coerceAtLeast(100_000.0) ?: prefs.monthlyBudgetTarget
                            onUpdatePrefs(
                                prefs.copy(
                                    dailyWaterTargetMl = w,
                                    dailySleepTargetMinutes = sHours * 60,
                                    dailyExerciseTargetMinutes = exMin,
                                    monthlyBudgetTarget = budget
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ذخیره اهداف روزانه و بودجه ماهانه")
                    }
                }
            }
        }

        // Privacy Vault PIN Lock, AI Retention & Backup Card
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                accentBorderColor = ElectricTeal.copy(alpha = 0.5f),
                borderWidth = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("قفل حریم خصوصی، هوش مصنوعی و پشتیبان‌گیری", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = ElectricTeal, modifier = Modifier.size(18.dp))
                            Text("قفل پین‌کد روی بخش مالی (خرج و درآمد)", style = MaterialTheme.typography.bodyMedium)
                        }
                        Switch(
                            checked = prefs.vaultLockEnabled,
                            onCheckedChange = { enabled ->
                                val pin = vaultPinInput.ifBlank { "1234" }
                                vaultPinInput = pin
                                onUpdatePrefs(prefs.copy(vaultLockEnabled = enabled, vaultPinCode = pin))
                            },
                            modifier = Modifier.testTag("vault_lock_switch")
                        )
                    }

                    if (prefs.vaultLockEnabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = vaultPinInput,
                                onValueChange = { vaultPinInput = PersianDateAndUnitUtils.toEnglishDigits(it).take(6) },
                                label = { Text("پین‌کد قفل مالی (مثلاً 1234)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    if (vaultPinInput.isNotBlank()) {
                                        onUpdatePrefs(prefs.copy(vaultPinCode = vaultPinInput))
                                    }
                                }
                            ) {
                                Text("ثبت پین")
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("نگهداری متن دستورات صوتی در تاریخچه", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = prefs.retainVoiceTranscripts,
                            onCheckedChange = { onUpdatePrefs(prefs.copy(retainVoiceTranscripts = it)) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ذخیره تاریخچه گفتگو با Gemini", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = prefs.retainConversations,
                            onCheckedChange = { onUpdatePrefs(prefs.copy(retainConversations = it)) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onClearConversations,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("پاک‌سازی چت هوش مصنوعی")
                        }

                        Button(
                            onClick = {
                                val json = onExportJson()
                                exportedJsonPreview = json
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_json_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("خروجی JSON")
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                    Button(
                        onClick = { showDeleteConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = RoseError),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("delete_all_data_button")
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حذف کامل داده‌های حساب کاربری")
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }

    if (exportedJsonPreview != null) {
        AlertDialog(
            onDismissRequest = { exportedJsonPreview = null },
            shape = RoundedCornerShape(26.dp),
            containerColor = Color(0xFF0F1612).copy(alpha = 0.94f),
            titleContentColor = ElectricTeal,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text("خروجی JSON داده‌های شما") },
            text = {
                OutlinedTextField(
                    value = exportedJsonPreview!!,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(Intent.EXTRA_TEXT, exportedJsonPreview)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "اشتراک‌گذاری فایل پشتیبان JSON"))
                        exportedJsonPreview = null
                    }
                ) {
                    Text("اشتراک‌گذاری / ذخیره")
                }
            },
            dismissButton = {
                TextButton(onClick = { exportedJsonPreview = null }) {
                    Text("بستن")
                }
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            shape = RoundedCornerShape(26.dp),
            containerColor = Color(0xFF0F1612).copy(alpha = 0.94f),
            titleContentColor = RoseError,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text("تأیید حذف کامل داده‌ها") },
            text = {
                Text("آیا مطمئن هستید که می‌خواهید تمامی تسک‌ها، رکوردهای مصرف، هزینه‌ها، عادت‌ها و تاریخچه حساب خود را حذف کنید؟ این عمل غیرقابل بازگشت است.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteAllData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseError)
                ) {
                    Text("بله، همه را حذف کن")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }
}
