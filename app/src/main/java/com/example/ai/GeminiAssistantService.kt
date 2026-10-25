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
                    Result.failure(IllegalArgumentException("مبلغ یا واحد پول هزینه مشخص نیست."))
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

        // 1. Check if it's a Question / Historical Query first
        if (norm.contains("؟") || norm.contains("?") ||
            norm.startsWith("چه ") || norm.startsWith("چقدر ") || norm.startsWith("چند ") ||
            norm.contains("باقی مونده") || norm.contains("باقی مانده") ||
            norm.contains("دیروز چی") || norm.contains("خلاصه") ||
            norm.contains("تغییرات") || norm.contains("کدام عادت")
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
        var sharedDateOffset = if (norm.contains("فردا")) 1 else if (norm.contains("دیروز") || norm.contains("دیشب")) -1 else 0

        for (clause in clauses) {
            val op = parseSingleClause(clause.trim(), sharedDateOffset, defaultCurrency)
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
            "عملیات شناسایی شد: ${operations.first().humanReadablePreview}. لطفاً پیش از ذخیره تأیید فرمایید."
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

    private fun splitCompoundClauses(norm: String): List<String> {
        val parts = norm.split(Regex("\\s+و\\s+"))
        if (parts.size <= 1) return listOf(norm)
        // Only split if each part looks like an independent action
        val actionKeywords = listOf("خوردم", "نوشیدم", "خریدم", "خوابیدم", "ورزش", "پیاده‌روی", "پرداخت", "کنم", "مصرف", "هزینه", "بدوم", "مطالعه", "تسک", "بذار", "ثبت")
        val validSplit = parts.all { part -> actionKeywords.any { kw -> part.contains(kw) } }
        return if (validSplit) parts else listOf(norm)
    }

    private fun parseSingleClause(
        clause: String,
        inheritedDateOffset: Int,
        defaultCurrency: String
    ): ProposedAiOperation? {
        val dateOffset = when {
            clause.contains("فردا") -> 1
            clause.contains("دیروز") || clause.contains("دیشب") -> -1
            else -> inheritedDateOffset
        }

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

        // A. Water / Hydration: "الان ۴۰۰ میلی‌لیتر آب خوردم"
        if (clause.contains("آب") && (clause.contains("خوردم") || clause.contains("نوشیدم") || clause.contains("میلی‌لیتر") || clause.contains("لیوان") || clause.contains("لیتر"))) {
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

        // B. Expense / Purchase (e.g. "یه قهوه ۳ یورویی خریدم" or "هزینه امروز رو ۱۲ یورو افزایش بده")
        val detectedCurrency = detectCurrency(clause)
        if (detectedCurrency != null || clause.contains("خریدم") || clause.contains("هزینه")) {
            val amount = firstNum ?: wordNum
            if (amount == null || amount <= 0.0) {
                return ProposedAiOperation(
                    operationType = "AskClarification",
                    clarificationQuestion = "مبلغ دقیق هزینه و واحد پول آن چقدر است؟",
                    humanReadablePreview = "نیاز به مبلغ و واحد پول هزینه"
                )
            }
            val currency = detectedCurrency ?: defaultCurrency
            val isCoffee = clause.contains("قهوه") || clause.contains("اسپرسو")
            val isFood = clause.contains("غذا") || clause.contains("ناهار") || clause.contains("شام")
            val title = when {
                isCoffee -> "خرید قهوه"
                isFood -> "خرید غذا"
                else -> "هزینه روزانه"
            }
            val category = when {
                isCoffee -> "COFFEE"
                isFood -> "FOOD"
                clause.contains("قبض") -> "BILLS"
                clause.contains("تاکسی") || clause.contains("اسنپ") -> "TRANSPORT"
                else -> "OTHER"
            }
            return ProposedAiOperation(
                operationType = "AddExpense",
                title = title,
                category = category,
                amount = amount,
                currency = currency,
                dueDateOffsetDays = dateOffset,
                requiresConfirmation = true,
                humanReadablePreview = "ثبت هزینه مالی: ${PersianDateAndUnitUtils.formatCurrencyAmount(amount, currency)} بابت $title"
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
                if (habits.isEmpty()) {
                    "📊 واقعیت ثبت‌شده: هنوز عادتی تعریف نکرده‌اید."
                } else if (missed.isEmpty()) {
                    "📊 واقعیت ثبت‌شده: عالی! تمام عادت‌های امروز را انجام داده‌اید."
                } else {
                    "📊 واقعیت ثبت‌شده: عادت‌های انجام‌نشده امروز: ${missed.joinToString("، ") { "«${it.title}»" }}"
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
