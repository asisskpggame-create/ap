package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class SubtaskItem(
    val id: String = "",
    val title: String = "",
    val completed: Boolean = false
)

@IgnoreExtraProperties
data class Task(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "عمومی",
    val tags: List<String> = emptyList(),
    val priority: String = "MEDIUM", // LOW, MEDIUM, HIGH, CRITICAL
    val dueDateEpochDay: Long = -1L,
    val dueTimeMinutes: Int = -1, // 0..1439 or -1
    val reminderAtMillis: Long = -1L,
    val estimatedDurationMinutes: Int = 30,
    val status: String = "PENDING", // PENDING, IN_PROGRESS, COMPLETED, ARCHIVED
    val subtasksJson: String = "[]",
    val recurrenceRule: String = "NONE", // NONE, DAILY, WEEKLY, MONTHLY, CUSTOM
    val recurrenceIntervalDays: Int = 0,
    val notes: String = "",
    val inputSource: String = "MANUAL", // MANUAL, VOICE, AI_ASSISTANT, RECURRING
    val sortOrder: Int = 0,
    val completedAtMillis: Long = -1L,
    val idempotencyKey: String = "",
    val actualDurationMinutes: Int = 0,
    val linkedDebtId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "title" to title,
        "description" to description,
        "category" to category,
        "tags" to tags.take(20),
        "priority" to priority,
        "dueDateEpochDay" to dueDateEpochDay,
        "dueTimeMinutes" to dueTimeMinutes,
        "reminderAtMillis" to reminderAtMillis,
        "estimatedDurationMinutes" to estimatedDurationMinutes,
        "status" to status,
        "subtasksJson" to subtasksJson,
        "recurrenceRule" to recurrenceRule,
        "recurrenceIntervalDays" to recurrenceIntervalDays,
        "notes" to notes,
        "inputSource" to inputSource,
        "sortOrder" to sortOrder,
        "completedAtMillis" to completedAtMillis,
        "idempotencyKey" to idempotencyKey,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class ConsumptionEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val itemId: String = "",
    val itemName: String = "",
    val category: String = "WATER", // WATER, COFFEE, TEA, BEVERAGE, FOOD, SUPPLEMENT, CUSTOM
    val quantity: Double = 0.0,
    val unit: String = "ml", // ml, liters, cups, pieces, servings, grams, kilograms, tablets, minutes, hours
    val timestampMillis: Long = 0L,
    val localEpochDay: Long = 0L,
    val linkedExpenseId: String = "",
    val linkedTaskId: String = "",
    val notes: String = "",
    val inputSource: String = "MANUAL", // MANUAL, QUICK_ADD, VOICE, AI_ASSISTANT
    val idempotencyKey: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "itemId" to itemId,
        "itemName" to itemName,
        "category" to category,
        "quantity" to quantity,
        "unit" to unit,
        "timestampMillis" to timestampMillis,
        "localEpochDay" to localEpochDay,
        "linkedExpenseId" to linkedExpenseId,
        "linkedTaskId" to linkedTaskId,
        "notes" to notes,
        "inputSource" to inputSource,
        "idempotencyKey" to idempotencyKey,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class CustomItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val category: String = "CUSTOM",
    val unit: String = "pieces",
    val defaultQuantity: Double = 1.0,
    val dailyTarget: Double = 0.0,
    val defaultPrice: Double = 0.0,
    val defaultCurrency: String = "TOMAN",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "name" to name,
        "category" to category,
        "unit" to unit,
        "defaultQuantity" to defaultQuantity,
        "dailyTarget" to dailyTarget,
        "defaultPrice" to defaultPrice,
        "defaultCurrency" to defaultCurrency,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class ExpenseEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val category: String = "OTHER", // FOOD, COFFEE, TRANSPORT, SHOPPING, BILLS, HEALTH, OTHER
    val amount: Double = 0.0,
    val currency: String = "TOMAN", // IRR, TOMAN, EUR, USD, GBP, AED
    val timestampMillis: Long = 0L,
    val localEpochDay: Long = 0L,
    val linkedConsumptionId: String = "",
    val notes: String = "",
    val inputSource: String = "MANUAL", // MANUAL, VOICE, AI_ASSISTANT
    val idempotencyKey: String = "",
    val transactionType: String = "EXPENSE", // EXPENSE, INCOME, TRANSFER, DEBT, RECEIVABLE, DEBT_SETTLED, RECEIVABLE_SETTLED
    val paidAmount: Double = 0.0,
    val dueEpochDay: Long = -1L,
    val installmentCount: Int = 1,
    val linkedDebtId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val isTransfer: Boolean
        get() = transactionType.equals("TRANSFER", ignoreCase = true) || notes.startsWith("[TRANSFER]")

    val isIncome: Boolean
        get() = !isTransfer && (transactionType.equals("INCOME", ignoreCase = true) || notes.startsWith("[INCOME]"))

    val isDebt: Boolean
        get() = !isTransfer && (
            transactionType.equals("DEBT", ignoreCase = true) ||
                transactionType.equals("DEBT_SETTLED", ignoreCase = true) ||
                notes.startsWith("[DEBT]")
            )

    val isReceivable: Boolean
        get() = !isTransfer && (
            transactionType.equals("RECEIVABLE", ignoreCase = true) ||
                transactionType.equals("RECEIVABLE_SETTLED", ignoreCase = true) ||
                notes.startsWith("[RECEIVABLE]")
            )

    val isSettled: Boolean
        get() = transactionType.endsWith("_SETTLED", ignoreCase = true) ||
            notes.contains("[SETTLED]") ||
            ((isDebt || isReceivable) && amount > 0.0 && paidAmount >= amount - 0.001)

    val isExpense: Boolean
        get() = !isIncome && !isDebt && !isReceivable && !isTransfer

    val remainingAmount: Double
        get() = if (isSettled) 0.0 else (amount - paidAmount).coerceAtLeast(0.0)

    val cleanNotes: String
        get() = notes
            .removePrefix("[TRANSFER] ").removePrefix("[TRANSFER]")
            .removePrefix("[INCOME] ").removePrefix("[INCOME]")
            .removePrefix("[DEBT] ").removePrefix("[DEBT]")
            .removePrefix("[RECEIVABLE] ").removePrefix("[RECEIVABLE]")
            .replace("[SETTLED]", "")
            .trim()

    fun toCreateMap(): Map<String, Any> {
        val validCategories = setOf("FOOD", "COFFEE", "TRANSPORT", "SHOPPING", "BILLS", "HEALTH", "OTHER")
        val safeCategory = if (category in validCategories) category else "OTHER"
        val validSources = setOf("MANUAL", "VOICE", "AI_ASSISTANT")
        val safeInputSource = if (inputSource in validSources) inputSource else "MANUAL"
        val prefix = when {
            isTransfer && !notes.startsWith("[TRANSFER]") -> "[TRANSFER]"
            isDebt && !notes.startsWith("[DEBT]") -> "[DEBT]"
            isReceivable && !notes.startsWith("[RECEIVABLE]") -> "[RECEIVABLE]"
            isIncome && !notes.startsWith("[INCOME]") -> "[INCOME]"
            else -> ""
        }
        val settledSuffix = if (isSettled && !notes.contains("[SETTLED]")) " [SETTLED]" else ""
        val baseNotes = notes.take(1960)
        val serializedNotes = buildString {
            if (prefix.isNotEmpty()) {
                append(prefix)
                if (baseNotes.isNotBlank()) append(" ")
            }
            append(baseNotes)
            append(settledSuffix)
        }.trim().take(2000)
        return mapOf(
            "userId" to userId,
            "title" to title.ifBlank { safeCategory }.take(250),
            "category" to safeCategory,
            "amount" to amount,
            "currency" to currency,
            "timestampMillis" to timestampMillis,
            "localEpochDay" to localEpochDay,
            "linkedConsumptionId" to linkedConsumptionId,
            "notes" to serializedNotes,
            "inputSource" to safeInputSource,
            "idempotencyKey" to idempotencyKey,
            "transactionType" to transactionType,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}

@IgnoreExtraProperties
data class Habit(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val category: String = "شخصی",
    val scheduleType: String = "DAILY", // DAILY, WEEKLY_DAYS, TIMES_PER_WEEK
    val scheduleDays: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7), // 1=Mon..7=Sun
    val targetPerPeriod: Int = 1,
    val linkedTaskCategory: String = "",
    val reminderTimeMinutes: Int = -1,
    val archived: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "title" to title,
        "category" to category,
        "scheduleType" to scheduleType,
        "scheduleDays" to scheduleDays.take(7),
        "targetPerPeriod" to targetPerPeriod,
        "linkedTaskCategory" to linkedTaskCategory,
        "reminderTimeMinutes" to reminderTimeMinutes,
        "archived" to archived,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class HabitRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val habitId: String = "",
    val localEpochDay: Long = 0L,
    val completedCount: Int = 1,
    val notes: String = "",
    val inputSource: String = "MANUAL",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "habitId" to habitId,
        "localEpochDay" to localEpochDay,
        "completedCount" to completedCount,
        "notes" to notes,
        "inputSource" to inputSource,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class SleepRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val sleepStartMillis: Long = 0L,
    val wakeUpMillis: Long = 0L,
    val durationMinutes: Int = 0,
    val localEpochDay: Long = 0L,
    val qualityRating: Int = 4, // 1..5
    val notes: String = "",
    val inputSource: String = "MANUAL",
    val idempotencyKey: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "sleepStartMillis" to sleepStartMillis,
        "wakeUpMillis" to wakeUpMillis,
        "durationMinutes" to durationMinutes,
        "localEpochDay" to localEpochDay,
        "qualityRating" to qualityRating.coerceIn(1, 5),
        "notes" to notes,
        "inputSource" to inputSource,
        "idempotencyKey" to idempotencyKey,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class ExerciseRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val activityType: String = "پیاده‌روی",
    val durationMinutes: Int = 30,
    val distanceMeters: Double = 0.0,
    val intensity: String = "MODERATE", // LOW, MODERATE, HIGH
    val repetitions: Int = 0,
    val timestampMillis: Long = 0L,
    val localEpochDay: Long = 0L,
    val notes: String = "",
    val inputSource: String = "MANUAL",
    val idempotencyKey: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "activityType" to activityType,
        "durationMinutes" to durationMinutes,
        "distanceMeters" to distanceMeters,
        "intensity" to intensity,
        "repetitions" to repetitions,
        "timestampMillis" to timestampMillis,
        "localEpochDay" to localEpochDay,
        "notes" to notes,
        "inputSource" to inputSource,
        "idempotencyKey" to idempotencyKey,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class ActivityEvent(
    @DocumentId val id: String = "",
    val userId: String = "",
    val timestampMillis: Long = 0L,
    val eventType: String = "",
    val entityType: String = "",
    val entityId: String = "",
    val inputSource: String = "MANUAL",
    val operationStatus: String = "EXECUTED", // PROPOSED, CONFIRMED, EXECUTED, REJECTED, FAILED
    val verificationSource: String = "CLIENT_RECORDED", // CLIENT_RECORDED vs SERVER_VERIFIED
    val correlationId: String = "",
    val beforeSummary: String = "",
    val afterSummary: String = "",
    val humanSummary: String = "",
    val recognizedTranscript: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "timestampMillis" to timestampMillis,
        "eventType" to eventType,
        "entityType" to entityType,
        "entityId" to entityId,
        "inputSource" to inputSource,
        "operationStatus" to operationStatus,
        "verificationSource" to verificationSource,
        "correlationId" to correlationId,
        "beforeSummary" to beforeSummary,
        "afterSummary" to afterSummary,
        "humanSummary" to humanSummary,
        "recognizedTranscript" to recognizedTranscript,
        "createdAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class ConversationTurn(
    @DocumentId val id: String = "",
    val userId: String = "",
    val role: String = "USER", // USER, ASSISTANT
    val messageText: String = "",
    val structuredActionJson: String = "",
    val timestampMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "role" to role,
        "messageText" to messageText,
        "structuredActionJson" to structuredActionJson,
        "timestampMillis" to timestampMillis,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class UserPreferencesDoc(
    @DocumentId val id: String = "default",
    val userId: String = "",
    val language: String = "fa", // fa, en
    val darkTheme: Boolean = true,
    val weekStartDay: Int = 6, // 6 = Saturday (Persian standard), 1 = Monday
    val calendarDisplay: String = "JALALI", // JALALI, GREGORIAN
    val defaultCurrency: String = "TOMAN", // TOMAN, IRR, EUR, USD, GBP, AED
    val dailyWaterTargetMl: Int = 2500,
    val dailySleepTargetMinutes: Int = 480,
    val dailyExerciseTargetMinutes: Int = 30,
    val retainVoiceTranscripts: Boolean = true,
    val retainConversations: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val onboardingCompleted: Boolean = false,
    val monthlyBudgetTarget: Double = 15000000.0,
    val vaultLockEnabled: Boolean = false,
    val vaultPinCode: String = "",
    val themePalette: String = "OBSIDIAN_TEAL", // OBSIDIAN_TEAL, EMERALD_MATRIX, ROYAL_AMETHYST, AMBER_CYBER, NORDIC_FROST, MINIMAL_DAYLIGHT
    val layoutStyle: String = "BENTO_GRID", // BENTO_GRID, COMPACT_PRO, MINIMAL_ZEN, KANBAN_BOARD
    val dashboardCardOrder: List<String> = listOf("QUICK_BAR", "FINANCE", "GOALS", "HABITS", "WELLNESS", "CALENDAR"),
    val dailyExpenseReminderEnabled: Boolean = true,
    val dailyExpenseReminderHour: Int = 21,
    val dailyExpenseReminderMinute: Int = 0,
    val customExpenseCategories: List<String> = listOf(
        "غذا و کافه", "خرید", "قبوض و اجاره", "حمل‌ونقل", "سلامت و درمان", "تفریح و سرگرمی", "سرمایه‌گذاری", "آموزش", "سایر"
    ),
    val financialAccounts: List<String> = listOf(
        "کارت اصلی", "کارت پس‌انداز", "کیف پول / نقدی"
    ),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "language" to language,
        "darkTheme" to darkTheme,
        "weekStartDay" to weekStartDay,
        "calendarDisplay" to calendarDisplay,
        "defaultCurrency" to defaultCurrency,
        "dailyWaterTargetMl" to dailyWaterTargetMl,
        "dailySleepTargetMinutes" to dailySleepTargetMinutes,
        "dailyExerciseTargetMinutes" to dailyExerciseTargetMinutes,
        "retainVoiceTranscripts" to retainVoiceTranscripts,
        "retainConversations" to retainConversations,
        "notificationsEnabled" to notificationsEnabled,
        "onboardingCompleted" to onboardingCompleted,
        "dailyExpenseReminderEnabled" to dailyExpenseReminderEnabled,
        "dailyExpenseReminderHour" to dailyExpenseReminderHour,
        "dailyExpenseReminderMinute" to dailyExpenseReminderMinute,
        "customExpenseCategories" to customExpenseCategories,
        "financialAccounts" to financialAccounts,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}
