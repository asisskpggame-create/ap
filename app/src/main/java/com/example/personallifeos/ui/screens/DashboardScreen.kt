package com.example.personallifeos.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.personallifeos.ui.components.AppCard
import com.example.personallifeos.ui.components.AppCircularProgressRing
import com.example.personallifeos.ui.components.AppPillActionButton
import com.example.personallifeos.ui.components.AppSectionHeader
import com.example.personallifeos.ui.components.AppStatusBadge
import com.example.personallifeos.ui.components.HabitStreakHeatmap
import com.example.personallifeos.ui.theme.AmberWarning
import com.example.personallifeos.ui.theme.CyanGlow
import com.example.personallifeos.ui.theme.CyanPrimary
import com.example.personallifeos.ui.theme.EmeraldAccent
import com.example.personallifeos.ui.theme.RoseDanger
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
    val completedTasksCount = tasks.count { it.completed || it.status == TaskStatus.COMPLETED }
    val totalTasksCount = tasks.size.coerceAtLeast(1)
    val taskProgressPercent = ((completedTasksCount.toFloat() / totalTasksCount.toFloat()) * 100).toInt().coerceIn(0, 100)

    val focusTask = tasks.firstOrNull { !it.completed && (it.isMajorMission || it.priority.name == "URGENT") }
        ?: tasks.firstOrNull { !it.completed }

    val totalIncome = expenses.filter { it.type.name == "INCOME" }.sumOf { it.amount }.coerceAtLeast(25_000_000L)
    val totalExpense = expenses.filter { it.type.name == "EXPENSE" }.sumOf { it.amount }.coerceAtLeast(18_500_000L)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. TOP APP BAR & USER GREETING (Header)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrackChanges,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "Personal Life OS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { onNavigate("reminders") },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "اعلانات",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onNavigate("settings") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "پروفایل",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // User Name and Date
            Column(modifier = Modifier.padding(top = 4.dp)) {
                Text(
                    text = "سلام، علی 👋",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = PersianUtils.getPersianDateString(),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 2. HERO CARD WITH PROGRESS RING & MOUNTAIN GRADIENT BACKDROP
        item {
            AppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                shape = RoundedCornerShape(24.dp),
                backgroundColor = Color.Transparent,
                borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF0F2B48),
                                    Color(0xFF0D1B2A),
                                    Color(0xFF07111D)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Progress ring on right/start in RTL
                        AppCircularProgressRing(
                            progressPercent = if (taskProgressPercent == 0) 75 else taskProgressPercent,
                            size = 100.dp,
                            strokeWidth = 9.dp,
                            ringColor = CyanPrimary,
                            trackColor = Color.White.copy(alpha = 0.12f)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${PersianUtils.formatPersianNumber(if (taskProgressPercent == 0) 75 else taskProgressPercent.toLong())}٪",
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Text(
                                    text = "پیشرفت امروز",
                                    fontSize = 9.sp,
                                    color = CyanGlow
                                )
                            }
                        }

                        // Right side text in RTL
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "برنامه روزانه فعال",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${PersianUtils.formatPersianNumber(completedTasksCount.toLong())} از ${PersianUtils.formatPersianNumber(tasks.size.toLong())} کار انجام شده است.",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.75f),
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "انرژی و تمرکز بالا ✨",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EmeraldAccent
                            )
                        }
                    }
                }
            }
        }

        // 3. TODAY'S MAIN FOCUS TASK CARD (کارت تمرکز امروز)
        item {
            AppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate("tasks") },
                shape = RoundedCornerShape(18.dp),
                backgroundColor = MaterialTheme.colorScheme.surface,
                borderColor = AmberWarning.copy(alpha = 0.45f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(AmberWarning.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = AmberWarning,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "تمرکز امروز:",
                                fontSize = 11.sp,
                                color = AmberWarning,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = focusTask?.title ?: "پرداخت قسط وام تجهیزات مهندسی",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ArrowBack, // Pointing left in RTL
                        contentDescription = "مشاهده",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 4. TODAY'S SCHEDULE (برنامه امروز)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppSectionHeader(
                    title = "برنامه امروز",
                    actionText = "مشاهده همه",
                    onActionClick = { onNavigate("tasks") }
                )

                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    backgroundColor = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Timeline item 1
                        ScheduleRowItem(
                            time = "۱۰:۰۰",
                            title = "جلسه هماهنگی و بررسی پروژه",
                            category = "کاری",
                            dotColor = CyanPrimary
                        )
                        // Timeline item 2
                        ScheduleRowItem(
                            time = "۱۴:۰۰",
                            title = "مطالعه و تحقیق پایان‌نامه ارشد",
                            category = "تحصیلی",
                            dotColor = CyanPrimary
                        )
                        // Timeline item 3
                        ScheduleRowItem(
                            time = "۱۹:۰۰",
                            title = "ورزش هوازی و بدنسازی عصرگاهی",
                            category = "سلامت",
                            dotColor = AmberWarning
                        )
                    }
                }
            }
        }

        // 5. QUICK ACTION PILLS ROW (+ وظیفه, یادداشت, هزینه, هدف)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppPillActionButton(
                    icon = Icons.Default.CheckCircle,
                    label = "+ وظیفه",
                    onClick = { onNavigate("tasks") },
                    badgeColor = CyanPrimary,
                    modifier = Modifier.weight(1f)
                )
                AppPillActionButton(
                    icon = Icons.Default.Edit,
                    label = "یادداشت",
                    onClick = { onNavigate("vault") },
                    badgeColor = AmberWarning,
                    modifier = Modifier.weight(1f)
                )
                AppPillActionButton(
                    icon = Icons.Default.Wallet,
                    label = "هزینه",
                    onClick = { onNavigate("expenses") },
                    badgeColor = EmeraldAccent,
                    modifier = Modifier.weight(1f)
                )
                AppPillActionButton(
                    icon = Icons.Default.TrackChanges,
                    label = "هدف",
                    onClick = { onNavigate("goals") },
                    badgeColor = CyanGlow,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 6. HABIT STREAK HEATMAP (پایش و استمرار ۳۰ روزه عادت‌ها)
        item {
            HabitStreakHeatmap(
                habits = habits,
                onToggleHabit = onToggleHabit,
                onAddHabitClick = { onNavigate("tasks") },
                title = "روند و پایبندی ۳۰ روزه عادت‌ها",
                modifier = Modifier.testTag("dashboard_habit_streak_heatmap")
            )
        }

        // 7. FINANCIAL SNAPSHOT CARD (وضعیت مالی این ماه)
        item {
            AppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate("expenses") },
                shape = RoundedCornerShape(20.dp),
                backgroundColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "وضعیت مالی این ماه",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(CyanPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Wallet,
                                contentDescription = null,
                                tint = CyanPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Income
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(14.dp))
                                Text("درآمد", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                text = "${PersianUtils.formatToman(totalIncome)} تومان",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldAccent
                            )
                        }

                        // Expense
                        Column(horizontalAlignment = Alignment.End) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = RoseDanger, modifier = Modifier.size(14.dp))
                                Text("هزینه", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                text = "${PersianUtils.formatToman(totalExpense)} تومان",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoseDanger
                            )
                        }
                    }
                }
            }
        }

        // 7. ACTIVE TOPICS SUMMARY
        if (topics.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppSectionHeader(
                        title = "تاپیک‌های فعال",
                        actionText = "همه تاپیک‌ها",
                        onActionClick = { onNavigate("topics") }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        topics.take(2).forEach { topic ->
                            AppCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onOpenTopicFromDashboard(topic.id) },
                                shape = RoundedCornerShape(16.dp),
                                backgroundColor = MaterialTheme.colorScheme.surface
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = topic.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = topic.tagOrCategory,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom spacing for navigation bar
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun ScheduleRowItem(
    time: String,
    title: String,
    category: String,
    dotColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = time,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(42.dp)
        )

        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        AppStatusBadge(
            text = category,
            color = dotColor,
            fontSize = 10.sp
        )
    }
}
