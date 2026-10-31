package com.example.personallifeos.data.model

enum class TaskCategory(val titleFa: String) {
    WORK("کاری"),
    PERSONAL("شخصی"),
    FINANCE("مالی"),
    HEALTH("سلامت"),
    STUDY("مطالعه"),
    GROWTH("رشد و یادگیری")
}

enum class TaskPriority(val titleFa: String, val weight: Int) {
    LOW("کم", 1),
    MEDIUM("متوسط", 2),
    HIGH("زیاد", 3),
    URGENT("فوری", 4)
}

enum class TaskStatus(val titleFa: String) {
    TODO("انجام نشده"),
    IN_PROGRESS("در حال انجام"),
    COMPLETED("انجام شده"),
    CANCELLED("لغو شده")
}

enum class ReminderRepeatMode(val titleFa: String) {
    ONCE("یک‌بار"),
    DAILY("روزانه"),
    WEEKLY("هفتگی"),
    MONTHLY("ماهانه")
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

data class SubtaskItem(
    val id: String,
    val title: String,
    val completed: Boolean = false
) {
    val isCompleted: Boolean get() = completed
}

data class TaskItem(
    val id: String,
    val title: String,
    val category: TaskCategory,
    val priority: TaskPriority,
    val completed: Boolean = false,
    val dueDate: String,
    val createdAt: String = "۱۴۰۵/۰۸/۰۱",
    val linkedDebtId: String? = null,
    val topicId: String? = null,
    val description: String = "",
    val status: TaskStatus = if (completed) TaskStatus.COMPLETED else TaskStatus.TODO,
    val subtasks: List<SubtaskItem> = emptyList(),
    val reminderTime: String? = null,
    val reminderEpochMillis: Long? = null,
    val reminderRepeatMode: ReminderRepeatMode = ReminderRepeatMode.ONCE,
    val reminderBeforeMinutes: Int = 0,
    val isMajorMission: Boolean = false,
    val completedAt: String? = null,
    val activityLog: List<String> = emptyList()
) {
    val reminderDateTime: String? get() = reminderTime
}

data class TopicItem(
    val id: String,
    val title: String,
    val description: String,
    val iconEmoji: String = "SVG_TARGET",
    val colorHex: Long = 0xFF06B6D4,
    val startDate: String = "۱۴۰۵/۰۸/۰۱",
    val endDate: String? = null,
    val priority: TaskPriority = TaskPriority.HIGH,
    val tagOrCategory: String = "عمومی",
    val isActive: Boolean = true,
    val isArchived: Boolean = false,
    val reminderEnabled: Boolean = true,
    val reminderTime: String? = null,
    val topicNote: String = "",
    val createdAt: String = "۱۴۰۵/۰۸/۰۱"
) {
    val name: String get() = title
    val categoryTag: String get() = tagOrCategory
}

data class TopicDashboardStats(
    val topicId: String,
    val totalTasks: Int,
    val completedTasks: Int,
    val remainingTasks: Int,
    val inProgressTasks: Int,
    val cancelledTasks: Int,
    val overdueTasks: Int,
    val progressPercent: Int,
    val nearestDeadline: String?,
    val urgentOrMajorTasks: List<TaskItem>
) {
    companion object {
        /**
         * Computes real-time topic progress strictly from the topic's actual tasks.
         * Example: 6 completed out of 10 active tasks = 60%.
         */
        fun compute(topic: TopicItem, allTasks: List<TaskItem>, todayJalali: String = "۱۴۰۵/۰۸/۰۵"): TopicDashboardStats {
            return compute(topic.id, allTasks, todayJalali)
        }

        fun compute(topicId: String, allTasks: List<TaskItem>, todayJalali: String = "۱۴۰۵/۰۸/۰۵"): TopicDashboardStats {
            val topicTasks = allTasks.filter { it.topicId == topicId }
            val activePool = topicTasks.filter { it.status != TaskStatus.CANCELLED }
            val total = activePool.size
            val completed = activePool.count { it.completed || it.status == TaskStatus.COMPLETED }
            val remaining = (total - completed).coerceAtLeast(0)
            val inProgress = activePool.count { !it.completed && it.status == TaskStatus.IN_PROGRESS }
            val cancelled = topicTasks.count { it.status == TaskStatus.CANCELLED }

            val normalizedToday = normalizeJalaliForCompare(todayJalali)
            val incompleteTasks = activePool.filter { !it.completed && it.status != TaskStatus.COMPLETED }
            val overdue = incompleteTasks.count { task ->
                val dueNorm = normalizeJalaliForCompare(task.dueDate)
                dueNorm.isNotEmpty() && normalizedToday.isNotEmpty() && dueNorm < normalizedToday
            }
            val nearest = incompleteTasks
                .map { it.dueDate }
                .filter { it.isNotBlank() }
                .minByOrNull { normalizeJalaliForCompare(it) }

            val urgentOrMajor = incompleteTasks.filter {
                it.isMajorMission || it.priority == TaskPriority.URGENT || it.priority == TaskPriority.HIGH
            }.sortedByDescending { it.priority.weight }

            val percent = if (total == 0) 0 else ((completed * 100) / total).coerceIn(0, 100)

            return TopicDashboardStats(
                topicId = topicId,
                totalTasks = total,
                completedTasks = completed,
                remainingTasks = remaining,
                inProgressTasks = inProgress,
                cancelledTasks = cancelled,
                overdueTasks = overdue,
                progressPercent = percent,
                nearestDeadline = nearest,
                urgentOrMajorTasks = urgentOrMajor
            )
        }

        private fun normalizeJalaliForCompare(input: String): String {
            val fa = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
            var res = input.trim()
            for (i in 0..9) {
                res = res.replace(fa[i], ('0' + i))
            }
            return res
        }
    }
}

data class TopicPresetTemplate(
    val title: String,
    val description: String,
    val iconEmoji: String,
    val colorHex: Long,
    val tagOrCategory: String,
    val priority: TaskPriority,
    val defaultMissions: List<String>
) {
    val name: String get() = title
    val categoryTag: String get() = tagOrCategory
}

val DEFAULT_TOPIC_PRESET_TEMPLATES = listOf(
    TopicPresetTemplate(
        title = "دانشگاه و پژوهش",
        description = "مدیریت درس‌ها، پروژه‌های دانشگاهی، مقالات و امتحانات ترم",
        iconEmoji = "SVG_UNIVERSITY",
        colorHex = 0xFF06B6D4,
        tagOrCategory = "تحصیلی و دانشگاه",
        priority = TaskPriority.HIGH,
        defaultMissions = listOf(
            "تحویل فاز اول پروژه مهندسی نرم‌افزار",
            "مرور جزوات و خلاصه‌نویسی فصل ۳",
            "آماده‌سازی ارائه سمینار پژوهشی"
        )
    ),
    TopicPresetTemplate(
        title = "پروژه‌های فیلم‌برداری و تولید محتوا",
        description = "برنامه‌ریزی پیش‌تولید، آفیش، فیلم‌برداری، تدوین و اصلاح رنگ",
        iconEmoji = "SVG_CINEMA",
        colorHex = 0xFFF59E0B,
        tagOrCategory = "تولید و رسانه",
        priority = TaskPriority.URGENT,
        defaultMissions = listOf(
            "تکمیل دکوپاژ و استوری‌بورد پروژه جدید",
            "هماهنگی تجهیزات نور، صدا و دوربین",
            "تدوین راف‌کات و خروجی نهایی۴K"
        )
    ),
    TopicPresetTemplate(
        title = "کار و کسب‌وکار",
        description = "مدیریت پروژه‌های کاری، جلسات، قراردادها و توسعه محصول",
        iconEmoji = "SVG_BUSINESS",
        colorHex = 0xFF10B981,
        tagOrCategory = "شغلی و حرفه‌ای",
        priority = TaskPriority.HIGH,
        defaultMissions = listOf(
            "بررسی گزارش عملکرد و شاخص‌های کلیدی (KPI)",
            "پیگیری قراردادها و صورت‌حساب‌های ماهانه"
        )
    ),
    TopicPresetTemplate(
        title = "زندگی شخصی و خانه",
        description = "برنامه‌های خانوادگی، خرید، تعمیرات و کارهای روزمره شخصی",
        iconEmoji = "SVG_HOME",
        colorHex = 0xFFEC4899,
        tagOrCategory = "شخصی",
        priority = TaskPriority.MEDIUM,
        defaultMissions = listOf(
            "سرویس دوره‌ای خودرو و تمدید بیمه",
            "برنامه‌ریزی سفر آخر هفته"
        )
    ),
    TopicPresetTemplate(
        title = "یادگیری و مطالعه عمیق",
        description = "مطالعه کتاب‌های تخصصی، دوره‌های آموزشی و تمرین مهارت جدید",
        iconEmoji = "SVG_STUDY",
        colorHex = 0xFF8B5CF6,
        tagOrCategory = "رشد فردی",
        priority = TaskPriority.MEDIUM,
        defaultMissions = listOf(
            "مطالعه روزانه ۳۰ صفحه کتاب معماری سیستم‌ها",
            "پیاده‌سازی تمرین‌های عملی دوره"
        )
    ),
    TopicPresetTemplate(
        title = "اهداف بلندمدت و استراتژیک",
        description = "مأموریت‌های کلان سالانه، سرمایه‌گذاری و نقشه راه آینده",
        iconEmoji = "SVG_TARGET",
        colorHex = 0xFF3B82F6,
        tagOrCategory = "استراتژیک",
        priority = TaskPriority.HIGH,
        defaultMissions = listOf(
            "تکمیل صندوق ذخیره اضطراری ۶ ماهه",
            "ارزیابی فصلی اهداف و بازنگری مسیر"
        )
    )
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
    TASK("وظیفه و مأموریت"),
    TOPIC("تاپیک موضوعی"),
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
    val sensitiveAmountToman: Long? = null,
    val topicId: String? = null,
    val topicTitle: String? = null,
    val repeatMode: ReminderRepeatMode = ReminderRepeatMode.ONCE,
    val snoozeIntervalMinutes: Int = 60,
    val triggerEpochMillis: Long? = null,
    val isEnabled: Boolean = true
) {
    val topicName: String? get() = topicTitle
}

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
