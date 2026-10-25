package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import com.example.ui.theme.GlassTheme
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.GlassTokens
import com.example.ui.theme.GlassmorphismDefaults
import com.example.ui.theme.LiquidGlassBackdrop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.firebase.authStateFlow
import com.example.data.firebase.configureAppCheck
import com.example.data.firebase.onGoogleSignInClicked
import com.example.data.firebase.safeAuthStateFlow
import com.example.data.firebase.safeCurrentUser
import com.example.data.firebase.signOut
import com.example.data.local.LifeOsDatabase
import com.example.data.local.PreferencesDataStore
import com.example.data.model.ConsumptionEntry
import com.example.data.model.ExpenseEntry
import com.example.data.model.Task
import com.example.data.repository.LifeOsRepository
import com.example.notifications.QuickVoiceTileService
import com.example.notifications.ReminderScheduler
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.DailyLogAndHealthScreen
import com.example.ui.screens.FirstRunOnboardingDialog
import com.example.ui.screens.HabitsScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.screens.VoiceCaptureModalSheet
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ElectricTeal
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.PersonalLifeOsTheme
import com.example.ui.theme.RoseError
import com.example.ui.viewmodel.LifeOsViewModel
import com.example.ui.viewmodel.UiState
import com.example.util.PersianDateAndUnitUtils
import com.example.util.WorldClassOsUtils
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.util.Locale

enum class AppSection(
    val route: String,
    val titleFa: String,
    val icon: ImageVector,
    val isPrimaryBottomNav: Boolean
) {
    HOME("HOME", "خانه", Icons.Default.Home, true),
    TASKS("TASKS", "تسک‌ها", Icons.Default.CheckCircle, true),
    DAILY_LOG("DAILY_LOG", "ثبت روزانه", Icons.Default.EditCalendar, true),
    HABITS("HABITS", "عادت‌ها", Icons.Default.Repeat, true),
    AI_ASSISTANT("AI_ASSISTANT", "Gemini", Icons.Default.AutoAwesome, true),
    HISTORY("HISTORY", "تاریخچه", Icons.Default.History, false),
    REPORTS("REPORTS", "گزارش‌ها", Icons.Default.Analytics, false),
    SETTINGS("SETTINGS", "تنظیمات", Icons.Default.Settings, false)
}

class MainActivity : ComponentActivity() {
    private val launchVoiceQuickTrigger = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        ReminderScheduler.scheduleSixHourPeriodicSync(this)
        if (intent?.action == QuickVoiceTileService.ACTION_QUICK_VOICE) {
            launchVoiceQuickTrigger.value = true
        }
        enableEdgeToEdge()
        setContent {
            PersonalLifeOSAppRoot(
                openVoiceInitially = launchVoiceQuickTrigger.value,
                onConsumedVoiceTrigger = { launchVoiceQuickTrigger.value = false }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == QuickVoiceTileService.ACTION_QUICK_VOICE) {
            launchVoiceQuickTrigger.value = true
        }
    }
}

@Composable
fun PersonalLifeOSAppRoot(
    openVoiceInitially: Boolean = false,
    onConsumedVoiceTrigger: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentUser by remember(context) { safeAuthStateFlow(context) }.collectAsStateWithLifecycle(
        initialValue = safeCurrentUser(context)
    )
    val uid = currentUser?.uid ?: LifeOsDatabase.LOCAL_DEVICE_USER_ID
    val vm: LifeOsViewModel = viewModel(
        key = "life_os_vm_$uid",
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LifeOsViewModel(
                    repository = LifeOsRepository(context.applicationContext),
                    prefsStore = PreferencesDataStore(context.applicationContext),
                    currentUserId = uid
                ) as T
            }
        }
    )

    val prefs by vm.preferencesState.collectAsStateWithLifecycle()

    PersonalLifeOsTheme(
        darkTheme = prefs.darkTheme,
        themePalette = prefs.themePalette,
        isRtl = prefs.language == "fa"
    ) {
        AuthenticatedLifeOsShell(
            userEmail = currentUser?.email,
            vm = vm,
            openVoiceInitially = openVoiceInitially,
            onConsumedVoiceTrigger = onConsumedVoiceTrigger
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticatedLifeOsShell(
    userEmail: String?,
    vm: LifeOsViewModel,
    openVoiceInitially: Boolean = false,
    onConsumedVoiceTrigger: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) {
        try {
            CredentialManager.create(context)
        } catch (_: Throwable) {
            null
        }
    }

    var currentSectionRoute by rememberSaveable { mutableStateOf(AppSection.HOME.route) }
    var showVoiceModal by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(openVoiceInitially) {
        if (openVoiceInitially) {
            showVoiceModal = true
            onConsumedVoiceTrigger()
        }
    }

    val prefs by vm.preferencesState.collectAsStateWithLifecycle()
    val tasksUiState by vm.tasksState.collectAsStateWithLifecycle()
    val consumptionUiState by vm.consumptionState.collectAsStateWithLifecycle()
    val expensesUiState by vm.expensesState.collectAsStateWithLifecycle()
    val customItems by vm.customItemsState.collectAsStateWithLifecycle()
    val habits by vm.habitsState.collectAsStateWithLifecycle()
    val habitRecords by vm.habitRecordsState.collectAsStateWithLifecycle()
    val sleepRecords by vm.sleepRecordsState.collectAsStateWithLifecycle()
    val exerciseRecords by vm.exerciseRecordsState.collectAsStateWithLifecycle()
    val activityEvents by vm.activityEventsState.collectAsStateWithLifecycle()
    val conversations by vm.conversationsState.collectAsStateWithLifecycle()
    val isAiProcessing by vm.isAiProcessing.collectAsStateWithLifecycle()
    val pendingAiPreview by vm.pendingAiPreview.collectAsStateWithLifecycle()
    val snackbarMessage by vm.snackbarMessage.collectAsStateWithLifecycle()
    val canUndo by vm.canUndo.collectAsStateWithLifecycle()
    val pendingSyncCount by vm.pendingSyncCountState.collectAsStateWithLifecycle()
    val lastSyncMillis by vm.lastSyncMillisState.collectAsStateWithLifecycle()
    val isSyncing by vm.isSyncingState.collectAsStateWithLifecycle()

    // Focus Mode & Pomodoro Timer State
    val activeFocusTask by vm.activeFocusTask.collectAsStateWithLifecycle()
    val focusElapsedSeconds by vm.focusElapsedSeconds.collectAsStateWithLifecycle()
    val isFocusPomodoro by vm.isFocusPomodoro.collectAsStateWithLifecycle()

    val tasks: List<Task> = (tasksUiState as? UiState.Success)?.data.orEmpty()
    val consumption: List<ConsumptionEntry> = (consumptionUiState as? UiState.Success)?.data.orEmpty()
    val expenses: List<ExpenseEntry> = (expensesUiState as? UiState.Success)?.data.orEmpty()

    val snackbarHostState = remember { SnackbarHostState() }

    // Automatic 6-hour data transfer check
    LaunchedEffect(lastSyncMillis, pendingSyncCount) {
        if (pendingSyncCount > 0) {
            val elapsed = System.currentTimeMillis() - lastSyncMillis
            if (elapsed >= ReminderScheduler.SIX_HOURS_MILLIS) {
                vm.triggerSixHourDataTransfer(context, isAutomatic = true)
            }
        }
    }

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = msg,
            actionLabel = if (canUndo) "بازگردانی" else null,
            duration = SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed && canUndo) {
            vm.triggerUndo()
        }
        vm.clearSnackbar()
    }

    BackHandler(enabled = currentSectionRoute != AppSection.HOME.route) {
        currentSectionRoute = AppSection.HOME.route
    }

    if (!prefs.onboardingCompleted) {
        FirstRunOnboardingDialog(
            currentPrefs = prefs,
            onComplete = { updatedPrefs, addStarterHabits ->
                vm.updatePreferences(updatedPrefs)
                if (addStarterHabits && habits.isEmpty()) {
                    vm.createHabit("مطالعه روزانه (۲۰ دقیقه)", "شخصی", "DAILY", listOf(1, 2, 3, 4, 5, 6, 7), 1)
                    vm.createHabit("ورزش و تحرک", "سلامت", "TIMES_PER_WEEK", listOf(1, 2, 3, 4, 5, 6, 7), 3)
                    vm.createHabit("خواب منظم قبل از ساعت ۲۳", "سلامت", "DAILY", listOf(1, 2, 3, 4, 5, 6, 7), 1)
                }
            }
        )
    }

    if (showVoiceModal) {
        VoiceCaptureModalSheet(
            isProcessing = isAiProcessing,
            pendingPreview = pendingAiPreview,
            onAnalyzeTranscript = { text ->
                vm.analyzeVoiceOrChatCommand(text, "VOICE")
            },
            onConfirmOperations = { ops, raw ->
                vm.confirmAndExecuteOperations(ops, raw, "VOICE")
                showVoiceModal = false
            },
            onRejectPreview = { raw ->
                vm.dismissPendingAiPreview(raw, "VOICE")
            },
            onDismissSheet = {
                showVoiceModal = false
            }
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideTablet = maxWidth >= 720.dp

        val activePendingTasksCount = tasks.count { it.status != "COMPLETED" && it.status != "ARCHIVED" }

        LiquidGlassBackdrop(
            modifier = Modifier.fillMaxSize(),
            primaryGlow = MaterialTheme.colorScheme.primary,
            secondaryGlow = MaterialTheme.colorScheme.secondary
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Circular 4-dot Grid Button with Specular Liquid Rim
                                GlassSurface(
                                    shape = CircleShape,
                                    color = Color(0x33FFFFFF),
                                    accentBorderColor = MaterialTheme.colorScheme.primary,
                                    borderWidth = GlassTheme.tokens.subtleBorderWidth,
                                    onClick = {
                                        WorldClassOsUtils.HapticSignature.crispTick(context)
                                        currentSectionRoute = AppSection.HOME.route
                                    },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(
                                            imageVector = Icons.Default.GridView,
                                            contentDescription = "داشبورد",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                // "X Active Tasks" Frosted Capsule Pill with glowing dot
                                GlassSurface(
                                    shape = GlassTheme.tokens.pillShape,
                                    color = Color(0x24FFFFFF),
                                    accentBorderColor = MaterialTheme.colorScheme.primary,
                                    borderWidth = GlassTheme.tokens.subtleBorderWidth,
                                    onClick = {
                                        WorldClassOsUtils.HapticSignature.crispTick(context)
                                        currentSectionRoute = AppSection.TASKS.route
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(7.dp)
                                        ) {}
                                        Text(
                                            text = "${PersianDateAndUnitUtils.toPersianDigits(activePendingTasksCount.toString())} تسک فعال",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        },
                        actions = {
                            Row(
                                modifier = Modifier.padding(end = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                GlassSurface(
                                    shape = CircleShape,
                                    color = Color(0x28FFFFFF),
                                    accentBorderColor = if (currentSectionRoute == AppSection.REPORTS.route) MaterialTheme.colorScheme.primary else null,
                                    borderWidth = GlassTheme.tokens.subtleBorderWidth,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    IconButton(
                                        onClick = { currentSectionRoute = AppSection.REPORTS.route },
                                        modifier = Modifier
                                            .size(38.dp)
                                            .testTag("nav_reports_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Analytics,
                                            contentDescription = "گزارش‌ها",
                                            tint = if (currentSectionRoute == AppSection.REPORTS.route) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                GlassSurface(
                                    shape = CircleShape,
                                    color = Color(0x28FFFFFF),
                                    accentBorderColor = if (currentSectionRoute == AppSection.HISTORY.route) MaterialTheme.colorScheme.primary else null,
                                    borderWidth = GlassTheme.tokens.subtleBorderWidth,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    IconButton(
                                        onClick = { currentSectionRoute = AppSection.HISTORY.route },
                                        modifier = Modifier
                                            .size(38.dp)
                                            .testTag("nav_history_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = "تاریخچه فعالیت‌ها",
                                            tint = if (currentSectionRoute == AppSection.HISTORY.route) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                GlassSurface(
                                    shape = CircleShape,
                                    color = Color(0x28FFFFFF),
                                    accentBorderColor = if (currentSectionRoute == AppSection.SETTINGS.route) MaterialTheme.colorScheme.primary else null,
                                    borderWidth = GlassTheme.tokens.subtleBorderWidth,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    IconButton(
                                        onClick = { currentSectionRoute = AppSection.SETTINGS.route },
                                        modifier = Modifier
                                            .size(38.dp)
                                            .testTag("nav_settings_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = "تنظیمات",
                                            tint = if (currentSectionRoute == AppSection.SETTINGS.route) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                },
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        onClick = {
                            WorldClassOsUtils.HapticSignature.crispTick(context)
                            showVoiceModal = true
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "ثبت صوتی هوشمند"
                            )
                        },
                        text = {
                            Text(
                                text = "فرمان صوتی",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = CircleShape,
                        modifier = Modifier
                            .border(
                                BorderStroke(
                                    1.2.dp,
                                    GlassmorphismDefaults.specularBorderBrush(Color.White, topIntensity = 0.75f)
                                ),
                                CircleShape
                            )
                            .testTag("voice_capture_fab")
                    )
                },
                bottomBar = {
                    if (!isWideTablet) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            GlassSurface(
                                shape = GlassTheme.tokens.dockShape,
                                color = Color(0xCC0D1410),
                                border = BorderStroke(
                                    GlassTheme.tokens.specularBorderWidth,
                                    GlassTheme.specularBorderBrush(null, topIntensity = 0.5f)
                                ),
                                tonalElevation = 0.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box {
                                    // Top Liquid Gloss Sheen reflection
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .background(GlassmorphismDefaults.GlossSheenBrush)
                                    )
                                    NavigationBar(
                                        containerColor = Color.Transparent,
                                        tonalElevation = 0.dp,
                                        windowInsets = WindowInsets(0, 0, 0, 0)
                                    ) {
                                        AppSection.entries.filter { it.isPrimaryBottomNav }.forEach { section ->
                                            val isSelected = currentSectionRoute == section.route
                                            NavigationBarItem(
                                                selected = isSelected,
                                                onClick = {
                                                    WorldClassOsUtils.HapticSignature.crispTick(context)
                                                    currentSectionRoute = section.route
                                                },
                                                icon = {
                                                    Icon(
                                                        imageVector = section.icon,
                                                        contentDescription = section.titleFa
                                                    )
                                                },
                                                label = {
                                                    Text(
                                                        text = section.titleFa,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                },
                                                colors = NavigationBarItemDefaults.colors(
                                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                ),
                                                modifier = Modifier.testTag("bottom_nav_${section.route.lowercase()}")
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideTablet) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        AppSection.entries.forEach { section ->
                            NavigationRailItem(
                                selected = currentSectionRoute == section.route,
                                onClick = { currentSectionRoute = section.route },
                                icon = {
                                    Icon(
                                        imageVector = section.icon,
                                        contentDescription = section.titleFa
                                    )
                                },
                                label = { Text(section.titleFa) },
                                modifier = Modifier.testTag("rail_nav_${section.route.lowercase()}")
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    // Persistent Floating Focus / Pomodoro Bar when a Task Timer is running
                    if (activeFocusTask != null) {
                        FloatingFocusTimerBar(
                            task = activeFocusTask!!,
                            elapsedSeconds = focusElapsedSeconds,
                            isPomodoro = isFocusPomodoro,
                            onToggleMode = { vm.toggleFocusTimerMode() },
                            onStopAndSave = { completeTaskNow ->
                                WorldClassOsUtils.HapticSignature.heavyImpact(context)
                                vm.stopAndSaveFocusTimer(completeTaskNow)
                            }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        when (currentSectionRoute) {
                            AppSection.HOME.route -> HomeDashboardScreen(
                                userEmail = userEmail,
                                prefs = prefs,
                                tasks = tasks,
                                consumption = consumption,
                                customItems = customItems,
                                expenses = expenses,
                                habits = habits,
                                habitRecords = habitRecords,
                                sleepRecords = sleepRecords,
                                exerciseRecords = exerciseRecords,
                                activityEvents = activityEvents,
                                onUpdatePrefs = { vm.updatePreferences(it) },
                                onCompleteTask = { vm.completeTask(it) },
                                onStartFocusTimer = { task -> vm.startFocusTimer(task) },
                                onToggleHabitDay = { habit, day -> vm.toggleHabitForDay(habit, day) },
                                onQuickAddWater = { ml ->
                                    vm.quickAddConsumption("آب", "WATER", ml, "ml")
                                },
                                onQuickAddCoffee = {
                                    vm.quickAddConsumption("قهوه", "COFFEE", 1.0, "فنجان")
                                },
                                onQuickAddPresetItem = { preset ->
                                    vm.quickAddConsumption(
                                        itemName = preset.name,
                                        category = preset.category,
                                        quantity = preset.defaultQuantity,
                                        unit = preset.unit,
                                        optionalExpenseAmount = preset.defaultPrice,
                                        optionalCurrency = preset.defaultCurrency
                                    )
                                },
                                onCreateCustomItem = { name, cat, unit, defQty, target, price ->
                                    vm.createCustomConsumptionItem(name, cat, unit, defQty, target, price)
                                },
                                onUpdateCustomItem = { updatedItem ->
                                    vm.updateCustomConsumptionItem(updatedItem)
                                },
                                onDeleteCustomItem = { itemToDelete ->
                                    vm.deleteCustomConsumptionItem(itemToDelete)
                                },
                                onOpenVoiceCapture = { showVoiceModal = true },
                                onNavigateToSection = { route -> currentSectionRoute = route }
                            )

                            AppSection.TASKS.route -> TasksScreen(
                                tasks = tasks,
                                calendarDisplay = prefs.calendarDisplay,
                                activeFocusTaskId = activeFocusTask?.id,
                                onStartFocusTimer = { task -> vm.startFocusTimer(task) },
                                onCreateTask = { title, desc, cat, tags, prio, dueDay, dueTime, remMillis, estDur, subtasksJson, recRule, recInterval, notes ->
                                    vm.createTask(
                                        title = title,
                                        description = desc,
                                        category = cat,
                                        tags = tags,
                                        priority = prio,
                                        dueDateEpochDay = dueDay,
                                        dueTimeMinutes = dueTime,
                                        reminderAtMillis = remMillis,
                                        estimatedDurationMinutes = estDur,
                                        subtasksJson = subtasksJson,
                                        recurrenceRule = recRule,
                                        recurrenceIntervalDays = recInterval,
                                        notes = notes
                                    )
                                },
                                onCompleteTask = { vm.completeTask(it) },
                                onBulkComplete = { vm.bulkCompleteTasks(it) },
                                onUpdateTask = { vm.updateTask(it) },
                                onRescheduleTask = { task, newDay -> vm.rescheduleTask(task, newDay) },
                                onArchiveOrRestore = { task, arch -> vm.archiveOrRestoreTask(task, arch) },
                                onDeleteTask = { vm.deleteTask(it) }
                            )

                            AppSection.DAILY_LOG.route -> DailyLogAndHealthScreen(
                                prefs = prefs,
                                consumption = consumption,
                                customItems = customItems,
                                expenses = expenses,
                                sleepRecords = sleepRecords,
                                exerciseRecords = exerciseRecords,
                                onUpdatePrefs = { vm.updatePreferences(it) },
                                onQuickAddConsumption = { name, cat, qty, unit, notes, price, curr ->
                                    vm.quickAddConsumption(name, cat, qty, unit, notes, price, curr)
                                },
                                onDeleteConsumption = { vm.deleteConsumption(it) },
                                onCreateCustomItem = { name, cat, unit, defQty, target, price ->
                                    vm.createCustomConsumptionItem(name, cat, unit, defQty, target, price)
                                },
                                onUpdateCustomItem = { updatedItem ->
                                    vm.updateCustomConsumptionItem(updatedItem)
                                },
                                onDeleteCustomItem = { itemToDelete ->
                                    vm.deleteCustomConsumptionItem(itemToDelete)
                                },
                                onAddExpense = { title, cat, amount, curr, notes, txType ->
                                    vm.addExpense(title, cat, amount, curr, notes, txType)
                                },
                                onDeleteExpense = { vm.deleteExpense(it) },
                                onAddSleep = { dur, qual, notes ->
                                    vm.addSleepRecord(dur, qual, notes)
                                },
                                onAddExercise = { actType, dur, intensity, dist, reps, notes ->
                                    vm.addExerciseRecord(actType, dur, intensity, dist, reps, notes)
                                }
                            )

                            AppSection.HABITS.route -> HabitsScreen(
                                prefs = prefs,
                                habits = habits,
                                habitRecords = habitRecords,
                                onCreateHabit = { title, cat, schedType, schedDays, target ->
                                    vm.createHabit(title, cat, schedType, schedDays, target)
                                },
                                onToggleHabitDay = { habit, day ->
                                    vm.toggleHabitForDay(habit, day)
                                }
                            )

                            AppSection.AI_ASSISTANT.route -> AiAssistantScreen(
                                conversations = conversations,
                                isProcessing = isAiProcessing,
                                pendingPreview = pendingAiPreview,
                                onSendPrompt = { prompt ->
                                    vm.analyzeVoiceOrChatCommand(prompt, "AI_ASSISTANT")
                                },
                                onConfirmOperations = { ops, raw ->
                                    vm.confirmAndExecuteOperations(ops, raw, "AI_ASSISTANT")
                                },
                                onRejectPreview = { raw ->
                                    vm.dismissPendingAiPreview(raw, "AI_ASSISTANT")
                                },
                                onOpenVoiceSheet = { showVoiceModal = true }
                            )

                            AppSection.HISTORY.route -> HistoryScreen(
                                prefs = prefs,
                                activityEvents = activityEvents
                            )

                            AppSection.REPORTS.route -> ReportsScreen(
                                prefs = prefs,
                                tasks = tasks,
                                consumption = consumption,
                                expenses = expenses,
                                habits = habits,
                                habitRecords = habitRecords,
                                sleepRecords = sleepRecords,
                                exerciseRecords = exerciseRecords
                            )

                            AppSection.SETTINGS.route -> SettingsScreen(
                                userEmail = userEmail,
                                userId = vm.currentUserId,
                                prefs = prefs,
                                pendingSyncCount = pendingSyncCount,
                                lastSyncMillis = lastSyncMillis,
                                isSyncing = isSyncing,
                                onTriggerSixHourSync = {
                                    vm.triggerSixHourDataTransfer(context, isAutomatic = false)
                                },
                                onConnectOptionalGoogleAccount = {
                                    onGoogleSignInClicked(
                                        context = context,
                                        credentialManager = credentialManager,
                                        onAuthSuccess = {
                                            vm.triggerSixHourDataTransfer(context, isAutomatic = false)
                                        },
                                        onAuthError = {},
                                        scope = scope
                                    )
                                },
                                onUpdatePrefs = { vm.updatePreferences(it) },
                                onClearConversations = { vm.clearAiConversationHistory() },
                                onExportJson = { vm.exportAllDataAsJson() },
                                onDeleteAllData = {
                                    vm.deleteAllAccountData {}
                                },
                                onSignOut = {
                                    signOut(
                                        context = context,
                                        credentialManager = credentialManager,
                                        onSignOutComplete = {},
                                        scope = scope
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun FloatingFocusTimerBar(
    task: Task,
    elapsedSeconds: Int,
    isPomodoro: Boolean,
    onToggleMode: () -> Unit,
    onStopAndSave: (completeTaskNow: Boolean) -> Unit
) {
    val pomodoroTargetSec = 25 * 60
    val displaySec = if (isPomodoro) {
        (pomodoroTargetSec - elapsedSeconds).coerceAtLeast(0)
    } else {
        elapsedSeconds
    }
    val mins = displaySec / 60
    val secs = displaySec % 60
    val timeText = PersianDateAndUnitUtils.toPersianDigits(
        String.format(Locale.US, "%02d:%02d", mins, secs)
    )

    com.example.ui.theme.LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("floating_focus_timer_bar"),
        shape = RoundedCornerShape(20.dp),
        accentBorderColor = ElectricTeal
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pulsing Active Session Icon Badge
                Surface(
                    shape = CircleShape,
                    color = ElectricTeal.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, ElectricTeal.copy(alpha = 0.5f)),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "تایمر تمرکز",
                            tint = ElectricTeal,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = timeText,
                            style = MaterialTheme.typography.titleMedium,
                            color = ElectricTeal,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = AmberWarning.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.35f)),
                            modifier = Modifier.clickable { onToggleMode() }
                        ) {
                            Text(
                                text = if (isPomodoro) "پومودورو ۲۵m" else "کرنومتر آزاد",
                                style = MaterialTheme.typography.labelSmall,
                                color = AmberWarning,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "در حال تمرکز: ${task.title}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = { onStopAndSave(false) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("stop_focus_save_button")
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, tint = RoseError, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("توقف", style = MaterialTheme.typography.labelSmall)
                }
                Button(
                    onClick = { onStopAndSave(true) },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("complete_focus_task_button")
                ) {
                    Text("تکمیل", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
