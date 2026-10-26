package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.ui.components.JalaliCalendarHeatmapCard
import com.example.ui.components.MonthlySpendingTrendsLineChart
import com.example.ui.components.MonthlyVisualExpensesGlassCard
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ElectricTeal
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.MutedViolet
import com.example.ui.theme.RoseError
import com.example.ui.theme.SkyInfo
import com.example.util.PersianDateAndUnitUtils
import com.example.util.WorldClassOsUtils
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
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
    onUpdatePrefs: (UserPreferencesDoc) -> Unit = {},
    onCompleteTask: (Task) -> Unit = {},
    onStartFocusTimer: (Task) -> Unit = {},
    onToggleHabitDay: (Habit, Long) -> Unit = { _, _ -> },
    onQuickAddWater: (Double) -> Unit = {},
    onQuickAddCoffee: () -> Unit = {},
    onQuickAddPresetItem: (CustomItem) -> Unit = {},
    onCreateCustomItem: (name: String, category: String, unit: String, defaultQty: Double, dailyTarget: Double, defaultPrice: Double) -> Unit = { _, _, _, _, _, _ -> },
    onUpdateCustomItem: (CustomItem) -> Unit = {},
    onDeleteCustomItem: (CustomItem) -> Unit = {},
    onAddExpense: (title: String, category: String, amount: Double, currency: String, notes: String, txType: String) -> Unit = { _, _, _, _, _, _ -> },
    onSettleDebtOrReceivable: (ExpenseEntry) -> Unit = {},
    onDeleteExpense: (ExpenseEntry) -> Unit = {},
    onQuickCreateTaskForDay: (title: String, dueEpochDay: Long) -> Unit = { _, _ -> },
    onQuickAddMealCalories: (name: String, calories: Double, notes: String) -> Unit = { _, _, _ -> },
    onExecuteQuickSmartInput: (String) -> Unit = {},
    onOpenVoiceCapture: () -> Unit = {},
    onNavigateToSection: (String) -> Unit = {}
) {
    val context = LocalContext.current
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

    // Task calculations
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
    val totalTodayTasks = completedTodayCount + pendingTodayTasks.size
    val hasTaskData = activeTasks.isNotEmpty()
    val taskCompletionPercent = if (totalTodayTasks == 0) 0 else ((completedTodayCount.toFloat() / totalTodayTasks.toFloat()) * 100f).toInt().coerceIn(0, 100)
    val taskProgressDisplay = if (!hasTaskData) {
        "داده کافی نیست"
    } else {
        "${PersianDateAndUnitUtils.toPersianDigits(taskCompletionPercent.toString())}٪ (${PersianDateAndUnitUtils.toPersianDigits(pendingTodayTasks.size.toString())} کار باقی‌مانده)"
    }

    // Habit calculations
    val activeHabits = habits.filter { !it.archived }
    val hasHabitData = activeHabits.isNotEmpty()
    val completedHabitsToday = habitRecords.filter { it.localEpochDay == todayEpochDay }.map { it.habitId }.toSet()
    val habitScorePercent = if (!hasHabitData) 0 else ((completedHabitsToday.size * 100) / activeHabits.size)
    val habitProgressDisplay = if (!hasHabitData) {
        "داده کافی نیست"
    } else {
        "${PersianDateAndUnitUtils.toPersianDigits(habitScorePercent.toString())}٪"
    }

    // Financial calculations (Today + Net Monthly Balance strictly separated from Debt/Receivable and Transfer)
    val todayAllFinanceEntries = expenses.filter { it.localEpochDay == todayEpochDay }
    val todayExpensesByCurrency = PersianDateAndUnitUtils.aggregateExpensesByCurrency(todayAllFinanceEntries)
    val todayIncomeByCurrency = PersianDateAndUnitUtils.aggregateIncomesByCurrency(todayAllFinanceEntries)

    val monthEntries = expenses.filter { it.localEpochDay in (todayEpochDay - 29)..todayEpochDay }
    val monthNetByCurrency = PersianDateAndUnitUtils.aggregateNetBalanceByCurrency(monthEntries)
    val monthNetDefault = monthNetByCurrency[prefs.defaultCurrency] ?: 0.0
    val monthNetDisplay = if (monthEntries.isEmpty()) {
        "داده کافی نیست"
    } else {
        PersianDateAndUnitUtils.formatCurrencyAmount(monthNetDefault, prefs.defaultCurrency)
    }

    val expDisplay = if (todayExpensesByCurrency.isEmpty()) "۰ تومان" else {
        todayExpensesByCurrency.entries.joinToString(" | ") {
            PersianDateAndUnitUtils.formatCurrencyAmount(it.value, it.key)
        }
    }
    val incDisplay = if (todayIncomeByCurrency.isEmpty()) "۰ تومان" else {
        todayIncomeByCurrency.entries.joinToString(" | ") {
            PersianDateAndUnitUtils.formatCurrencyAmount(it.value, it.key)
        }
    }

    // Daily Safe-to-Spend & Estimated End-of-Month Projection
    val hasBudgetOrExpenseData = prefs.monthlyBudgetTarget > 0.0 || monthEntries.any { it.isExpense }
    val safeToSpend = WorldClassOsUtils.calculateDailySafeToSpend(
        monthlyBudget = prefs.monthlyBudgetTarget,
        defaultCurrency = prefs.defaultCurrency,
        todayEpochDay = todayEpochDay,
        expenses = expenses
    )

    // Unsettled debts and upcoming installments for Priority Reminders Card
    val unsettledDebts = expenses.filter { it.isDebt && !it.isSettled }
        .sortedBy { if (it.dueEpochDay > 0) it.dueEpochDay else Long.MAX_VALUE }
    val linkedDebtTasks = activeTasks.filter { it.linkedDebtId.isNotBlank() && it.status != "COMPLETED" }
        .sortedBy { it.dueDateEpochDay }

    // Health calculations
    val todayConsumption = consumption.filter { it.localEpochDay == todayEpochDay }
    val todayWaterMl = PersianDateAndUnitUtils.sumCompatibleConsumption(
        todayConsumption.filter { it.category == "WATER" },
        "ml"
    ).toInt()
    val latestSleep = sleepRecords.firstOrNull { it.localEpochDay == todayEpochDay } ?: sleepRecords.firstOrNull()
    val sleepText = if (latestSleep == null) "داده کافی نیست" else {
        "${PersianDateAndUnitUtils.toPersianDigits((latestSleep.durationMinutes / 60).toString())}h ${PersianDateAndUnitUtils.toPersianDigits((latestSleep.durationMinutes % 60).toString())}m"
    }
    val todayExerciseMinutes = exerciseRecords.filter { it.localEpochDay == todayEpochDay }.sumOf { it.durationMinutes }

    // Card visibility personalization via prefs.dashboardCardOrder
    val allCardKeys = listOf("REMINDERS", "FINANCE", "GOALS", "HEALTH", "CHARTS")
    val enabledCards = remember(prefs.dashboardCardOrder) {
        val raw = prefs.dashboardCardOrder
        if (raw.isEmpty()) allCardKeys.toSet() else raw.toSet()
    }
    var showCustomizeCardsDialog by remember { mutableStateOf(false) }

    if (showCustomizeCardsDialog) {
        val cardLabels = listOf(
            "REMINDERS" to "یادآوری‌های مهم و اقساط پیش‌رو",
            "FINANCE" to "وضعیت امور مالی و سهم روزانه",
            "GOALS" to "اهداف و کارهای امروز",
            "HEALTH" to "عادت‌ها و شاخص سلامت",
            "CHARTS" to "نمودارهای روند مالی و تقویم حرارتی"
        )
        AlertDialog(
            onDismissRequest = { showCustomizeCardsDialog = false },
            title = { Text("شخصی‌سازی کارت‌های داشبورد", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "کارت‌های مورد نظر برای نمایش در داشبورد را انتخاب کنید:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    cardLabels.forEach { (key, label) ->
                        val isChecked = enabledCards.contains(key)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val updated = if (isChecked && enabledCards.size > 1) {
                                        enabledCards - key
                                    } else {
                                        enabledCards + key
                                    }
                                    onUpdatePrefs(prefs.copy(dashboardCardOrder = updated.toList()))
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    val updated = if (!checked && enabledCards.size > 1) {
                                        enabledCards - key
                                    } else {
                                        enabledCards + key
                                    }
                                    onUpdatePrefs(prefs.copy(dashboardCardOrder = updated.toList()))
                                }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCustomizeCardsDialog = false }) {
                    Text("تأیید و بستن", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("pure_status_dashboard_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // =========================================================================================
        // 1. DASHBOARD HEADER & EXECUTIVE SUMMARY ("روزت چطور پیش می‌ره؟")
        // =========================================================================================
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$greeting • روزت چطور پیش می‌ره؟",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = { showCustomizeCardsDialog = true },
                        modifier = Modifier.testTag("dashboard_customize_cards_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "شخصی‌سازی کارت‌های داشبورد",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "نمای زنده",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Executive At-a-Glance Hero Card (Net Monthly Balance + Today's Spend + Active Goal % + Today's Next Tasks + 1-Tap Quick Actions)
        item {
            LiquidGlassCard(
                shape = RoundedCornerShape(22.dp),
                accentBorderColor = ElectricTeal.copy(alpha = 0.65f),
                modifier = Modifier.fillMaxWidth()
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
                        Column {
                            Text(
                                text = "موجودی خالص این ماه (درآمد منهای هزینه)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = monthNetDisplay,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                color = if (monthNetDefault >= 0.0) EmeraldSuccess else RoseError
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ElectricTeal.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = "خلاصه درآمد و هزینه",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricTeal,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = RoseError.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, RoseError.copy(alpha = 0.3f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "خرج امروز",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = expDisplay,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = RoseError,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SkyInfo.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, SkyInfo.copy(alpha = 0.3f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "هدف فعال امروز",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = taskProgressDisplay,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SkyInfo,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // 1-Tap Fast Action Row (Never more than 1 tap away)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onNavigateToSection("DAILY_LOG") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("ثبت هزینه روزانه", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { onNavigateToSection("GOALS_HABITS_0") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("مرور کارهای مهم", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { onQuickAddWater(250.0) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("+آب", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // =========================================================================================
        // 1B. PRIORITY REMINDERS & NEXT ACTION CARD ("یادآوری‌های مهم و اقساط پیش‌رو")
        // =========================================================================================
        if (enabledCards.contains("REMINDERS")) {
            item {
                DashboardStatusCard(
                    title = "یادآوری‌های مهم و اقدام بعدی",
                    badgeText = "اولویت امروز",
                    badgeColor = AmberWarning,
                    icon = Icons.Default.NotificationsActive,
                    navigationButtonText = if (unsettledDebts.isNotEmpty()) "مدیریت بدهی و اقساط" else "مدیریت کارها",
                    onNavigate = {
                        if (unsettledDebts.isNotEmpty()) onNavigateToSection("DAILY_LOG_DEBT") else onNavigateToSection("GOALS_HABITS_0")
                    }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val nextTask = pendingTodayTasks.firstOrNull()
                        val nextDebt = unsettledDebts.firstOrNull()
                        val nextInstallmentTask = linkedDebtTasks.firstOrNull()

                        if (nextTask == null && nextDebt == null && overdueCount == 0) {
                            Text(
                                text = "مورد فوری یا عقب‌افتاده‌ای برای امروز ثبت نشده است.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            if (overdueCount > 0) {
                                Text(
                                    text = "• ${PersianDateAndUnitUtils.toPersianDigits(overdueCount.toString())} تسک عقب‌افتاده نیازمند تعیین تکلیف",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = RoseError
                                )
                            }
                            if (nextTask != null) {
                                Text(
                                    text = "• کار مهم بعدی: «${nextTask.title}»",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (nextInstallmentTask != null) {
                                Text(
                                    text = "• قسط در مسیر: «${nextInstallmentTask.title}»",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AmberWarning
                                )
                            } else if (nextDebt != null) {
                                Text(
                                    text = "• بدهی باز: «${nextDebt.title}» — مانده: ${PersianDateAndUnitUtils.formatCurrencyAmount(nextDebt.remainingAmount, nextDebt.currency)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AmberWarning
                                )
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================================
        // 2. STATUS CARD 1: 💰 وضعیت امور مالی (Financial Status Card + Navigation Button)
        // =========================================================================================
        if (enabledCards.contains("FINANCE")) {
            item {
                DashboardStatusCard(
                    title = "خلاصه مالی امروز",
                    badgeText = "امور مالی",
                    badgeColor = EmeraldSuccess,
                    icon = Icons.Default.AttachMoney,
                    navigationButtonText = "مدیریت مالی",
                    onNavigate = { onNavigateToSection("DAILY_LOG") }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "سهم مجاز امروز",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (!hasBudgetOrExpenseData) {
                                        "داده کافی نیست"
                                    } else {
                                        PersianDateAndUnitUtils.formatCurrencyAmount(safeToSpend.remainingSafeToSpendToday, prefs.defaultCurrency)
                                    },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = if (!hasBudgetOrExpenseData) MaterialTheme.colorScheme.onSurfaceVariant else if (safeToSpend.isOverBudgetToday) RoseError else EmeraldSuccess
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (!hasBudgetOrExpenseData) MaterialTheme.colorScheme.surfaceVariant else if (safeToSpend.isOverBudgetToday) RoseError.copy(alpha = 0.15f) else EmeraldSuccess.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (!hasBudgetOrExpenseData) "بودجه تعیین نشده" else if (safeToSpend.isOverBudgetToday) "عبور از سقف" else "در محدوده مجاز",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!hasBudgetOrExpenseData) MaterialTheme.colorScheme.onSurfaceVariant else if (safeToSpend.isOverBudgetToday) RoseError else EmeraldSuccess,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (hasBudgetOrExpenseData) {
                            Text(
                                text = "برآورد هزینه پایان ماه (تخمینی): ${PersianDateAndUnitUtils.formatCurrencyAmount(safeToSpend.projectedEndOfMonthSpend, prefs.defaultCurrency)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "خرج امروز: $expDisplay",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = RoseError
                            )
                            Text(
                                text = "درآمد امروز: $incDisplay",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = EmeraldSuccess
                            )
                        }

                        if (prefs.monthlyBudgetTarget > 0) {
                            val budgetProgress = (safeToSpend.spentToday / (prefs.monthlyBudgetTarget / 30.0)).toFloat().coerceIn(0f, 1f)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "مصرف از بودجه روزانه",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${(budgetProgress * 100).toInt()}٪",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldSuccess
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { budgetProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape),
                                    color = EmeraldSuccess,
                                    trackColor = EmeraldSuccess.copy(alpha = 0.18f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================================
        // 3. STATUS CARD 2: 🎯 وضعیت اهداف و تسک‌ها (Goals & Tasks Status Card + Navigation Button)
        // =========================================================================================
        if (enabledCards.contains("GOALS")) {
            item {
                DashboardStatusCard(
                    title = "اهداف و کارهای امروز",
                    badgeText = "اهداف و تسک‌ها",
                    badgeColor = SkyInfo,
                    icon = Icons.Default.CheckCircle,
                    navigationButtonText = "مدیریت اهداف",
                    onNavigate = { onNavigateToSection("GOALS_HABITS_0") }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "پیشرفت امروز",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (!hasTaskData) "داده کافی نیست" else "${PersianDateAndUnitUtils.toPersianDigits(taskCompletionPercent.toString())}٪",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = SkyInfo
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "کارهای باز: ${PersianDateAndUnitUtils.toPersianDigits(pendingTodayTasks.size.toString())}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (overdueCount > 0) {
                                    Text(
                                        text = "عقب‌افتاده: ${PersianDateAndUnitUtils.toPersianDigits(overdueCount.toString())}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = AmberWarning
                                    )
                                }
                            }
                        }

                        LinearProgressIndicator(
                            progress = { taskCompletionPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = SkyInfo,
                            trackColor = SkyInfo.copy(alpha = 0.18f)
                        )

                        Text(
                            text = if (!hasTaskData) "هنوز تسکی ثبت نشده است" else "انجام‌شده: ${PersianDateAndUnitUtils.toPersianDigits(completedTodayCount.toString())} از ${PersianDateAndUnitUtils.toPersianDigits(totalTodayTasks.toString())} کار",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // =========================================================================================
        // 4. STATUS CARD 3: 🧘 وضعیت عادت‌ها و سلامت (Habits & Health Status Card + Navigation Button)
        // =========================================================================================
        if (enabledCards.contains("HEALTH")) {
            item {
                DashboardStatusCard(
                    title = "عادت‌ها و شاخص سلامت",
                    badgeText = "سلامت و روتین",
                    badgeColor = ElectricTeal,
                    icon = Icons.AutoMirrored.Filled.DirectionsRun,
                    navigationButtonText = "ثبت سلامت و روتین",
                    onNavigate = { onNavigateToSection("GOALS_HABITS_2") }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "استمرار عادت‌ها",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = habitProgressDisplay,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = ElectricTeal
                                )
                            }

                            Text(
                                text = if (!hasHabitData) "عادتی تعریف نشده" else "${PersianDateAndUnitUtils.toPersianDigits(completedHabitsToday.size.toString())} از ${PersianDateAndUnitUtils.toPersianDigits(activeHabits.size.toString())} عادت انجام شد",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.LocalDrink, contentDescription = null, tint = ElectricTeal, modifier = Modifier.size(15.dp))
                                Text(
                                    text = "آب: ${PersianDateAndUnitUtils.toPersianDigits(todayWaterMl.toString())}ml",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Bedtime, contentDescription = null, tint = MutedViolet, modifier = Modifier.size(15.dp))
                                Text(
                                    text = "خواب: $sleepText",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(15.dp))
                                Text(
                                    text = "ورزش: ${PersianDateAndUnitUtils.toPersianDigits(todayExerciseMinutes.toString())} دقیقه",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================================
        // 5-7. VISUAL ANALYTICS (CHARTS & HEATMAP)
        // =========================================================================================
        if (enabledCards.contains("CHARTS")) {
            item {
                MonthlySpendingTrendsLineChart(
                    expenses = expenses,
                    defaultCurrency = prefs.defaultCurrency
                )
            }

            item {
                MonthlyVisualExpensesGlassCard(
                    expenses = expenses,
                    defaultCurrency = prefs.defaultCurrency,
                    onNavigateToExpenses = { onNavigateToSection("DAILY_LOG") }
                )
            }

            item {
                JalaliCalendarHeatmapCard(
                    todayEpochDay = todayEpochDay,
                    defaultCurrency = prefs.defaultCurrency,
                    tasks = tasks,
                    expenses = expenses,
                    consumption = consumption,
                    habitRecords = habitRecords,
                    onQuickCreateTaskForDay = { title, dueEpochDay ->
                        onQuickCreateTaskForDay(title, dueEpochDay)
                    },
                    onCompleteTask = { task ->
                        onCompleteTask(task)
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun DashboardStatusCard(
    title: String,
    badgeText: String,
    badgeColor: Color,
    icon: ImageVector,
    navigationButtonText: String,
    onNavigate: () -> Unit,
    content: @Composable () -> Unit
) {
    LiquidGlassCard(
        shape = RoundedCornerShape(22.dp),
        accentBorderColor = badgeColor.copy(alpha = 0.65f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Card Header
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
                        color = badgeColor.copy(alpha = 0.20f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = badgeColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeColor.copy(alpha = 0.18f)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Card Body Status Content
            content()

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

            // Navigation Button at the Bottom of Dashboard Status Card
            Button(
                onClick = onNavigate,
                colors = ButtonDefaults.buttonColors(
                    containerColor = badgeColor.copy(alpha = 0.18f),
                    contentColor = badgeColor
                ),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("nav_button_${badgeText.lowercase()}")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = navigationButtonText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
