package com.example.data.local.task

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Room Database Entity for the 'Tasks' module.
 *
 * Persian Language Support Considerations:
 * 1. [normalizedSearchIndex]: Automatically populated with normalized text (Yeh/Kaf unified, ZWNJ/نیم‌فاصله stripped,
 *    numbers unified) to allow resilient full-text and LIKE search queries in SQLite regardless of keyboard layouts.
 * 2. [category]: Defaults to standard Persian categories ("شخصی", "کاری", "سلامت", "مالی", "یادگیری", "عمومی").
 * 3. [dueDateEpochDay]: Stores date as integer Epoch Day, enabling seamless Jalali (Shamsi) calendar indexing and queries.
 * 4. Helper extensions provide standard Persian titles for priorities and statuses.
 */
@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["status"]),
        Index(value = ["dueDateEpochDay"]),
        Index(value = ["category"]),
        Index(value = ["priority"]),
        Index(value = ["userId", "status", "dueDateEpochDay"]),
        Index(value = ["normalizedSearchIndex"])
    ]
)
data class TaskEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "userId")
    val userId: String = "local_device_user",

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description", defaultValue = "''")
    val description: String = "",

    @ColumnInfo(name = "category", defaultValue = "'شخصی'")
    val category: String = "شخصی",

    @ColumnInfo(name = "priority", defaultValue = "'MEDIUM'")
    val priority: String = "MEDIUM", // CRITICAL, HIGH, MEDIUM, LOW

    @ColumnInfo(name = "status", defaultValue = "'PENDING'")
    val status: String = "PENDING", // PENDING, IN_PROGRESS, COMPLETED, ARCHIVED

    @ColumnInfo(name = "dueDateEpochDay", defaultValue = "-1")
    val dueDateEpochDay: Long = -1L, // -1 means no due date; otherwise local Epoch Day (compatible with Jalali conversions)

    @ColumnInfo(name = "dueTimeMinutes", defaultValue = "-1")
    val dueTimeMinutes: Int = -1, // Minutes from midnight (0..1439), -1 means all-day

    @ColumnInfo(name = "reminderAtMillis", defaultValue = "-1")
    val reminderAtMillis: Long = -1L,

    @ColumnInfo(name = "estimatedDurationMinutes", defaultValue = "30")
    val estimatedDurationMinutes: Int = 30,

    @ColumnInfo(name = "actualDurationMinutes", defaultValue = "0")
    val actualDurationMinutes: Int = 0,

    @ColumnInfo(name = "subtasksJson", defaultValue = "'[]'")
    val subtasksJson: String = "[]",

    @ColumnInfo(name = "recurrenceRule", defaultValue = "'NONE'")
    val recurrenceRule: String = "NONE", // NONE, DAILY, WEEKLY, MONTHLY

    @ColumnInfo(name = "recurrenceIntervalDays", defaultValue = "0")
    val recurrenceIntervalDays: Int = 0,

    @ColumnInfo(name = "notes", defaultValue = "''")
    val notes: String = "",

    @ColumnInfo(name = "tagsCsv", defaultValue = "''")
    val tagsCsv: String = "",

    @ColumnInfo(name = "sortOrder", defaultValue = "0")
    val sortOrder: Int = 0,

    @ColumnInfo(name = "inputSource", defaultValue = "'MANUAL'")
    val inputSource: String = "MANUAL", // MANUAL, VOICE, QUICK_ADD, AI_ASSISTANT

    @ColumnInfo(name = "completedAtMillis", defaultValue = "-1")
    val completedAtMillis: Long = -1L,

    @ColumnInfo(name = "createdAtMillis")
    val createdAtMillis: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updatedAtMillis")
    val updatedAtMillis: Long = System.currentTimeMillis(),

    /**
     * Normalized string containing title, description, category, and notes
     * with Persian/Arabic character unification, ZWNJ stripping, and number mapping
     * for high-speed, tolerant Persian search.
     */
    @ColumnInfo(name = "normalizedSearchIndex", defaultValue = "''")
    val normalizedSearchIndex: String = PersianTextNormalizer.buildSearchIndex(
        title, description, category, notes
    )
) {

    /**
     * Persian title representation of priority.
     */
    val priorityFa: String
        get() = when (priority.uppercase()) {
            "CRITICAL" -> "بحرانی"
            "HIGH" -> "بالا"
            "MEDIUM" -> "متوسط"
            "LOW" -> "کم"
            else -> "عادی"
        }

    /**
     * Persian title representation of status.
     */
    val statusFa: String
        get() = when (status.uppercase()) {
            "COMPLETED" -> "تکمیل‌شده"
            "IN_PROGRESS" -> "در حال انجام"
            "ARCHIVED" -> "آرشیو شده"
            else -> "در انتظار"
        }

    fun isOverdue(currentEpochDay: Long): Boolean =
        status != "COMPLETED" && status != "ARCHIVED" && dueDateEpochDay in 0 until currentEpochDay

    fun isDueToday(currentEpochDay: Long): Boolean =
        dueDateEpochDay == currentEpochDay

    companion object {
        fun create(
            title: String,
            userId: String = "local_device_user",
            description: String = "",
            category: String = "شخصی",
            priority: String = "MEDIUM",
            dueDateEpochDay: Long = -1L,
            dueTimeMinutes: Int = -1,
            reminderAtMillis: Long = -1L,
            estimatedDurationMinutes: Int = 30,
            subtasksJson: String = "[]",
            recurrenceRule: String = "NONE",
            recurrenceIntervalDays: Int = 0,
            notes: String = "",
            tagsCsv: String = "",
            inputSource: String = "MANUAL"
        ): TaskEntity {
            val now = System.currentTimeMillis()
            val normalized = PersianTextNormalizer.buildSearchIndex(title, description, category, notes)
            return TaskEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                title = title.trim(),
                description = description.trim(),
                category = category.trim().ifEmpty { "شخصی" },
                priority = priority,
                status = "PENDING",
                dueDateEpochDay = dueDateEpochDay,
                dueTimeMinutes = dueTimeMinutes,
                reminderAtMillis = reminderAtMillis,
                estimatedDurationMinutes = estimatedDurationMinutes,
                subtasksJson = subtasksJson,
                recurrenceRule = recurrenceRule,
                recurrenceIntervalDays = recurrenceIntervalDays,
                notes = notes.trim(),
                tagsCsv = tagsCsv,
                inputSource = inputSource,
                createdAtMillis = now,
                updatedAtMillis = now,
                normalizedSearchIndex = normalized
            )
        }
    }
}
