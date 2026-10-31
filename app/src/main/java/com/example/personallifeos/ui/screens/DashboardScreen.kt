package com.example.personallifeos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personallifeos.data.model.DebtItem
import com.example.personallifeos.data.model.ExpenseItem
import com.example.personallifeos.data.model.GoalItem
import com.example.personallifeos.data.model.HabitItem
import com.example.personallifeos.data.model.HealthLog
import com.example.personallifeos.data.model.ReminderItem
import com.example.personallifeos.data.model.TaskItem
import com.example.personallifeos.data.model.TaskStatus
import com.example.personallifeos.data.model.TopicItem
import com.example.personallifeos.ui.components.AppSvgIcons
import com.example.personallifeos.ui.theme.AmberWarning
import com.example.personallifeos.ui.theme.BorderCyanGlow
import com.example.personallifeos.ui.theme.CyanPrimary
import com.example.personallifeos.ui.theme.EmeraldAccent
import com.example.personallifeos.ui.theme.RoseDanger
import com.example.personallifeos.ui.theme.SurfaceCard
import com.example.personallifeos.ui.theme.SurfaceDark
import com.example.personallifeos.ui.theme.TextMuted
import com.example.personallifeos.ui.theme.TextPrimary
import com.example.personallifeos.ui.theme.TextSecondary
import com.example.personallifeos.util.PersianUtils

@Composable
fun DashboardScreen(
    tasks: List<TaskItem>,
    habits: List<HabitItem>,
    expenses: List<ExpenseItem>,
    debts: List<DebtItem> = emptyList(),
    goals: List<GoalItem> = emptyList(),
    topics: List<TopicItem> = emptyList(),
    reminders: List<ReminderItem> = emptyList(),
    visibleCards: Set<String> = setOf("FINANCE", "REMINDERS", "GOALS_TASKS", "HEALTH", "HABITS_TRENDS"),
    maskSensitiveAmounts: Boolean = false,
    healthLog: HealthLog,
    onNavigate: (String) -> Unit,
    onOpenTopicFromDashboard: (String) -> Unit = {},
    onOpenVoiceModal: () -> Unit,
    onToggleTask: (TaskItem) -> Unit,
    onToggleHabit: (HabitItem) -> Unit,
    onSnoozeReminder: (String, Int) -> Unit = { _, _ -> },
    onDismissReminder: (String) -> Unit = {},
    onToggleCardVisibility: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showCustomizeBar by remember { mutableStateOf(false) }
    var selectedSnoozeMinutes by remember { mutableIntStateOf(15) }

    val pendingTasks = tasks.filter { !it.completed && it.status != TaskStatus.CANCELLED }
    val overdueTasks = pendingTasks.filter { it.dueDate < "۱۴۰۵/۰۸/۰۵" }
    val safeToSpend = PersianUtils.calculateSafeToSpend(25_000_000L, expenses, debts)
    val totalDebtsRem = PersianUtils.aggregateTotalDebtsRemainingToman(debts)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Greeting & Today's View Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderCyanGlow, RoundedCornerShape(24.dp))
                    .testTag("dashboard_hero_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF0F172A), Color(0xFF092C3E))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(Color(0x2606B6D4), CircleShape)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SmartToy,
                                    contentDescription = null,
                                    tint = CyanPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "نمای امروز • ۵ آبان ۱۴۰۵",
                                    color = CyanPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SurfaceCard)
                                    .clickable { showCustomizeBar = !showCustomizeBar }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("dashboard_customize_cards_button")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Tune, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("شخصی‌سازی کارت‌ها", fontSize = 11.sp, color = TextPrimary)
                                }
                            }
                        }

                        Text(
                            text = "الان چه وضعیتی دارم و اقدام مهم بعدی چیست؟",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        val majorTask = pendingTasks.firstOrNull { it.isMajorMission }
                        val activeGoal = goals.firstOrNull()
                        Text(
                            text = when {
                                majorTask != null -> "مأموریت اصلی امروز: ${majorTask.title} (${PersianUtils.formatPersianNumber(pendingTasks.size.toLong())} وظیفه باز • ${PersianUtils.formatPersianNumber(topics.count { !it.isArchived }.toLong())} تاپیک فعال)"
                                activeGoal != null -> "اقدام مهم بعدی: ${activeGoal.nextAction} (${PersianUtils.formatPersianNumber(pendingTasks.size.toLong())} وظیفه نیازمند توجه امروز)"
                                else -> "تمام ماژول‌های مالی، تاپیک‌ها، مأموریت‌ها و یادآوری‌های مشترک آماده هستند."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )

                        if (overdueTasks.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(RoseDanger.copy(alpha = 0.16f))
                                    .border(0.5.dp, RoseDanger, RoundedCornerShape(12.dp))
                                    .clickable { onNavigate("tasks") }
                                    .padding(10.dp)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = RoseDanger, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${PersianUtils.formatPersianNumber(overdueTasks.size.toLong())} مأموریت عقب‌افتاده نیازمند رسیدگی فوری: ${overdueTasks.first().title}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RoseDanger
                                )
                            }
                        }

                        // Quick Action Bar
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Button(
                                onClick = onOpenVoiceModal,
                                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.testTag("dashboard_voice_btn")
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("ثبت صوتی / فیش", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = { onNavigate("expenses") },
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("+ ثبت مالی", color = AmberWarning, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { onNavigate("assistant") },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.testTag("dashboard_assistant_btn")
                            ) {
                                Text("تحلیل AI", color = TextPrimary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Customize Dashboard Cards Selector
        if (showCustomizeBar) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("انتخاب کارت‌های قابل نمایش در داشبورد:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(
                                listOf(
                                    "FINANCE" to "وضعیت مالی و برآورد",
                                    "REMINDERS" to "یادآوری‌های اولویت‌دار",
                                    "GOALS_TASKS" to "اهداف و کارهای امروز",
                                    "HEALTH" to "سلامت و آب",
                                    "HABITS_TRENDS" to "عادت‌ها و روندها"
                                )
                            ) { (key, label) ->
                                val isVisible = visibleCards.contains(key)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isVisible) CyanPrimary else SurfaceDark)
                                        .clickable { onToggleCardVisibility(key) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = if (isVisible) "✓ $label" else label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isVisible) Color.Black else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 1. Finance & Safe-to-Spend Summary Card
        if (visibleCards.contains("FINANCE")) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(0.5.dp, Color(0x33F59E0B), RoundedCornerShape(20.dp))
                        .clickable { onNavigate("expenses") }
                        .testTag("metric_expenses")
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Wallet, contentDescription = null, tint = AmberWarning)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("خلاصه مالی، قسط نزدیک و Safe-to-Spend", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                            }
                            Text("ورود به مالی ←", fontSize = 11.sp, color = AmberWarning, fontWeight = FontWeight.Bold)
                        }

                        if (!safeToSpend.hasEnoughData) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "داده کافی نیست — هنوز تراکنشی ثبت نشده است.",
                                    fontSize = 12.sp,
                                    color = AmberWarning
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SurfaceDark)
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Text("هزینه واقعی ماه", fontSize = 10.sp, color = TextMuted)
                                        Text(
                                            text = if (maskSensitiveAmounts) "•••••• تومان" else PersianUtils.formatToman(safeToSpend.totalExpenseToman),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SurfaceDark)
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Text("قابل‌خرج (Safe-to-Spend)", fontSize = 10.sp, color = TextMuted)
                                        Text(
                                            text = if (maskSensitiveAmounts) "•••••• تومان" else PersianUtils.formatToman(safeToSpend.safeToSpendToman),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldAccent
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "برآورد هزینه پایان ماه (تخمینی): ${if (maskSensitiveAmounts) "••••••" else PersianUtils.formatToman(safeToSpend.estimatedEndOfMonthExpenseToman)}",
                                    fontSize = 11.sp,
                                    color = CyanPrimary
                                )
                                Text(
                                    text = "بدهی باز: ${if (maskSensitiveAmounts) "••••" else PersianUtils.formatToman(totalDebtsRem)}",
                                    fontSize = 11.sp,
                                    color = RoseDanger,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Unified Priority Reminders Card (with SVG Vector Icons)
        if (visibleCards.contains("REMINDERS") && reminders.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(0.5.dp, BorderCyanGlow, RoundedCornerShape(20.dp))
                        .testTag("dashboard_reminders_card")
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "موتور یادآوری هوشمند (تاپیک، وظیفه، قسط و عادت)",
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        // Snooze Interval Selector (۱۰، ۱۵، ۳۰، ۶۰ دقیقه)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("فاصله تعویق (Snooze):", fontSize = 10.sp, color = TextMuted)
                            listOf(10, 15, 30, 60).forEach { mins ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (selectedSnoozeMinutes == mins) AmberWarning else SurfaceDark)
                                        .clickable { selectedSnoozeMinutes = mins }
                                        .padding(horizontal = 7.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "${PersianUtils.formatPersianNumber(mins.toLong())} د",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedSnoozeMinutes == mins) Color.Black else TextSecondary
                                    )
                                }
                            }
                        }

                        reminders.take(4).forEach { rem ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceDark)
                                    .clickable {
                                        if (!rem.topicId.isNullOrBlank()) {
                                            onOpenTopicFromDashboard(rem.topicId)
                                        } else {
                                            onNavigate("tasks")
                                        }
                                    }
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    if (!rem.topicName.isNullOrBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "تاپیک: ${rem.topicName}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = EmeraldAccent
                                            )
                                        }
                                    }
                                    Text(
                                        text = if (maskSensitiveAmounts && rem.sensitiveAmountToman != null)
                                            "${rem.entityType.titleFa}: [محتوای حساس مالی پنهان شد]"
                                        else rem.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "${rem.entityType.titleFa} • ${rem.triggerTime} • ${rem.repeatMode.titleFa}",
                                        fontSize = 10.sp,
                                        color = if (rem.isSnoozed) AmberWarning else CyanPrimary
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(AmberWarning.copy(alpha = 0.2f))
                                            .clickable { onSnoozeReminder(rem.id, selectedSnoozeMinutes) }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                            .testTag("snooze_${rem.id}")
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Snooze, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("Snooze", fontSize = 10.sp, color = AmberWarning, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(EmeraldAccent.copy(alpha = 0.2f))
                                            .clickable { onDismissReminder(rem.id) }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = "تایید", tint = EmeraldAccent, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Metrics Grid (Tasks, Habits, Health)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "تاپیک‌ها و کارهای امروز",
                    value = "${PersianUtils.formatPersianNumber(pendingTasks.size.toLong())} وظیفه • ${PersianUtils.formatPersianNumber(topics.count { !it.isArchived }.toLong())} تاپیک",
                    subtitle = "ورود به اهداف و مسیر",
                    icon = Icons.Default.CheckCircle,
                    iconTint = CyanPrimary,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("metric_tasks")
                        .clickable { onNavigate("tasks") }
                )

                MetricCard(
                    title = "پایش سلامت و کالری",
                    value = "${PersianUtils.formatPersianNumber(healthLog.waterGlasses.toLong())} لیوان آب",
                    subtitle = "${PersianUtils.formatPersianNumber(healthLog.dailyCalorieIntake.toLong())} کالری • ورود",
                    icon = Icons.Default.Favorite,
                    iconTint = Color(0xFFEC4899),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("metric_health")
                        .clickable { onNavigate("health") }
                )
            }
        }

        // 4. Today's Priority Tasks (Independent + Topic Missions with SVG Icons)
        if (visibleCards.contains("GOALS_TASKS")) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "مأموریت‌های تاپیک‌ها، وظایف مستقل و اقساط امروز",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "مشاهده همه در مسیر",
                        color = CyanPrimary,
                        fontSize = 12.sp,
                        modifier = Modifier.clickable { onNavigate("tasks") }
                    )
                }
            }

            items(tasks.take(4), key = { it.id }) { task ->
                val parentTopic = topics.find { it.id == task.topicId }
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (parentTopic != null) {
                                onOpenTopicFromDashboard(parentTopic.id)
                            } else {
                                onToggleTask(task)
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (task.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (task.completed) EmeraldAccent else TextMuted,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { onToggleTask(task) }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (parentTopic != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        AppSvgIcons.TopicSvgIcon(
                                            iconKey = parentTopic.iconEmoji,
                                            tint = Color(parentTopic.colorHex),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = parentTopic.name,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(parentTopic.colorHex)
                                        )
                                    }
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.PushPin, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(11.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "وظیفه مستقل",
                                            fontSize = 10.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                                if (task.isMajorMission) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("اصلی", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = AmberWarning)
                                    }
                                }
                            }
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = if (task.completed) TextMuted else TextPrimary
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(
                                    text = "دسته: ${task.category.titleFa}",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                                Text(
                                    text = "موعد: ${task.dueDate}",
                                    fontSize = 11.sp,
                                    color = if (!task.completed && task.dueDate < "۱۴۰۵/۰۸/۰۵") RoseDanger else CyanPrimary
                                )
                                if (task.linkedDebtId != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Link, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = "قسط بدهی",
                                            fontSize = 11.sp,
                                            color = AmberWarning,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Active Habits Quick Row
        if (visibleCards.contains("HABITS_TRENDS")) {
            item {
                Text(
                    text = "استمرار عادت‌های فعال امروز",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            items(habits, key = { it.id }) { habit ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x2610B981)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(text = habit.title, color = TextPrimary, fontWeight = FontWeight.Medium)
                                Text(
                                    text = "زنجیره: ${PersianUtils.formatPersianNumber(habit.streak.toLong())} روز متوالی",
                                    color = EmeraldAccent,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Button(
                            onClick = { onToggleHabit(habit) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (habit.completedToday) EmeraldAccent else Color(0xFF1E293B)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (habit.completedToday) "تکمیل شد" else "ثبت امروز",
                                color = if (habit.completedToday) Color.Black else TextPrimary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        modifier = modifier.border(0.5.dp, Color(0x1AFFFFFF), RoundedCornerShape(18.dp))
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
                Text(text = title, fontSize = 12.sp, color = TextMuted)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
                }
            }
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = subtitle, fontSize = 11.sp, color = iconTint)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = iconTint, modifier = Modifier.size(10.dp))
            }
        }
    }
}
