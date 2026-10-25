package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.LifeOsDatabase
import com.example.data.local.task.TaskDao
import com.example.data.local.task.TaskEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * UI State representation for the Tasks Screen.
 */
sealed interface TasksUiState {
    data object Loading : TasksUiState
    data class Success(
        val tasks: List<TaskEntity>,
        val activeCount: Int = 0,
        val completedCount: Int = 0,
        val searchQuery: String = "",
        val selectedCategory: String = "ALL",
        val selectedStatus: String = "ALL"
    ) : TasksUiState
    data class Error(val message: String) : TasksUiState
}

/**
 * ViewModel for the 'Tasks' module.
 *
 * Exposes the reactive task list as a [StateFlow] and provides methods for:
 * 1. [addTask]: Adds a new task entity with optional Persian category, priority, and due dates.
 * 2. [deleteTask]: Deletes a task by ID or entity.
 * 3. [toggleTaskCompletion]: Toggles completion status between COMPLETED and PENDING.
 * 4. Filtering and Persian search capabilities.
 */
class TasksViewModel(
    private val taskDao: TaskDao,
    val userId: String = "local_device_user"
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("ALL")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedStatus = MutableStateFlow("ALL")
    val selectedStatus: StateFlow<String> = _selectedStatus.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    /**
     * Primary reactive StateFlow emitting the live list of tasks for the current user.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val tasks: StateFlow<List<TaskEntity>> = combine(
        _searchQuery,
        _selectedCategory,
        _selectedStatus
    ) { query, category, status ->
        Triple(query, category, status)
    }.flatMapLatest { (query, category, status) ->
        val sourceFlow = if (query.isBlank()) {
            taskDao.observeAllTasks(userId)
        } else {
            taskDao.searchTasks(userId, query)
        }

        sourceFlow.map { taskList ->
            taskList.filter { task ->
                val matchesCategory = (category == "ALL" || task.category == category)
                val matchesStatus = when (status) {
                    "ALL" -> true
                    "PENDING" -> task.status != "COMPLETED" && task.status != "ARCHIVED"
                    "COMPLETED" -> task.status == "COMPLETED"
                    "ARCHIVED" -> task.status == "ARCHIVED"
                    else -> task.status == status
                }
                matchesCategory && matchesStatus
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    /**
     * High-level UI State combining task list, loading state, counters, and errors.
     */
    val uiState: StateFlow<TasksUiState> = tasks
        .map<List<TaskEntity>, TasksUiState> { taskList ->
            val active = taskList.count { it.status != "COMPLETED" && it.status != "ARCHIVED" }
            val completed = taskList.count { it.status == "COMPLETED" }
            TasksUiState.Success(
                tasks = taskList,
                activeCount = active,
                completedCount = completed,
                searchQuery = _searchQuery.value,
                selectedCategory = _selectedCategory.value,
                selectedStatus = _selectedStatus.value
            )
        }
        .catch { error ->
            emit(TasksUiState.Error(error.localizedMessage ?: "خطا در بارگذاری تسک‌ها"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = TasksUiState.Loading
        )

    // ==========================================
    // 1. ADD TASK
    // ==========================================

    /**
     * Adds a new task from individual parameters with Persian localization defaults.
     */
    fun addTask(
        title: String,
        description: String = "",
        category: String = "شخصی",
        priority: String = "MEDIUM",
        dueDateEpochDay: Long = -1L,
        dueTimeMinutes: Int = -1,
        reminderAtMillis: Long = -1L,
        estimatedDurationMinutes: Int = 30,
        subtasksJson: String = "[]",
        recurrenceRule: String = "NONE",
        notes: String = ""
    ) {
        if (title.isBlank()) {
            _userMessage.value = "عنوان تسک نمی‌تواند خالی باشد"
            return
        }

        val newTask = TaskEntity.create(
            title = title,
            userId = userId,
            description = description,
            category = category,
            priority = priority,
            dueDateEpochDay = dueDateEpochDay,
            dueTimeMinutes = dueTimeMinutes,
            reminderAtMillis = reminderAtMillis,
            estimatedDurationMinutes = estimatedDurationMinutes,
            subtasksJson = subtasksJson,
            recurrenceRule = recurrenceRule,
            notes = notes
        )

        viewModelScope.launch {
            try {
                taskDao.insertTask(newTask)
                _userMessage.value = "تسک «${newTask.title}» اضافه شد"
            } catch (e: Exception) {
                _userMessage.value = "خطا در افزودن تسک: ${e.message}"
            }
        }
    }

    /**
     * Adds an existing [TaskEntity] instance directly.
     */
    fun addTask(task: TaskEntity) {
        viewModelScope.launch {
            try {
                taskDao.insertTask(task.copy(userId = userId))
                _userMessage.value = "تسک «${task.title}» اضافه شد"
            } catch (e: Exception) {
                _userMessage.value = "خطا در افزودن تسک: ${e.message}"
            }
        }
    }

    // ==========================================
    // 2. DELETE TASK
    // ==========================================

    /**
     * Deletes a task by its unique ID.
     */
    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            try {
                taskDao.deleteTaskById(taskId)
                _userMessage.value = "تسک حذف شد"
            } catch (e: Exception) {
                _userMessage.value = "خطا در حذف تسک: ${e.message}"
            }
        }
    }

    /**
     * Deletes a task by entity.
     */
    fun deleteTask(task: TaskEntity) {
        deleteTask(task.id)
    }

    // ==========================================
    // 3. TOGGLE TASK COMPLETION
    // ==========================================

    /**
     * Toggles the completion status of a task between 'COMPLETED' and 'PENDING'.
     * Sets completion timestamp when completed, or clears it when uncompleted.
     */
    fun toggleTaskCompletion(taskId: String) {
        viewModelScope.launch {
            try {
                val existingTask = taskDao.getTaskById(taskId) ?: return@launch
                val isCurrentlyCompleted = existingTask.status.equals("COMPLETED", ignoreCase = true)
                val newStatus = if (isCurrentlyCompleted) "PENDING" else "COMPLETED"
                val completedAt = if (newStatus == "COMPLETED") System.currentTimeMillis() else -1L

                taskDao.updateTaskStatus(
                    taskId = taskId,
                    newStatus = newStatus,
                    completedAtMillis = completedAt,
                    updatedAtMillis = System.currentTimeMillis()
                )

                _userMessage.value = if (newStatus == "COMPLETED") {
                    "تسک «${existingTask.title}» انجام شد ✦"
                } else {
                    "تسک «${existingTask.title}» به لیست در انتظار بازگشت"
                }
            } catch (e: Exception) {
                _userMessage.value = "خطا در به‌روزرسانی وضعیت تسک: ${e.message}"
            }
        }
    }

    /**
     * Toggles the completion status directly from a [TaskEntity].
     */
    fun toggleTaskCompletion(task: TaskEntity) {
        toggleTaskCompletion(task.id)
    }

    // ==========================================
    // 4. SEARCH & FILTER CONTROLS
    // ==========================================

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategoryFilter(category: String) {
        _selectedCategory.value = category
    }

    fun setStatusFilter(status: String) {
        _selectedStatus.value = status
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    companion object {
        fun provideFactory(
            taskDao: TaskDao,
            userId: String = "local_device_user"
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(TasksViewModel::class.java)) {
                    return TasksViewModel(taskDao, userId) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}
