package com.example.personallifeos.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.personallifeos.data.local.AppDatabase
import com.example.personallifeos.data.model.AiActionDraft
import com.example.personallifeos.data.model.CategoryBudget
import com.example.personallifeos.data.model.ChatMessage
import com.example.personallifeos.data.model.DebtItem
import com.example.personallifeos.data.model.DebtType
import com.example.personallifeos.data.model.ExpenseItem
import com.example.personallifeos.data.model.GoalItem
import com.example.personallifeos.data.model.HabitItem
import com.example.personallifeos.data.model.HealthLog
import com.example.personallifeos.data.model.QuickPreset
import com.example.personallifeos.data.model.ReminderEntityType
import com.example.personallifeos.data.model.ReminderItem
import com.example.personallifeos.data.model.TaskCategory
import com.example.personallifeos.data.model.TaskItem
import com.example.personallifeos.data.model.TaskPriority
import com.example.personallifeos.data.model.TransactionType
import com.example.personallifeos.data.model.VaultNote
import com.example.personallifeos.data.repository.LifeOsRepository
import com.example.personallifeos.sms.BankNotificationItem
import com.example.personallifeos.sms.BankSmsParsedResult
import com.example.personallifeos.sms.BankType
import com.example.personallifeos.sms.BankUserConfig
import com.example.personallifeos.sms.DetectedCurrencyUnit
import com.example.personallifeos.sms.IranianBankSmsEngine
import com.example.personallifeos.sms.SmsTransactionDirection
import com.example.personallifeos.sms.SmsVerificationStatus
import com.example.personallifeos.ui.screens.NotificationSettingsState
import com.example.personallifeos.util.DetectedIntentType
import com.example.personallifeos.util.PersianUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val repository = LifeOsRepository(database.lifeOsDao())

    val tasks: StateFlow<List<TaskItem>> = repository.allTasks.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val habits: StateFlow<List<HabitItem>> = repository.allHabits.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val expenses: StateFlow<List<ExpenseItem>> = repository.allExpenses.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val debts: StateFlow<List<DebtItem>> = repository.allDebts.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val vaultNotes: StateFlow<List<VaultNote>> = repository.allVaultNotes.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private val _defaultHealth = HealthLog(
        id = "hl1",
        date = PersianUtils.getPersianDateString(),
        sleepHours = 7.5f,
        sleepQuality = "good",
        waterGlasses = 6,
        exerciseMinutes = 45,
        exerciseType = "بدنسازی و هوازی",
        caloriesBurned = 480,
        dailyCalorieIntake = 1850,
        lastScannedFoodSummary = "سینه مرغ گریل و سالاد کینوا (۴۲۰ کالری • ۳۸ گرم پروتئین)"
    )

    private val _healthLog = MutableStateFlow(_defaultHealth)
    val healthLog: StateFlow<HealthLog> = _healthLog.asStateFlow()

    // 1-Tap Quick Expense Presets
    private val _quickPresets = MutableStateFlow(
        listOf(
            QuickPreset("qp1", "قهوه و کافه", 120_000L, "کافه و دخانیات", "☕"),
            QuickPreset("qp2", "اسنپ / تپسی", 145_000L, "حمل و نقل", "🚕"),
            QuickPreset("qp3", "نان و سوپرمارکت", 280_000L, "سوپرمارکت و روزمره", "🍞"),
            QuickPreset("qp4", "ناهار کاری", 390_000L, "غذا و رستوران", "🍽️"),
            QuickPreset("qp5", "بنزین و خودرو", 150_000L, "حمل و نقل", "⛽")
        )
    )
    val quickPresets: StateFlow<List<QuickPreset>> = _quickPresets.asStateFlow()

    // Long-Term Goals & Projects (Section 4.1 & 4.2 of Roadmap)
    private val _goals = MutableStateFlow(
        listOf(
            GoalItem(
                id = "g1",
                title = "صندوق ذخیره اضطراری ۶ ماهه",
                description = "پس‌انداز مستمر ماهانه برای امنیت مالی و پوشش هزینه‌های ضروری",
                targetDate = "۱۴۰۵/۱۲/۲۹",
                progressPercent = 65,
                nextAction = "انتقال ۳ میلیون تومان به حساب پس‌انداز بلوبانک",
                category = TaskCategory.FINANCE,
                targetBudgetToman = 90_000_000L
            ),
            GoalItem(
                id = "g2",
                title = "تسلط بر معماری سیستم‌های توزیع‌شده و Kotlin",
                description = "مطالعه عمیق، پیاده‌سازی پروژه‌های عملی و تست‌های یکپارچه",
                targetDate = "۱۴۰۵/۱۰/۳۰",
                progressPercent = 78,
                nextAction = "مطالعه فصل ۴ کتاب طراحی سیستم‌های داده‌محور",
                category = TaskCategory.STUDY
            ),
            GoalItem(
                id = "g3",
                title = "تناسب اندام و کاهش درصد چربی",
                description = "۴ جلسه تمرین هوازی و قدرتی در هفته به همراه پایش کالری",
                targetDate = "۱۴۰۵/۰۹/۳۰",
                progressPercent = 52,
                nextAction = "۴۵ دقیقه تمرین هوازی امروز عصر",
                category = TaskCategory.HEALTH
            )
        )
    )
    val goals: StateFlow<List<GoalItem>> = _goals.asStateFlow()

    // Category Monthly Budgets
    private val _categoryBudgets = MutableStateFlow(
        listOf(
            CategoryBudget("سوپرمارکت و روزمره", 8_000_000L),
            CategoryBudget("غذا و رستوران", 4_500_000L),
            CategoryBudget("حمل و نقل", 3_000_000L),
            CategoryBudget("کافه و دخانیات", 2_000_000L),
            CategoryBudget("خرید عمومی", 6_000_000L)
        )
    )
    val categoryBudgets: StateFlow<List<CategoryBudget>> = _categoryBudgets.asStateFlow()

    // Unified Reminder Engine (Section 8 of Roadmap)
    private val _reminders = MutableStateFlow(
        listOf(
            ReminderItem(
                id = "rem_debt_d1",
                entityType = ReminderEntityType.DEBT,
                entityId = "d1",
                title = "سررسید قسط وام تجهیزات مهندسی (بانک رسالت)",
                triggerTime = "۱۴۰۵/۰۸/۰۷ • ساعت ۰۹:۰۰",
                sensitiveAmountToman = 3_000_000L
            ),
            ReminderItem(
                id = "rem_task_1",
                entityType = ReminderEntityType.TASK,
                entityId = "1",
                title = "بررسی و تایید گزارش مالی شرکت",
                triggerTime = "۱۴۰۵/۰۸/۰۶ • ساعت ۱۱:۳۰"
            ),
            ReminderItem(
                id = "rem_daily_exp",
                entityType = ReminderEntityType.DAILY_EXPENSE,
                entityId = "daily_log",
                title = "یادآور شبانه ثبت مخارج و بررسی پیامک‌های بانکی",
                triggerTime = "هر شب • ساعت ۲۱:۳۰"
            )
        )
    )
    val reminders: StateFlow<List<ReminderItem>> = _reminders.asStateFlow()

    // Customizable Dashboard Cards (Section 6 of Roadmap)
    private val _dashboardVisibleCards = MutableStateFlow(
        setOf("FINANCE", "REMINDERS", "GOALS_TASKS", "HEALTH", "HABITS_TRENDS")
    )
    val dashboardVisibleCards: StateFlow<Set<String>> = _dashboardVisibleCards.asStateFlow()

    // Pomodoro Focus Timer State (Section 4.4 of Roadmap)
    private val _pomodoroRemainingSeconds = MutableStateFlow(25 * 60)
    val pomodoroRemainingSeconds: StateFlow<Int> = _pomodoroRemainingSeconds.asStateFlow()

    private val _isPomodoroRunning = MutableStateFlow(false)
    val isPomodoroRunning: StateFlow<Boolean> = _isPomodoroRunning.asStateFlow()

    private val _completedPomodoroSessions = MutableStateFlow(3)
    val completedPomodoroSessions: StateFlow<Int> = _completedPomodoroSessions.asStateFlow()

    private var pomodoroJob: Job? = null

    private val _configuredBanks = MutableStateFlow<List<BankUserConfig>>(IranianBankSmsEngine.getConfiguredBanks())
    val configuredBanks: StateFlow<List<BankUserConfig>> = _configuredBanks.asStateFlow()

    private val _pendingReviewSms = MutableStateFlow<BankSmsParsedResult?>(null)
    val pendingReviewSms: StateFlow<BankSmsParsedResult?> = _pendingReviewSms.asStateFlow()

    private val _notificationSettings = MutableStateFlow(NotificationSettingsState())
    val notificationSettings: StateFlow<NotificationSettingsState> = _notificationSettings.asStateFlow()

    private val _bankNotifications = MutableStateFlow<List<BankNotificationItem>>(emptyList())
    val bankNotifications: StateFlow<List<BankNotificationItem>> = _bankNotifications.asStateFlow()

    private val _activeFloatingBanner = MutableStateFlow<BankNotificationItem?>(null)
    val activeFloatingBanner: StateFlow<BankNotificationItem?> = _activeFloatingBanner.asStateFlow()

    init {
        viewModelScope.launch {
            repository.healthLog.collect { log ->
                if (log != null) {
                    _healthLog.value = log
                }
            }
        }
        val sampleParsed = IranianBankSmsEngine.parse(
            "بانک ملی ایران\nواریز: ۱,۲۵۰,۰۰۰ ریال\nبه: ۰۱*****۱۲۳۴\nمانده: ۴۵,۸۹۰,۰۰۰ ریال\nپیگیری: ۸۸۷۷۱۱",
            "98300017"
        )
        _bankNotifications.value = listOf(
            BankNotificationItem(
                id = "seed_notif_1",
                sender = "98300017",
                rawText = "بانک ملی ایران - واریز ۱,۲۵۰,۰۰۰ ریال",
                parsedResult = sampleParsed,
                receivedAt = "۱۰:۳۰",
                isRead = false
            )
        )
    }

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                id = "m1",
                sender = "gemini",
                text = "سلام! من دستیار هوشمند Personal Life OS نسخه 1.6.0R هستم. می‌توانید هزینه‌ها و درآمدهای محاوره‌ای (مثلاً «۵۰۰ تومن سیگار خریدم»)، بدهی و اقساط، یا پرسش‌های تحلیلی ۳۰ روز اخیر را با من مطرح کنید. تمام عملیات حساس پیش از ثبت، کارت تأییدیه نمایش می‌دهند.",
                timestamp = "۱۰:۰۰"
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    // =========================================================================
    // Tasks, Goals, Habits & Pomodoro CRUD
    // =========================================================================

    fun addTask(
        title: String,
        category: TaskCategory,
        priority: TaskPriority,
        dueDate: String,
        linkedDebtId: String? = null
    ) {
        viewModelScope.launch {
            val newId = System.currentTimeMillis().toString()
            val newTask = TaskItem(
                id = newId,
                title = title,
                category = category,
                priority = priority,
                completed = false,
                dueDate = dueDate,
                createdAt = PersianUtils.getPersianDateString(),
                linkedDebtId = linkedDebtId
            )
            repository.insertTask(newTask)
            upsertEntityReminder(ReminderEntityType.TASK, newId, title, "$dueDate • ساعت ۱۰:۰۰")
        }
    }

    fun toggleTask(task: TaskItem) {
        viewModelScope.launch {
            val updated = task.copy(completed = !task.completed)
            repository.updateTask(updated)
            if (updated.completed) {
                cancelEntityReminder(ReminderEntityType.TASK, task.id)
            }
        }
    }

    fun postponeTaskToTomorrow(task: TaskItem) {
        viewModelScope.launch {
            val newDate = "۱۴۰۵/۰۸/۰۶ (فردا)"
            repository.updateTask(task.copy(dueDate = newDate))
            upsertEntityReminder(ReminderEntityType.TASK, task.id, task.title, "$newDate • ساعت ۱۰:۰۰")
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            repository.deleteTask(taskId)
            cancelEntityReminder(ReminderEntityType.TASK, taskId)
        }
    }

    fun addGoal(title: String, description: String, targetDate: String, nextAction: String, category: TaskCategory) {
        val g = GoalItem(
            id = "g_${System.currentTimeMillis()}",
            title = title,
            description = description,
            targetDate = targetDate,
            progressPercent = 15,
            nextAction = nextAction,
            category = category
        )
        _goals.value = listOf(g) + _goals.value
    }

    fun updateGoalProgress(goalId: String, deltaPercent: Int) {
        _goals.value = _goals.value.map { g ->
            if (g.id == goalId) g.copy(progressPercent = (g.progressPercent + deltaPercent).coerceIn(0, 100))
            else g
        }
    }

    fun deleteGoal(goalId: String) {
        _goals.value = _goals.value.filterNot { it.id == goalId }
    }

    fun addHabit(title: String, category: TaskCategory = TaskCategory.HEALTH) {
        viewModelScope.launch {
            val newHabit = HabitItem(
                id = System.currentTimeMillis().toString(),
                title = title,
                streak = 1,
                completedToday = false,
                category = category
            )
            repository.insertHabit(newHabit)
        }
    }

    fun toggleHabit(habit: HabitItem) {
        viewModelScope.launch {
            val newCompleted = !habit.completedToday
            val newStreak = if (newCompleted) habit.streak + 1 else maxOf(0, habit.streak - 1)
            repository.updateHabit(habit.copy(completedToday = newCompleted, streak = newStreak))
        }
    }

    fun togglePomodoroTimer() {
        if (_isPomodoroRunning.value) {
            _isPomodoroRunning.value = false
            pomodoroJob?.cancel()
        } else {
            _isPomodoroRunning.value = true
            pomodoroJob = viewModelScope.launch {
                while (_isPomodoroRunning.value && _pomodoroRemainingSeconds.value > 0) {
                    delay(1000)
                    _pomodoroRemainingSeconds.value -= 1
                }
                if (_pomodoroRemainingSeconds.value == 0) {
                    _isPomodoroRunning.value = false
                    _completedPomodoroSessions.value += 1
                    _pomodoroRemainingSeconds.value = 25 * 60
                }
            }
        }
    }

    fun resetPomodoroTimer(minutes: Int = 25) {
        _isPomodoroRunning.value = false
        pomodoroJob?.cancel()
        _pomodoroRemainingSeconds.value = minutes * 60
    }

    // =========================================================================
    // Finance Pro: Expenses, Incomes, Transfers, Quick Presets & Budgets
    // =========================================================================

    fun addExpense(
        title: String,
        amount: Long,
        category: String,
        bankName: String? = null,
        isSms: Boolean = false,
        type: TransactionType = TransactionType.EXPENSE,
        idempotencyKey: String? = null,
        accountName: String? = null
    ) {
        viewModelScope.launch {
            if (!idempotencyKey.isNullOrBlank()) {
                val existing = repository.getExpenseByIdempotencyKey(idempotencyKey)
                if (existing != null || IranianBankSmsEngine.isDuplicateKey(idempotencyKey)) {
                    return@launch
                }
                IranianBankSmsEngine.markIdempotencyKeyProcessed(idempotencyKey)
            }
            val newExpense = ExpenseItem(
                id = System.currentTimeMillis().toString(),
                title = title,
                amount = amount,
                category = category,
                date = PersianUtils.getPersianDateString(),
                type = type,
                isSmsParsed = isSms,
                bankName = bankName,
                accountName = accountName ?: bankName ?: "حساب پیش‌فرض",
                idempotencyKey = idempotencyKey
            )
            repository.insertExpense(newExpense)
        }
    }

    fun updateExpenseItem(updated: ExpenseItem) {
        viewModelScope.launch {
            repository.updateExpense(updated)
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            repository.deleteExpense(expenseId)
        }
    }

    fun triggerQuickPreset(preset: QuickPreset) {
        addExpense(
            title = preset.title,
            amount = preset.defaultAmount,
            category = preset.category,
            isSms = false,
            type = TransactionType.EXPENSE,
            accountName = "کارت روزمره"
        )
    }

    fun addQuickPreset(title: String, amount: Long, category: String, emoji: String = "⚡") {
        val newPreset = QuickPreset(
            id = "qp_${System.currentTimeMillis()}",
            title = title,
            defaultAmount = amount,
            category = category,
            iconEmoji = emoji
        )
        _quickPresets.value = _quickPresets.value + newPreset
    }

    fun deleteQuickPreset(id: String) {
        _quickPresets.value = _quickPresets.value.filterNot { it.id == id }
    }

    fun updateCategoryBudget(category: String, newLimitToman: Long) {
        val existing = _categoryBudgets.value.any { it.category == category }
        if (existing) {
            _categoryBudgets.value = _categoryBudgets.value.map {
                if (it.category == category) it.copy(monthlyLimitToman = newLimitToman) else it
            }
        } else {
            _categoryBudgets.value = _categoryBudgets.value + CategoryBudget(category, newLimitToman)
        }
    }

    // =========================================================================
    // Debt, Receivable, Installments & Debt-to-Goal Link (Section 3.3 & 5)
    // =========================================================================

    fun addDebt(
        title: String,
        personName: String,
        totalAmount: Long,
        dueDate: String,
        type: DebtType,
        createInstallmentPlanCount: Int = 0
    ) {
        viewModelScope.launch {
            val debtId = "debt_${System.currentTimeMillis()}"
            val newDebt = DebtItem(
                id = debtId,
                title = title,
                type = type,
                totalAmount = totalAmount,
                paidAmount = 0L,
                dueDate = dueDate,
                isSettled = false,
                personName = personName
            )
            repository.insertDebt(newDebt)
            upsertEntityReminder(
                entityType = ReminderEntityType.DEBT,
                entityId = debtId,
                title = "${type.titleFa}: $title ($personName)",
                triggerTime = "$dueDate • ساعت ۰۹:۰۰",
                sensitiveAmount = totalAmount
            )
            if (createInstallmentPlanCount > 0) {
                createDebtRepaymentPlan(newDebt, createInstallmentPlanCount)
            }
        }
    }

    /**
     * Phase 2 Roadmap rule:
     * Creates installment tasks in "اهداف و مسیر" linked via `linkedDebtId` without creating duplicate debt records.
     */
    fun createDebtRepaymentPlan(debt: DebtItem, installmentsCount: Int = 4) {
        val count = installmentsCount.coerceIn(1, 24)
        val perInstallment = maxOf(1L, debt.remainingAmount / count)
        viewModelScope.launch {
            for (i in 1..count) {
                val taskTitle = "قسط $i از $count: ${debt.title} (${PersianUtils.formatToman(perInstallment)})"
                val task = TaskItem(
                    id = "inst_${debt.id}_$i",
                    title = taskTitle,
                    category = TaskCategory.FINANCE,
                    priority = TaskPriority.HIGH,
                    completed = false,
                    dueDate = debt.dueDate,
                    createdAt = PersianUtils.getPersianDateString(),
                    linkedDebtId = debt.id
                )
                repository.insertTask(task)
            }
        }
    }

    /**
     * Records a partial or full installment payment on a Debt/Receivable:
     * - Deducts accurately from remainingAmount
     * - Prevents duplicate recording via idempotencyKey
     * - When remainingAmount reaches 0, marks debt settled, completes all linked tasks, and cancels future reminders.
     */
    fun recordDebtInstallmentPayment(debt: DebtItem, paymentAmount: Long) {
        if (paymentAmount <= 0L || debt.isSettled) return
        viewModelScope.launch {
            val newPaid = (debt.paidAmount + paymentAmount).coerceAtMost(debt.totalAmount)
            val nowSettled = newPaid >= debt.totalAmount
            val updatedDebt = debt.copy(
                paidAmount = newPaid,
                isSettled = nowSettled
            )
            repository.updateDebt(updatedDebt)

            // Record corresponding transaction once with deterministic idempotencyKey
            val txKey = "debt_pay_${debt.id}_${newPaid}"
            val txType = if (debt.type == DebtType.DEBT) TransactionType.EXPENSE else TransactionType.INCOME
            addExpense(
                title = "پرداخت/دریافت قسط: ${debt.title} (${debt.personName})",
                amount = paymentAmount,
                category = "اقساط و بدهی",
                isSms = false,
                type = txType,
                idempotencyKey = txKey,
                accountName = "حساب اصلی"
            )

            if (nowSettled) {
                cancelEntityReminder(ReminderEntityType.DEBT, debt.id)
                tasks.value.filter { it.linkedDebtId == debt.id && !it.completed }.forEach { linkedTask ->
                    repository.updateTask(linkedTask.copy(completed = true))
                }
            }
        }
    }

    fun settleDebtCompletely(debt: DebtItem) {
        recordDebtInstallmentPayment(debt, debt.remainingAmount)
    }

    fun deleteDebt(debtId: String) {
        viewModelScope.launch {
            repository.deleteDebt(debtId)
            cancelEntityReminder(ReminderEntityType.DEBT, debtId)
        }
    }

    // =========================================================================
    // Unified Reminder Engine & Dashboard Customization (Section 6 & 8)
    // =========================================================================

    fun upsertEntityReminder(
        entityType: ReminderEntityType,
        entityId: String,
        title: String,
        triggerTime: String,
        sensitiveAmount: Long? = null
    ) {
        val uniqueId = "rem_${entityType.name}_$entityId"
        val updatedItem = ReminderItem(
            id = uniqueId,
            entityType = entityType,
            entityId = entityId,
            title = title,
            triggerTime = triggerTime,
            isSnoozed = false,
            isCompleted = false,
            sensitiveAmountToman = sensitiveAmount
        )
        val filtered = _reminders.value.filterNot { it.id == uniqueId }
        _reminders.value = listOf(updatedItem) + filtered
    }

    fun snoozeReminder(reminderId: String) {
        _reminders.value = _reminders.value.map { rem ->
            if (rem.id == reminderId) {
                rem.copy(
                    isSnoozed = true,
                    triggerTime = "تعویق‌شده (+۱ ساعت دیگر)"
                )
            } else rem
        }
    }

    fun cancelEntityReminder(entityType: ReminderEntityType, entityId: String) {
        val uniqueId = "rem_${entityType.name}_$entityId"
        _reminders.value = _reminders.value.filterNot { it.id == uniqueId || it.entityId == entityId }
    }

    fun dismissReminder(reminderId: String) {
        _reminders.value = _reminders.value.filterNot { it.id == reminderId }
    }

    fun toggleDashboardCardVisibility(cardKey: String) {
        val current = _dashboardVisibleCards.value.toMutableSet()
        if (current.contains(cardKey)) {
            if (current.size > 1) current.remove(cardKey)
        } else {
            current.add(cardKey)
        }
        _dashboardVisibleCards.value = current
    }

    // =========================================================================
    // Iranian Bank SMS Engine & Live Notification Center
    // =========================================================================

    fun saveBankConfig(bank: BankType, senderNumber: String, requireSenderMatch: Boolean) {
        IranianBankSmsEngine.configureBankSenderPolicy(
            bank = bank,
            senderNumber = senderNumber,
            requireSenderMatch = requireSenderMatch,
            verifyDefaultTemplate = true
        )
        _configuredBanks.value = IranianBankSmsEngine.getConfiguredBanks()
    }

    fun parseAndAddSms(smsText: String, senderAddress: String? = null): String {
        val parsed = IranianBankSmsEngine.parse(smsText, senderAddress)
        return when (parsed.verificationStatus) {
            SmsVerificationStatus.REJECTED -> {
                parsed.reviewReasonFa ?: "پیامک غیرمالی یا ناموفق است و ثبت نشد."
            }
            SmsVerificationStatus.DUPLICATE -> {
                "این پیامک قبلاً ثبت شده است (مسدود شده توسط کلید ضدتکرار)."
            }
            SmsVerificationStatus.NEEDS_REVIEW -> {
                _pendingReviewSms.value = parsed
                "نیازمند بررسی دستی: ${parsed.reviewReasonFa ?: "اطلاعات تراکنش کامل نیست"}"
            }
            SmsVerificationStatus.AUTO_ACCEPTED -> {
                val txType = when (parsed.direction) {
                    SmsTransactionDirection.DEPOSIT -> TransactionType.INCOME
                    SmsTransactionDirection.TRANSFER_CARD,
                    SmsTransactionDirection.TRANSFER_PAYA,
                    SmsTransactionDirection.TRANSFER_POL -> TransactionType.TRANSFER
                    else -> TransactionType.EXPENSE
                }
                val title = parsed.merchantOrTerminal ?: parsed.destination ?: "${parsed.direction.faName} ${parsed.bank.faName}"
                addExpense(
                    title = title,
                    amount = parsed.amountToman,
                    category = parsed.suggestedCategory,
                    bankName = parsed.bank.faName,
                    isSms = true,
                    type = txType,
                    idempotencyKey = parsed.idempotencyKey,
                    accountName = parsed.bank.faName
                )
                "تراکنش ${PersianUtils.formatToman(parsed.amountToman)} (${parsed.direction.faName}) از ${parsed.bank.faName} با موفقیت ثبت شد ✓"
            }
        }
    }

    fun confirmPendingReview(
        overrideDirection: SmsTransactionDirection,
        overrideCurrency: DetectedCurrencyUnit = DetectedCurrencyUnit.RIAL,
        overrideAmountToman: Long? = null
    ): String {
        val pending = _pendingReviewSms.value ?: return "پیامکی برای بررسی وجود ندارد."
        val error = IranianBankSmsEngine.validateManualConfirmReview(
            parsed = pending,
            overrideDirection = overrideDirection,
            overrideCurrency = overrideCurrency,
            overrideAmountToman = overrideAmountToman
        )
        if (error != null) {
            return error
        }
        val finalAmount = overrideAmountToman ?: pending.amountToman
        val txType = when (overrideDirection) {
            SmsTransactionDirection.DEPOSIT -> TransactionType.INCOME
            SmsTransactionDirection.TRANSFER_CARD,
            SmsTransactionDirection.TRANSFER_PAYA,
            SmsTransactionDirection.TRANSFER_POL -> TransactionType.TRANSFER
            else -> TransactionType.EXPENSE
        }
        val title = pending.merchantOrTerminal ?: pending.destination ?: "${overrideDirection.faName} ${pending.bank.faName}"
        addExpense(
            title = title,
            amount = finalAmount,
            category = pending.suggestedCategory,
            bankName = pending.bank.faName,
            isSms = true,
            type = txType,
            idempotencyKey = pending.idempotencyKey,
            accountName = pending.bank.faName
        )
        _pendingReviewSms.value = null
        return "تراکنش بررسی‌شده (${overrideDirection.faName} - ${PersianUtils.formatToman(finalAmount)}) ثبت شد ✓"
    }

    fun dismissPendingReview() {
        _pendingReviewSms.value = null
    }

    fun updateNotificationSettings(newSettings: NotificationSettingsState) {
        _notificationSettings.value = newSettings
    }

    fun simulateIncomingBankSms(sender: String, rawText: String) {
        if (!_notificationSettings.value.liveReceiptListenerEnabled) return
        val parsed = IranianBankSmsEngine.parse(rawText, sender)
        val timeStr = PersianUtils.formatPersianNumber(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()))
        val item = BankNotificationItem(
            id = "notif_${System.currentTimeMillis()}",
            sender = sender,
            rawText = rawText,
            parsedResult = parsed,
            receivedAt = timeStr,
            isRead = false
        )
        _bankNotifications.value = listOf(item) + _bankNotifications.value
        if (_notificationSettings.value.floatingBannerEnabled) {
            _activeFloatingBanner.value = item
        }
        if (parsed.verificationStatus == SmsVerificationStatus.AUTO_ACCEPTED) {
            val txType = when (parsed.direction) {
                SmsTransactionDirection.DEPOSIT -> TransactionType.INCOME
                SmsTransactionDirection.TRANSFER_CARD,
                SmsTransactionDirection.TRANSFER_PAYA,
                SmsTransactionDirection.TRANSFER_POL -> TransactionType.TRANSFER
                else -> TransactionType.EXPENSE
            }
            val title = parsed.merchantOrTerminal ?: parsed.destination ?: "${parsed.direction.faName} ${parsed.bank.faName}"
            addExpense(
                title = title,
                amount = parsed.amountToman,
                category = parsed.suggestedCategory,
                bankName = parsed.bank.faName,
                isSms = true,
                type = txType,
                idempotencyKey = parsed.idempotencyKey,
                accountName = parsed.bank.faName
            )
        } else if (parsed.verificationStatus == SmsVerificationStatus.NEEDS_REVIEW) {
            _pendingReviewSms.value = parsed
        }
    }

    fun confirmNotificationReview(item: BankNotificationItem, direction: SmsTransactionDirection) {
        val err = IranianBankSmsEngine.validateManualConfirmReview(
            parsed = item.parsedResult,
            overrideDirection = direction,
            overrideCurrency = DetectedCurrencyUnit.RIAL
        )
        if (err == null) {
            val txType = if (direction == SmsTransactionDirection.DEPOSIT) TransactionType.INCOME else TransactionType.EXPENSE
            val title = item.parsedResult.merchantOrTerminal ?: "${direction.faName} ${item.parsedResult.bank.faName}"
            addExpense(
                title = title,
                amount = item.parsedResult.amountToman,
                category = item.parsedResult.suggestedCategory,
                bankName = item.parsedResult.bank.faName,
                isSms = true,
                type = txType,
                idempotencyKey = item.parsedResult.idempotencyKey,
                accountName = item.parsedResult.bank.faName
            )
            _bankNotifications.value = _bankNotifications.value.map {
                if (it.id == item.id) it.copy(isRead = true, isConfirmedOrDismissed = true) else it
            }
            if (_pendingReviewSms.value?.idempotencyKey == item.parsedResult.idempotencyKey) {
                _pendingReviewSms.value = null
            }
        }
    }

    fun dismissNotification(notifId: String) {
        _bankNotifications.value = _bankNotifications.value.filterNot { it.id == notifId }
        if (_activeFloatingBanner.value?.id == notifId) {
            _activeFloatingBanner.value = null
        }
    }

    fun markAllNotificationsRead() {
        _bankNotifications.value = _bankNotifications.value.map { it.copy(isRead = true) }
        _activeFloatingBanner.value = null
    }

    fun dismissFloatingBanner() {
        _activeFloatingBanner.value = null
    }

    // =========================================================================
    // Health & AI Photo Scanner Updates
    // =========================================================================

    fun updateWater(delta: Int) {
        val current = _healthLog.value
        val newCount = maxOf(0, current.waterGlasses + delta)
        val updated = current.copy(waterGlasses = newCount)
        _healthLog.value = updated
        viewModelScope.launch { repository.updateHealthLog(updated) }
    }

    fun updateFullHealthMetrics(
        sleepHours: Float,
        sleepQuality: String,
        exerciseMinutes: Int,
        exerciseType: String,
        caloriesBurned: Int,
        dailyCalorieIntake: Int
    ) {
        val updated = _healthLog.value.copy(
            sleepHours = sleepHours,
            sleepQuality = sleepQuality,
            exerciseMinutes = exerciseMinutes,
            exerciseType = exerciseType,
            caloriesBurned = caloriesBurned,
            dailyCalorieIntake = dailyCalorieIntake
        )
        _healthLog.value = updated
        viewModelScope.launch { repository.updateHealthLog(updated) }
    }

    fun recordScannedFoodOrReceipt(summaryText: String, estimatedCalories: Int, receiptAmountToman: Long? = null) {
        val current = _healthLog.value
        val updated = current.copy(
            dailyCalorieIntake = current.dailyCalorieIntake + estimatedCalories,
            lastScannedFoodSummary = summaryText
        )
        _healthLog.value = updated
        viewModelScope.launch { repository.updateHealthLog(updated) }
        if (receiptAmountToman != null && receiptAmountToman > 0) {
            addExpense(
                title = "رسید اسکن‌شده: $summaryText",
                amount = receiptAmountToman,
                category = "غذا و رستوران",
                isSms = false,
                type = TransactionType.EXPENSE
            )
        }
    }

    // =========================================================================
    // Vault Notes CRUD
    // =========================================================================

    fun addVaultNote(title: String, content: String, category: String) {
        viewModelScope.launch {
            val newNote = VaultNote(
                id = System.currentTimeMillis().toString(),
                title = title,
                content = content,
                category = category,
                updatedAt = PersianUtils.getPersianDateString(),
                isEncrypted = true
            )
            repository.insertVaultNote(newNote)
        }
    }

    fun deleteVaultNote(noteId: String) {
        viewModelScope.launch {
            repository.deleteVaultNote(noteId)
        }
    }

    // =========================================================================
    // Controlled AI Assistant & Colloquial Intent Engine (Section 7 of Roadmap)
    // =========================================================================

    fun sendChatMessage(userText: String) {
        if (userText.isBlank() || _isChatLoading.value) return

        val timeStr = PersianUtils.formatPersianNumber(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()))
        val userMsg = ChatMessage(
            id = System.currentTimeMillis().toString(),
            sender = "user",
            text = userText,
            timestamp = timeStr
        )
        _messages.value = _messages.value + userMsg
        _isChatLoading.value = true

        viewModelScope.launch {
            delay(450)
            val intent = PersianUtils.detectTransactionIntent(userText)

            val aiMessage = when {
                intent.intentType == DetectedIntentType.ANALYTICS_QUERY -> {
                    val totalExp = PersianUtils.aggregateExpensesToman(expenses.value)
                    val totalInc = PersianUtils.aggregateIncomesToman(expenses.value)
                    val safe = PersianUtils.calculateSafeToSpend(25_000_000L, expenses.value, debts.value)
                    val topCat = expenses.value
                        .filter { it.type == TransactionType.EXPENSE }
                        .groupBy { it.category }
                        .maxByOrNull { e -> e.value.sumOf { it.amount } }
                    val pendingTasksCount = tasks.value.count { !it.completed }
                    val debtRem = PersianUtils.aggregateTotalDebtsRemainingToman(debts.value)

                    val analysisText = buildString {
                        appendLine("📊 گزارش تحلیلی واقعی (بازه ۳۰ روز اخیر و ۷ روز اخیر):")
                        appendLine("• مجموع درآمد: ${PersianUtils.formatToman(totalInc)}")
                        appendLine("• مجموع هزینه خالص (بدون انتقال داخلی): ${PersianUtils.formatToman(totalExp)}")
                        if (topCat != null) {
                            appendLine("• بیشترین دسته هزینه: «${topCat.key}» (${PersianUtils.formatToman(topCat.value.sumOf { it.amount })})")
                        }
                        appendLine("• مبلغ قابل‌خرج تا پایان ماه (Safe-to-Spend): ${PersianUtils.formatToman(safe.safeToSpendToman)}")
                        appendLine("• بدهی‌های باز: ${PersianUtils.formatToman(debtRem)} | کارهای باقیمانده: ${PersianUtils.formatPersianNumber(pendingTasksCount.toLong())} مورد")
                    }.trim()

                    ChatMessage(
                        id = (System.currentTimeMillis() + 1).toString(),
                        sender = "gemini",
                        text = analysisText,
                        timestamp = timeStr
                    )
                }

                intent.isAmbiguous -> {
                    ChatMessage(
                        id = (System.currentTimeMillis() + 1).toString(),
                        sender = "gemini",
                        text = "❓ پرسش تکمیلی (مدیریت ابهام): ${intent.clarificationQuestionFa ?: "لطفاً مبلغ یا جزئیات بیشتر را مشخص کنید تا پیش‌نویس ثبت آماده شود."}",
                        timestamp = timeStr,
                        isClarification = true
                    )
                }

                else -> {
                    val draft = AiActionDraft(
                        actionType = intent.intentType.name,
                        title = intent.title,
                        amountToman = intent.amountToman ?: 0L,
                        category = intent.category,
                        personOrAccount = intent.personOrAccount
                    )
                    val amtText = if (draft.amountToman > 0) " به مبلغ ${PersianUtils.formatToman(draft.amountToman)}" else ""
                    ChatMessage(
                        id = (System.currentTimeMillis() + 1).toString(),
                        sender = "gemini",
                        text = "🛡️ پیش‌نویس آماده شد: «${intent.intentType.titleFa}» با عنوان «${draft.title}»$amtText در دسته «${draft.category}».\nطبق قانون ایمنی، لطفاً برای ثبت نهایی در دیتابیس تأیید فرمایید:",
                        timestamp = timeStr,
                        requiresConfirmation = true,
                        actionDraft = draft
                    )
                }
            }

            _messages.value = _messages.value + aiMessage
            _isChatLoading.value = false
        }
    }

    fun confirmAiDraft(messageId: String) {
        val msg = _messages.value.find { it.id == messageId } ?: return
        val draft = msg.actionDraft ?: return
        if (draft.isExecuted || draft.isCancelled) return

        when (draft.actionType) {
            "EXPENSE" -> addExpense(draft.title, draft.amountToman, draft.category, type = TransactionType.EXPENSE)
            "INCOME" -> addExpense(draft.title, draft.amountToman, draft.category, type = TransactionType.INCOME)
            "TRANSFER" -> addExpense(draft.title, draft.amountToman, draft.category, type = TransactionType.TRANSFER)
            "DEBT" -> addDebt(draft.title, draft.personOrAccount ?: "طلبکار", draft.amountToman, "۱۴۰۵/۰۸/۳۰", DebtType.DEBT)
            "RECEIVABLE" -> addDebt(draft.title, draft.personOrAccount ?: "بدهکار", draft.amountToman, "۱۴۰۵/۰۸/۳۰", DebtType.RECEIVABLE)
            "TASK" -> addTask(draft.title, TaskCategory.WORK, TaskPriority.HIGH, "۱۴۰۵/۰۸/۰۶")
        }

        _messages.value = _messages.value.map {
            if (it.id == messageId) {
                it.copy(
                    requiresConfirmation = false,
                    actionDraft = draft.copy(isExecuted = true),
                    text = it.text + "\n\n✅ تأیید شد و با موفقیت در دیتابیس ثبت گردید."
                )
            } else it
        }
    }

    fun cancelAiDraft(messageId: String) {
        val msg = _messages.value.find { it.id == messageId } ?: return
        val draft = msg.actionDraft ?: return
        _messages.value = _messages.value.map {
            if (it.id == messageId) {
                it.copy(
                    requiresConfirmation = false,
                    actionDraft = draft.copy(isCancelled = true),
                    text = it.text + "\n\n❌ عملیات به درخواست شما لغو شد."
                )
            } else it
        }
    }

    // =========================================================================
    // Backup (JSON & AES-256-GCM Encrypted) & Factory Reset
    // =========================================================================

    fun exportBackupJson(): String {
        val root = JSONObject()
        root.put("version", "1.6.0R")
        root.put("exportedAt", PersianUtils.getPersianDateString())

        val tasksArr = JSONArray()
        tasks.value.forEach { t ->
            tasksArr.put(JSONObject().apply {
                put("id", t.id)
                put("title", t.title)
                put("category", t.category.name)
                put("priority", t.priority.name)
                put("completed", t.completed)
                put("dueDate", t.dueDate)
                put("linkedDebtId", t.linkedDebtId ?: "")
            })
        }
        root.put("tasks", tasksArr)

        val expArr = JSONArray()
        expenses.value.forEach { e ->
            expArr.put(JSONObject().apply {
                put("id", e.id)
                put("title", e.title)
                put("amount", e.amount)
                put("category", e.category)
                put("date", e.date)
                put("type", e.type.name)
                put("accountName", e.accountName ?: "")
            })
        }
        root.put("expenses", expArr)

        val debtArr = JSONArray()
        debts.value.forEach { d ->
            debtArr.put(JSONObject().apply {
                put("id", d.id)
                put("title", d.title)
                put("type", d.type.name)
                put("totalAmount", d.totalAmount)
                put("paidAmount", d.paidAmount)
                put("personName", d.personName)
                put("dueDate", d.dueDate)
                put("isSettled", d.isSettled)
            })
        }
        root.put("debts", debtArr)

        return root.toString(2)
    }

    fun exportEncryptedAesBackup(passphrase: String): String {
        val plain = exportBackupJson()
        return PersianUtils.encryptBackupAesGcm(plain, passphrase.ifBlank { "1234" })
    }

    fun resetData() {
        viewModelScope.launch {
            repository.resetAllData()
            IranianBankSmsEngine.resetDefaultState()
            _configuredBanks.value = IranianBankSmsEngine.getConfiguredBanks()
            _pendingReviewSms.value = null
            _healthLog.value = _defaultHealth
        }
    }
}
