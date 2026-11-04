package com.example.personallifeos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LifeOsDao {

    // Tasks
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    suspend fun getTaskById(taskId: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: String)

    @Query("DELETE FROM tasks WHERE topicId = :topicId")
    suspend fun deleteTasksByTopicId(topicId: String)

    @Query("UPDATE tasks SET topicId = NULL WHERE topicId = :topicId")
    suspend fun detachTasksFromTopic(topicId: String)

    // Topics
    @Query("SELECT * FROM topics ORDER BY createdAt DESC")
    fun getAllTopics(): Flow<List<TopicEntity>>

    @Query("SELECT * FROM topics WHERE id = :topicId LIMIT 1")
    suspend fun getTopicById(topicId: String): TopicEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: TopicEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopics(topics: List<TopicEntity>)

    @Update
    suspend fun updateTopic(topic: TopicEntity)

    @Query("DELETE FROM topics WHERE id = :topicId")
    suspend fun deleteTopic(topicId: String)

    @Query("DELETE FROM topics")
    suspend fun clearTopics()

    // Reminders (Unified Persistent Reminder Engine)
    @Query("SELECT * FROM reminders ORDER BY id DESC")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE isCompleted = 0 AND isEnabled = 1")
    suspend fun getActiveRemindersList(): List<ReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminders(reminders: List<ReminderEntity>)

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :reminderId")
    suspend fun deleteReminder(reminderId: String)

    @Query("DELETE FROM reminders WHERE entityType = :entityType AND entityId = :entityId")
    suspend fun deleteReminderByEntity(entityType: String, entityId: String)

    @Query("DELETE FROM reminders WHERE topicId = :topicId")
    suspend fun deleteRemindersByTopic(topicId: String)

    @Query("DELETE FROM reminders")
    suspend fun clearReminders()

    // Habits
    @Query("SELECT * FROM habits")
    fun getAllHabits(): Flow<List<HabitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabits(habits: List<HabitEntity>)

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    // Expenses
    @Query("SELECT * FROM expenses ORDER BY id DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExpenses(expenses: List<ExpenseEntity>): List<Long>

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Query("SELECT * FROM expenses WHERE idempotencyKey = :key LIMIT 1")
    suspend fun getExpenseByIdempotencyKey(key: String): ExpenseEntity?

    @Query("DELETE FROM expenses WHERE id = :expenseId")
    suspend fun deleteExpense(expenseId: String)

    // Debts & Receivables
    @Query("SELECT * FROM debts ORDER BY id DESC")
    fun getAllDebts(): Flow<List<DebtEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: DebtEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebts(debts: List<DebtEntity>)

    @Update
    suspend fun updateDebt(debt: DebtEntity)

    @Query("DELETE FROM debts WHERE id = :debtId")
    suspend fun deleteDebt(debtId: String)

    @Query("DELETE FROM debts")
    suspend fun clearDebts()

    // Health
    @Query("SELECT * FROM health_log LIMIT 1")
    fun getHealthLog(): Flow<HealthLogEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateHealthLog(log: HealthLogEntity)

    // Vault Notes
    @Query("SELECT * FROM vault_notes ORDER BY updatedAt DESC")
    fun getAllVaultNotes(): Flow<List<VaultNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultNote(note: VaultNoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultNotes(notes: List<VaultNoteEntity>)

    @Query("DELETE FROM vault_notes WHERE id = :noteId")
    suspend fun deleteVaultNote(noteId: String)

    // Clear all for Factory Reset
    @Query("DELETE FROM tasks")
    suspend fun clearTasks()

    @Query("DELETE FROM habits")
    suspend fun clearHabits()

    @Query("DELETE FROM expenses")
    suspend fun clearExpenses()

    @Query("DELETE FROM vault_notes")
    suspend fun clearVaultNotes()

    // Bank Configurations
    @Query("SELECT * FROM bank_configs ORDER BY bankNameKey ASC")
    fun getAllBankConfigs(): Flow<List<BankConfigEntity>>

    @Query("SELECT * FROM bank_configs WHERE bankNameKey = :key LIMIT 1")
    suspend fun getBankConfigByKey(key: String): BankConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBankConfig(config: BankConfigEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBankConfigs(configs: List<BankConfigEntity>): List<Long>

    @Update
    suspend fun updateBankConfig(config: BankConfigEntity)

    @Query("DELETE FROM bank_configs WHERE bankNameKey = :key")
    suspend fun deleteBankConfig(key: String)

    @Query("DELETE FROM bank_configs")
    suspend fun clearBankConfigs()

    // SMS Processing History
    @Query("SELECT * FROM sms_processing_history ORDER BY timestampMillis DESC")
    fun getAllProcessingHistory(): Flow<List<SmsProcessingHistoryEntity>>

    @Query("SELECT * FROM sms_processing_history WHERE processingResult = :result ORDER BY timestampMillis DESC")
    fun getProcessingHistoryByResult(result: String): Flow<List<SmsProcessingHistoryEntity>>

    @Query("SELECT * FROM sms_processing_history WHERE processingResult = 'NEEDS_REVIEW' ORDER BY timestampMillis DESC")
    fun getPendingReviewHistory(): Flow<List<SmsProcessingHistoryEntity>>

    @Query("SELECT * FROM sms_processing_history WHERE idempotencyKey = :key LIMIT 1")
    suspend fun getHistoryByIdempotencyKey(key: String): SmsProcessingHistoryEntity?

    @Query("SELECT * FROM sms_processing_history WHERE id = :id LIMIT 1")
    suspend fun getHistoryById(id: String): SmsProcessingHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProcessingHistory(record: SmsProcessingHistoryEntity): Long

    @Update
    suspend fun updateProcessingHistory(record: SmsProcessingHistoryEntity)

    @Query("DELETE FROM sms_processing_history WHERE id = :id")
    suspend fun deleteProcessingHistory(id: String)

    @Query("DELETE FROM sms_processing_history")
    suspend fun clearProcessingHistory()
}
