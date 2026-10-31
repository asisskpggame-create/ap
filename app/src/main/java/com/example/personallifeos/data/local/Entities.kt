package com.example.personallifeos.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.personallifeos.data.model.DebtItem
import com.example.personallifeos.data.model.DebtType
import com.example.personallifeos.data.model.ExpenseItem
import com.example.personallifeos.data.model.HabitItem
import com.example.personallifeos.data.model.HealthLog
import com.example.personallifeos.data.model.ReminderEntityType
import com.example.personallifeos.data.model.ReminderItem
import com.example.personallifeos.data.model.ReminderRepeatMode
import com.example.personallifeos.data.model.SubtaskItem
import com.example.personallifeos.data.model.TaskCategory
import com.example.personallifeos.data.model.TaskItem
import com.example.personallifeos.data.model.TaskPriority
import com.example.personallifeos.data.model.TaskStatus
import com.example.personallifeos.data.model.TopicItem
import com.example.personallifeos.data.model.TransactionType
import com.example.personallifeos.data.model.VaultNote

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String,
    val priority: String,
    val completed: Boolean,
    val dueDate: String,
    val createdAt: String,
    val linkedDebtId: String? = null,
    val topicId: String? = null,
    val description: String = "",
    val status: String = if (completed) "COMPLETED" else "TODO",
    val subtasksSerialized: String = "",
    val reminderTime: String? = null,
    val reminderEpochMillis: Long? = null,
    val reminderRepeatMode: String = "ONCE",
    val reminderBeforeMinutes: Int = 0,
    val isMajorMission: Boolean = false,
    val completedAt: String? = null,
    val activityLogSerialized: String = ""
) {
    fun toDomain(): TaskItem {
        return toModelInternal()
    }

    fun toModel(): TaskItem = toDomain()

    private fun toModelInternal(): TaskItem {
        val parsedStatus = TaskStatus.entries.find { it.name.equals(status, ignoreCase = true) }
            ?: if (completed) TaskStatus.COMPLETED else TaskStatus.TODO
        val isDone = completed || parsedStatus == TaskStatus.COMPLETED
        return TaskItem(
            id = id,
            title = title,
            category = TaskCategory.entries.find { it.name.equals(category, ignoreCase = true) } ?: TaskCategory.WORK,
            priority = TaskPriority.entries.find { it.name.equals(priority, ignoreCase = true) } ?: TaskPriority.MEDIUM,
            completed = isDone,
            dueDate = dueDate,
            createdAt = createdAt,
            linkedDebtId = linkedDebtId,
            topicId = topicId,
            description = description,
            status = if (isDone) TaskStatus.COMPLETED else parsedStatus,
            subtasks = deserializeSubtasks(subtasksSerialized),
            reminderTime = reminderTime,
            reminderEpochMillis = reminderEpochMillis,
            reminderRepeatMode = ReminderRepeatMode.entries.find { it.name.equals(reminderRepeatMode, ignoreCase = true) } ?: ReminderRepeatMode.ONCE,
            reminderBeforeMinutes = reminderBeforeMinutes,
            isMajorMission = isMajorMission,
            completedAt = completedAt,
            activityLog = if (activityLogSerialized.isBlank()) emptyList() else activityLogSerialized.split("||").filter { it.isNotBlank() }
        )
    }

    companion object {
        fun fromModel(task: TaskItem): TaskEntity = fromDomain(task)

        fun fromDomain(task: TaskItem): TaskEntity {
            val isDone = task.completed || task.status == TaskStatus.COMPLETED
            val finalStatus = if (isDone) TaskStatus.COMPLETED else task.status
            return TaskEntity(
                id = task.id,
                title = task.title,
                category = task.category.name,
                priority = task.priority.name,
                completed = isDone,
                dueDate = task.dueDate,
                createdAt = task.createdAt,
                linkedDebtId = task.linkedDebtId,
                topicId = task.topicId,
                description = task.description,
                status = finalStatus.name,
                subtasksSerialized = serializeSubtasks(task.subtasks),
                reminderTime = task.reminderTime,
                reminderEpochMillis = task.reminderEpochMillis,
                reminderRepeatMode = task.reminderRepeatMode.name,
                reminderBeforeMinutes = task.reminderBeforeMinutes,
                isMajorMission = task.isMajorMission,
                completedAt = task.completedAt,
                activityLogSerialized = task.activityLog.joinToString("||")
            )
        }

        fun serializeSubtasks(list: List<SubtaskItem>): String {
            if (list.isEmpty()) return ""
            return list.joinToString(";;") { item ->
                val cleanTitle = item.title.replace("::", " ").replace(";;", " ")
                "${item.id}::${cleanTitle}::${item.completed}"
            }
        }

        fun deserializeSubtasks(raw: String): List<SubtaskItem> {
            if (raw.isBlank()) return emptyList()
            return raw.split(";;").mapNotNull { entry ->
                val parts = entry.split("::")
                if (parts.size >= 3) {
                    SubtaskItem(
                        id = parts[0],
                        title = parts[1],
                        completed = parts[2].toBooleanStrictOrNull() ?: false
                    )
                } else null
            }
        }
    }
}

@Entity(tableName = "topics")
data class TopicEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val iconEmoji: String,
    val colorHex: Long,
    val startDate: String,
    val endDate: String? = null,
    val priority: String = "HIGH",
    val tagOrCategory: String = "عمومی",
    val isActive: Boolean = true,
    val isArchived: Boolean = false,
    val reminderEnabled: Boolean = true,
    val reminderTime: String? = null,
    val topicNote: String = "",
    val createdAt: String = "۱۴۰۵/۰۸/۰۱"
) {
    fun toDomain() = TopicItem(
        id = id,
        title = title,
        description = description,
        iconEmoji = iconEmoji,
        colorHex = colorHex,
        startDate = startDate,
        endDate = endDate,
        priority = TaskPriority.entries.find { it.name.equals(priority, ignoreCase = true) } ?: TaskPriority.HIGH,
        tagOrCategory = tagOrCategory,
        isActive = isActive,
        isArchived = isArchived,
        reminderEnabled = reminderEnabled,
        reminderTime = reminderTime,
        topicNote = topicNote,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(topic: TopicItem) = TopicEntity(
            id = topic.id,
            title = topic.title,
            description = topic.description,
            iconEmoji = topic.iconEmoji,
            colorHex = topic.colorHex,
            startDate = topic.startDate,
            endDate = topic.endDate,
            priority = topic.priority.name,
            tagOrCategory = topic.tagOrCategory,
            isActive = topic.isActive,
            isArchived = topic.isArchived,
            reminderEnabled = topic.reminderEnabled,
            reminderTime = topic.reminderTime,
            topicNote = topic.topicNote,
            createdAt = topic.createdAt
        )
    }
}

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey val id: String,
    val entityType: String,
    val entityId: String,
    val title: String,
    val triggerTime: String,
    val isSnoozed: Boolean = false,
    val isCompleted: Boolean = false,
    val sensitiveAmountToman: Long? = null,
    val topicId: String? = null,
    val topicTitle: String? = null,
    val repeatMode: String = "ONCE",
    val snoozeIntervalMinutes: Int = 60,
    val triggerEpochMillis: Long? = null,
    val isEnabled: Boolean = true
) {
    fun toDomain() = ReminderItem(
        id = id,
        entityType = ReminderEntityType.entries.find { it.name.equals(entityType, ignoreCase = true) } ?: ReminderEntityType.TASK,
        entityId = entityId,
        title = title,
        triggerTime = triggerTime,
        isSnoozed = isSnoozed,
        isCompleted = isCompleted,
        sensitiveAmountToman = sensitiveAmountToman,
        topicId = topicId,
        topicTitle = topicTitle,
        repeatMode = ReminderRepeatMode.entries.find { it.name.equals(repeatMode, ignoreCase = true) } ?: ReminderRepeatMode.ONCE,
        snoozeIntervalMinutes = snoozeIntervalMinutes,
        triggerEpochMillis = triggerEpochMillis,
        isEnabled = isEnabled
    )

    companion object {
        fun fromDomain(rem: ReminderItem) = ReminderEntity(
            id = rem.id,
            entityType = rem.entityType.name,
            entityId = rem.entityId,
            title = rem.title,
            triggerTime = rem.triggerTime,
            isSnoozed = rem.isSnoozed,
            isCompleted = rem.isCompleted,
            sensitiveAmountToman = rem.sensitiveAmountToman,
            topicId = rem.topicId,
            topicTitle = rem.topicTitle,
            repeatMode = rem.repeatMode.name,
            snoozeIntervalMinutes = rem.snoozeIntervalMinutes,
            triggerEpochMillis = rem.triggerEpochMillis,
            isEnabled = rem.isEnabled
        )
    }
}

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String,
    val title: String,
    val streak: Int,
    val completedToday: Boolean,
    val category: String
) {
    fun toDomain() = HabitItem(
        id = id,
        title = title,
        streak = streak,
        completedToday = completedToday,
        category = TaskCategory.entries.find { it.name.equals(category, ignoreCase = true) } ?: TaskCategory.HEALTH
    )

    companion object {
        fun fromDomain(habit: HabitItem) = HabitEntity(
            id = habit.id,
            title = habit.title,
            streak = habit.streak,
            completedToday = habit.completedToday,
            category = habit.category.name
        )
    }
}

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val title: String,
    val amount: Long,
    val category: String,
    val date: String,
    val isSmsParsed: Boolean,
    val bankName: String?,
    val type: String = "EXPENSE",
    val accountName: String? = "حساب پیش‌فرض",
    val idempotencyKey: String? = null
) {
    fun toDomain() = ExpenseItem(
        id = id,
        title = title,
        amount = amount,
        category = category,
        date = date,
        isSmsParsed = isSmsParsed,
        bankName = bankName,
        accountName = accountName ?: bankName ?: "حساب پیش‌فرض",
        type = TransactionType.entries.find { it.name.equals(type, ignoreCase = true) } ?: TransactionType.EXPENSE,
        idempotencyKey = idempotencyKey
    )

    companion object {
        fun fromDomain(expense: ExpenseItem) = ExpenseEntity(
            id = expense.id,
            title = expense.title,
            amount = expense.amount,
            category = expense.category,
            date = expense.date,
            isSmsParsed = expense.isSmsParsed,
            bankName = expense.bankName,
            type = expense.type.name,
            accountName = expense.accountName ?: expense.bankName ?: "حساب پیش‌فرض",
            idempotencyKey = expense.idempotencyKey
        )
    }
}

@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey val id: String,
    val title: String,
    val type: String, // DEBT or RECEIVABLE
    val totalAmount: Long,
    val paidAmount: Long,
    val dueDate: String,
    val isSettled: Boolean,
    val personName: String
) {
    fun toDomain() = DebtItem(
        id = id,
        title = title,
        type = DebtType.entries.find { it.name.equals(type, ignoreCase = true) } ?: DebtType.DEBT,
        totalAmount = totalAmount,
        paidAmount = paidAmount,
        dueDate = dueDate,
        isSettled = isSettled,
        personName = personName
    )

    companion object {
        fun fromDomain(d: DebtItem) = DebtEntity(
            id = d.id,
            title = d.title,
            type = d.type.name,
            totalAmount = d.totalAmount,
            paidAmount = d.paidAmount,
            dueDate = d.dueDate,
            isSettled = d.isSettled,
            personName = d.personName
        )
    }
}

@Entity(tableName = "health_log")
data class HealthLogEntity(
    @PrimaryKey val id: String,
    val date: String,
    val sleepHours: Float,
    val sleepQuality: String,
    val waterGlasses: Int,
    val exerciseMinutes: Int,
    val exerciseType: String,
    val caloriesBurned: Int,
    val dailyCalorieIntake: Int = 1850,
    val lastScannedFoodSummary: String? = null
) {
    fun toDomain() = HealthLog(
        id = id,
        date = date,
        sleepHours = sleepHours,
        sleepQuality = sleepQuality,
        waterGlasses = waterGlasses,
        exerciseMinutes = exerciseMinutes,
        exerciseType = exerciseType,
        caloriesBurned = caloriesBurned,
        dailyCalorieIntake = dailyCalorieIntake,
        lastScannedFoodSummary = lastScannedFoodSummary
    )

    companion object {
        fun fromDomain(log: HealthLog) = HealthLogEntity(
            id = log.id,
            date = log.date,
            sleepHours = log.sleepHours,
            sleepQuality = log.sleepQuality,
            waterGlasses = log.waterGlasses,
            exerciseMinutes = log.exerciseMinutes,
            exerciseType = log.exerciseType,
            caloriesBurned = log.caloriesBurned,
            dailyCalorieIntake = log.dailyCalorieIntake,
            lastScannedFoodSummary = log.lastScannedFoodSummary
        )
    }
}

@Entity(tableName = "vault_notes")
data class VaultNoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val category: String,
    val updatedAt: String,
    val isEncrypted: Boolean
) {
    fun toDomain() = VaultNote(
        id = id,
        title = title,
        content = content,
        category = category,
        updatedAt = updatedAt,
        isEncrypted = isEncrypted
    )

    companion object {
        fun fromDomain(note: VaultNote) = VaultNoteEntity(
            id = note.id,
            title = note.title,
            content = note.content,
            category = note.category,
            updatedAt = note.updatedAt,
            isEncrypted = note.isEncrypted
        )
    }
}
