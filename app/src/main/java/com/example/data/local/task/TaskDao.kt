package com.example.data.local.task

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for the 'Tasks' module.
 *
 * Implements basic and advanced CRUD operations with full reactive Flow support,
 * Persian search integration, and Jalali (Epoch Day) date filtering.
 */
@Dao
interface TaskDao {

    // ==========================================
    // 1. CREATE (Insert & Upsert)
    // ==========================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTasks(tasks: List<TaskEntity>)

    // ==========================================
    // 2. READ (Flows & Direct Queries)
    // ==========================================

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    fun observeTaskById(taskId: String): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    suspend fun getTaskById(taskId: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE userId = :userId ORDER BY sortOrder ASC, updatedAtMillis DESC")
    fun observeAllTasks(userId: String): Flow<List<TaskEntity>>

    /**
     * Observes pending and active tasks (excluding completed and archived).
     * Ordered by due date (today/overdue first) and user sort order.
     */
    @Query("""
        SELECT * FROM tasks 
        WHERE userId = :userId AND status NOT IN ('COMPLETED', 'ARCHIVED')
        ORDER BY 
            CASE WHEN dueDateEpochDay >= 0 THEN dueDateEpochDay ELSE 999999 END ASC,
            CASE WHEN dueTimeMinutes >= 0 THEN dueTimeMinutes ELSE 9999 END ASC,
            sortOrder ASC,
            updatedAtMillis DESC
    """)
    fun observeActiveTasks(userId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE userId = :userId AND status = :status ORDER BY updatedAtMillis DESC")
    fun observeTasksByStatus(userId: String, status: String): Flow<List<TaskEntity>>

    /**
     * Observes tasks due on a specific Jalali / Shamsi date represented by its Epoch Day.
     */
    @Query("""
        SELECT * FROM tasks 
        WHERE userId = :userId AND dueDateEpochDay = :epochDay AND status != 'ARCHIVED'
        ORDER BY 
            CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END ASC,
            CASE WHEN dueTimeMinutes >= 0 THEN dueTimeMinutes ELSE 9999 END ASC,
            sortOrder ASC
    """)
    fun observeTasksForEpochDay(userId: String, epochDay: Long): Flow<List<TaskEntity>>

    /**
     * Observes overdue tasks whose due date has passed today's Epoch Day and are not completed.
     */
    @Query("""
        SELECT * FROM tasks 
        WHERE userId = :userId 
          AND dueDateEpochDay >= 0 
          AND dueDateEpochDay < :currentEpochDay 
          AND status NOT IN ('COMPLETED', 'ARCHIVED')
        ORDER BY dueDateEpochDay ASC, dueTimeMinutes ASC
    """)
    fun observeOverdueTasks(userId: String, currentEpochDay: Long): Flow<List<TaskEntity>>

    @Query("""
        SELECT * FROM tasks 
        WHERE userId = :userId AND category = :category AND status != 'ARCHIVED'
        ORDER BY updatedAtMillis DESC
    """)
    fun observeTasksByCategory(userId: String, category: String): Flow<List<TaskEntity>>

    @Query("""
        SELECT * FROM tasks 
        WHERE userId = :userId AND priority = :priority AND status != 'ARCHIVED'
        ORDER BY updatedAtMillis DESC
    """)
    fun observeTasksByPriority(userId: String, priority: String): Flow<List<TaskEntity>>

    /**
     * Resilient Persian text search query.
     * Checks against [normalizedSearchIndex] (unifying Yeh, Kaf, ZWNJ, and numerals)
     * as well as original title and description columns.
     */
    @Query("""
        SELECT * FROM tasks 
        WHERE userId = :userId 
          AND status != 'ARCHIVED'
          AND (
            normalizedSearchIndex LIKE '%' || :normalizedQuery || '%'
            OR normalizedSearchIndex LIKE '%' || :continuousQuery || '%'
            OR title LIKE '%' || :rawQuery || '%'
            OR description LIKE '%' || :rawQuery || '%'
            OR notes LIKE '%' || :rawQuery || '%'
          )
        ORDER BY updatedAtMillis DESC
    """)
    fun searchTasksPersian(
        userId: String,
        normalizedQuery: String,
        continuousQuery: String = normalizedQuery,
        rawQuery: String = normalizedQuery
    ): Flow<List<TaskEntity>>

    /**
     * Helper to search with automatic normalization.
     */
    fun searchTasks(userId: String, query: String): Flow<List<TaskEntity>> {
        val normalized = PersianTextNormalizer.normalizeForSearch(query, replaceZwnjWithSpace = true)
        val continuous = PersianTextNormalizer.normalizeNoSpace(query)
        return searchTasksPersian(
            userId = userId,
            normalizedQuery = normalized,
            continuousQuery = continuous,
            rawQuery = query.trim()
        )
    }

    @Query("SELECT COUNT(*) FROM tasks WHERE userId = :userId AND status NOT IN ('COMPLETED', 'ARCHIVED')")
    fun countPendingTasks(userId: String): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM tasks 
        WHERE userId = :userId 
          AND status = 'COMPLETED' 
          AND (dueDateEpochDay = :todayEpochDay OR completedAtMillis >= :startOfDayMillis)
    """)
    fun countCompletedToday(userId: String, todayEpochDay: Long, startOfDayMillis: Long): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM tasks 
        WHERE userId = :userId 
          AND dueDateEpochDay >= 0 
          AND dueDateEpochDay < :currentEpochDay 
          AND status NOT IN ('COMPLETED', 'ARCHIVED')
    """)
    fun countOverdueTasks(userId: String, currentEpochDay: Long): Flow<Int>

    @Query("SELECT DISTINCT category FROM tasks WHERE userId = :userId")
    suspend fun getDistinctCategories(userId: String): List<String>

    // ==========================================
    // 3. UPDATE
    // ==========================================

    @Update
    suspend fun updateTask(task: TaskEntity): Int

    @Query("""
        UPDATE tasks 
        SET status = :newStatus, 
            completedAtMillis = :completedAtMillis, 
            updatedAtMillis = :updatedAtMillis 
        WHERE id = :taskId
    """)
    suspend fun updateTaskStatus(
        taskId: String,
        newStatus: String,
        completedAtMillis: Long = if (newStatus == "COMPLETED") System.currentTimeMillis() else -1L,
        updatedAtMillis: Long = System.currentTimeMillis()
    ): Int

    @Query("""
        UPDATE tasks 
        SET dueDateEpochDay = :newDueDateEpochDay, 
            dueTimeMinutes = :newDueTimeMinutes, 
            updatedAtMillis = :updatedAtMillis 
        WHERE id = :taskId
    """)
    suspend fun rescheduleTask(
        taskId: String,
        newDueDateEpochDay: Long,
        newDueTimeMinutes: Int = -1,
        updatedAtMillis: Long = System.currentTimeMillis()
    ): Int

    @Query("""
        UPDATE tasks 
        SET priority = :newPriority, 
            updatedAtMillis = :updatedAtMillis 
        WHERE id = :taskId
    """)
    suspend fun updatePriority(
        taskId: String,
        newPriority: String,
        updatedAtMillis: Long = System.currentTimeMillis()
    ): Int

    @Query("""
        UPDATE tasks 
        SET sortOrder = :newSortOrder, 
            updatedAtMillis = :updatedAtMillis 
        WHERE id = :taskId
    """)
    suspend fun updateSortOrder(
        taskId: String,
        newSortOrder: Int,
        updatedAtMillis: Long = System.currentTimeMillis()
    ): Int

    @Query("""
        UPDATE tasks 
        SET actualDurationMinutes = actualDurationMinutes + :additionalMinutes, 
            updatedAtMillis = :updatedAtMillis 
        WHERE id = :taskId
    """)
    suspend fun logFocusDuration(
        taskId: String,
        additionalMinutes: Int,
        updatedAtMillis: Long = System.currentTimeMillis()
    ): Int

    // ==========================================
    // 4. DELETE & ARCHIVE
    // ==========================================

    @Delete
    suspend fun deleteTask(task: TaskEntity): Int

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: String): Int

    @Query("DELETE FROM tasks WHERE userId = :userId AND status = 'COMPLETED'")
    suspend fun deleteCompletedTasks(userId: String): Int

    @Query("UPDATE tasks SET status = 'ARCHIVED', updatedAtMillis = :updatedAtMillis WHERE id = :taskId")
    suspend fun archiveTask(taskId: String, updatedAtMillis: Long = System.currentTimeMillis()): Int

    @Query("DELETE FROM tasks WHERE userId = :userId")
    suspend fun deleteAllTasksForUser(userId: String): Int
}
