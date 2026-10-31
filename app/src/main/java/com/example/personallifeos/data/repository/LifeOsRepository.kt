package com.example.personallifeos.data.repository

import com.example.personallifeos.data.local.DebtEntity
import com.example.personallifeos.data.local.ExpenseEntity
import com.example.personallifeos.data.local.HabitEntity
import com.example.personallifeos.data.local.HealthLogEntity
import com.example.personallifeos.data.local.LifeOsDao
import com.example.personallifeos.data.local.ReminderEntity
import com.example.personallifeos.data.local.TaskEntity
import com.example.personallifeos.data.local.TopicEntity
import com.example.personallifeos.data.local.VaultNoteEntity
import com.example.personallifeos.data.model.DebtItem
import com.example.personallifeos.data.model.ExpenseItem
import com.example.personallifeos.data.model.HabitItem
import com.example.personallifeos.data.model.HealthLog
import com.example.personallifeos.data.model.ReminderItem
import com.example.personallifeos.data.model.TaskItem
import com.example.personallifeos.data.model.TopicItem
import com.example.personallifeos.data.model.VaultNote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LifeOsRepository(private val dao: LifeOsDao) {

    val allTasks: Flow<List<TaskItem>> = dao.getAllTasks().map { list ->
        list.map { it.toDomain() }
    }

    val allTopics: Flow<List<TopicItem>> = dao.getAllTopics().map { list ->
        list.map { it.toDomain() }
    }

    val allReminders: Flow<List<ReminderItem>> = dao.getAllReminders().map { list ->
        list.map { it.toDomain() }
    }

    val allHabits: Flow<List<HabitItem>> = dao.getAllHabits().map { list ->
        list.map { it.toDomain() }
    }

    val allExpenses: Flow<List<ExpenseItem>> = dao.getAllExpenses().map { list ->
        list.map { it.toDomain() }
    }

    val allDebts: Flow<List<DebtItem>> = dao.getAllDebts().map { list ->
        list.map { it.toDomain() }
    }

    val healthLog: Flow<HealthLog?> = dao.getHealthLog().map { it?.toDomain() }

    val allVaultNotes: Flow<List<VaultNote>> = dao.getAllVaultNotes().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun insertTask(task: TaskItem) {
        dao.insertTask(TaskEntity.fromDomain(task))
    }

    suspend fun insertTasks(tasks: List<TaskItem>) {
        dao.insertTasks(tasks.map { TaskEntity.fromDomain(it) })
    }

    suspend fun updateTask(task: TaskItem) {
        dao.updateTask(TaskEntity.fromDomain(task))
    }

    suspend fun deleteTask(taskId: String) {
        dao.deleteTask(taskId)
    }

    suspend fun deleteTasksByTopicId(topicId: String) {
        dao.deleteTasksByTopicId(topicId)
    }

    suspend fun detachTasksFromTopic(topicId: String) {
        dao.detachTasksFromTopic(topicId)
    }

    suspend fun insertTopic(topic: TopicItem) {
        dao.insertTopic(TopicEntity.fromDomain(topic))
    }

    suspend fun updateTopic(topic: TopicItem) {
        dao.updateTopic(TopicEntity.fromDomain(topic))
    }

    suspend fun deleteTopic(topicId: String, deleteChildTasks: Boolean = false) {
        if (deleteChildTasks) {
            dao.deleteTasksByTopicId(topicId)
        } else {
            dao.detachTasksFromTopic(topicId)
        }
        dao.deleteRemindersByTopic(topicId)
        dao.deleteTopic(topicId)
    }

    suspend fun upsertReminder(reminder: ReminderItem) {
        dao.insertReminder(ReminderEntity.fromDomain(reminder))
    }

    suspend fun deleteReminder(reminderId: String) {
        dao.deleteReminder(reminderId)
    }

    suspend fun deleteReminderByEntity(entityType: String, entityId: String) {
        dao.deleteReminderByEntity(entityType, entityId)
    }

    suspend fun insertHabit(habit: HabitItem) {
        dao.insertHabit(HabitEntity.fromDomain(habit))
    }

    suspend fun updateHabit(habit: HabitItem) {
        dao.updateHabit(HabitEntity.fromDomain(habit))
    }

    suspend fun insertExpense(expense: ExpenseItem) {
        dao.insertExpense(ExpenseEntity.fromDomain(expense))
    }

    suspend fun updateExpense(expense: ExpenseItem) {
        dao.updateExpense(ExpenseEntity.fromDomain(expense))
    }

    suspend fun getExpenseByIdempotencyKey(key: String): ExpenseItem? {
        return dao.getExpenseByIdempotencyKey(key)?.toDomain()
    }

    suspend fun deleteExpense(expenseId: String) {
        dao.deleteExpense(expenseId)
    }

    suspend fun insertDebt(debt: DebtItem) {
        dao.insertDebt(DebtEntity.fromDomain(debt))
    }

    suspend fun updateDebt(debt: DebtItem) {
        dao.updateDebt(DebtEntity.fromDomain(debt))
    }

    suspend fun deleteDebt(debtId: String) {
        dao.deleteDebt(debtId)
    }

    suspend fun updateHealthLog(log: HealthLog) {
        dao.insertOrUpdateHealthLog(HealthLogEntity.fromDomain(log))
    }

    suspend fun insertVaultNote(note: VaultNote) {
        dao.insertVaultNote(VaultNoteEntity.fromDomain(note))
    }

    suspend fun deleteVaultNote(noteId: String) {
        dao.deleteVaultNote(noteId)
    }

    suspend fun resetAllData() {
        dao.clearTasks()
        dao.clearTopics()
        dao.clearReminders()
        dao.clearHabits()
        dao.clearExpenses()
        dao.clearDebts()
        dao.clearVaultNotes()
    }
}
