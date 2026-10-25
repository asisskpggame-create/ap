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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.AppLayoutStyle
import com.example.ui.theme.AppThemePalette
import com.example.ui.theme.ElectricTeal
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MutedViolet
import com.example.ui.theme.RoseError
import com.example.ui.theme.SkyInfo
import com.example.util.PersianDateAndUnitUtils
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
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$greeting، ${userEmail?.substringBefore("@") ?: "کاربر"} 👋",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.ExtraBold
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
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("theme_layout_studio_card"),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
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

        // 2. Daily Safe-to-Spend ("سهم مجاز خرج امروز") — Present in all layouts
        item {
            val statusColor = if (safeToSpend.isOverBudgetToday) RoseError else EmeraldSuccess
            val dailyAllowanceBase = (safeToSpend.remainingSafeToSpendToday + safeToSpend.spentToday).coerceAtLeast(1.0)
            val spentRatio = (safeToSpend.spentToday / dailyAllowanceBase).toFloat().coerceIn(0f, 1f)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToSection("DAILY_LOG") }
                    .testTag("home_safe_to_spend_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.45f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    statusColor.copy(alpha = 0.12f),
                                    MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = statusColor.copy(alpha = 0.18f),
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Payments,
                                            contentDescription = null,
                                            tint = statusColor,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "سهم مجاز خرج امروز (${safeToSpend.jalaliMonthName} • ${PersianDateAndUnitUtils.toPersianDigits(safeToSpend.remainingDaysIncludingToday.toString())} روز مانده)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = statusColor.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
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
                                style = MaterialTheme.typography.headlineMedium,
                                color = statusColor,
                                fontWeight = FontWeight.Bold
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
                                .height(6.dp)
                                .clip(RoundedCornerShape(50)),
                            color = statusColor,
                            trackColor = MaterialTheme.colorScheme.surface
                        )
                    }
                }
            }
        }

        // 3. Clean «ثبت سریع» Bar — 1-tap logs immediately; Long-Press or subtle Tune icon opens edit sheet (no inline edit clutter)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ثبت سریع",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
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
                                    .size(30.dp)
                                    .clickable { showQuickLogManageSheet = true }
                                    .testTag("manage_quick_presets_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = "مدیریت آیتم‌های ثبت سریع",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
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
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
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
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (preset.category == "COFFEE") Icons.Default.Coffee else Icons.Default.Payments,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (preset.defaultPrice > 0.0) {
                                            "+ ${preset.name} • ${PersianDateAndUnitUtils.formatCurrencyAmount(preset.defaultPrice, preset.defaultCurrency)}"
                                        } else {
                                            "+ ${preset.name}"
                                        },
                                        style = MaterialTheme.typography.labelLarge,
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
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) },
                            label = { Text("تسک جدید") }
                        )
                    }
                }
            }
        }

        // 4. Layout-Specific Dashboard Architecture
        when (currentLayoutStyle) {
            AppLayoutStyle.COMPACT_PRO -> {
                // RAYCAST COMPACT PRO: High-density 4-KPI strip + Priority Tasks promoted immediately to top + 3-column mini metrics
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
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
                // THINGS 3 ZEN: Clean, breathable focus mode — Priority Tasks + subtle single-line wellness summary
                item {
                    PriorityTasksSectionCard(
                        pendingTodayTasks = pendingTodayTasks,
                        onNavigateToSection = onNavigateToSection,
                        onCompleteTask = onCompleteTask,
                        onStartFocusTimer = onStartFocusTimer
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
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
            }

            AppLayoutStyle.BENTO_GRID -> {
                // SCREENSHOT-INSPIRED OLED BENTO GRID: Glowing Neon-Lime AI Assistant Card + Capsule "پیشرفت امروز" Bar + Dual-Ring Hero + 2x3 Bento Cards
                item {
                    val accentColor = MaterialTheme.colorScheme.primary
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(26.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.55f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            accentColor.copy(alpha = 0.22f),
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            accentColor.copy(alpha = 0.08f)
                                        )
                                    )
                                )
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
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = accentColor,
                                            modifier = Modifier.size(44.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.AutoAwesome,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }
                                        Column {
                                            Text(
                                                text = "دستیار هوشمند",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = accentColor,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                            Text(
                                                text = dailyInsightText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                    Surface(
                                        shape = CircleShape,
                                        color = accentColor.copy(alpha = 0.16f),
                                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clickable { onOpenVoiceCapture() }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Mic,
                                                contentDescription = "فرمان صوتی",
                                                tint = accentColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                // Thick capsule "پیشرفت امروز" progress bar inspired by the user's screenshot
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "پیشرفت امروز",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "${PersianDateAndUnitUtils.toPersianDigits(completedTodayCount.toString())} از ${PersianDateAndUnitUtils.toPersianDigits((completedTodayCount + pendingTodayTasks.size).toString())} انجام شده",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text(
                                                text = "${PersianDateAndUnitUtils.toPersianDigits((productivityProgress * 100).toInt().toString())}٪",
                                                style = MaterialTheme.typography.headlineSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = accentColor
                                            )
                                        }
                                        LinearProgressIndicator(
                                            progress = { productivityProgress.coerceIn(0.04f, 1f) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(10.dp)
                                                .clip(RoundedCornerShape(50)),
                                            color = accentColor,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Grid of 6 Live Summary Cards
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
                                title = "فعالیت ورزشی امروز",
                                primaryValue = "${PersianDateAndUnitUtils.toPersianDigits(todayExerciseMinutes.toString())} دقیقه",
                                subtitle = "هدف: ${PersianDateAndUnitUtils.toPersianDigits(prefs.dailyExerciseTargetMinutes.toString())} دقیقه",
                                progress = (todayExerciseMinutes.toFloat() / prefs.dailyExerciseTargetMinutes.coerceAtLeast(1)).coerceIn(0f, 1f),
                                accentColor = MaterialTheme.colorScheme.primary,
                                icon = Icons.AutoMirrored.Filled.DirectionsRun,
                                onClick = { onNavigateToSection("DAILY_LOG") }
                            )
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

        // 8. Recent Activity Timeline (shown in Bento Grid & Kanban Board)
        if (currentLayoutStyle != AppLayoutStyle.MINIMAL_ZEN) {
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
                                Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
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
                    val priorityAccent = when (task.priority) {
                        "CRITICAL" -> RoseError
                        "HIGH" -> AmberWarning
                        "LOW" -> SkyInfo
                        else -> ElectricTeal
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, priorityAccent.copy(alpha = 0.28f)),
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
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
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
    Card(
        modifier = Modifier.width(264.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.surfaceVariant
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
                    fontWeight = FontWeight.Bold
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
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.32f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.08f),
                            MaterialTheme.colorScheme.surfaceVariant
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
