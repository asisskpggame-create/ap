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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personallifeos.ui.screens.AssistantScreen
import com.example.personallifeos.ui.screens.BankVisualSelectorDialog
import com.example.personallifeos.ui.screens.DashboardScreen
import com.example.personallifeos.ui.screens.ExpensesScreen
import com.example.personallifeos.ui.screens.FloatingReceiptNotificationBanner
import com.example.personallifeos.ui.screens.HealthScreen
import com.example.personallifeos.ui.screens.NotificationCenterDialog
import com.example.personallifeos.ui.screens.SettingsScreen
import com.example.personallifeos.ui.screens.TasksScreen
import com.example.personallifeos.ui.screens.VaultScreen
import com.example.personallifeos.ui.screens.VoiceCaptureDialog
import com.example.personallifeos.ui.theme.AmberWarning
import com.example.personallifeos.ui.theme.BgDark
import com.example.personallifeos.ui.theme.CyanPrimary
import com.example.personallifeos.ui.theme.EmeraldAccent
import com.example.personallifeos.ui.theme.SurfaceDark
import com.example.personallifeos.ui.theme.TextMuted
import com.example.personallifeos.ui.theme.TextPrimary
import com.example.personallifeos.ui.theme.TextSecondary
import com.example.personallifeos.ui.viewmodel.MainViewModel
import com.example.personallifeos.util.PersianUtils

sealed class Screen(val route: String, val titleFa: String, val icon: ImageVector) {
    data object Expenses : Screen("expenses", "مالی", Icons.Default.Wallet)
    data object Tasks : Screen("tasks", "اهداف و مسیر", Icons.Default.CheckCircle)
    data object Dashboard : Screen("dashboard", "داشبورد", Icons.Default.Dashboard)
    data object Assistant : Screen("assistant", "AI", Icons.Default.SmartToy)
    data object Health : Screen("health", "سلامت", Icons.Default.Favorite)
    data object Vault : Screen("vault", "صندوق امن", Icons.Default.Lock)
    data object Settings : Screen("settings", "تنظیمات", Icons.Default.Settings)
}

// Exact 4-Pillar Core Navigation Order from APP_ROADMAP_FA.md Section 2:
// 1. مالی | 2. اهداف و مسیر | 3. داشبورد | 4. AI
val navigationItems = listOf(
    Screen.Expenses,
    Screen.Tasks,
    Screen.Dashboard,
    Screen.Assistant
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalLifeOsApp(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
    var isVoiceModalOpen by remember { mutableStateOf(false) }
    var isBankSelectorOpen by remember { mutableStateOf(false) }
    var isNotificationCenterOpen by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 720

    val tasks by viewModel.tasks.collectAsState()
    val habits by viewModel.habits.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val debts by viewModel.debts.collectAsState()
    val quickPresets by viewModel.quickPresets.collectAsState()
    val goals by viewModel.goals.collectAsState()
    val categoryBudgets by viewModel.categoryBudgets.collectAsState()
    val reminders by viewModel.reminders.collectAsState()
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

    val unreadCount = bankNotifications.count { !it.isRead }

    BackHandler(enabled = currentScreen != Screen.Dashboard) {
        currentScreen = Screen.Dashboard
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .background(BgDark)
        ) {
            if (isTablet) {
                NavigationRail(
                    containerColor = SurfaceDark,
                    contentColor = CyanPrimary,
                    modifier = Modifier.testTag("tablet_nav_rail")
                ) {
                    navigationItems.forEach { screen ->
                        NavigationRailItem(
                            selected = currentScreen == screen,
                            onClick = { currentScreen = screen },
                            icon = { Icon(screen.icon, contentDescription = screen.titleFa) },
                            label = { Text(screen.titleFa, fontSize = 11.sp) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                selectedTextColor = CyanPrimary,
                                indicatorColor = CyanPrimary,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextMuted
                            )
                        )
                    }
                }
            }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Personal Life OS",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Box(
                                    modifier = Modifier
                                        .padding(start = 6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x2606B6D4))
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "v1.6.0R",
                                        fontSize = 10.sp,
                                        color = CyanPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        },
                        actions = {
                            // Mirza Add Bank Quick Button
                            IconButton(
                                onClick = { isBankSelectorOpen = true },
                                modifier = Modifier.testTag("topbar_add_bank_btn")
                            ) {
                                Icon(
                                    Icons.Default.AccountBalance,
                                    contentDescription = "افزودن بانک جدید",
                                    tint = EmeraldAccent
                                )
                            }

                            // Smart Notification Center Quick Button with Unread Badge
                            IconButton(
                                onClick = { isNotificationCenterOpen = true },
                                modifier = Modifier.testTag("topbar_notifications_btn")
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (unreadCount > 0) {
                                            Badge(containerColor = AmberWarning, contentColor = Color.Black) {
                                                Text(PersianUtils.formatPersianNumber(unreadCount.toLong()), fontSize = 9.sp)
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Notifications,
                                        contentDescription = "مرکز اعلان و فیش‌خوان زنده",
                                        tint = if (unreadCount > 0) AmberWarning else TextSecondary
                                    )
                                }
                            }

                            // Health & Calorie Module Quick Button
                            IconButton(
                                onClick = {
                                    currentScreen = if (currentScreen == Screen.Health) Screen.Dashboard else Screen.Health
                                },
                                modifier = Modifier.testTag("topbar_health_btn")
                            ) {
                                Icon(
                                    Icons.Default.Favorite,
                                    contentDescription = "سلامت و کالری",
                                    tint = if (currentScreen == Screen.Health) Color(0xFFEC4899) else TextSecondary
                                )
                            }

                            // Encrypted Vault Quick Button
                            IconButton(
                                onClick = {
                                    currentScreen = if (currentScreen == Screen.Vault) Screen.Dashboard else Screen.Vault
                                },
                                modifier = Modifier.testTag("topbar_vault_btn")
                            ) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = "صندوق امن",
                                    tint = if (currentScreen == Screen.Vault) CyanPrimary else TextSecondary
                                )
                            }

                            // Settings Quick Button
                            IconButton(
                                onClick = {
                                    currentScreen = if (currentScreen == Screen.Settings) Screen.Dashboard else Screen.Settings
                                },
                                modifier = Modifier.testTag("topbar_settings_btn")
                            ) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = "تنظیمات",
                                    tint = if (currentScreen == Screen.Settings) CyanPrimary else TextSecondary
                                )
                            }

                            // Voice & SMS Quick Button
                            IconButton(
                                onClick = { isVoiceModalOpen = true },
                                modifier = Modifier.testTag("topbar_voice_btn")
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = "ویس‌کپچر", tint = CyanPrimary)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = SurfaceDark,
                            titleContentColor = TextPrimary
                        )
                    )
                },
                bottomBar = {
                    if (!isTablet) {
                        NavigationBar(
                            containerColor = SurfaceDark,
                            tonalElevation = 8.dp,
                            modifier = Modifier.testTag("bottom_nav_bar")
                        ) {
                            navigationItems.forEach { screen ->
                                NavigationBarItem(
                                    selected = currentScreen == screen,
                                    onClick = { currentScreen = screen },
                                    icon = { Icon(screen.icon, contentDescription = screen.titleFa, modifier = Modifier.size(20.dp)) },
                                    label = { Text(screen.titleFa, fontSize = 10.sp) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color.Black,
                                        selectedTextColor = CyanPrimary,
                                        indicatorColor = CyanPrimary,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextMuted
                                    ),
                                    modifier = Modifier.testTag("nav_${screen.route}")
                                )
                            }
                        }
                    }
                },
                floatingActionButton = {
                    if (currentScreen == Screen.Dashboard) {
                        FloatingActionButton(
                            onClick = { isVoiceModalOpen = true },
                            containerColor = CyanPrimary,
                            contentColor = Color.Black,
                            shape = CircleShape,
                            modifier = Modifier.testTag("fab_voice_capture")
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "ضبط صوت")
                        }
                    }
                },
                containerColor = BgDark
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
                                reminders = reminders,
                                visibleCards = dashboardVisibleCards,
                                maskSensitiveAmounts = notificationSettings.maskSensitiveAmounts,
                                healthLog = healthLog,
                                onNavigate = { route ->
                                    currentScreen = when (route) {
                                        "tasks" -> Screen.Tasks
                                        "expenses" -> Screen.Expenses
                                        "assistant" -> Screen.Assistant
                                        "health" -> Screen.Health
                                        "vault" -> Screen.Vault
                                        "settings" -> Screen.Settings
                                        else -> Screen.Dashboard
                                    }
                                },
                                onOpenVoiceModal = { isVoiceModalOpen = true },
                                onToggleTask = { viewModel.toggleTask(it) },
                                onToggleHabit = { viewModel.toggleHabit(it) },
                                onSnoozeReminder = { viewModel.snoozeReminder(it) },
                                onDismissReminder = { viewModel.dismissReminder(it) },
                                onToggleCardVisibility = { viewModel.toggleDashboardCardVisibility(it) }
                            )

                            Screen.Tasks -> TasksScreen(
                                tasks = tasks,
                                habits = habits,
                                goals = goals,
                                debts = debts,
                                pomodoroRemainingSeconds = pomodoroRemainingSeconds,
                                isPomodoroRunning = isPomodoroRunning,
                                completedPomodoroSessions = completedPomodoroSessions,
                                onAddTask = { title, cat, prio, due -> viewModel.addTask(title, cat, prio, due) },
                                onToggleTask = { viewModel.toggleTask(it) },
                                onPostponeTask = { viewModel.postponeTaskToTomorrow(it) },
                                onDeleteTask = { viewModel.deleteTask(it) },
                                onPayLinkedDebtInstallment = { debt, amt -> viewModel.recordDebtInstallmentPayment(debt, amt) },
                                onAddGoal = { title, desc, date, action, cat -> viewModel.addGoal(title, desc, date, action, cat) },
                                onUpdateGoalProgress = { id, delta -> viewModel.updateGoalProgress(id, delta) },
                                onDeleteGoal = { viewModel.deleteGoal(it) },
                                onToggleHabit = { viewModel.toggleHabit(it) },
                                onAddHabit = { viewModel.addHabit(it) },
                                onTogglePomodoro = { viewModel.togglePomodoroTimer() },
                                onResetPomodoro = { viewModel.resetPomodoroTimer(it) }
                            )

                            Screen.Expenses -> ExpensesScreen(
                                expenses = expenses,
                                debts = debts,
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
                                onParseSms = { sms, sender -> viewModel.parseAndAddSms(sms, sender) },
                                onConfirmReview = { dir, curr, amt -> viewModel.confirmPendingReview(dir, curr, amt) },
                                onDismissReview = { viewModel.dismissPendingReview() },
                                onOpenBankSelector = { isBankSelectorOpen = true },
                                onOpenNotificationCenter = { isNotificationCenterOpen = true }
                            )

                            Screen.Health -> HealthScreen(
                                healthLog = healthLog,
                                onUpdateWater = { viewModel.updateWater(it) },
                                onUpdateFullMetrics = { sleep, qual, exMin, exType, burned, intake ->
                                    viewModel.updateFullHealthMetrics(sleep, qual, exMin, exType, burned, intake)
                                },
                                onRecordScannedFoodOrReceipt = { summary, cal, receiptAmt ->
                                    viewModel.recordScannedFoodOrReceipt(summary, cal, receiptAmt)
                                }
                            )

                            Screen.Vault -> VaultScreen(
                                vaultNotes = vaultNotes,
                                onAddNote = { title, content, cat -> viewModel.addVaultNote(title, content, cat) },
                                onDeleteNote = { viewModel.deleteVaultNote(it) }
                            )

                            Screen.Assistant -> AssistantScreen(
                                messages = chatMessages,
                                isLoading = isChatLoading,
                                onSendMessage = { viewModel.sendChatMessage(it) },
                                onConfirmDraft = { viewModel.confirmAiDraft(it) },
                                onCancelDraft = { viewModel.cancelAiDraft(it) }
                            )

                            Screen.Settings -> SettingsScreen(
                                notificationSettings = notificationSettings,
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
            onDismiss = { isBankSelectorOpen = false },
            onSaveBankConfig = { bank, sender, reqMatch ->
                viewModel.saveBankConfig(bank, sender, reqMatch)
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
    }
}
