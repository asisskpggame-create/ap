package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiParseResult
import com.example.ai.GeminiAssistantService
import com.example.ai.ProposedAiOperation
import com.example.data.local.PreferencesDataStore
import com.example.data.model.ActivityEvent
import com.example.data.model.ConsumptionEntry
import com.example.data.model.ConversationTurn
import com.example.data.model.CustomItem
import com.example.data.model.ExerciseRecord
import com.example.data.model.ExpenseEntry
import com.example.data.model.Habit
import com.example.data.model.HabitRecord
import com.example.data.model.SleepRecord
import com.example.data.model.Task
import com.example.data.model.UserPreferencesDoc
import com.example.data.repository.LifeOsRepository
import com.example.util.PersianDateAndUnitUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class LifeOsViewModel(
    private val repository: LifeOsRepository,
    private val prefsStore: PreferencesDataStore,
    val currentUserId: String
) : ViewModel() {

    val tasksState: StateFlow<UiState<List<Task>>> = repository.observeTasks(currentUserId)
        .map<List<Task>, UiState<List<Task>>> { UiState.Success(it) }
        .catch { e ->
            Log.w(TAG, "Error observing tasks", e)
            emit(UiState.Error(e.localizedMessage ?: "خطا در دریافت تسک‌ها"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val consumptionState: StateFlow<UiState<List<ConsumptionEntry>>> = repository.observeConsumptionEntries(currentUserId)
        .map<List<ConsumptionEntry>, UiState<List<ConsumptionEntry>>> { UiState.Success(it) }
        .catch { e ->
            Log.w(TAG, "Error observing consumption", e)
            emit(UiState.Error(e.localizedMessage ?: "خطا در دریافت رکوردهای مصرف"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val customItemsState: StateFlow<List<CustomItem>> = repository.observeCustomItems(currentUserId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val expensesState: StateFlow<UiState<List<ExpenseEntry>>> = repository.observeExpenses(currentUserId)
        .map<List<ExpenseEntry>, UiState<List<ExpenseEntry>>> { UiState.Success(it) }
        .catch { e ->
            Log.w(TAG, "Error observing expenses", e)
            emit(UiState.Error(e.localizedMessage ?: "خطا در دریافت هزینه‌ها"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val habitsState: StateFlow<List<Habit>> = repository.observeHabits(currentUserId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val habitRecordsState: StateFlow<List<HabitRecord>> = repository.observeHabitRecords(currentUserId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val sleepRecordsState: StateFlow<List<SleepRecord>> = repository.observeSleepRecords(currentUserId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val exerciseRecordsState: StateFlow<List<ExerciseRecord>> = repository.observeExerciseRecords(currentUserId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val activityEventsState: StateFlow<List<ActivityEvent>> = repository.observeActivityEvents(currentUserId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val conversationsState: StateFlow<List<ConversationTurn>> = repository.observeConversations(currentUserId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val preferencesState: StateFlow<UserPreferencesDoc> = prefsStore.observeLocalPreferences(currentUserId)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            UserPreferencesDoc(userId = currentUserId)
        )

    val pendingSyncCountState: StateFlow<Int> = repository.observePendingSyncCount(currentUserId)
        .catch { emit(0) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

    val lastSyncMillisState: StateFlow<Long> = prefsStore.observeLastSyncMillis()
        .catch { emit(0L) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0L)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncingState: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private var lastUndoCallback: (suspend () -> Unit)? = null
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _isAiProcessing = MutableStateFlow(false)
    val isAiProcessing: StateFlow<Boolean> = _isAiProcessing.asStateFlow()

    private val _pendingAiPreview = MutableStateFlow<AiParseResult?>(null)
    val pendingAiPreview: StateFlow<AiParseResult?> = _pendingAiPreview.asStateFlow()

    private val _lastSubmittedVoiceHash = MutableStateFlow<String?>(null)

    // Focus Mode & Pomodoro Timer State
    private val _activeFocusTask = MutableStateFlow<Task?>(null)
    val activeFocusTask: StateFlow<Task?> = _activeFocusTask.asStateFlow()

    private val _focusElapsedSeconds = MutableStateFlow(0)
    val focusElapsedSeconds: StateFlow<Int> = _focusElapsedSeconds.asStateFlow()

    private val _isFocusPomodoro = MutableStateFlow(true) // true = 25-min Pomodoro, false = Stopwatch
    val isFocusPomodoro: StateFlow<Boolean> = _isFocusPomodoro.asStateFlow()

    private var focusTimerJob: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch {
            if (currentUserId != "local_device_user") {
                repository.migrateLocalDataToAuthenticatedUser(currentUserId)
            }
            repository.ensureDefaultQuickPresets(preferencesState.value.defaultCurrency)
        }
    }

    fun startFocusTimer(task: Task, pomodoroMode: Boolean = true) {
        focusTimerJob?.cancel()
        _activeFocusTask.value = task
        _isFocusPomodoro.value = pomodoroMode
        _focusElapsedSeconds.value = 0
        focusTimerJob = viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000L)
                _focusElapsedSeconds.value += 1
            }
        }
        postSnackbar("⏱ تایمر تمرکز برای «${task.title}» فعال شد.")
    }

    fun toggleFocusTimerMode() {
        _isFocusPomodoro.value = !_isFocusPomodoro.value
    }

    fun stopAndSaveFocusTimer(completeTaskNow: Boolean = false) {
        val currentTask = _activeFocusTask.value ?: return
        val elapsedSec = _focusElapsedSeconds.value
        focusTimerJob?.cancel()
        focusTimerJob = null
        _activeFocusTask.value = null
        _focusElapsedSeconds.value = 0

        val addedMinutes = ((elapsedSec + 30) / 60).coerceAtLeast(1)
        val newTotalMinutes = currentTask.actualDurationMinutes + addedMinutes
        viewModelScope.launch {
            val updatedTask = currentTask.copy(actualDurationMinutes = newTotalMinutes)
            repository.updateTask(updatedTask, "FOCUS_TIMER")
            if (completeTaskNow) {
                repository.completeTask(updatedTask, "FOCUS_TIMER")
            }
            repository.logActivityEvent(
                eventType = "FOCUS_SESSION_LOGGED",
                entityType = "TASK",
                entityId = currentTask.id,
                inputSource = "MANUAL",
                afterSummary = "${addedMinutes}m focus (total ${newTotalMinutes}m)",
                humanSummary = "ثبت جلسه تمرکز (پومودورو): ${PersianDateAndUnitUtils.toPersianDigits(addedMinutes.toString())} دقیقه روی «${currentTask.title}»"
            )
            postSnackbar("⏱ ${PersianDateAndUnitUtils.toPersianDigits(addedMinutes.toString())} دقیقه زمان واقعی روی «${currentTask.title}» ذخیره شد.")
        }
    }

    fun triggerSixHourDataTransfer(context: android.content.Context? = null, isAutomatic: Boolean = false) {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            val res = repository.performSixHourDataTransfer(context)
            val now = System.currentTimeMillis()
            prefsStore.saveLastSyncMillis(now)
            _isSyncing.value = false
            if (!isAutomatic) {
                val count = res.getOrDefault(0)
                postSnackbar("انتقال ۶ ساعته اطلاعات انجام شد (${PersianDateAndUnitUtils.toPersianDigits(count.toString())} رکورد همگام‌سازی شد).")
            }
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun triggerUndo() {
        val action = lastUndoCallback ?: return
        lastUndoCallback = null
        _canUndo.value = false
        viewModelScope.launch {
            action()
            _snackbarMessage.value = "عملیات بازگردانی (Undo) انجام شد."
        }
    }

    private fun postSnackbar(msg: String, undoAction: (suspend () -> Unit)? = null) {
        lastUndoCallback = undoAction
        _canUndo.value = undoAction != null
        _snackbarMessage.value = msg
    }

    // --- Task Operations ---

    fun createTask(
        title: String,
        description: String = "",
        category: String = "عمومی",
        tags: List<String> = emptyList(),
        priority: String = "MEDIUM",
        dueDateEpochDay: Long = PersianDateAndUnitUtils.currentLocalEpochDay(),
        dueTimeMinutes: Int = -1,
        reminderAtMillis: Long = -1L,
        estimatedDurationMinutes: Int = 30,
        subtasksJson: String = "[]",
        recurrenceRule: String = "NONE",
        recurrenceIntervalDays: Int = 0,
        notes: String = "",
        inputSource: String = "MANUAL"
    ) {
        viewModelScope.launch {
            val res = repository.createTask(
                title = title,
                description = description,
                category = category,
                tags = tags,
                priority = priority,
                dueDateEpochDay = dueDateEpochDay,
                dueTimeMinutes = dueTimeMinutes,
                reminderAtMillis = reminderAtMillis,
                estimatedDurationMinutes = estimatedDurationMinutes,
                subtasksJson = subtasksJson,
                recurrenceRule = recurrenceRule,
                recurrenceIntervalDays = recurrenceIntervalDays,
                notes = notes,
                inputSource = inputSource
            )
            if (res.isSuccess) {
                postSnackbar("تسک «$title» ذخیره شد.")
            } else {
                postSnackbar("خطا در ذخیره تسک: ${res.exceptionOrNull()?.localizedMessage ?: ""}")
            }
        }
    }

    fun completeTask(task: Task) {
        viewModelScope.launch {
            val res = repository.completeTask(task, "MANUAL")
            if (res.isSuccess) {
                val nextId = res.getOrNull()
                val msg = if (nextId != null) {
                    "تسک «${task.title}» تکمیل شد و نوبت بعدی ایجاد گردید."
                } else {
                    "تسک «${task.title}» تکمیل شد."
                }
                postSnackbar(msg) {
                    repository.updateTask(task.copy(status = "PENDING", completedAtMillis = -1L), "MANUAL")
                }
            }
        }
    }

    fun bulkCompleteTasks(tasksToComplete: List<Task>) {
        viewModelScope.launch {
            var count = 0
            for (t in tasksToComplete) {
                if (repository.completeTask(t, "MANUAL").isSuccess) count++
            }
            if (count > 0) {
                postSnackbar("${PersianDateAndUnitUtils.toPersianDigits(count.toString())} تسک تکمیل شد.")
            }
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task, "MANUAL")
            postSnackbar("تسک «${task.title}» ویرایش شد.")
        }
    }

    fun rescheduleTask(task: Task, newEpochDay: Long) {
        viewModelScope.launch {
            repository.updateTask(task.copy(dueDateEpochDay = newEpochDay), "MANUAL")
            postSnackbar("زمان تسک به ${PersianDateAndUnitUtils.formatEpochDay(newEpochDay)} تغییر یافت.")
        }
    }

    fun archiveOrRestoreTask(task: Task, archive: Boolean) {
        viewModelScope.launch {
            repository.archiveOrRestoreTask(task, archive, "MANUAL")
            postSnackbar(if (archive) "تسک آرشیو شد." else "تسک بازیابی شد.")
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            val res = repository.deleteTask(task, "MANUAL")
            if (res.isSuccess) {
                postSnackbar("تسک «${task.title}» حذف شد.") {
                    repository.createTask(
                        title = task.title,
                        description = task.description,
                        category = task.category,
                        tags = task.tags,
                        priority = task.priority,
                        dueDateEpochDay = task.dueDateEpochDay,
                        dueTimeMinutes = task.dueTimeMinutes,
                        estimatedDurationMinutes = task.estimatedDurationMinutes,
                        recurrenceRule = task.recurrenceRule,
                        notes = task.notes,
                        inputSource = task.inputSource
                    )
                }
            }
        }
    }

    // --- Consumption & Expense Operations ---

    fun quickAddConsumption(
        itemName: String,
        category: String,
        quantity: Double,
        unit: String,
        notes: String = "",
        optionalExpenseAmount: Double = 0.0,
        optionalCurrency: String = preferencesState.value.defaultCurrency
    ) {
        viewModelScope.launch {
            val normalizedExpenseAmount = if (optionalExpenseAmount > 0.0) {
                PersianDateAndUnitUtils.normalizeColloquialTomanAmount(optionalExpenseAmount, optionalCurrency)
            } else {
                0.0
            }
            var linkedExpId = ""
            if (normalizedExpenseAmount > 0.0) {
                val expCategory = when (category) {
                    "COFFEE", "TEA" -> "COFFEE"
                    "WATER", "FOOD", "BEVERAGE" -> "FOOD"
                    "SUPPLEMENT" -> "HEALTH"
                    else -> "OTHER"
                }
                val expRes = repository.addExpense(
                    title = "خرید $itemName",
                    category = expCategory,
                    amount = normalizedExpenseAmount,
                    currency = optionalCurrency,
                    notes = notes.ifBlank { "ثبت خودکار از دکمه سریع روزانه ($quantity $unit)" },
                    inputSource = "MANUAL",
                    transactionType = "EXPENSE"
                )
                linkedExpId = expRes.getOrDefault("")
            }
            val res = repository.addConsumptionEntry(
                itemName = itemName,
                category = category,
                quantity = quantity,
                unit = unit,
                notes = notes,
                inputSource = "QUICK_ADD",
                linkedExpenseId = linkedExpId
            )
            if (res.isSuccess) {
                val entryId = res.getOrThrow()
                val msg = if (normalizedExpenseAmount > 0.0) {
                    "ثبت شد: $itemName (${PersianDateAndUnitUtils.formatCurrencyAmount(normalizedExpenseAmount, optionalCurrency)} به خرج روزانه/ماهانه اضافه شد)"
                } else {
                    "ثبت شد: ${PersianDateAndUnitUtils.toPersianDigits(quantity.toInt().toString())} $unit $itemName"
                }
                postSnackbar(msg) {
                    val currentCons = (consumptionState.value as? UiState.Success)?.data.orEmpty()
                    currentCons.find { it.id == entryId }?.let { repository.deleteConsumptionEntry(it) }
                    if (linkedExpId.isNotBlank()) {
                        val currentExps = (expensesState.value as? UiState.Success)?.data.orEmpty()
                        currentExps.find { it.id == linkedExpId }?.let { repository.deleteExpense(it) }
                    }
                }
            }
        }
    }

    fun deleteConsumption(entry: ConsumptionEntry) {
        viewModelScope.launch {
            val res = repository.deleteConsumptionEntry(entry)
            if (res.isSuccess) {
                postSnackbar("رکورد «${entry.itemName}» حذف شد.") {
                    repository.addConsumptionEntry(
                        itemName = entry.itemName,
                        category = entry.category,
                        quantity = entry.quantity,
                        unit = entry.unit,
                        localEpochDay = entry.localEpochDay,
                        notes = entry.notes,
                        inputSource = entry.inputSource
                    )
                }
            }
        }
    }

    fun createCustomConsumptionItem(
        name: String,
        category: String,
        unit: String,
        defaultQty: Double,
        dailyTarget: Double,
        defaultPrice: Double
    ) {
        viewModelScope.launch {
            val curr = preferencesState.value.defaultCurrency
            val normalizedPrice = if (defaultPrice > 0.0) {
                PersianDateAndUnitUtils.normalizeColloquialTomanAmount(defaultPrice, curr)
            } else {
                0.0
            }
            val res = repository.addCustomItem(
                name = name,
                category = category,
                unit = unit,
                defaultQuantity = defaultQty,
                dailyTarget = dailyTarget,
                defaultPrice = normalizedPrice,
                defaultCurrency = curr
            )
            if (res.isSuccess) {
                postSnackbar("دکمه ثبت سریع «$name» اضافه شد.")
            }
        }
    }

    fun updateCustomConsumptionItem(item: CustomItem) {
        viewModelScope.launch {
            val normalizedItem = if (item.defaultPrice > 0.0) {
                item.copy(
                    defaultPrice = PersianDateAndUnitUtils.normalizeColloquialTomanAmount(
                        item.defaultPrice,
                        item.defaultCurrency
                    )
                )
            } else {
                item
            }
            val res = repository.updateCustomItem(normalizedItem)
            if (res.isSuccess) {
                postSnackbar("دکمه «${normalizedItem.name}» (${PersianDateAndUnitUtils.formatCurrencyAmount(normalizedItem.defaultPrice, normalizedItem.defaultCurrency)}) بروزرسانی شد.")
            }
        }
    }

    fun deleteCustomConsumptionItem(item: CustomItem) {
        viewModelScope.launch {
            val res = repository.deleteCustomItem(item.id, item.name)
            if (res.isSuccess) {
                postSnackbar("دکمه «${item.name}» حذف شد.") {
                    repository.addCustomItem(
                        name = item.name,
                        category = item.category,
                        unit = item.unit,
                        defaultQuantity = item.defaultQuantity,
                        dailyTarget = item.dailyTarget,
                        defaultPrice = item.defaultPrice,
                        defaultCurrency = item.defaultCurrency
                    )
                }
            }
        }
    }

    fun addExpense(
        title: String,
        category: String,
        amount: Double,
        currency: String = preferencesState.value.defaultCurrency,
        notes: String = "",
        transactionType: String = "EXPENSE"
    ) {
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) {
            postSnackbar("وارد کردن تیتر / دلیل برای ثبت خرج یا درآمد الزامی است.")
            return
        }
        viewModelScope.launch {
            val normalizedAmount = PersianDateAndUnitUtils.normalizeColloquialTomanAmount(amount, currency)
            val res = repository.addExpense(
                title = cleanTitle,
                category = category,
                amount = normalizedAmount,
                currency = currency,
                notes = notes,
                inputSource = "MANUAL",
                transactionType = transactionType
            )
            if (res.isSuccess) {
                val expId = res.getOrThrow()
                val typeLabel = when {
                    transactionType.equals("INCOME", ignoreCase = true) -> "درآمد"
                    transactionType.equals("DEBT", ignoreCase = true) -> "بدهکاری"
                    transactionType.equals("RECEIVABLE", ignoreCase = true) -> "طلبکاری"
                    else -> "خرج"
                }
                postSnackbar("$typeLabel «$cleanTitle» (${PersianDateAndUnitUtils.formatCurrencyAmount(normalizedAmount, currency)}) ثبت شد.") {
                    val currentList = (expensesState.value as? UiState.Success)?.data.orEmpty()
                    currentList.find { it.id == expId }?.let { repository.deleteExpense(it) }
                }
            }
        }
    }

    fun settleDebtOrReceivable(entry: ExpenseEntry) {
        viewModelScope.launch {
            val res = repository.settleDebtOrReceivable(entry)
            if (res.isSuccess) {
                val actionLabel = if (entry.isDebt) "بدهکاری" else "طلبکاری"
                postSnackbar("$actionLabel «${entry.title}» (${PersianDateAndUnitUtils.formatCurrencyAmount(entry.amount, entry.currency)}) تسویه شد.")
            }
        }
    }

    fun deleteExpense(expense: ExpenseEntry) {
        viewModelScope.launch {
            val res = repository.deleteExpense(expense)
            if (res.isSuccess) {
                val typeLabel = when {
                    expense.isDebt -> "بدهکاری"
                    expense.isReceivable -> "طلبکاری"
                    expense.isIncome -> "درآمد"
                    else -> "خرج"
                }
                postSnackbar("$typeLabel «${expense.title}» حذف شد.") {
                    repository.addExpense(
                        title = expense.title,
                        category = expense.category,
                        amount = expense.amount,
                        currency = expense.currency,
                        localEpochDay = expense.localEpochDay,
                        notes = expense.cleanNotes,
                        inputSource = expense.inputSource,
                        transactionType = expense.transactionType
                    )
                }
            }
        }
    }

    // --- Habit, Sleep & Exercise Operations ---

    fun createHabit(
        title: String,
        category: String,
        scheduleType: String,
        scheduleDays: List<Int>,
        targetPerPeriod: Int,
        linkedTaskCategory: String = ""
    ) {
        viewModelScope.launch {
            val res = repository.createHabit(
                title = title,
                category = category,
                scheduleType = scheduleType,
                scheduleDays = scheduleDays,
                targetPerPeriod = targetPerPeriod,
                linkedTaskCategory = linkedTaskCategory
            )
            if (res.isSuccess) {
                postSnackbar("عادت «$title» ایجاد شد.")
            }
        }
    }

    fun toggleHabitForDay(habit: Habit, epochDay: Long) {
        viewModelScope.launch {
            val existing = habitRecordsState.value.find { it.habitId == habit.id && it.localEpochDay == epochDay }
            repository.toggleHabitCompletionForDay(habit, epochDay, existing, "MANUAL")
        }
    }

    fun addSleepRecord(
        durationMinutes: Int,
        qualityRating: Int,
        notes: String
    ) {
        viewModelScope.launch {
            val res = repository.addSleepRecord(
                durationMinutes = durationMinutes,
                qualityRating = qualityRating,
                notes = notes,
                inputSource = "MANUAL"
            )
            if (res.isSuccess) {
                postSnackbar("رکورد خواب ثبت شد.")
            }
        }
    }

    fun addExerciseRecord(
        activityType: String,
        durationMinutes: Int,
        intensity: String,
        distanceMeters: Double = 0.0,
        repetitions: Int = 0,
        notes: String = ""
    ) {
        viewModelScope.launch {
            val res = repository.addExerciseRecord(
                activityType = activityType,
                durationMinutes = durationMinutes,
                intensity = intensity,
                distanceMeters = distanceMeters,
                repetitions = repetitions,
                notes = notes,
                inputSource = "MANUAL"
            )
            if (res.isSuccess) {
                postSnackbar("رکورد ورزش ثبت شد.")
            }
        }
    }

    // --- Voice Capture & Gemini Assistant Workflow ---

    fun analyzeVoiceOrChatCommand(
        inputText: String,
        inputSource: String = "VOICE"
    ) {
        val clean = inputText.trim()
        if (clean.isEmpty()) return

        viewModelScope.launch {
            _isAiProcessing.value = true
            val prefs = preferencesState.value
            val today = PersianDateAndUnitUtils.currentLocalEpochDay()

            if (prefs.retainConversations && inputSource == "AI_ASSISTANT") {
                repository.addConversationTurn("USER", clean)
            }

            repository.logActivityEvent(
                eventType = "AI_COMMAND_RECEIVED",
                entityType = "AI_COMMAND",
                entityId = "cmd_${System.currentTimeMillis()}",
                inputSource = inputSource,
                operationStatus = "PROPOSED",
                humanSummary = "دریافت دستور: «${clean.take(80)}»",
                recognizedTranscript = if (prefs.retainVoiceTranscripts) clean else ""
            )

            val tasks = (tasksState.value as? UiState.Success)?.data.orEmpty()
            val consumption = (consumptionState.value as? UiState.Success)?.data.orEmpty()
            val expenses = (expensesState.value as? UiState.Success)?.data.orEmpty()

            val result = GeminiAssistantService.processUserCommandOrQuery(
                userInput = clean,
                todayEpochDay = today,
                defaultCurrency = prefs.defaultCurrency,
                tasks = tasks,
                consumption = consumption,
                expenses = expenses,
                habits = habitsState.value,
                habitRecords = habitRecordsState.value,
                sleepRecords = sleepRecordsState.value,
                exerciseRecords = exerciseRecordsState.value,
                recentEvents = activityEventsState.value
            )

            if (result.operations.isNotEmpty()) {
                _pendingAiPreview.value = result
                repository.logActivityEvent(
                    eventType = "AI_OPERATION_PROPOSED",
                    entityType = "AI_COMMAND",
                    entityId = "prop_${System.currentTimeMillis()}",
                    inputSource = inputSource,
                    operationStatus = "PROPOSED",
                    afterSummary = result.operations.joinToString(" | ") { it.humanReadablePreview },
                    humanSummary = "پیشنهاد ${result.operations.size} عملیات هوشمند جهت تأیید کاربر"
                )
            } else if (prefs.retainConversations && inputSource == "AI_ASSISTANT") {
                repository.addConversationTurn("ASSISTANT", result.conversationalReply)
            }

            _isAiProcessing.value = false
        }
    }

    fun dismissPendingAiPreview(rawTranscript: String = "", inputSource: String = "VOICE") {
        val current = _pendingAiPreview.value ?: return
        _pendingAiPreview.value = null
        viewModelScope.launch {
            repository.logActivityEvent(
                eventType = "AI_OPERATION_REJECTED",
                entityType = "AI_COMMAND",
                entityId = "rej_${System.currentTimeMillis()}",
                inputSource = inputSource,
                operationStatus = "REJECTED",
                humanSummary = "لغو پیش‌نمایش عملیات توسط کاربر (${current.operations.size} مورد)",
                recognizedTranscript = if (preferencesState.value.retainVoiceTranscripts) rawTranscript else ""
            )
            postSnackbar("عملیات پیشنهادی لغو شد.")
        }
    }

    fun executeQuickSmartInput(
        rawInput: String,
        inputSource: String = "QUICK_BAR"
    ) {
        val clean = rawInput.trim()
        if (clean.isEmpty()) return

        val prefs = preferencesState.value
        val today = PersianDateAndUnitUtils.currentLocalEpochDay()
        val tasks = (tasksState.value as? UiState.Success)?.data.orEmpty()
        val consumption = (consumptionState.value as? UiState.Success)?.data.orEmpty()
        val expenses = (expensesState.value as? UiState.Success)?.data.orEmpty()

        val parsed = GeminiAssistantService.parsePersianCommandDeterministic(
            rawText = clean,
            todayEpochDay = today,
            defaultCurrency = prefs.defaultCurrency,
            tasks = tasks,
            consumption = consumption,
            expenses = expenses,
            habits = habitsState.value,
            habitRecords = habitRecordsState.value,
            sleepRecords = sleepRecordsState.value,
            exerciseRecords = exerciseRecordsState.value,
            recentEvents = activityEventsState.value
        )

        if (parsed.operations.isNotEmpty() && !parsed.isAmbiguous) {
            confirmAndExecuteOperations(parsed.operations, clean, inputSource)
        } else if (parsed.operations.isEmpty() && parsed.conversationalReply.isNotBlank()) {
            postSnackbar(parsed.conversationalReply.lines().firstOrNull() ?: parsed.conversationalReply)
        } else {
            analyzeVoiceOrChatCommand(clean, inputSource)
        }
    }

    fun confirmAndExecuteOperations(
        operations: List<ProposedAiOperation>,
        rawTranscript: String,
        inputSource: String = "VOICE"
    ) {
        val commandHash = "${rawTranscript.trim().lowercase()}:${operations.map { "${it.operationType}_${it.title}_${it.quantity}_${it.amount}_${it.transactionType}" }}"
        if (_lastSubmittedVoiceHash.value == commandHash && inputSource == "VOICE") {
            postSnackbar("این دستور قبلاً اجرا شده است (جلوگیری از ثبت تکراری).")
            _pendingAiPreview.value = null
            return
        }

        viewModelScope.launch {
            _isAiProcessing.value = true
            val today = PersianDateAndUnitUtils.currentLocalEpochDay()
            val correlationId = "corr_${System.currentTimeMillis()}"
            val executedSummaries = mutableListOf<String>()
            val createdExpenseIds = mutableListOf<String>()
            val createdTaskIds = mutableListOf<String>()
            val createdConsumptionIds = mutableListOf<String>()
            var anyFailed = false

            for ((idx, op) in operations.withIndex()) {
                val validated = GeminiAssistantService.validateOperation(op)
                if (validated.isFailure) {
                    anyFailed = true
                    repository.logActivityEvent(
                        eventType = "AI_OPERATION_FAILED",
                        entityType = op.operationType,
                        entityId = "invalid_$idx",
                        inputSource = inputSource,
                        operationStatus = "FAILED",
                        correlationId = correlationId,
                        humanSummary = "خطای اعتبارسنجی: ${validated.exceptionOrNull()?.message}"
                    )
                    continue
                }

                val targetDay = today + op.dueDateOffsetDays
                val idemKey = "idem_${commandHash.hashCode()}_${System.currentTimeMillis()}_$idx"

                val writeResult: Result<*> = when (op.operationType) {
                    "CreateTask" -> repository.createTask(
                        title = op.title,
                        category = op.category.ifBlank { "عمومی" },
                        priority = op.priority,
                        dueDateEpochDay = targetDay,
                        dueTimeMinutes = op.dueTimeMinutes,
                        estimatedDurationMinutes = if (op.durationMinutes > 0) op.durationMinutes else 30,
                        recurrenceRule = op.recurrenceRule,
                        inputSource = inputSource,
                        idempotencyKey = idemKey
                    ).also { res -> res.getOrNull()?.let { createdTaskIds.add(it) } }

                    "CompleteTask" -> {
                        val tasks = (tasksState.value as? UiState.Success)?.data.orEmpty()
                        val match = tasks.firstOrNull {
                            it.status != "COMPLETED" && it.title.contains(op.targetTaskTitle.ifBlank { op.title }, ignoreCase = true)
                        }
                        if (match != null) {
                            repository.completeTask(match, inputSource)
                        } else {
                            Result.failure<Unit>(IllegalStateException("تسک مورد نظر یافت نشد"))
                        }
                    }

                    "AddConsumptionEntry" -> repository.addConsumptionEntry(
                        itemName = op.title.ifBlank { op.category },
                        category = op.category.ifBlank { "WATER" },
                        quantity = op.quantity,
                        unit = op.unit.ifBlank { "ml" },
                        localEpochDay = targetDay,
                        inputSource = inputSource,
                        idempotencyKey = idemKey
                    ).also { res -> res.getOrNull()?.let { createdConsumptionIds.add(it) } }

                    "AddExpense" -> {
                        val curr = op.currency.ifBlank { preferencesState.value.defaultCurrency }
                        val normalizedAmt = PersianDateAndUnitUtils.normalizeColloquialTomanAmount(op.amount, curr)
                        repository.addExpense(
                            title = op.title.ifBlank { "هزینه" },
                            category = op.category.ifBlank { "OTHER" },
                            amount = normalizedAmt,
                            currency = curr,
                            localEpochDay = targetDay,
                            notes = op.notes,
                            inputSource = inputSource,
                            idempotencyKey = idemKey,
                            transactionType = op.transactionType.ifBlank { "EXPENSE" }
                        ).also { res -> res.getOrNull()?.let { createdExpenseIds.add(it) } }
                    }

                    "AddSleepRecord" -> repository.addSleepRecord(
                        durationMinutes = op.durationMinutes,
                        qualityRating = op.qualityRating,
                        localEpochDay = targetDay,
                        inputSource = inputSource,
                        idempotencyKey = idemKey
                    )

                    "AddExerciseRecord" -> repository.addExerciseRecord(
                        activityType = op.title.ifBlank { "ورزش" },
                        durationMinutes = op.durationMinutes,
                        intensity = op.intensity,
                        localEpochDay = targetDay,
                        inputSource = inputSource,
                        idempotencyKey = idemKey
                    )

                    else -> Result.success("SKIPPED")
                }

                if (writeResult.isSuccess) {
                    executedSummaries.add(op.humanReadablePreview)
                    repository.logActivityEvent(
                        eventType = "AI_OPERATION_EXECUTED",
                        entityType = op.operationType,
                        entityId = writeResult.getOrNull()?.toString() ?: "ok",
                        inputSource = inputSource,
                        operationStatus = "EXECUTED",
                        correlationId = correlationId,
                        afterSummary = op.humanReadablePreview,
                        humanSummary = "اجرای موفق: ${op.humanReadablePreview}",
                        recognizedTranscript = if (preferencesState.value.retainVoiceTranscripts) rawTranscript else ""
                    )
                } else {
                    anyFailed = true
                    repository.logActivityEvent(
                        eventType = "AI_OPERATION_FAILED",
                        entityType = op.operationType,
                        entityId = "err_$idx",
                        inputSource = inputSource,
                        operationStatus = "FAILED",
                        correlationId = correlationId,
                        humanSummary = "خطا در اجرای عملیات: ${writeResult.exceptionOrNull()?.localizedMessage ?: ""}"
                    )
                }
            }

            if (executedSummaries.isNotEmpty()) {
                _lastSubmittedVoiceHash.value = commandHash
                val summaryMsg = "✅ ${executedSummaries.size} عملیات با موفقیت در دیتابیس ذخیره شد:\n" +
                    executedSummaries.joinToString("\n") { "• $it" }
                if (preferencesState.value.retainConversations) {
                    repository.addConversationTurn("ASSISTANT", summaryMsg)
                }
                val snackbarText = if (executedSummaries.size == 1) {
                    "✓ ${executedSummaries.first()}"
                } else {
                    "✓ ${PersianDateAndUnitUtils.toPersianDigits(executedSummaries.size.toString())} مورد ثبت شد"
                }
                postSnackbar(snackbarText) {
                    val curExps = (expensesState.value as? UiState.Success)?.data.orEmpty()
                    createdExpenseIds.forEach { expId ->
                        curExps.find { it.id == expId }?.let { repository.deleteExpense(it) }
                    }
                    val curTasks = (tasksState.value as? UiState.Success)?.data.orEmpty()
                    createdTaskIds.forEach { tId ->
                        curTasks.find { it.id == tId }?.let { repository.deleteTask(it) }
                    }
                    val curCons = (consumptionState.value as? UiState.Success)?.data.orEmpty()
                    createdConsumptionIds.forEach { cId ->
                        curCons.find { it.id == cId }?.let { repository.deleteConsumptionEntry(it) }
                    }
                }
            } else if (anyFailed) {
                postSnackbar("برخی عملیات‌ها به دلیل نقص اطلاعات ثبت نشدند.")
            }

            _pendingAiPreview.value = null
            _isAiProcessing.value = false
        }
    }

    // --- Settings, Onboarding & Export ---

    fun updatePreferences(updated: UserPreferencesDoc) {
        viewModelScope.launch {
            prefsStore.saveLocalPreferences(updated)
            repository.saveUserPreferencesToCloud(updated)
            postSnackbar("تنظیمات ذخیره شد.")
        }
    }

    fun clearAiConversationHistory() {
        viewModelScope.launch {
            repository.clearAllConversations()
            postSnackbar("تاریخچه گفتگوهای هوش مصنوعی پاک شد.")
        }
    }

    fun deleteAllAccountData(onCompleted: () -> Unit) {
        viewModelScope.launch {
            val res = repository.deleteAllUserData()
            if (res.isSuccess) {
                postSnackbar("تمام داده‌های حساب کاربری حذف شد.")
                onCompleted()
            } else {
                postSnackbar("خطا در حذف داده‌ها.")
            }
        }
    }

    fun exportAllDataAsJson(): String {
        val tasks = (tasksState.value as? UiState.Success)?.data.orEmpty()
        val consumption = (consumptionState.value as? UiState.Success)?.data.orEmpty()
        val expenses = (expensesState.value as? UiState.Success)?.data.orEmpty()
        val habits = habitsState.value
        val sleep = sleepRecordsState.value
        val exercise = exerciseRecordsState.value

        val root = JSONObject().apply {
            put("app", "Personal Life OS")
            put("exportedAtMillis", System.currentTimeMillis())
            put("userId", currentUserId)
            put("tasksCount", tasks.size)
            put("tasks", JSONArray().apply {
                tasks.forEach { t ->
                    put(JSONObject().apply {
                        put("id", t.id)
                        put("title", t.title)
                        put("category", t.category)
                        put("priority", t.priority)
                        put("status", t.status)
                        put("dueDateEpochDay", t.dueDateEpochDay)
                        put("recurrenceRule", t.recurrenceRule)
                    })
                }
            })
            put("consumptionCount", consumption.size)
            put("expensesCount", expenses.size)
            put("habitsCount", habits.size)
            put("sleepRecordsCount", sleep.size)
            put("exerciseRecordsCount", exercise.size)
        }
        viewModelScope.launch {
            repository.logActivityEvent(
                eventType = "DATA_EXPORTED",
                entityType = "EXPORT",
                entityId = "json_export",
                inputSource = "MANUAL",
                humanSummary = "خروجی JSON داده‌های کاربر تهیه شد"
            )
        }
        return root.toString(2)
    }

    private companion object {
        const val TAG = "LifeOsVM"
    }
}
