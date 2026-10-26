package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.local.CachedActivityEventEntity
import com.example.data.local.CachedConsumptionEntity
import com.example.data.local.CachedConversationEntity
import com.example.data.local.CachedCustomItemEntity
import com.example.data.local.CachedExerciseEntity
import com.example.data.local.CachedExpenseEntity
import com.example.data.local.CachedHabitEntity
import com.example.data.local.CachedHabitRecordEntity
import com.example.data.local.CachedSleepEntity
import com.example.data.local.CachedTaskEntity
import com.example.data.local.LifeOsDao
import com.example.data.local.LifeOsDatabase
import com.example.data.local.SyncQueueEntity
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
import com.example.notifications.ReminderScheduler
import com.example.util.PersianDateAndUnitUtils
import com.example.util.WorldClassOsUtils
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class LifeOsRepository(
    private val cloudDb: FirebaseFirestore?,
    private val localDao: LifeOsDao? = null
) {
    constructor(context: Context) : this(
        resolveFirestoreSafely(context.applicationContext),
        LifeOsDatabase.getInstance(context.applicationContext).lifeOsDao()
    )

    private val db: FirebaseFirestore
        get() = cloudDb
            ?: throw IllegalStateException("Cloud Firestore is not available on this device.")

    private val auth
        get() = try {
            Firebase.auth
        } catch (_: Throwable) {
            null
        }

    private val recentIdempotencyKeys = ConcurrentHashMap<String, String>()

    /**
     * When running in direct cloud mode (localDao == null, e.g., Firestore Emulator Rule Tests),
     * strictly requires a signed-in Firebase user.
     */
    fun requireUserId(): String {
        return auth?.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    /**
     * Resolves the active user ID for on-device Local-First operation:
     * returns the authenticated Google UID if signed in, or LOCAL_DEVICE_USER_ID ("local_device_user")
     * when working without an account on the phone.
     */
    fun resolveActiveUserId(): String {
        return if (localDao != null) {
            auth?.currentUser?.uid ?: LifeOsDatabase.LOCAL_DEVICE_USER_ID
        } else {
            requireUserId()
        }
    }

    companion object {
        private fun resolveFirestoreSafely(appContext: Context): FirebaseFirestore? {
            return try {
                com.example.data.firebase.ensureFirebaseInitialized(appContext)
                val dbId = try {
                    appContext.getString(R.string.firestore_database_id)
                } catch (_: Throwable) {
                    "ai-studio-android-personal-a911cedb-55ed-4d73-8197-39f8e08a22de"
                }
                if (dbId.isNotBlank()) {
                    FirebaseFirestore.getInstance(dbId)
                } else {
                    FirebaseFirestore.getInstance()
                }
            } catch (_: Throwable) {
                null
            }
        }
    }

    private fun cleanDocId(prefix: String = "doc"): String {
        return "${prefix}_${UUID.randomUUID().toString().replace("-", "")}"
    }

    fun observePendingSyncCount(userId: String = resolveActiveUserId()): Flow<Int> {
        return localDao?.observePendingSyncCount(userId) ?: flowOf(0)
    }

    suspend fun migrateLocalDataToAuthenticatedUser(newUid: String) {
        val dao = localDao ?: return
        if (newUid.isBlank() || newUid == LifeOsDatabase.LOCAL_DEVICE_USER_ID) return
        dao.migrateLocalTasksToUser(newUid)
        dao.migrateLocalConsumptionToUser(newUid)
        dao.migrateLocalCustomItemsToUser(newUid)
        dao.migrateLocalExpensesToUser(newUid)
        dao.migrateLocalHabitsToUser(newUid)
        dao.migrateLocalHabitRecordsToUser(newUid)
        dao.migrateLocalSleepToUser(newUid)
        dao.migrateLocalExerciseToUser(newUid)
        dao.migrateLocalEventsToUser(newUid)
        dao.migrateLocalConversationsToUser(newUid)
    }

    // --- Real-Time Observation Flows (Local-First on Device via Room, or Cloud-Direct when localDao == null) ---

    fun observeTasks(userId: String = resolveActiveUserId()): Flow<List<Task>> {
        if (localDao != null) {
            return localDao.observeCachedTasks(userId).map { list -> list.map { it.toDomain() } }
        }
        val path = "users/$userId/tasks"
        return flow {
            emitAll(
                db.collection("users").document(userId).collection("tasks")
                    .whereEqualTo("userId", userId)
                    .snapshots()
                    .map { snapshot ->
                        snapshot.toObjects(Task::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                            .sortedWith(
                                compareBy<Task> { it.status == "COMPLETED" }
                                    .thenBy { it.sortOrder }
                                    .thenByDescending { it.createdAt?.seconds ?: 0L }
                            )
                    }
                    .catch { error ->
                        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                        throw error
                    }
            )
        }
    }

    fun observeConsumptionEntries(userId: String = resolveActiveUserId()): Flow<List<ConsumptionEntry>> {
        if (localDao != null) {
            return localDao.observeCachedConsumption(userId).map { list -> list.map { it.toDomain() } }
        }
        val path = "users/$userId/consumptionEntries"
        return flow {
            emitAll(
                db.collection("users").document(userId).collection("consumptionEntries")
                    .whereEqualTo("userId", userId)
                    .snapshots()
                    .map { snapshot ->
                        snapshot.toObjects(ConsumptionEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                            .sortedByDescending { it.timestampMillis }
                    }
                    .catch { error ->
                        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                        throw error
                    }
            )
        }
    }

    fun observeCustomItems(userId: String = resolveActiveUserId()): Flow<List<CustomItem>> {
        if (localDao != null) {
            return localDao.observeCachedCustomItems(userId).map { list -> list.map { it.toDomain() } }
        }
        val path = "users/$userId/customItems"
        return flow {
            emitAll(
                db.collection("users").document(userId).collection("customItems")
                    .whereEqualTo("userId", userId)
                    .snapshots()
                    .map { snapshot ->
                        snapshot.toObjects(CustomItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }
                    .catch { error ->
                        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                        throw error
                    }
            )
        }
    }

    fun observeExpenses(userId: String = resolveActiveUserId()): Flow<List<ExpenseEntry>> {
        if (localDao != null) {
            return localDao.observeCachedExpenses(userId).map { list -> list.map { it.toDomain() } }
        }
        val path = "users/$userId/expenses"
        return flow {
            emitAll(
                db.collection("users").document(userId).collection("expenses")
                    .whereEqualTo("userId", userId)
                    .snapshots()
                    .map { snapshot ->
                        snapshot.toObjects(ExpenseEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                            .sortedByDescending { it.timestampMillis }
                    }
                    .catch { error ->
                        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                        throw error
                    }
            )
        }
    }

    fun observeHabits(userId: String = resolveActiveUserId()): Flow<List<Habit>> {
        if (localDao != null) {
            return localDao.observeCachedHabits(userId).map { list -> list.map { it.toDomain() } }
        }
        val path = "users/$userId/habits"
        return flow {
            emitAll(
                db.collection("users").document(userId).collection("habits")
                    .whereEqualTo("userId", userId)
                    .snapshots()
                    .map { snapshot ->
                        snapshot.toObjects(Habit::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }
                    .catch { error ->
                        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                        throw error
                    }
            )
        }
    }

    fun observeHabitRecords(userId: String = resolveActiveUserId()): Flow<List<HabitRecord>> {
        if (localDao != null) {
            return localDao.observeCachedHabitRecords(userId).map { list -> list.map { it.toDomain() } }
        }
        val path = "users/$userId/habitRecords"
        return flow {
            emitAll(
                db.collection("users").document(userId).collection("habitRecords")
                    .whereEqualTo("userId", userId)
                    .snapshots()
                    .map { snapshot ->
                        snapshot.toObjects(HabitRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }
                    .catch { error ->
                        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                        throw error
                    }
            )
        }
    }

    fun observeSleepRecords(userId: String = resolveActiveUserId()): Flow<List<SleepRecord>> {
        if (localDao != null) {
            return localDao.observeCachedSleepRecords(userId).map { list -> list.map { it.toDomain() } }
        }
        val path = "users/$userId/sleepRecords"
        return flow {
            emitAll(
                db.collection("users").document(userId).collection("sleepRecords")
                    .whereEqualTo("userId", userId)
                    .snapshots()
                    .map { snapshot ->
                        snapshot.toObjects(SleepRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                            .sortedByDescending { it.wakeUpMillis }
                    }
                    .catch { error ->
                        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                        throw error
                    }
            )
        }
    }

    fun observeExerciseRecords(userId: String = resolveActiveUserId()): Flow<List<ExerciseRecord>> {
        if (localDao != null) {
            return localDao.observeCachedExerciseRecords(userId).map { list -> list.map { it.toDomain() } }
        }
        val path = "users/$userId/exerciseRecords"
        return flow {
            emitAll(
                db.collection("users").document(userId).collection("exerciseRecords")
                    .whereEqualTo("userId", userId)
                    .snapshots()
                    .map { snapshot ->
                        snapshot.toObjects(ExerciseRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                            .sortedByDescending { it.timestampMillis }
                    }
                    .catch { error ->
                        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                        throw error
                    }
            )
        }
    }

    fun observeActivityEvents(userId: String = resolveActiveUserId()): Flow<List<ActivityEvent>> {
        if (localDao != null) {
            return localDao.observeCachedActivityEvents(userId).map { list -> list.map { it.toDomain() } }
        }
        val path = "users/$userId/activityEvents"
        return flow {
            emitAll(
                db.collection("users").document(userId).collection("activityEvents")
                    .whereEqualTo("userId", userId)
                    .snapshots()
                    .map { snapshot ->
                        snapshot.toObjects(ActivityEvent::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                            .sortedByDescending { it.timestampMillis }
                    }
                    .catch { error ->
                        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                        throw error
                    }
            )
        }
    }

    fun observeConversations(userId: String = resolveActiveUserId()): Flow<List<ConversationTurn>> {
        if (localDao != null) {
            return localDao.observeCachedConversations(userId).map { list -> list.map { it.toDomain() } }
        }
        val path = "users/$userId/conversations"
        return flow {
            emitAll(
                db.collection("users").document(userId).collection("conversations")
                    .whereEqualTo("userId", userId)
                    .snapshots()
                    .map { snapshot ->
                        snapshot.toObjects(ConversationTurn::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                            .sortedBy { it.timestampMillis }
                    }
                    .catch { error ->
                        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                        throw error
                    }
            )
        }
    }

    // --- Append-Only Audit Event Logger ---

    suspend fun logActivityEvent(
        eventType: String,
        entityType: String,
        entityId: String,
        inputSource: String,
        operationStatus: String = "EXECUTED",
        correlationId: String = cleanDocId("corr"),
        beforeSummary: String = "",
        afterSummary: String = "",
        humanSummary: String,
        recognizedTranscript: String = ""
    ): Result<String> {
        val uid = resolveActiveUserId()
        val eventId = cleanDocId("evt")
        val event = ActivityEvent(
            id = eventId,
            userId = uid,
            timestampMillis = System.currentTimeMillis(),
            eventType = eventType.take(100),
            entityType = entityType.take(100),
            entityId = entityId.take(128),
            inputSource = inputSource.take(50),
            operationStatus = operationStatus,
            verificationSource = "CLIENT_RECORDED",
            correlationId = correlationId.take(128),
            beforeSummary = beforeSummary.take(2000),
            afterSummary = afterSummary.take(2000),
            humanSummary = humanSummary.ifBlank { eventType }.take(1000),
            recognizedTranscript = recognizedTranscript.take(2000)
        )

        if (localDao != null) {
            localDao.upsertActivityEvent(CachedActivityEventEntity.fromDomain(event, synced = false))
            return Result.success(eventId)
        }

        val docRef = db.collection("users").document(uid).collection("activityEvents").document(eventId)
        return try {
            docRef.set(event.toCreateMap()).await()
            Result.success(eventId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Task CRUD + Recurrence + Idempotency ---

    suspend fun createTask(
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
        inputSource: String = "MANUAL",
        idempotencyKey: String = "",
        linkedDebtId: String = ""
    ): Result<String> {
        val uid = resolveActiveUserId()
        if (idempotencyKey.isNotBlank()) {
            recentIdempotencyKeys["$uid:task:$idempotencyKey"]?.let { existingId ->
                return Result.success(existingId)
            }
        }
        val taskId = cleanDocId("tsk")
        val task = Task(
            id = taskId,
            userId = uid,
            title = title.trim().take(300).ifBlank { "تسک جدید" },
            description = description.take(2000),
            category = category.trim().take(100).ifBlank { "عمومی" },
            tags = tags.take(20),
            priority = priority,
            dueDateEpochDay = dueDateEpochDay,
            dueTimeMinutes = dueTimeMinutes,
            reminderAtMillis = reminderAtMillis,
            estimatedDurationMinutes = estimatedDurationMinutes.coerceIn(0, 10080),
            status = "PENDING",
            subtasksJson = subtasksJson.take(5000),
            recurrenceRule = recurrenceRule,
            recurrenceIntervalDays = recurrenceIntervalDays.coerceIn(0, 366),
            notes = notes.take(5000),
            inputSource = inputSource,
            idempotencyKey = idempotencyKey.take(128),
            linkedDebtId = linkedDebtId.take(128)
        )

        if (reminderAtMillis > System.currentTimeMillis()) {
            ReminderScheduler.scheduleEntityReminder(
                context = null,
                entityType = "TASK",
                entityId = taskId,
                title = "یادآور تسک: ${task.title}",
                body = task.description.ifBlank { "دسته: ${task.category}" },
                triggerAtMillis = reminderAtMillis
            )
        }

        if (localDao != null) {
            localDao.upsertTask(CachedTaskEntity.fromDomain(task, synced = false))
            localDao.enqueueSyncOperation(
                SyncQueueEntity(
                    userId = uid,
                    operation = "INSERT",
                    entityType = "TASK",
                    entityId = taskId,
                    payloadJson = JSONObject().put("title", task.title).put("priority", task.priority).toString()
                )
            )
            if (idempotencyKey.isNotBlank()) {
                recentIdempotencyKeys["$uid:task:$idempotencyKey"] = taskId
            }
            logActivityEvent(
                eventType = "TASK_CREATED",
                entityType = "TASK",
                entityId = taskId,
                inputSource = inputSource,
                afterSummary = task.title,
                humanSummary = "ایجاد تسک (روی گوشی): «${task.title}»"
            )
            return Result.success(taskId)
        }

        val docRef = db.collection("users").document(uid).collection("tasks").document(taskId)
        return try {
            docRef.set(task.toCreateMap()).await()
            if (idempotencyKey.isNotBlank()) {
                recentIdempotencyKeys["$uid:task:$idempotencyKey"] = taskId
            }
            logActivityEvent(
                eventType = "TASK_CREATED",
                entityType = "TASK",
                entityId = taskId,
                inputSource = inputSource,
                afterSummary = task.title,
                humanSummary = "ایجاد تسک: «${task.title}»"
            )
            Result.success(taskId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getUserTasks(userId: String = resolveActiveUserId()): Result<List<Task>> {
        if (localDao != null) {
            return Result.success(localDao.getTasksForUser(userId).map { it.toDomain() })
        }
        val path = "users/$userId/tasks"
        return try {
            val snap = db.collection("users").document(userId).collection("tasks")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            Result.success(snap.toObjects(Task::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun getTaskById(targetUserId: String, taskId: String): Result<Task?> {
        val docRef = db.collection("users").document(targetUserId).collection("tasks").document(taskId)
        return try {
            val snap = docRef.get().await()
            Result.success(snap.toObject(Task::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    /**
     * Completes a task. If the task is recurring, preserves the completed task's history
     * and creates the next occurrence on the next scheduled date without erasing history.
     */
    suspend fun completeTask(task: Task, inputSource: String = "MANUAL"): Result<String?> {
        val uid = resolveActiveUserId()
        require(task.id.isNotBlank()) { "Task ID must not be blank" }
        val nowMillis = System.currentTimeMillis()
        ReminderScheduler.cancelEntityReminder(null, "TASK", task.id)

        if (localDao != null) {
            val completedTask = task.copy(userId = uid, status = "COMPLETED", completedAtMillis = nowMillis)
            localDao.upsertTask(CachedTaskEntity.fromDomain(completedTask, synced = false))
            logActivityEvent(
                eventType = "TASK_COMPLETED",
                entityType = "TASK",
                entityId = task.id,
                inputSource = inputSource,
                beforeSummary = "status=${task.status}",
                afterSummary = "status=COMPLETED",
                humanSummary = "تکمیل تسک: «${task.title}»"
            )
            var nextTaskId: String? = null
            if (task.recurrenceRule != "NONE") {
                val nextEpochDay = PersianDateAndUnitUtils.calculateNextOccurrenceEpochDay(
                    currentDueEpochDay = task.dueDateEpochDay,
                    recurrenceRule = task.recurrenceRule,
                    recurrenceIntervalDays = task.recurrenceIntervalDays
                )
                if (nextEpochDay != null) {
                    nextTaskId = createTask(
                        title = task.title,
                        description = task.description,
                        category = task.category,
                        tags = task.tags,
                        priority = task.priority,
                        dueDateEpochDay = nextEpochDay,
                        dueTimeMinutes = task.dueTimeMinutes,
                        reminderAtMillis = if (task.reminderAtMillis > 0) task.reminderAtMillis + (nextEpochDay - task.dueDateEpochDay) * 86_400_000L else -1L,
                        estimatedDurationMinutes = task.estimatedDurationMinutes,
                        subtasksJson = task.subtasksJson,
                        recurrenceRule = task.recurrenceRule,
                        recurrenceIntervalDays = task.recurrenceIntervalDays,
                        notes = task.notes,
                        inputSource = "RECURRING"
                    ).getOrNull()
                }
            }
            return Result.success(nextTaskId)
        }

        val docRef = db.collection("users").document(uid).collection("tasks").document(task.id)
        return try {
            docRef.update(
                mapOf(
                    "status" to "COMPLETED",
                    "completedAtMillis" to nowMillis,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            logActivityEvent(
                eventType = "TASK_COMPLETED",
                entityType = "TASK",
                entityId = task.id,
                inputSource = inputSource,
                beforeSummary = "status=${task.status}",
                afterSummary = "status=COMPLETED",
                humanSummary = "تکمیل تسک: «${task.title}»"
            )

            var nextTaskId: String? = null
            if (task.recurrenceRule != "NONE") {
                val nextEpochDay = PersianDateAndUnitUtils.calculateNextOccurrenceEpochDay(
                    currentDueEpochDay = task.dueDateEpochDay,
                    recurrenceRule = task.recurrenceRule,
                    recurrenceIntervalDays = task.recurrenceIntervalDays
                )
                if (nextEpochDay != null) {
                    nextTaskId = createTask(
                        title = task.title,
                        description = task.description,
                        category = task.category,
                        tags = task.tags,
                        priority = task.priority,
                        dueDateEpochDay = nextEpochDay,
                        dueTimeMinutes = task.dueTimeMinutes,
                        reminderAtMillis = if (task.reminderAtMillis > 0) task.reminderAtMillis + (nextEpochDay - task.dueDateEpochDay) * 86_400_000L else -1L,
                        estimatedDurationMinutes = task.estimatedDurationMinutes,
                        subtasksJson = task.subtasksJson,
                        recurrenceRule = task.recurrenceRule,
                        recurrenceIntervalDays = task.recurrenceIntervalDays,
                        notes = task.notes,
                        inputSource = "RECURRING"
                    ).getOrNull()
                }
            }
            Result.success(nextTaskId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateTask(task: Task, inputSource: String = "MANUAL"): Result<Unit> {
        val uid = resolveActiveUserId()
        require(task.id.isNotBlank()) { "Task ID must not be blank" }
        if (task.status == "COMPLETED" || task.status == "ARCHIVED") {
            ReminderScheduler.cancelEntityReminder(null, "TASK", task.id)
        } else if (task.reminderAtMillis > System.currentTimeMillis()) {
            ReminderScheduler.rescheduleEntityReminder(
                context = null,
                entityType = "TASK",
                entityId = task.id,
                title = "یادآور تسک: ${task.title}",
                body = task.description.ifBlank { "دسته: ${task.category}" },
                newTriggerAtMillis = task.reminderAtMillis
            )
        }

        if (localDao != null) {
            localDao.upsertTask(CachedTaskEntity.fromDomain(task.copy(userId = uid), synced = false))
            logActivityEvent(
                eventType = "TASK_UPDATED",
                entityType = "TASK",
                entityId = task.id,
                inputSource = inputSource,
                afterSummary = "${task.title} (${task.status})",
                humanSummary = "به‌روزرسانی تسک: «${task.title}»"
            )
            return Result.success(Unit)
        }

        val docRef = db.collection("users").document(uid).collection("tasks").document(task.id)
        return try {
            docRef.update(
                mapOf(
                    "title" to task.title.take(300),
                    "description" to task.description.take(2000),
                    "category" to task.category.take(100),
                    "tags" to task.tags.take(20),
                    "priority" to task.priority,
                    "dueDateEpochDay" to task.dueDateEpochDay,
                    "dueTimeMinutes" to task.dueTimeMinutes,
                    "reminderAtMillis" to task.reminderAtMillis,
                    "estimatedDurationMinutes" to task.estimatedDurationMinutes.coerceIn(0, 10080),
                    "status" to task.status,
                    "subtasksJson" to task.subtasksJson.take(5000),
                    "recurrenceRule" to task.recurrenceRule,
                    "recurrenceIntervalDays" to task.recurrenceIntervalDays.coerceIn(0, 366),
                    "notes" to task.notes.take(5000),
                    "sortOrder" to task.sortOrder,
                    "completedAtMillis" to task.completedAtMillis,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            logActivityEvent(
                eventType = "TASK_UPDATED",
                entityType = "TASK",
                entityId = task.id,
                inputSource = inputSource,
                afterSummary = "${task.title} (${task.status})",
                humanSummary = "به‌روزرسانی تسک: «${task.title}»"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun archiveOrRestoreTask(task: Task, archive: Boolean, inputSource: String = "MANUAL"): Result<Unit> {
        val newStatus = if (archive) "ARCHIVED" else "PENDING"
        return updateTask(task.copy(status = newStatus), inputSource)
    }

    suspend fun deleteTask(task: Task, inputSource: String = "MANUAL"): Result<Unit> {
        val uid = resolveActiveUserId()
        ReminderScheduler.cancelEntityReminder(null, "TASK", task.id)
        if (localDao != null) {
            localDao.deleteCachedTask(uid, task.id)
            localDao.enqueueSyncOperation(
                SyncQueueEntity(
                    userId = uid,
                    operation = "DELETE",
                    entityType = "TASK",
                    entityId = task.id,
                    payloadJson = JSONObject().put("title", task.title).toString()
                )
            )
            logActivityEvent(
                eventType = "TASK_DELETED",
                entityType = "TASK",
                entityId = task.id,
                inputSource = inputSource,
                beforeSummary = task.title,
                humanSummary = "حذف تسک: «${task.title}»"
            )
            return Result.success(Unit)
        }

        val docRef = db.collection("users").document(uid).collection("tasks").document(task.id)
        return try {
            docRef.delete().await()
            logActivityEvent(
                eventType = "TASK_DELETED",
                entityType = "TASK",
                entityId = task.id,
                inputSource = inputSource,
                beforeSummary = task.title,
                humanSummary = "حذف تسک: «${task.title}»"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Consumption & Custom Items CRUD ---

    suspend fun addConsumptionEntry(
        itemName: String,
        category: String,
        quantity: Double,
        unit: String,
        localEpochDay: Long = PersianDateAndUnitUtils.currentLocalEpochDay(),
        notes: String = "",
        inputSource: String = "MANUAL",
        linkedExpenseId: String = "",
        linkedTaskId: String = "",
        idempotencyKey: String = ""
    ): Result<String> {
        val uid = resolveActiveUserId()
        if (idempotencyKey.isNotBlank()) {
            recentIdempotencyKeys["$uid:cons:$idempotencyKey"]?.let { return Result.success(it) }
        }
        val entryId = cleanDocId("cns")
        val entry = ConsumptionEntry(
            id = entryId,
            userId = uid,
            itemName = itemName.trim().take(200).ifBlank { category },
            category = category,
            quantity = quantity.coerceIn(0.01, 1000000.0),
            unit = PersianDateAndUnitUtils.normalizeUnit(unit).take(50),
            timestampMillis = System.currentTimeMillis(),
            localEpochDay = localEpochDay,
            linkedExpenseId = linkedExpenseId.take(128),
            linkedTaskId = linkedTaskId.take(128),
            notes = notes.take(2000),
            inputSource = inputSource,
            idempotencyKey = idempotencyKey.take(128)
        )

        if (localDao != null) {
            localDao.upsertConsumptionOne(CachedConsumptionEntity.fromDomain(entry, synced = false))
            if (idempotencyKey.isNotBlank()) {
                recentIdempotencyKeys["$uid:cons:$idempotencyKey"] = entryId
            }
            logActivityEvent(
                eventType = "CONSUMPTION_LOGGED",
                entityType = "CONSUMPTION",
                entityId = entryId,
                inputSource = inputSource,
                afterSummary = "${entry.quantity} ${entry.unit} ${entry.itemName}",
                humanSummary = "ثبت مصرف: ${entry.quantity} ${entry.unit} ${entry.itemName}"
            )
            return Result.success(entryId)
        }

        val docRef = db.collection("users").document(uid).collection("consumptionEntries").document(entryId)
        return try {
            docRef.set(entry.toCreateMap()).await()
            if (idempotencyKey.isNotBlank()) {
                recentIdempotencyKeys["$uid:cons:$idempotencyKey"] = entryId
            }
            logActivityEvent(
                eventType = "CONSUMPTION_LOGGED",
                entityType = "CONSUMPTION",
                entityId = entryId,
                inputSource = inputSource,
                afterSummary = "${entry.quantity} ${entry.unit} ${entry.itemName}",
                humanSummary = "ثبت مصرف: ${entry.quantity} ${entry.unit} ${entry.itemName}"
            )
            Result.success(entryId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteConsumptionEntry(entry: ConsumptionEntry): Result<Unit> {
        val uid = resolveActiveUserId()
        if (localDao != null) {
            localDao.deleteCachedConsumption(uid, entry.id)
            localDao.enqueueSyncOperation(
                SyncQueueEntity(
                    userId = uid,
                    operation = "DELETE",
                    entityType = "CONSUMPTION",
                    entityId = entry.id,
                    payloadJson = JSONObject().put("itemName", entry.itemName).toString()
                )
            )
            logActivityEvent(
                eventType = "CONSUMPTION_DELETED",
                entityType = "CONSUMPTION",
                entityId = entry.id,
                inputSource = "MANUAL",
                beforeSummary = "${entry.quantity} ${entry.unit} ${entry.itemName}",
                humanSummary = "حذف رکورد مصرف: ${entry.itemName}"
            )
            return Result.success(Unit)
        }

        val docRef = db.collection("users").document(uid).collection("consumptionEntries").document(entry.id)
        return try {
            docRef.delete().await()
            logActivityEvent(
                eventType = "CONSUMPTION_DELETED",
                entityType = "CONSUMPTION",
                entityId = entry.id,
                inputSource = "MANUAL",
                beforeSummary = "${entry.quantity} ${entry.unit} ${entry.itemName}",
                humanSummary = "حذف رکورد مصرف: ${entry.itemName}"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun ensureDefaultQuickPresets(defaultCurrency: String = "TOMAN") {
        val dao = localDao ?: return
        val uid = resolveActiveUserId()
        val existing = dao.getCustomItemsForUser(uid)
        if (existing.isEmpty()) {
            val defaults = listOf(
                CustomItem(
                    id = "preset_double_espresso",
                    userId = uid,
                    name = "دبل (اسپرسو)",
                    category = "COFFEE",
                    unit = "cups",
                    defaultQuantity = 1.0,
                    dailyTarget = 2.0,
                    defaultPrice = 90000.0,
                    defaultCurrency = defaultCurrency
                ),
                CustomItem(
                    id = "preset_cigarette",
                    userId = uid,
                    name = "سیگار",
                    category = "CUSTOM",
                    unit = "pieces",
                    defaultQuantity = 1.0,
                    dailyTarget = 0.0,
                    defaultPrice = 150000.0,
                    defaultCurrency = defaultCurrency
                ),
                CustomItem(
                    id = "preset_cafe_latte",
                    userId = uid,
                    name = "کافه لاته",
                    category = "COFFEE",
                    unit = "cups",
                    defaultQuantity = 1.0,
                    dailyTarget = 1.0,
                    defaultPrice = 110000.0,
                    defaultCurrency = defaultCurrency
                ),
                CustomItem(
                    id = "preset_mineral_water",
                    userId = uid,
                    name = "آب معدنی",
                    category = "WATER",
                    unit = "pieces",
                    defaultQuantity = 1.0,
                    dailyTarget = 2.0,
                    defaultPrice = 15000.0,
                    defaultCurrency = defaultCurrency
                )
            )
            defaults.forEach { preset ->
                dao.upsertCustomItem(CachedCustomItemEntity.fromDomain(preset, synced = false))
            }
        }
    }

    suspend fun addCustomItem(
        name: String,
        category: String,
        unit: String,
        defaultQuantity: Double,
        dailyTarget: Double = 0.0,
        defaultPrice: Double = 0.0,
        defaultCurrency: String = "TOMAN"
    ): Result<String> {
        val uid = resolveActiveUserId()
        val itemId = cleanDocId("itm")
        val item = CustomItem(
            id = itemId,
            userId = uid,
            name = name.trim().take(150).ifBlank { "آیتم جدید" },
            category = category.take(80),
            unit = PersianDateAndUnitUtils.normalizeUnit(unit).take(50),
            defaultQuantity = defaultQuantity.coerceAtLeast(0.1),
            dailyTarget = dailyTarget.coerceAtLeast(0.0),
            defaultPrice = defaultPrice.coerceAtLeast(0.0),
            defaultCurrency = defaultCurrency.take(16)
        )

        if (localDao != null) {
            localDao.upsertCustomItem(CachedCustomItemEntity.fromDomain(item, synced = false))
            logActivityEvent(
                eventType = "CUSTOM_ITEM_CREATED",
                entityType = "CUSTOM_ITEM",
                entityId = itemId,
                inputSource = "MANUAL",
                afterSummary = "${item.name} (${PersianDateAndUnitUtils.formatCurrencyAmount(item.defaultPrice, item.defaultCurrency)})",
                humanSummary = "ایجاد دکمه ثبت سریع: «${item.name}» با قیمت ${PersianDateAndUnitUtils.formatCurrencyAmount(item.defaultPrice, item.defaultCurrency)}"
            )
            return Result.success(itemId)
        }

        val docRef = db.collection("users").document(uid).collection("customItems").document(itemId)
        return try {
            docRef.set(item.toCreateMap()).await()
            Result.success(itemId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateCustomItem(item: CustomItem): Result<Unit> {
        val uid = resolveActiveUserId()
        require(item.id.isNotBlank()) { "CustomItem ID must not be blank" }
        val normalized = item.copy(
            userId = uid,
            name = item.name.trim().take(150).ifBlank { "آیتم" },
            category = item.category.take(80).ifBlank { "CUSTOM" },
            unit = PersianDateAndUnitUtils.normalizeUnit(item.unit).take(50).ifBlank { "pieces" },
            defaultQuantity = item.defaultQuantity.coerceAtLeast(0.1),
            dailyTarget = item.dailyTarget.coerceAtLeast(0.0),
            defaultPrice = item.defaultPrice.coerceAtLeast(0.0),
            defaultCurrency = item.defaultCurrency.take(16).ifBlank { "TOMAN" }
        )

        if (localDao != null) {
            localDao.upsertCustomItem(CachedCustomItemEntity.fromDomain(normalized, synced = false))
            logActivityEvent(
                eventType = "CUSTOM_ITEM_UPDATED",
                entityType = "CUSTOM_ITEM",
                entityId = normalized.id,
                inputSource = "MANUAL",
                afterSummary = "${normalized.name} (${PersianDateAndUnitUtils.formatCurrencyAmount(normalized.defaultPrice, normalized.defaultCurrency)})",
                humanSummary = "ویرایش دکمه سریع: «${normalized.name}» ← ${PersianDateAndUnitUtils.formatCurrencyAmount(normalized.defaultPrice, normalized.defaultCurrency)}"
            )
            return Result.success(Unit)
        }

        val docRef = db.collection("users").document(uid).collection("customItems").document(normalized.id)
        return try {
            docRef.update(
                mapOf(
                    "name" to normalized.name,
                    "category" to normalized.category,
                    "unit" to normalized.unit,
                    "defaultQuantity" to normalized.defaultQuantity,
                    "dailyTarget" to normalized.dailyTarget,
                    "defaultPrice" to normalized.defaultPrice,
                    "defaultCurrency" to normalized.defaultCurrency,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteCustomItem(itemId: String, itemName: String = ""): Result<Unit> {
        val uid = resolveActiveUserId()
        if (localDao != null) {
            localDao.deleteCachedCustomItem(uid, itemId)
            localDao.enqueueSyncOperation(
                SyncQueueEntity(
                    userId = uid,
                    operation = "DELETE",
                    entityType = "CUSTOM_ITEM",
                    entityId = itemId,
                    payloadJson = JSONObject().put("name", itemName).toString()
                )
            )
            logActivityEvent(
                eventType = "CUSTOM_ITEM_DELETED",
                entityType = "CUSTOM_ITEM",
                entityId = itemId,
                inputSource = "MANUAL",
                humanSummary = "حذف دکمه ثبت سریع: «${itemName.ifBlank { itemId }}»"
            )
            return Result.success(Unit)
        }

        val docRef = db.collection("users").document(uid).collection("customItems").document(itemId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Expense & Income CRUD (Strictly Separated from Non-Monetary Consumption) ---

    suspend fun addExpense(
        title: String,
        category: String,
        amount: Double,
        currency: String = "TOMAN",
        localEpochDay: Long = PersianDateAndUnitUtils.currentLocalEpochDay(),
        linkedConsumptionId: String = "",
        notes: String = "",
        inputSource: String = "MANUAL",
        idempotencyKey: String = "",
        transactionType: String = "EXPENSE",
        paidAmount: Double = 0.0,
        dueEpochDay: Long = -1L,
        installmentCount: Int = 1,
        linkedDebtId: String = ""
    ): Result<String> {
        val uid = resolveActiveUserId()
        if (idempotencyKey.isNotBlank()) {
            recentIdempotencyKeys["$uid:exp:$idempotencyKey"]?.let { return Result.success(it) }
        }
        val cleanTitle = title.trim().take(250)
        if (cleanTitle.isBlank()) {
            return Result.failure(IllegalArgumentException("وارد کردن تیتر / دلیل برای ثبت خرج یا درآمد الزامی است."))
        }
        val normType = when {
            transactionType.equals("TRANSFER", ignoreCase = true) -> "TRANSFER"
            transactionType.equals("INCOME", ignoreCase = true) -> "INCOME"
            transactionType.equals("DEBT", ignoreCase = true) -> "DEBT"
            transactionType.equals("RECEIVABLE", ignoreCase = true) -> "RECEIVABLE"
            transactionType.equals("DEBT_SETTLED", ignoreCase = true) -> "DEBT_SETTLED"
            transactionType.equals("RECEIVABLE_SETTLED", ignoreCase = true) -> "RECEIVABLE_SETTLED"
            else -> "EXPENSE"
        }
        val docPrefix = when (normType) {
            "TRANSFER" -> "trf"
            "INCOME" -> "inc"
            "DEBT", "DEBT_SETTLED" -> "dbt"
            "RECEIVABLE", "RECEIVABLE_SETTLED" -> "rcv"
            else -> "exp"
        }
        val expenseId = cleanDocId(docPrefix)
        val safeAmount = amount.coerceAtLeast(0.0)
        val safePaid = paidAmount.coerceIn(0.0, safeAmount)
        val expense = ExpenseEntry(
            id = expenseId,
            userId = uid,
            title = cleanTitle,
            category = category,
            amount = safeAmount,
            currency = currency.uppercase(),
            timestampMillis = System.currentTimeMillis(),
            localEpochDay = localEpochDay,
            linkedConsumptionId = linkedConsumptionId.take(128),
            notes = notes.take(2000),
            inputSource = inputSource,
            idempotencyKey = idempotencyKey.take(128),
            transactionType = normType,
            paidAmount = safePaid,
            dueEpochDay = dueEpochDay,
            installmentCount = installmentCount.coerceAtLeast(1),
            linkedDebtId = linkedDebtId.take(128)
        )

        val labelFa = when (normType) {
            "TRANSFER" -> "انتقال بین حساب شخصی"
            "INCOME" -> "ثبت درآمد"
            "DEBT" -> "ثبت بدهکاری"
            "RECEIVABLE" -> "ثبت طلبکاری"
            else -> "ثبت خرج"
        }
        val evType = when (normType) {
            "TRANSFER" -> "TRANSFER_ADDED"
            "INCOME" -> "INCOME_ADDED"
            "DEBT" -> "DEBT_ADDED"
            "RECEIVABLE" -> "RECEIVABLE_ADDED"
            else -> "EXPENSE_ADDED"
        }

        if ((normType == "DEBT" || normType == "RECEIVABLE") && dueEpochDay > PersianDateAndUnitUtils.currentLocalEpochDay()) {
            val triggerMillis = System.currentTimeMillis() + (dueEpochDay - PersianDateAndUnitUtils.currentLocalEpochDay()) * 86_400_000L
            ReminderScheduler.scheduleEntityReminder(
                context = null,
                entityType = "DEBT",
                entityId = expenseId,
                title = "یادآور سررسید: ${expense.title}",
                body = "مبلغ: ${PersianDateAndUnitUtils.formatCurrencyAmount(expense.remainingAmount, expense.currency)}",
                triggerAtMillis = triggerMillis,
                hideSensitiveContent = true
            )
        }

        if (localDao != null) {
            localDao.upsertExpenseOne(CachedExpenseEntity.fromDomain(expense, synced = false))
            if (idempotencyKey.isNotBlank()) {
                recentIdempotencyKeys["$uid:exp:$idempotencyKey"] = expenseId
            }
            logActivityEvent(
                eventType = evType,
                entityType = "EXPENSE",
                entityId = expenseId,
                inputSource = inputSource,
                afterSummary = "$normType: ${expense.amount} ${expense.currency} (${expense.title})",
                humanSummary = "$labelFa: ${PersianDateAndUnitUtils.formatCurrencyAmount(expense.amount, expense.currency)} • «${expense.title}»"
            )
            return Result.success(expenseId)
        }

        val docRef = db.collection("users").document(uid).collection("expenses").document(expenseId)
        return try {
            docRef.set(expense.toCreateMap()).await()
            if (idempotencyKey.isNotBlank()) {
                recentIdempotencyKeys["$uid:exp:$idempotencyKey"] = expenseId
            }
            logActivityEvent(
                eventType = evType,
                entityType = "EXPENSE",
                entityId = expenseId,
                inputSource = inputSource,
                afterSummary = "$normType: ${expense.amount} ${expense.currency} (${expense.title})",
                humanSummary = "$labelFa: ${PersianDateAndUnitUtils.formatCurrencyAmount(expense.amount, expense.currency)} • «${expense.title}»"
            )
            Result.success(expenseId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    /**
     * Phase 2 (debt-goal-link): Creates repayment tasks in Path linked to a single master debt record.
     * Rule: Never creates a second debt record. Tasks only reference [debtEntry.id] via [Task.linkedDebtId].
     */
    suspend fun createDebtRepaymentPlan(
        debtEntry: ExpenseEntry,
        installmentCount: Int,
        firstDueEpochDay: Long = if (debtEntry.dueEpochDay > 0) debtEntry.dueEpochDay else PersianDateAndUnitUtils.currentLocalEpochDay() + 30L,
        intervalDays: Int = 30
    ): Result<List<Task>> {
        val uid = resolveActiveUserId()
        require(debtEntry.id.isNotBlank()) { "Debt ID must not be blank" }
        val safeCount = installmentCount.coerceIn(1, 60)
        val remaining = debtEntry.remainingAmount.coerceAtLeast(0.0)
        val perInstallmentAmount = if (safeCount > 0) remaining / safeCount else remaining

        if (localDao != null) {
            val updatedDebt = debtEntry.copy(
                userId = uid,
                installmentCount = safeCount,
                dueEpochDay = firstDueEpochDay
            )
            localDao.upsertExpenseOne(CachedExpenseEntity.fromDomain(updatedDebt, synced = false))

            val existingLinked = localDao.getTasksLinkedToDebt(uid, debtEntry.id)
            if (existingLinked.isNotEmpty()) {
                return Result.success(existingLinked.map { it.toDomain() })
            }
        }

        val createdTasks = mutableListOf<Task>()
        for (i in 1..safeCount) {
            val dueDay = firstDueEpochDay + (i - 1) * intervalDays.toLong()
            val formattedAmt = PersianDateAndUnitUtils.formatCurrencyAmount(perInstallmentAmount, debtEntry.currency)
            val taskTitle = if (safeCount == 1) {
                "بازپرداخت بدهی: ${debtEntry.title} ($formattedAmt)"
            } else {
                "قسط ${PersianDateAndUnitUtils.toPersianDigits(i.toString())} از ${PersianDateAndUnitUtils.toPersianDigits(safeCount.toString())} • ${debtEntry.title} ($formattedAmt)"
            }
            val idemKey = "debt_plan_${debtEntry.id}_inst_$i"
            val taskIdRes = createTask(
                title = taskTitle,
                description = "ارجاع به بدهی اصلی «${debtEntry.title}» به مبلغ کل ${PersianDateAndUnitUtils.formatCurrencyAmount(debtEntry.amount, debtEntry.currency)}",
                category = "مالی",
                tags = listOf("قسط", "بدهی"),
                priority = "HIGH",
                dueDateEpochDay = dueDay,
                notes = "مبلغ این قسط: $formattedAmt",
                inputSource = "MANUAL",
                idempotencyKey = idemKey,
                linkedDebtId = debtEntry.id
            )
            val taskId = taskIdRes.getOrNull()
            if (taskId != null && localDao != null) {
                localDao.getTasksForUser(uid).find { it.id == taskId }?.toDomain()?.let {
                    createdTasks.add(it)
                }
            }
        }
        return Result.success(createdTasks)
    }

    /**
     * Phase 2 (debt-goal-link): Records a partial or installment payment against a master debt/receivable.
     * Rules:
     * - Idempotent per (debtId, linkedTaskId / idempotencyKey): paying an installment from Path never double-records.
     * - Reduces remainingAmount by increasing paidAmount.
     * - If paidAmount reaches amount, transitions status to SETTLED and marks linked tasks COMPLETED.
     */
    suspend fun recordDebtPartialPayment(
        debtEntry: ExpenseEntry,
        paymentAmount: Double,
        linkedTaskId: String = "",
        idempotencyKey: String = ""
    ): Result<ExpenseEntry> {
        val uid = resolveActiveUserId()
        require(debtEntry.id.isNotBlank()) { "Debt ID must not be blank" }
        if (paymentAmount <= 0.0) {
            return Result.failure(IllegalArgumentException("مبلغ پرداخت باید بیشتر از صفر باشد."))
        }

        val effectiveIdemKey = when {
            idempotencyKey.isNotBlank() -> idempotencyKey
            linkedTaskId.isNotBlank() -> "debt_pay_${debtEntry.id}_task_$linkedTaskId"
            else -> ""
        }

        val currentDebt = if (localDao != null) {
            localDao.getExpenseById(uid, debtEntry.id)?.toDomain() ?: debtEntry
        } else {
            debtEntry
        }

        if (effectiveIdemKey.isNotBlank()) {
            val idemMapKey = "$uid:debtpay:$effectiveIdemKey"
            if (recentIdempotencyKeys.containsKey(idemMapKey)) {
                return Result.success(currentDebt)
            }
            recentIdempotencyKeys[idemMapKey] = currentDebt.id
        }

        if (currentDebt.isSettled) {
            return Result.success(currentDebt)
        }

        val newPaid = (currentDebt.paidAmount + paymentAmount).coerceAtMost(currentDebt.amount)
        val isNowSettled = newPaid >= currentDebt.amount - 0.001
        val newType = when {
            currentDebt.isDebt && isNowSettled -> "DEBT_SETTLED"
            currentDebt.isDebt -> "DEBT"
            currentDebt.isReceivable && isNowSettled -> "RECEIVABLE_SETTLED"
            else -> "RECEIVABLE"
        }
        val updatedNotes = if (isNowSettled && !currentDebt.notes.contains("[SETTLED]")) {
            "${currentDebt.notes} [SETTLED]".trim()
        } else {
            currentDebt.notes
        }

        val updatedDebt = currentDebt.copy(
            userId = uid,
            paidAmount = newPaid,
            transactionType = newType,
            notes = updatedNotes
        )

        if (localDao != null) {
            localDao.upsertExpenseOne(CachedExpenseEntity.fromDomain(updatedDebt, synced = false))
            if (linkedTaskId.isNotBlank()) {
                ReminderScheduler.cancelEntityReminder(null, "TASK", linkedTaskId)
                val linkedTasks = localDao.getTasksLinkedToDebt(uid, currentDebt.id)
                linkedTasks.find { it.id == linkedTaskId && it.status != "COMPLETED" }?.let { taskEntity ->
                    localDao.upsertTask(
                        taskEntity.copy(
                            status = "COMPLETED",
                            completedAtMillis = System.currentTimeMillis(),
                            syncedWithCloud = false
                        )
                    )
                }
            }
            if (isNowSettled) {
                val linkedTasks = localDao.getTasksLinkedToDebt(uid, currentDebt.id)
                ReminderScheduler.cancelDebtAndLinkedTaskReminders(
                    context = null,
                    debtId = currentDebt.id,
                    linkedTaskIds = linkedTasks.map { it.id }
                )
                for (taskEntity in linkedTasks) {
                    if (taskEntity.status != "COMPLETED") {
                        localDao.upsertTask(
                            taskEntity.copy(
                                status = "COMPLETED",
                                completedAtMillis = System.currentTimeMillis(),
                                syncedWithCloud = false
                            )
                        )
                    }
                }
            }
            logActivityEvent(
                eventType = if (isNowSettled) "DEBT_SETTLED" else "DEBT_PARTIAL_PAYMENT",
                entityType = "EXPENSE",
                entityId = currentDebt.id,
                inputSource = "MANUAL",
                beforeSummary = "paid=${currentDebt.paidAmount}/${currentDebt.amount}",
                afterSummary = "paid=$newPaid/${currentDebt.amount}",
                humanSummary = if (isNowSettled) {
                    "تسویه کامل «${currentDebt.title}» (${PersianDateAndUnitUtils.formatCurrencyAmount(currentDebt.amount, currentDebt.currency)})"
                } else {
                    "ثبت پرداخت جزئی «${currentDebt.title}»: ${PersianDateAndUnitUtils.formatCurrencyAmount(paymentAmount, currentDebt.currency)} (مانده: ${PersianDateAndUnitUtils.formatCurrencyAmount(updatedDebt.remainingAmount, currentDebt.currency)})"
                }
            )
            return Result.success(updatedDebt)
        }

        return Result.success(updatedDebt)
    }

    /**
     * Phase 2 (debt-goal-link): Updates due date or total amount of a master debt record
     * and propagates due date changes to linked pending tasks.
     */
    suspend fun updateDebtScheduleOrAmount(
        debtEntry: ExpenseEntry,
        newAmount: Double = debtEntry.amount,
        newDueEpochDay: Long = debtEntry.dueEpochDay
    ): Result<ExpenseEntry> {
        val uid = resolveActiveUserId()
        val safeAmount = newAmount.coerceAtLeast(debtEntry.paidAmount)
        val isNowSettled = safeAmount > 0.0 && debtEntry.paidAmount >= safeAmount - 0.001
        val newType = when {
            debtEntry.isDebt && isNowSettled -> "DEBT_SETTLED"
            debtEntry.isDebt -> "DEBT"
            debtEntry.isReceivable && isNowSettled -> "RECEIVABLE_SETTLED"
            else -> "RECEIVABLE"
        }
        val updated = debtEntry.copy(
            userId = uid,
            amount = safeAmount,
            dueEpochDay = newDueEpochDay,
            transactionType = newType
        )
        if ( isNowSettled ) {
            ReminderScheduler.cancelEntityReminder(null, "DEBT", debtEntry.id)
        } else if (newDueEpochDay > PersianDateAndUnitUtils.currentLocalEpochDay()) {
            val triggerMillis = System.currentTimeMillis() + (newDueEpochDay - PersianDateAndUnitUtils.currentLocalEpochDay()) * 86_400_000L
            ReminderScheduler.rescheduleEntityReminder(
                context = null,
                entityType = "DEBT",
                entityId = debtEntry.id,
                title = "یادآور سررسید: ${updated.title}",
                body = "مبلغ: ${PersianDateAndUnitUtils.formatCurrencyAmount(updated.remainingAmount, updated.currency)}",
                newTriggerAtMillis = triggerMillis,
                hideSensitiveContent = true
            )
        }
        if (localDao != null) {
            localDao.upsertExpenseOne(CachedExpenseEntity.fromDomain(updated, synced = false))
            if (newDueEpochDay > 0) {
                val linkedTasks = localDao.getTasksLinkedToDebt(uid, debtEntry.id)
                    .filter { it.status != "COMPLETED" }
                    .sortedBy { it.dueDateEpochDay }
                linkedTasks.forEachIndexed { idx, taskEntity ->
                    val nextDay = newDueEpochDay + idx * 30L
                    localDao.upsertTask(
                        taskEntity.copy(
                            dueDateEpochDay = nextDay,
                            syncedWithCloud = false
                        )
                    )
                }
            }
        }
        return Result.success(updated)
    }

    suspend fun settleDebtOrReceivable(entry: ExpenseEntry): Result<Unit> {
        val uid = resolveActiveUserId()
        require(entry.id.isNotBlank()) { "Entry ID must not be blank" }
        val settledType = if (entry.isDebt) "DEBT_SETTLED" else "RECEIVABLE_SETTLED"
        val updatedNotes = if (entry.notes.contains("[SETTLED]")) entry.notes else "${entry.notes} [SETTLED]".trim()
        val settledEntry = entry.copy(
            userId = uid,
            transactionType = settledType,
            paidAmount = entry.amount,
            notes = updatedNotes
        )
        val labelFa = if (entry.isDebt) "تسویه بدهکاری" else "وصول طلبکاری"
        val evType = if (entry.isDebt) "DEBT_SETTLED" else "RECEIVABLE_SETTLED"

        if (localDao != null) {
            localDao.upsertExpenseOne(CachedExpenseEntity.fromDomain(settledEntry, synced = false))
            val linkedTasks = localDao.getTasksLinkedToDebt(uid, entry.id)
            ReminderScheduler.cancelDebtAndLinkedTaskReminders(
                context = null,
                debtId = entry.id,
                linkedTaskIds = linkedTasks.map { it.id }
            )
            for (taskEntity in linkedTasks) {
                if (taskEntity.status != "COMPLETED") {
                    localDao.upsertTask(
                        taskEntity.copy(
                            status = "COMPLETED",
                            completedAtMillis = System.currentTimeMillis(),
                            syncedWithCloud = false
                        )
                    )
                }
            }
            logActivityEvent(
                eventType = evType,
                entityType = "EXPENSE",
                entityId = entry.id,
                inputSource = "MANUAL",
                beforeSummary = "${entry.transactionType}: ${entry.amount} ${entry.currency}",
                afterSummary = "$settledType: ${entry.amount} ${entry.currency}",
                humanSummary = "$labelFa: ${PersianDateAndUnitUtils.formatCurrencyAmount(entry.amount, entry.currency)} • «${entry.title}»"
            )
            return Result.success(Unit)
        }

        val docRef = db.collection("users").document(uid).collection("expenses").document(entry.id)
        return try {
            docRef.set(settledEntry.toCreateMap()).await()
            logActivityEvent(
                eventType = evType,
                entityType = "EXPENSE",
                entityId = entry.id,
                inputSource = "MANUAL",
                beforeSummary = "${entry.transactionType}: ${entry.amount} ${entry.currency}",
                afterSummary = "$settledType: ${entry.amount} ${entry.currency}",
                humanSummary = "$labelFa: ${PersianDateAndUnitUtils.formatCurrencyAmount(entry.amount, entry.currency)} • «${entry.title}»"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteExpense(expense: ExpenseEntry): Result<Unit> {
        val uid = resolveActiveUserId()
        val labelFa = when {
            expense.isDebt -> "حذف بدهکاری"
            expense.isReceivable -> "حذف طلبکاری"
            expense.isIncome -> "حذف درآمد"
            else -> "حذف خرج"
        }
        if (localDao != null) {
            localDao.deleteCachedExpense(uid, expense.id)
            localDao.enqueueSyncOperation(
                SyncQueueEntity(
                    userId = uid,
                    operation = "DELETE",
                    entityType = "EXPENSE",
                    entityId = expense.id,
                    payloadJson = JSONObject().put("title", expense.title).put("amount", expense.amount).toString()
                )
            )
            logActivityEvent(
                eventType = "EXPENSE_DELETED",
                entityType = "EXPENSE",
                entityId = expense.id,
                inputSource = "MANUAL",
                beforeSummary = "${expense.amount} ${expense.currency}",
                humanSummary = "$labelFa: «${expense.title}»"
            )
            return Result.success(Unit)
        }

        val docRef = db.collection("users").document(uid).collection("expenses").document(expense.id)
        return try {
            docRef.delete().await()
            logActivityEvent(
                eventType = "EXPENSE_DELETED",
                entityType = "EXPENSE",
                entityId = expense.id,
                inputSource = "MANUAL",
                beforeSummary = "${expense.amount} ${expense.currency}",
                humanSummary = "$labelFa: «${expense.title}»"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Habits & Habit Records ---

    suspend fun createHabit(
        title: String,
        category: String = "شخصی",
        scheduleType: String = "DAILY",
        scheduleDays: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7),
        targetPerPeriod: Int = 1,
        linkedTaskCategory: String = "",
        reminderTimeMinutes: Int = -1
    ): Result<String> {
        val uid = resolveActiveUserId()
        val habitId = cleanDocId("hbt")
        val habit = Habit(
            id = habitId,
            userId = uid,
            title = title.trim().take(200),
            category = category.take(100),
            scheduleType = scheduleType,
            scheduleDays = scheduleDays.take(7),
            targetPerPeriod = targetPerPeriod.coerceIn(1, 100),
            linkedTaskCategory = linkedTaskCategory.take(100),
            reminderTimeMinutes = reminderTimeMinutes,
            archived = false
        )

        if (localDao != null) {
            localDao.upsertHabit(CachedHabitEntity.fromDomain(habit, synced = false))
            logActivityEvent(
                eventType = "HABIT_CREATED",
                entityType = "HABIT",
                entityId = habitId,
                inputSource = "MANUAL",
                afterSummary = habit.title,
                humanSummary = "ایجاد عادت: «${habit.title}»"
            )
            return Result.success(habitId)
        }

        val docRef = db.collection("users").document(uid).collection("habits").document(habitId)
        return try {
            docRef.set(habit.toCreateMap()).await()
            logActivityEvent(
                eventType = "HABIT_CREATED",
                entityType = "HABIT",
                entityId = habitId,
                inputSource = "MANUAL",
                afterSummary = habit.title,
                humanSummary = "ایجاد عادت: «${habit.title}»"
            )
            Result.success(habitId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleHabitCompletionForDay(
        habit: Habit,
        localEpochDay: Long,
        existingRecord: HabitRecord?,
        inputSource: String = "MANUAL"
    ): Result<Unit> {
        val uid = resolveActiveUserId()
        if (localDao != null) {
            if (existingRecord != null) {
                localDao.deleteCachedHabitRecord(uid, existingRecord.id)
                logActivityEvent(
                    eventType = "HABIT_UNCHECKED",
                    entityType = "HABIT",
                    entityId = habit.id,
                    inputSource = inputSource,
                    humanSummary = "لغو ثبت عادت «${habit.title}»"
                )
            } else {
                val recordId = "hrec_${habit.id}_$localEpochDay"
                val record = HabitRecord(
                    id = recordId,
                    userId = uid,
                    habitId = habit.id,
                    localEpochDay = localEpochDay,
                    completedCount = 1,
                    inputSource = inputSource
                )
                localDao.upsertHabitRecord(CachedHabitRecordEntity.fromDomain(record, synced = false))
                logActivityEvent(
                    eventType = "HABIT_COMPLETED",
                    entityType = "HABIT",
                    entityId = habit.id,
                    inputSource = inputSource,
                    humanSummary = "انجام عادت: «${habit.title}»"
                )
            }
            return Result.success(Unit)
        }

        return try {
            if (existingRecord != null) {
                val docRef = db.collection("users").document(uid).collection("habitRecords").document(existingRecord.id)
                docRef.delete().await()
                logActivityEvent(
                    eventType = "HABIT_UNCHECKED",
                    entityType = "HABIT",
                    entityId = habit.id,
                    inputSource = inputSource,
                    humanSummary = "لغو ثبت عادت «${habit.title}»"
                )
            } else {
                val recordId = "hrec_${habit.id}_$localEpochDay"
                val docRef = db.collection("users").document(uid).collection("habitRecords").document(recordId)
                val record = HabitRecord(
                    id = recordId,
                    userId = uid,
                    habitId = habit.id,
                    localEpochDay = localEpochDay,
                    completedCount = 1,
                    inputSource = inputSource
                )
                docRef.set(record.toCreateMap()).await()
                logActivityEvent(
                    eventType = "HABIT_COMPLETED",
                    entityType = "HABIT",
                    entityId = habit.id,
                    inputSource = inputSource,
                    humanSummary = "انجام عادت: «${habit.title}»"
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid/habitRecords")
            Result.failure(e)
        }
    }

    // --- Sleep & Exercise Records ---

    suspend fun addSleepRecord(
        durationMinutes: Int,
        qualityRating: Int = 4,
        sleepStartMillis: Long = System.currentTimeMillis() - durationMinutes * 60_000L,
        wakeUpMillis: Long = System.currentTimeMillis(),
        localEpochDay: Long = PersianDateAndUnitUtils.currentLocalEpochDay(),
        notes: String = "",
        inputSource: String = "MANUAL",
        idempotencyKey: String = ""
    ): Result<String> {
        val uid = resolveActiveUserId()
        if (idempotencyKey.isNotBlank()) {
            recentIdempotencyKeys["$uid:slp:$idempotencyKey"]?.let { return Result.success(it) }
        }
        val recordId = cleanDocId("slp")
        val safeDuration = durationMinutes.coerceIn(1, 1800)
        val safeWake = wakeUpMillis.coerceAtLeast(sleepStartMillis + 60_000L)
        val record = SleepRecord(
            id = recordId,
            userId = uid,
            sleepStartMillis = sleepStartMillis,
            wakeUpMillis = safeWake,
            durationMinutes = safeDuration,
            localEpochDay = localEpochDay,
            qualityRating = qualityRating.coerceIn(1, 5),
            notes = notes.take(2000),
            inputSource = inputSource,
            idempotencyKey = idempotencyKey.take(128)
        )

        if (localDao != null) {
            localDao.upsertSleepRecord(CachedSleepEntity.fromDomain(record, synced = false))
            if (idempotencyKey.isNotBlank()) {
                recentIdempotencyKeys["$uid:slp:$idempotencyKey"] = recordId
            }
            val hours = safeDuration / 60
            val mins = safeDuration % 60
            logActivityEvent(
                eventType = "SLEEP_LOGGED",
                entityType = "SLEEP",
                entityId = recordId,
                inputSource = inputSource,
                afterSummary = "${hours}h ${mins}m (Quality $qualityRating/5)",
                humanSummary = "ثبت خواب: $hours ساعت و $mins دقیقه"
            )
            return Result.success(recordId)
        }

        val docRef = db.collection("users").document(uid).collection("sleepRecords").document(recordId)
        return try {
            docRef.set(record.toCreateMap()).await()
            if (idempotencyKey.isNotBlank()) {
                recentIdempotencyKeys["$uid:slp:$idempotencyKey"] = recordId
            }
            val hours = safeDuration / 60
            val mins = safeDuration % 60
            logActivityEvent(
                eventType = "SLEEP_LOGGED",
                entityType = "SLEEP",
                entityId = recordId,
                inputSource = inputSource,
                afterSummary = "${hours}h ${mins}m (Quality $qualityRating/5)",
                humanSummary = "ثبت خواب: $hours ساعت و $mins دقیقه"
            )
            Result.success(recordId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun addExerciseRecord(
        activityType: String,
        durationMinutes: Int,
        intensity: String = "MODERATE",
        distanceMeters: Double = 0.0,
        repetitions: Int = 0,
        localEpochDay: Long = PersianDateAndUnitUtils.currentLocalEpochDay(),
        notes: String = "",
        inputSource: String = "MANUAL",
        idempotencyKey: String = ""
    ): Result<String> {
        val uid = resolveActiveUserId()
        if (idempotencyKey.isNotBlank()) {
            recentIdempotencyKeys["$uid:exr:$idempotencyKey"]?.let { return Result.success(it) }
        }
        val recordId = cleanDocId("exr")
        val record = ExerciseRecord(
            id = recordId,
            userId = uid,
            activityType = activityType.trim().take(150).ifBlank { "ورزش" },
            durationMinutes = durationMinutes.coerceIn(1, 1440),
            distanceMeters = distanceMeters.coerceAtLeast(0.0),
            intensity = intensity,
            repetitions = repetitions.coerceAtLeast(0),
            timestampMillis = System.currentTimeMillis(),
            localEpochDay = localEpochDay,
            notes = notes.take(2000),
            inputSource = inputSource,
            idempotencyKey = idempotencyKey.take(128)
        )

        if (localDao != null) {
            localDao.upsertExerciseRecord(CachedExerciseEntity.fromDomain(record, synced = false))
            if (idempotencyKey.isNotBlank()) {
                recentIdempotencyKeys["$uid:exr:$idempotencyKey"] = recordId
            }
            logActivityEvent(
                eventType = "EXERCISE_LOGGED",
                entityType = "EXERCISE",
                entityId = recordId,
                inputSource = inputSource,
                afterSummary = "${record.activityType} (${record.durationMinutes} min)",
                humanSummary = "ثبت ورزش: ${record.durationMinutes} دقیقه ${record.activityType}"
            )
            return Result.success(recordId)
        }

        val docRef = db.collection("users").document(uid).collection("exerciseRecords").document(recordId)
        return try {
            docRef.set(record.toCreateMap()).await()
            if (idempotencyKey.isNotBlank()) {
                recentIdempotencyKeys["$uid:exr:$idempotencyKey"] = recordId
            }
            logActivityEvent(
                eventType = "EXERCISE_LOGGED",
                entityType = "EXERCISE",
                entityId = recordId,
                inputSource = inputSource,
                afterSummary = "${record.activityType} (${record.durationMinutes} min)",
                humanSummary = "ثبت ورزش: ${record.durationMinutes} دقیقه ${record.activityType}"
            )
            Result.success(recordId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Conversations & Preferences ---

    suspend fun addConversationTurn(
        role: String,
        messageText: String,
        structuredActionJson: String = ""
    ): Result<String> {
        val uid = resolveActiveUserId()
        val convId = cleanDocId("msg")
        val turn = ConversationTurn(
            id = convId,
            userId = uid,
            role = role,
            messageText = messageText.take(8000),
            structuredActionJson = structuredActionJson.take(10000),
            timestampMillis = System.currentTimeMillis()
        )

        if (localDao != null) {
            localDao.upsertConversation(CachedConversationEntity.fromDomain(turn, synced = false))
            return Result.success(convId)
        }

        val docRef = db.collection("users").document(uid).collection("conversations").document(convId)
        return try {
            docRef.set(turn.toCreateMap()).await()
            Result.success(convId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun clearAllConversations(): Result<Unit> {
        val uid = resolveActiveUserId()
        if (localDao != null) {
            localDao.clearCachedConversations(uid)
            logActivityEvent(
                eventType = "CONVERSATIONS_CLEARED",
                entityType = "CONVERSATION",
                entityId = "all",
                inputSource = "MANUAL",
                humanSummary = "پاکسازی تاریخچه گفتگوهای هوش مصنوعی"
            )
            return Result.success(Unit)
        }

        val col = db.collection("users").document(uid).collection("conversations")
        return try {
            val snap = col.whereEqualTo("userId", uid).get().await()
            for (doc in snap.documents) {
                doc.reference.delete().await()
            }
            logActivityEvent(
                eventType = "CONVERSATIONS_CLEARED",
                entityType = "CONVERSATION",
                entityId = "all",
                inputSource = "MANUAL",
                humanSummary = "پاکسازی تاریخچه گفتگوهای هوش مصنوعی"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$uid/conversations")
            Result.failure(e)
        }
    }

    suspend fun saveUserPreferencesToCloud(prefs: UserPreferencesDoc): Result<Unit> {
        val cloudUser = auth?.currentUser
        if (localDao != null && (cloudUser == null || cloudDb == null)) {
            logActivityEvent(
                eventType = "SETTINGS_UPDATED",
                entityType = "PREFERENCES",
                entityId = "default",
                inputSource = "MANUAL",
                humanSummary = "ذخیره تنظیمات و اهداف روزانه روی گوشی"
            )
            return Result.success(Unit)
        }

        val uid = cloudUser?.uid ?: requireUserId()
        val docRef = db.collection("users").document(uid).collection("userPreferences").document("default")
        val payload = prefs.copy(userId = uid).toCreateMap()
        return try {
            docRef.set(payload).await()
            logActivityEvent(
                eventType = "SETTINGS_UPDATED",
                entityType = "PREFERENCES",
                entityId = "default",
                inputSource = "MANUAL",
                humanSummary = "به‌روزرسانی تنظیمات و اهداف روزانه"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    /**
     * 6-Hour Periodic Data Transfer Engine:
     * 1. Writes an automatic on-device JSON snapshot (`personal_life_os_6h_sync.json`) so local users
     *    without an account always have a fresh 6-hour backup file on their phone.
     * 2. If a Google account is signed in, transfers all unsynced Room records to Cloud Firestore
     *    and marks them as synced (`syncedWithCloud = true`).
     */
    suspend fun performSixHourDataTransfer(context: Context? = null): Result<Int> = withContext(Dispatchers.IO) {
        val dao = localDao ?: return@withContext Result.success(0)
        val uid = resolveActiveUserId()

        try {
            val unsyncedTasks = dao.getUnsyncedTasks(uid)
            val unsyncedCons = dao.getUnsyncedConsumption(uid)
            val unsyncedItems = dao.getUnsyncedCustomItems(uid)
            val unsyncedExp = dao.getUnsyncedExpenses(uid)
            val unsyncedHabits = dao.getUnsyncedHabits(uid)
            val unsyncedHabitRecs = dao.getUnsyncedHabitRecords(uid)
            val unsyncedSleep = dao.getUnsyncedSleepRecords(uid)
            val unsyncedExercise = dao.getUnsyncedExerciseRecords(uid)
            val unsyncedEvents = dao.getUnsyncedActivityEvents(uid)
            val unsyncedConvs = dao.getUnsyncedConversations(uid)

            val totalPending = unsyncedTasks.size + unsyncedCons.size + unsyncedItems.size +
                unsyncedExp.size + unsyncedHabits.size + unsyncedHabitRecs.size +
                unsyncedSleep.size + unsyncedExercise.size + unsyncedEvents.size + unsyncedConvs.size

            // 1. Write 6-hour local snapshot file + AES-256-GCM encrypted backup file on the device
            if (context != null) {
                val allTasks = dao.getTasksForUser(uid)
                val snapshotJson = JSONObject().apply {
                    put("syncCycle", "EVERY_6_HOURS")
                    put("timestampMillis", System.currentTimeMillis())
                    put("userId", uid)
                    put("pendingRecordsTransferred", totalPending)
                    put("totalTasksOnDevice", allTasks.size)
                    put("tasks", JSONArray().apply {
                        allTasks.forEach { t ->
                            put(JSONObject().apply {
                                put("id", t.id)
                                put("title", t.title)
                                put("status", t.status)
                                put("dueDateEpochDay", t.dueDateEpochDay)
                                put("actualDurationMinutes", t.actualDurationMinutes)
                            })
                        }
                    })
                    put("unsyncedExpensesSnapshot", JSONArray().apply {
                        unsyncedExp.forEach { e ->
                            put(JSONObject().apply {
                                put("id", e.id)
                                put("title", e.title)
                                put("category", e.category)
                                put("amount", e.amount)
                                put("currency", e.currency)
                                put("transactionType", e.transactionType)
                                put("localEpochDay", e.localEpochDay)
                            })
                        }
                    })
                }
                val plainSnapshot = snapshotJson.toString(2)
                val outFile = File(context.applicationContext.filesDir, "personal_life_os_6h_sync.json")
                outFile.writeText(plainSnapshot)
                WorldClassOsUtils.EncryptedBackupManager.writeEncryptedBackupFile(
                    context = context.applicationContext,
                    plainJson = plainSnapshot
                )
            }

            // 1b. Drain FIFO sync_queue entries (including propagating DELETE tombstones to Firestore)
            val cloudUid = auth?.currentUser?.uid
            val fifoQueue = dao.getPendingSyncQueue(uid)
            for (qItem in fifoQueue) {
                if (qItem.operation == "DELETE" && cloudUid != null && cloudDb != null) {
                    val subCollection = when (qItem.entityType) {
                        "TASK" -> "tasks"
                        "CONSUMPTION" -> "consumptionEntries"
                        "CUSTOM_ITEM" -> "customItems"
                        "EXPENSE" -> "expenses"
                        "HABIT" -> "habits"
                        "HABIT_RECORD" -> "habitRecords"
                        else -> null
                    }
                    if (subCollection != null) {
                        try {
                            cloudDb.collection("users").document(cloudUid)
                                .collection(subCollection)
                                .document(qItem.entityId)
                                .delete()
                                .await()
                        } catch (_: Exception) {
                        }
                    }
                }
                dao.markSyncQueueItemSynced(qItem.queueId)
            }
            dao.pruneSyncedQueueItems(uid)

            // 2. If signed in with a Google Account, push unsynced records to Cloud Firestore
            if (cloudUid != null && cloudDb != null) {
                val userDoc = cloudDb.collection("users").document(cloudUid)

                for (t in unsyncedTasks) {
                    val domain = t.toDomain().copy(userId = cloudUid)
                    userDoc.collection("tasks").document(t.id).set(domain.toCreateMap()).await()
                    dao.upsertTask(t.copy(userId = cloudUid, syncedWithCloud = true))
                }
                for (c in unsyncedCons) {
                    val domain = c.toDomain().copy(userId = cloudUid)
                    userDoc.collection("consumptionEntries").document(c.id).set(domain.toCreateMap()).await()
                    dao.upsertConsumptionOne(c.copy(userId = cloudUid, syncedWithCloud = true))
                }
                for (i in unsyncedItems) {
                    val domain = i.toDomain().copy(userId = cloudUid)
                    userDoc.collection("customItems").document(i.id).set(domain.toCreateMap()).await()
                    dao.upsertCustomItem(i.copy(userId = cloudUid, syncedWithCloud = true))
                }
                for (e in unsyncedExp) {
                    val domain = e.toDomain().copy(userId = cloudUid)
                    userDoc.collection("expenses").document(e.id).set(domain.toCreateMap()).await()
                    dao.upsertExpenseOne(e.copy(userId = cloudUid, syncedWithCloud = true))
                }
                for (h in unsyncedHabits) {
                    val domain = h.toDomain().copy(userId = cloudUid)
                    userDoc.collection("habits").document(h.id).set(domain.toCreateMap()).await()
                    dao.upsertHabit(h.copy(userId = cloudUid, syncedWithCloud = true))
                }
                for (hr in unsyncedHabitRecs) {
                    val domain = hr.toDomain().copy(userId = cloudUid)
                    userDoc.collection("habitRecords").document(hr.id).set(domain.toCreateMap()).await()
                    dao.upsertHabitRecord(hr.copy(userId = cloudUid, syncedWithCloud = true))
                }
                for (s in unsyncedSleep) {
                    val domain = s.toDomain().copy(userId = cloudUid)
                    userDoc.collection("sleepRecords").document(s.id).set(domain.toCreateMap()).await()
                    dao.upsertSleepRecord(s.copy(userId = cloudUid, syncedWithCloud = true))
                }
                for (ex in unsyncedExercise) {
                    val domain = ex.toDomain().copy(userId = cloudUid)
                    userDoc.collection("exerciseRecords").document(ex.id).set(domain.toCreateMap()).await()
                    dao.upsertExerciseRecord(ex.copy(userId = cloudUid, syncedWithCloud = true))
                }
                for (ev in unsyncedEvents) {
                    val domain = ev.toDomain().copy(userId = cloudUid)
                    userDoc.collection("activityEvents").document(ev.id).set(domain.toCreateMap()).await()
                    dao.upsertActivityEvent(ev.copy(userId = cloudUid, syncedWithCloud = true))
                }
                for (cv in unsyncedConvs) {
                    val domain = cv.toDomain().copy(userId = cloudUid)
                    userDoc.collection("conversations").document(cv.id).set(domain.toCreateMap()).await()
                    dao.upsertConversation(cv.copy(userId = cloudUid, syncedWithCloud = true))
                }
            } else {
                // In local-only mode without an account, mark records as snapshotted in the 6-hour local transfer
                unsyncedTasks.forEach { dao.upsertTask(it.copy(syncedWithCloud = true)) }
                unsyncedCons.forEach { dao.upsertConsumptionOne(it.copy(syncedWithCloud = true)) }
                unsyncedItems.forEach { dao.upsertCustomItem(it.copy(syncedWithCloud = true)) }
                unsyncedExp.forEach { dao.upsertExpenseOne(it.copy(syncedWithCloud = true)) }
                unsyncedHabits.forEach { dao.upsertHabit(it.copy(syncedWithCloud = true)) }
                unsyncedHabitRecs.forEach { dao.upsertHabitRecord(it.copy(syncedWithCloud = true)) }
                unsyncedSleep.forEach { dao.upsertSleepRecord(it.copy(syncedWithCloud = true)) }
                unsyncedExercise.forEach { dao.upsertExerciseRecord(it.copy(syncedWithCloud = true)) }
                unsyncedEvents.forEach { dao.upsertActivityEvent(it.copy(syncedWithCloud = true)) }
                unsyncedConvs.forEach { dao.upsertConversation(it.copy(syncedWithCloud = true)) }
            }

            val syncTargetDesc = if (cloudUid != null) "ابری (Cloud Firestore) و حافظه گوشی" else "پشتیبان دوره‌ای روی حافظه گوشی"
            val syncEvent = ActivityEvent(
                id = cleanDocId("evt"),
                userId = uid,
                timestampMillis = System.currentTimeMillis(),
                eventType = "SIX_HOUR_DATA_TRANSFER",
                entityType = "SYNC",
                entityId = "sync_6h",
                inputSource = "SYSTEM_6H",
                operationStatus = "EXECUTED",
                verificationSource = "CLIENT_RECORDED",
                humanSummary = "انتقال ۶ ساعته اطلاعات انجام شد ($totalPending رکورد به $syncTargetDesc)"
            )
            dao.upsertActivityEvent(CachedActivityEventEntity.fromDomain(syncEvent, synced = true))

            Result.success(totalPending)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAllUserData(): Result<Unit> {
        val uid = resolveActiveUserId()
        if (localDao != null) {
            localDao.clearTasksForUser(uid)
            localDao.clearConsumptionForUser(uid)
            localDao.clearCustomItemsForUser(uid)
            localDao.clearExpensesForUser(uid)
            localDao.clearHabitsForUser(uid)
            localDao.clearHabitRecordsForUser(uid)
            localDao.clearSleepForUser(uid)
            localDao.clearExerciseForUser(uid)
            localDao.clearCachedConversations(uid)
        }

        val cloudUid = auth?.currentUser?.uid
        if ((cloudUid == null || cloudDb == null) && localDao != null) {
            return Result.success(Unit)
        }

        val targetUid = cloudUid ?: requireUserId()
        val subcollections = listOf(
            "tasks", "consumptionEntries", "customItems", "expenses",
            "habits", "habitRecords", "sleepRecords", "exerciseRecords",
            "conversations", "userPreferences"
        )
        return try {
            for (sub in subcollections) {
                val snap = db.collection("users").document(targetUid).collection(sub)
                    .whereEqualTo("userId", targetUid)
                    .get()
                    .await()
                for (doc in snap.documents) {
                    doc.reference.delete().await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$targetUid")
            Result.failure(e)
        }
    }
}
