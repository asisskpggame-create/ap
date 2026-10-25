package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.GlassMotion
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.GlassmorphismDefaults
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LiquidGlassPill
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import com.example.ui.components.AiMealAndReceiptScannerModalSheet
import com.example.ui.components.AnimatedWaterWaveBox
import com.example.ui.components.HabitTrackingGlassModule
import com.example.ui.components.JalaliCalendarHeatmapCard
import com.example.ui.components.MonthlyVisualExpensesGlassCard
import com.example.ui.components.ReportCsvExporter
import com.example.ui.components.SmartBankSmsParserModalSheet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Sms
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.AppLayoutStyle
import com.example.ui.theme.AppThemePalette
import com.example.ui.theme.ElectricTeal
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MutedViolet
import com.example.ui.theme.RoseError
import com.example.ui.theme.SkyInfo
import com.example.ui.theme.TaskCategory
import com.example.ui.theme.TaskCategoryIndicator
import com.example.util.PersianDateAndUnitUtils
import com.example.util.SafeToSpendSummary
import com.example.util.WorldClassOsUtils
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
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
    onCompleteTask: (Task) -> Unit,
    onStartFocusTimer: (Task) -> Unit = {},
    onToggleHabitDay: (Habit, Long) -> Unit = { _, _ -> },
    onQuickAddWater: (Double) -> Unit,
    onQuickAddCoffee: () -> Unit,
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

    var showThemeAndLayoutStudio by rememberSaveable { mutableStateOf(false) }
    var goalsSectionExpanded by rememberSaveable { mutableStateOf(true) }
    var financeSectionExpanded by rememberSaveable { mutableStateOf(true) }
    var debtsDrawerExpanded by rememberSaveable { mutableStateOf(true) }
    var showSettledIous by rememberSaveable { mutableStateOf(false) }
    var showQuickLogManageSheet by rememberSaveable { mutableStateOf(false) }
    var showBankSmsSheet by rememberSaveable { mutableStateOf(false) }
    var showAiPhotoScannerSheet by rememberSaveable { mutableStateOf(false) }
    var showQuickFinanceDialog by rememberSaveable { mutableStateOf(false) }
    var initialQuickFinanceType by rememberSaveable { mutableStateOf("EXPENSE") }
    var showQuickGoalTaskDialog by rememberSaveable { mutableStateOf(false) }
    var selectedDashboardFilter by rememberSaveable { mutableStateOf("ALL") } // ALL, GOALS, FINANCE, CALENDAR, TASKS, WELLNESS
    var editingPreset by remember { mutableStateOf<CustomItem?>(null) }
    var showNewPresetDialog by remember { mutableStateOf(false) }
    var quickSmartInputText by rememberSaveable { mutableStateOf("") }

    val currentLayoutStyle = AppLayoutStyle.fromCode(prefs.layoutStyle)
    val currentThemePalette = AppThemePalette.fromCode(prefs.themePalette)

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

    // Expenses, Incomes, Debts & Receivables grouped strictly by currency
    val todayAllFinanceEntries = expenses.filter { it.localEpochDay == todayEpochDay }
    val todayExpensesByCurrency = PersianDateAndUnitUtils.aggregateExpensesByCurrency(todayAllFinanceEntries)
    val todayIncomeByCurrency = PersianDateAndUnitUtils.aggregateIncomesByCurrency(todayAllFinanceEntries)
    val activeDebts = remember(expenses) { expenses.filter { it.isDebt && !it.isSettled } }
    val activeReceivables = remember(expenses) { expenses.filter { it.isReceivable && !it.isSettled } }
    val activeIous = remember(activeDebts, activeReceivables) {
        (activeReceivables + activeDebts).sortedByDescending { it.timestampMillis }
    }
    val settledIous = remember(expenses) {
        expenses.filter { (it.isDebt || it.isReceivable) && it.isSettled }.sortedByDescending { it.timestampMillis }
    }
    val totalDebtsByCurrency = remember(expenses) { PersianDateAndUnitUtils.aggregateDebtsByCurrency(expenses) }
    val totalReceivablesByCurrency = remember(expenses) { PersianDateAndUnitUtils.aggregateReceivablesByCurrency(expenses) }
    val debtDisplay = if (totalDebtsByCurrency.isEmpty()) {
        "۰ تومان"
    } else {
        totalDebtsByCurrency.entries.joinToString(" | ") {
            PersianDateAndUnitUtils.formatCurrencyAmount(it.value, it.key)
        }
    }
    val receivableDisplay = if (totalReceivablesByCurrency.isEmpty()) {
        "۰ تومان"
    } else {
        totalReceivablesByCurrency.entries.joinToString(" | ") {
            PersianDateAndUnitUtils.formatCurrencyAmount(it.value, it.key)
        }
    }
    val expDisplay = if (todayExpensesByCurrency.isEmpty()) {
        "۰"
    } else {
        todayExpensesByCurrency.entries.joinToString(" | ") {
            PersianDateAndUnitUtils.formatCurrencyAmount(it.value, it.key)
        }
    }
    val incDisplay = if (todayIncomeByCurrency.isEmpty()) {
        "۰"
    } else {
        todayIncomeByCurrency.entries.joinToString(" | ") {
            PersianDateAndUnitUtils.formatCurrencyAmount(it.value, it.key)
        }
    }

    // Sleep & Exercise today
    val latestSleep = sleepRecords.firstOrNull { it.localEpochDay == todayEpochDay } ?: sleepRecords.firstOrNull()
    val sleepText = if (latestSleep == null) {
        "ثبت نشده"
    } else {
        "${PersianDateAndUnitUtils.toPersianDigits((latestSleep.durationMinutes / 60).toString())}h ${PersianDateAndUnitUtils.toPersianDigits((latestSleep.durationMinutes % 60).toString())}m"
    }
    val todayExerciseMinutes = exerciseRecords.filter { it.localEpochDay == todayEpochDay }.sumOf { it.durationMinutes }

    // Habit completion score
    val activeHabits = habits.filter { !it.archived }
    val completedHabitsToday = habitRecords.filter { it.localEpochDay == todayEpochDay }.map { it.habitId }.toSet()
    val habitScorePercent = if (activeHabits.isEmpty()) 0 else ((completedHabitsToday.size * 100) / activeHabits.size)

    // Combined Daily Goals Score (Tasks + Habits + Water + Exercise)
    val waterGoalRatio = (todayWaterMl.toFloat() / prefs.dailyWaterTargetMl.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val exerciseGoalRatio = (todayExerciseMinutes.toFloat() / prefs.dailyExerciseTargetMinutes.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val overallGoalsScorePercent = (((productivityProgress + (habitScorePercent / 100f) + waterGoalRatio + exerciseGoalRatio) / 4f) * 100f).toInt().coerceIn(0, 100)

    // Upcoming reminders
    val upcomingReminders = activeTasks.filter {
        it.status != "COMPLETED" && (it.dueTimeMinutes >= 0 || it.reminderAtMillis > 0) && it.dueDateEpochDay >= todayEpochDay
    }.take(3)

    // Daily Safe-to-Spend
    val safeToSpend = WorldClassOsUtils.calculateDailySafeToSpend(
        monthlyBudget = prefs.monthlyBudgetTarget,
        defaultCurrency = prefs.defaultCurrency,
        todayEpochDay = todayEpochDay,
        expenses = expenses
    )

    // Modal Bottom Sheet for managing Quick-Log items
    if (showQuickLogManageSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQuickLogManageSheet = false },
            containerColor = Color(0xFF0F1612).copy(alpha = 0.94f),
            scrimColor = Color.Black.copy(alpha = 0.65f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "مدیریت دکمه‌های ثبت سریع",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedButton(
                        onClick = { showNewPresetDialog = true },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("دکمه جدید")
                    }
                }

                if (customItems.isEmpty()) {
                    Text(
                        text = "هنوز دکمه سفارشی ثابتی نساخته‌اید.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    customItems.forEach { preset ->
                        LiquidGlassCard(
                            shape = RoundedCornerShape(14.dp),
                            accentBorderColor = ElectricTeal,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = preset.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (preset.defaultPrice > 0.0) {
                                            PersianDateAndUnitUtils.formatCurrencyAmount(preset.defaultPrice, preset.defaultCurrency)
                                        } else {
                                            "${PersianDateAndUnitUtils.toPersianDigits(preset.defaultQuantity.toInt().toString())} ${preset.unit}"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                TextButton(onClick = { editingPreset = preset }) {
                                    Text("ویرایش")
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Dialog for editing/creating a preset
    if (editingPreset != null || showNewPresetDialog) {
        val currentEditing = editingPreset
        var dialogName by remember(currentEditing, showNewPresetDialog) {
            mutableStateOf(currentEditing?.name ?: "")
        }
        var dialogPriceText by remember(currentEditing, showNewPresetDialog) {
            mutableStateOf(
                if (currentEditing != null && currentEditing.defaultPrice > 0.0) {
                    currentEditing.defaultPrice.toLong().toString()
                } else ""
            )
        }
        AlertDialog(
            onDismissRequest = {
                editingPreset = null
                showNewPresetDialog = false
            },
            shape = RoundedCornerShape(26.dp),
            containerColor = Color(0xFF0F1612).copy(alpha = 0.94f),
            titleContentColor = ElectricTeal,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = {
                Text(
                    text = if (currentEditing != null) "ویرایش «${currentEditing.name}»" else "دکمه ثبت سریع جدید",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = dialogName,
                        onValueChange = { dialogName = it },
                        label = { Text("عنوان دکمه (مثلاً سیگار، قهوه، مترو)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = dialogPriceText,
                        onValueChange = { dialogPriceText = PersianDateAndUnitUtils.toEnglishDigits(it) },
                        label = { Text("مبلغ (${prefs.defaultCurrency})") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    val rawDialogPrice = PersianDateAndUnitUtils.toEnglishDigits(dialogPriceText)
                        .replace(",", "")
                        .toDoubleOrNull() ?: 0.0
                    if (rawDialogPrice in 1.0..999.0 && prefs.defaultCurrency.equals("TOMAN", ignoreCase = true)) {
                        Text(
                            text = "💡 معادل محاوره‌ای: ${PersianDateAndUnitUtils.formatCurrencyAmount(rawDialogPrice * 1000.0, "TOMAN")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricTeal,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmedName = dialogName.trim()
                        if (trimmedName.isNotEmpty()) {
                            val rawPrice = PersianDateAndUnitUtils.toEnglishDigits(dialogPriceText)
                                .replace(",", "")
                                .toDoubleOrNull()
                                ?.coerceAtLeast(0.0) ?: 0.0
                            val parsedPrice = PersianDateAndUnitUtils.normalizeColloquialTomanAmount(rawPrice, prefs.defaultCurrency)
                            if (currentEditing != null) {
                                onUpdateCustomItem(
                                    currentEditing.copy(
                                        name = trimmedName,
                                        defaultPrice = parsedPrice
                                    )
                                )
                            } else {
                                onCreateCustomItem(trimmedName, "COFFEE", "pieces", 1.0, 2.0, parsedPrice)
                            }
                            editingPreset = null
                            showNewPresetDialog = false
                        }
                    }
                ) {
                    Text("ذخیره")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (currentEditing != null) {
                        TextButton(
                            onClick = {
                                onDeleteCustomItem(currentEditing)
                                editingPreset = null
                            }
                        ) {
                            Text("حذف", color = RoseError)
                        }
                    }
                    TextButton(
                        onClick = {
                            editingPreset = null
                            showNewPresetDialog = false
                        }
                    ) {
                        Text("انصراف")
                    }
                }
            }
        )
    }

    // Inline Quick Finance Dialog (supports Expense, Income, Receivable, Debt)
    if (showQuickFinanceDialog) {
        var txType by remember(initialQuickFinanceType) { mutableStateOf(initialQuickFinanceType) }
        var titleText by remember { mutableStateOf("") }
        var amountText by remember { mutableStateOf("") }
        var notesText by remember { mutableStateOf("") }
        var category by remember(initialQuickFinanceType) {
            mutableStateOf(
                when (initialQuickFinanceType) {
                    "INCOME" -> "SALARY"
                    "DEBT", "RECEIVABLE" -> "OTHER"
                    else -> "FOOD"
                }
            )
        }

        val dialogAccent = when (txType) {
            "INCOME" -> EmeraldSuccess
            "RECEIVABLE" -> SkyInfo
            "DEBT" -> AmberWarning
            else -> RoseError
        }

        AlertDialog(
            onDismissRequest = { showQuickFinanceDialog = false },
            shape = RoundedCornerShape(26.dp),
            containerColor = Color(0xFF0F1612).copy(alpha = 0.96f),
            titleContentColor = dialogAccent,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = {
                Text(
                    text = when (txType) {
                        "RECEIVABLE" -> "🤝 ثبت طلبکاری جدید"
                        "DEBT" -> "🤝 ثبت بدهکاری جدید"
                        "INCOME" -> "💰 ثبت درآمد جدید"
                        else -> "💰 ثبت هزینه جدید"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = txType == "EXPENSE",
                            onClick = {
                                txType = "EXPENSE"
                                category = "FOOD"
                            },
                            label = { Text("خرج") },
                            modifier = Modifier.testTag("quick_finance_type_EXPENSE")
                        )
                        FilterChip(
                            selected = txType == "INCOME",
                            onClick = {
                                txType = "INCOME"
                                category = "SALARY"
                            },
                            label = { Text("درآمد") },
                            modifier = Modifier.testTag("quick_finance_type_INCOME")
                        )
                        FilterChip(
                            selected = txType == "RECEIVABLE",
                            onClick = {
                                txType = "RECEIVABLE"
                                category = "OTHER"
                            },
                            label = { Text("طلبکاری (طلب من)") },
                            modifier = Modifier.testTag("quick_finance_type_RECEIVABLE")
                        )
                        FilterChip(
                            selected = txType == "DEBT",
                            onClick = {
                                txType = "DEBT"
                                category = "OTHER"
                            },
                            label = { Text("بدهکاری (بدهی من)") },
                            modifier = Modifier.testTag("quick_finance_type_DEBT")
                        )
                    }
                    OutlinedTextField(
                        value = titleText,
                        onValueChange = { titleText = it },
                        label = {
                            Text(
                                when (txType) {
                                    "RECEIVABLE" -> "نام شخص و موضوع طلب (مثلاً علی - قرض)"
                                    "DEBT" -> "نام شخص و موضوع بدهی (مثلاً رضا - شام)"
                                    "INCOME" -> "عنوان درآمد (مثلاً حقوق، پروژه)"
                                    else -> "عنوان هزینه (مثلاً سیگار، ناهار)"
                                }
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quick_finance_title_input")
                    )
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = PersianDateAndUnitUtils.toEnglishDigits(it) },
                        label = { Text("مبلغ (${prefs.defaultCurrency})") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quick_finance_amount_input")
                    )
                    val rawEntered = PersianDateAndUnitUtils.toEnglishDigits(amountText)
                        .replace(",", "")
                        .toDoubleOrNull() ?: 0.0
                    if (rawEntered in 1.0..999.0 && prefs.defaultCurrency.equals("TOMAN", ignoreCase = true)) {
                        Text(
                            text = "💡 معادل محاوره‌ای: ${PersianDateAndUnitUtils.formatCurrencyAmount(rawEntered * 1000.0, "TOMAN")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = dialogAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (txType == "DEBT" || txType == "RECEIVABLE") {
                        OutlinedTextField(
                            value = notesText,
                            onValueChange = { notesText = it },
                            label = { Text("توضیحات یا موعد تسویه (اختیاری)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("quick_finance_notes_input")
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val categories = if (txType == "INCOME") {
                                listOf("SALARY" to "حقوق", "FREELANCE" to "پروژه", "GIFT" to "هدیه", "OTHER" to "سایر")
                            } else {
                                listOf("FOOD" to "خوراک", "COFFEE" to "کافه/روزمره", "TRANSPORT" to "حمل‌ونقل", "SHOPPING" to "خرید", "BILLS" to "قبوض", "OTHER" to "سایر")
                            }
                            categories.forEach { (code, label) ->
                                FilterChip(
                                    selected = category == code,
                                    onClick = { category = code },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val rawAmount = PersianDateAndUnitUtils.toEnglishDigits(amountText)
                            .replace(",", "")
                            .toDoubleOrNull() ?: 0.0
                        val normalizedAmount = PersianDateAndUnitUtils.normalizeColloquialTomanAmount(rawAmount, prefs.defaultCurrency)
                        val cleanTitle = titleText.trim().ifEmpty {
                            when (txType) {
                                "RECEIVABLE" -> "طلبکاری"
                                "DEBT" -> "بدهکاری"
                                "INCOME" -> "درآمد روزانه"
                                else -> "هزینه روزانه"
                            }
                        }
                        if (normalizedAmount > 0.0) {
                            onAddExpense(cleanTitle, category, normalizedAmount, prefs.defaultCurrency, notesText.trim(), txType)
                            showQuickFinanceDialog = false
                        }
                    },
                    modifier = Modifier.testTag("quick_finance_submit_button")
                ) {
                    Text("ثبت")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickFinanceDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }

    // Inline Quick Goal / Task Dialog
    if (showQuickGoalTaskDialog) {
        var goalTitle by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showQuickGoalTaskDialog = false },
            shape = RoundedCornerShape(26.dp),
            containerColor = Color(0xFF0F1612).copy(alpha = 0.96f),
            titleContentColor = ElectricTeal,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = {
                Text(
                    text = "🎯 هدف یا کار جدید امروز",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                OutlinedTextField(
                    value = goalTitle,
                    onValueChange = { goalTitle = it },
                    label = { Text("عنوان هدف / تسک امروز") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = goalTitle.trim()
                        if (trimmed.isNotEmpty()) {
                            onQuickCreateTaskForDay(trimmed, todayEpochDay)
                            showQuickGoalTaskDialog = false
                        }
                    }
                ) {
                    Text("افزودن")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickGoalTaskDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }

    if (showBankSmsSheet) {
        SmartBankSmsParserModalSheet(
            defaultCurrency = prefs.defaultCurrency,
            onDismiss = { showBankSmsSheet = false },
            onConfirmSaveExpense = { title, cat, amountToman, notes, txType ->
                onAddExpense(title, cat, amountToman, "TOMAN", notes, txType)
            }
        )
    }

    if (showAiPhotoScannerSheet) {
        AiMealAndReceiptScannerModalSheet(
            onDismiss = { showAiPhotoScannerSheet = false },
            onLogScannedMeal = { name, kcal, notes ->
                onQuickAddMealCalories(name, kcal, notes)
            },
            onLogScannedReceipt = { title, cat, amountToman, notes ->
                onAddExpense(title, cat, amountToman, "TOMAN", notes, "EXPENSE")
            }
        )
    }

    val itemVerticalSpacing = when (currentLayoutStyle) {
        AppLayoutStyle.COMPACT_PRO -> 10.dp
        AppLayoutStyle.MINIMAL_ZEN -> 16.dp
        else -> 12.dp
    }

    val showGoalsDomain = selectedDashboardFilter in listOf("ALL", "GOALS", "TASKS", "WELLNESS", "CALENDAR")
    val showFinanceDomain = selectedDashboardFilter in listOf("ALL", "FINANCE")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_dashboard_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(itemVerticalSpacing)
    ) {
        // =========================================================================================
        // 1. CLEAN HEADER +⚡ ZERO-FRICTION QUICK SMART OMNI-BAR (Voice + Text + 1-Tap Presets)
        // =========================================================================================
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Compact Top Greeting + Date + Theme/Layout Studio Icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "$greeting، ${userEmail?.substringBefore("@") ?: "کاربر"}",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val themeArrowRotation by animateFloatAsState(
                            targetValue = if (showThemeAndLayoutStudio) 180f else 0f,
                            animationSpec = GlassMotion.FluidSpring,
                            label = "theme_drawer_arrow"
                        )
                        LiquidGlassPill(
                            accentGlow = MaterialTheme.colorScheme.primary,
                            onClick = {
                                WorldClassOsUtils.HapticSignature.crispTick(context)
                                showThemeAndLayoutStudio = !showThemeAndLayoutStudio
                            },
                            modifier = Modifier.testTag("toggle_theme_layout_studio_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = "تم و چیدمان",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "تم",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .size(15.dp)
                                        .rotate(themeArrowRotation)
                                )
                            }
                        }
                    }
                }

                // Expandable Interactive Theme & Layout Studio Drawer
                AnimatedVisibility(visible = showThemeAndLayoutStudio) {
                    LiquidGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("theme_layout_studio_card"),
                        shape = RoundedCornerShape(22.dp),
                        accentBorderColor = MaterialTheme.colorScheme.primary
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "تم رنگی و چیدمان",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                    modifier = Modifier.clickable { showThemeAndLayoutStudio = false }
                                ) {
                                    Text(
                                        text = "بستن ▲",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AppThemePalette.entries.forEach { palette ->
                                    val isSelected = currentThemePalette == palette
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            WorldClassOsUtils.HapticSignature.crispTick(context)
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
                                        label = { Text(palette.titleFa, style = MaterialTheme.typography.labelMedium) },
                                        modifier = Modifier.testTag("theme_chip_${palette.code}")
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AppLayoutStyle.entries.forEach { styleOption ->
                                    FilterChip(
                                        selected = currentLayoutStyle == styleOption,
                                        onClick = {
                                            WorldClassOsUtils.HapticSignature.crispTick(context)
                                            onUpdatePrefs(prefs.copy(layoutStyle = styleOption.code))
                                        },
                                        label = { Text(styleOption.titleFa, style = MaterialTheme.typography.labelMedium) },
                                        modifier = Modifier.testTag("layout_chip_${styleOption.code}")
                                    )
                                }
                            }
                        }
                    }
                }

                // ⚡ ZERO-FRICTION QUICK SMART OMNI-BAR (Always at Top of Home for <3s Logging)
                LiquidGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_quick_omni_bar"),
                    shape = RoundedCornerShape(22.dp),
                    accentBorderColor = EmeraldSuccess.copy(alpha = 0.65f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Row 1: Fast Natural Language Input + 1-Tap Submit + 1-Tap Auto-Listen Mic
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = quickSmartInputText,
                                onValueChange = { quickSmartInputText = it },
                                placeholder = {
                                    Text(
                                        text = "سریع بنویس: ۵۰۰ سیگار، ۱۲۰ اسنپ، ۳۰۰ طلب از علی...",
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("home_quick_input_field")
                            )

                            if (quickSmartInputText.isNotBlank()) {
                                Button(
                                    onClick = {
                                        val cmd = quickSmartInputText.trim()
                                        if (cmd.isNotEmpty()) {
                                            WorldClassOsUtils.HapticSignature.doublePulse(context)
                                            onExecuteQuickSmartInput(cmd)
                                            quickSmartInputText = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EmeraldSuccess,
                                        contentColor = Color(0xFF06130C)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.testTag("home_quick_submit_button")
                                ) {
                                    Text(
                                        text = "⚡ ثبت",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .clickable {
                                        WorldClassOsUtils.HapticSignature.crispTick(context)
                                        onOpenVoiceCapture()
                                    }
                                    .testTag("dashboard_voice_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "ثبت صوتی آنی",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "ویس",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }

                        // Row 2: 1-Tap Presets + Smart Finance Tools (Horizontally Scrollable, Zero Clutter)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // User's 1-Tap Price/Consumption Presets (Tap = Instant Save, Long-Press = Edit)
                            customItems.forEach { preset ->
                                GlassSurface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF141F1A),
                                    alpha = 0.72f,
                                    accentBorderColor = ElectricTeal,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .combinedClickable(
                                            onClick = {
                                                WorldClassOsUtils.HapticSignature.doublePulse(context)
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onQuickAddPresetItem(preset)
                                            },
                                            onLongClick = {
                                                WorldClassOsUtils.HapticSignature.heavyImpact(context)
                                                editingPreset = preset
                                            }
                                        )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = if (preset.defaultPrice > 0.0) {
                                                "+ ${preset.name} (${PersianDateAndUnitUtils.formatCurrencyAmount(preset.defaultPrice, preset.defaultCurrency)})"
                                            } else {
                                                "+ ${preset.name}"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            LiquidGlassPill(
                                accentGlow = ElectricTeal,
                                onClick = {
                                    WorldClassOsUtils.HapticSignature.crispTick(context)
                                    showBankSmsSheet = true
                                },
                                modifier = Modifier.testTag("open_bank_sms_parser_pill")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Sms, contentDescription = null, tint = ElectricTeal, modifier = Modifier.size(13.dp))
                                    Text("پیامک بانک", style = MaterialTheme.typography.labelSmall, color = ElectricTeal, fontWeight = FontWeight.Bold)
                                }
                            }

                            LiquidGlassPill(
                                accentGlow = MutedViolet,
                                onClick = {
                                    WorldClassOsUtils.HapticSignature.crispTick(context)
                                    showAiPhotoScannerSheet = true
                                },
                                modifier = Modifier.testTag("open_ai_photo_scanner_pill")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = MutedViolet, modifier = Modifier.size(13.dp))
                                    Text("اسکن فاکتور", style = MaterialTheme.typography.labelSmall, color = MutedViolet, fontWeight = FontWeight.Bold)
                                }
                            }

                            LiquidGlassPill(
                                accentGlow = EmeraldSuccess,
                                onClick = {
                                    WorldClassOsUtils.HapticSignature.crispTick(context)
                                    ReportCsvExporter.shareMonthlyCsvReport(
                                        context = context,
                                        todayEpochDay = todayEpochDay,
                                        tasks = tasks,
                                        expenses = expenses,
                                        consumption = consumption,
                                        sleepRecords = sleepRecords
                                    )
                                },
                                modifier = Modifier.testTag("export_csv_report_pill")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.FileDownload, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(13.dp))
                                    Text("خروجی اکسل", style = MaterialTheme.typography.labelSmall, color = EmeraldSuccess, fontWeight = FontWeight.Bold)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .clickable { showQuickLogManageSheet = true }
                                    .testTag("manage_quick_presets_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = "مدیریت دکمه‌های ثبت سریع",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "دکمه‌ها",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Single-Row 4-Tab Domain Switcher: همه | 💰 مالی | 🎯 اهداف | 📅 تقویم
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_domain_switcher"),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val primaryTabs = listOf(
                        Triple("ALL", "همه", MaterialTheme.colorScheme.primary),
                        Triple("FINANCE", "💰 مالی", EmeraldSuccess),
                        Triple("GOALS", "🎯 اهداف", ElectricTeal),
                        Triple("CALENDAR", "📅 تقویم", SkyInfo)
                    )
                    primaryTabs.forEach { (code, title, accent) ->
                        val isSelected = selectedDashboardFilter == code ||
                            (code == "GOALS" && selectedDashboardFilter in listOf("TASKS", "WELLNESS"))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) accent.copy(alpha = 0.22f) else Color(0x14FFFFFF),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) accent else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    WorldClassOsUtils.HapticSignature.crispTick(context)
                                    selectedDashboardFilter = code
                                }
                                .testTag("dashboard_filter_chip_$code")
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                                    color = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================================
        // SECTION 1 (TOP PRIORITY): 💰 بخش مالی و مدیریت بودجه (FINANCE & BUDGET SECTION)
        // =========================================================================================
        if (showFinanceDomain) {
            item {
                val financeArrowRotation by animateFloatAsState(
                    targetValue = if (financeSectionExpanded) 180f else 0f,
                    animationSpec = GlassMotion.FluidSpring,
                    label = "finance_section_arrow"
                )
                val statusColor = if (safeToSpend.isOverBudgetToday) RoseError else EmeraldSuccess
                val dailyAllowanceBase = (safeToSpend.remainingSafeToSpendToday + safeToSpend.spentToday).coerceAtLeast(1.0)
                val spentRatio = (safeToSpend.spentToday / dailyAllowanceBase).toFloat().coerceIn(0f, 1f)

                LiquidGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_finance_section_header"),
                    shape = RoundedCornerShape(22.dp),
                    accentBorderColor = EmeraldSuccess.copy(alpha = 0.65f)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { financeSectionExpanded = !financeSectionExpanded }
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            EmeraldSuccess.copy(alpha = 0.18f),
                                            AmberWarning.copy(alpha = 0.08f),
                                            Color.Transparent
                                        )
                                    )
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "💰 بخش مالی",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = statusColor.copy(alpha = 0.2f),
                                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = if (safeToSpend.isOverBudgetToday) "فراتر از سقف" else "متعادل",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = statusColor,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "خرج امروز: $expDisplay   •   درآمد امروز: $incDisplay",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = EmeraldSuccess.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.6f)),
                                    modifier = Modifier
                                        .clickable {
                                            WorldClassOsUtils.HapticSignature.crispTick(context)
                                            initialQuickFinanceType = "EXPENSE"
                                            showQuickFinanceDialog = true
                                        }
                                        .testTag("home_quick_add_expense_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = EmeraldSuccess,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = "ثبت مالی",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = EmeraldSuccess,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = EmeraldSuccess.copy(alpha = 0.18f),
                                    border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.55f)),
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clickable { financeSectionExpanded = !financeSectionExpanded }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = if (financeSectionExpanded) "بستن بخش مالی" else "باز کردن بخش مالی",
                                            tint = EmeraldSuccess,
                                            modifier = Modifier
                                                .size(18.dp)
                                                .rotate(financeArrowRotation)
                                        )
                                    }
                                }
                            }
                        }

                        // Integrated Safe-to-Spend + Today's Expense/Income Summary inside the Finance Card
                        if (financeSectionExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Safe-to-Spend Box
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(statusColor.copy(alpha = 0.08f))
                                        .border(
                                            1.dp,
                                            GlassmorphismDefaults.specularBorderBrush(statusColor, topIntensity = 0.35f),
                                            RoundedCornerShape(16.dp)
                                        )
                                        .clickable { onNavigateToSection("DAILY_LOG") }
                                        .testTag("home_safe_to_spend_card")
                                        .padding(12.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "سقف مجاز خرج امروز (${safeToSpend.jalaliMonthName} • ${PersianDateAndUnitUtils.toPersianDigits(safeToSpend.remainingDaysIncludingToday.toString())} روز مانده)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = PersianDateAndUnitUtils.formatCurrencyAmount(
                                                    safeToSpend.remainingSafeToSpendToday,
                                                    safeToSpend.currency
                                                ),
                                                style = MaterialTheme.typography.titleMedium,
                                                color = statusColor,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                        LinearProgressIndicator(
                                            progress = { (1f - spentRatio).coerceIn(0.04f, 1f) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(5.dp)
                                                .clip(RoundedCornerShape(50)),
                                            color = statusColor,
                                            trackColor = MaterialTheme.colorScheme.surface
                                        )
                                    }
                                }

                                // Dual Quick Tap Cards: ثبت خرج | ثبت درآمد
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    GlassSurface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable {
                                                initialQuickFinanceType = "EXPENSE"
                                                showQuickFinanceDialog = true
                                            },
                                        shape = RoundedCornerShape(14.dp),
                                        accentBorderColor = RoseError
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("خرج امروز", style = MaterialTheme.typography.labelSmall, color = RoseError, fontWeight = FontWeight.Bold)
                                                Text(expDisplay, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                            Text("+ خرج", style = MaterialTheme.typography.labelSmall, color = RoseError, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    GlassSurface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable {
                                                initialQuickFinanceType = "INCOME"
                                                showQuickFinanceDialog = true
                                            },
                                        shape = RoundedCornerShape(14.dp),
                                        accentBorderColor = EmeraldSuccess
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("درآمد امروز", style = MaterialTheme.typography.labelSmall, color = EmeraldSuccess, fontWeight = FontWeight.Bold)
                                                Text(incDisplay, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                            Text("+ درآمد", style = MaterialTheme.typography.labelSmall, color = EmeraldSuccess, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        // Subtle Drawer Grabber Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { financeSectionExpanded = !financeSectionExpanded }
                                .padding(bottom = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(34.dp)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(EmeraldSuccess.copy(alpha = 0.35f))
                            )
                        }
                    }
                }
            }

            if (financeSectionExpanded) {
                // Debts & Receivables (بدهکاری و طلبکاری) Collapsible Glass Card
                item {
                    val debtsArrowRotation by animateFloatAsState(
                        targetValue = if (debtsDrawerExpanded) 180f else 0f,
                        animationSpec = GlassMotion.FluidSpring,
                        label = "debts_drawer_arrow"
                    )
                    LiquidGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_debts_receivables_card"),
                        shape = RoundedCornerShape(22.dp),
                        accentBorderColor = SkyInfo.copy(alpha = 0.55f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { debtsDrawerExpanded = !debtsDrawerExpanded },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "🤝 بدهکاری و طلبکاری",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = SkyInfo.copy(alpha = 0.16f),
                                        border = BorderStroke(1.dp, SkyInfo.copy(alpha = 0.45f))
                                    ) {
                                        Text(
                                            text = "${PersianDateAndUnitUtils.toPersianDigits(activeIous.size.toString())} باز",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SkyInfo,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = SkyInfo.copy(alpha = 0.18f),
                                        border = BorderStroke(1.dp, SkyInfo.copy(alpha = 0.55f)),
                                        modifier = Modifier
                                            .clickable {
                                                WorldClassOsUtils.HapticSignature.crispTick(context)
                                                initialQuickFinanceType = "RECEIVABLE"
                                                showQuickFinanceDialog = true
                                            }
                                            .testTag("home_add_debt_receivable_button")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = null,
                                                tint = SkyInfo,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "ثبت بدهی / طلب",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SkyInfo,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = CircleShape,
                                        color = SkyInfo.copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, SkyInfo.copy(alpha = 0.45f)),
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clickable { debtsDrawerExpanded = !debtsDrawerExpanded }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowDown,
                                                contentDescription = if (debtsDrawerExpanded) "بستن" else "باز کردن",
                                                tint = SkyInfo,
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .rotate(debtsArrowRotation)
                                            )
                                        }
                                    }
                                }
                            }

                            // Dual Summary Glass Boxes: طلب من (Receivables) | بدهی من (Debts)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                GlassSurface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            WorldClassOsUtils.HapticSignature.crispTick(context)
                                            initialQuickFinanceType = "RECEIVABLE"
                                            showQuickFinanceDialog = true
                                        }
                                        .testTag("home_receivable_summary_box"),
                                    shape = RoundedCornerShape(14.dp),
                                    accentBorderColor = SkyInfo
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("طلب من", style = MaterialTheme.typography.labelSmall, color = SkyInfo, fontWeight = FontWeight.Bold)
                                            Text("${PersianDateAndUnitUtils.toPersianDigits(activeReceivables.size.toString())} مورد", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text(
                                            text = receivableDisplay,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                GlassSurface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            WorldClassOsUtils.HapticSignature.crispTick(context)
                                            initialQuickFinanceType = "DEBT"
                                            showQuickFinanceDialog = true
                                        }
                                        .testTag("home_debt_summary_box"),
                                    shape = RoundedCornerShape(14.dp),
                                    accentBorderColor = AmberWarning
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("بدهی من", style = MaterialTheme.typography.labelSmall, color = AmberWarning, fontWeight = FontWeight.Bold)
                                            Text("${PersianDateAndUnitUtils.toPersianDigits(activeDebts.size.toString())} مورد", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text(
                                            text = debtDisplay,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            AnimatedVisibility(visible = debtsDrawerExpanded) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (activeIous.isEmpty()) {
                                        Text(
                                            text = "بدهکاری یا طلبکاری بازی ثبت نشده است.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        activeIous.forEach { item ->
                                            val isRec = item.isReceivable
                                            val itemAccent = if (isRec) SkyInfo else AmberWarning
                                            val cleanNote = item.cleanNotes
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(itemAccent.copy(alpha = 0.09f))
                                                    .border(
                                                        1.dp,
                                                        itemAccent.copy(alpha = 0.35f),
                                                        RoundedCornerShape(12.dp)
                                                    )
                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(
                                                    modifier = Modifier.weight(1f),
                                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = itemAccent.copy(alpha = 0.2f)
                                                        ) {
                                                            Text(
                                                                text = if (isRec) "طلب" else "بدهی",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = itemAccent,
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                        Text(
                                                            text = item.title,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    Text(
                                                        text = PersianDateAndUnitUtils.formatCurrencyAmount(item.amount, item.currency) +
                                                            if (cleanNote.isNotBlank()) " • $cleanNote" else "",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = itemAccent
                                                    )
                                                }

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(10.dp),
                                                        color = EmeraldSuccess.copy(alpha = 0.2f),
                                                        border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.55f)),
                                                        modifier = Modifier
                                                            .clickable {
                                                                WorldClassOsUtils.HapticSignature.doublePulse(context)
                                                                onSettleDebtOrReceivable(item)
                                                            }
                                                            .testTag("settle_iou_button_${item.id}")
                                                    ) {
                                                        Text(
                                                            text = "تسویه شد ✓",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = EmeraldSuccess,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                                        )
                                                    }

                                                    IconButton(
                                                        onClick = { onDeleteExpense(item) },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "حذف",
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (settledIous.isNotEmpty()) {
                                        Text(
                                            text = if (showSettledIous) {
                                                "بستن تسویه‌شده‌ها ▲"
                                            } else {
                                                "مشاهده تسویه‌شده‌ها (${PersianDateAndUnitUtils.toPersianDigits(settledIous.size.toString())}) ▼"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .clickable { showSettledIous = !showSettledIous }
                                                .padding(top = 2.dp)
                                        )
                                        AnimatedVisibility(visible = showSettledIous) {
                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                settledIous.take(5).forEach { settled ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "✓ ${settled.title} (${if (settled.isReceivable) "طلب" else "بدهی"} تسویه‌شده)",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                        Text(
                                                            text = PersianDateAndUnitUtils.formatCurrencyAmount(settled.amount, settled.currency),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = EmeraldSuccess
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
                }

                // Today's Recent Financial Transactions Card
                item {
                    LiquidGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_today_transactions_card"),
                        shape = RoundedCornerShape(22.dp),
                        accentBorderColor = EmeraldSuccess.copy(alpha = 0.4f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "💳 تراکنش‌های امروز (${PersianDateAndUnitUtils.toPersianDigits(todayAllFinanceEntries.size.toString())})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "همه ←",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = EmeraldSuccess,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { onNavigateToSection("DAILY_LOG") }
                                )
                            }

                            if (todayAllFinanceEntries.isEmpty()) {
                                Text(
                                    text = "امروز تراکنشی ثبت نشده است.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                todayAllFinanceEntries.take(5).forEach { tx ->
                                    val txColor = when {
                                        tx.isReceivable -> SkyInfo
                                        tx.isDebt -> AmberWarning
                                        tx.isIncome -> EmeraldSuccess
                                        else -> RoseError
                                    }
                                    val txTypeLabel = when {
                                        tx.isReceivable -> if (tx.isSettled) "طلب (تسویه‌شده)" else "طلبکاری"
                                        tx.isDebt -> if (tx.isSettled) "بدهی (تسویه‌شده)" else "بدهکاری"
                                        tx.isIncome -> "درآمد"
                                        else -> "خرج"
                                    }
                                    val signPrefix = when {
                                        tx.isIncome || tx.isReceivable -> "+"
                                        else -> "-"
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(txColor.copy(alpha = 0.08f))
                                            .padding(horizontal = 10.dp, vertical = 7.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = tx.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = txTypeLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "$signPrefix ${PersianDateAndUnitUtils.formatCurrencyAmount(tx.amount, tx.currency)}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = txColor
                                            )
                                            IconButton(
                                                onClick = { onDeleteExpense(tx) },
                                                modifier = Modifier.size(26.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "حذف تراکنش",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Visual Monthly Expenses Distribution GlassCard (Inside Finance Section)
                item {
                    MonthlyVisualExpensesGlassCard(
                        expenses = expenses,
                        defaultCurrency = prefs.defaultCurrency,
                        onNavigateToExpenses = { onNavigateToSection("DAILY_LOG") }
                    )
                }
            } // end if (financeSectionExpanded)
        }

        // =========================================================================================
        // SECTION 2: 🎯 بخش اهداف و برنامه‌های روزانه (GOALS & PRODUCTIVITY SECTION)
        // =========================================================================================
        if (showGoalsDomain) {
            item {
                val goalsArrowRotation by animateFloatAsState(
                    targetValue = if (goalsSectionExpanded) 180f else 0f,
                    animationSpec = GlassMotion.FluidSpring,
                    label = "goals_section_arrow"
                )
                LiquidGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_goals_section_header"),
                    shape = RoundedCornerShape(22.dp),
                    accentBorderColor = ElectricTeal.copy(alpha = 0.65f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("today_progress_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { goalsSectionExpanded = !goalsSectionExpanded }
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            ElectricTeal.copy(alpha = 0.16f),
                                            MutedViolet.copy(alpha = 0.08f),
                                            Color.Transparent
                                        )
                                    )
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "🎯 اهداف امروز",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = ElectricTeal.copy(alpha = 0.2f),
                                        border = BorderStroke(1.dp, ElectricTeal.copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = "${PersianDateAndUnitUtils.toPersianDigits(overallGoalsScorePercent.toString())}٪",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ElectricTeal,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "کارها: ${PersianDateAndUnitUtils.toPersianDigits(completedTodayCount.toString())}/${PersianDateAndUnitUtils.toPersianDigits((completedTodayCount + pendingTodayTasks.size).toString())}  •  عادت‌ها: ${PersianDateAndUnitUtils.toPersianDigits(completedHabitsToday.size.toString())}/${PersianDateAndUnitUtils.toPersianDigits(activeHabits.size.toString())}  •  آب: ${PersianDateAndUnitUtils.toPersianDigits(todayWaterMl.toString())}ml",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = ElectricTeal.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, ElectricTeal.copy(alpha = 0.6f)),
                                    modifier = Modifier
                                        .clickable {
                                            WorldClassOsUtils.HapticSignature.crispTick(context)
                                            showQuickGoalTaskDialog = true
                                        }
                                        .testTag("home_quick_add_goal_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = ElectricTeal,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = "هدف جدید",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = ElectricTeal,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = ElectricTeal.copy(alpha = 0.18f),
                                    border = BorderStroke(1.dp, ElectricTeal.copy(alpha = 0.55f)),
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clickable { goalsSectionExpanded = !goalsSectionExpanded }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = if (goalsSectionExpanded) "بستن بخش اهداف" else "باز کردن بخش اهداف",
                                            tint = ElectricTeal,
                                            modifier = Modifier
                                                .size(18.dp)
                                                .rotate(goalsArrowRotation)
                                        )
                                    }
                                }
                            }
                        }

                        // Integrated Progress Bar inside Goals Header (No duplicate card needed)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .height(8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF101813))
                        ) {
                            val progressFraction = (overallGoalsScorePercent / 100f).coerceIn(0.04f, 1f)
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progressFraction)
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(ElectricTeal, EmeraldSuccess)
                                        )
                                    )
                            )
                        }

                        // Subtle Drawer Grabber Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { goalsSectionExpanded = !goalsSectionExpanded }
                                .padding(bottom = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(34.dp)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(ElectricTeal.copy(alpha = 0.35f))
                            )
                        }
                    }
                }
            }

            if (goalsSectionExpanded) {
                // Interactive Jalali Calendar Heatmap when CALENDAR filter is selected
                if (selectedDashboardFilter == "CALENDAR") {
                    item {
                        JalaliCalendarHeatmapCard(
                            todayEpochDay = todayEpochDay,
                            defaultCurrency = prefs.defaultCurrency,
                            tasks = tasks,
                            expenses = expenses,
                            consumption = consumption,
                            habitRecords = habitRecords,
                            onQuickCreateTaskForDay = onQuickCreateTaskForDay,
                            onCompleteTask = onCompleteTask
                        )
                    }
                }

                // Priority Tasks of Today
                item {
                    PriorityTasksSectionCard(
                        pendingTodayTasks = pendingTodayTasks,
                        onNavigateToSection = onNavigateToSection,
                        onCompleteTask = onCompleteTask,
                        onStartFocusTimer = onStartFocusTimer
                    )
                }

                // Interactive Habit Tracking Glass Module
                item {
                    HabitTrackingGlassModule(
                        habits = habits,
                        habitRecords = habitRecords,
                        todayEpochDay = todayEpochDay,
                        weekStartDay = prefs.weekStartDay,
                        onToggleHabitDay = onToggleHabitDay,
                        onNavigateToHabits = { onNavigateToSection("HABITS") }
                    )
                }

                // Daily Wellness & Energy Goals Cluster (Water, Habits Score, Sleep, Exercise + Quick Water/Coffee Actions)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AnimatedWaterWaveBox(
                                progress = (todayWaterMl.toFloat() / prefs.dailyWaterTargetMl.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(22.dp))
                            ) {
                                DashboardMetricCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    title = "هدف آب امروز",
                                    primaryValue = "${PersianDateAndUnitUtils.toPersianDigits(todayWaterMl.toString())} ml",
                                    subtitle = "هدف: ${PersianDateAndUnitUtils.toPersianDigits(prefs.dailyWaterTargetMl.toString())} ml",
                                    progress = (todayWaterMl.toFloat() / prefs.dailyWaterTargetMl.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f),
                                    accentColor = SkyInfo,
                                    icon = Icons.Default.LocalDrink,
                                    onClick = { onNavigateToSection("DAILY_LOG") }
                                )
                            }

                            DashboardMetricCard(
                                modifier = Modifier.weight(1f),
                                title = "هدف ورزش امروز",
                                primaryValue = "${PersianDateAndUnitUtils.toPersianDigits(todayExerciseMinutes.toString())} دقیقه",
                                subtitle = "هدف: ${PersianDateAndUnitUtils.toPersianDigits(prefs.dailyExerciseTargetMinutes.toString())} دقیقه",
                                progress = (todayExerciseMinutes.toFloat() / prefs.dailyExerciseTargetMinutes.coerceAtLeast(1)).coerceIn(0f, 1f),
                                accentColor = MaterialTheme.colorScheme.primary,
                                icon = Icons.AutoMirrored.Filled.DirectionsRun,
                                onClick = { onNavigateToSection("DAILY_LOG") }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
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

                            DashboardMetricCard(
                                modifier = Modifier.weight(1f),
                                title = "امتیاز عادت‌ها",
                                primaryValue = "${PersianDateAndUnitUtils.toPersianDigits(habitScorePercent.toString())}٪",
                                subtitle = "${PersianDateAndUnitUtils.toPersianDigits(completedHabitsToday.size.toString())} از ${PersianDateAndUnitUtils.toPersianDigits(activeHabits.size.toString())} عادت",
                                progress = habitScorePercent / 100f,
                                accentColor = ElectricTeal,
                                icon = Icons.Default.CheckCircle,
                                onClick = { onNavigateToSection("HABITS") }
                            )
                        }

                        // Quick Wellness Chips (+250ml Water, +1 Coffee)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AssistChip(
                                onClick = {
                                    WorldClassOsUtils.HapticSignature.crispTick(context)
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onQuickAddWater(250.0)
                                },
                                leadingIcon = { Icon(Icons.Default.LocalDrink, contentDescription = null, tint = SkyInfo, modifier = Modifier.size(16.dp)) },
                                label = { Text("+۲۵۰ ml آب", style = MaterialTheme.typography.labelMedium) },
                                modifier = Modifier.testTag("quick_add_water_250")
                            )
                            AssistChip(
                                onClick = {
                                    WorldClassOsUtils.HapticSignature.crispTick(context)
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onQuickAddCoffee()
                                },
                                leadingIcon = { Icon(Icons.Default.Coffee, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(16.dp)) },
                                label = { Text("+۱ فنجان قهوه (${PersianDateAndUnitUtils.toPersianDigits(todayCoffeeCount.toString())})", style = MaterialTheme.typography.labelMedium) }
                            )
                        }
                    }
                }

                // Jalali Calendar in ALL mode (placed below core tasks/habits so it doesn't push actions down)
                if (selectedDashboardFilter == "ALL") {
                    item {
                        JalaliCalendarHeatmapCard(
                            todayEpochDay = todayEpochDay,
                            defaultCurrency = prefs.defaultCurrency,
                            tasks = tasks,
                            expenses = expenses,
                            consumption = consumption,
                            habitRecords = habitRecords,
                            onQuickCreateTaskForDay = onQuickCreateTaskForDay,
                            onCompleteTask = onCompleteTask
                        )
                    }
                }

                if (currentLayoutStyle != AppLayoutStyle.MINIMAL_ZEN) {
                    item {
                        WeeklyFocusGlassChartCard(
                            productivityProgress = productivityProgress,
                            onNavigateToSection = onNavigateToSection
                        )
                    }
                }

                if (upcomingReminders.isNotEmpty()) {
                    item {
                        LiquidGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            accentBorderColor = AmberWarning.copy(alpha = 0.45f)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("یادآورهای پیش‌رو", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
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
            } // end if (goalsSectionExpanded)
        }

        // 3. Recent Activity Timeline
        if (currentLayoutStyle != AppLayoutStyle.MINIMAL_ZEN && selectedDashboardFilter == "ALL") {
            item {
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    accentBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
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
                                Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("آخرین فعالیت‌ها", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Text(
                                text = "همه ←",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { onNavigateToSection("HISTORY") }
                            )
                        }

                        if (activityEvents.isEmpty()) {
                            Text(
                                text = "هنوز رویدادی ثبت نشده است.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            activityEvents.take(4).forEach { evt ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        modifier = Modifier.size(7.dp)
                                    ) {}
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = evt.humanSummary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun WeeklyFocusGlassChartCard(
    productivityProgress: Float,
    onNavigateToSection: (String) -> Unit
) {
    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("weekly_focus_glass_chart_card"),
        shape = RoundedCornerShape(26.dp),
        accentBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
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
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = "روند تمرکز هفتگی",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "گزارش‌ها ←",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onNavigateToSection("REPORTS") }
                )
            }

            // 7 Days 3D Glass Pillars Chart (شنبه تا جمعه)
            val dayData = remember(productivityProgress) {
                listOf(
                    Triple("ش", 0.65f, "۶۵٪"),
                    Triple("ی", 0.40f, "۴۰٪"),
                    Triple("د", 0.55f, "۵۵٪"),
                    Triple("س", 0.70f, "۷۰٪"),
                    Triple("چ", 0.60f, "۶۰٪"),
                    Triple("پ", productivityProgress.coerceIn(0.25f, 1f), "${(productivityProgress * 100).toInt()}٪"),
                    Triple("ج", 0.35f, "۳۵٪")
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                dayData.forEachIndexed { index, (dayLabel, ratio, tag) ->
                    val isToday = index == 5
                    val pillarHeight = (ratio * 74).coerceIn(22f, 78f).dp

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = PersianDateAndUnitUtils.toPersianDigits(tag),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isToday) FontWeight.Black else FontWeight.Normal
                        )

                        Box(
                            modifier = Modifier
                                .width(26.dp)
                                .height(pillarHeight)
                                .clip(RoundedCornerShape(50))
                                .border(
                                    width = if (isToday) 1.2.dp else 1.dp,
                                    brush = GlassmorphismDefaults.specularBorderBrush(
                                        accentColor = if (isToday) Color.White else null,
                                        topIntensity = if (isToday) 0.8f else 0.4f
                                    ),
                                    shape = RoundedCornerShape(50)
                                )
                                .background(
                                    if (isToday) {
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0xFFD8FA70),
                                                Color(0xFFA2EE23),
                                                Color(0xFF569414)
                                            )
                                        )
                                    } else {
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0x3D2E3E28),
                                                Color(0x1F142012)
                                            )
                                        )
                                    }
                                )
                        )

                        Text(
                            text = dayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

private val SubtleRingTrack = androidx.compose.ui.graphics.Color(0xFF283552)

@Composable
private fun PriorityTasksSectionCard(
    pendingTodayTasks: List<Task>,
    onNavigateToSection: (String) -> Unit,
    onCompleteTask: (Task) -> Unit,
    onStartFocusTimer: (Task) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        accentBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "🎯 اولویت‌های امروز",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = PersianDateAndUnitUtils.toPersianDigits(pendingTodayTasks.size.toString()),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "مشاهده همه ←",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigateToSection("TASKS") }
                )
            }

            if (pendingTodayTasks.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = EmeraldSuccess.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = EmeraldSuccess.copy(alpha = 0.22f),
                            border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.55f)),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = EmeraldSuccess,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "همه کارهای امروز با موفقیت تیک خورد! ✦",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "مومنتوم امروزت عالیه؛ می‌تونی استراحت کنی یا کار جدیدت رو با فرمان صوتی ثبت کنی.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                pendingTodayTasks.take(4).forEach { task ->
                    val taskCategory = TaskCategory.fromCode(task.category)
                    val priorityAccent = when (task.priority) {
                        "CRITICAL" -> RoseError
                        "HIGH" -> AmberWarning
                        "LOW" -> SkyInfo
                        else -> taskCategory.color
                    }
                    LiquidGlassCard(
                        shape = RoundedCornerShape(14.dp),
                        accentBorderColor = priorityAccent,
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
                                    tint = taskCategory.color
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = task.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    TaskCategoryIndicator(category = taskCategory)
                                }
                                Text(
                                    text = buildString {
                                        append(taskCategory.titleFa)
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
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                modifier = Modifier.size(34.dp)
                            ) {
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
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            PriorityBadge(priority = task.priority)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun UnifiedQuickCommandAndBudgetCard(
    safeToSpend: SafeToSpendSummary,
    customItems: List<CustomItem>,
    onNavigateToSection: (String) -> Unit,
    onOpenVoiceCapture: () -> Unit,
    onQuickAddPresetItem: (CustomItem) -> Unit,
    onQuickAddWater: (Double) -> Unit,
    onQuickAddCoffee: () -> Unit,
    onManagePresets: () -> Unit,
    onEditPreset: (CustomItem) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val statusColor = if (safeToSpend.isOverBudgetToday) RoseError else EmeraldSuccess
    val dailyAllowanceBase = (safeToSpend.remainingSafeToSpendToday + safeToSpend.spentToday).coerceAtLeast(1.0)
    val spentRatio = (safeToSpend.spentToday / dailyAllowanceBase).toFloat().coerceIn(0f, 1f)

    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        accentBorderColor = statusColor.copy(alpha = 0.45f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Upper Section: Safe-to-Spend Allowance Bar (TestTag: home_safe_to_spend_card)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(statusColor.copy(alpha = 0.08f))
                    .border(
                        1.dp,
                        GlassmorphismDefaults.specularBorderBrush(statusColor, topIntensity = 0.35f),
                        RoundedCornerShape(16.dp)
                    )
                    .clickable { onNavigateToSection("DAILY_LOG") }
                    .testTag("home_safe_to_spend_card")
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = statusColor.copy(alpha = 0.22f),
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Payments,
                                        contentDescription = null,
                                        tint = statusColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Text(
                                text = "سهم مجاز خرج امروز (${safeToSpend.jalaliMonthName} • ${PersianDateAndUnitUtils.toPersianDigits(safeToSpend.remainingDaysIncludingToday.toString())} روز مانده)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = statusColor.copy(alpha = 0.18f),
                            border = BorderStroke(
                                1.dp,
                                GlassmorphismDefaults.specularBorderBrush(statusColor, topIntensity = 0.5f)
                            )
                        ) {
                            Text(
                                text = if (safeToSpend.isOverBudgetToday) "عبور از سقف امروز" else "در محدوده امن",
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = PersianDateAndUnitUtils.formatCurrencyAmount(
                                safeToSpend.remainingSafeToSpendToday,
                                safeToSpend.currency
                            ),
                            style = MaterialTheme.typography.titleLarge,
                            color = statusColor,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "خرج امروز: ${PersianDateAndUnitUtils.formatCurrencyAmount(safeToSpend.spentToday, safeToSpend.currency)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    LinearProgressIndicator(
                        progress = { (1f - spentRatio).coerceIn(0.04f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(50)),
                        color = statusColor,
                        trackColor = MaterialTheme.colorScheme.surface
                    )
                }
            }

            // Lower Section: Quick Actions & Voice Capture Dock
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ثبت سریع و اقدامات یک‌لمسی",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .clickable { onOpenVoiceCapture() }
                                .testTag("dashboard_voice_button")
                        ) {
                            Text(
                                text = "🎤 فرمان صوتی",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .size(28.dp)
                                .clickable { onManagePresets() }
                                .testTag("manage_quick_presets_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "مدیریت آیتم‌های ثبت سریع",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    customItems.forEach { preset ->
                        GlassSurface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF141F1A),
                            alpha = 0.65f,
                            accentBorderColor = ElectricTeal,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .combinedClickable(
                                    onClick = {
                                        WorldClassOsUtils.HapticSignature.doublePulse(context)
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onQuickAddPresetItem(preset)
                                    },
                                    onLongClick = {
                                        WorldClassOsUtils.HapticSignature.heavyImpact(context)
                                        onEditPreset(preset)
                                    }
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = if (preset.category == "COFFEE") Icons.Default.Coffee else Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = if (preset.defaultPrice > 0.0) {
                                        "+ ${preset.name} • ${PersianDateAndUnitUtils.formatCurrencyAmount(preset.defaultPrice, preset.defaultCurrency)}"
                                    } else {
                                        "+ ${preset.name}"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    AssistChip(
                        onClick = {
                            WorldClassOsUtils.HapticSignature.crispTick(context)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onQuickAddWater(250.0)
                        },
                        leadingIcon = { Icon(Icons.Default.LocalDrink, contentDescription = null, tint = SkyInfo, modifier = Modifier.size(16.dp)) },
                        label = { Text("+۲۵۰ ml آب", style = MaterialTheme.typography.labelMedium) },
                        modifier = Modifier.testTag("quick_add_water_250")
                    )
                    AssistChip(
                        onClick = {
                            WorldClassOsUtils.HapticSignature.crispTick(context)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onQuickAddCoffee()
                        },
                        leadingIcon = { Icon(Icons.Default.Coffee, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(16.dp)) },
                        label = { Text("+۱ فنجان قهوه", style = MaterialTheme.typography.labelMedium) }
                    )
                    AssistChip(
                        onClick = { onNavigateToSection("TASKS") },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)) },
                        label = { Text("تسک جدید", style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactKpiCell(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    accentColor: androidx.compose.ui.graphics.Color
) {
    GlassSurface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF141F1A),
        alpha = 0.65f,
        accentBorderColor = accentColor
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = accentColor,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun KanbanDomainColumn(
    title: String,
    badge: String,
    accentColor: androidx.compose.ui.graphics.Color,
    content: @Composable () -> Unit
) {
    LiquidGlassCard(
        modifier = Modifier.width(264.dp),
        shape = RoundedCornerShape(22.dp),
        accentBorderColor = accentColor.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = accentColor.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            content()
        }
    }
}

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
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = color.copy(alpha = 0.14f)
        ) {
            Text(
                text = PersianDateAndUnitUtils.toPersianDigits(count.toString()),
                style = MaterialTheme.typography.labelLarge,
                color = color,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
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
    LiquidGlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        accentBorderColor = accentColor.copy(alpha = 0.45f),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.10f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = accentColor.copy(alpha = 0.16f),
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = title,
                                tint = accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
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
                    progress = { progress.coerceIn(0.03f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(50)),
                    color = accentColor,
                    trackColor = MaterialTheme.colorScheme.surface
                )
            }
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
    LiquidGlassPill(
        shape = RoundedCornerShape(8.dp),
        accentGlow = color
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
