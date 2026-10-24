package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "cached_tasks")
data class CachedTaskEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val description: String = "",
    val category: String = "عمومی",
    val tagsCsv: String = "",
    val priority: String = "MEDIUM",
    val dueDateEpochDay: Long = -1L,
    val dueTimeMinutes: Int = -1,
    val reminderAtMillis: Long = -1L,
    val estimatedDurationMinutes: Int = 30,
    val status: String = "PENDING",
    val subtasksJson: String = "[]",
    val recurrenceRule: String = "NONE",
    val recurrenceIntervalDays: Int = 0,
    val notes: String = "",
    val inputSource: String = "MANUAL",
    val sortOrder: Int = 0,
    val completedAtMillis: Long = -1L,
    val idempotencyKey: String = "",
    val actualDurationMinutes: Int = 0,
    val syncedWithCloud: Boolean = false,
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    fun toDomain(): Task = Task(
        id = id,
        userId = userId,
        title = title,
        description = description,
        category = category,
        tags = if (tagsCsv.isBlank()) emptyList() else tagsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() },
        priority = priority,
        dueDateEpochDay = dueDateEpochDay,
        dueTimeMinutes = dueTimeMinutes,
        reminderAtMillis = reminderAtMillis,
        estimatedDurationMinutes = estimatedDurationMinutes,
        status = status,
        subtasksJson = subtasksJson,
        recurrenceRule = recurrenceRule,
        recurrenceIntervalDays = recurrenceIntervalDays,
        notes = notes,
        inputSource = inputSource,
        sortOrder = sortOrder,
        completedAtMillis = completedAtMillis,
        idempotencyKey = idempotencyKey,
        actualDurationMinutes = actualDurationMinutes
    )

    companion object {
        fun fromDomain(t: Task, synced: Boolean = false): CachedTaskEntity = CachedTaskEntity(
            id = t.id,
            userId = t.userId,
            title = t.title,
            description = t.description,
            category = t.category,
            tagsCsv = t.tags.joinToString(","),
            priority = t.priority,
            dueDateEpochDay = t.dueDateEpochDay,
            dueTimeMinutes = t.dueTimeMinutes,
            reminderAtMillis = t.reminderAtMillis,
            estimatedDurationMinutes = t.estimatedDurationMinutes,
            status = t.status,
            subtasksJson = t.subtasksJson,
            recurrenceRule = t.recurrenceRule,
            recurrenceIntervalDays = t.recurrenceIntervalDays,
            notes = t.notes,
            inputSource = t.inputSource,
            sortOrder = t.sortOrder,
            completedAtMillis = t.completedAtMillis,
            idempotencyKey = t.idempotencyKey,
            actualDurationMinutes = t.actualDurationMinutes,
            syncedWithCloud = synced,
            updatedAtMillis = System.currentTimeMillis()
        )
    }
}

@Entity(tableName = "cached_consumption")
data class CachedConsumptionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val itemId: String = "",
    val itemName: String,
    val category: String,
    val quantity: Double,
    val unit: String,
    val localEpochDay: Long,
    val timestampMillis: Long,
    val linkedExpenseId: String = "",
    val linkedTaskId: String = "",
    val notes: String = "",
    val inputSource: String = "MANUAL",
    val idempotencyKey: String = "",
    val syncedWithCloud: Boolean = false
) {
    fun toDomain(): ConsumptionEntry = ConsumptionEntry(
        id = id,
        userId = userId,
        itemId = itemId,
        itemName = itemName,
        category = category,
        quantity = quantity,
        unit = unit,
        timestampMillis = timestampMillis,
        localEpochDay = localEpochDay,
        linkedExpenseId = linkedExpenseId,
        linkedTaskId = linkedTaskId,
        notes = notes,
        inputSource = inputSource,
        idempotencyKey = idempotencyKey
    )

    companion object {
        fun fromDomain(c: ConsumptionEntry, synced: Boolean = false): CachedConsumptionEntity = CachedConsumptionEntity(
            id = c.id,
            userId = c.userId,
            itemId = c.itemId,
            itemName = c.itemName,
            category = c.category,
            quantity = c.quantity,
            unit = c.unit,
            localEpochDay = c.localEpochDay,
            timestampMillis = c.timestampMillis,
            linkedExpenseId = c.linkedExpenseId,
            linkedTaskId = c.linkedTaskId,
            notes = c.notes,
            inputSource = c.inputSource,
            idempotencyKey = c.idempotencyKey,
            syncedWithCloud = synced
        )
    }
}

@Entity(tableName = "cached_custom_items")
data class CachedCustomItemEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val category: String,
    val unit: String,
    val defaultQuantity: Double,
    val dailyTarget: Double,
    val defaultPrice: Double,
    val defaultCurrency: String,
    val syncedWithCloud: Boolean = false
) {
    fun toDomain(): CustomItem = CustomItem(
        id = id,
        userId = userId,
        name = name,
        category = category,
        unit = unit,
        defaultQuantity = defaultQuantity,
        dailyTarget = dailyTarget,
        defaultPrice = defaultPrice,
        defaultCurrency = defaultCurrency
    )

    companion object {
        fun fromDomain(item: CustomItem, synced: Boolean = false): CachedCustomItemEntity = CachedCustomItemEntity(
            id = item.id,
            userId = item.userId,
            name = item.name,
            category = item.category,
            unit = item.unit,
            defaultQuantity = item.defaultQuantity,
            dailyTarget = item.dailyTarget,
            defaultPrice = item.defaultPrice,
            defaultCurrency = item.defaultCurrency,
            syncedWithCloud = synced
        )
    }
}

@Entity(tableName = "cached_expenses")
data class CachedExpenseEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val category: String,
    val amount: Double,
    val currency: String,
    val localEpochDay: Long,
    val timestampMillis: Long,
    val linkedConsumptionId: String = "",
    val notes: String = "",
    val inputSource: String = "MANUAL",
    val idempotencyKey: String = "",
    val transactionType: String = "EXPENSE",
    val syncedWithCloud: Boolean = false
) {
    fun toDomain(): ExpenseEntry = ExpenseEntry(
        id = id,
        userId = userId,
        title = title,
        category = category,
        amount = amount,
        currency = currency,
        timestampMillis = timestampMillis,
        localEpochDay = localEpochDay,
        linkedConsumptionId = linkedConsumptionId,
        notes = notes,
        inputSource = inputSource,
        idempotencyKey = idempotencyKey,
        transactionType = transactionType
    )

    companion object {
        fun fromDomain(e: ExpenseEntry, synced: Boolean = false): CachedExpenseEntity = CachedExpenseEntity(
            id = e.id,
            userId = e.userId,
            title = e.title,
            category = e.category,
            amount = e.amount,
            currency = e.currency,
            localEpochDay = e.localEpochDay,
            timestampMillis = e.timestampMillis,
            linkedConsumptionId = e.linkedConsumptionId,
            notes = e.notes,
            inputSource = e.inputSource,
            idempotencyKey = e.idempotencyKey,
            transactionType = if (e.isIncome) "INCOME" else "EXPENSE",
            syncedWithCloud = synced
        )
    }
}

@Entity(tableName = "cached_habits")
data class CachedHabitEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val category: String,
    val scheduleType: String,
    val scheduleDaysCsv: String,
    val targetPerPeriod: Int,
    val linkedTaskCategory: String = "",
    val reminderTimeMinutes: Int = -1,
    val archived: Boolean = false,
    val syncedWithCloud: Boolean = false
) {
    fun toDomain(): Habit = Habit(
        id = id,
        userId = userId,
        title = title,
        category = category,
        scheduleType = scheduleType,
        scheduleDays = if (scheduleDaysCsv.isBlank()) listOf(1, 2, 3, 4, 5, 6, 7)
        else scheduleDaysCsv.split(",").mapNotNull { it.trim().toIntOrNull() },
        targetPerPeriod = targetPerPeriod,
        linkedTaskCategory = linkedTaskCategory,
        reminderTimeMinutes = reminderTimeMinutes,
        archived = archived
    )

    companion object {
        fun fromDomain(h: Habit, synced: Boolean = false): CachedHabitEntity = CachedHabitEntity(
            id = h.id,
            userId = h.userId,
            title = h.title,
            category = h.category,
            scheduleType = h.scheduleType,
            scheduleDaysCsv = h.scheduleDays.joinToString(","),
            targetPerPeriod = h.targetPerPeriod,
            linkedTaskCategory = h.linkedTaskCategory,
            reminderTimeMinutes = h.reminderTimeMinutes,
            archived = h.archived,
            syncedWithCloud = synced
        )
    }
}

@Entity(tableName = "cached_habit_records")
data class CachedHabitRecordEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val habitId: String,
    val localEpochDay: Long,
    val completedCount: Int = 1,
    val notes: String = "",
    val inputSource: String = "MANUAL",
    val syncedWithCloud: Boolean = false
) {
    fun toDomain(): HabitRecord = HabitRecord(
        id = id,
        userId = userId,
        habitId = habitId,
        localEpochDay = localEpochDay,
        completedCount = completedCount,
        notes = notes,
        inputSource = inputSource
    )

    companion object {
        fun fromDomain(r: HabitRecord, synced: Boolean = false): CachedHabitRecordEntity = CachedHabitRecordEntity(
            id = r.id,
            userId = r.userId,
            habitId = r.habitId,
            localEpochDay = r.localEpochDay,
            completedCount = r.completedCount,
            notes = r.notes,
            inputSource = r.inputSource,
            syncedWithCloud = synced
        )
    }
}

@Entity(tableName = "cached_sleep_records")
data class CachedSleepEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val sleepStartMillis: Long,
    val wakeUpMillis: Long,
    val durationMinutes: Int,
    val localEpochDay: Long,
    val qualityRating: Int,
    val notes: String = "",
    val inputSource: String = "MANUAL",
    val idempotencyKey: String = "",
    val syncedWithCloud: Boolean = false
) {
    fun toDomain(): SleepRecord = SleepRecord(
        id = id,
        userId = userId,
        sleepStartMillis = sleepStartMillis,
        wakeUpMillis = wakeUpMillis,
        durationMinutes = durationMinutes,
        localEpochDay = localEpochDay,
        qualityRating = qualityRating,
        notes = notes,
        inputSource = inputSource,
        idempotencyKey = idempotencyKey
    )

    companion object {
        fun fromDomain(s: SleepRecord, synced: Boolean = false): CachedSleepEntity = CachedSleepEntity(
            id = s.id,
            userId = s.userId,
            sleepStartMillis = s.sleepStartMillis,
            wakeUpMillis = s.wakeUpMillis,
            durationMinutes = s.durationMinutes,
            localEpochDay = s.localEpochDay,
            qualityRating = s.qualityRating,
            notes = s.notes,
            inputSource = s.inputSource,
            idempotencyKey = s.idempotencyKey,
            syncedWithCloud = synced
        )
    }
}

@Entity(tableName = "cached_exercise_records")
data class CachedExerciseEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val activityType: String,
    val durationMinutes: Int,
    val distanceMeters: Double,
    val intensity: String,
    val repetitions: Int,
    val timestampMillis: Long,
    val localEpochDay: Long,
    val notes: String = "",
    val inputSource: String = "MANUAL",
    val idempotencyKey: String = "",
    val syncedWithCloud: Boolean = false
) {
    fun toDomain(): ExerciseRecord = ExerciseRecord(
        id = id,
        userId = userId,
        activityType = activityType,
        durationMinutes = durationMinutes,
        distanceMeters = distanceMeters,
        intensity = intensity,
        repetitions = repetitions,
        timestampMillis = timestampMillis,
        localEpochDay = localEpochDay,
        notes = notes,
        inputSource = inputSource,
        idempotencyKey = idempotencyKey
    )

    companion object {
        fun fromDomain(e: ExerciseRecord, synced: Boolean = false): CachedExerciseEntity = CachedExerciseEntity(
            id = e.id,
            userId = e.userId,
            activityType = e.activityType,
            durationMinutes = e.durationMinutes,
            distanceMeters = e.distanceMeters,
            intensity = e.intensity,
            repetitions = e.repetitions,
            timestampMillis = e.timestampMillis,
            localEpochDay = e.localEpochDay,
            notes = e.notes,
            inputSource = e.inputSource,
            idempotencyKey = e.idempotencyKey,
            syncedWithCloud = synced
        )
    }
}

@Entity(tableName = "cached_activity_events")
data class CachedActivityEventEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val timestampMillis: Long,
    val eventType: String,
    val entityType: String,
    val entityId: String,
    val inputSource: String,
    val operationStatus: String,
    val verificationSource: String,
    val correlationId: String,
    val beforeSummary: String,
    val afterSummary: String,
    val humanSummary: String,
    val recognizedTranscript: String,
    val syncedWithCloud: Boolean = false
) {
    fun toDomain(): ActivityEvent = ActivityEvent(
        id = id,
        userId = userId,
        timestampMillis = timestampMillis,
        eventType = eventType,
        entityType = entityType,
        entityId = entityId,
        inputSource = inputSource,
        operationStatus = operationStatus,
        verificationSource = verificationSource,
        correlationId = correlationId,
        beforeSummary = beforeSummary,
        afterSummary = afterSummary,
        humanSummary = humanSummary,
        recognizedTranscript = recognizedTranscript
    )

    companion object {
        fun fromDomain(ev: ActivityEvent, synced: Boolean = false): CachedActivityEventEntity = CachedActivityEventEntity(
            id = ev.id,
            userId = ev.userId,
            timestampMillis = ev.timestampMillis,
            eventType = ev.eventType,
            entityType = ev.entityType,
            entityId = ev.entityId,
            inputSource = ev.inputSource,
            operationStatus = ev.operationStatus,
            verificationSource = ev.verificationSource,
            correlationId = ev.correlationId,
            beforeSummary = ev.beforeSummary,
            afterSummary = ev.afterSummary,
            humanSummary = ev.humanSummary,
            recognizedTranscript = ev.recognizedTranscript,
            syncedWithCloud = synced
        )
    }
}

@Entity(tableName = "cached_conversations")
data class CachedConversationEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val role: String,
    val messageText: String,
    val structuredActionJson: String,
    val timestampMillis: Long,
    val syncedWithCloud: Boolean = false
) {
    fun toDomain(): ConversationTurn = ConversationTurn(
        id = id,
        userId = userId,
        role = role,
        messageText = messageText,
        structuredActionJson = structuredActionJson,
        timestampMillis = timestampMillis
    )

    companion object {
        fun fromDomain(c: ConversationTurn, synced: Boolean = false): CachedConversationEntity = CachedConversationEntity(
            id = c.id,
            userId = c.userId,
            role = c.role,
            messageText = c.messageText,
            structuredActionJson = c.structuredActionJson,
            timestampMillis = c.timestampMillis,
            syncedWithCloud = synced
        )
    }
}

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val queueId: Long = 0L,
    val userId: String,
    val entityType: String,
    val entityId: String,
    val operation: String,
    val payloadJson: String,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val synced: Boolean = false
)

@Dao
interface LifeOsDao {
    // --- Offline Sync FIFO Queue ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueueSyncOperation(item: SyncQueueEntity)

    @Query("SELECT * FROM sync_queue WHERE userId = :userId AND synced = 0 ORDER BY createdAtMillis ASC")
    suspend fun getPendingSyncQueue(userId: String): List<SyncQueueEntity>

    @Query("UPDATE sync_queue SET synced = 1 WHERE queueId = :queueId")
    suspend fun markSyncQueueItemSynced(queueId: Long)

    @Query("DELETE FROM sync_queue WHERE userId = :userId AND synced = 1")
    suspend fun pruneSyncedQueueItems(userId: String)

    // --- Tasks ---
    @Query("SELECT * FROM cached_tasks WHERE userId = :userId ORDER BY CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END ASC, sortOrder ASC, updatedAtMillis DESC")
    fun observeCachedTasks(userId: String): Flow<List<CachedTaskEntity>>

    @Query("SELECT * FROM cached_tasks WHERE userId = :userId")
    suspend fun getTasksForUser(userId: String): List<CachedTaskEntity>

    @Query("SELECT * FROM cached_tasks WHERE userId = :userId AND syncedWithCloud = 0")
    suspend fun getUnsyncedTasks(userId: String): List<CachedTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTasks(tasks: List<CachedTaskEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(task: CachedTaskEntity)

    @Query("DELETE FROM cached_tasks WHERE id = :id AND userId = :userId")
    suspend fun deleteCachedTask(userId: String, id: String)

    // --- Consumption ---
    @Query("SELECT * FROM cached_consumption WHERE userId = :userId ORDER BY timestampMillis DESC")
    fun observeCachedConsumption(userId: String): Flow<List<CachedConsumptionEntity>>

    @Query("SELECT * FROM cached_consumption WHERE userId = :userId AND syncedWithCloud = 0")
    suspend fun getUnsyncedConsumption(userId: String): List<CachedConsumptionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConsumption(entries: List<CachedConsumptionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConsumptionOne(entry: CachedConsumptionEntity)

    @Query("DELETE FROM cached_consumption WHERE id = :id AND userId = :userId")
    suspend fun deleteCachedConsumption(userId: String, id: String)

    // --- Custom Items ---
    @Query("SELECT * FROM cached_custom_items WHERE userId = :userId")
    fun observeCachedCustomItems(userId: String): Flow<List<CachedCustomItemEntity>>

    @Query("SELECT * FROM cached_custom_items WHERE userId = :userId")
    suspend fun getCustomItemsForUser(userId: String): List<CachedCustomItemEntity>

    @Query("SELECT * FROM cached_custom_items WHERE userId = :userId AND syncedWithCloud = 0")
    suspend fun getUnsyncedCustomItems(userId: String): List<CachedCustomItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCustomItem(item: CachedCustomItemEntity)

    @Query("DELETE FROM cached_custom_items WHERE id = :id AND userId = :userId")
    suspend fun deleteCachedCustomItem(userId: String, id: String)

    // --- Expenses ---
    @Query("SELECT * FROM cached_expenses WHERE userId = :userId ORDER BY timestampMillis DESC")
    fun observeCachedExpenses(userId: String): Flow<List<CachedExpenseEntity>>

    @Query("SELECT * FROM cached_expenses WHERE userId = :userId AND syncedWithCloud = 0")
    suspend fun getUnsyncedExpenses(userId: String): List<CachedExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExpenses(expenses: List<CachedExpenseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExpenseOne(expense: CachedExpenseEntity)

    @Query("DELETE FROM cached_expenses WHERE id = :id AND userId = :userId")
    suspend fun deleteCachedExpense(userId: String, id: String)

    // --- Habits & Records ---
    @Query("SELECT * FROM cached_habits WHERE userId = :userId")
    fun observeCachedHabits(userId: String): Flow<List<CachedHabitEntity>>

    @Query("SELECT * FROM cached_habits WHERE userId = :userId AND syncedWithCloud = 0")
    suspend fun getUnsyncedHabits(userId: String): List<CachedHabitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabit(habit: CachedHabitEntity)

    @Query("SELECT * FROM cached_habit_records WHERE userId = :userId")
    fun observeCachedHabitRecords(userId: String): Flow<List<CachedHabitRecordEntity>>

    @Query("SELECT * FROM cached_habit_records WHERE userId = :userId AND syncedWithCloud = 0")
    suspend fun getUnsyncedHabitRecords(userId: String): List<CachedHabitRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabitRecord(record: CachedHabitRecordEntity)

    @Query("DELETE FROM cached_habit_records WHERE id = :id AND userId = :userId")
    suspend fun deleteCachedHabitRecord(userId: String, id: String)

    // --- Sleep & Exercise ---
    @Query("SELECT * FROM cached_sleep_records WHERE userId = :userId ORDER BY wakeUpMillis DESC")
    fun observeCachedSleepRecords(userId: String): Flow<List<CachedSleepEntity>>

    @Query("SELECT * FROM cached_sleep_records WHERE userId = :userId AND syncedWithCloud = 0")
    suspend fun getUnsyncedSleepRecords(userId: String): List<CachedSleepEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSleepRecord(record: CachedSleepEntity)

    @Query("SELECT * FROM cached_exercise_records WHERE userId = :userId ORDER BY timestampMillis DESC")
    fun observeCachedExerciseRecords(userId: String): Flow<List<CachedExerciseEntity>>

    @Query("SELECT * FROM cached_exercise_records WHERE userId = :userId AND syncedWithCloud = 0")
    suspend fun getUnsyncedExerciseRecords(userId: String): List<CachedExerciseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExerciseRecord(record: CachedExerciseEntity)

    // --- Activity Events & Conversations ---
    @Query("SELECT * FROM cached_activity_events WHERE userId = :userId ORDER BY timestampMillis DESC")
    fun observeCachedActivityEvents(userId: String): Flow<List<CachedActivityEventEntity>>

    @Query("SELECT * FROM cached_activity_events WHERE userId = :userId AND syncedWithCloud = 0")
    suspend fun getUnsyncedActivityEvents(userId: String): List<CachedActivityEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertActivityEvent(event: CachedActivityEventEntity)

    @Query("SELECT * FROM cached_conversations WHERE userId = :userId ORDER BY timestampMillis ASC")
    fun observeCachedConversations(userId: String): Flow<List<CachedConversationEntity>>

    @Query("SELECT * FROM cached_conversations WHERE userId = :userId AND syncedWithCloud = 0")
    suspend fun getUnsyncedConversations(userId: String): List<CachedConversationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConversation(turn: CachedConversationEntity)

    @Query("DELETE FROM cached_conversations WHERE userId = :userId")
    suspend fun clearCachedConversations(userId: String)

    // --- Sync Status & Account Migration ---
    @Query("""
        SELECT (
            (SELECT COUNT(*) FROM cached_tasks WHERE userId = :userId AND syncedWithCloud = 0) +
            (SELECT COUNT(*) FROM cached_consumption WHERE userId = :userId AND syncedWithCloud = 0) +
            (SELECT COUNT(*) FROM cached_expenses WHERE userId = :userId AND syncedWithCloud = 0) +
            (SELECT COUNT(*) FROM cached_habits WHERE userId = :userId AND syncedWithCloud = 0) +
            (SELECT COUNT(*) FROM cached_habit_records WHERE userId = :userId AND syncedWithCloud = 0) +
            (SELECT COUNT(*) FROM cached_sleep_records WHERE userId = :userId AND syncedWithCloud = 0) +
            (SELECT COUNT(*) FROM cached_exercise_records WHERE userId = :userId AND syncedWithCloud = 0) +
            (SELECT COUNT(*) FROM cached_activity_events WHERE userId = :userId AND syncedWithCloud = 0)
        )
    """)
    fun observePendingSyncCount(userId: String): Flow<Int>

    @Query("UPDATE cached_tasks SET userId = :newUid, syncedWithCloud = 0 WHERE userId = 'local_device_user'")
    suspend fun migrateLocalTasksToUser(newUid: String)

    @Query("UPDATE cached_consumption SET userId = :newUid, syncedWithCloud = 0 WHERE userId = 'local_device_user'")
    suspend fun migrateLocalConsumptionToUser(newUid: String)

    @Query("UPDATE cached_custom_items SET userId = :newUid, syncedWithCloud = 0 WHERE userId = 'local_device_user'")
    suspend fun migrateLocalCustomItemsToUser(newUid: String)

    @Query("UPDATE cached_expenses SET userId = :newUid, syncedWithCloud = 0 WHERE userId = 'local_device_user'")
    suspend fun migrateLocalExpensesToUser(newUid: String)

    @Query("UPDATE cached_habits SET userId = :newUid, syncedWithCloud = 0 WHERE userId = 'local_device_user'")
    suspend fun migrateLocalHabitsToUser(newUid: String)

    @Query("UPDATE cached_habit_records SET userId = :newUid, syncedWithCloud = 0 WHERE userId = 'local_device_user'")
    suspend fun migrateLocalHabitRecordsToUser(newUid: String)

    @Query("UPDATE cached_sleep_records SET userId = :newUid, syncedWithCloud = 0 WHERE userId = 'local_device_user'")
    suspend fun migrateLocalSleepToUser(newUid: String)

    @Query("UPDATE cached_exercise_records SET userId = :newUid, syncedWithCloud = 0 WHERE userId = 'local_device_user'")
    suspend fun migrateLocalExerciseToUser(newUid: String)

    @Query("UPDATE cached_activity_events SET userId = :newUid, syncedWithCloud = 0 WHERE userId = 'local_device_user'")
    suspend fun migrateLocalEventsToUser(newUid: String)

    @Query("UPDATE cached_conversations SET userId = :newUid, syncedWithCloud = 0 WHERE userId = 'local_device_user'")
    suspend fun migrateLocalConversationsToUser(newUid: String)

    // --- Full Clear ---
    @Query("DELETE FROM cached_tasks WHERE userId = :userId")
    suspend fun clearTasksForUser(userId: String)

    @Query("DELETE FROM cached_consumption WHERE userId = :userId")
    suspend fun clearConsumptionForUser(userId: String)

    @Query("DELETE FROM cached_custom_items WHERE userId = :userId")
    suspend fun clearCustomItemsForUser(userId: String)

    @Query("DELETE FROM cached_expenses WHERE userId = :userId")
    suspend fun clearExpensesForUser(userId: String)

    @Query("DELETE FROM cached_habits WHERE userId = :userId")
    suspend fun clearHabitsForUser(userId: String)

    @Query("DELETE FROM cached_habit_records WHERE userId = :userId")
    suspend fun clearHabitRecordsForUser(userId: String)

    @Query("DELETE FROM cached_sleep_records WHERE userId = :userId")
    suspend fun clearSleepForUser(userId: String)

    @Query("DELETE FROM cached_exercise_records WHERE userId = :userId")
    suspend fun clearExerciseForUser(userId: String)
}

@Database(
    entities = [
        CachedTaskEntity::class,
        CachedConsumptionEntity::class,
        CachedCustomItemEntity::class,
        CachedExpenseEntity::class,
        CachedHabitEntity::class,
        CachedHabitRecordEntity::class,
        CachedSleepEntity::class,
        CachedExerciseEntity::class,
        CachedActivityEventEntity::class,
        CachedConversationEntity::class,
        SyncQueueEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class LifeOsDatabase : RoomDatabase() {
    abstract fun lifeOsDao(): LifeOsDao

    companion object {
        const val LOCAL_DEVICE_USER_ID = "local_device_user"

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE cached_expenses ADD COLUMN transactionType TEXT NOT NULL DEFAULT 'EXPENSE'")
                } catch (_: Exception) {
                }
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE cached_tasks ADD COLUMN actualDurationMinutes INTEGER NOT NULL DEFAULT 0")
                } catch (_: Exception) {
                }
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sync_queue (
                        queueId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId TEXT NOT NULL,
                        entityType TEXT NOT NULL,
                        entityId TEXT NOT NULL,
                        operation TEXT NOT NULL,
                        payloadJson TEXT NOT NULL,
                        createdAtMillis INTEGER NOT NULL,
                        retryCount INTEGER NOT NULL,
                        synced INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        @Volatile
        private var INSTANCE: LifeOsDatabase? = null

        fun getInstance(context: Context): LifeOsDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LifeOsDatabase::class.java,
                    "personal_life_os_cache.db"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
