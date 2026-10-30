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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personallifeos.data.model.DebtItem
import com.example.personallifeos.data.model.GoalItem
import com.example.personallifeos.data.model.HabitItem
import com.example.personallifeos.data.model.TaskCategory
import com.example.personallifeos.data.model.TaskItem
import com.example.personallifeos.data.model.TaskPriority
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
import java.util.Locale

@Composable
fun TasksScreen(
    tasks: List<TaskItem>,
    habits: List<HabitItem>,
    goals: List<GoalItem> = emptyList(),
    debts: List<DebtItem> = emptyList(),
    pomodoroRemainingSeconds: Int = 1500,
    isPomodoroRunning: Boolean = false,
    completedPomodoroSessions: Int = 3,
    onAddTask: (String, TaskCategory, TaskPriority, String) -> Unit,
    onToggleTask: (TaskItem) -> Unit,
    onPostponeTask: (TaskItem) -> Unit = {},
    onDeleteTask: (String) -> Unit,
    onPayLinkedDebtInstallment: (DebtItem, Long) -> Unit = { _, _ -> },
    onAddGoal: (String, String, String, String, TaskCategory) -> Unit = { _, _, _, _, _ -> },
    onUpdateGoalProgress: (String, Int) -> Unit = { _, _ -> },
    onDeleteGoal: (String) -> Unit = {},
    onToggleHabit: (HabitItem) -> Unit,
    onAddHabit: (String) -> Unit,
    onTogglePomodoro: () -> Unit = {},
    onResetPomodoro: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<TaskCategory?>(null) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showAddHabitDialog by remember { mutableStateOf(false) }
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var selectedJalaliDay by remember { mutableIntStateOf(5) }

    val filteredTasks = remember(tasks, searchQuery, selectedCategory) {
        tasks.filter { t ->
            val matchesSearch = PersianUtils.normalizePersianText(t.title)
                .contains(PersianUtils.normalizePersianText(searchQuery))
            val matchesCategory = selectedCategory == null || t.category == selectedCategory
            matchesSearch && matchesCategory
        }
    }

    Scaffold(
        modifier = modifier.testTag("tasks_screen"),
        containerColor = Color.Transparent,
        floatingActionButton = {
            if (selectedTab in 0..2) {
                FloatingActionButton(
                    onClick = {
                        when (selectedTab) {
                            0 -> showAddTaskDialog = true
                            1 -> showAddGoalDialog = true
                            2 -> showAddHabitDialog = true
                        }
                    },
                    containerColor = CyanPrimary,
                    contentColor = Color.Black,
                    modifier = Modifier.testTag("add_item_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "افزودن")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = SurfaceDark,
                contentColor = CyanPrimary,
                edgePadding = 12.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = CyanPrimary
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "وظایف (${PersianUtils.formatPersianNumber(tasks.size.toLong())})",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 0) CyanPrimary else TextSecondary,
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier.testTag("tab_tasks")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "اهداف و پروژه‌ها (${PersianUtils.formatPersianNumber(goals.size.toLong())})",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 1) CyanPrimary else TextSecondary,
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier.testTag("tab_goals")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Text(
                            text = "عادت‌ها و تقویم شمسی (${PersianUtils.formatPersianNumber(habits.size.toLong())})",
                            fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 2) CyanPrimary else TextSecondary,
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier.testTag("tab_habits")
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = {
                        Text(
                            text = "پومودورو و تمرکز 🍅",
                            fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 3) CyanPrimary else TextSecondary,
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier.testTag("tab_pomodoro")
                )
            }

            when (selectedTab) {
                // =============================================================
                // TAB 0: کارها و برنامه امروز (Tasks + Linked Debt Installments)
                // =============================================================
                0 -> {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("جستجو در وظایف و اقساط متصل...", color = TextMuted, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .testTag("task_search_field"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceCard,
                            unfocusedContainerColor = SurfaceCard,
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = Color(0x26FFFFFF)
                        ),
                        singleLine = true
                    )

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        item {
                            CategoryChip(
                                title = "همه",
                                selected = selectedCategory == null,
                                onClick = { selectedCategory = null }
                            )
                        }
                        items(TaskCategory.entries) { cat ->
                            CategoryChip(
                                title = cat.titleFa,
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = if (selectedCategory == cat) null else cat }
                            )
                        }
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredTasks, key = { it.id }) { task ->
                            val linkedDebt = debts.find { it.id == task.linkedDebtId }
                            TaskRowItem(
                                task = task,
                                linkedDebt = linkedDebt,
                                onToggle = { onToggleTask(task) },
                                onPostpone = { onPostponeTask(task) },
                                onPayInstallment = { debt ->
                                    val instAmount = maxOf(100_000L, debt.remainingAmount / 4)
                                    onPayLinkedDebtInstallment(debt, instAmount)
                                },
                                onDelete = { onDeleteTask(task.id) }
                            )
                        }
                    }
                }

                // =============================================================
                // TAB 1: اهداف بلندمدت و پروژه‌ها (Goals & Next Action)
                // =============================================================
                1 -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(goals, key = { it.id }) { goal ->
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(0.5.dp, BorderCyanGlow, RoundedCornerShape(20.dp))
                                    .testTag("goal_card_${goal.id}")
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
                                            Icon(Icons.Default.Flag, contentDescription = null, tint = CyanPrimary)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(goal.title, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                                                Text("دسته: ${goal.category.titleFa} • سررسید هدف: ${goal.targetDate}", fontSize = 11.sp, color = TextMuted)
                                            }
                                        }
                                        IconButton(onClick = { onDeleteGoal(goal.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف هدف", tint = TextMuted)
                                        }
                                    }

                                    Text(goal.description, fontSize = 12.sp, color = TextSecondary)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("پیشرفت واقعی:", fontSize = 11.sp, color = TextMuted)
                                        Text("${PersianUtils.formatPersianNumber(goal.progressPercent.toLong())}٪", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldAccent)
                                    }

                                    LinearProgressIndicator(
                                        progress = { goal.progressPercent / 100f },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = EmeraldAccent,
                                        trackColor = SurfaceDark
                                    )

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(SurfaceDark)
                                            .padding(10.dp)
                                    ) {
                                        Text(
                                            text = "🎯 اقدام بعدی: ${goal.nextAction}",
                                            fontSize = 11.sp,
                                            color = CyanPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { onUpdateGoalProgress(goal.id, 10) },
                                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("+۱۰٪ ثبت پیشرفت", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        OutlinedButton(
                                            onClick = { onAddTask(goal.nextAction, goal.category, TaskPriority.HIGH, goal.targetDate) },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("افزودن اقدام به کارهای امروز", color = TextPrimary, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // =============================================================
                // TAB 2: عادت‌ها و تقویم تعاملی شمسی با نقشه حرارتی (Jalali Heatmap)
                // =============================================================
                2 -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        // Interactive 30-Day Jalali Calendar Heatmap Card (v1.2.0 Super-App Feature)
                        item {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(0.5.dp, BorderCyanGlow, RoundedCornerShape(20.dp))
                                    .testTag("jalali_heatmap_card")
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "تقویم تعاملی شمسی و نقشه حرارتی استمرار (آبان ۱۴۰۵)",
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "شدت رنگ هر روز نشان‌دهنده امتیاز تکمیل عادت‌ها و کارهای آن روز است. برای مشاهده جزئیات، روز را لمس کنید:",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )

                                    // 5 rows of 6 days = 30 days of Aban
                                    for (row in 0 until 5) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            for (col in 1..6) {
                                                val dayNum = row * 6 + col
                                                val isSelected = selectedJalaliDay == dayNum
                                                val heatAlpha = when {
                                                    dayNum <= 5 -> 0.85f
                                                    dayNum % 3 == 0 -> 0.55f
                                                    dayNum % 2 == 0 -> 0.30f
                                                    else -> 0.12f
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(34.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(EmeraldAccent.copy(alpha = heatAlpha))
                                                        .border(
                                                            width = if (isSelected) 1.5.dp else 0.dp,
                                                            color = if (isSelected) Color.White else Color.Transparent,
                                                            shape = RoundedCornerShape(8.dp)
                                                        )
                                                        .clickable { selectedJalaliDay = dayNum },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = PersianUtils.formatPersianNumber(dayNum.toLong()),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (heatAlpha > 0.5f) Color.Black else TextPrimary
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Text(
                                        text = "📅 وضعیت ${PersianUtils.formatPersianNumber(selectedJalaliDay.toLong())} آبان ۱۴۰۵: امتیاز استمرار بالا • ${PersianUtils.formatPersianNumber(habits.count { it.completedToday }.toLong())} عادت فعال ثبت‌شده",
                                        fontSize = 11.sp,
                                        color = CyanPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        items(habits, key = { it.id }) { habit ->
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("habit_card_${habit.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(Color(0x2610B981)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Star, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(20.dp))
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = habit.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = "استمرار (Streak): ${PersianUtils.formatPersianNumber(habit.streak.toLong())} روز پیاپی 🔥",
                                                fontSize = 12.sp,
                                                color = EmeraldAccent
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
                                            text = if (habit.completedToday) "انجام شد ✓" else "ثبت برای امروز",
                                            color = if (habit.completedToday) Color.Black else TextPrimary,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // =============================================================
                // TAB 3: تایمر تمرکز و پومودورو (Pomodoro Focus Timer)
                // =============================================================
                3 -> {
                    val mins = pomodoroRemainingSeconds / 60
                    val secs = pomodoroRemainingSeconds % 60
                    val formattedTime = String.format(Locale.US, "%02d:%02d", mins, secs)

                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        item {
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, BorderCyanGlow, RoundedCornerShape(24.dp))
                                    .testTag("pomodoro_timer_card")
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .background(Color(0x2606B6D4)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Timer, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(30.dp))
                                    }

                                    Text(
                                        text = "تایمر تمرکز عمیق و پومودورو",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )

                                    Text(
                                        text = PersianUtils.formatPersianNumber(formattedTime),
                                        fontSize = 46.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isPomodoroRunning) EmeraldAccent else CyanPrimary
                                    )

                                    Text(
                                        text = "جلسات تکمیل‌شده امروز: ${PersianUtils.formatPersianNumber(completedPomodoroSessions.toLong())} پومودورو 🍅",
                                        fontSize = 12.sp,
                                        color = AmberWarning,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Button(
                                            onClick = onTogglePomodoro,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isPomodoroRunning) AmberWarning else EmeraldAccent
                                            ),
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.testTag("pomodoro_start_pause_btn")
                                        ) {
                                            Icon(
                                                imageVector = if (isPomodoroRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = Color.Black
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isPomodoroRunning) "توقف موقت" else "شروع تمرکز",
                                                color = Color.Black,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        OutlinedButton(
                                            onClick = { onResetPomodoro(25) },
                                            shape = RoundedCornerShape(14.dp)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, tint = TextPrimary)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("۲۵ دقیقه", color = TextPrimary)
                                        }

                                        OutlinedButton(
                                            onClick = { onResetPomodoro(5) },
                                            shape = RoundedCornerShape(14.dp)
                                        ) {
                                            Text("استراحت ۵ د", color = CyanPrimary)
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

    // Add Task Dialog
    if (showAddTaskDialog) {
        var taskTitle by remember { mutableStateOf("") }
        var taskCategory by remember { mutableStateOf(TaskCategory.WORK) }
        var taskPriority by remember { mutableStateOf(TaskPriority.MEDIUM) }
        var taskDueDate by remember { mutableStateOf("۱۴۰۵/۰۸/۱۰") }

        AlertDialog(
            onDismissRequest = { showAddTaskDialog = false },
            title = { Text("افزودن وظیفه جدید", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = taskTitle,
                        onValueChange = { taskTitle = it },
                        label = { Text("عنوان وظیفه") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_task_title_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = taskDueDate,
                        onValueChange = { taskDueDate = it },
                        label = { Text("موعد انجام (تاریخ)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Text("اولویت:", fontSize = 12.sp, color = TextMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TaskPriority.entries.forEach { p ->
                            Button(
                                onClick = { taskPriority = p },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (taskPriority == p) CyanPrimary else SurfaceCard
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = p.titleFa,
                                    fontSize = 11.sp,
                                    color = if (taskPriority == p) Color.Black else TextPrimary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (taskTitle.isNotBlank()) {
                            onAddTask(taskTitle, taskCategory, taskPriority, taskDueDate)
                            showAddTaskDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                    modifier = Modifier.testTag("confirm_add_task_btn")
                ) {
                    Text("ثبت وظیفه", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTaskDialog = false }) {
                    Text("انصراف", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // Add Goal Dialog
    if (showAddGoalDialog) {
        var gTitle by remember { mutableStateOf("") }
        var gDesc by remember { mutableStateOf("") }
        var gAction by remember { mutableStateOf("") }
        var gDate by remember { mutableStateOf("۱۴۰۵/۱۲/۲۹") }
        AlertDialog(
            onDismissRequest = { showAddGoalDialog = false },
            title = { Text("تعریف هدف بلندمدت جدید", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = gTitle, onValueChange = { gTitle = it }, label = { Text("عنوان هدف") }, singleLine = true)
                    OutlinedTextField(value = gDesc, onValueChange = { gDesc = it }, label = { Text("معیار موفقیت و چرایی") }, singleLine = true)
                    OutlinedTextField(value = gAction, onValueChange = { gAction = it }, label = { Text("اقدام بعدی (Next Action)") }, singleLine = true)
                    OutlinedTextField(value = gDate, onValueChange = { gDate = it }, label = { Text("تاریخ هدف") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (gTitle.isNotBlank()) {
                            onAddGoal(gTitle, gDesc.ifBlank { "هدف استراتژیک شخصی" }, gDate, gAction.ifBlank { "گام اول اجرایی" }, TaskCategory.PERSONAL)
                            showAddGoalDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                ) {
                    Text("ثبت هدف", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddGoalDialog = false }) {
                    Text("انصراف", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // Add Habit Dialog
    if (showAddHabitDialog) {
        var habitTitle by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddHabitDialog = false },
            title = { Text("عادت جدید برای پایش روزانه", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = habitTitle,
                    onValueChange = { habitTitle = it },
                    label = { Text("عنوان عادت (مثلا: مطالعه شبانه)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_habit_title_input"),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (habitTitle.isNotBlank()) {
                            onAddHabit(habitTitle)
                            showAddHabitDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                    modifier = Modifier.testTag("confirm_add_habit_btn")
                ) {
                    Text("ثبت عادت", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddHabitDialog = false }) {
                    Text("انصراف", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }
}

@Composable
fun TaskRowItem(
    task: TaskItem,
    linkedDebt: DebtItem? = null,
    onToggle: () -> Unit,
    onPostpone: () -> Unit = {},
    onPayInstallment: (DebtItem) -> Unit = {},
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_item_${task.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggle) {
                    Icon(
                        imageVector = if (task.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (task.completed) EmeraldAccent else TextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onToggle() }
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (task.completed) TextMuted else TextPrimary
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        val priorityColor = when (task.priority) {
                            TaskPriority.URGENT -> RoseDanger
                            TaskPriority.HIGH -> AmberWarning
                            TaskPriority.MEDIUM -> CyanPrimary
                            TaskPriority.LOW -> TextSecondary
                        }
                        Text(
                            text = task.priority.titleFa,
                            color = priorityColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text("•", color = TextMuted, fontSize = 11.sp)
                        Text(
                            text = "موعد: ${task.dueDate}",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                        if (task.linkedDebtId != null) {
                            Text("•", color = TextMuted, fontSize = 11.sp)
                            Text(
                                text = "🔗 متصل به بدهی",
                                color = AmberWarning,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                IconButton(onClick = onPostpone, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Schedule, contentDescription = "جابه‌جایی به فردا", tint = CyanPrimary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = TextMuted, modifier = Modifier.size(18.dp))
                }
            }

            // Debt-to-Goal Link Action Row (Section 5 of Roadmap)
            if (linkedDebt != null && !linkedDebt.isSettled && !task.completed) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceDark)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Link, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "مانده بدهی: ${PersianUtils.formatToman(linkedDebt.remainingAmount)}",
                            fontSize = 11.sp,
                            color = AmberWarning,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = { onPayInstallment(linkedDebt) },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("ثبت پرداخت واقعی قسط 💳", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryChip(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) CyanPrimary else SurfaceCard)
            .border(0.5.dp, if (selected) CyanPrimary else Color(0x26FFFFFF), RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text = title,
            color = if (selected) Color.Black else TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
