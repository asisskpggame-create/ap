package com.example.personallifeos.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.personallifeos.data.local.AppDatabase
import com.example.personallifeos.data.model.ChatMessage
import com.example.personallifeos.data.model.ExpenseItem
import com.example.personallifeos.data.model.HabitItem
import com.example.personallifeos.data.model.HealthLog
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
import com.example.personallifeos.util.PersianUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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

    private val _defaultHealth = HealthLog(
        id = "hl1",
        date = PersianUtils.getPersianDateString(),
        sleepHours = 7.5f,
        sleepQuality = "good",
        waterGlasses = 6,
        exerciseMinutes = 45,
        exerciseType = "بدنسازی و هوازی",
        caloriesBurned = 480
    )

    private val _healthLog = MutableStateFlow(_defaultHealth)
    val healthLog: StateFlow<HealthLog> = _healthLog.asStateFlow()

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
        // Seed initial sample notification for the notification center
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

    val vaultNotes: StateFlow<List<VaultNote>> = repository.allVaultNotes.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                id = "m1",
                sender = "gemini",
                text = "سلام! من دستیار هوشمند جمینای شما در Personal Life OS نسخه 1.6.0R هستم. چطور می‌توانم در مدیریت مالی، فیش‌خوان بانکی و کارهای امروز به شما کمک کنم؟",
                timestamp = "۱۰:۰۰"
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    // Tasks CRUD
    fun addTask(title: String, category: TaskCategory, priority: TaskPriority, dueDate: String) {
        viewModelScope.launch {
            val newTask = TaskItem(
                id = System.currentTimeMillis().toString(),
                title = title,
                category = category,
                priority = priority,
                completed = false,
                dueDate = dueDate,
                createdAt = PersianUtils.getPersianDateString()
            )
            repository.insertTask(newTask)
        }
    }

    fun toggleTask(task: TaskItem) {
        viewModelScope.launch {
            repository.updateTask(task.copy(completed = !task.completed))
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            repository.deleteTask(taskId)
        }
    }

    // Habits CRUD
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

    // Expenses CRUD with Idempotency Protection
    fun addExpense(
        title: String,
        amount: Long,
        category: String,
        bankName: String? = null,
        isSms: Boolean = false,
        type: TransactionType = TransactionType.EXPENSE,
        idempotencyKey: String? = null
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
                idempotencyKey = idempotencyKey
            )
            repository.insertExpense(newExpense)
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            repository.deleteExpense(expenseId)
        }
    }

    fun saveBankConfig(bank: BankType, senderNumber: String, requireSenderMatch: Boolean) {
        IranianBankSmsEngine.configureBankSenderPolicy(
            bank = bank,
            senderNumber = senderNumber,
            requireSenderMatch = requireSenderMatch,
            verifyDefaultTemplate = true
        )
        _configuredBanks.value = IranianBankSmsEngine.getConfiguredBanks()
    }

    /**
     * Audited SMS parsing & recording workflow:
     * - AUTO_ACCEPTED: recorded automatically with idempotency key
     * - NEEDS_REVIEW: held in _pendingReviewSms for explicit user review (never silently defaults UNKNOWN to expense)
     * - DUPLICATE / REJECTED: blocked with explicit feedback
     */
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
                    idempotencyKey = parsed.idempotencyKey
                )
                "تراکنش ${PersianUtils.formatToman(parsed.amountToman)} (${parsed.direction.faName}) از ${parsed.bank.faName} با موفقیت ثبت شد ✓"
            }
        }
    }

    /**
     * Audit Fix #1:
     * Manual confirmation of a NEEDS_REVIEW SMS requires valid direction != UNKNOWN,
     * valid currency != UNKNOWN, and amount > 0.
     */
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
            idempotencyKey = pending.idempotencyKey
        )
        _pendingReviewSms.value = null
        return "تراکنش بررسی‌شده (${overrideDirection.faName} - ${PersianUtils.formatToman(finalAmount)}) ثبت شد ✓"
    }

    fun dismissPendingReview() {
        _pendingReviewSms.value = null
    }

    // Live Notification Center handlers
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
            val txType = if (parsed.direction == SmsTransactionDirection.DEPOSIT) TransactionType.INCOME else TransactionType.EXPENSE
            val title = parsed.merchantOrTerminal ?: parsed.destination ?: "${parsed.direction.faName} ${parsed.bank.faName}"
            addExpense(
                title = title,
                amount = parsed.amountToman,
                category = parsed.suggestedCategory,
                bankName = parsed.bank.faName,
                isSms = true,
                type = txType,
                idempotencyKey = parsed.idempotencyKey
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
                idempotencyKey = item.parsedResult.idempotencyKey
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

    // Health Updates
    fun updateWater(delta: Int) {
        val current = _healthLog.value
        val newCount = maxOf(0, current.waterGlasses + delta)
        val updated = current.copy(waterGlasses = newCount)
        _healthLog.value = updated
        viewModelScope.launch { repository.updateHealthLog(updated) }
    }

    // Vault Notes CRUD
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

    // AI Assistant Chat
    fun sendChatMessage(userText: String) {
        if (userText.isBlank() || _isChatLoading.value) return

        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val userMsg = ChatMessage(
            id = System.currentTimeMillis().toString(),
            sender = "user",
            text = userText,
            timestamp = PersianUtils.formatPersianNumber(timeStr)
        )
        _messages.value = _messages.value + userMsg
        _isChatLoading.value = true

        viewModelScope.launch {
            delay(600)
            val aiResponse = when {
                userText.contains("بودجه") || userText.contains("هزینه") || userText.contains("مالی") -> {
                    val total = expenses.value.sumOf { it.amount }
                    "مجموع هزینه‌های ثبت شده شما تا کنون ${PersianUtils.formatToman(total)} است. برای کاهش مخارج، پیشنهاد می‌کنم برای دسته‌بندی خرید عمومی سقف بودجه تعیین کنید."
                }
                userText.contains("کار") || userText.contains("وظیفه") -> {
                    val pending = tasks.value.count { !it.completed }
                    "شما در حال حاضر $pending وظیفه انجام نشده دارید. توصیه من اتمام کارهای فوری تا قبل از عصر است."
                }
                userText.contains("آب") || userText.contains("سلامت") || userText.contains("ورزش") -> {
                    "امروز ${_healthLog.value.waterGlasses} لیوان آب نوشیده‌اید و ${_healthLog.value.exerciseMinutes} دقیقه ورزش داشته‌اید. وضعیت تندرستی شما عالی است!"
                }
                else -> {
                    "من دستیار هوشمند Personal Life OS شما هستم. یادداشت و درخواست شما بررسی شد: «$userText». همه برنامه‌ها هماهنگ و منظم هستند! 🌟"
                }
            }

            val aiMsg = ChatMessage(
                id = (System.currentTimeMillis() + 1).toString(),
                sender = "gemini",
                text = aiResponse,
                timestamp = PersianUtils.formatPersianNumber(timeStr)
            )
            _messages.value = _messages.value + aiMsg
            _isChatLoading.value = false
        }
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
