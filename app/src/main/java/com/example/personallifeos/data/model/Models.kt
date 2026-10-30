package com.example.personallifeos.data.model

enum class TaskCategory(val titleFa: String) {
    WORK("کاری"),
    PERSONAL("شخصی"),
    FINANCE("مالی"),
    HEALTH("سلامت"),
    STUDY("مطالعه")
}

enum class TaskPriority(val titleFa: String) {
    LOW("پایین"),
    MEDIUM("متوسط"),
    HIGH("بالا"),
    URGENT("فوری")
}

enum class TransactionType(val titleFa: String) {
    EXPENSE("هزینه"),
    INCOME("درآمد"),
    TRANSFER("انتقال بین حساب‌ها")
}

enum class DebtType(val titleFa: String) {
    DEBT("بدهی من به دیگران"),
    RECEIVABLE("طلب من از دیگران")
}

data class TaskItem(
    val id: String,
    val title: String,
    val category: TaskCategory,
    val priority: TaskPriority,
    val completed: Boolean = false,
    val dueDate: String,
    val createdAt: String,
    val linkedDebtId: String? = null
)

data class GoalItem(
    val id: String,
    val title: String,
    val description: String,
    val targetDate: String,
    val progressPercent: Int,
    val nextAction: String,
    val category: TaskCategory = TaskCategory.PERSONAL,
    val targetBudgetToman: Long? = null
)

data class HabitItem(
    val id: String,
    val title: String,
    val streak: Int,
    val completedToday: Boolean = false,
    val category: TaskCategory = TaskCategory.HEALTH
)

data class ExpenseItem(
    val id: String,
    val title: String,
    val amount: Long, // in Tomans
    val category: String,
    val date: String,
    val type: TransactionType = TransactionType.EXPENSE,
    val isSmsParsed: Boolean = false,
    val bankName: String? = null,
    val accountName: String? = "حساب پیش‌فرض",
    val idempotencyKey: String? = null
)

data class DebtItem(
    val id: String,
    val title: String,
    val type: DebtType,
    val totalAmount: Long, // in Tomans
    val paidAmount: Long = 0,
    val dueDate: String,
    val isSettled: Boolean = false,
    val personName: String
) {
    val remainingAmount: Long get() = maxOf(0L, totalAmount - paidAmount)
}

data class QuickPreset(
    val id: String,
    val title: String,
    val defaultAmount: Long,
    val category: String,
    val iconEmoji: String
)

data class CategoryBudget(
    val category: String,
    val monthlyLimitToman: Long
)

enum class ReminderEntityType(val titleFa: String) {
    TASK("وظیفه"),
    DEBT("قسط و بدهی"),
    HABIT("عادت روزانه"),
    GOAL("هدف"),
    DAILY_EXPENSE("ثبت مخارج روزانه")
}

data class ReminderItem(
    val id: String,
    val entityType: ReminderEntityType,
    val entityId: String,
    val title: String,
    val triggerTime: String,
    val isSnoozed: Boolean = false,
    val isCompleted: Boolean = false,
    val sensitiveAmountToman: Long? = null
)

data class HealthLog(
    val id: String,
    val date: String,
    val sleepHours: Float,
    val sleepQuality: String, // 'poor' | 'fair' | 'good' | 'excellent'
    val waterGlasses: Int,
    val exerciseMinutes: Int,
    val exerciseType: String,
    val caloriesBurned: Int,
    val dailyCalorieIntake: Int = 1850,
    val lastScannedFoodSummary: String? = null
)

data class VaultNote(
    val id: String,
    val title: String,
    val content: String,
    val category: String,
    val updatedAt: String,
    val isEncrypted: Boolean = true
)

data class AiActionDraft(
    val actionType: String, // "EXPENSE", "INCOME", "TRANSFER", "DEBT", "RECEIVABLE", "TASK"
    val title: String,
    val amountToman: Long = 0L,
    val category: String = "خرید عمومی",
    val personOrAccount: String? = null,
    val isExecuted: Boolean = false,
    val isCancelled: Boolean = false
)

data class ChatMessage(
    val id: String,
    val sender: String, // "user" or "gemini"
    val text: String,
    val timestamp: String,
    val requiresConfirmation: Boolean = false,
    val isClarification: Boolean = false,
    val actionDraft: AiActionDraft? = null
)
