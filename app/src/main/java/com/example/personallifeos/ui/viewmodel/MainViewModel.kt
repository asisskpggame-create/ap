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
import com.example.personallifeos.data.model.VaultNote
import com.example.personallifeos.data.repository.LifeOsRepository
import com.example.personallifeos.sms.BankSmsParsedResult
import com.example.personallifeos.sms.BankType
import com.example.personallifeos.sms.IranianBankSmsEngine
import com.example.personallifeos.sms.SmsTransactionDirection
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

    init {
        viewModelScope.launch {
            repository.healthLog.collect { log ->
                if (log != null) {
                    _healthLog.value = log
                }
            }
        }
    }

    val vaultNotes: StateFlow<List<VaultNote>> = repository.allVaultNotes.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // Chat Messages State
    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                id = "m1",
                sender = "gemini",
                text = "سلام! من دستیار هوشمند جمینای شما در Personal Life OS هستم. چطور می‌توانم در کارهای امروز به شما کمک کنم؟",
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

    // Expenses CRUD
    fun addExpense(title: String, amount: Long, category: String, bankName: String? = null, isSms: Boolean = false) {
        viewModelScope.launch {
            val newExpense = ExpenseItem(
                id = System.currentTimeMillis().toString(),
                title = title,
                amount = amount,
                category = category,
                date = PersianUtils.getPersianDateString(),
                isSmsParsed = isSms,
                bankName = bankName
            )
            repository.insertExpense(newExpense)
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            repository.deleteExpense(expenseId)
        }
    }

    fun parseAndAddSms(smsText: String): String {
        val parsed = IranianBankSmsEngine.parse(smsText)
        if (parsed.isOtpOrServiceMessage) {
            return "پیامک سیستمی یا رمز پویا است (فاقد گردش مالی)."
        }
        if (parsed.amountToman <= 0) {
            return "مبلغ یا الگوی تراکنش بانکی شناسایی نشد."
        }
        val title = parsed.merchantOrTerminal ?: parsed.destination ?: "${parsed.direction.faName} ${parsed.bank.faName}"
        addExpense(
            title = title,
            amount = parsed.amountToman,
            category = parsed.suggestedCategory,
            bankName = parsed.bank.faName,
            isSms = true
        )
        return "تراکنش ${PersianUtils.formatToman(parsed.amountToman)} از ${parsed.bank.faName} با موفقیت ثبت شد ✓"
    }

    // Health Updates
    fun updateWater(delta: Int) {
        val current = _healthLog.value
        val newCount = maxOf(0, current.waterGlasses + delta)
        val updated = current.copy(waterGlasses = newCount)
        _healthLog.value = updated
        viewModelScope.launch { repository.updateHealthLog(updated) }
    }

    fun updateSleep(hours: Float, quality: String) {
        val updated = _healthLog.value.copy(sleepHours = hours, sleepQuality = quality)
        _healthLog.value = updated
        viewModelScope.launch { repository.updateHealthLog(updated) }
    }

    fun updateExercise(minutes: Int, type: String, calories: Int) {
        val updated = _healthLog.value.copy(
            exerciseMinutes = minutes,
            exerciseType = type,
            caloriesBurned = calories
        )
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
            delay(900) // Simulated Gemini AI inference
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

    // Factory Reset
    fun resetData() {
        viewModelScope.launch {
            repository.resetAllData()
            _healthLog.value = _defaultHealth
        }
    }
}
