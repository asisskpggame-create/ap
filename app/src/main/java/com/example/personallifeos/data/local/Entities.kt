package com.example.personallifeos.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.personallifeos.data.model.DebtItem
import com.example.personallifeos.data.model.DebtType
import com.example.personallifeos.data.model.ExpenseItem
import com.example.personallifeos.data.model.HabitItem
import com.example.personallifeos.data.model.HealthLog
import com.example.personallifeos.data.model.TaskCategory
import com.example.personallifeos.data.model.TaskItem
import com.example.personallifeos.data.model.TaskPriority
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
    val linkedDebtId: String? = null
) {
    fun toDomain() = TaskItem(
        id = id,
        title = title,
        category = TaskCategory.entries.find { it.name.equals(category, ignoreCase = true) } ?: TaskCategory.WORK,
        priority = TaskPriority.entries.find { it.name.equals(priority, ignoreCase = true) } ?: TaskPriority.MEDIUM,
        completed = completed,
        dueDate = dueDate,
        createdAt = createdAt,
        linkedDebtId = linkedDebtId
    )

    companion object {
        fun fromDomain(task: TaskItem) = TaskEntity(
            id = task.id,
            title = task.title,
            category = task.category.name,
            priority = task.priority.name,
            completed = task.completed,
            dueDate = task.dueDate,
            createdAt = task.createdAt,
            linkedDebtId = task.linkedDebtId
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
    val caloriesBurned: Int
) {
    fun toDomain() = HealthLog(
        id = id,
        date = date,
        sleepHours = sleepHours,
        sleepQuality = sleepQuality,
        waterGlasses = waterGlasses,
        exerciseMinutes = exerciseMinutes,
        exerciseType = exerciseType,
        caloriesBurned = caloriesBurned
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
            caloriesBurned = log.caloriesBurned
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
