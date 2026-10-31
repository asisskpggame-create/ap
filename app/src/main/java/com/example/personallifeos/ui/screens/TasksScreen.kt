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
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personallifeos.data.model.DebtItem
import com.example.personallifeos.data.model.GoalItem
import com.example.personallifeos.data.model.HabitItem
import com.example.personallifeos.data.model.ReminderRepeatMode
import com.example.personallifeos.data.model.TaskCategory
import com.example.personallifeos.data.model.TaskItem
import com.example.personallifeos.data.model.TaskPriority
import com.example.personallifeos.data.model.TaskStatus
import com.example.personallifeos.data.model.TopicDashboardStats
import com.example.personallifeos.data.model.TopicItem
import com.example.personallifeos.data.model.TopicPresetTemplate
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
import java.util.Locale

@Composable
fun TasksScreen(
    tasks: List<TaskItem>,
    habits: List<HabitItem>,
    goals: List<GoalItem> = emptyList(),
    debts: List<DebtItem> = emptyList(),
    topics: List<TopicItem> = emptyList(),
    initialOpenTopicId: String? = null,
    onConsumeDeepLinkTopic: () -> Unit = {},
    pomodoroRemainingSeconds: Int = 1500,
    isPomodoroRunning: Boolean = false,
    completedPomodoroSessions: Int = 3,
    onAddTask: (String, TaskCategory, TaskPriority, String) -> Unit,
    onAddAdvancedTask: (
        title: String,
        category: TaskCategory,
        priority: TaskPriority,
        dueDate: String,
        description: String,
        topicId: String?,
        isMajorMission: Boolean,
        reminderDateTime: String,
        reminderEpochMillis: Long,
        reminderRepeatMode: ReminderRepeatMode,
        subtaskTitles: List<String>
    ) -> Unit = { title, cat, prio, due, _, _, _, _, _, _, _ ->
        onAddTask(title, cat, prio, due)
    },
    onToggleTask: (TaskItem) -> Unit,
    onUpdateTaskStatus: (TaskItem, TaskStatus) -> Unit = { t, _ -> onToggleTask(t) },
    onToggleMajorMission: (TaskItem) -> Unit = {},
    onAddSubtask: (TaskItem, String) -> Unit = { _, _ -> },
    onToggleSubtask: (TaskItem, String) -> Unit = { _, _ -> },
    onMoveTaskToTopic: (TaskItem, String?) -> Unit = { _, _ -> },
    onTriggerRealNotificationForTask: (TaskItem) -> Unit = {},
    onPostponeTask: (TaskItem) -> Unit = {},
    onDeleteTask: (String) -> Unit,
    onPayLinkedDebtInstallment: (DebtItem, Long) -> Unit = { _, _ -> },
    onAddTopic: (
        name: String,
        description: String,
        iconEmoji: String,
        colorHex: Long,
        startDate: String,
        endDate: String,
        priority: TaskPriority,
        categoryTag: String,
        reminderEnabled: Boolean,
        reminderTime: String
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _ -> },
    onCreateTopicFromTemplate: (TopicPresetTemplate) -> Unit = {},
    onToggleTopicArchive: (TopicItem) -> Unit = {},
    onToggleTopicReminder: (TopicItem) -> Unit = {},
    onDuplicateTopicAsTemplate: (TopicItem) -> Unit = {},
    onUpdateTopicNote: (TopicItem, String) -> Unit = { _, _ -> },
    onDeleteTopicConfirmed: (TopicItem, Boolean) -> Unit = { _, _ -> },
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
    var taskScopeFilter by remember { mutableStateOf("ALL") } // ALL, INDEPENDENT, TOPIC, TODAY, TOMORROW, OVERDUE

    // Chooser Sheet & Workspace Dialog states
    var showChooserSheet by remember { mutableStateOf(false) }
    var chooserInitialMode by remember { mutableIntStateOf(0) }
    var chooserPreselectedTopicId by remember { mutableStateOf<String?>(null) }
    var activeWorkspaceTopicId by remember { mutableStateOf<String?>(null) }
    var topicPendingDelete by remember { mutableStateOf<TopicItem?>(null) }

    // Topic Filter & Sort states
    var topicArchiveFilter by remember { mutableStateOf("ACTIVE") } // ACTIVE, ARCHIVED, ALL
    var topicSortMode by remember { mutableStateOf("PRIORITY") } // PRIORITY, PROGRESS, DATE

    var showAddHabitDialog by remember { mutableStateOf(false) }
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var selectedJalaliDay by remember { mutableIntStateOf(5) }

    // Handle Deep-Link from Android System Reminder Notification
    LaunchedEffect(initialOpenTopicId) {
        if (!initialOpenTopicId.isNullOrBlank()) {
            activeWorkspaceTopicId = initialOpenTopicId
            onConsumeDeepLinkTopic()
        }
    }

    val filteredTasks = remember(tasks, searchQuery, selectedCategory, taskScopeFilter) {
        tasks.filter { t ->
            val normQ = PersianUtils.normalizePersianText(searchQuery)
            val matchesSearch = normQ.isBlank() ||
                PersianUtils.normalizePersianText(t.title).contains(normQ) ||
                PersianUtils.normalizePersianText(t.description).contains(normQ)
            val matchesCategory = selectedCategory == null || t.category == selectedCategory
            val matchesScope = when (taskScopeFilter) {
                "INDEPENDENT" -> t.topicId == null
                "TOPIC" -> t.topicId != null
                "TODAY" -> t.dueDate == "۱۴۰۵/۰۸/۰۵"
                "TOMORROW" -> t.dueDate == "۱۴۰۵/۰۸/۰۶"
                "OVERDUE" -> !t.completed && t.status != TaskStatus.CANCELLED && t.dueDate < "۱۴۰۵/۰۸/۰۵"
                else -> true
            }
            matchesSearch && matchesCategory && matchesScope
        }
    }

    val filteredAndSortedTopics = remember(topics, tasks, searchQuery, topicArchiveFilter, topicSortMode) {
        val normQ = PersianUtils.normalizePersianText(searchQuery)
        val filtered = topics.filter { topic ->
            val matchesArchive = when (topicArchiveFilter) {
                "ACTIVE" -> !topic.isArchived
                "ARCHIVED" -> topic.isArchived
                else -> true
            }
            val topicTasks = tasks.filter { it.topicId == topic.id }
            val matchesQuery = normQ.isBlank() ||
                PersianUtils.normalizePersianText(topic.name).contains(normQ) ||
                PersianUtils.normalizePersianText(topic.categoryTag).contains(normQ) ||
                topicTasks.any { PersianUtils.normalizePersianText(it.title).contains(normQ) }
            matchesArchive && matchesQuery
        }
        when (topicSortMode) {
            "PROGRESS" -> filtered.sortedByDescending { topic ->
                TopicDashboardStats.compute(topic.id, tasks).progressPercent
            }
            "DATE" -> filtered.sortedBy { it.startDate }
            else -> filtered.sortedBy { it.priority.ordinal }
        }
    }

    Scaffold(
        modifier = modifier.testTag("tasks_screen"),
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (selectedTab) {
                        0, 1 -> {
                            chooserInitialMode = 0
                            chooserPreselectedTopicId = null
                            showChooserSheet = true
                        }
                        2 -> showAddGoalDialog = true
                        3 -> showAddHabitDialog = true
                        else -> {
                            chooserInitialMode = 0
                            showChooserSheet = true
                        }
                    }
                },
                containerColor = CyanPrimary,
                contentColor = Color.Black,
                modifier = Modifier.testTag("add_item_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "افزودن وظیفه یا تاپیک")
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
                            text = "تاپیک‌ها و مأموریت‌ها (${PersianUtils.formatPersianNumber(topics.count { !it.isArchived }.toLong())})",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 1) EmeraldAccent else TextSecondary,
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier.testTag("tab_topics")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Text(
                            text = "اهداف بلندمدت (${PersianUtils.formatPersianNumber(goals.size.toLong())})",
                            fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 2) CyanPrimary else TextSecondary,
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier.testTag("tab_goals")
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = {
                        Text(
                            text = "عادت‌ها و تقویم شمسی (${PersianUtils.formatPersianNumber(habits.size.toLong())})",
                            fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 3) CyanPrimary else TextSecondary,
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier.testTag("tab_habits")
                )
                Tab(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Timer,
                                contentDescription = null,
                                tint = if (selectedTab == 4) CyanPrimary else TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "پومودورو و تمرکز",
                                fontWeight = if (selectedTab == 4) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 4) CyanPrimary else TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    },
                    modifier = Modifier.testTag("tab_pomodoro")
                )
            }

            when (selectedTab) {
                // =============================================================
                // TAB 0: کارها، مأموریت‌ها و برنامه امروز (Independent + Topic Tasks)
                // =============================================================
                0 -> {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("جستجو در وظایف، تاپیک‌ها و اقساط...", color = TextMuted, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
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

                    // Topics Quick Strip inside Tasks Tab with SVG Vector Icons
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x2610B981))
                                    .border(1.dp, EmeraldAccent, RoundedCornerShape(12.dp))
                                    .clickable { selectedTab = 1 }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                                    .testTag("open_topics_center_chip")
                            ) {
                                Icon(
                                    Icons.Default.FolderSpecial,
                                    contentDescription = null,
                                    tint = EmeraldAccent,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "مرکز تاپیک‌ها (${PersianUtils.formatPersianNumber(topics.count { !it.isArchived }.toLong())})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EmeraldAccent
                                )
                            }
                        }
                        items(topics.filter { !it.isArchived }, key = { it.id }) { topic ->
                            val stats = TopicDashboardStats.compute(topic.id, tasks)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceCard)
                                    .border(0.8.dp, Color(topic.colorHex), RoundedCornerShape(12.dp))
                                    .clickable { activeWorkspaceTopicId = topic.id }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                                    .testTag("quick_topic_chip_${topic.id}")
                            ) {
                                AppSvgIcons.TopicSvgIcon(
                                    iconKey = topic.iconEmoji,
                                    tint = Color(topic.colorHex),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "${topic.name} (${PersianUtils.formatPersianNumber(stats.progressPercent.toLong())}٪)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }

                    // Scope & Deadline Filter Chips (Independent, Topic, Today, Tomorrow, Overdue)
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        items(
                            listOf(
                                "ALL" to "همه (${PersianUtils.formatPersianNumber(tasks.size.toLong())})",
                                "INDEPENDENT" to "مستقل (${PersianUtils.formatPersianNumber(tasks.count { it.topicId == null }.toLong())})",
                                "TOPIC" to "متصل به تاپیک (${PersianUtils.formatPersianNumber(tasks.count { it.topicId != null }.toLong())})",
                                "TODAY" to "امروز",
                                "TOMORROW" to "فردا",
                                "OVERDUE" to "عقب‌افتاده (${PersianUtils.formatPersianNumber(tasks.count { !it.completed && it.dueDate < "۱۴۰۵/۰۸/۰۵" }.toLong())})"
                            )
                        ) { (key, label) ->
                            CategoryChip(
                                title = label,
                                selected = taskScopeFilter == key,
                                onClick = { taskScopeFilter = key }
                            )
                        }
                    }

                    // Category Filter Row
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        item {
                            CategoryChip(
                                title = "همه دسته‌ها",
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
                            val parentTopic = topics.find { it.id == task.topicId }
                            TaskRowItem(
                                task = task,
                                parentTopic = parentTopic,
                                linkedDebt = linkedDebt,
                                onToggle = { onToggleTask(task) },
                                onOpenParentTopic = { topic -> activeWorkspaceTopicId = topic.id },
                                onTriggerNotification = { onTriggerRealNotificationForTask(task) },
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
                // TAB 1: مرکز مدیریت تاپیک‌ها (Topics & Mission Management Center)
                // =============================================================
                1 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Search across Topics & Tasks
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("جستجو در نام تاپیک‌ها، برچسب‌ها یا مأموریت‌ها...", color = TextMuted, fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = EmeraldAccent) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("topic_search_field"),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SurfaceCard,
                                unfocusedContainerColor = SurfaceCard,
                                focusedBorderColor = EmeraldAccent,
                                unfocusedBorderColor = Color(0x26FFFFFF)
                            ),
                            singleLine = true
                        )

                        // Filter (Active / Archived / All) + Sort (Priority / Progress / Date) + Quick Add Topic Button
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                CategoryChip(
                                    title = "فعال (${PersianUtils.formatPersianNumber(topics.count { !it.isArchived }.toLong())})",
                                    selected = topicArchiveFilter == "ACTIVE",
                                    onClick = { topicArchiveFilter = "ACTIVE" }
                                )
                            }
                            item {
                                CategoryChip(
                                    title = "آرشیوشده (${PersianUtils.formatPersianNumber(topics.count { it.isArchived }.toLong())})",
                                    selected = topicArchiveFilter == "ARCHIVED",
                                    onClick = { topicArchiveFilter = "ARCHIVED" }
                                )
                            }
                            item {
                                CategoryChip(
                                    title = "همه تاپیک‌ها",
                                    selected = topicArchiveFilter == "ALL",
                                    onClick = { topicArchiveFilter = "ALL" }
                                )
                            }
                            item {
                                val sortLabel = when (topicSortMode) {
                                    "PROGRESS" -> "مرتب‌سازی: پیشرفت"
                                    "DATE" -> "مرتب‌سازی: تاریخ"
                                    else -> "مرتب‌سازی: اولویت"
                                }
                                CategoryChip(
                                    title = sortLabel,
                                    selected = true,
                                    onClick = {
                                        topicSortMode = when (topicSortMode) {
                                            "PRIORITY" -> "PROGRESS"
                                            "PROGRESS" -> "DATE"
                                            else -> "PRIORITY"
                                        }
                                    }
                                )
                            }
                            item {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(EmeraldAccent)
                                        .clickable {
                                            chooserInitialMode = 2
                                            showChooserSheet = true
                                        }
                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                        .testTag("create_topic_direct_btn")
                                ) {
                                    Text(
                                        text = "+ تاپیک یا قالب جدید",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.Black
                                    )
                                }
                            }
                        }

                        // Weekly & Overall Topics Progress Summary Banner
                        val totalTopicTasks = tasks.count { it.topicId != null && it.status != TaskStatus.CANCELLED }
                        val completedTopicTasks = tasks.count { it.topicId != null && (it.completed || it.status == TaskStatus.COMPLETED) }
                        val overallTopicProgress = if (totalTopicTasks > 0) (completedTopicTasks * 100) / totalTopicTasks else 0

                        Card(
                            shape = RoundedCornerShape(18.dp),
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
                                Column {
                                    Text(
                                        text = "آمار کل پیشرفت مأموریت‌های تاپیک‌ها (هفتگی / ماهانه)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "${PersianUtils.formatPersianNumber(completedTopicTasks.toLong())} مأموریت تکمیل‌شده از ${PersianUtils.formatPersianNumber(totalTopicTasks.toLong())} مأموریت ثبت‌شده",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                                Text(
                                    text = "${PersianUtils.formatPersianNumber(overallTopicProgress.toLong())}٪",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EmeraldAccent
                                )
                            }
                        }

                        // Topics List with SVG Vector Icons
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(filteredAndSortedTopics, key = { it.id }) { topic ->
                                val stats = TopicDashboardStats.compute(topic.id, tasks)
                                val brandColor = Color(topic.colorHex)

                                Card(
                                    shape = RoundedCornerShape(22.dp),
                                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, brandColor.copy(alpha = 0.7f), RoundedCornerShape(22.dp))
                                        .clickable { activeWorkspaceTopicId = topic.id }
                                        .testTag("topic_card_${topic.id}")
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(brandColor.copy(alpha = 0.14f), SurfaceCard)
                                                )
                                            )
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
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(46.dp)
                                                        .clip(RoundedCornerShape(14.dp))
                                                        .background(brandColor.copy(alpha = 0.22f))
                                                        .border(1.dp, brandColor, RoundedCornerShape(14.dp)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    AppSvgIcons.TopicSvgIcon(
                                                        iconKey = topic.iconEmoji,
                                                        tint = brandColor,
                                                        contentDescription = topic.name,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                                Column {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(
                                                            text = topic.name,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            fontSize = 15.sp,
                                                            color = TextPrimary
                                                        )
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(brandColor.copy(alpha = 0.2f))
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = topic.priority.titleFa,
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = brandColor
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        text = topic.description,
                                                        fontSize = 11.sp,
                                                        color = TextSecondary
                                                    )
                                                }
                                            }

                                            // Quick Actions on Topic Card
                                            Row {
                                                IconButton(
                                                    onClick = { onDuplicateTopicAsTemplate(topic) },
                                                    modifier = Modifier.size(30.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.ContentCopy,
                                                        contentDescription = "کپی تاپیک به‌عنوان الگو",
                                                        tint = CyanPrimary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { onToggleTopicArchive(topic) },
                                                    modifier = Modifier.size(30.dp)
                                                ) {
                                                    Icon(
                                                        if (topic.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                                        contentDescription = "آرشیو / بازیابی",
                                                        tint = AmberWarning,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { topicPendingDelete = topic },
                                                    modifier = Modifier.size(30.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        contentDescription = "حذف تاپیک",
                                                        tint = RoseDanger,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }

                                        // Progress Row & Real Progress Bar
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "وظایف: ${PersianUtils.formatPersianNumber(stats.completedTasks.toLong())} تکمیل از ${PersianUtils.formatPersianNumber(stats.totalTasks.toLong())} • باقی‌مانده: ${PersianUtils.formatPersianNumber(stats.remainingTasks.toLong())}" +
                                                    (if (stats.overdueTasks > 0) " • ${PersianUtils.formatPersianNumber(stats.overdueTasks.toLong())} عقب‌افتاده" else ""),
                                                fontSize = 11.sp,
                                                color = if (stats.overdueTasks > 0) RoseDanger else TextSecondary
                                            )
                                            Text(
                                                text = "${PersianUtils.formatPersianNumber(stats.progressPercent.toLong())}٪",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = brandColor
                                            )
                                        }

                                        LinearProgressIndicator(
                                            progress = { stats.progressPercent / 100f },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                            color = brandColor,
                                            trackColor = SurfaceDark
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Schedule, contentDescription = null, tint = TextMuted, modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "نزدیک‌ترین مهلت: ${stats.nearestDeadline ?: (topic.endDate ?: "ندارد")}",
                                                    fontSize = 10.sp,
                                                    color = TextMuted
                                                )
                                            }
                                            Text(
                                                text = "ورود به داشبورد تاپیک ←",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = brandColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // =============================================================
                // TAB 2: اهداف بلندمدت و پروژه‌ها (Goals & Next Action)
                // =============================================================
                2 -> {
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

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(SurfaceDark)
                                            .padding(10.dp)
                                    ) {
                                        Icon(Icons.Default.TrackChanges, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "اقدام بعدی: ${goal.nextAction}",
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
                // TAB 3: عادت‌ها و تقویم تعاملی شمسی با نقشه حرارتی (Jalali Heatmap)
                // =============================================================
                3 -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
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
                                        text = "تقویم تعاملی شمسی و نمای تقویمی مأموریت‌ها (آبان ۱۴۰۵)",
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "شدت رنگ هر روز نشان‌دهنده امتیاز تکمیل عادت‌ها و مأموریت‌های آن روز است. برای مشاهده مأموریت‌های هر روز، روی آن لمس کنید:",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )

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

                                    val formattedDay = String.format(Locale.US, "%02d", selectedJalaliDay)
                                    val selectedDateFa = PersianUtils.formatPersianNumber("1405/08/$formattedDay")
                                    val dayTasksCount = tasks.count { it.dueDate == selectedDateFa }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "وضعیت ${PersianUtils.formatPersianNumber(selectedJalaliDay.toLong())} آبان ۱۴۰۵: ${PersianUtils.formatPersianNumber(dayTasksCount.toLong())} مأموریت زمان‌بندی‌شده • ${PersianUtils.formatPersianNumber(habits.count { it.completedToday }.toLong())} عادت فعال",
                                            fontSize = 11.sp,
                                            color = CyanPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
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
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "استمرار (Streak): ${PersianUtils.formatPersianNumber(habit.streak.toLong())} روز پیاپی",
                                                    fontSize = 12.sp,
                                                    color = EmeraldAccent
                                                )
                                            }
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
                                            text = if (habit.completedToday) "انجام شد" else "ثبت برای امروز",
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
                // TAB 4: تایمر تمرکز و پومودورو (Pomodoro Focus Timer)
                // =============================================================
                4 -> {
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
                                        text = "جلسات تکمیل‌شده امروز: ${PersianUtils.formatPersianNumber(completedPomodoroSessions.toLong())} پومودورو",
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

    // 1. Modern Add Button Bottom-Sheet Chooser (New Task vs New Topic)
    AddTaskOrTopicChooserSheet(
        isOpen = showChooserSheet,
        topics = topics,
        initialSelectedTopicId = chooserPreselectedTopicId,
        initialMode = chooserInitialMode,
        onDismiss = {
            showChooserSheet = false
            chooserPreselectedTopicId = null
        },
        onCreateTask = { title, cat, prio, due, desc, topId, isMajor, remTime, remEpoch, repMode, subtasks ->
            onAddAdvancedTask(title, cat, prio, due, desc, topId, isMajor, remTime, remEpoch, repMode, subtasks)
        },
        onCreateTopic = { name, desc, icon, color, start, end, prio, tag, remEn, remTime ->
            onAddTopic(name, desc, icon, color, start, end, prio, tag, remEn, remTime)
        },
        onCreateTopicFromTemplate = { tpl ->
            onCreateTopicFromTemplate(tpl)
        }
    )

    // 2. Dedicated Topic Workspace & Mini-Dashboard Dialog
    val activeTopic = topics.find { it.id == activeWorkspaceTopicId }
    if (activeTopic != null) {
        val topicTasks = tasks.filter { it.topicId == activeTopic.id }
        TopicWorkspaceDialog(
            topic = activeTopic,
            allTopics = topics,
            topicTasks = topicTasks,
            onDismiss = { activeWorkspaceTopicId = null },
            onOpenNewTaskForTopic = { topId ->
                chooserPreselectedTopicId = topId
                chooserInitialMode = 1
                showChooserSheet = true
            },
            onUpdateTaskStatus = onUpdateTaskStatus,
            onToggleMajorMission = onToggleMajorMission,
            onAddSubtask = onAddSubtask,
            onToggleSubtask = onToggleSubtask,
            onMoveTaskToTopic = onMoveTaskToTopic,
            onTriggerRealNotificationForTask = onTriggerRealNotificationForTask,
            onDeleteTask = onDeleteTask,
            onToggleTopicReminder = onToggleTopicReminder,
            onToggleTopicArchive = onToggleTopicArchive,
            onDuplicateTopicAsTemplate = onDuplicateTopicAsTemplate,
            onUpdateTopicNote = onUpdateTopicNote,
            onDeleteTopicConfirmed = onDeleteTopicConfirmed
        )
    }

    // Topic Delete Confirmation from Topics Center
    topicPendingDelete?.let { delTopic ->
        val count = tasks.count { it.topicId == delTopic.id }
        AlertDialog(
            onDismissRequest = { topicPendingDelete = null },
            title = { Text("تأیید حذف تاپیک «${delTopic.name}»", fontWeight = FontWeight.Bold, color = RoseDanger) },
            text = {
                Text(
                    text = "آیا از حذف این تاپیک اطمینان دارید؟ (${PersianUtils.formatPersianNumber(count.toLong())} وظیفه وابسته)",
                    fontSize = 12.sp,
                    color = TextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTopicConfirmed(delTopic, false)
                        topicPendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                ) {
                    Text("حذف تاپیک (حفظ وظایف به‌صورت مستقل)", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(
                        onClick = {
                            onDeleteTopicConfirmed(delTopic, true)
                            topicPendingDelete = null
                        }
                    ) {
                        Text("حذف کامل با وظایف", color = RoseDanger, fontSize = 11.sp)
                    }
                    TextButton(onClick = { topicPendingDelete = null }) {
                        Text("انصراف", color = TextSecondary, fontSize = 11.sp)
                    }
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
    parentTopic: TopicItem? = null,
    linkedDebt: DebtItem? = null,
    onToggle: () -> Unit,
    onOpenParentTopic: (TopicItem) -> Unit = {},
    onTriggerNotification: () -> Unit = {},
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
                        .clickable {
                            if (parentTopic != null) onOpenParentTopic(parentTopic) else onToggle()
                        }
                ) {
                    // Topic SVG Badge vs Independent SVG Badge Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 3.dp)
                    ) {
                        if (parentTopic != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(parentTopic.colorHex).copy(alpha = 0.2f))
                                    .clickable { onOpenParentTopic(parentTopic) }
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                AppSvgIcons.TopicSvgIcon(
                                    iconKey = parentTopic.iconEmoji,
                                    tint = Color(parentTopic.colorHex),
                                    modifier = Modifier.size(12.dp)
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x1FFFFFFF))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
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

                        Text(
                            text = "• ${task.status.titleFa}",
                            fontSize = 10.sp,
                            color = when (task.status) {
                                TaskStatus.COMPLETED -> EmeraldAccent
                                TaskStatus.IN_PROGRESS -> CyanPrimary
                                TaskStatus.CANCELLED -> RoseDanger
                                TaskStatus.TODO -> TextMuted
                            }
                        )
                    }

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
                            color = if (!task.completed && task.dueDate < "۱۴۰۵/۰۸/۰۵") RoseDanger else TextMuted,
                            fontSize = 11.sp
                        )
                        if (task.subtasks.isNotEmpty()) {
                            val doneSub = task.subtasks.count { it.isCompleted }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Checklist, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "${PersianUtils.formatPersianNumber(doneSub.toLong())}/${PersianUtils.formatPersianNumber(task.subtasks.size.toLong())}",
                                    color = CyanPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (task.linkedDebtId != null) {
                            Text("•", color = TextMuted, fontSize = 11.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Link, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "متصل به بدهی",
                                    color = AmberWarning,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                IconButton(onClick = onTriggerNotification, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = "ارسال اعلان یادآوری", tint = EmeraldAccent, modifier = Modifier.size(18.dp))
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
                        Icon(Icons.Default.CreditCard, contentDescription = null, tint = Color.Black, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ثبت پرداخت واقعی قسط", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
