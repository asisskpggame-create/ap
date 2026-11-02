package com.example.personallifeos.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personallifeos.ui.components.AppBottomNavBar
import com.example.personallifeos.ui.components.NavDestinationItem
import com.example.personallifeos.ui.screens.AddTaskOrTopicChooserSheet
import com.example.personallifeos.ui.screens.AssistantScreen
import com.example.personallifeos.ui.screens.BankVisualSelectorDialog
import com.example.personallifeos.ui.screens.CalendarScreen
import com.example.personallifeos.ui.screens.DashboardScreen
import com.example.personallifeos.ui.screens.ExpensesScreen
import com.example.personallifeos.ui.screens.FloatingReceiptNotificationBanner
import com.example.personallifeos.ui.screens.GoalsScreen
import com.example.personallifeos.ui.screens.HealthScreen
import com.example.personallifeos.ui.screens.MoreHubScreen
import com.example.personallifeos.ui.screens.NotificationCenterDialog
import com.example.personallifeos.ui.screens.SettingsScreen
import com.example.personallifeos.ui.screens.TasksScreen
import com.example.personallifeos.ui.screens.TopicWorkspaceDialog
import com.example.personallifeos.ui.screens.TopicsScreen
import com.example.personallifeos.ui.screens.VaultScreen
import com.example.personallifeos.ui.screens.VoiceCaptureDialog
import com.example.personallifeos.ui.theme.CyanPrimary
import com.example.personallifeos.ui.viewmodel.MainViewModel

/**
 * 5-Pillar Navigation Destinations (as defined in master redesign specification & mockup):
 * 1. Home (خانه)
 * 2. Tasks (وظایف)
 * 3. Topics (تاپیک‌ها)
 * 4. Finance (مالی)
 * 5. More (بیشتر)
 */
sealed class Screen(val route: String, val titleFa: String, val icon: ImageVector) {
    data object Dashboard : Screen("home", "خانه", Icons.Default.Home)
    data object Tasks : Screen("tasks", "وظایف", Icons.Default.CheckCircle)
    data object Topics : Screen("topics", "تاپیک‌ها", Icons.Default.School)
    data object Expenses : Screen("finance", "مالی", Icons.Default.Wallet)
    data object More : Screen("more", "بیشتر", Icons.Default.GridView)
}

val navigationItems = listOf(
    NavDestinationItem("home", "خانه", Icons.Default.Home, CyanPrimary),
    NavDestinationItem("tasks", "وظایف", Icons.Default.CheckCircle, CyanPrimary),
    NavDestinationItem("topics", "تاپیک‌ها", Icons.Default.School, CyanPrimary),
    NavDestinationItem("finance", "مالی", Icons.Default.Wallet, CyanPrimary),
    NavDestinationItem("more", "بیشتر", Icons.Default.GridView, CyanPrimary)
)

@Composable
fun PersonalLifeOsApp(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
    var isVoiceModalOpen by remember { mutableStateOf(false) }
    var isBankSelectorOpen by remember { mutableStateOf(false) }
    var isNotificationCenterOpen by remember { mutableStateOf(false) }
    var isTopicWorkspaceOpen by remember { mutableStateOf(false) }
    var isAddTopicChooserOpen by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 720

    val tasks by viewModel.tasks.collectAsState()
    val topics by viewModel.topics.collectAsState()
    val habits by viewModel.habits.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val debts by viewModel.debts.collectAsState()
    val recurringExpenses by viewModel.recurringExpenses.collectAsState()
    val quickPresets by viewModel.quickPresets.collectAsState()
    val goals by viewModel.goals.collectAsState()
    val categoryBudgets by viewModel.categoryBudgets.collectAsState()
    val reminders by viewModel.reminders.collectAsState()
    val deepLinkTopicId by viewModel.deepLinkTopicId.collectAsState()
    val deepLinkTaskId by viewModel.deepLinkTaskId.collectAsState()
    val dashboardVisibleCards by viewModel.dashboardVisibleCards.collectAsState()
    val pomodoroRemainingSeconds by viewModel.pomodoroRemainingSeconds.collectAsState()
    val isPomodoroRunning by viewModel.isPomodoroRunning.collectAsState()
    val completedPomodoroSessions by viewModel.completedPomodoroSessions.collectAsState()
    val healthLog by viewModel.healthLog.collectAsState()
    val vaultNotes by viewModel.vaultNotes.collectAsState()
    val chatMessages by viewModel.messages.collectAsState()
    val isChatLoading by viewModel.isChatLoading.collectAsState()
    val configuredBanks by viewModel.configuredBanks.collectAsState()
    val pendingReviewSms by viewModel.pendingReviewSms.collectAsState()
    val bankNotifications by viewModel.bankNotifications.collectAsState()
    val notificationSettings by viewModel.notificationSettings.collectAsState()
    val activeFloatingBanner by viewModel.activeFloatingBanner.collectAsState()
    val requestOpenVoiceModal by viewModel.requestOpenVoiceModal.collectAsState()
    val requestedNavigationTab by viewModel.requestedNavigationTab.collectAsState()
    val moreSubscreen by viewModel.moreSubscreen.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    LaunchedEffect(requestOpenVoiceModal) {
        if (requestOpenVoiceModal) {
            isVoiceModalOpen = true
            viewModel.clearVoiceCaptureRequest()
        }
    }

    LaunchedEffect(requestedNavigationTab) {
        requestedNavigationTab?.let { tabIdx ->
            when (tabIdx) {
                0 -> currentScreen = Screen.Dashboard
                1 -> currentScreen = Screen.Tasks
                2 -> currentScreen = Screen.Topics
                3 -> currentScreen = Screen.Expenses
                4 -> currentScreen = Screen.More
            }
            viewModel.clearNavigationTabRequest()
        }
    }

    // Deep Link Navigation
    LaunchedEffect(deepLinkTopicId, deepLinkTaskId) {
        if (!deepLinkTopicId.isNullOrBlank()) {
            currentScreen = Screen.Topics
            isTopicWorkspaceOpen = true
        } else if (!deepLinkTaskId.isNullOrBlank()) {
            currentScreen = Screen.Tasks
        }
    }

    BackHandler(enabled = moreSubscreen != null || currentScreen != Screen.Dashboard) {
        if (moreSubscreen != null) {
            viewModel.navigateToMoreSubscreen(null)
        } else {
            currentScreen = Screen.Dashboard
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (isTablet) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("tablet_nav_rail")
                ) {
                    navigationItems.forEach { item ->
                        val isSelected = when (item.id) {
                            "home" -> currentScreen == Screen.Dashboard
                            "tasks" -> currentScreen == Screen.Tasks
                            "topics" -> currentScreen == Screen.Topics
                            "finance" -> currentScreen == Screen.Expenses
                            "more" -> currentScreen == Screen.More
                            else -> false
                        }
                        NavigationRailItem(
                            selected = isSelected,
                            onClick = {
                                currentScreen = when (item.id) {
                                    "home" -> Screen.Dashboard
                                    "tasks" -> Screen.Tasks
                                    "topics" -> Screen.Topics
                                    "finance" -> Screen.Expenses
                                    else -> Screen.More
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.titleFa) },
                            label = { Text(item.titleFa, fontSize = 11.sp) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            Scaffold(
                bottomBar = {
                    if (!isTablet) {
                        val currentTabId = when (currentScreen) {
                            Screen.Dashboard -> "home"
                            Screen.Tasks -> "tasks"
                            Screen.Topics -> "topics"
                            Screen.Expenses -> "finance"
                            Screen.More -> "more"
                        }
                        AppBottomNavBar(
                            items = navigationItems,
                            selectedId = currentTabId,
                            onItemSelected = { selectedId ->
                                currentScreen = when (selectedId) {
                                    "home" -> Screen.Dashboard
                                    "tasks" -> Screen.Tasks
                                    "topics" -> Screen.Topics
                                    "finance" -> Screen.Expenses
                                    else -> Screen.More
                                }
                            }
                        )
                    }
                },
                floatingActionButton = {
                    if (currentScreen == Screen.Dashboard) {
                        FloatingActionButton(
                            onClick = { isVoiceModalOpen = true },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.Black,
                            shape = CircleShape,
                            modifier = Modifier.testTag("fab_voice_capture")
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "ضبط صوت")
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.background
            ) { paddingValues ->
                Column(modifier = Modifier.padding(paddingValues)) {
                    FloatingReceiptNotificationBanner(
                        activeNotification = activeFloatingBanner,
                        maskSensitiveAmounts = notificationSettings.maskSensitiveAmounts,
                        onDismiss = { viewModel.dismissFloatingBanner() },
                        onOpenNotificationCenter = {
                            viewModel.dismissFloatingBanner()
                            isNotificationCenterOpen = true
                        }
                    )

                    Box(modifier = Modifier.fillMaxSize()) {
                        when (currentScreen) {
                            Screen.Dashboard -> DashboardScreen(
                                tasks = tasks,
                                habits = habits,
                                expenses = expenses,
                                debts = debts,
                                goals = goals,
                                topics = topics,
                                reminders = reminders,
                                visibleCards = dashboardVisibleCards,
                                maskSensitiveAmounts = notificationSettings.maskSensitiveAmounts,
                                healthLog = healthLog,
                                onNavigate = { route ->
                                    when (route) {
                                        "tasks" -> currentScreen = Screen.Tasks
                                        "topics" -> currentScreen = Screen.Topics
                                        "expenses" -> currentScreen = Screen.Expenses
                                        "goals" -> {
                                            currentScreen = Screen.More
                                            viewModel.navigateToMoreSubscreen("goals")
                                        }
                                        "calendar" -> {
                                            currentScreen = Screen.More
                                            viewModel.navigateToMoreSubscreen("calendar")
                                        }
                                        "vault" -> {
                                            currentScreen = Screen.More
                                            viewModel.navigateToMoreSubscreen("vault")
                                        }
                                        "settings" -> {
                                            currentScreen = Screen.More
                                            viewModel.navigateToMoreSubscreen("settings")
                                        }
                                        "reminders" -> isNotificationCenterOpen = true
                                        else -> currentScreen = Screen.Dashboard
                                    }
                                },
                                onOpenTopicFromDashboard = { topicId ->
                                    viewModel.handleNotificationDeepLink(topicId, null)
                                    currentScreen = Screen.Topics
                                    isTopicWorkspaceOpen = true
                                },
                                onOpenVoiceModal = { isVoiceModalOpen = true },
                                onToggleTask = { viewModel.toggleTask(it) },
                                onToggleHabit = { viewModel.toggleHabit(it) },
                                onSnoozeReminder = { remId, mins -> viewModel.snoozeReminder(remId, mins) },
                                onDismissReminder = { viewModel.dismissReminder(it) },
                                onToggleCardVisibility = { viewModel.toggleDashboardCardVisibility(it) }
                            )

                            Screen.Tasks -> TasksScreen(
                                tasks = tasks,
                                habits = habits,
                                goals = goals,
                                debts = debts,
                                topics = topics,
                                initialOpenTopicId = deepLinkTopicId,
                                onConsumeDeepLinkTopic = { viewModel.clearDeepLinkTarget() },
                                pomodoroRemainingSeconds = pomodoroRemainingSeconds,
                                isPomodoroRunning = isPomodoroRunning,
                                completedPomodoroSessions = completedPomodoroSessions,
                                onAddTask = { title, cat, prio, due -> viewModel.addTask(title, cat, prio, due) },
                                onAddAdvancedTask = { title, cat, prio, due, desc, topId, isMajor, remTime, epoch, repMode, subtasks ->
                                    viewModel.addTask(
                                        title = title,
                                        category = cat,
                                        priority = prio,
                                        dueDate = due,
                                        description = desc,
                                        topicId = topId,
                                        isMajorMission = isMajor,
                                        reminderDateTime = remTime,
                                        reminderEpochMillis = epoch,
                                        reminderRepeatMode = repMode,
                                        subtaskTitles = subtasks
                                    )
                                },
                                onToggleTask = { viewModel.toggleTask(it) },
                                onUpdateTaskStatus = { task, status -> viewModel.updateTaskStatus(task, status) },
                                onToggleMajorMission = { viewModel.toggleTaskMajorMission(it) },
                                onAddSubtask = { task, subTitle -> viewModel.addSubtaskToTask(task, subTitle) },
                                onToggleSubtask = { task, subId -> viewModel.toggleSubtaskInTask(task, subId) },
                                onMoveTaskToTopic = { task, newTopId -> viewModel.moveTaskToTopic(task, newTopId) },
                                onTriggerRealNotificationForTask = { viewModel.triggerImmediateTestReminderNotification(it) },
                                onPostponeTask = { viewModel.postponeTaskToTomorrow(it) },
                                onDeleteTask = { viewModel.deleteTask(it) },
                                onPayLinkedDebtInstallment = { debt, amt -> viewModel.recordDebtInstallmentPayment(debt, amt) },
                                onAddTopic = { name, desc, icon, color, start, end, prio, tag, remEn, remTime ->
                                    viewModel.addTopic(
                                        title = name,
                                        description = desc,
                                        iconEmoji = icon,
                                        colorHex = color,
                                        startDate = start,
                                        endDate = end,
                                        priority = prio,
                                        tagOrCategory = tag,
                                        reminderEnabled = remEn,
                                        reminderTime = remTime
                                    )
                                },
                                onCreateTopicFromTemplate = { viewModel.createTopicFromPresetTemplate(it) },
                                onToggleTopicArchive = { viewModel.toggleTopicArchive(it) },
                                onToggleTopicReminder = { viewModel.toggleTopicReminder(it) },
                                onDuplicateTopicAsTemplate = { viewModel.duplicateTopicAsTemplate(it) },
                                onUpdateTopicNote = { topic, note -> viewModel.updateTopicNote(topic, note) },
                                onDeleteTopicConfirmed = { topic, keepTasks -> viewModel.deleteTopic(topic.id, !keepTasks) },
                                onToggleHabit = { viewModel.toggleHabit(it) },
                                onAddHabit = { viewModel.addHabit(it) },
                                onTogglePomodoro = { viewModel.togglePomodoroTimer() },
                                onResetPomodoro = { viewModel.resetPomodoroTimer(it) }
                            )

                            Screen.Topics -> TopicsScreen(
                                topics = topics,
                                tasks = tasks,
                                onOpenTopic = { topicId ->
                                    viewModel.handleNotificationDeepLink(topicId, null)
                                    isTopicWorkspaceOpen = true
                                },
                                onAddNewTopicClick = { isAddTopicChooserOpen = true }
                            )

                            Screen.Expenses -> ExpensesScreen(
                                expenses = expenses,
                                debts = debts,
                                recurringExpenses = recurringExpenses,
                                quickPresets = quickPresets,
                                categoryBudgets = categoryBudgets,
                                configuredBanks = configuredBanks,
                                pendingReviewSms = pendingReviewSms,
                                onAddExpense = { title, amount, cat, bank, isSms, txType, acc ->
                                    viewModel.addExpense(
                                        title = title,
                                        amount = amount,
                                        category = cat,
                                        bankName = bank,
                                        isSms = isSms,
                                        type = txType,
                                        accountName = acc
                                    )
                                },
                                onUpdateExpense = { viewModel.updateExpenseItem(it) },
                                onDeleteExpense = { viewModel.deleteExpense(it) },
                                onTriggerQuickPreset = { viewModel.triggerQuickPreset(it) },
                                onAddQuickPreset = { title, amt, cat, emoji -> viewModel.addQuickPreset(title, amt, cat, emoji) },
                                onAddDebt = { title, person, amt, due, type, count ->
                                    viewModel.addDebt(title, person, amt, due, type, count)
                                },
                                onRecordDebtPayment = { debt, amt -> viewModel.recordDebtInstallmentPayment(debt, amt) },
                                onCreateDebtPlan = { debt, count -> viewModel.createDebtRepaymentPlan(debt, count) },
                                onSettleDebt = { viewModel.settleDebtCompletely(it) },
                                onDeleteDebt = { viewModel.deleteDebt(it) },
                                onAddRecurringExpense = { title, amt, cat, freq, due, note ->
                                    viewModel.addRecurringExpense(title, amt, cat, freq, due, note)
                                },
                                onToggleRecurringExpense = { viewModel.toggleRecurringExpenseActive(it) },
                                onDeleteRecurringExpense = { viewModel.deleteRecurringExpense(it) },
                                onExecuteRecurringExpense = { viewModel.executeRecurringExpenseNow(it) },
                                onParseSms = { sms, sender -> viewModel.parseAndAddSms(sms, sender) },
                                onConfirmReview = { dir, curr, amt -> viewModel.confirmPendingReview(dir, curr, amt) },
                                onDismissReview = { viewModel.dismissPendingReview() },
                                onOpenBankSelector = { isBankSelectorOpen = true },
                                onOpenNotificationCenter = { isNotificationCenterOpen = true }
                            )

                            Screen.More -> {
                                when (moreSubscreen) {
                                    null -> MoreHubScreen(
                                        onNavigateToSubscreen = { sub ->
                                            if (sub == "reminders") {
                                                isNotificationCenterOpen = true
                                            } else {
                                                viewModel.navigateToMoreSubscreen(sub)
                                            }
                                        }
                                    )

                                    "calendar" -> CalendarScreen(
                                        tasks = tasks,
                                        onBack = { viewModel.navigateToMoreSubscreen(null) },
                                        onOpenTask = { /* open task */ }
                                    )

                                    "goals" -> GoalsScreen(
                                        goals = goals,
                                        onBack = { viewModel.navigateToMoreSubscreen(null) },
                                        onAddGoalClick = { /* add goal */ }
                                    )

                                    "habits" -> TasksScreen(
                                        tasks = tasks,
                                        habits = habits,
                                        goals = goals,
                                        debts = debts,
                                        topics = topics,
                                        onAddTask = { t, c, p, d -> viewModel.addTask(t, c, p, d) },
                                        onAddAdvancedTask = { title, cat, prio, due, desc, topId, isMajor, remTime, epoch, repMode, subtasks ->
                                            viewModel.addTask(
                                                title = title,
                                                category = cat,
                                                priority = prio,
                                                dueDate = due,
                                                description = desc,
                                                topicId = topId,
                                                isMajorMission = isMajor,
                                                reminderDateTime = remTime,
                                                reminderEpochMillis = epoch,
                                                reminderRepeatMode = repMode,
                                                subtaskTitles = subtasks
                                            )
                                        },
                                        onToggleTask = { viewModel.toggleTask(it) },
                                        onUpdateTaskStatus = { t, s -> viewModel.updateTaskStatus(t, s) },
                                        onToggleMajorMission = { viewModel.toggleTaskMajorMission(it) },
                                        onAddSubtask = { t, s -> viewModel.addSubtaskToTask(t, s) },
                                        onToggleSubtask = { t, s -> viewModel.toggleSubtaskInTask(t, s) },
                                        onMoveTaskToTopic = { t, n -> viewModel.moveTaskToTopic(t, n) },
                                        onTriggerRealNotificationForTask = { viewModel.triggerImmediateTestReminderNotification(it) },
                                        onPostponeTask = { viewModel.postponeTaskToTomorrow(it) },
                                        onDeleteTask = { viewModel.deleteTask(it) },
                                        onPayLinkedDebtInstallment = { d, a -> viewModel.recordDebtInstallmentPayment(d, a) },
                                        onAddTopic = { n, d, ic, col, st, en, pr, tg, re, rt ->
                                            viewModel.addTopic(
                                                title = n,
                                                description = d,
                                                iconEmoji = ic,
                                                colorHex = col,
                                                startDate = st,
                                                endDate = en,
                                                priority = pr,
                                                tagOrCategory = tg,
                                                reminderEnabled = re,
                                                reminderTime = rt
                                            )
                                        },
                                        onCreateTopicFromTemplate = { viewModel.createTopicFromPresetTemplate(it) },
                                        onToggleTopicArchive = { viewModel.toggleTopicArchive(it) },
                                        onToggleTopicReminder = { viewModel.toggleTopicReminder(it) },
                                        onDuplicateTopicAsTemplate = { viewModel.duplicateTopicAsTemplate(it) },
                                        onUpdateTopicNote = { topic, note -> viewModel.updateTopicNote(topic, note) },
                                        onDeleteTopicConfirmed = { topic, keepTasks -> viewModel.deleteTopic(topic.id, !keepTasks) },
                                        onToggleHabit = { viewModel.toggleHabit(it) },
                                        onAddHabit = { viewModel.addHabit(it) },
                                        onTogglePomodoro = { viewModel.togglePomodoroTimer() },
                                        onResetPomodoro = { viewModel.resetPomodoroTimer(it) }
                                    )

                                    "health" -> HealthScreen(
                                        healthLog = healthLog,
                                        habits = habits,
                                        onToggleHabit = { viewModel.toggleHabit(it) },
                                        onUpdateWater = { viewModel.updateWater(it) },
                                        onUpdateFullMetrics = { sleep, qual, exMin, exType, burned, intake ->
                                            viewModel.updateFullHealthMetrics(sleep, qual, exMin, exType, burned, intake)
                                        },
                                        onRecordScannedFoodOrReceipt = { summary, cal, receiptAmt ->
                                            viewModel.recordScannedFoodOrReceipt(summary, cal, receiptAmt)
                                        }
                                    )

                                    "vault" -> VaultScreen(
                                        vaultNotes = vaultNotes,
                                        onAddNote = { title, content, cat -> viewModel.addVaultNote(title, content, cat) },
                                        onDeleteNote = { viewModel.deleteVaultNote(it) }
                                    )

                                    "assistant" -> AssistantScreen(
                                        messages = chatMessages,
                                        isLoading = isChatLoading,
                                        onSendMessage = { viewModel.sendChatMessage(it) },
                                        onConfirmDraft = { viewModel.confirmAiDraft(it) },
                                        onCancelDraft = { viewModel.cancelAiDraft(it) }
                                    )

                                    "settings" -> SettingsScreen(
                                        notificationSettings = notificationSettings,
                                        currentThemeMode = themeMode,
                                        onSelectThemeMode = { viewModel.setThemeMode(it) },
                                        onUpdateNotificationSettings = { viewModel.updateNotificationSettings(it) },
                                        onOpenNotificationCenter = { isNotificationCenterOpen = true },
                                        onOpenBankSelector = { isBankSelectorOpen = true },
                                        onExportPlainJson = { viewModel.exportBackupJson() },
                                        onExportEncryptedAes = { viewModel.exportEncryptedAesBackup(it) },
                                        onResetData = { viewModel.resetData() }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        VoiceCaptureDialog(
            isOpen = isVoiceModalOpen,
            onDismiss = { isVoiceModalOpen = false },
            onAddExpenseFromSms = { smsText ->
                viewModel.parseAndAddSms(smsText)
            },
            onAddColloquialExpense = { title, amt, cat, txType ->
                viewModel.addExpense(title = title, amount = amt, category = cat, type = txType)
            }
        )

        BankVisualSelectorDialog(
            isOpen = isBankSelectorOpen,
            liveNotifications = bankNotifications,
            onDismiss = { isBankSelectorOpen = false },
            onSaveBankConfig = { bank, sender, reqMatch, customName ->
                viewModel.saveBankConfig(bank, sender, reqMatch, customName)
            }
        )

        NotificationCenterDialog(
            isOpen = isNotificationCenterOpen,
            notifications = bankNotifications,
            settings = notificationSettings,
            onUpdateSettings = { viewModel.updateNotificationSettings(it) },
            onSimulateIncomingSms = { sender, raw -> viewModel.simulateIncomingBankSms(sender, raw) },
            onConfirmNeedsReview = { item, dir -> viewModel.confirmNotificationReview(item, dir) },
            onDismissNotification = { viewModel.dismissNotification(it) },
            onMarkAllRead = { viewModel.markAllNotificationsRead() },
            onOpenBankSelector = { isBankSelectorOpen = true },
            onClose = { isNotificationCenterOpen = false }
        )

        // Topic Workspace Sheet for Tab 3 (Topics)
        val selectedTopic = topics.find { it.id == deepLinkTopicId }
        if (isTopicWorkspaceOpen && selectedTopic != null) {
            TopicWorkspaceDialog(
                topic = selectedTopic,
                allTopics = topics,
                topicTasks = tasks.filter { it.topicId == selectedTopic.id },
                onDismiss = {
                    isTopicWorkspaceOpen = false
                    viewModel.clearDeepLinkTarget()
                },
                onOpenNewTaskForTopic = {
                    isTopicWorkspaceOpen = false
                    currentScreen = Screen.Tasks
                },
                onUpdateTaskStatus = { t, s -> viewModel.updateTaskStatus(t, s) },
                onToggleMajorMission = { viewModel.toggleTaskMajorMission(it) },
                onAddSubtask = { t, s -> viewModel.addSubtaskToTask(t, s) },
                onToggleSubtask = { t, s -> viewModel.toggleSubtaskInTask(t, s) },
                onMoveTaskToTopic = { t, n -> viewModel.moveTaskToTopic(t, n) },
                onTriggerRealNotificationForTask = { viewModel.triggerImmediateTestReminderNotification(it) },
                onDeleteTask = { viewModel.deleteTask(it) },
                onToggleTopicReminder = { viewModel.toggleTopicReminder(it) },
                onToggleTopicArchive = { viewModel.toggleTopicArchive(it) },
                onDuplicateTopicAsTemplate = { viewModel.duplicateTopicAsTemplate(it) },
                onUpdateTopicNote = { topic, note -> viewModel.updateTopicNote(topic, note) },
                onDeleteTopicConfirmed = { topic, keepTasks ->
                    viewModel.deleteTopic(topic.id, !keepTasks)
                    isTopicWorkspaceOpen = false
                    viewModel.clearDeepLinkTarget()
                }
            )
        }

        // Add Task Or Topic Chooser Sheet
        if (isAddTopicChooserOpen) {
            AddTaskOrTopicChooserSheet(
                isOpen = isAddTopicChooserOpen,
                topics = topics,
                onDismiss = { isAddTopicChooserOpen = false },
                onCreateTask = { title, cat, prio, due, desc, topId, isMajor, remTime, epoch, repMode, subtasks ->
                    viewModel.addTask(
                        title = title,
                        category = cat,
                        priority = prio,
                        dueDate = due,
                        description = desc,
                        topicId = topId,
                        isMajorMission = isMajor,
                        reminderDateTime = remTime,
                        reminderEpochMillis = epoch,
                        reminderRepeatMode = repMode,
                        subtaskTitles = subtasks
                    )
                    isAddTopicChooserOpen = false
                },
                onCreateTopic = { name, desc, icon, color, start, end, prio, tag, remEn, remTime ->
                    viewModel.addTopic(
                        title = name,
                        description = desc,
                        iconEmoji = icon,
                        colorHex = color,
                        startDate = start,
                        endDate = end,
                        priority = prio,
                        tagOrCategory = tag,
                        reminderEnabled = remEn,
                        reminderTime = remTime
                    )
                    isAddTopicChooserOpen = false
                },
                onCreateTopicFromTemplate = { template ->
                    viewModel.createTopicFromPresetTemplate(template)
                    isAddTopicChooserOpen = false
                }
            )
        }
    }
}
