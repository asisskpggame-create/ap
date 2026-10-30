package com.example.personallifeos.util

import android.util.Base64
import com.example.personallifeos.data.model.DebtItem
import com.example.personallifeos.data.model.DebtType
import com.example.personallifeos.data.model.ExpenseItem
import com.example.personallifeos.data.model.TransactionType
import java.math.BigDecimal
import java.math.RoundingMode
import java.security.SecureRandom
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

enum class CurrencyInputUnit(val labelFa: String, val multiplierToToman: BigDecimal) {
    TOMAN("تومان", BigDecimal.ONE),
    HEZAR_TOMAN("هزار تومان", BigDecimal("1000")),
    MILLION_TOMAN("میلیون تومان", BigDecimal("1000000")),
    IRR("ریال", BigDecimal("0.1")),
    USD("دلار ($)", BigDecimal("68000")),
    EUR("یورو (€)", BigDecimal("74000")),
    AED("درهم (AED)", BigDecimal("18500"))
}

data class ColloquialAmountResult(
    val amountToman: Long,
    val formattedPreview: String,
    val extractedTitle: String,
    val suggestedCategory: String,
    val wasColloquialMultiplied: Boolean
)

enum class DetectedIntentType(val titleFa: String) {
    EXPENSE("ثبت هزینه"),
    INCOME("ثبت درآمد"),
    TRANSFER("انتقال بین حساب‌ها"),
    DEBT("ثبت بدهی"),
    RECEIVABLE("ثبت طلب"),
    TASK("ثبت کار و وظیفه"),
    ANALYTICS_QUERY("پرسش تحلیلی"),
    AMBIGUOUS("نیازمند توضیح بیشتر")
}

data class DetectedIntentResult(
    val intentType: DetectedIntentType,
    val title: String,
    val amountToman: Long?,
    val category: String,
    val personOrAccount: String?,
    val isAmbiguous: Boolean,
    val clarificationQuestionFa: String? = null
)

data class SafeToSpendSummary(
    val monthlyBudgetToman: Long,
    val totalIncomeToman: Long,
    val totalExpenseToman: Long,
    val upcomingDebtToman: Long,
    val safeToSpendToman: Long,
    val estimatedEndOfMonthExpenseToman: Long,
    val hasEnoughData: Boolean,
    val formulaExplanationFa: String
)

object PersianUtils {

    private val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    val JALALI_MONTH_NAMES = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    fun formatPersianNumber(num: Long): String {
        return formatPersianNumber(num.toString())
    }

    fun formatPersianNumber(text: String): String {
        val sb = StringBuilder()
        for (ch in text) {
            if (ch in '0'..'9') {
                sb.append(persianDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun formatToman(amount: Long): String {
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
        }
        val formatter = DecimalFormat("#,###", symbols)
        val formatted = formatter.format(amount)
        return "${formatPersianNumber(formatted)} تومان"
    }

    fun normalizePersianText(input: String?): String {
        if (input.isNullOrEmpty()) return ""
        return input
            .replace('ك', 'ک')
            .replace('ي', 'ی')
            .replace('۰', '0')
            .replace('۱', '1')
            .replace('۲', '2')
            .replace('۳', '3')
            .replace('۴', '4')
            .replace('۵', '5')
            .replace('۶', '6')
            .replace('۷', '7')
            .replace('۸', '8')
            .replace('۹', '9')
            .replace('٠', '0')
            .replace('١', '1')
            .replace('٢', '2')
            .replace('٣', '3')
            .replace('٤', '4')
            .replace('٥', '5')
            .replace('٦', '6')
            .replace('٧', '7')
            .replace('٨', '8')
            .replace('٩', '9')
            .trim()
            .lowercase()
    }

    // =========================================================================
    // BigDecimal Financial Precision & Accounting Rules (HALF_UP, scale = 2)
    // =========================================================================

    fun convertToTomanBigDecimal(rawAmount: String, unit: CurrencyInputUnit): Long {
        val cleaned = normalizePersianText(rawAmount).replace(",", "").replace("/", ".").trim()
        val bd = cleaned.toBigDecimalOrNull() ?: return 0L
        return bd.multiply(unit.multiplierToToman)
            .setScale(0, RoundingMode.HALF_UP)
            .toLong()
            .coerceAtLeast(0L)
    }

    /**
     * Non-negotiable rule: TRANSFER between user's own accounts is NEVER counted
     * as EXPENSE or INCOME.
     */
    fun aggregateExpensesToman(expenses: List<ExpenseItem>): Long {
        return expenses
            .asSequence()
            .filter { it.type == TransactionType.EXPENSE }
            .fold(BigDecimal.ZERO) { acc, item -> acc.add(BigDecimal.valueOf(item.amount)) }
            .setScale(2, RoundingMode.HALF_UP)
            .toLong()
    }

    fun aggregateIncomesToman(expenses: List<ExpenseItem>): Long {
        return expenses
            .asSequence()
            .filter { it.type == TransactionType.INCOME }
            .fold(BigDecimal.ZERO) { acc, item -> acc.add(BigDecimal.valueOf(item.amount)) }
            .setScale(2, RoundingMode.HALF_UP)
            .toLong()
    }

    fun aggregateTransfersToman(expenses: List<ExpenseItem>): Long {
        return expenses
            .asSequence()
            .filter { it.type == TransactionType.TRANSFER }
            .fold(BigDecimal.ZERO) { acc, item -> acc.add(BigDecimal.valueOf(item.amount)) }
            .setScale(2, RoundingMode.HALF_UP)
            .toLong()
    }

    fun aggregateNetBalanceToman(expenses: List<ExpenseItem>): Long {
        val income = BigDecimal.valueOf(aggregateIncomesToman(expenses))
        val expense = BigDecimal.valueOf(aggregateExpensesToman(expenses))
        return income.subtract(expense).setScale(2, RoundingMode.HALF_UP).toLong()
    }

    fun aggregateTotalDebtsRemainingToman(debts: List<DebtItem>): Long {
        return debts
            .asSequence()
            .filter { it.type == DebtType.DEBT && !it.isSettled }
            .fold(BigDecimal.ZERO) { acc, item -> acc.add(BigDecimal.valueOf(item.remainingAmount)) }
            .setScale(2, RoundingMode.HALF_UP)
            .toLong()
    }

    fun aggregateTotalReceivablesRemainingToman(debts: List<DebtItem>): Long {
        return debts
            .asSequence()
            .filter { it.type == DebtType.RECEIVABLE && !it.isSettled }
            .fold(BigDecimal.ZERO) { acc, item -> acc.add(BigDecimal.valueOf(item.remainingAmount)) }
            .setScale(2, RoundingMode.HALF_UP)
            .toLong()
    }

    fun aggregateAccountBalances(expenses: List<ExpenseItem>): Map<String, Long> {
        val map = mutableMapOf<String, BigDecimal>()
        for (tx in expenses) {
            val accName = tx.accountName?.takeIf { it.isNotBlank() } ?: tx.bankName ?: "حساب پیش‌فرض"
            val current = map.getOrDefault(accName, BigDecimal.ZERO)
            val delta = when (tx.type) {
                TransactionType.INCOME -> BigDecimal.valueOf(tx.amount)
                TransactionType.EXPENSE -> BigDecimal.valueOf(tx.amount).negate()
                TransactionType.TRANSFER -> BigDecimal.ZERO
            }
            map[accName] = current.add(delta)
        }
        return map.mapValues { it.value.setScale(0, RoundingMode.HALF_UP).toLong() }
    }

    fun calculateSafeToSpend(
        monthlyBudgetToman: Long,
        expenses: List<ExpenseItem>,
        debts: List<DebtItem>,
        currentDayOfMonth: Int = 5
    ): SafeToSpendSummary {
        val totalIncome = aggregateIncomesToman(expenses)
        val totalExpense = aggregateExpensesToman(expenses)
        val unsettledDebt = aggregateTotalDebtsRemainingToman(debts)
        val effectiveCap = if (totalIncome > monthlyBudgetToman) totalIncome else monthlyBudgetToman
        val safeToSpend = maxOf(0L, effectiveCap - totalExpense - (unsettledDebt / 2))
        val daysElapsed = currentDayOfMonth.coerceAtLeast(1)
        val estimatedEndOfMonth = BigDecimal.valueOf(totalExpense)
            .multiply(BigDecimal.valueOf(30))
            .divide(BigDecimal.valueOf(daysElapsed.toLong()), 0, RoundingMode.HALF_UP)
            .toLong()

        return SafeToSpendSummary(
            monthlyBudgetToman = monthlyBudgetToman,
            totalIncomeToman = totalIncome,
            totalExpenseToman = totalExpense,
            upcomingDebtToman = unsettledDebt,
            safeToSpendToman = safeToSpend,
            estimatedEndOfMonthExpenseToman = estimatedEndOfMonth,
            hasEnoughData = expenses.isNotEmpty(),
            formulaExplanationFa = "فرمول: سقف بودجه/درآمد (${formatToman(effectiveCap)}) منهای هزینه‌های واقعی (${formatToman(totalExpense)}) و تعهد قسط نزدیک"
        )
    }

    // =========================================================================
    // Colloquial Persian Toman Normalizer & Speech Intent Engine
    // =========================================================================

    private val wordNumbers = mapOf(
        "یک" to 1L, "دو" to 2L, "سه" to 3L, "چهار" to 4L, "پنج" to 5L,
        "شش" to 6L, "شیش" to 6L, "هفت" to 7L, "هشت" to 8L, "نه" to 9L,
        "ده" to 10L, "یازده" to 11L, "دوازده" to 12L, "پانزده" to 15L, "پونزده" to 15L,
        "بیست" to 20L, "سی" to 30L, "چهل" to 40L, "پنجاه" to 50L,
        "شصت" to 60L, "هفتاد" to 70L, "هشتاد" to 80L, "نود" to 90L,
        "صد" to 100L, "صد و پنجاه" to 150L, "دویست" to 200L, "سیصد" to 300L,
        "چهارصد" to 400L, "پانصد" to 500L, "پونصد" to 500L,
        "ششصد" to 600L, "شیشصد" to 600L, "هفتصد" to 700L, "هشتصد" to 800L, "نهصد" to 900L
    )

    /**
     * Converts colloquial Iranian speech/typing like "500 تومن سیگار خریدم" or "پانصد تومان اسنپ"
     * or "2 میلیون و 500" into exact Toman amounts.
     */
    fun normalizeColloquialTomanAmount(rawInput: String): ColloquialAmountResult? {
        val norm = normalizePersianText(rawInput)
        if (norm.isBlank()) return null

        var working = norm
        for ((word, num) in wordNumbers.entries.sortedByDescending { it.key.length }) {
            working = working.replace(word, num.toString())
        }

        var detectedAmountToman: Long? = null
        var wasColloquial = false

        // Pattern 1: "X میلیون و Y" (e.g., 2 میلیون و 500 -> 2,500,000)
        val millionCombo = Regex("""(\d+)\s*میلیون\s*و\s*(\d{1,3})""").find(working)
        if (millionCombo != null) {
            val m = millionCombo.groupValues[1].toLongOrNull() ?: 0L
            val k = millionCombo.groupValues[2].toLongOrNull() ?: 0L
            detectedAmountToman = (m * 1_000_000L) + (k * 1_000L)
            wasColloquial = true
        }

        // Pattern 2: "X میلیون"
        if (detectedAmountToman == null) {
            val millionOnly = Regex("""(\d+(?:\.\d+)?)\s*میلیون""").find(working)
            if (millionOnly != null) {
                val m = millionOnly.groupValues[1].toDoubleOrNull() ?: 0.0
                detectedAmountToman = (m * 1_000_000.0).toLong()
                wasColloquial = true
            }
        }

        // Pattern 3: "X هزار"
        if (detectedAmountToman == null) {
            val hezarMatch = Regex("""(\d+)\s*هزار""").find(working)
            if (hezarMatch != null) {
                val k = hezarMatch.groupValues[1].toLongOrNull() ?: 0L
                detectedAmountToman = k * 1_000L
                wasColloquial = true
            }
        }

        // Pattern 4: Pure digits or digits with "تومان / تومن" where 1..999 means * 1,000 in colloquial Persian
        if (detectedAmountToman == null) {
            val numMatch = Regex("""([\d,]+)""").find(working)
            if (numMatch != null) {
                val rawNum = numMatch.groupValues[1].replace(",", "").toLongOrNull()
                if (rawNum != null && rawNum > 0) {
                    if (rawNum in 1..999) {
                        detectedAmountToman = rawNum * 1_000L
                        wasColloquial = true
                    } else {
                        detectedAmountToman = rawNum
                    }
                }
            }
        }

        val finalAmount = detectedAmountToman ?: return null
        val category = inferCategoryFromText(norm)
        val cleanedTitle = norm
            .replace(Regex("""[\d,]+"""), "")
            .replace("تومان", "")
            .replace("تومن", "")
            .replace("هزار", "")
            .replace("میلیون", "")
            .replace("خریدم", "")
            .replace("دادم", "")
            .replace("پرداخت کردم", "")
            .trim()
            .ifBlank { category }

        return ColloquialAmountResult(
            amountToman = finalAmount,
            formattedPreview = formatToman(finalAmount),
            extractedTitle = cleanedTitle,
            suggestedCategory = category,
            wasColloquialMultiplied = wasColloquial
        )
    }

    fun inferCategoryFromText(text: String): String {
        val norm = normalizePersianText(text)
        return when {
            norm.contains("سیگار") || norm.contains("قهوه") || norm.contains("کافه") -> "کافه و دخانیات"
            norm.contains("اسنپ") || norm.contains("تپسی") || norm.contains("تاکسی") || norm.contains("مترو") || norm.contains("بنزین") -> "حمل و نقل"
            norm.contains("رستوران") || norm.contains("غذا") || norm.contains("ناهار") || norm.contains("شام") || norm.contains("پیتزا") -> "غذا و رستوران"
            norm.contains("سوپرمارکت") || norm.contains("رفاه") || norm.contains("کوروش") || norm.contains("نان") || norm.contains("میوه") -> "سوپرمارکت و روزمره"
            norm.contains("اجاره") || norm.contains("قبض") || norm.contains("شارژ ساختمان") -> "مسکن و قبوض"
            norm.contains("دارو") || norm.contains("دکتر") || norm.contains("آزمایش") || norm.contains("باشگاه") -> "سلامت و درمان"
            norm.contains("حقوق") || norm.contains("پاداش") || norm.contains("فریلنس") || norm.contains("پروژه") -> "حقوق و دستمزد"
            else -> "خرید عمومی"
        }
    }

    fun detectTransactionIntent(input: String): DetectedIntentResult {
        val norm = normalizePersianText(input)
        val colloquial = normalizeColloquialTomanAmount(norm)

        // Check if analytical query
        if (norm.contains("چقدر") || norm.contains("بیشترین") || norm.contains("گزارش") ||
            norm.contains("وضعیت") || norm.contains("کدوم") || norm.contains("روند") ||
            norm.contains("چند") || norm.endsWith("؟") || norm.endsWith("?")
        ) {
            return DetectedIntentResult(
                intentType = DetectedIntentType.ANALYTICS_QUERY,
                title = input,
                amountToman = null,
                category = "تحلیل هوشمند",
                personOrAccount = null,
                isAmbiguous = false
            )
        }

        // Check if task intent
        if (norm.contains("وظیفه") || norm.contains("تسک") || norm.contains("یادم بنداز") || norm.contains("برنامه فردا")) {
            val taskTitle = norm
                .replace("وظیفه", "")
                .replace("تسک", "")
                .replace("یادم بنداز", "")
                .replace("ثبت کن", "")
                .trim()
            return DetectedIntentResult(
                intentType = DetectedIntentType.TASK,
                title = taskTitle.ifBlank { input },
                amountToman = null,
                category = "کاری",
                personOrAccount = null,
                isAmbiguous = taskTitle.length < 2,
                clarificationQuestionFa = if (taskTitle.length < 2) "عنوان دقیق وظیفه و موعد انجام آن چیست؟" else null
            )
        }

        // Check Transfer between own accounts
        if (norm.contains("انتقال به حساب خودم") || norm.contains("کارت به کارت به خودم") || norm.contains("جابجایی بین حساب")) {
            val amt = colloquial?.amountToman
            return DetectedIntentResult(
                intentType = DetectedIntentType.TRANSFER,
                title = colloquial?.extractedTitle ?: "انتقال بین حساب‌های شخصی",
                amountToman = amt,
                category = "انتقال داخلی",
                personOrAccount = "حساب شخصی",
                isAmbiguous = amt == null || amt <= 0L,
                clarificationQuestionFa = if (amt == null) "مبلغ انتقال بین حساب‌های خودتان چقدر بوده است؟" else null
            )
        }

        // Check Receivable (طلب)
        if (norm.contains("طلب") || norm.contains("قرض دادم") || norm.contains("بهم بدهکاره") || norm.contains("باید بهم بده")) {
            val amt = colloquial?.amountToman
            return DetectedIntentResult(
                intentType = DetectedIntentType.RECEIVABLE,
                title = colloquial?.extractedTitle ?: "طلب ثبت‌شده",
                amountToman = amt,
                category = "مطالبات",
                personOrAccount = extractPersonName(norm),
                isAmbiguous = amt == null || amt <= 0L,
                clarificationQuestionFa = if (amt == null) "مبلغ دقیق طلب و نام طرف حساب چیست؟" else null
            )
        }

        // Check Debt (بدهی)
        if (norm.contains("بدهی") || norm.contains("قرض گرفتم") || norm.contains("بدهکارم") || norm.contains("قسط وام")) {
            val amt = colloquial?.amountToman
            return DetectedIntentResult(
                intentType = DetectedIntentType.DEBT,
                title = colloquial?.extractedTitle ?: "بدهی ثبت‌شده",
                amountToman = amt,
                category = "بدهی و اقساط",
                personOrAccount = extractPersonName(norm),
                isAmbiguous = amt == null || amt <= 0L,
                clarificationQuestionFa = if (amt == null) "مبلغ دقیق بدهی و نام طلبکار یا بانک چیست؟" else null
            )
        }

        // Check Income (درآمد)
        if (norm.contains("حقوق") || norm.contains("درآمد") || norm.contains("واریز شد") || norm.contains("دریافت کردم") || norm.contains("سود")) {
            val amt = colloquial?.amountToman
            return DetectedIntentResult(
                intentType = DetectedIntentType.INCOME,
                title = colloquial?.extractedTitle ?: "درآمد جدید",
                amountToman = amt,
                category = colloquial?.suggestedCategory ?: "حقوق و دستمزد",
                personOrAccount = null,
                isAmbiguous = amt == null || amt <= 0L,
                clarificationQuestionFa = if (amt == null) "مبلغ درآمد دریافتی شما چقدر بوده است؟" else null
            )
        }

        // Default Expense if amount or expense verb present
        if (colloquial != null || norm.contains("خریدم") || norm.contains("خرج") || norm.contains("هزینه") || norm.contains("دادم")) {
            val amt = colloquial?.amountToman
            return DetectedIntentResult(
                intentType = DetectedIntentType.EXPENSE,
                title = colloquial?.extractedTitle ?: norm,
                amountToman = amt,
                category = colloquial?.suggestedCategory ?: "خرید عمومی",
                personOrAccount = null,
                isAmbiguous = amt == null || amt <= 0L,
                clarificationQuestionFa = if (amt == null) "مبلغ این هزینه به تومان چقدر بوده است؟ (مثلاً: ۲۵۰ هزار تومان)" else null
            )
        }

        return DetectedIntentResult(
            intentType = DetectedIntentType.AMBIGUOUS,
            title = input,
            amountToman = null,
            category = "نامشخص",
            personOrAccount = null,
            isAmbiguous = true,
            clarificationQuestionFa = "منظور شما ثبت هزینه، درآمد، بدهی یا وظیفه است؟ لطفاً مبلغ و عنوان را مشخص بفرمایید."
        )
    }

    private fun extractPersonName(norm: String): String {
        val m = Regex("""(?:به|از)\s+([آ-یa-zA-Z]{2,15})""").find(norm)
        return m?.groupValues?.get(1) ?: "طرف حساب"
    }

    // =========================================================================
    // AES-256-GCM Encrypted Backup & CSV Report Helper
    // =========================================================================

    fun encryptBackupAesGcm(plainJson: String, passphrase: String): String {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, 10000, 256)
        val secretKey = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        val cipherBytes = cipher.doFinal(plainJson.toByteArray(Charsets.UTF_8))
        val combined = salt + iv + cipherBytes
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decryptBackupAesGcm(encryptedBase64: String, passphrase: String): String? {
        return try {
            val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            if (combined.size < 29) return null
            val salt = combined.copyOfRange(0, 16)
            val iv = combined.copyOfRange(16, 28)
            val cipherBytes = combined.copyOfRange(28, combined.size)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val spec = PBEKeySpec(passphrase.toCharArray(), salt, 10000, 256)
            val secretKey = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
            String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    fun generateCsvReport(expenses: List<ExpenseItem>, debts: List<DebtItem>): String {
        val sb = StringBuilder()
        sb.appendLine("ID,Title,Type,Amount_Toman,Category,Account_Or_Bank,Date,Is_SMS")
        for (e in expenses) {
            val safeTitle = e.title.replace(",", " ")
            val acc = (e.bankName ?: e.accountName ?: "پیش‌فرض").replace(",", " ")
            sb.appendLine("${e.id},$safeTitle,${e.type.titleFa},${e.amount},${e.category},$acc,${e.date},${e.isSmsParsed}")
        }
        sb.appendLine()
        sb.appendLine("Debt_ID,Title,Type,Person,Total_Toman,Paid_Toman,Remaining_Toman,DueDate,Settled")
        for (d in debts) {
            sb.appendLine("${d.id},${d.title.replace(",", " ")},${d.type.titleFa},${d.personName},${d.totalAmount},${d.paidAmount},${d.remainingAmount},${d.dueDate},${d.isSettled}")
        }
        return sb.toString()
    }

    fun getPersianDateString(): String {
        return "۱۴۰۵/۰۸/۰۵ (۰۵ آبان)"
    }
}
