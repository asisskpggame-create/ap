package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.GlassmorphismDefaults
import com.example.ui.theme.LiquidGlassCard
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
import com.example.ui.components.HabitTrackingGlassModule
import com.example.ui.components.MonthlyVisualExpensesGlassCard
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
    var showQuickLogManageSheet by rememberSaveable { mutableStateOf(false) }
    var editingPreset by remember { mutableStateOf<CustomItem?>(null) }
    var showNewPresetDialog by remember { mutableStateOf(false) }

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
    val todayBeverageCount = todayConsumption.filter { it.category == "TEA" || it.category == "BEVERAGE" }.sumOf { it.quantity }.toInt()

    // Expenses today grouped strictly by currency
    val todayExpensesByCurrency = PersianDateAndUnitUtils.aggregateExpensesByCurrency(
        expenses.filter { it.localEpochDay == todayEpochDay }
    )
    val expDisplay = if (todayExpensesByCurrency.isEmpty()) {
        "۰"
    } else {
        todayExpensesByCurrency.entries.joinToString(" | ") {
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

    // Gemini daily insight generated from real data
    val dailyInsightText = buildString {
        if (activeTasks.isEmpty() && todayWaterMl == 0 && todayExpensesByCurrency.isEmpty()) {
            append("امروز آماده‌ست تا بترکونی! اولین کار یا مصرف امروزت رو با یک لمس یا فرمان صوتی ثبت کن.")
        } else {
            append("امروز ${PersianDateAndUnitUtils.toPersianDigits(completedTodayCount.toString())} تسک انجام شده و ${PersianDateAndUnitUtils.toPersianDigits(pendingTodayTasks.size.toString())} تسک در انتظار است. ")
            if (overdueCount > 0) {
                append("${PersianDateAndUnitUtils.toPersianDigits(overdueCount.toString())} تسک عقب‌افتاده در اولویت قرار دارد. ")
            }
            if (todayWaterMl < prefs.dailyWaterTargetMl / 2) {
                append("مصرف آب امروز (${PersianDateAndUnitUtils.toPersianDigits(todayWaterMl.toString())} ml) کمتر از نیمی از هدف روزانه است.")
            } else {
                append("روند هیدراتاسیون امروز عالی پیش می‌رود.")
            }
        }
    }

    // Modal Bottom Sheet for managing Quick-Log items (separate from 1-tap logging so no accidental taps occur)
    if (showQuickLogManageSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQuickLogManageSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
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

    // Dialog for editing/creating a preset (opened via Long-Press on a quick-log chip or from the Manage Sheet)
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
                        label = { Text("عنوان دکمه") },
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
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmedName = dialogName.trim()
                        if (trimmedName.isNotEmpty()) {
                            val parsedPrice = PersianDateAndUnitUtils.toEnglishDigits(dialogPriceText)
                                .replace(",", "")
                                .toDoubleOrNull()
                                ?.coerceAtLeast(0.0) ?: 0.0
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

    val itemVerticalSpacing = when (currentLayoutStyle) {
        AppLayoutStyle.COMPACT_PRO -> 10.dp
        AppLayoutStyle.MINIMAL_ZEN -> 18.dp
        else -> 14.dp
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_dashboard_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(itemVerticalSpacing)
    ) {
        // 1. Header + Theme/Layout Studio Button + Local/Cloud Sync Pill
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = formattedDate,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "مومنتوم امروز ✦",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$greeting، ${userEmail?.substringBefore("@") ?: "کاربر"}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .clickable {
                                    WorldClassOsUtils.HapticSignature.crispTick(context)
                                    showThemeAndLayoutStudio = !showThemeAndLayoutStudio
                                }
                                .testTag("toggle_theme_layout_studio_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = "انتخاب تم و چیدمان",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "تم و چیدمان",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = "پردازش روی گوشی و انتقال ۶ ساعته",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (userEmail == null) "گوشی•۶س" else "ابری•۶س",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Expandable Interactive Theme & Layout Studio Panel
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "انتخاب تم رنگی (۶ پوسته استاندارد)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = currentThemePalette.titleFa,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
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

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.GridView, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "نوع چیدمان داشبورد (۴ معماری نمایش)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = currentLayoutStyle.titleFa,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

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

                            Text(
                                text = currentLayoutStyle.subtitleFa,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 2. Layout-Specific Dashboard Architecture
        when (currentLayoutStyle) {
            AppLayoutStyle.COMPACT_PRO -> {
                // RAYCAST COMPACT PRO: High-density 4-KPI strip + Priority Tasks promoted immediately to top + 3-column mini metrics
                item {
                    LiquidGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        accentBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CompactKpiCell(Modifier.weight(1f), "بهره‌وری", "${PersianDateAndUnitUtils.toPersianDigits((productivityProgress * 100).toInt().toString())}٪", MaterialTheme.colorScheme.primary)
                            CompactKpiCell(Modifier.weight(1f), "در انتظار", PersianDateAndUnitUtils.toPersianDigits(pendingTodayTasks.size.toString()), SkyInfo)
                            CompactKpiCell(Modifier.weight(1f), "آب امروز", "${PersianDateAndUnitUtils.toPersianDigits(todayWaterMl.toString())}ml", SkyInfo)
                            CompactKpiCell(Modifier.weight(1f), "عادت‌ها", "${PersianDateAndUnitUtils.toPersianDigits(habitScorePercent.toString())}٪", MutedViolet)
                        }
                    }
                }

                item {
                    PriorityTasksSectionCard(
                        pendingTodayTasks = pendingTodayTasks,
                        onNavigateToSection = onNavigateToSection,
                        onCompleteTask = onCompleteTask,
                        onStartFocusTimer = onStartFocusTimer
                    )
                }

                item {
                    UnifiedQuickCommandAndBudgetCard(
                        safeToSpend = safeToSpend,
                        customItems = customItems,
                        onNavigateToSection = onNavigateToSection,
                        onOpenVoiceCapture = onOpenVoiceCapture,
                        onQuickAddPresetItem = onQuickAddPresetItem,
                        onQuickAddWater = onQuickAddWater,
                        onQuickAddCoffee = onQuickAddCoffee,
                        onManagePresets = { showQuickLogManageSheet = true },
                        onEditPreset = { editingPreset = it }
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DashboardMetricCard(
                            modifier = Modifier.weight(1f),
                            title = "هزینه امروز",
                            primaryValue = expDisplay,
                            subtitle = "تفکیک ارز",
                            progress = if (todayExpensesByCurrency.isEmpty()) 0f else 0.5f,
                            accentColor = EmeraldSuccess,
                            icon = Icons.Default.Payments,
                            onClick = { onNavigateToSection("DAILY_LOG") }
                        )
                        DashboardMetricCard(
                            modifier = Modifier.weight(1f),
                            title = "خواب",
                            primaryValue = sleepText,
                            subtitle = "ثبت روزانه",
                            progress = ((latestSleep?.durationMinutes ?: 0).toFloat() / prefs.dailySleepTargetMinutes.coerceAtLeast(1)).coerceIn(0f, 1f),
                            accentColor = MutedViolet,
                            icon = Icons.Default.Bedtime,
                            onClick = { onNavigateToSection("DAILY_LOG") }
                        )
                        DashboardMetricCard(
                            modifier = Modifier.weight(1f),
                            title = "ورزش",
                            primaryValue = "${PersianDateAndUnitUtils.toPersianDigits(todayExerciseMinutes.toString())}m",
                            subtitle = "هدف: ${PersianDateAndUnitUtils.toPersianDigits(prefs.dailyExerciseTargetMinutes.toString())}m",
                            progress = (todayExerciseMinutes.toFloat() / prefs.dailyExerciseTargetMinutes.coerceAtLeast(1)).coerceIn(0f, 1f),
                            accentColor = MaterialTheme.colorScheme.primary,
                            icon = Icons.AutoMirrored.Filled.DirectionsRun,
                            onClick = { onNavigateToSection("DAILY_LOG") }
                        )
                    }
                }
            }

            AppLayoutStyle.MINIMAL_ZEN -> {
                // THINGS 3 ZEN: Clean, breathable focus mode — Priority Tasks + Unified Quick & Budget dock + subtle wellness summary
                item {
                    PriorityTasksSectionCard(
                        pendingTodayTasks = pendingTodayTasks,
                        onNavigateToSection = onNavigateToSection,
                        onCompleteTask = onCompleteTask,
                        onStartFocusTimer = onStartFocusTimer
                    )
                }

                item {
                    UnifiedQuickCommandAndBudgetCard(
                        safeToSpend = safeToSpend,
                        customItems = customItems,
                        onNavigateToSection = onNavigateToSection,
                        onOpenVoiceCapture = onOpenVoiceCapture,
                        onQuickAddPresetItem = onQuickAddPresetItem,
                        onQuickAddWater = onQuickAddWater,
                        onQuickAddCoffee = onQuickAddCoffee,
                        onManagePresets = { showQuickLogManageSheet = true },
                        onEditPreset = { editingPreset = it }
                    )
                }

                item {
                    LiquidGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        accentBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "💧 آب: ${PersianDateAndUnitUtils.toPersianDigits(todayWaterMl.toString())}ml   •   ☕ قهوه: ${PersianDateAndUnitUtils.toPersianDigits(todayCoffeeCount.toString())}   •   ✅ عادت: ${PersianDateAndUnitUtils.toPersianDigits(habitScorePercent.toString())}٪",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            AppLayoutStyle.KANBAN_BOARD -> {
                // SUPERLIST KANBAN BOARD: Horizontal scrollable domain columns
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Column 1: Tasks & Focus
                        KanbanDomainColumn(
                            title = "🎯 تسک‌ها و تمرکز",
                            badge = "${PersianDateAndUnitUtils.toPersianDigits(pendingTodayTasks.size.toString())} فعال",
                            accentColor = MaterialTheme.colorScheme.primary
                        ) {
                            StatBadgeRow(label = "تکمیل‌شده امروز", count = completedTodayCount, color = EmeraldSuccess)
                            StatBadgeRow(label = "در انتظار امروز", count = pendingTodayTasks.size, color = SkyInfo)
                            StatBadgeRow(label = "عقب‌افتاده", count = overdueCount, color = if (overdueCount > 0) RoseError else MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        // Column 2: Finance & Consumption
                        KanbanDomainColumn(
                            title = "💳 مالی و مصرف روزانه",
                            badge = prefs.defaultCurrency,
                            accentColor = EmeraldSuccess
                        ) {
                            Text("خرج امروز: $expDisplay", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text("قهوه امروز: ${PersianDateAndUnitUtils.toPersianDigits(todayCoffeeCount.toString())} فنجان", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("آب مصرفی: ${PersianDateAndUnitUtils.toPersianDigits(todayWaterMl.toString())} میلی‌لیتر", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        // Column 3: Wellness & Habits
                        KanbanDomainColumn(
                            title = "⚡ سلامت و عادت‌ها",
                            badge = "${PersianDateAndUnitUtils.toPersianDigits(habitScorePercent.toString())}٪",
                            accentColor = MutedViolet
                        ) {
                            Text("خواب شبانه: $sleepText", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text("ورزش امروز: ${PersianDateAndUnitUtils.toPersianDigits(todayExerciseMinutes.toString())} دقیقه", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("عادت‌های امروز: ${PersianDateAndUnitUtils.toPersianDigits(completedHabitsToday.size.toString())} از ${PersianDateAndUnitUtils.toPersianDigits(activeHabits.size.toString())}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                item {
                    PriorityTasksSectionCard(
                        pendingTodayTasks = pendingTodayTasks,
                        onNavigateToSection = onNavigateToSection,
                        onCompleteTask = onCompleteTask,
                        onStartFocusTimer = onStartFocusTimer
                    )
                }

                item {
                    UnifiedQuickCommandAndBudgetCard(
                        safeToSpend = safeToSpend,
                        customItems = customItems,
                        onNavigateToSection = onNavigateToSection,
                        onOpenVoiceCapture = onOpenVoiceCapture,
                        onQuickAddPresetItem = onQuickAddPresetItem,
                        onQuickAddWater = onQuickAddWater,
                        onQuickAddCoffee = onQuickAddCoffee,
                        onManagePresets = { showQuickLogManageSheet = true },
                        onEditPreset = { editingPreset = it }
                    )
                }
            }

            AppLayoutStyle.BENTO_GRID -> {
                // 1. SCREENSHOT-INSPIRED LUMINOUS HERO AI BENTO CARD
                item {
                    val accentColor = MaterialTheme.colorScheme.primary
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(26.dp))
                            .border(
                                border = BorderStroke(
                                    1.2.dp,
                                    GlassmorphismDefaults.specularBorderBrush(Color.White, topIntensity = 0.65f)
                                ),
                                shape = RoundedCornerShape(26.dp)
                            )
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFD8FA70),
                                        Color(0xFFA6EE2C),
                                        Color(0xFF4C8210)
                                    )
                                )
                            )
                    ) {
                        // Top Specular Sheen Overlay
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(GlassmorphismDefaults.GlossSheenBrush)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF0F1E08),
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = Color(0xFFD8FA70),
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "دستیار هوشمند شما",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color(0xFF0A1404),
                                        fontWeight = FontWeight.Black
                                    )
                                }

                                Text(
                                    text = if (pendingTodayTasks.isNotEmpty()) {
                                        "شما ${PersianDateAndUnitUtils.toPersianDigits(pendingTodayTasks.size.toString())} تسک برای تمرکز و پیگیری امروز دارید."
                                    } else {
                                        dailyInsightText
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF1B360A),
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 20.sp
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                // Dark Frosted Capsule Pill: "پرسش از هوش مصنوعی ←"
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color(0xFF0F1E08),
                                    border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                                    modifier = Modifier
                                        .clickable {
                                            WorldClassOsUtils.HapticSignature.crispTick(context)
                                            onNavigateToSection("AI_ASSISTANT")
                                        }
                                        .testTag("ask_ai_anything_hero_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "پرسش از هوش مصنوعی ←",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Color(0xFFD8FA70),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Right Visual: 3D Holographic AI Sphere with concentric orbit rings
                            Box(
                                modifier = Modifier
                                    .size(105.dp)
                                    .padding(start = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val centerX = size.width / 2f
                                    val centerY = size.height / 2f
                                    val maxRadius = size.minDimension / 2f

                                    // Ambient Core Glow
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color(0x80D8FA70),
                                                Color(0x33A6EE2C),
                                                Color.Transparent
                                            ),
                                            center = Offset(centerX, centerY),
                                            radius = maxRadius
                                        ),
                                        radius = maxRadius,
                                        center = Offset(centerX, centerY)
                                    )

                                    // Concentric Orbit Ellipses
                                    drawOval(
                                        color = Color(0x660F1E08),
                                        topLeft = Offset(centerX - maxRadius * 0.95f, centerY - maxRadius * 0.45f),
                                        size = androidx.compose.ui.geometry.Size(maxRadius * 1.9f, maxRadius * 0.9f),
                                        style = Stroke(width = 2.5f)
                                    )

                                    drawOval(
                                        color = Color(0x40FFFFFF),
                                        topLeft = Offset(centerX - maxRadius * 0.85f, centerY - maxRadius * 0.70f),
                                        size = androidx.compose.ui.geometry.Size(maxRadius * 1.7f, maxRadius * 1.4f),
                                        style = Stroke(width = 1.8f)
                                    )

                                    // Luminous 3D AI Sphere Core
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFFFFFFFF),
                                                Color(0xFFD8FA70),
                                                Color(0xFF4C8210)
                                            ),
                                            center = Offset(centerX - maxRadius * 0.15f, centerY - maxRadius * 0.15f),
                                            radius = maxRadius * 0.45f
                                        ),
                                        radius = maxRadius * 0.38f,
                                        center = Offset(centerX, centerY)
                                    )

                                    // Specular Highlight Pin
                                    drawCircle(
                                        color = Color.White.copy(alpha = 0.9f),
                                        radius = maxRadius * 0.08f,
                                        center = Offset(centerX - maxRadius * 0.15f, centerY - maxRadius * 0.15f)
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. SCREENSHOT-INSPIRED TODAY'S PROGRESS WITH THICK 3D CYLINDER BAR
                item {
                    val accentColor = MaterialTheme.colorScheme.primary
                    LiquidGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("today_progress_card"),
                        shape = RoundedCornerShape(24.dp),
                        accentBorderColor = accentColor.copy(alpha = 0.45f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "پیشرفت امروز",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Text(
                                        text = formattedDate.take(15),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text(
                                    text = "${PersianDateAndUnitUtils.toPersianDigits(completedTodayCount.toString())} از ${PersianDateAndUnitUtils.toPersianDigits((completedTodayCount + pendingTodayTasks.size).toString())} تسک انجام شده",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Text(
                                    text = "${PersianDateAndUnitUtils.toPersianDigits((productivityProgress * 100).toInt().toString())}٪",
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Black,
                                    color = accentColor
                                )
                            }

                            // Thick 3D Cylinder Progress Bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color(0xFF101813))
                                    .border(BorderStroke(1.dp, Color(0x24FFFFFF)), RoundedCornerShape(50))
                            ) {
                                val progressFraction = productivityProgress.coerceIn(0.04f, 1f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(progressFraction)
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(
                                                    Color(0xFF70D418),
                                                    Color(0xFFCEF665)
                                                )
                                            )
                                        )
                                ) {
                                    // 3D Glass Sheen Highlight Line across the top of the cylinder
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(3.dp)
                                            .padding(horizontal = 6.dp)
                                            .background(Color.White.copy(alpha = 0.45f), RoundedCornerShape(50))
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. TODAY'S PRIORITY TASKS (Promoted directly under Today's Progress!)
                item {
                    PriorityTasksSectionCard(
                        pendingTodayTasks = pendingTodayTasks,
                        onNavigateToSection = onNavigateToSection,
                        onCompleteTask = onCompleteTask,
                        onStartFocusTimer = onStartFocusTimer
                    )
                }

                // 4. UNIFIED QUICK ACTIONS & SAFE-TO-SPEND BENTO DOCK
                item {
                    UnifiedQuickCommandAndBudgetCard(
                        safeToSpend = safeToSpend,
                        customItems = customItems,
                        onNavigateToSection = onNavigateToSection,
                        onOpenVoiceCapture = onOpenVoiceCapture,
                        onQuickAddPresetItem = onQuickAddPresetItem,
                        onQuickAddWater = onQuickAddWater,
                        onQuickAddCoffee = onQuickAddCoffee,
                        onManagePresets = { showQuickLogManageSheet = true },
                        onEditPreset = { editingPreset = it }
                    )
                }

                // 5. SCREENSHOT-INSPIRED TODAY'S FOCUS 3D GLASS PILLARS WEEKLY CHART
                item {
                    WeeklyFocusGlassChartCard(
                        productivityProgress = productivityProgress,
                        onNavigateToSection = onNavigateToSection
                    )
                }

                // 6. REFINED 2x2 WELLNESS & LIFE METRIC BENTO CLUSTER
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
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

                            DashboardMetricCard(
                                modifier = Modifier.weight(1f),
                                title = "امتیاز عادت‌ها",
                                primaryValue = "${PersianDateAndUnitUtils.toPersianDigits(habitScorePercent.toString())}٪",
                                subtitle = "${PersianDateAndUnitUtils.toPersianDigits(completedHabitsToday.size.toString())} از ${PersianDateAndUnitUtils.toPersianDigits(activeHabits.size.toString())} عادت",
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
                        }
                    }
                }
            }
        }

        // Visual Monthly Expenses Distribution GlassCard
        item {
            MonthlyVisualExpensesGlassCard(
                expenses = expenses,
                defaultCurrency = prefs.defaultCurrency,
                onNavigateToExpenses = { onNavigateToSection("DAILY_LOG") }
            )
        }

        // Interactive Habit Tracking Glass Module with Haptic Toggle
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

        // 7. Upcoming Reminders
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

        // 8. Recent Activity Timeline (shown in Bento Grid & Kanban Board)
        if (currentLayoutStyle != AppLayoutStyle.MINIMAL_ZEN) {
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
                                Text("تایم‌لاین فعالیت‌های اخیر", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
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
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
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
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun WeeklyFocusGlassChartCard(
    productivityProgress: Float,
    onNavigateToSection: (String) -> Unit
) {
    val context = LocalContext.current
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
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        text = "روند تمرکز و بهره‌وری",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "مشاهده جزئیات ←",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onNavigateToSection("REPORTS") }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "پیشرفت عالی!",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "بهره‌وری شما ۱۵٪ بیشتر از هفته گذشته است",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${PersianDateAndUnitUtils.toPersianDigits((productivityProgress * 100).toInt().toString())}٪",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // 7 Days 3D Glass Pillars Chart (شنبه تا جمعه)
            val dayData = remember(productivityProgress) {
                listOf(
                    Triple("ش", 0.65f, "+۱۰٪"),
                    Triple("ی", 0.40f, "-۱۵٪"),
                    Triple("د", 0.55f, "-۵٪"),
                    Triple("س", 0.70f, "+۵٪"),
                    Triple("چ", 0.60f, "+۲٪"),
                    Triple("پ", productivityProgress.coerceIn(0.25f, 1f), "${(productivityProgress * 100).toInt()}٪"),
                    Triple("ج", 0.35f, "-۸٪")
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                dayData.forEachIndexed { index, (dayLabel, ratio, tag) ->
                    val isToday = index == 5
                    val pillarHeight = (ratio * 80).coerceIn(24f, 85f).dp

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isToday) Color(0xFFD8FA70) else Color(0x332B3B2B),
                            border = BorderStroke(1.dp, if (isToday) Color.White else Color(0x22FFFFFF))
                        ) {
                            Text(
                                text = PersianDateAndUnitUtils.toPersianDigits(tag),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isToday) Color(0xFF0C1905) else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isToday) FontWeight.Black else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(28.dp)
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
                        ) {
                            if (isToday) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(6.dp)
                                        .padding(start = 3.dp)
                                        .background(Color.White.copy(alpha = 0.45f), RoundedCornerShape(50))
                                )
                            }
                        }

                        Text(
                            text = dayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .border(BorderStroke(1.2.dp, Color(0x66FFFFFF)), RoundedCornerShape(50))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFD8FA70),
                                Color(0xFFA4EE25)
                            )
                        )
                    )
                    .clickable {
                        WorldClassOsUtils.HapticSignature.crispTick(context)
                        onNavigateToSection("AI_ASSISTANT")
                    }
                    .testTag("boost_productivity_ai_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFF0C1905),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "✦ ارتقای بهره‌وری با هوش مصنوعی",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFF0C1905),
                        fontWeight = FontWeight.Black
                    )
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
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "✨ هیچ تسک فعالی برای امروز باقی نمانده است. با دکمه + یا فرمان صوتی تسک جدید اضافه کنید.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
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
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, priorityAccent.copy(alpha = 0.35f)),
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
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
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
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f))
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
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
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
