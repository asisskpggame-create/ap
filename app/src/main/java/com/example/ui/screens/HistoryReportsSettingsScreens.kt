package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrackChanges
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
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
import com.example.ui.theme.CollapsibleGlassSection
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
                Text(
                    text = "تاریخچه فعالیت‌ها",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("جستجو در فعالیت‌ها...") },
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
                        "ALL" to "همه",
                        "AI" to "هوش مصنوعی",
                        "TASK" to "کارها",
                        "LOG" to "مالی و مصرف",
                        "HEALTH" to "عادت و سلامت"
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
                        "VOICE" to "صوتی",
                        "AI_ASSISTANT" to "چت",
                        "MANUAL" to "دستی"
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
    val context = LocalContext.current
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Analytics, contentDescription = null, tint = ElectricTeal)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "گزارش‌ها و تحلیل عملکرد",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            com.example.ui.components.ReportCsvExporter.shareMonthlyCsvReport(
                                context = context,
                                todayEpochDay = todayEpochDay,
                                tasks = tasks,
                                expenses = expenses,
                                consumption = consumption,
                                sleepRecords = sleepRecords
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("reports_csv_export_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("خروجی اکسل (CSV)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
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
    activityEvents: List<ActivityEvent> = emptyList(),
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

    // Collapsible Drawer Section states
    var syncSectionExpanded by rememberSaveable { mutableStateOf(true) }
    var appearanceSectionExpanded by rememberSaveable { mutableStateOf(true) }
    var targetsSectionExpanded by rememberSaveable { mutableStateOf(true) }
    var securitySectionExpanded by rememberSaveable { mutableStateOf(true) }

    // Silent 9-tap Developer Mode state
    var developerModeEnabled by rememberSaveable { mutableStateOf(false) }
    var versionTapCount by rememberSaveable { mutableIntStateOf(0) }
    var lastVersionTapMillis by rememberSaveable { mutableLongStateOf(0L) }
    var devLogsCopied by remember { mutableStateOf(false) }

    val currentPaletteTitle = com.example.ui.theme.AppThemePalette.fromCode(prefs.themePalette).titleFa

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = ElectricTeal)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "تنظیمات",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 1. Collapsible Drawer: Account & Backup
        item {
            CollapsibleGlassSection(
                title = "حساب کاربری و پشتیبان‌گیری",
                subtitle = if (isLocalOnlyMode) "ذخیره روی گوشی" else (userEmail ?: "متصل به حساب گوگل"),
                badgeText = if (syncSectionExpanded) "بستن" else "باز کردن",
                icon = Icons.Default.Security,
                accentColor = EmeraldSuccess,
                expanded = syncSectionExpanded,
                onToggle = { syncSectionExpanded = !syncSectionExpanded }
            ) {
                val lastSyncLabel = if (lastSyncMillis > 0L) {
                    val day = PersianDateAndUnitUtils.currentLocalEpochDay(lastSyncMillis)
                    PersianDateAndUnitUtils.formatEpochDay(day, prefs.calendarDisplay)
                } else {
                    "هنوز انجام نشده"
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "آخرین همگام‌سازی: $lastSyncLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (pendingSyncCount > 0) {
                        Text(
                            text = "${PersianDateAndUnitUtils.toPersianDigits(pendingSyncCount.toString())} مورد در صف",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldSuccess,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Button(
                    onClick = onTriggerSixHourSync,
                    enabled = !isSyncing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sync_now_button")
                ) {
                    Text(if (isSyncing) "در حال همگام‌سازی..." else "همگام‌سازی الان")
                }

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
                    Text("ارسال فایل پشتیبان")
                }

                if (isLocalOnlyMode) {
                    OutlinedButton(
                        onClick = onConnectOptionalGoogleAccount,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("connect_optional_google_button")
                    ) {
                        Text("اتصال به حساب گوگل")
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
                        Text("خروج از حساب")
                    }
                }
            }
        }

        // 2. Collapsible Drawer: Appearance, Theme, Layout, Calendar & Currency
        item {
            CollapsibleGlassSection(
                title = "ظاهر، تم و تقویم",
                subtitle = "$currentPaletteTitle • ${prefs.defaultCurrency}",
                badgeText = if (appearanceSectionExpanded) "بستن" else "باز کردن",
                icon = Icons.Default.Palette,
                accentColor = ElectricTeal,
                expanded = appearanceSectionExpanded,
                onToggle = { appearanceSectionExpanded = !appearanceSectionExpanded }
            ) {
                Text("تم رنگی", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
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

                Text("چیدمان صفحه", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
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
                    Text("حالت تاریک", style = MaterialTheme.typography.bodyMedium)
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

                Text("تقویم", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "JALALI" to "شمسی",
                        "GREGORIAN" to "میلادی",
                        "BOTH" to "هر دو"
                    ).forEach { (code, label) ->
                        FilterChip(
                            selected = prefs.calendarDisplay == code,
                            onClick = { onUpdatePrefs(prefs.copy(calendarDisplay = code)) },
                            label = { Text(label) }
                        )
                    }
                }

                Text("اول هفته", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
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

                Text("واحد پول", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
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

        // 3. Collapsible Drawer: Daily Targets & Monthly Budget
        item {
            CollapsibleGlassSection(
                title = "اهداف روزانه و بودجه",
                subtitle = "آب: ${PersianDateAndUnitUtils.toPersianDigits(prefs.dailyWaterTargetMl.toString())}ml • ورزش: ${PersianDateAndUnitUtils.toPersianDigits(prefs.dailyExerciseTargetMinutes.toString())} دقیقه",
                badgeText = if (targetsSectionExpanded) "بستن" else "باز کردن",
                icon = Icons.Default.TrackChanges,
                accentColor = EmeraldSuccess,
                expanded = targetsSectionExpanded,
                onToggle = { targetsSectionExpanded = !targetsSectionExpanded }
            ) {
                OutlinedTextField(
                    value = monthlyBudgetInput,
                    onValueChange = { monthlyBudgetInput = PersianDateAndUnitUtils.toEnglishDigits(it) },
                    label = { Text("بودجه ماهانه (${prefs.defaultCurrency})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = waterInput,
                    onValueChange = { waterInput = PersianDateAndUnitUtils.toEnglishDigits(it) },
                    label = { Text("هدف آب روزانه (ml)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = sleepInput,
                    onValueChange = { sleepInput = PersianDateAndUnitUtils.toEnglishDigits(it) },
                    label = { Text("هدف خواب روزانه (ساعت)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = exerciseInput,
                    onValueChange = { exerciseInput = PersianDateAndUnitUtils.toEnglishDigits(it) },
                    label = { Text("هدف ورزش روزانه (دقیقه)") },
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
                    Text("ذخیره تغییرات")
                }
            }
        }

        // 4. Collapsible Drawer: Security & Data Management
        item {
            CollapsibleGlassSection(
                title = "امنیت و داده‌ها",
                subtitle = if (prefs.vaultLockEnabled) "قفل رمز فعال است" else "قفل رمز غیرفعال",
                badgeText = if (securitySectionExpanded) "بستن" else "باز کردن",
                icon = Icons.Default.Lock,
                accentColor = ElectricTeal,
                expanded = securitySectionExpanded,
                onToggle = { securitySectionExpanded = !securitySectionExpanded }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = ElectricTeal, modifier = Modifier.size(18.dp))
                        Text("قفل رمز عبور برنامه", style = MaterialTheme.typography.bodyMedium)
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
                            label = { Text("رمز عبور") },
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
                            Text("ثبت رمز")
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ذخیره متن صوتی", style = MaterialTheme.typography.bodyMedium)
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
                    Text("ذخیره تاریخچه چت", style = MaterialTheme.typography.bodyMedium)
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
                        Text("پاک کردن چت‌ها")
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

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

                Button(
                    onClick = { showDeleteConfirmDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseError),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("delete_all_data_button")
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("حذف همه اطلاعات")
                }
            }
        }

        // 5. App Version Row (9 consecutive silent clicks unlock Developer Mode & Logging)
        item {
            val versionInteractionSource = remember { MutableInteractionSource() }
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = versionInteractionSource,
                        indication = null,
                        onClick = {
                            val now = System.currentTimeMillis()
                            // Reset streak if more than 4 seconds elapsed between taps
                            val nextCount = if (lastVersionTapMillis > 0L && (now - lastVersionTapMillis) > 4000L) {
                                1
                            } else {
                                versionTapCount + 1
                            }
                            lastVersionTapMillis = now
                            versionTapCount = nextCount
                            // Completely silent on taps 1..8; activate Developer Mode on 9th consecutive tap
                            if (nextCount >= 9) {
                                developerModeEnabled = true
                            }
                        }
                    )
                    .testTag("settings_app_version_row"),
                shape = RoundedCornerShape(20.dp),
                accentBorderColor = if (developerModeEnabled) AmberWarning.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "نسخه برنامه",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "۲.۵.۰",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // 6. Hidden Developer Mode & Live Logging Panel (Appears ONLY after 9 consecutive clicks on App Version)
        if (developerModeEnabled) {
            item {
                val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }
                val formattedLogs = remember(activityEvents, userId, pendingSyncCount, lastSyncMillis) {
                    buildList {
                        add("[SYS] Developer Mode: ACTIVE | User: $userId")
                        add("[SYNC] PendingQueue=$pendingSyncCount | LastSync=$lastSyncMillis")
                        add("[PREFS] Theme=${prefs.themePalette} | Layout=${prefs.layoutStyle} | Currency=${prefs.defaultCurrency}")
                        if (activityEvents.isEmpty()) {
                            add("[LOG] No operation events recorded yet.")
                        } else {
                            activityEvents.take(25).forEach { ev ->
                                val ts = if (ev.timestampMillis > 0L) timeFormatter.format(Date(ev.timestampMillis)) else "--:--:--"
                                add("[$ts] [${ev.inputSource}] ${ev.eventType} (${ev.operationStatus}) -> ${ev.humanSummary}")
                            }
                        }
                    }
                }

                LiquidGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("developer_mode_panel"),
                    shape = RoundedCornerShape(22.dp),
                    accentBorderColor = AmberWarning,
                    borderWidth = 1.4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BugReport,
                                    contentDescription = null,
                                    tint = AmberWarning,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "حالت توسعه‌دهنده و لاگ‌ها",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = AmberWarning
                                )
                            }

                            Button(
                                onClick = {
                                    developerModeEnabled = false
                                    versionTapCount = 0
                                    devLogsCopied = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RoseError),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("disable_developer_mode_button")
                            ) {
                                Text(
                                    text = "غیرفعال کردن",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Live Console Log Box
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF070C09).copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                formattedLogs.forEach { logLine ->
                                    Text(
                                        text = logLine,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFFA7F3D0)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    clipboard?.setPrimaryClip(
                                        ClipData.newPlainText("app_logs", formattedLogs.joinToString("\n"))
                                    )
                                    devLogsCopied = true
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("copy_developer_logs_button")
                            ) {
                                Text(if (devLogsCopied) "کپی شد ✓" else "کپی لاگ‌ها")
                            }

                            OutlinedButton(
                                onClick = {
                                    developerModeEnabled = false
                                    versionTapCount = 0
                                    devLogsCopied = false
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("بستن حالت توسعه‌دهنده")
                            }
                        }
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
            title = { Text("خروجی JSON") },
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
                        context.startActivity(Intent.createChooser(sendIntent, "اشتراک‌گذاری JSON"))
                        exportedJsonPreview = null
                    }
                ) {
                    Text("اشتراک‌گذاری")
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
            title = { Text("حذف همه اطلاعات") },
            text = {
                Text("آیا مطمئن هستید؟ این کار قابل بازگشت نیست.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteAllData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseError)
                ) {
                    Text("حذف")
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
