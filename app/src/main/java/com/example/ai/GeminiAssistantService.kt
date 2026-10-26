package com.example.ai

import com.example.BuildConfig
import com.example.data.model.ActivityEvent
import com.example.data.model.ConsumptionEntry
import com.example.data.model.ExpenseEntry
import com.example.data.model.ExerciseRecord
import com.example.data.model.Habit
import com.example.data.model.HabitRecord
import com.example.data.model.SleepRecord
import com.example.data.model.Task
import com.example.util.PersianDateAndUnitUtils
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Controlled set of typed application operations that Gemini can propose.
 * Never grants unrestricted database access.
 */
data class ProposedAiOperation(
    val operationType: String, // CreateTask, UpdateTask, CompleteTask, AddConsumptionEntry, AddExpense, AddSleepRecord, AddExerciseRecord, GetTasks, GetConsumptionSummary, GetExpenseSummary, GetHabitSummary, GetActivityHistory, GenerateDailySummary, AskClarification
    val title: String = "",
    val category: String = "",
    val quantity: Double = 0.0,
    val unit: String = "",
    val amount: Double = 0.0,
    val currency: String = "",
    val transactionType: String = "EXPENSE", // EXPENSE, INCOME, DEBT, RECEIVABLE
    val notes: String = "",
    val durationMinutes: Int = 0,
    val dueDateOffsetDays: Int = 0,
    val dueTimeMinutes: Int = -1,
    val priority: String = "MEDIUM",
    val recurrenceRule: String = "NONE",
    val qualityRating: Int = 4,
    val intensity: String = "MODERATE",
    val targetTaskTitle: String = "",
    val requiresConfirmation: Boolean = true,
    val clarificationQuestion: String = "",
    val humanReadablePreview: String = ""
)

data class AiParseResult(
    val conversationalReply: String,
    val operations: List<ProposedAiOperation>,
    val isAmbiguous: Boolean = false,
    val clarificationPrompt: String = "",
    val sourceModel: String = "Firebase AI Logic (gemini-3.5-flash)"
)

object GeminiAssistantService {

    private const val MODEL_NAME = "gemini-3.5-flash"

    /**
     * Validates a proposed AI operation to ensure no silently invented negative quantities,
     * missing units, or invalid currencies/times are accepted.
     */
    fun validateOperation(op: ProposedAiOperation): Result<ProposedAiOperation> {
        return when (op.operationType) {
            "CreateTask" -> {
                if (op.title.isBlank()) {
                    Result.failure(IllegalArgumentException("عنوان تسک مشخص نشده است."))
                } else {
                    Result.success(op)
                }
            }
            "CompleteTask", "UpdateTask" -> {
                if (op.targetTaskTitle.isBlank() && op.title.isBlank()) {
                    Result.failure(IllegalArgumentException("نام تسک مورد نظر مشخص نیست. لطفاً دقیق‌تر بفرمایید کدام تسک؟"))
                } else {
                    Result.success(op.copy(requiresConfirmation = true))
                }
            }
            "AddConsumptionEntry" -> {
                if (op.quantity <= 0.0 || op.unit.isBlank()) {
                    Result.failure(IllegalArgumentException("مقدار یا واحد مصرف مشخص نیست."))
                } else {
                    Result.success(op)
                }
            }
            "AddExpense" -> {
                if (op.amount <= 0.0 || op.currency.isBlank()) {
                    Result.failure(IllegalArgumentException("مبلغ یا واحد پول مشخص نیست."))
                } else {
                    Result.success(op.copy(requiresConfirmation = true))
                }
            }
            "AddSleepRecord" -> {
                if (op.durationMinutes <= 0 || op.durationMinutes > 1440) {
                    Result.failure(IllegalArgumentException("مدت زمان خواب نامعتبر است."))
                } else {
                    Result.success(op)
                }
            }
            "AddExerciseRecord" -> {
                if (op.durationMinutes <= 0 || op.durationMinutes > 1440) {
                    Result.failure(IllegalArgumentException("مدت زمان ورزش نامعتبر است."))
                } else {
                    Result.success(op)
                }
            }
            "AskClarification" -> Result.success(op)
            "GetTasks", "GetConsumptionSummary", "GetExpenseSummary", "GetHabitSummary", "GetActivityHistory", "GenerateDailySummary" -> {
                Result.success(op.copy(requiresConfirmation = false))
            }
            else -> Result.failure(IllegalArgumentException("عملیات ناشناخته: ${op.operationType}"))
        }
    }

    /**
     * Parses a Persian or English voice/chat command using real context from stored records.
     * Uses Firebase AI Logic first, falls back to Gemini REST API if needed, and includes
     * a high-precision deterministic Persian NLU parser so voice capture and tests work offline too.
     */
    suspend fun processUserCommandOrQuery(
        userInput: String,
        todayEpochDay: Long,
        defaultCurrency: String,
        tasks: List<Task>,
        consumption: List<ConsumptionEntry>,
        expenses: List<ExpenseEntry>,
        habits: List<Habit>,
        habitRecords: List<HabitRecord>,
        sleepRecords: List<SleepRecord>,
        exerciseRecords: List<ExerciseRecord>,
        recentEvents: List<ActivityEvent>
    ): AiParseResult = withContext(Dispatchers.IO) {
        val cleanInput = userInput.trim()
        if (cleanInput.isEmpty()) {
            return@withContext AiParseResult(
                conversationalReply = "لطفاً دستور صوتی یا متنی خود را وارد کنید.",
                operations = emptyList(),
                isAmbiguous = true,
                clarificationPrompt = "چه کاری برایتان انجام دهم؟"
            )
        }

        // First run deterministic Persian parser to extract structured operations accurately
        val deterministicResult = parsePersianCommandDeterministic(
            rawText = cleanInput,
            todayEpochDay = todayEpochDay,
            defaultCurrency = defaultCurrency,
            tasks = tasks,
            consumption = consumption,
            expenses = expenses,
            habits = habits,
            habitRecords = habitRecords,
            sleepRecords = sleepRecords,
            exerciseRecords = exerciseRecords,
            recentEvents = recentEvents
        )

        // Build factual context snapshot for Gemini
        val contextSnapshot = buildFactualContextPrompt(
            todayEpochDay = todayEpochDay,
            tasks = tasks,
            consumption = consumption,
            expenses = expenses,
            habits = habits,
            habitRecords = habitRecords,
            sleepRecords = sleepRecords,
            exerciseRecords = exerciseRecords,
            recentEvents = recentEvents
        )

        val systemPrompt = """
شما دستیار هوشمند «Personal Life OS» هستید. زبان اصلی شما فارسی است.
قوانین حیاتی:
۱. فقط بر اساس داده‌های واقعی ذخیره‌شده در بخش CONTEXT به سوالات تاریخی پاسخ دهید. هرگز عدد یا رکورد خیالی نسازید.
۲. حقایق محاسبه‌شده (Facts) را از پیشنهادهای هوشمند (Suggestions) جدا کنید.
۳. اگر داده کافی وجود ندارد، به صراحت اعلام کنید.
۴. پاسخ را کوتاه، دقیق، محترمانه و کاربردی به زبان فارسی بنویسید.

CONTEXT (داده‌های واقعی کاربر در دیتابیس):
$contextSnapshot

ورودی کاربر:
$cleanInput
""".trimIndent()

        // Try Firebase AI Logic first (as provisioned via Firebase App Check + AI Logic)
        val aiText = tryFirebaseAiLogic(systemPrompt) ?: tryGeminiRestFallback(systemPrompt)

        if (!aiText.isNullOrBlank()) {
            deterministicResult.copy(
                conversationalReply = aiText.trim(),
                sourceModel = "Firebase AI Logic ($MODEL_NAME)"
            )
        } else {
            deterministicResult
        }
    }

    private suspend fun tryFirebaseAiLogic(prompt: String): String? {
        return try {
            val model = Firebase.ai(backend = GenerativeBackend.googleAI())
                .generativeModel(MODEL_NAME)
            val response = model.generateContent(prompt)
            response.text?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    private fun tryGeminiRestFallback(prompt: String): String? {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") return null

        return try {
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent?key=$apiKey")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 20_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
            val reqJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        })
                    })
                })
            }
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(reqJson.toString()) }
            if (conn.responseCode in 200..299) {
                val respStr = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
                val root = JSONObject(respStr)
                root.optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text")
                    ?.takeIf { it.isNotBlank() }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun buildFactualContextPrompt(
        todayEpochDay: Long,
        tasks: List<Task>,
        consumption: List<ConsumptionEntry>,
        expenses: List<ExpenseEntry>,
        habits: List<Habit>,
        habitRecords: List<HabitRecord>,
        sleepRecords: List<SleepRecord>,
        exerciseRecords: List<ExerciseRecord>,
        recentEvents: List<ActivityEvent>
    ): String {
        val todayTasks = tasks.filter { it.dueDateEpochDay == todayEpochDay && it.status != "ARCHIVED" }
        val pendingToday = todayTasks.filter { it.status != "COMPLETED" }
        val completedYesterday = tasks.filter {
            it.status == "COMPLETED" && it.dueDateEpochDay == todayEpochDay - 1L
        }
        val overdueTasks = tasks.filter {
            it.status != "COMPLETED" && it.status != "ARCHIVED" && it.dueDateEpochDay in 0 until todayEpochDay
        }
        val todayWaterMl = PersianDateAndUnitUtils.sumCompatibleConsumption(
            consumption.filter { it.localEpochDay == todayEpochDay && it.category == "WATER" },
            "ml"
        )
        val weekCoffees = consumption.filter {
            it.localEpochDay in (todayEpochDay - 6)..todayEpochDay && it.category == "COFFEE"
        }.sumOf { it.quantity }

        val monthExpenses = PersianDateAndUnitUtils.aggregateExpensesByCurrency(
            expenses.filter { it.localEpochDay in (todayEpochDay - 29)..todayEpochDay }
        )
        val lastSleep = sleepRecords.firstOrNull()
        val todayExerciseMin = exerciseRecords.filter { it.localEpochDay == todayEpochDay }.sumOf { it.durationMinutes }
        val completedHabitIdsToday = habitRecords.filter { it.localEpochDay == todayEpochDay }.map { it.habitId }.toSet()
        val missedHabitsToday = habits.filter { !it.archived && it.id !in completedHabitIdsToday }

        return buildString {
            appendLine("- تاریخ امروز: ${PersianDateAndUnitUtils.formatEpochDay(todayEpochDay)}")
            appendLine("- تسک‌های باقی‌مانده امروز (${pendingToday.size}): ${pendingToday.joinToString("، ") { it.title }.ifBlank { "هیچ" }}")
            appendLine("- تسک‌های انجام‌شده دیروز (${completedYesterday.size}): ${completedYesterday.joinToString("، ") { it.title }.ifBlank { "هیچ" }}")
            appendLine("- تسک‌های عقب‌افتاده (${overdueTasks.size}): ${overdueTasks.joinToString("، ") { it.title }.ifBlank { "هیچ" }}")
            appendLine("- آب ثبت‌شده امروز: ${todayWaterMl.toInt()} میلی‌لیتر")
            appendLine("- قهوه مصرفی ۷ روز اخیر: $weekCoffees واحد")
            appendLine("- هزینه‌های ۳۰ روز اخیر به تفکیک ارز: ${monthExpenses.entries.joinToString(" | ") { "${it.value} ${it.key}" }.ifBlank { "ثبت نشده" }}")
            appendLine("- آخرین خواب ثبت‌شده: ${if (lastSleep != null) "${lastSleep.durationMinutes / 60} ساعت و ${lastSleep.durationMinutes % 60} دقیقه" else "ثبت نشده"}")
            appendLine("- ورزش امروز: $todayExerciseMin دقیقه")
            appendLine("- عادت‌های انجام‌نشده امروز: ${missedHabitsToday.joinToString("، ") { it.title }.ifBlank { "همه انجام شده‌اند" }}")
            appendLine("- آخرین تغییرات ثبت‌شده امروز (${recentEvents.take(5).size}): ${recentEvents.take(5).joinToString("؛ ") { it.humanSummary }}")
        }
    }

    /**
     * Parses Persian & English conversational commands into typed, validated operations.
     * Supports multi-intent commands separated by «و» (e.g., "فردا ساعت ۹ قبض رو پرداخت کنم و نیم ساعت ورزش کنم.")
     * and factual historical queries.
     */
    fun parsePersianCommandDeterministic(
        rawText: String,
        todayEpochDay: Long,
        defaultCurrency: String = "TOMAN",
        tasks: List<Task> = emptyList(),
        consumption: List<ConsumptionEntry> = emptyList(),
        expenses: List<ExpenseEntry> = emptyList(),
        habits: List<Habit> = emptyList(),
        habitRecords: List<HabitRecord> = emptyList(),
        sleepRecords: List<SleepRecord> = emptyList(),
        exerciseRecords: List<ExerciseRecord> = emptyList(),
        recentEvents: List<ActivityEvent> = emptyList()
    ): AiParseResult {
        val norm = PersianDateAndUnitUtils.toEnglishDigits(rawText)
            .replace("میلیلیتر", "میلی‌لیتر")
            .replace("پیادهروی", "پیاده‌روی")
            .replace("برنامهریزی", "برنامه‌ریزی")
            .replace("انجامشده", "انجام‌شده")
            .trim()

        // 1. Check if it's a Question / Historical / Analytical Query first
        if (norm.contains("؟") || norm.contains("?") ||
            norm.startsWith("چه ") || norm.startsWith("چقدر ") || norm.startsWith("چند ") ||
            norm.contains("باقی مونده") || norm.contains("باقی مانده") ||
            norm.contains("دیروز چی") || norm.contains("خلاصه") ||
            norm.contains("تغییرات") || norm.contains("کدام عادت") ||
            norm.contains("بیشترین هزینه") || norm.contains("بیشترین خرج") ||
            norm.contains("وضعیت بدهی") || norm.contains("برنامه بازپرداخت") ||
            norm.contains("روند عادت")
        ) {
            return answerHistoricalQueryFactual(
                norm = norm,
                todayEpochDay = todayEpochDay,
                tasks = tasks,
                consumption = consumption,
                expenses = expenses,
                habits = habits,
                habitRecords = habitRecords,
                sleepRecords = sleepRecords,
                exerciseRecords = exerciseRecords,
                recentEvents = recentEvents
            )
        }

        // 1B. Check for ambiguous sensitive/destructive debt or expense commands without amount/target
        if ((norm.contains("تسویه بدهی") || norm.contains("حذف بدهی") || norm.contains("پرداخت قسط")) &&
            !Regex("\\d+").containsMatchIn(norm)
        ) {
            val unsettled = expenses.filter { it.isDebt && !it.isSettled }
            val debtHint = if (unsettled.isNotEmpty()) {
                "بدهی‌های باز شما: ${unsettled.take(3).joinToString("، ") { "«${it.title}» (${PersianDateAndUnitUtils.formatCurrencyAmount(it.remainingAmount, it.currency)})" }}"
            } else {
                "در حال حاضر بدهی بازی ثبت نشده است."
            }
            val op = ProposedAiOperation(
                operationType = "AskClarification",
                requiresConfirmation = true,
                clarificationQuestion = "لطفاً مشخص فرمایید کدام بدهی یا قسط و به چه مبلغی مد نظر شماست؟ ($debtHint)",
                humanReadablePreview = "نیاز به شفاف‌سازی و تأیید عملیات حساس مالی"
            )
            return AiParseResult(
                conversationalReply = op.clarificationQuestion,
                operations = listOf(op),
                isAmbiguous = true,
                clarificationPrompt = op.clarificationQuestion
            )
        }

        // 2. Check for ambiguous task completion ("این تسک رو انجام‌شده کن")
        if ((norm.contains("این تسک") || norm.contains("تسک رو")) &&
            (norm.contains("انجام") || norm.contains("تکمیل") || norm.contains("تمام"))
        ) {
            val pending = tasks.filter { it.status != "COMPLETED" && it.status != "ARCHIVED" }
            val hint = if (pending.isNotEmpty()) {
                "تسک‌های فعال شما: ${pending.take(3).joinToString("، ") { "«${it.title}»" }}"
            } else {
                "در حال حاضر تسک فعالی در لیست وجود ندارد."
            }
            val op = ProposedAiOperation(
                operationType = "AskClarification",
                requiresConfirmation = true,
                clarificationQuestion = "منظورتان دقیقاً کدام تسک است؟ لطفاً عنوان تسک را مشخص فرمایید. ($hint)",
                humanReadablePreview = "نیاز به شفاف‌سازی: انتخاب تسک جهت تکمیل"
            )
            return AiParseResult(
                conversationalReply = op.clarificationQuestion,
                operations = listOf(op),
                isAmbiguous = true,
                clarificationPrompt = op.clarificationQuestion
            )
        }

        // 3. Check for multi-task planning command ("برای فردا سه تا کار مهم برنامه‌ریزی کن")
        if (norm.contains("سه تا کار") || (norm.contains("برنامه‌ریزی کن") && !norm.contains("قبض"))) {
            val offset = if (norm.contains("فردا")) 1 else 0
            val op = ProposedAiOperation(
                operationType = "AskClarification",
                dueDateOffsetDays = offset,
                requiresConfirmation = true,
                clarificationQuestion = "برای ثبت ۳ کار مهم ${if (offset == 1) "فردا" else "امروز"}، لطفاً عناوین مورد نظر خود را مشخص یا در پیش‌نمایش زیر ویرایش کنید.",
                humanReadablePreview = "پیشنهاد برنامه‌ریزی ۳ اولویت اصلی برای ${if (offset == 1) "فردا" else "امروز"}"
            )
            return AiParseResult(
                conversationalReply = op.clarificationQuestion,
                operations = listOf(op),
                isAmbiguous = true,
                clarificationPrompt = op.clarificationQuestion
            )
        }

        // 4. Split compound commands joined by " و "
        val clauses = splitCompoundClauses(norm)
        val operations = mutableListOf<ProposedAiOperation>()
        val sharedDateOffset = PersianDateAndUnitUtils.extractRelativeDateOffsetDays(norm, todayEpochDay)

        for (clause in clauses) {
            val op = parseSingleClause(clause.trim(), sharedDateOffset, defaultCurrency, todayEpochDay)
            if (op != null) {
                operations.add(op)
            }
        }

        if (operations.isEmpty()) {
            // If no specific consumption/expense/sleep/exercise matched, treat as a task if actionable, else ask clarification
            if (norm.length >= 3) {
                val dueTime = extractTimeMinutes(norm)
                val op = ProposedAiOperation(
                    operationType = "CreateTask",
                    title = rawText.trim(),
                    category = "عمومی",
                    dueDateOffsetDays = sharedDateOffset,
                    dueTimeMinutes = dueTime,
                    priority = if (norm.contains("مهم") || norm.contains("فوری")) "HIGH" else "MEDIUM",
                    requiresConfirmation = true,
                    humanReadablePreview = "ایجاد تسک: «${rawText.trim()}»"
                )
                operations.add(op)
            }
        }

        val summaryText = if (operations.size > 1) {
            "تعداد ${PersianDateAndUnitUtils.toPersianDigits(operations.size.toString())} عملیات مجزا شناسایی شد. لطفاً پیش‌نمایش را بررسی و تأیید کنید:"
        } else if (operations.size == 1) {
            val first = operations.first()
            if (first.operationType == "AskClarification") {
                first.clarificationQuestion
            } else {
                "عملیات شناسایی شد: ${first.humanReadablePreview}. لطفاً پیش از ذخیره تأیید فرمایید."
            }
        } else {
            "دستور شما دریافت شد اما برای ثبت دقیق نیاز به جزئیات بیشتری دارد."
        }

        return AiParseResult(
            conversationalReply = summaryText,
            operations = operations,
            isAmbiguous = operations.any { it.operationType == "AskClarification" },
            clarificationPrompt = operations.firstOrNull { it.operationType == "AskClarification" }?.clarificationQuestion ?: ""
        )
    }

    /**
     * Lightweight instant financial parser for the Finance Quick-Entry Form:
     * When the user types or dictates a phrase like "۶۰۰ هزار تومان بابت فروش جمینای گرفتم"
     * or "۳۰۰ هزار تومن بابت اشتراک پرداخت کردم", returns the detected financial operation
     * (transactionType, amount, currency, cleaned title, category, notes).
     */
    fun detectInstantFinancialSuggestion(
        rawText: String,
        defaultCurrency: String = "TOMAN",
        todayEpochDay: Long = PersianDateAndUnitUtils.currentLocalEpochDay()
    ): ProposedAiOperation? {
        val norm = PersianDateAndUnitUtils.toEnglishDigits(rawText)
            .replace('ي', 'ی')
            .replace('ك', 'ک')
            .trim()
        if (norm.length < 3) return null
        val op = parseSingleClause(
            clause = norm,
            inheritedDateOffset = PersianDateAndUnitUtils.extractRelativeDateOffsetDays(norm, todayEpochDay),
            defaultCurrency = defaultCurrency,
            todayEpochDay = todayEpochDay
        )
        return op?.takeIf { it.operationType == "AddExpense" && it.amount > 0.0 }
    }

    private fun splitCompoundClauses(norm: String): List<String> {
        // Protect compound numbers like "2 میلیون و 500" or "صد و پنجاه" from splitting on " و "
        if (Regex("""(?:میلیون|ملیون|صد|بیست|سی|چهل|پنجاه|شصت|هفتاد|هشتاد|نود|یک|دو)\s+و\s+(?:\d+|نیم|پانصد|پونصد|دویست|سیصد|چهارصد|ششصد|هفتصد|هشتصد|نهصد|پنجاه|پنج)""").containsMatchIn(norm)) {
            return listOf(norm)
        }
        val parts = norm.split(Regex("\\s+و\\s+"))
        if (parts.size <= 1) return listOf(norm)
        // Only split if each part looks like an independent action with its own verb/action keyword
        val actionKeywords = listOf(
            "خوردم", "نوشیدم", "خریدم", "خوابیدم", "ورزش", "پیاده‌روی", "پرداخت", "دریافت",
            "کنم", "مصرف", "هزینه", "بدوم", "مطالعه", "تسک", "بذار", "ثبت",
            "بدهکارم", "طلبکارم", "طلب دارم", "قرض", "گرفتم", "دادم", "واریز", "انتقال", "فروختم"
        )
        val validSplit = parts.all { part -> actionKeywords.any { kw -> part.contains(kw) } }
        return if (validSplit) parts else listOf(norm)
    }

    private fun cleanFinancialTitle(
        rawClause: String,
        fallbackLabel: String
    ): String {
        val cleaned = rawClause
            .replace(Regex("""\d+(?:\.\d+)?"""), "")
            .replace(Regex("""\b(یک و نیم|دو و نیم|نیم|نهصد|هشتصد|هفتصد|ششصد|شیشصد|پانصد|پونصد|چهارصد|چارصد|سیصد|دویست|یکصد|صد|نود|هشتاد|هفتاد|شصت|پنجاه|چهل|سی|بیست|پانزده|پونزده|دوازده|یازده|ده|نه|هشت|هفت|شش|شیش|پنج|چهار|سه|دو|یک|یه)\b"""), "")
            .replace(Regex("""\b(میلیارد|میلیون|ملیون|هزار|تومان|تومن|ریال|دلار|یورو|درهم|پوند)\b"""), "")
            .replace("دریافت کردم", "")
            .replace("دریافت شد", "")
            .replace("پرداخت کردم", "")
            .replace("پرداخت شد", "")
            .replace("انتقال دادم", "")
            .replace("منتقل کردم", "")
            .replace("کارت به کارت کردم", "")
            .replace("واریز شد", "")
            .replace("واریز کرد", "")
            .replace("به حسابم نشست", "")
            .replace("پولش رو گرفتم", "")
            .replace("رو پس گرفتم", "")
            .replace("پس گرفتم", "")
            .replace("رو پس دادم", "")
            .replace("پس دادم", "")
            .replace("قرض دادم", "")
            .replace("قرض گرفتم", "")
            .replace("طلب دارم", "")
            .replace("طلبکارم", "")
            .replace("طلبکاری", "")
            .replace("بدهکارم", "")
            .replace("بدهکاری", "")
            .replace("خریدم", "")
            .replace("فروختم", "")
            .replace("خرج کردم", "")
            .replace("هزینه کردم", "")
            .replace("کرایه دادم", "")
            .replace("پول دادم", "")
            .replace("گرفتم", "")
            .replace("دادم", "")
            .replace("امروز", "")
            .replace("دیروز", "")
            .replace("پریروز", "")
            .replace("فردا", "")
            .replace("دیشب", "")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .removePrefix("بابت ")
            .removePrefix("برای ")
            .removePrefix("از ")
            .removePrefix("به ")
            .removeSuffix(" بابت")
            .removeSuffix(" رو")
            .removeSuffix(" را")
            .trim()
        return if (cleaned.length in 2..50) cleaned else fallbackLabel
    }

    private fun parseSingleClause(
        clause: String,
        inheritedDateOffset: Int,
        defaultCurrency: String,
        todayEpochDay: Long = PersianDateAndUnitUtils.currentLocalEpochDay()
    ): ProposedAiOperation? {
        val clauseOffset = PersianDateAndUnitUtils.extractRelativeDateOffsetDays(clause, todayEpochDay)
        val dateOffset = if (clauseOffset != 0) clauseOffset else inheritedDateOffset

        val numberRegex = Regex("(\\d+(?:\\.\\d+)?)")
        val firstNum = numberRegex.find(clause)?.groupValues?.get(1)?.toDoubleOrNull()
        val wordNum = when {
            clause.contains("نیم ساعت") -> 30.0
            clause.contains("یک ساعت") || clause.contains("یه ساعت") -> 60.0
            clause.contains("دو عدد") || clause.contains("دو تا") || clause.contains("دو ") -> 2.0
            clause.contains("سه عدد") || clause.contains("سه تا") || clause.contains("سه ") -> 3.0
            clause.contains("یک ") || clause.contains("یه ") -> 1.0
            else -> null
        }

        // A. Water / Hydration: "الان ۴۰۰ میلی‌لیتر آب خوردم" (Exclude "قبض آب" which is financial!)
        if (clause.contains("آب") && !clause.contains("قبض") &&
            (clause.contains("خوردم") || clause.contains("نوشیدم") || clause.contains("میلی‌لیتر") || clause.contains("لیوان") || clause.contains("لیتر"))
        ) {
            val qty = firstNum ?: wordNum ?: return ProposedAiOperation(
                operationType = "AskClarification",
                clarificationQuestion = "چه مقدار آب نوشیدید؟ (مثلاً ۲۵۰ میلی‌لیتر یا ۱ لیوان)",
                humanReadablePreview = "نیاز به مشخص کردن مقدار آب"
            )
            val unit = when {
                clause.contains("لیتر") && !clause.contains("میلی") -> "liters"
                clause.contains("لیوان") -> "cups"
                else -> "ml"
            }
            return ProposedAiOperation(
                operationType = "AddConsumptionEntry",
                title = "آب",
                category = "WATER",
                quantity = qty,
                unit = unit,
                dueDateOffsetDays = dateOffset,
                requiresConfirmation = true,
                humanReadablePreview = "ثبت مصرف آب: ${PersianDateAndUnitUtils.toPersianDigits(qty.toInt().toString())} $unit"
            )
        }

        val detectedCurrency = detectCurrency(clause)
        val currency = detectedCurrency ?: defaultCurrency
        val extractedCurrencyAmount = PersianDateAndUnitUtils.extractSpokenOrTypedCurrencyAmount(clause, currency)

        // B0. Account-to-Account Transfer (انتقال بین حساب‌ها):
        // e.g. "۵ میلیون از حساب اصلی به پس‌انداز انتقال دادم", "۲ میلیون کارت به کارت به حساب خودم"
        val isTransferIntent = clause.contains("انتقال دادم") ||
            clause.contains("منتقل کردم") ||
            clause.contains("انتقال به") ||
            clause.contains("انتقال از") ||
            clause.contains("بین حساب") ||
            (clause.contains("به پس‌انداز") && (clause.contains("انتقال") || clause.contains("ریختم") || clause.contains("زدم"))) ||
            (clause.contains("کارت به کارت") && (clause.contains("حساب خودم") || clause.contains("پس‌انداز") || clause.contains("کارت خودم")))

        if (isTransferIntent) {
            val amount = extractedCurrencyAmount ?: firstNum?.let {
                PersianDateAndUnitUtils.normalizeColloquialTomanAmount(it, currency, clause)
            }
            return if (amount != null && amount > 0.0) {
                val transferTitle = cleanFinancialTitle(clause, "انتقال بین حساب‌ها").let {
                    if (it.startsWith("انتقال")) it else "انتقال: $it"
                }
                ProposedAiOperation(
                    operationType = "AddExpense",
                    title = transferTitle,
                    category = "OTHER",
                    amount = amount,
                    currency = currency,
                    transactionType = "TRANSFER",
                    notes = "[TRANSFER] $transferTitle",
                    dueDateOffsetDays = dateOffset,
                    requiresConfirmation = true,
                    humanReadablePreview = "ثبت انتقال بین حساب‌ها: ${PersianDateAndUnitUtils.formatCurrencyAmount(amount, currency)} ($transferTitle)"
                )
            } else {
                ProposedAiOperation(
                    operationType = "AskClarification",
                    requiresConfirmation = true,
                    clarificationQuestion = "مبلغ دقیق انتقال بین حساب‌ها چقدر است؟",
                    humanReadablePreview = "نیاز به مبلغ انتقال حساب"
                )
            }
        }

        // B1. Debt & Receivable (بدهکاری و طلبکاری):
        // e.g. "۲۰۰ هزار تومان از مشتری طلب دارم", "یک میلیون تومان به علی قرض دادم", "۵۰۰ به علی بدهکارم"
        val isReceivableIntent = clause.contains("طلبکارم") || clause.contains("طلبکاری") ||
            clause.contains("طلب دارم") || clause.contains("طلب از") || clause.contains("قرض دادم") ||
            clause.contains("دستی دادم")
        val isDebtIntent = clause.contains("بدهکارم") || clause.contains("بدهکاری") ||
            (clause.contains("بدهی") && !clause.contains("پس دادم") && !clause.contains("پرداخت کردم")) ||
            clause.contains("قرض گرفتم") || clause.contains("وام گرفتم")

        if (isReceivableIntent || isDebtIntent) {
            val amount = extractedCurrencyAmount ?: firstNum?.let {
                PersianDateAndUnitUtils.normalizeColloquialTomanAmount(it, currency, clause)
            } ?: wordNum?.let {
                PersianDateAndUnitUtils.normalizeColloquialTomanAmount(it, currency, clause)
            }
            if (amount != null && amount > 0.0) {
                val txType = if (isReceivableIntent) "RECEIVABLE" else "DEBT"
                val txLabel = if (isReceivableIntent) "طلبکاری" else "بدهکاری"
                val title = cleanFinancialTitle(clause, txLabel)
                return ProposedAiOperation(
                    operationType = "AddExpense",
                    title = title,
                    category = "OTHER",
                    amount = amount,
                    currency = currency,
                    transactionType = txType,
                    dueDateOffsetDays = dateOffset,
                    requiresConfirmation = true,
                    humanReadablePreview = "ثبت $txLabel: ${PersianDateAndUnitUtils.formatCurrencyAmount(amount, currency)} ($title)"
                )
            } else {
                val txLabel = if (isReceivableIntent) "طلبکاری" else "بدهکاری"
                return ProposedAiOperation(
                    operationType = "AskClarification",
                    requiresConfirmation = true,
                    clarificationQuestion = "مبلغ دقیق $txLabel و نام شخص یا عنوان آن چقدر است؟",
                    humanReadablePreview = "نیاز به مبلغ و عنوان $txLabel"
                )
            }
        }

        // B2. Comprehensive Income vs. Expense Intent Detection
        // Guard against "اسنپ گرفتم" / "تاکسی گرفتم" / "کرایه گرفتم" (which mean taking a taxi = EXPENSE, unless "کرایه خانه گرفتم")
        val isTaxiRidePhrase = (clause.contains("اسنپ") || clause.contains("تپسی") || clause.contains("تاکسی") || clause.contains("آژانس")) &&
            clause.contains("گرفتم")

        val hasStrongIncomeVerb = !isTaxiRidePhrase && (
            clause.contains("دریافت کردم") ||
                clause.contains("دریافت شد") ||
                clause.contains("واریز شد") ||
                clause.contains("واریز کرد") ||
                clause.contains("به حسابم نشست") ||
                clause.contains("به کارتم نشست") ||
                clause.contains("فروختم") ||
                clause.contains("پس گرفتم") ||
                clause.contains("برگشت خورد") ||
                clause.contains("کش‌بک") ||
                clause.contains("بابت فروش") ||
                clause.contains("از فروش") ||
                clause.contains("فروش جمینای") ||
                clause.contains("فروش gemini", ignoreCase = true) ||
                clause.contains("فروش خدمات") ||
                clause.contains("فروش محصول") ||
                clause.contains("حقوق") ||
                clause.contains("درآمد") ||
                clause.contains("پاداش") ||
                clause.contains("کارانه") ||
                clause.contains("عیدی") ||
                clause.contains("سود بانکی") ||
                clause.contains("سود سهام") ||
                clause.contains("پورسانت") ||
                clause.contains("حق‌الزحمه") ||
                clause.contains("دستمزد") ||
                clause.contains("بیعانه گرفتم") ||
                (clause.contains("گرفتم") && !clause.contains("قرض") && !clause.contains("وام") && !clause.contains("خریدم"))
            )

        val hasStrongExpenseVerb = isTaxiRidePhrase ||
            clause.contains("پرداخت کردم") ||
            clause.contains("پرداخت شد") ||
            clause.contains("خریدم") ||
            clause.contains("خرج کردم") ||
            clause.contains("هزینه کردم") ||
            clause.contains("کرایه دادم") ||
            clause.contains("پول دادم") ||
            clause.contains("پس دادم") ||
            clause.contains("حساب کردم") ||
            clause.contains("کارت کشیدم") ||
            (clause.contains("دادم") && !clause.contains("انتقال دادم") && !clause.contains("قرض دادم") && !clause.contains("انجام دادم"))

        // Contradictory verb check inside a single clause -> AskClarification instead of guessing!
        if (hasStrongIncomeVerb && hasStrongExpenseVerb && !clause.contains("فروختم و پولش رو گرفتم")) {
            return ProposedAiOperation(
                operationType = "AskClarification",
                requiresConfirmation = true,
                clarificationQuestion = "در جمله «$clause» هم نشانه دریافت و هم پرداخت دیده می‌شود؛ آیا این تراکنش دریافت (درآمد +) است یا پرداخت (هزینه −)؟",
                humanReadablePreview = "نیاز به شفاف‌سازی نوع تراکنش (دریافت یا پرداخت)"
            )
        }

        // B2-Income: Execute Income classification
        if (hasStrongIncomeVerb && (extractedCurrencyAmount != null || firstNum != null || wordNum != null)) {
            val amount = extractedCurrencyAmount ?: firstNum?.let {
                PersianDateAndUnitUtils.normalizeColloquialTomanAmount(it, currency, clause)
            } ?: wordNum?.let {
                PersianDateAndUnitUtils.normalizeColloquialTomanAmount(it, currency, clause)
            }
            if (amount != null && amount > 0.0) {
                val cleanedIncomeTitle = cleanFinancialTitle(
                    rawClause = clause,
                    fallbackLabel = when {
                        clause.contains("جمینای") || clause.contains("gemini", ignoreCase = true) -> "فروش Gemini"
                        clause.contains("حقوق") -> "حقوق"
                        clause.contains("فروش") -> "فروش خدمات"
                        clause.contains("سود") -> "سود بانکی"
                        else -> "درآمد"
                    }
                )
                val incomeCategory = when {
                    clause.contains("حقوق") || clause.contains("دستمزد") || clause.contains("کارانه") -> "OTHER"
                    clause.contains("سود") || clause.contains("سرمایه") -> "OTHER"
                    else -> "OTHER"
                }
                return ProposedAiOperation(
                    operationType = "AddExpense",
                    title = cleanedIncomeTitle,
                    category = incomeCategory,
                    amount = amount,
                    currency = currency,
                    transactionType = "INCOME",
                    dueDateOffsetDays = dateOffset,
                    requiresConfirmation = true,
                    humanReadablePreview = "ثبت دریافت (درآمد +): ${PersianDateAndUnitUtils.formatCurrencyAmount(amount, currency)} ($cleanedIncomeTitle)"
                )
            } else {
                return ProposedAiOperation(
                    operationType = "AskClarification",
                    requiresConfirmation = true,
                    clarificationQuestion = "مبلغ دقیق دریافتی (درآمد) چقدر بوده است؟",
                    humanReadablePreview = "نیاز به مبلغ دریافتی"
                )
            }
        }

        // B3. Expense / Payment (پرداخت / هزینه −)
        val nonExpenseTaskOrHealthWords = listOf(
            "ساعت", "دقیقه", "جلسه", "تسک", "خوابیدم", "خواب", "ورزش", "پیاده‌روی",
            "دویدن", "باشگاه", "قرص", "ویتامین", "مکمل", "آب", "میلی‌لیتر", "لیوان", "لیتر",
            "یادداشت", "خاطره", "ژورنال", "کتاب", "مطالعه"
        )
        val hasNonExpenseKeyword = nonExpenseTaskOrHealthWords.any { clause.contains(it) && !clause.contains("قبض") && !clause.contains("خریدم") && !clause.contains("شهریه") }
        val textWithoutNumbers = clause.replace(Regex("""\d+(?:\.\d+)?"""), "").trim()

        val commonExpenseNouns = listOf(
            "غذا", "ناهار", "شام", "صبحانه", "قهوه", "اسپرسو", "لاته", "کافه", "سیگار",
            "ویپ", "اسنپ", "تپسی", "تاکسی", "مترو", "بنزین", "قبض", "اجاره", "شارژ",
            "اینترنت", "اشتراک", "سوپرمارکت", "نان", "میوه", "دارو", "دکتر", "لباس",
            "کفش", "کرایه", "قسط", "شهریه", "بیمه", "تعمیر"
        )
        val hasKnownExpenseNoun = commonExpenseNouns.any { clause.contains(it) }

        val isShortNumericExpense = (firstNum != null || extractedCurrencyAmount != null) &&
            !hasNonExpenseKeyword &&
            textWithoutNumbers.isNotBlank() &&
            textWithoutNumbers.split(Regex("\\s+")).size <= 5 &&
            !clause.contains("کنم") &&
            !clause.contains("بدم")

        if (detectedCurrency != null || hasStrongExpenseVerb || clause.contains("هزینه") || clause.contains("خرج") || isShortNumericExpense) {
            val amount = extractedCurrencyAmount ?: firstNum?.let {
                PersianDateAndUnitUtils.normalizeColloquialTomanAmount(it, currency, clause)
            } ?: wordNum?.let {
                PersianDateAndUnitUtils.normalizeColloquialTomanAmount(it, currency, clause)
            }
            if (amount == null || amount <= 0.0) {
                return ProposedAiOperation(
                    operationType = "AskClarification",
                    requiresConfirmation = true,
                    clarificationQuestion = "مبلغ دقیق هزینه و واحد پول آن چقدر است؟",
                    humanReadablePreview = "نیاز به مبلغ و واحد پول هزینه"
                )
            }

            // Ambiguity check: If there is NO verb (neither income nor expense) and NO known expense noun
            // e.g. "۵۰۰ هزار تومان با علی" or "۲۰۰ هزار تومان شرکت" -> ask clarification instead of guessing!
            if (!hasStrongExpenseVerb && !hasStrongIncomeVerb && !hasKnownExpenseNoun &&
                !clause.contains("خرج") && !clause.contains("هزینه") && !clause.contains("خرید")
            ) {
                val cleanSubj = cleanFinancialTitle(clause, "تراکنش")
                return ProposedAiOperation(
                    operationType = "AskClarification",
                    title = cleanSubj,
                    amount = amount,
                    currency = currency,
                    requiresConfirmation = true,
                    clarificationQuestion = "مشخص نیست مبلغ ${PersianDateAndUnitUtils.formatCurrencyAmount(amount, currency)} مربوط به «$cleanSubj» دریافت بوده یا پرداخت یا طلب/بدهی. لطفاً نوع تراکنش را مشخص فرمایید.",
                    humanReadablePreview = "نیاز به تعیین نوع تراکنش (دریافت / پرداخت / طلب / بدهی) برای «$cleanSubj»"
                )
            }

            val isCigarette = clause.contains("سیگار") || clause.contains("ویپ") || clause.contains("تنباکو")
            val isCoffee = clause.contains("قهوه") || clause.contains("اسپرسو") || clause.contains("لاته") || clause.contains("کافه")
            val isFood = clause.contains("غذا") || clause.contains("ناهار") || clause.contains("شام") || clause.contains("صبحانه") || clause.contains("ساندویچ") || clause.contains("پیتزا") || clause.contains("نان") || clause.contains("میوه") || clause.contains("سوپرمارکت")
            val title = when {
                isCigarette -> "خرید سیگار"
                isCoffee -> "خرید قهوه"
                isFood -> cleanFinancialTitle(clause, "خرید خوراک")
                clause.contains("اسنپ") || clause.contains("تپسی") || clause.contains("تاکسی") -> "کرایه اسنپ / تاکسی"
                clause.contains("بنزین") -> "سوخت و بنزین"
                clause.contains("قبض") -> cleanFinancialTitle(clause, "پرداخت قبض").let { if (it.contains("قبض")) it else "پرداخت قبض $it" }
                clause.contains("اشتراک") -> cleanFinancialTitle(clause, "خرید اشتراک")
                else -> cleanFinancialTitle(clause, "هزینه روزانه")
            }
            val category = when {
                isCigarette || isCoffee -> "COFFEE"
                isFood -> "FOOD"
                clause.contains("قبض") || clause.contains("اجاره") || clause.contains("شارژ") || clause.contains("اینترنت") || clause.contains("اشتراک") -> "BILLS"
                clause.contains("تاکسی") || clause.contains("اسنپ") || clause.contains("تپسی") || clause.contains("بنزین") || clause.contains("مترو") -> "TRANSPORT"
                clause.contains("دارو") || clause.contains("دکتر") || clause.contains("پزشک") -> "HEALTH"
                clause.contains("لباس") || clause.contains("کفش") || clause.contains("خرید") -> "SHOPPING"
                else -> "OTHER"
            }
            return ProposedAiOperation(
                operationType = "AddExpense",
                title = title,
                category = category,
                amount = amount,
                currency = currency,
                transactionType = "EXPENSE",
                dueDateOffsetDays = dateOffset,
                requiresConfirmation = true,
                humanReadablePreview = "ثبت پرداخت (هزینه −): ${PersianDateAndUnitUtils.formatCurrencyAmount(amount, currency)} ($title)"
            )
        }

        // C. Supplements / Vitamins: "دو عدد مکمل مصرف کردم"
        if (clause.contains("مکمل") || clause.contains("ویتامین") || clause.contains("قرص")) {
            val qty = firstNum ?: wordNum ?: 1.0
            val unit = if (clause.contains("قرص") || clause.contains("کپسول")) "tablets" else "pieces"
            val name = when {
                clause.contains("ویتامین") -> "ویتامین"
                else -> "مکمل"
            }
            return ProposedAiOperation(
                operationType = "AddConsumptionEntry",
                title = name,
                category = "SUPPLEMENT",
                quantity = qty,
                unit = unit,
                dueDateOffsetDays = dateOffset,
                requiresConfirmation = true,
                humanReadablePreview = "ثبت مکمل: ${PersianDateAndUnitUtils.toPersianDigits(qty.toInt().toString())} عدد $name"
            )
        }

        // D. Sleep: "دیشب ۷ ساعت خوابیدم"
        if (clause.contains("خوابیدم") || clause.contains("خواب ")) {
            val num = firstNum ?: wordNum ?: return ProposedAiOperation(
                operationType = "AskClarification",
                clarificationQuestion = "چند ساعت خوابیدید؟",
                humanReadablePreview = "نیاز به مدت زمان خواب"
            )
            val durationMin = if (clause.contains("دقیقه") && !clause.contains("ساعت")) num.toInt() else (num * 60).toInt()
            return ProposedAiOperation(
                operationType = "AddSleepRecord",
                title = "خواب شبانه",
                durationMinutes = durationMin,
                qualityRating = 4,
                dueDateOffsetDays = if (clause.contains("دیشب")) 0 else dateOffset,
                requiresConfirmation = true,
                humanReadablePreview = "ثبت خواب: ${PersianDateAndUnitUtils.toPersianDigits((durationMin / 60).toString())} ساعت و ${PersianDateAndUnitUtils.toPersianDigits((durationMin % 60).toString())} دقیقه"
            )
        }

        // E. Exercise: "امروز ۴۵ دقیقه پیاده‌روی کردم" or "نیم ساعت ورزش کنم"
        if (clause.contains("پیاده‌روی") || clause.contains("ورزش") || clause.contains("دویدن") || clause.contains("باشگاه")) {
            val durationMin = when {
                clause.contains("نیم ساعت") -> 30
                clause.contains("ساعت") && firstNum != null -> (firstNum * 60).toInt()
                firstNum != null -> firstNum.toInt()
                else -> 30
            }
            val actType = if (clause.contains("پیاده‌روی")) "پیاده‌روی" else "ورزش"
            // If it says "فردا ... ورزش کنم", it's a future scheduled Task! If past ("کردم"), it's an ExerciseRecord!
            return if (dateOffset > 0 || clause.contains("کنم")) {
                ProposedAiOperation(
                    operationType = "CreateTask",
                    title = "$actType (${PersianDateAndUnitUtils.toPersianDigits(durationMin.toString())} دقیقه)",
                    category = "سلامت و ورزش",
                    durationMinutes = durationMin,
                    dueDateOffsetDays = dateOffset,
                    dueTimeMinutes = extractTimeMinutes(clause),
                    priority = "MEDIUM",
                    requiresConfirmation = true,
                    humanReadablePreview = "ایجاد تسک ورزشی: $actType به مدت ${PersianDateAndUnitUtils.toPersianDigits(durationMin.toString())} دقیقه"
                )
            } else {
                ProposedAiOperation(
                    operationType = "AddExerciseRecord",
                    title = actType,
                    durationMinutes = durationMin,
                    intensity = "MODERATE",
                    dueDateOffsetDays = dateOffset,
                    requiresConfirmation = true,
                    humanReadablePreview = "ثبت ورزش: ${PersianDateAndUnitUtils.toPersianDigits(durationMin.toString())} دقیقه $actType"
                )
            }
        }

        // F. Journal / Daily Log / Diary: e.g. "یادداشت روزانه: ..." or "خاطره امروز: ..." or "یادداشت کن ..."
        if (clause.contains("یادداشت روزانه") || clause.contains("خاطره") || clause.contains("دفترچه خاطرات") ||
            clause.contains("یادداشت کن") || clause.contains("ژورنال") || clause.startsWith("یادداشت")
        ) {
            val content = clause
                .replace("یادداشت روزانه", "")
                .replace("ثبت خاطره", "")
                .replace("خاطره امروز", "")
                .replace("دفترچه خاطرات", "")
                .replace("یادداشت کن که", "")
                .replace("یادداشت کن", "")
                .replace("ژورنال", "")
                .trim()
                .removePrefix(":")
                .trim()
                .ifBlank { clause }

            return ProposedAiOperation(
                operationType = "CreateTask",
                title = content.take(80),
                category = "یادداشت روزانه",
                dueDateOffsetDays = dateOffset,
                priority = "LOW",
                requiresConfirmation = true,
                humanReadablePreview = "ثبت یادداشت روزانه: «$content»"
            )
        }

        // G. Tasks with Due Date, Time, Priority & Category (e.g. "فردا ساعت ۹ جلسه با علی دارم" / "تسک پرداخت قبض")
        val dueTime = extractTimeMinutes(clause)
        val priority = when {
            clause.contains("خیلی مهم") || clause.contains("بحرانی") || clause.contains("فوری") -> "CRITICAL"
            clause.contains("مهم") || clause.contains("اولویت بالا") || clause.contains("قبض") -> "HIGH"
            clause.contains("کم‌اهمیت") || clause.contains("اولویت پایین") -> "LOW"
            else -> "MEDIUM"
        }
        val category = when {
            clause.contains("قبض") || clause.contains("مالی") || clause.contains("بانک") || clause.contains("حساب") -> "مالی"
            clause.contains("جلسه") || clause.contains("کاری") || clause.contains("پروژه") || clause.contains("شرکت") || clause.contains("تیم") -> "کار"
            clause.contains("کتاب") || clause.contains("درس") || clause.contains("مطالعه") || clause.contains("دانشگاه") -> "یادگیری"
            clause.contains("دارو") || clause.contains("دکتر") || clause.contains("پزشک") || clause.contains("ویزیت") -> "سلامت"
            clause.contains("خرید") || clause.contains("سوپرمارکت") || clause.contains("بازار") -> "خرید"
            else -> "عمومی"
        }

        val cleanedTitle = clause
            .replace("تسک جدید", "")
            .replace("تسک", "")
            .replace("فردا", "")
            .replace("امروز", "")
            .replace(Regex("ساعت\\s*\\d+(?::\\d+)?"), "")
            .replace("رو پرداخت کنم", "پرداخت")
            .replace("را پرداخت کنم", "پرداخت")
            .replace("اضافه کن", "")
            .replace("ثبت کن", "")
            .replace("انجام بدم", "")
            .replace("کنم", "")
            .trim()
            .removePrefix(":")
            .trim()
            .ifBlank { clause }

        return ProposedAiOperation(
            operationType = "CreateTask",
            title = if (clause.contains("قبض") && !cleanedTitle.contains("قبض")) "پرداخت قبض" else cleanedTitle,
            category = category,
            dueDateOffsetDays = dateOffset,
            dueTimeMinutes = dueTime,
            priority = priority,
            requiresConfirmation = true,
            humanReadablePreview = "ایجاد تسک [$category]: «${if (clause.contains("قبض") && !cleanedTitle.contains("قبض")) "پرداخت قبض" else cleanedTitle}»" +
                (if (dateOffset == 1) " (فردا)" else "") +
                (if (dueTime >= 0) " ساعت ${PersianDateAndUnitUtils.formatTimeMinutes(dueTime)}" else "") +
                (if (priority == "HIGH" || priority == "CRITICAL") " [اولویت $priority]" else "")
        )
    }

    private fun detectCurrency(text: String): String? {
        return when {
            text.contains("یورو") || text.contains("eur", ignoreCase = true) || text.contains("€") -> "EUR"
            text.contains("دلار") || text.contains("usd", ignoreCase = true) || text.contains("$") -> "USD"
            text.contains("پوند") || text.contains("gbp", ignoreCase = true) || text.contains("£") -> "GBP"
            text.contains("درهم") || text.contains("aed", ignoreCase = true) -> "AED"
            text.contains("ریال") || text.contains("irr", ignoreCase = true) -> "IRR"
            text.contains("تومان") || text.contains("تومن") -> "TOMAN"
            else -> null
        }
    }

    private fun extractTimeMinutes(text: String): Int {
        val timeMatch = Regex("ساعت\\s*(\\d{1,2})(?::(\\d{1,2}))?").find(text) ?: return -1
        val hour = timeMatch.groupValues[1].toIntOrNull() ?: return -1
        val min = timeMatch.groupValues.getOrNull(2)?.toIntOrNull() ?: 0
        if (hour !in 0..23 || min !in 0..59) return -1
        return hour * 60 + min
    }

    private fun answerHistoricalQueryFactual(
        norm: String,
        todayEpochDay: Long,
        tasks: List<Task>,
        consumption: List<ConsumptionEntry>,
        expenses: List<ExpenseEntry>,
        habits: List<Habit>,
        habitRecords: List<HabitRecord>,
        sleepRecords: List<SleepRecord>,
        exerciseRecords: List<ExerciseRecord>,
        recentEvents: List<ActivityEvent>
    ): AiParseResult {
        val reply = when {
            norm.contains("آب") -> {
                val ml = PersianDateAndUnitUtils.sumCompatibleConsumption(
                    consumption.filter { it.localEpochDay == todayEpochDay && it.category == "WATER" },
                    "ml"
                ).toInt()
                "📊 واقعیت ثبت‌شده: امروز مجموعاً ${PersianDateAndUnitUtils.toPersianDigits(ml.toString())} میلی‌لیتر آب ثبت کرده‌اید."
            }
            norm.contains("قهوه") -> {
                val count = consumption.filter {
                    it.localEpochDay in (todayEpochDay - 6)..todayEpochDay && it.category == "COFFEE"
                }.sumOf { it.quantity }.toInt()
                "📊 واقعیت ثبت‌شده: در ۷ روز گذشته ${PersianDateAndUnitUtils.toPersianDigits(count.toString())} وعده قهوه ثبت شده است."
            }
            norm.contains("بیشترین هزینه") || norm.contains("بیشترین خرج") -> {
                val monthExp = expenses.filter { it.localEpochDay in (todayEpochDay - 29)..todayEpochDay && it.isExpense }
                if (monthExp.isEmpty()) {
                    "📊 واقعیت ثبت‌شده (بازه ۳۰ روز اخیر): داده کافی نیست — هیچ هزینه‌ای در ۳۰ روز گذشته ثبت نشده است."
                } else {
                    val topCat = monthExp.groupBy { it.category }
                        .mapValues { entry -> entry.value.sumOf { it.amount } }
                        .maxByOrNull { it.value }
                    val currency = monthExp.first().currency
                    "📊 واقعیت ثبت‌شده (بازه ۳۰ روز اخیر): بیشترین هزینه شما در دسته «${topCat?.key ?: "نامشخص"}» به مبلغ ${PersianDateAndUnitUtils.formatCurrencyAmount(topCat?.value ?: 0.0, currency)} بوده است."
                }
            }
            norm.contains("بدهی") || norm.contains("قسط") || norm.contains("بازپرداخت") -> {
                val openDebts = expenses.filter { it.isDebt && !it.isSettled }
                if (openDebts.isEmpty()) {
                    "📊 واقعیت ثبت‌شده: در حال حاضر هیچ بدهی تسویه‌نشده‌ای در سیستم ثبت نشده است."
                } else {
                    val totalRem = openDebts.sumOf { it.remainingAmount }
                    val curr = openDebts.first().currency
                    val linkedTasksCount = tasks.count { it.linkedDebtId.isNotBlank() && it.status != "COMPLETED" }
                    "📊 واقعیت ثبت‌شده: شما ${PersianDateAndUnitUtils.toPersianDigits(openDebts.size.toString())} بدهی باز با مانده کل ${PersianDateAndUnitUtils.formatCurrencyAmount(totalRem, curr)} دارید (${PersianDateAndUnitUtils.toPersianDigits(linkedTasksCount.toString())} قسط فعال در بخش اهداف و مسیر)."
                }
            }
            norm.contains("خرج") || norm.contains("هزینه") -> {
                val totals = PersianDateAndUnitUtils.aggregateExpensesByCurrency(
                    expenses.filter { it.localEpochDay in (todayEpochDay - 29)..todayEpochDay }
                )
                if (totals.isEmpty()) {
                    "📊 واقعیت ثبت‌شده: در ۳۰ روز گذشته هیچ هزینه‌ای در سیستم ثبت نشده است."
                } else {
                    val formatted = totals.entries.joinToString(" ، ") {
                        PersianDateAndUnitUtils.formatCurrencyAmount(it.value, it.key)
                    }
                    "📊 واقعیت ثبت‌شده (به تفکیک ارز بدون ترکیب خودکار): مجموع هزینه‌های این ماه شما: $formatted"
                }
            }
            norm.contains("خواب") -> {
                val last = sleepRecords.firstOrNull()
                if (last == null) {
                    "📊 واقعیت ثبت‌شده: هنوز رکورد خوابی برای دیشب ثبت نشده است."
                } else {
                    "📊 واقعیت ثبت‌شده: آخرین خواب ثبت‌شده شما ${PersianDateAndUnitUtils.toPersianDigits((last.durationMinutes / 60).toString())} ساعت و ${PersianDateAndUnitUtils.toPersianDigits((last.durationMinutes % 60).toString())} دقیقه با کیفیت ${PersianDateAndUnitUtils.toPersianDigits(last.qualityRating.toString())} از ۵ بوده است."
                }
            }
            norm.contains("دیروز") -> {
                val doneYesterday = tasks.filter { it.status == "COMPLETED" && it.dueDateEpochDay == todayEpochDay - 1L }
                if (doneYesterday.isEmpty()) {
                    "📊 واقعیت ثبت‌شده: برای دیروز تسک تکمیل‌شده‌ای ثبت نشده است."
                } else {
                    "📊 واقعیت ثبت‌شده: دیروز ${PersianDateAndUnitUtils.toPersianDigits(doneYesterday.size.toString())} تسک تکمیل کردید: ${doneYesterday.joinToString("، ") { "«${it.title}»" }}"
                }
            }
            norm.contains("عادت") -> {
                val doneIds = habitRecords.filter { it.localEpochDay == todayEpochDay }.map { it.habitId }.toSet()
                val missed = habits.filter { !it.archived && it.id !in doneIds }
                val weekRecordsCount = habitRecords.count { it.localEpochDay in (todayEpochDay - 6)..todayEpochDay }
                if (habits.isEmpty()) {
                    "📊 واقعیت ثبت‌شده: هنوز عادتی تعریف نکرده‌اید."
                } else if (missed.isEmpty()) {
                    "📊 واقعیت ثبت‌شده (بازه امروز و ۷ روز اخیر): عالی! تمام عادت‌های امروز انجام شده‌اند (مجموع ثبت ۷ روز اخیر: ${PersianDateAndUnitUtils.toPersianDigits(weekRecordsCount.toString())} مورد)."
                } else {
                    "📊 واقعیت ثبت‌شده (بازه امروز و ۷ روز اخیر): عادت‌های انجام‌نشده امروز: ${missed.joinToString("، ") { "«${it.title}»" }} (مجموع ثبت ۷ روز اخیر: ${PersianDateAndUnitUtils.toPersianDigits(weekRecordsCount.toString())} مورد)."
                }
            }
            norm.contains("تغییرات") || norm.contains("جمینای") -> {
                val aiEvents = recentEvents.filter { it.inputSource == "AI_ASSISTANT" || it.inputSource == "VOICE" }.take(5)
                if (aiEvents.isEmpty()) {
                    "📊 واقعیت ثبت‌شده: امروز هنوز تغییری توسط دستیار صوتی یا هوش مصنوعی ثبت نشده است."
                } else {
                    "📊 تغییرات انجام‌شده امروز:\n" + aiEvents.joinToString("\n") { "• ${it.humanSummary}" }
                }
            }
            else -> {
                val pendingToday = tasks.filter { it.dueDateEpochDay == todayEpochDay && it.status != "COMPLETED" && it.status != "ARCHIVED" }
                if (pendingToday.isEmpty()) {
                    "📊 واقعیت ثبت‌شده: برای امروز هیچ تسک باقی‌مانده‌ای ندارید.\n💡 پیشنهاد: می‌توانید اولویت‌های فردا را از همین حالا برنامه‌ریزی کنید."
                } else {
                    "📊 واقعیت ثبت‌شده: امروز ${PersianDateAndUnitUtils.toPersianDigits(pendingToday.size.toString())} تسک باقی مانده است: ${pendingToday.joinToString("، ") { "«${it.title}»" }}"
                }
            }
        }
        return AiParseResult(
            conversationalReply = reply,
            operations = emptyList(),
            isAmbiguous = false
        )
    }
}
