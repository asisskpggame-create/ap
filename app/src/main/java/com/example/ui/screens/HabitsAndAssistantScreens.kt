package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ai.AiParseResult
import com.example.ai.ProposedAiOperation
import com.example.data.model.ConversationTurn
import com.example.data.model.Habit
import com.example.data.model.HabitRecord
import com.example.data.model.UserPreferencesDoc
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.OutlinedTextFieldDefaults
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ElectricTeal
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LiquidGlassPill
import com.example.ui.theme.MutedViolet
import com.example.util.PersianDateAndUnitUtils
import com.example.util.WorldClassOsUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HabitsScreen(
    prefs: UserPreferencesDoc,
    habits: List<Habit>,
    habitRecords: List<HabitRecord>,
    onCreateHabit: (title: String, category: String, scheduleType: String, scheduleDays: List<Int>, targetPerPeriod: Int) -> Unit,
    onToggleHabitDay: (Habit, Long) -> Unit
) {
    val context = LocalContext.current
    val todayEpochDay = PersianDateAndUnitUtils.currentLocalEpochDay()
    var newHabitTitle by remember { mutableStateOf("") }
    var scheduleType by remember { mutableStateOf("DAILY") }
    var targetPerWeek by remember { mutableIntStateOf(3) }

    val presetHabits = listOf(
        "مطالعه روزانه (۲۰ دقیقه)" to "DAILY",
        "ورزش و تحرک" to "TIMES_PER_WEEK",
        "مدیتیشن و تمرکز" to "DAILY",
        "خواب منظم قبل از ساعت ۲۳" to "DAILY",
        "مرور اهداف شخصی" to "DAILY"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("habits_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Create Habit Card
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                accentBorderColor = MutedViolet.copy(alpha = 0.45f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("عادت جدید", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presetHabits.forEach { (title, sched) ->
                            AssistChip(
                                onClick = {
                                    onCreateHabit(title, "شخصی", sched, listOf(1, 2, 3, 4, 5, 6, 7), if (sched == "TIMES_PER_WEEK") 3 else 1)
                                },
                                label = { Text("+ $title") }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = newHabitTitle,
                        onValueChange = { newHabitTitle = it },
                        label = { Text("عنوان عادت دلخواه") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "DAILY" to "روزانه",
                            "TIMES_PER_WEEK" to "چند بار در هفته",
                            "WEEKLY_DAYS" to "روزهای زوج/فرد"
                        ).forEach { (code, lbl) ->
                            FilterChip(
                                selected = scheduleType == code,
                                onClick = { scheduleType = code },
                                label = { Text(lbl) }
                            )
                        }
                    }

                    if (scheduleType == "TIMES_PER_WEEK") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("هدف هفتگی:", style = MaterialTheme.typography.bodySmall)
                            listOf(2, 3, 4, 5).forEach { cnt ->
                                FilterChip(
                                    selected = targetPerWeek == cnt,
                                    onClick = { targetPerWeek = cnt },
                                    label = { Text("${PersianDateAndUnitUtils.toPersianDigits(cnt.toString())} روز در هفته") }
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (newHabitTitle.isNotBlank()) {
                                val days = if (scheduleType == "WEEKLY_DAYS") listOf(6, 1, 3) else listOf(1, 2, 3, 4, 5, 6, 7)
                                val target = if (scheduleType == "TIMES_PER_WEEK") targetPerWeek else 1
                                onCreateHabit(newHabitTitle.trim(), "شخصی", scheduleType, days, target)
                                newHabitTitle = ""
                            }
                        },
                        enabled = newHabitTitle.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("افزودن عادت")
                    }
                }
            }
        }

        if (habits.isEmpty()) {
            item {
                com.example.ui.components.GlassEmptyState(
                    title = "هنوز عادتی ثبت نشده",
                    subtitle = "یکی از پیشنهادهای بالا را انتخاب کنید یا عادت دلخواه خود را بسازید.",
                    badgeText = "روتین روزانه",
                    icon = Icons.Default.LocalFireDepartment,
                    accentColor = MutedViolet,
                    actionButtonText = "افزودن مطالعه روزانه",
                    actionButtonIcon = Icons.Default.Add,
                    onActionButtonClick = {
                        onCreateHabit("مطالعه روزانه (۲۰ دقیقه)", "شخصی", "DAILY", listOf(1, 2, 3, 4, 5, 6, 7), 1)
                    },
                    testTag = "habits_empty_state"
                )
            }
        }

        items(habits.filter { !it.archived }, key = { it.id }) { habit ->
            val completedDays = habitRecords
                .filter { it.habitId == habit.id && it.completedCount > 0 }
                .map { it.localEpochDay }
                .toSet()

            val streak = PersianDateAndUnitUtils.calculateHabitStreak(
                habit = habit,
                completedEpochDays = completedDays,
                todayEpochDay = todayEpochDay,
                weekStartDay = prefs.weekStartDay
            )
            val isDoneToday = todayEpochDay in completedDays

            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                accentBorderColor = if (isDoneToday) EmeraldSuccess.copy(alpha = 0.65f) else MutedViolet.copy(alpha = 0.4f)
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = habit.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = when (habit.scheduleType) {
                                    "DAILY" -> "برنامه: هر روز"
                                    "TIMES_PER_WEEK" -> "برنامه: ${PersianDateAndUnitUtils.toPersianDigits(habit.targetPerPeriod.toString())} بار در هفته"
                                    else -> "برنامه: روزهای منتخب هفته"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Streak Badge
                        LiquidGlassPill(
                            accentGlow = AmberWarning,
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${PersianDateAndUnitUtils.toPersianDigits(streak.toString())} استمرار",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = AmberWarning
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                if (!isDoneToday) {
                                    WorldClassOsUtils.HapticSignature.successThud(context)
                                    WorldClassOsUtils.HapticSignature.doublePulse(context)
                                } else {
                                    WorldClassOsUtils.HapticSignature.crispTick(context)
                                }
                                onToggleHabitDay(habit, todayEpochDay)
                            }
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isDoneToday) "ثبت شد" else "انجام امروز")
                        }
                    }

                    // 14-Day Interactive Calendar Heatmap
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "وضعیت ۱۴ روز اخیر:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            (13 downTo 0).forEach { daysAgo ->
                                val day = todayEpochDay - daysAgo
                                val done = day in completedDays
                                val jalali = PersianDateAndUnitUtils.epochDayToJalali(day)
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (done) EmeraldSuccess else MaterialTheme.colorScheme.surface
                                        )
                                        .clickable {
                                            if (!done) {
                                                WorldClassOsUtils.HapticSignature.successThud(context)
                                            } else {
                                                WorldClassOsUtils.HapticSignature.crispTick(context)
                                            }
                                            onToggleHabitDay(habit, day)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = PersianDateAndUnitUtils.toPersianDigits(jalali.day.toString()),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (done) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiAssistantScreen(
    conversations: List<ConversationTurn>,
    isProcessing: Boolean,
    pendingPreview: AiParseResult?,
    onSendPrompt: (String) -> Unit,
    onConfirmOperations: (List<ProposedAiOperation>, String) -> Unit,
    onRejectPreview: (String) -> Unit,
    onOpenVoiceSheet: () -> Unit
) {
    var inputPrompt by remember { mutableStateOf("") }

    val quickPrompts = listOf(
        "امروز چه تسک‌هایی باقی مانده؟",
        "دیروز چه کارهایی انجام دادم؟",
        "امروز چقدر آب ثبت کردم؟",
        "این هفته چند تا قهوه خوردم؟",
        "این ماه چقدر هزینه کردم؟",
        "دیشب چقدر خوابیدم؟",
        "کدام عادت‌ها را امروز انجام ندادم؟",
        "تغییراتی که جمینای امروز انجام داده را نشان بده"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ElectricTeal)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "دستیار هوشمند",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "ثبت سریع صوتی و متنی",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(onClick = onOpenVoiceSheet) {
                Icon(Icons.Default.Mic, contentDescription = "فرمان صوتی", tint = ElectricTeal)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Prompt Chips
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickPrompts.forEach { q ->
                AssistChip(
                    onClick = { onSendPrompt(q) },
                    label = { Text(q, style = MaterialTheme.typography.bodySmall) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Conversation History + Structured Operation Confirmation
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("ai_conversation_list"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (conversations.isEmpty() && pendingPreview == null) {
                item {
                    LiquidGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        accentBorderColor = ElectricTeal
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "آماده دریافت دستور",
                                style = MaterialTheme.typography.titleSmall,
                                color = ElectricTeal,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "می‌توانید با متن یا صدا تراکنش مالی، تسک و عادت جدید ثبت کنید یا گزارش عملکرد بگیرید.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(conversations, key = { it.id }) { turn ->
                val isUser = turn.role == "USER"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.Start else Arrangement.End
                ) {
                    LiquidGlassCard(
                        shape = RoundedCornerShape(18.dp),
                        accentBorderColor = if (isUser) ElectricTeal else MutedViolet,
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = if (isUser) "شما" else "دستیار هوشمند",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isUser) ElectricTeal else MutedViolet
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = turn.messageText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            if (pendingPreview != null) {
                item {
                    StructuredOperationPreviewCard(
                        preview = pendingPreview,
                        rawTranscript = inputPrompt,
                        onConfirm = { ops ->
                            onConfirmOperations(ops, inputPrompt)
                            inputPrompt = ""
                        },
                        onCancel = {
                            onRejectPreview(inputPrompt)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bottom Prompt Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 72.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputPrompt,
                onValueChange = { inputPrompt = it },
                placeholder = { Text("پیام یا دستور خود را بنویسید...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_chat_input"),
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF141F1A).copy(alpha = 0.65f),
                    unfocusedContainerColor = Color(0xFF141F1A).copy(alpha = 0.45f),
                    focusedBorderColor = ElectricTeal.copy(alpha = 0.7f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )
            Button(
                onClick = {
                    if (inputPrompt.isNotBlank()) {
                        val toSend = inputPrompt
                        inputPrompt = ""
                        onSendPrompt(toSend)
                    }
                },
                enabled = inputPrompt.isNotBlank() && !isProcessing,
                shape = CircleShape,
                contentPadding = PaddingValues(14.dp),
                modifier = Modifier.testTag("ai_send_button")
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "ارسال")
                }
            }
        }
    }
}
