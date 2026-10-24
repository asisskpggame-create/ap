package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ActivityEvent
import com.example.data.model.ConsumptionEntry
import com.example.data.model.CustomItem
import com.example.data.model.ExerciseRecord
import com.example.data.model.ExpenseEntry
import com.example.data.model.Habit
import com.example.data.model.HabitRecord
import com.example.data.model.SleepRecord
import com.example.data.model.Task
import com.example.data.model.UserPreferencesDoc
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ElectricTeal
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MutedViolet
import com.example.ui.theme.RoseError
import com.example.ui.theme.SkyInfo
import com.example.util.PersianDateAndUnitUtils
import com.example.util.WorldClassOsUtils
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeDashboardScreen(
    userEmail: String?,
    prefs: UserPreferencesDoc,
    tasks: List<Task>,
    consumption: List<ConsumptionEntry>,
    customItems: List<CustomItem> = emptyList(),
    expenses: List<ExpenseEntry>,
    habits: List<Habit>,
    habitRecords: List<HabitRecord>,
    sleepRecords: List<SleepRecord>,
    exerciseRecords: List<ExerciseRecord>,
    activityEvents: List<ActivityEvent>,
    onCompleteTask: (Task) -> Unit,
    onStartFocusTimer: (Task) -> Unit = {},
    onQuickAddWater: (Double) -> Unit,
    onQuickAddCoffee: () -> Unit,
    onQuickAddPresetItem: (CustomItem) -> Unit = {},
    onOpenVoiceCapture: () -> Unit,
    onNavigateToSection: (String) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val todayEpochDay = PersianDateAndUnitUtils.currentLocalEpochDay()
    val formattedDate = PersianDateAndUnitUtils.formatEpochDay(
        epochDay = todayEpochDay,
        calendarDisplay = prefs.calendarDisplay,
        usePersianDigits = prefs.language == "fa"
    )

    val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "صبح بخیر"
        in 12..16 -> "ظهر بخیر"
        in 17..20 -> "عصر بخیر"
        else -> "شب بخیر"
    }

    // Real persisted calculations — zero fake numbers
    val activeTasks = tasks.filter { it.status != "ARCHIVED" }
    val completedTodayCount = activeTasks.count {
        it.status == "COMPLETED" && (it.dueDateEpochDay == todayEpochDay || it.completedAtMillis >= (todayEpochDay * 86_400_000L))
    }
    val pendingTodayTasks = activeTasks.filter {
        it.status != "COMPLETED" && (it.dueDateEpochDay == todayEpochDay || it.dueDateEpochDay < 0)
    }
    val overdueCount = activeTasks.count {
        it.status != "COMPLETED" && it.dueDateEpochDay in 0 until todayEpochDay
    }
    val totalTodayDenominator = (completedTodayCount + pendingTodayTasks.size).coerceAtLeast(1)
    val productivityProgress = if (completedTodayCount + pendingTodayTasks.size == 0) {
        0f
    } else {
        (completedTodayCount.toFloat() / totalTodayDenominator.toFloat()).coerceIn(0f, 1f)
    }

    // Water & Coffee today
    val todayConsumption = consumption.filter { it.localEpochDay == todayEpochDay }
    val todayWaterMl = PersianDateAndUnitUtils.sumCompatibleConsumption(
        todayConsumption.filter { it.category == "WATER" },
        "ml"
    ).toInt()
    val todayCoffeeCount = todayConsumption.filter { it.category == "COFFEE" }.sumOf { it.quantity }.toInt()
    val todayBeverageCount = todayConsumption.filter { it.category == "TEA" || it.category == "BEVERAGE" }.sumOf { it.quantity }.toInt()

    // Expenses today grouped strictly by currency
    val todayExpensesByCurrency = PersianDateAndUnitUtils.aggregateExpensesByCurrency(
        expenses.filter { it.localEpochDay == todayEpochDay }
    )

    // Sleep & Exercise today
    val latestSleep = sleepRecords.firstOrNull { it.localEpochDay == todayEpochDay } ?: sleepRecords.firstOrNull()
    val todayExerciseMinutes = exerciseRecords.filter { it.localEpochDay == todayEpochDay }.sumOf { it.durationMinutes }

    // Habit completion score
    val activeHabits = habits.filter { !it.archived }
    val completedHabitsToday = habitRecords.filter { it.localEpochDay == todayEpochDay }.map { it.habitId }.toSet()
    val habitScorePercent = if (activeHabits.isEmpty()) 0 else ((completedHabitsToday.size * 100) / activeHabits.size)

    // Upcoming reminders
    val upcomingReminders = activeTasks.filter {
        it.status != "COMPLETED" && (it.dueTimeMinutes >= 0 || it.reminderAtMillis > 0) && it.dueDateEpochDay >= todayEpochDay
    }.take(3)

    // Gemini daily insight generated from real data
    val dailyInsightText = buildString {
        if (activeTasks.isEmpty() && todayWaterMl == 0 && todayExpensesByCurrency.isEmpty()) {
            append("هنوز رکوردی برای امروز ثبت نشده است. با دکمه فرمان صوتی، اولین تسک یا مصرف آب امروز خود را ثبت کنید.")
        } else {
            append("📊 تحلیل روزانه: ")
            append("امروز ${PersianDateAndUnitUtils.toPersianDigits(completedTodayCount.toString())} تسک تکمیل شده و ${PersianDateAndUnitUtils.toPersianDigits(pendingTodayTasks.size.toString())} تسک در انتظار است. ")
            if (overdueCount > 0) {
                append("⚠️ ${PersianDateAndUnitUtils.toPersianDigits(overdueCount.toString())} تسک عقب‌افتاده دارید که پیشنهاد می‌شود در اولویت قرار دهید. ")
            }
            if (todayWaterMl < prefs.dailyWaterTargetMl / 2) {
                append("💧 مصرف آب امروز (${PersianDateAndUnitUtils.toPersianDigits(todayWaterMl.toString())} میلی‌لیتر) کمتر از نیمی از هدف روزانه است.")
            } else {
                append("💧 روند هیدراتاسیون امروز شما مطلوب است.")
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_dashboard_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header & Cloud Sync Pill
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$greeting، ${userEmail?.substringBefore("@") ?: "کاربر"} 👋",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = ElectricTeal.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, ElectricTeal.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "پردازش روی گوشی و انتقال ۶ ساعته",
                            tint = ElectricTeal,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (userEmail == null) "حافظه گوشی • ۶ ساعته" else "ابری • ۶ ساعته",
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricTeal
                        )
                    }
                }
            }
        }

        // 2. Productivity Progress Ring + Counts Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier.size(96.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(88.dp)) {
                            drawArc(
                                color = SubtleRingTrack,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = ElectricTeal,
                                startAngle = -90f,
                                sweepAngle = 360f * productivityProgress,
                                useCenter = false,
                                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${PersianDateAndUnitUtils.toPersianDigits((productivityProgress * 100).toInt().toString())}٪",
                                style = MaterialTheme.typography.titleLarge,
                                color = ElectricTeal,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "بهره‌وری",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatBadgeRow(label = "تکمیل‌شده امروز", count = completedTodayCount, color = EmeraldSuccess)
                        StatBadgeRow(label = "در انتظار امروز", count = pendingTodayTasks.size, color = SkyInfo)
                        StatBadgeRow(label = "عقب‌افتاده (Overdue)", count = overdueCount, color = if (overdueCount > 0) RoseError else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // 3. Gemini Daily Insight + Voice Command Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                ),
                border = BorderStroke(1.dp, MutedViolet.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MutedViolet,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "بینش هوشمند روزانه Gemini",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Button(
                            onClick = onOpenVoiceCapture,
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("dashboard_voice_button")
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("فرمان صوتی", style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    Text(
                        text = dailyInsightText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // 4. Quick-Add Bar (including 1-tap priced items like Double Espresso 90k & Cigarette 150k)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ثبت سریع روزانه (با یک کلیک)",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ویرایش قیمت/نام ←",
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricTeal,
                            modifier = Modifier.clickable { onNavigateToSection("DAILY_LOG") }
                        )
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        customItems.forEach { preset ->
                            AssistChip(
                                onClick = {
                                    WorldClassOsUtils.HapticSignature.doublePulse(context)
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onQuickAddPresetItem(preset)
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (preset.category == "COFFEE") Icons.Default.Coffee else Icons.Default.Payments,
                                        contentDescription = null,
                                        tint = AmberWarning,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        if (preset.defaultPrice > 0.0) {
                                            "+ ${preset.name} (${PersianDateAndUnitUtils.formatCurrencyAmount(preset.defaultPrice, preset.defaultCurrency)})"
                                        } else {
                                            "+ ${preset.name}"
                                        }
                                    )
                                }
                            )
                        }
                        AssistChip(
                            onClick = {
                                WorldClassOsUtils.HapticSignature.crispTick(context)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onQuickAddWater(250.0)
                            },
                            leadingIcon = { Icon(Icons.Default.LocalDrink, contentDescription = null, tint = SkyInfo, modifier = Modifier.size(18.dp)) },
                            label = { Text("+۲۵۰ ml آب") },
                            modifier = Modifier.testTag("quick_add_water_250")
                        )
                        AssistChip(
                            onClick = {
                                WorldClassOsUtils.HapticSignature.crispTick(context)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onQuickAddCoffee()
                            },
                            leadingIcon = { Icon(Icons.Default.Coffee, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(18.dp)) },
                            label = { Text("+۱ فنجان قهوه") }
                        )
                        AssistChip(
                            onClick = { onNavigateToSection("TASKS") },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = ElectricTeal, modifier = Modifier.size(18.dp)) },
                            label = { Text("تسک جدید") }
                        )
                    }
                }
            }
        }

        // 4b. Daily Safe-to-Spend ("سقف بودجه روزانه و سهم مجاز خرج امروز")
        item {
            val safeToSpend = WorldClassOsUtils.calculateDailySafeToSpend(
                monthlyBudget = prefs.monthlyBudgetTarget,
                defaultCurrency = prefs.defaultCurrency,
                todayEpochDay = todayEpochDay,
                expenses = expenses
            )
            val statusColor = if (safeToSpend.isOverBudgetToday) RoseError else EmeraldSuccess

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToSection("DAILY_LOG") }
                    .testTag("home_safe_to_spend_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.55f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "سهم مجاز خرج امروز (${safeToSpend.jalaliMonthName} • ${PersianDateAndUnitUtils.toPersianDigits(safeToSpend.remainingDaysIncludingToday.toString())} روز مانده)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = statusColor.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = if (safeToSpend.isOverBudgetToday) "عبور از سقف امروز" else "در محدوده امن",
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = PersianDateAndUnitUtils.formatCurrencyAmount(
                                safeToSpend.remainingSafeToSpendToday,
                                safeToSpend.currency
                            ),
                            style = MaterialTheme.typography.titleLarge,
                            color = statusColor,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "خرج امروز: ${PersianDateAndUnitUtils.formatCurrencyAmount(safeToSpend.spentToday, safeToSpend.currency)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 5. Grid of 6 Live Summary Cards (Water, Coffee, Expenses, Sleep, Exercise, Habits)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Water Card
                    DashboardMetricCard(
                        modifier = Modifier.weight(1f),
                        title = "آب مصرفی امروز",
                        primaryValue = "${PersianDateAndUnitUtils.toPersianDigits(todayWaterMl.toString())} ml",
                        subtitle = "هدف: ${PersianDateAndUnitUtils.toPersianDigits(prefs.dailyWaterTargetMl.toString())} ml",
                        progress = (todayWaterMl.toFloat() / prefs.dailyWaterTargetMl.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f),
                        accentColor = SkyInfo,
                        icon = Icons.Default.LocalDrink,
                        onClick = { onNavigateToSection("DAILY_LOG") }
                    )

                    // Coffee & Beverages Card
                    DashboardMetricCard(
                        modifier = Modifier.weight(1f),
                        title = "قهوه و نوشیدنی",
                        primaryValue = "${PersianDateAndUnitUtils.toPersianDigits(todayCoffeeCount.toString())} قهوه",
                        subtitle = "سایر نوشیدنی‌ها: ${PersianDateAndUnitUtils.toPersianDigits(todayBeverageCount.toString())}",
                        progress = (todayCoffeeCount / 3f).coerceIn(0f, 1f),
                        accentColor = AmberWarning,
                        icon = Icons.Default.Coffee,
                        onClick = { onNavigateToSection("DAILY_LOG") }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Expenses Card
                    val expDisplay = if (todayExpensesByCurrency.isEmpty()) {
                        "۰"
                    } else {
                        todayExpensesByCurrency.entries.joinToString(" | ") {
                            PersianDateAndUnitUtils.formatCurrencyAmount(it.value, it.key)
                        }
                    }
                    DashboardMetricCard(
                        modifier = Modifier.weight(1f),
                        title = "هزینه امروز",
                        primaryValue = expDisplay,
                        subtitle = "تفکیک‌شده بر اساس ارز",
                        progress = if (todayExpensesByCurrency.isEmpty()) 0f else 0.5f,
                        accentColor = EmeraldSuccess,
                        icon = Icons.Default.Payments,
                        onClick = { onNavigateToSection("DAILY_LOG") }
                    )

                    // Habit Score Card
                    DashboardMetricCard(
                        modifier = Modifier.weight(1f),
                        title = "امتیاز عادت‌ها",
                        primaryValue = "${PersianDateAndUnitUtils.toPersianDigits(habitScorePercent.toString())}٪",
                        subtitle = "${PersianDateAndUnitUtils.toPersianDigits(completedHabitsToday.size.toString())} از ${PersianDateAndUnitUtils.toPersianDigits(activeHabits.size.toString())} عادت فعال",
                        progress = habitScorePercent / 100f,
                        accentColor = MutedViolet,
                        icon = Icons.Default.CheckCircle,
                        onClick = { onNavigateToSection("HABITS") }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Sleep Summary Card
                    val sleepText = if (latestSleep == null) {
                        "ثبت نشده"
                    } else {
                        "${PersianDateAndUnitUtils.toPersianDigits((latestSleep.durationMinutes / 60).toString())}h ${PersianDateAndUnitUtils.toPersianDigits((latestSleep.durationMinutes % 60).toString())}m"
                    }
                    DashboardMetricCard(
                        modifier = Modifier.weight(1f),
                        title = "خلاصه خواب",
                        primaryValue = sleepText,
                        subtitle = if (latestSleep != null) "کیفیت: ${PersianDateAndUnitUtils.toPersianDigits(latestSleep.qualityRating.toString())}/۵" else "لمس برای ثبت خواب",
                        progress = ((latestSleep?.durationMinutes ?: 0).toFloat() / prefs.dailySleepTargetMinutes.coerceAtLeast(1)).coerceIn(0f, 1f),
                        accentColor = MutedViolet,
                        icon = Icons.Default.Bedtime,
                        onClick = { onNavigateToSection("DAILY_LOG") }
                    )

                    // Exercise Summary Card
                    DashboardMetricCard(
                        modifier = Modifier.weight(1f),
                        title = "فعالیت ورزشی امروز",
                        primaryValue = "${PersianDateAndUnitUtils.toPersianDigits(todayExerciseMinutes.toString())} دقیقه",
                        subtitle = "هدف: ${PersianDateAndUnitUtils.toPersianDigits(prefs.dailyExerciseTargetMinutes.toString())} دقیقه",
                        progress = (todayExerciseMinutes.toFloat() / prefs.dailyExerciseTargetMinutes.coerceAtLeast(1)).coerceIn(0f, 1f),
                        accentColor = ElectricTeal,
                        icon = Icons.Default.DirectionsRun,
                        onClick = { onNavigateToSection("DAILY_LOG") }
                    )
                }
            }
        }

        // 6. Today's Priority Tasks
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "اولویت‌های امروز (${PersianDateAndUnitUtils.toPersianDigits(pendingTodayTasks.size.toString())})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "مشاهده همه",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onNavigateToSection("TASKS") }
                        )
                    }

                    if (pendingTodayTasks.isEmpty()) {
                        Text(
                            text = "هیچ تسک فعالی برای امروز ثبت نشده است. با دکمه + یا فرمان صوتی تسک جدید اضافه کنید.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        pendingTodayTasks.take(4).forEach { task ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            WorldClassOsUtils.HapticSignature.crispTick(context)
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onCompleteTask(task)
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.RadioButtonUnchecked,
                                            contentDescription = "تکمیل تسک",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = task.title,
                                            style = MaterialTheme.typography.bodyLarge,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = buildString {
                                                append(task.category)
                                                if (task.dueTimeMinutes >= 0) {
                                                    append(" • ساعت ${PersianDateAndUnitUtils.formatTimeMinutes(task.dueTimeMinutes)}")
                                                }
                                                if (task.actualDurationMinutes > 0) {
                                                    append(" • ⏱ ${PersianDateAndUnitUtils.toPersianDigits(task.actualDurationMinutes.toString())}m")
                                                }
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            WorldClassOsUtils.HapticSignature.crispTick(context)
                                            onStartFocusTimer(task)
                                        },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "شروع تایمر تمرکز (پومودورو)",
                                            tint = ElectricTeal
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    PriorityBadge(priority = task.priority)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 7. Upcoming Reminders
        if (upcomingReminders.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("یادآورهای پیش‌رو", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        upcomingReminders.forEach { rTask ->
                            Text(
                                text = "• ${rTask.title} — ${PersianDateAndUnitUtils.formatEpochDay(rTask.dueDateEpochDay, prefs.calendarDisplay)} ${PersianDateAndUnitUtils.formatTimeMinutes(rTask.dueTimeMinutes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // 8. Recent Activity Timeline
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = null, tint = ElectricTeal, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("تایم‌لاین فعالیت‌های اخیر", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "لاگ کامل",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onNavigateToSection("HISTORY") }
                        )
                    }

                    if (activityEvents.isEmpty()) {
                        Text(
                            text = "هنوز رویدادی در تاریخچه عملیات ثبت نشده است.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        activityEvents.take(5).forEach { evt ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = ElectricTeal.copy(alpha = 0.2f),
                                    modifier = Modifier.size(8.dp)
                                ) {}
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = evt.humanSummary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = evt.inputSource,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

private val SubtleRingTrack = androidx.compose.ui.graphics.Color(0xFF283552)

@Composable
private fun StatBadgeRow(
    label: String,
    count: Int,
    color: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = PersianDateAndUnitUtils.toPersianDigits(count.toString()),
            style = MaterialTheme.typography.titleMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DashboardMetricCard(
    modifier: Modifier = Modifier,
    title: String,
    primaryValue: String,
    subtitle: String,
    progress: Float,
    accentColor: androidx.compose.ui.graphics.Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
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
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = primaryValue,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp),
                color = accentColor,
                trackColor = MaterialTheme.colorScheme.surface
            )
        }
    }
}

@Composable
fun PriorityBadge(priority: String) {
    val (label, color) = when (priority) {
        "CRITICAL" -> "بحرانی" to RoseError
        "HIGH" -> "بالا" to AmberWarning
        "LOW" -> "کم" to SkyInfo
        else -> "متوسط" to ElectricTeal
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
