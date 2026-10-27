package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.ConsumptionEntry
import com.example.data.model.ExerciseRecord
import com.example.data.model.Habit
import com.example.data.model.HabitRecord
import com.example.data.model.SleepRecord
import com.example.data.model.Task
import com.example.data.model.UserPreferencesDoc
import com.example.ui.components.AiMealAndReceiptScannerModalSheet
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ElectricTeal
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.MutedViolet
import com.example.ui.theme.RoseError
import com.example.ui.theme.SkyInfo
import com.example.util.PersianDateAndUnitUtils
import com.example.util.WorldClassOsUtils

@Composable
fun GoalsAndHabitsCombinedScreen(
    prefs: UserPreferencesDoc,
    tasks: List<Task>,
    calendarDisplay: String,
    activeFocusTaskId: String?,
    onStartFocusTimer: (Task) -> Unit,
    onCreateTask: (title: String, desc: String, cat: String, tags: List<String>, prio: String, dueDay: Long, dueTime: Int, remMillis: Long, estDur: Int, subtasksJson: String, recRule: String, recInterval: Int, notes: String) -> Unit,
    onCompleteTask: (Task) -> Unit,
    onBulkComplete: (List<Task>) -> Unit,
    onUpdateTask: (Task) -> Unit,
    onRescheduleTask: (Task, Long) -> Unit,
    onArchiveOrRestore: (Task, Boolean) -> Unit,
    onDeleteTask: (Task) -> Unit,
    habits: List<Habit>,
    habitRecords: List<HabitRecord>,
    onCreateHabit: (title: String, category: String, scheduleType: String, scheduleDays: List<Int>, targetPerPeriod: Int) -> Unit,
    onToggleHabitDay: (Habit, Long) -> Unit,
    consumption: List<ConsumptionEntry> = emptyList(),
    sleepRecords: List<SleepRecord> = emptyList(),
    exerciseRecords: List<ExerciseRecord> = emptyList(),
    onQuickAddConsumption: (itemName: String, category: String, quantity: Double, unit: String, notes: String) -> Unit = { _, _, _, _, _ -> },
    onDeleteConsumption: (ConsumptionEntry) -> Unit = {},
    onAddSleep: (durationMinutes: Int, qualityRating: Int, notes: String) -> Unit = { _, _, _ -> },
    onAddExercise: (activityType: String, durationMinutes: Int, intensity: String, distanceMeters: Double, repetitions: Int, notes: String) -> Unit = { _, _, _, _, _, _ -> },
    initialTabIndex: Int = 0
) {
    val context = LocalContext.current
    var selectedSectionIndex by remember { mutableIntStateOf(initialTabIndex.coerceIn(0, 2)) }

    LaunchedEffect(initialTabIndex) {
        selectedSectionIndex = initialTabIndex.coerceIn(0, 2)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Unified Header
        Text(
            text = "اهداف، عادت‌ها و سلامت",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Segmented Section Selector (Unified 3-Part Switcher)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("goals_habits_section_switcher")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val sections = listOf(
                    0 to "اهداف و تسک‌ها",
                    1 to "عادت‌های روزانه",
                    2 to "سلامت و روتین"
                )

                sections.forEach { (index, title) ->
                    val isSelected = selectedSectionIndex == index
                    val activeBg = if (isSelected) {
                        ElectricTeal.copy(alpha = 0.22f)
                    } else {
                        Color.Transparent
                    }
                    val activeBorder = if (isSelected) ElectricTeal else Color.Transparent

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = activeBg,
                        border = BorderStroke(1.dp, activeBorder),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                WorldClassOsUtils.HapticSignature.crispTick(context)
                                selectedSectionIndex = index
                            }
                            .testTag("goals_habits_tab_$index")
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) ElectricTeal else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Isolated Content Body per Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            AnimatedContent(
                targetState = selectedSectionIndex,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "goals_habits_section_transition"
            ) { targetIndex ->
                when (targetIndex) {
                    0 -> TasksScreen(
                        tasks = tasks,
                        calendarDisplay = calendarDisplay,
                        activeFocusTaskId = activeFocusTaskId,
                        onStartFocusTimer = onStartFocusTimer,
                        onCreateTask = onCreateTask,
                        onCompleteTask = onCompleteTask,
                        onBulkComplete = onBulkComplete,
                        onUpdateTask = onUpdateTask,
                        onRescheduleTask = onRescheduleTask,
                        onArchiveOrRestore = onArchiveOrRestore,
                        onDeleteTask = onDeleteTask
                    )
                    1 -> HabitsScreen(
                        prefs = prefs,
                        habits = habits,
                        habitRecords = habitRecords,
                        onCreateHabit = onCreateHabit,
                        onToggleHabitDay = onToggleHabitDay
                    )
                    2 -> HealthAndRoutineSubScreen(
                        prefs = prefs,
                        consumption = consumption,
                        sleepRecords = sleepRecords,
                        exerciseRecords = exerciseRecords,
                        onQuickAddConsumption = onQuickAddConsumption,
                        onDeleteConsumption = onDeleteConsumption,
                        onAddSleep = onAddSleep,
                        onAddExercise = onAddExercise
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HealthAndRoutineSubScreen(
    prefs: UserPreferencesDoc,
    consumption: List<ConsumptionEntry>,
    sleepRecords: List<SleepRecord>,
    exerciseRecords: List<ExerciseRecord>,
    onQuickAddConsumption: (itemName: String, category: String, quantity: Double, unit: String, notes: String) -> Unit,
    onDeleteConsumption: (ConsumptionEntry) -> Unit,
    onAddSleep: (durationMinutes: Int, qualityRating: Int, notes: String) -> Unit,
    onAddExercise: (activityType: String, durationMinutes: Int, intensity: String, distanceMeters: Double, repetitions: Int, notes: String) -> Unit
) {
    val context = LocalContext.current
    val todayEpochDay = PersianDateAndUnitUtils.currentLocalEpochDay()

    val todayConsumption = remember(consumption, todayEpochDay) {
        consumption.filter { it.localEpochDay == todayEpochDay }
    }
    val todayWaterMl = remember(todayConsumption) {
        PersianDateAndUnitUtils.sumCompatibleConsumption(
            todayConsumption.filter { it.category == "WATER" },
            "ml"
        ).toInt()
    }
    val waterProgress = (todayWaterMl.toFloat() / prefs.dailyWaterTargetMl.coerceAtLeast(500).toFloat()).coerceIn(0f, 1f)

    // Sleep state
    var sleepHours by remember { mutableStateOf("7.5") }
    var sleepQuality by remember { mutableIntStateOf(4) }
    var sleepNotes by remember { mutableStateOf("") }

    // Exercise state
    var exerciseActivity by remember { mutableStateOf("پیاده‌روی") }
    var exerciseDurationText by remember { mutableStateOf("30") }
    var exerciseIntensity by remember { mutableStateOf("MODERATE") }
    var exerciseNotes by remember { mutableStateOf("") }

    // Nutrition state
    var mealName by remember { mutableStateOf("") }
    var mealCaloriesText by remember { mutableStateOf("") }
    var showMealScannerSheet by remember { mutableStateOf(false) }

    if (showMealScannerSheet) {
        AiMealAndReceiptScannerModalSheet(
            onDismiss = { showMealScannerSheet = false },
            onLogScannedMeal = { scannedMealName, kcal, notes ->
                onQuickAddConsumption(scannedMealName, "FOOD", kcal, "kcal", notes)
                showMealScannerSheet = false
            },
            onLogScannedReceipt = { _, _, _, _ ->
                showMealScannerSheet = false
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("health_routine_screen_list"),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Hydration & Daily Beverages Card
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                accentBorderColor = SkyInfo.copy(alpha = 0.55f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
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
                            Icon(Icons.Default.LocalDrink, contentDescription = null, tint = SkyInfo)
                            Column {
                                Text(
                                    text = "آبرسانی و نوشیدنی امروز",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${PersianDateAndUnitUtils.toPersianDigits(todayWaterMl.toString())} از ${PersianDateAndUnitUtils.toPersianDigits(prefs.dailyWaterTargetMl.toString())} میلی‌لیتر",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SkyInfo
                                )
                            }
                        }

                        Text(
                            text = "${PersianDateAndUnitUtils.toPersianDigits((waterProgress * 100).toInt().toString())}٪",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = if (waterProgress >= 1f) EmeraldSuccess else SkyInfo
                        )
                    }

                    LinearProgressIndicator(
                        progress = { waterProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(CircleShape),
                        color = if (waterProgress >= 1f) EmeraldSuccess else SkyInfo,
                        trackColor = SkyInfo.copy(alpha = 0.18f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                WorldClassOsUtils.HapticSignature.doublePulse(context)
                                onQuickAddConsumption("آب", "WATER", 250.0, "ml", "۱ لیوان آب")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SkyInfo, contentColor = Color.Black),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("health_add_water_250")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("۱ لیوان (۲۵۰ml)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                WorldClassOsUtils.HapticSignature.doublePulse(context)
                                onQuickAddConsumption("آب", "WATER", 500.0, "ml", "بطری آب")
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("health_add_water_500")
                        ) {
                            Icon(Icons.Default.LocalDrink, contentDescription = null, tint = SkyInfo, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("بطری (۵۰۰ml)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = {
                                WorldClassOsUtils.HapticSignature.crispTick(context)
                                onQuickAddConsumption("قهوه", "COFFEE", 1.0, "فنجان", "")
                            },
                            leadingIcon = { Icon(Icons.Default.Coffee, contentDescription = null, modifier = Modifier.size(15.dp)) },
                            label = { Text("+ ۱ فنجان قهوه") }
                        )
                        AssistChip(
                            onClick = {
                                WorldClassOsUtils.HapticSignature.crispTick(context)
                                onQuickAddConsumption("چای", "TEA", 1.0, "فنجان", "")
                            },
                            leadingIcon = { Icon(Icons.Default.Coffee, contentDescription = null, modifier = Modifier.size(15.dp)) },
                            label = { Text("+ ۱ لیوان چای") }
                        )
                    }
                }
            }
        }

        // 2. Sleep Tracker Card
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                accentBorderColor = MutedViolet.copy(alpha = 0.55f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Bedtime, contentDescription = null, tint = MutedViolet)
                        Column {
                            Text(
                                text = "ثبت خواب شبانه",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "هدف خواب روزانه: ${PersianDateAndUnitUtils.toPersianDigits((prefs.dailySleepTargetMinutes / 60).toString())} ساعت",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("6" to "۶ ساعت", "7" to "۷ ساعت", "7.5" to "۷.۵ ساعت", "8" to "۸ ساعت", "9" to "۹ ساعت").forEach { (valStr, lbl) ->
                            FilterChip(
                                selected = sleepHours == valStr,
                                onClick = { sleepHours = valStr },
                                label = { Text(lbl) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("کیفیت خواب:", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            (1..5).forEach { star ->
                                IconButton(
                                    onClick = { sleepQuality = star },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "کیفیت $star",
                                        tint = if (star <= sleepQuality) AmberWarning else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = sleepNotes,
                        onValueChange = { sleepNotes = it },
                        label = { Text("توضیحات خواب (اختیاری)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val hrs = sleepHours.toDoubleOrNull() ?: 7.5
                            val mins = (hrs * 60).toInt().coerceIn(30, 960)
                            WorldClassOsUtils.HapticSignature.doublePulse(context)
                            onAddSleep(mins, sleepQuality, sleepNotes.trim())
                            sleepNotes = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MutedViolet, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("submit_sleep_record_button")
                    ) {
                        Icon(Icons.Default.Bedtime, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ثبت رکورد خواب", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 3. Exercise & Activity Tracker Card
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                accentBorderColor = AmberWarning.copy(alpha = 0.55f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null, tint = AmberWarning)
                        Column {
                            Text(
                                text = "ثبت ورزش و تحرک",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "هدف روزانه: ${PersianDateAndUnitUtils.toPersianDigits(prefs.dailyExerciseTargetMinutes.toString())} دقیقه",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("پیاده‌روی", "دویدن", "بدنسازی", "یوگا و کشش", "دوچرخه‌سواری", "شنا").forEach { act ->
                            FilterChip(
                                selected = exerciseActivity == act,
                                onClick = { exerciseActivity = act },
                                label = { Text(act) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = exerciseDurationText,
                            onValueChange = { exerciseDurationText = PersianDateAndUnitUtils.toEnglishDigits(it) },
                            label = { Text("مدت (دقیقه)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        listOf("LOW" to "سبک", "MODERATE" to "متوسط", "HIGH" to "سنگین").forEach { (code, lbl) ->
                            FilterChip(
                                selected = exerciseIntensity == code,
                                onClick = { exerciseIntensity = code },
                                label = { Text(lbl) }
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val dur = (exerciseDurationText.toIntOrNull() ?: 30).coerceIn(5, 600)
                            WorldClassOsUtils.HapticSignature.doublePulse(context)
                            onAddExercise(exerciseActivity, dur, exerciseIntensity, 0.0, 0, exerciseNotes.trim())
                            exerciseNotes = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AmberWarning, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("submit_exercise_record_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ثبت فعالیت ورزشی", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 4. Nutrition & Calorie Log + AI Meal Scanner
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                accentBorderColor = EmeraldSuccess.copy(alpha = 0.5f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
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
                            Icon(Icons.Default.Restaurant, contentDescription = null, tint = EmeraldSuccess)
                            Text(
                                text = "تغذیه و کالری وعده‌ها",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        OutlinedButton(
                            onClick = { showMealScannerSheet = true },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تخمین هوشمند کالری", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = mealName,
                            onValueChange = { mealName = it },
                            label = { Text("نام وعده یا خوراکی") },
                            singleLine = true,
                            modifier = Modifier.weight(1.3f)
                        )
                        OutlinedTextField(
                            value = mealCaloriesText,
                            onValueChange = { mealCaloriesText = PersianDateAndUnitUtils.toEnglishDigits(it) },
                            label = { Text("کالری (kcal)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(0.9f)
                        )
                    }

                    Button(
                        onClick = {
                            val cleanMeal = mealName.trim()
                            val kcal = mealCaloriesText.toDoubleOrNull() ?: 250.0
                            if (cleanMeal.isNotBlank()) {
                                WorldClassOsUtils.HapticSignature.doublePulse(context)
                                onQuickAddConsumption(cleanMeal, "FOOD", kcal, "kcal", "")
                                mealName = ""
                                mealCaloriesText = ""
                            }
                        },
                        enabled = mealName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ثبت وعده غذایی", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 5. Recent Today's Health Logs (Consumption, Sleep, Exercise)
        if (todayConsumption.isNotEmpty() || sleepRecords.isNotEmpty() || exerciseRecords.isNotEmpty()) {
            item {
                Text(
                    text = "گزارش ثبت‌شده‌های امروز و اخیر",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        items(todayConsumption, key = { "cons_${it.id}" }) { entry ->
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                accentBorderColor = SkyInfo.copy(alpha = 0.35f)
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
                            text = "${entry.itemName} • ${PersianDateAndUnitUtils.toPersianDigits(entry.quantity.toInt().toString())} ${entry.unit}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (entry.notes.isNotBlank()) {
                            Text(
                                text = entry.notes,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = { onDeleteConsumption(entry) }) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = RoseError)
                    }
                }
            }
        }

        items(sleepRecords.take(3), key = { "slp_${it.id}" }) { slp ->
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                accentBorderColor = MutedViolet.copy(alpha = 0.35f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "خواب: ${PersianDateAndUnitUtils.toPersianDigits((slp.durationMinutes / 60).toString())} ساعت و ${PersianDateAndUnitUtils.toPersianDigits((slp.durationMinutes % 60).toString())} دقیقه",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MutedViolet
                        )
                        Text(
                            text = "${PersianDateAndUnitUtils.formatEpochDay(slp.localEpochDay, prefs.calendarDisplay)} • کیفیت: ${PersianDateAndUnitUtils.toPersianDigits(slp.qualityRating.toString())} از ۵",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        items(exerciseRecords.take(3), key = { "ex_${it.id}" }) { ex ->
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                accentBorderColor = AmberWarning.copy(alpha = 0.35f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ورزش: ${ex.activityType} (${PersianDateAndUnitUtils.toPersianDigits(ex.durationMinutes.toString())} دقیقه)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = AmberWarning
                        )
                        Text(
                            text = PersianDateAndUnitUtils.formatEpochDay(ex.localEpochDay, prefs.calendarDisplay),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}
