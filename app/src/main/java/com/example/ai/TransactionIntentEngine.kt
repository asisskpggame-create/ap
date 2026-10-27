package com.example.ai

import com.example.util.PersianDateAndUnitUtils
import java.util.Locale

enum class TransactionIntentType {
    INCOME,
    EXPENSE,
    TRANSFER,
    RECEIVABLE,
    DEBT,
    UNKNOWN
}

data class TransactionIntent(
    val type: TransactionIntentType,
    val amount: Double,
    val currency: String,
    val category: String,
    val account: String = "",
    val person: String = "",
    val title: String = "",
    val isAmbiguous: Boolean = false,
    val confidence: Double = 0.0,
    val reasoning: String = ""
)

/**
 * On-device, deterministic Persian Intent Engine for financial transactions.
 * Acts as the primary brain of Personal Life OS for spoken and typed natural language,
 * delegating to Gemini only when input is ambiguous or confidence is low.
 */
object TransactionIntentEngine {

    fun detectIntent(
        rawText: String,
        defaultCurrency: String = "TOMAN"
    ): TransactionIntent {
        val norm = PersianDateAndUnitUtils.toEnglishDigits(rawText)
            .replace("میلیلیتر", "میلی‌لیتر")
            .replace("پیادهروی", "پیاده‌روی")
            .replace("برنامهریزی", "برنامه‌ریزی")
            .replace("انجامشده", "انجام‌شده")
            .trim()

        if (norm.isBlank()) {
            return TransactionIntent(
                type = TransactionIntentType.UNKNOWN,
                amount = 0.0,
                currency = defaultCurrency,
                category = "نامشخص",
                isAmbiguous = true,
                confidence = 0.0,
                reasoning = "متن ورودی خالی است."
            )
        }

        val lower = norm.lowercase(Locale.ROOT)

        // 1. Determine currency
        val currency = when {
            lower.contains("دلار") || lower.contains("usd") || lower.contains("$") -> "USD"
            lower.contains("یورو") || lower.contains("eur") || lower.contains("€") -> "EUR"
            lower.contains("درهم") || lower.contains("aed") -> "AED"
            lower.contains("پوند") || lower.contains("gbp") || lower.contains("£") -> "GBP"
            lower.contains("ریال") || lower.contains("irr") -> "IRR"
            else -> "TOMAN"
        }

        // 2. Extract numeric amount with multiplier words (میلیون, هزار)
        val extractedAmount = extractSpokenOrWrittenAmount(lower, currency)

        // 3. Extract person if mentioned in loan/debt context (e.g. به علی, از علی, از رضا, از مشتری)
        val personRegex = Regex("""(?:به|از)\s+([آابپتثجچحخدذرزژسشصضطظعغفقکگلمنوهی\s]{2,20}?)(?:\s+(?:قرض|طلب|بدهکار|طلبکار|پول|وام|دادم|گرفتم)|$)""")
        val personMatch = personRegex.find(norm)
        val extractedPerson = personMatch?.groupValues?.get(1)?.trim()?.takeIf {
            !it.contains("حساب") && !it.contains("کارت") && !it.contains("بانک")
        } ?: ""

        // 4. Extract account transition (e.g. از بانک ملت به ملی, از حساب اصلی به پس‌انداز)
        val accountRegex = Regex("""(?:از\s+([^\n]+?)\s+به\s+([^\n]+?)(?:\s+انتقال|\s+کارت|\s+واریز|$))""")
        val accountMatch = accountRegex.find(norm)
        val extractedAccount = if (accountMatch != null) {
            "${accountMatch.groupValues[1].trim()} -> ${accountMatch.groupValues[2].trim()}"
        } else {
            ""
        }

        // 5. Classify Intent Type
        val (type, category, title, confidence, reason) = classifyIntentDetails(
            lower = lower,
            norm = norm,
            amount = extractedAmount,
            person = extractedPerson,
            account = extractedAccount
        )

        val isAmbiguous = (extractedAmount <= 0.0) || (type == TransactionIntentType.UNKNOWN) || (confidence < 0.65)

        return TransactionIntent(
            type = type,
            amount = PersianDateAndUnitUtils.roundFinancialAmount(extractedAmount),
            currency = currency,
            category = category,
            account = extractedAccount,
            person = extractedPerson,
            title = title,
            isAmbiguous = isAmbiguous,
            confidence = confidence,
            reasoning = reason
        )
    }

    private fun extractSpokenOrWrittenAmount(lower: String, currency: String): Double {
        // Match numbers like: 2.5 میلیون, 600 هزار, 300 تومن, 85 هزار
        val complexMillionRegex = Regex("""([0-9]+(?:\.[0-9]+)?)\s*میلیون(?:\s*و\s*([0-9]+(?:\.[0-9]+)?)\s*هزار)?""")
        val cmMatch = complexMillionRegex.find(lower)
        if (cmMatch != null) {
            val millions = cmMatch.groupValues[1].toDoubleOrNull() ?: 0.0
            val thousands = cmMatch.groupValues.getOrNull(2)?.toDoubleOrNull() ?: 0.0
            return (millions * 1_000_000.0) + (thousands * 1_000.0)
        }

        if (lower.contains("یک و نیم میلیون") || lower.contains("یک و نیم ملیون")) {
            return 1_500_000.0
        }
        if (lower.contains("یک میلیون") || lower.contains("یه میلیون")) {
            return 1_000_000.0
        }
        if (lower.contains("پانصد هزار") || lower.contains("پونصد هزار")) {
            return 500_000.0
        }

        val millionRegex = Regex("""([0-9]+(?:\.[0-9]+)?)\s*میلیون""")
        val mMatch = millionRegex.find(lower)
        if (mMatch != null) {
            val base = mMatch.groupValues[1].toDoubleOrNull() ?: 0.0
            return base * 1_000_000.0
        }

        val thousandRegex = Regex("""([0-9]+(?:\.[0-9]+)?)\s*هزار""")
        val tMatch = thousandRegex.find(lower)
        if (tMatch != null) {
            val base = tMatch.groupValues[1].toDoubleOrNull() ?: 0.0
            return base * 1_000.0
        }

        val plainRegex = Regex("""(?<![0-9/:-])([0-9]{1,3}(?:[,،][0-9]{3})+|[0-9]{1,12})(?![0-9/:-])""")
        val pMatch = plainRegex.find(lower)
        if (pMatch != null) {
            val raw = pMatch.groupValues[1].replace(",", "").replace("،", "").toDoubleOrNull() ?: 0.0
            return when {
                // If user said "600 تومن بابت فروش جمینای گرفتم", in colloquial Iranian Persian 600 refers to 600,000 Tomans
                raw in 10.0..999.0 && (lower.contains("تومن") || lower.contains("تومان")) && !lower.contains("تک") -> {
                    raw * 1_000.0
                }
                else -> raw
            }
        }

        return 0.0
    }

    private data class ClassificationDetails(
        val type: TransactionIntentType,
        val category: String,
        val title: String,
        val confidence: Double,
        val reason: String
    )

    private fun classifyIntentDetails(
        lower: String,
        norm: String,
        amount: Double,
        person: String,
        account: String
    ): ClassificationDetails {
        // Transfer between personal accounts
        if (lower.contains("انتقال") || lower.contains("کارت به کارت به حساب خودم") ||
            lower.contains("جابجایی بین حساب") || account.isNotBlank()
        ) {
            return ClassificationDetails(
                type = TransactionIntentType.TRANSFER,
                category = "انتقال بین حسابی",
                title = if (account.isNotBlank()) "انتقال $account" else "انتقال بین حسابی",
                confidence = 0.95,
                reason = "کلمات کلیدی انتقال بین حسابی تشخیص داده شد"
            )
        }

        // Receivable (طلبکاری / قرض دادن به کسی)
        if (lower.contains("طلب دارم") || lower.contains("طلبکارم") ||
            (lower.contains("قرض دادم") && !lower.contains("گرفتم")) ||
            (lower.contains("از") && lower.contains("گرفتم") && person.isNotBlank() && !lower.contains("بابت") && !lower.contains("فروش"))
        ) {
            val title = if (person.isNotBlank()) "طلب از $person" else "طلب / دریافت طلب"
            return ClassificationDetails(
                type = TransactionIntentType.RECEIVABLE,
                category = "طلب و وام",
                title = title,
                confidence = 0.92,
                reason = "جمله دلالت بر طلبکاری یا قرض دادن دارد"
            )
        }

        // Debt (بدهی / قرض گرفتن از کسی)
        if (lower.contains("بدهکارم") || lower.contains("بدهی") ||
            lower.contains("قرض گرفتم") || lower.contains("وام گرفتم") ||
            (lower.contains("به") && lower.contains("دادم") && person.isNotBlank() && lower.contains("قرض"))
        ) {
            val title = if (person.isNotBlank()) "بدهی به $person" else "بدهی و استقراض"
            return ClassificationDetails(
                type = TransactionIntentType.DEBT,
                category = "بدهی و اقساط",
                title = title,
                confidence = 0.92,
                reason = "جمله دلالت بر بدهی یا قرض گرفتن دارد"
            )
        }

        // Income (دریافت / فروش / حقوق / دستمزد / واریز)
        val hasIncomeVerb = lower.contains("گرفتم") || lower.contains("دریافت کردم") ||
            lower.contains("واریز شد") || lower.contains("فروختم") ||
            lower.contains("دستمزد") || lower.contains("حقوق") ||
            lower.contains("پاداش") || lower.contains("سود") ||
            lower.contains("درآمد")

        val isServiceRide = lower.contains("اسنپ گرفتم") || lower.contains("تپسی گرفتم") || lower.contains("تاکسی گرفتم")

        if (hasIncomeVerb && !isServiceRide) {
            val title = when {
                lower.contains("جمینای") || lower.contains("gemini") -> "فروش Gemini"
                lower.contains("طراحی سایت") || lower.contains("پروژه") -> "پروژه و فریلنسری"
                lower.contains("حقوق") -> "حقوق ماهانه"
                lower.contains("مشاوره") -> "خدمات مشاوره"
                lower.contains("سود") -> "سود بانکی و سرمایه"
                lower.contains("فروش") -> "فروش خدمات / کالا"
                else -> "درآمد و واریزی"
            }
            val cat = when {
                lower.contains("حقوق") -> "حقوق و دستمزد"
                lower.contains("سود") -> "سرمایه‌گذاری"
                else -> "کسب درآمد / فروش"
            }
            return ClassificationDetails(
                type = TransactionIntentType.INCOME,
                category = cat,
                title = title,
                confidence = 0.95,
                reason = "کلمات کلیدی صریح دریافت یا فروش کالا/خدمات"
            )
        }

        // Expense (پرداخت / خرید / هزینه / تاکسی / غذا / بنزین / قبض)
        val hasExpenseVerb = lower.contains("پرداخت") || lower.contains("خریدم") ||
            lower.contains("خرج") || isServiceRide ||
            lower.contains("بنزین") || lower.contains("قهوه") ||
            lower.contains("اجاره") || lower.contains("قبض") ||
            lower.contains("کارمزد") || lower.contains("صدقه") ||
            lower.contains("دادم")

        if (hasExpenseVerb) {
            val (cat, title) = when {
                isServiceRide -> "حمل‌ونقل" to "کرایه تاکسی / اسنپ"
                lower.contains("قهوه") || lower.contains("کافه") -> "غذا و رستوران" to "کافه و قهوه"
                lower.contains("غذا") || lower.contains("ناهار") || lower.contains("شام") -> "غذا و رستوران" to "غذا و رستوران"
                lower.contains("سوپرمارکت") || lower.contains("خرید") -> "خرید روزمره" to "خرید سوپرمارکت"
                lower.contains("بنزین") -> "حمل‌ونقل" to "بنزین و سوخت"
                lower.contains("اینترنت") || lower.contains("اشتراک") -> "قبوض و اشتراک" to "اینترنت و اشتراک"
                lower.contains("اجاره") -> "مسکن و اجاره" to "اجاره مسکن"
                lower.contains("دارو") || lower.contains("دکتر") -> "سلامت و درمان" to "دارو و ویزیت پزشکی"
                lower.contains("کارمزد") -> "خدمات بانکی" to "کارمزد بانکی"
                lower.contains("صدقه") -> "بخشش و صدقه" to "صدقه و کار خیر"
                else -> "عمومی" to "پرداخت هزینه"
            }
            return ClassificationDetails(
                type = TransactionIntentType.EXPENSE,
                category = cat,
                title = title,
                confidence = 0.95,
                reason = "کلمات کلیدی صریح پرداخت یا خرید هزینه"
            )
        }

        return ClassificationDetails(
            type = TransactionIntentType.UNKNOWN,
            category = "عمومی",
            title = norm.take(30),
            confidence = 0.30,
            reason = "نوع تراکنش به صورت صریح قابل تشخیص نیست"
        )
    }
}
