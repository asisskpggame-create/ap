package com.example.util

import com.example.data.model.ConsumptionEntry
import com.example.data.model.ExpenseEntry
import com.example.data.model.Habit
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.roundToLong

data class JalaliDate(
    val year: Int,
    val month: Int, // 1..12
    val day: Int    // 1..31
)

object PersianDateAndUnitUtils {

    private val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    private val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

    val jalaliMonthNamesFa = listOf(
        "فروردین", "اردبیهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    val gregorianMonthNamesEn = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    // ISO 1=Mon .. 7=Sun
    private val weekdayNamesFa = mapOf(
        1 to "دوشنبه",
        2 to "سه‌شنبه",
        3 to "چهارشنبه",
        4 to "پنج‌شنبه",
        5 to "جمعه",
        6 to "شنبه",
        7 to "یکشنبه"
    )

    fun toPersianDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(persianDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun toEnglishDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            val pIdx = persianDigits.indexOf(ch)
            val aIdx = arabicDigits.indexOf(ch)
            when {
                pIdx >= 0 -> sb.append(('0'.code + pIdx).toChar())
                aIdx >= 0 -> sb.append(('0'.code + aIdx).toChar())
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    /**
     * Computes the local calendar epoch day (days since 1970-01-01 in local TimeZone, DST-safe).
     */
    fun currentLocalEpochDay(
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long {
        val cal = Calendar.getInstance(timeZone)
        cal.timeInMillis = nowMillis
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        return gregorianToEpochDay(year, month, day)
    }

    fun gregorianToEpochDay(year: Int, month: Int, day: Int): Long {
        val y = year.toLong()
        val m = month.toLong()
        val d = day.toLong()
        val yAdj = if (m <= 2) y - 1 else y
        val era = if (yAdj >= 0) yAdj / 400 else (yAdj - 399) / 400
        val yoe = yAdj - era * 400
        val doy = (153 * (if (m > 2) m - 3 else m + 9) + 2) / 5 + d - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146097 + doe - 719468
    }

    fun epochDayToGregorian(epochDay: Long): Triple<Int, Int, Int> {
        val z = epochDay + 719468
        val era = if (z >= 0) z / 146097 else (z - 146096) / 146097
        val doe = z - era * 146097
        val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
        val y = yoe + era * 400
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val d = (doy - (153 * mp + 2) / 5 + 1).toInt()
        val m = (if (mp < 10) mp + 3 else mp - 9).toInt()
        val year = (if (m <= 2) y + 1 else y).toInt()
        return Triple(year, m, d)
    }

    /**
     * Converts Gregorian (year, month, day) to Solar Hijri / Jalali date.
     */
    fun gregorianToJalali(gy: Int, gm: Int, gd: Int): JalaliDate {
        val gDaysInMonth = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        val gy2 = if (gm > 2) gy + 1 else gy
        var days = 355666 + (365 * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) + ((gy2 + 399) / 400) + gd + gDaysInMonth[gm - 1]
        var jy = -1595 + (33 * (days / 12053))
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            jy += (days - 1) / 365
            days = (days - 1) % 365
        }
        val jm: Int
        val jd: Int
        if (days < 186) {
            jm = 1 + (days / 31)
            jd = 1 + (days % 31)
        } else {
            jm = 7 + ((days - 186) / 30)
            jd = 1 + ((days - 186) % 30)
        }
        return JalaliDate(jy, jm, jd)
    }

    fun epochDayToJalali(epochDay: Long): JalaliDate {
        val (gy, gm, gd) = epochDayToGregorian(epochDay)
        return gregorianToJalali(gy, gm, gd)
    }

    /**
     * ISO day of week for epochDay: 1 = Monday .. 7 = Sunday (1970-01-01 was Thursday = 4).
     */
    fun dayOfWeekIso(epochDay: Long): Int {
        val mod = ((epochDay + 3) % 7).toInt()
        return (if (mod < 0) mod + 7 else mod) + 1
    }

    fun formatEpochDay(
        epochDay: Long,
        calendarDisplay: String = "JALALI",
        usePersianDigits: Boolean = true
    ): String {
        if (epochDay < 0) return "بدون تاریخ"
        val dowName = weekdayNamesFa[dayOfWeekIso(epochDay)] ?: ""
        return if (calendarDisplay == "GREGORIAN") {
            val (y, m, d) = epochDayToGregorian(epochDay)
            val mName = gregorianMonthNamesEn.getOrElse(m - 1) { "$m" }
            val raw = "$dowName، $d $mName $y"
            if (usePersianDigits) toPersianDigits(raw) else raw
        } else {
            val j = epochDayToJalali(epochDay)
            val mName = jalaliMonthNamesFa.getOrElse(j.month - 1) { "${j.month}" }
            val raw = "$dowName، ${j.day} $mName ${j.year}"
            if (usePersianDigits) toPersianDigits(raw) else raw
        }
    }

    fun formatTimeMinutes(minutesFromMidnight: Int, usePersianDigits: Boolean = true): String {
        if (minutesFromMidnight < 0) return ""
        val h = (minutesFromMidnight / 60).coerceIn(0, 23)
        val m = (minutesFromMidnight % 60).coerceIn(0, 59)
        val raw = String.format(Locale.US, "%02d:%02d", h, m)
        return if (usePersianDigits) toPersianDigits(raw) else raw
    }

    // --- Task Recurrence Logic ---

    fun isJalaliLeapYear(jy: Int): Boolean {
        val r = (jy + 2346) % 2820
        val y = r % 128
        val leapYearsInCycle = setOf(
            0, 4, 8, 12, 16, 20, 24, 29, 33, 37, 41, 45, 49, 53, 57, 62,
            66, 70, 74, 78, 82, 86, 90, 95, 99, 103, 107, 111, 115, 119, 124
        )
        return y in leapYearsInCycle
    }

    /**
     * Calculates the next due epochDay when a recurring task is completed.
     * Returns null if recurrenceRule is NONE.
     */
    fun calculateNextOccurrenceEpochDay(
        currentDueEpochDay: Long,
        recurrenceRule: String,
        recurrenceIntervalDays: Int = 1,
        fallbackTodayEpochDay: Long = currentLocalEpochDay()
    ): Long? {
        val baseDay = if (currentDueEpochDay >= 0) currentDueEpochDay else fallbackTodayEpochDay
        return when (recurrenceRule.uppercase(Locale.US)) {
            "DAILY" -> baseDay + 1L
            "WEEKLY" -> baseDay + 7L
            "MONTHLY" -> {
                val j = epochDayToJalali(baseDay)
                val daysToAdd = when (j.month) {
                    in 1..6 -> 31L
                    in 7..11 -> 30L
                    12 -> if (isJalaliLeapYear(j.year)) 30L else 29L
                    else -> 30L
                }
                baseDay + daysToAdd
            }
            "CUSTOM" -> {
                val interval = if (recurrenceIntervalDays > 0) recurrenceIntervalDays else 1
                baseDay + interval.toLong()
            }
            else -> null
        }
    }

    // --- Compatible Unit Conversion Logic ---

    enum class UnitFamily { VOLUME, WEIGHT, DURATION, COUNT, UNKNOWN }

    fun normalizeUnit(unit: String): String {
        return when (unit.trim().lowercase(Locale.US)) {
            "ml", "milliliter", "milliliters", "میلی‌لیتر", "میلیلیتر", "سی‌سی" -> "ml"
            "l", "liter", "liters", "لیتر" -> "liters"
            "cup", "cups", "لیوان", "فنجان" -> "cups"
            "g", "gram", "grams", "گرم" -> "grams"
            "kg", "kilogram", "kilograms", "کیلو", "کیلوگرم" -> "kilograms"
            "min", "minute", "minutes", "دقیقه" -> "minutes"
            "h", "hr", "hour", "hours", "ساعت" -> "hours"
            "piece", "pieces", "عدد", "دانه" -> "pieces"
            "serving", "servings", "پرس", "وعده" -> "servings"
            "tablet", "tablets", "قرص", "کپسول" -> "tablets"
            else -> unit.trim().lowercase(Locale.US)
        }
    }

    fun getUnitFamily(unit: String): UnitFamily {
        return when (normalizeUnit(unit)) {
            "ml", "liters", "cups" -> UnitFamily.VOLUME
            "grams", "kilograms" -> UnitFamily.WEIGHT
            "minutes", "hours" -> UnitFamily.DURATION
            "pieces", "servings", "tablets" -> UnitFamily.COUNT
            else -> UnitFamily.UNKNOWN
        }
    }

    /**
     * Converts quantity between compatible units within the same family.
     * Returns null if units are incompatible (never combines incompatible units).
     */
    fun convertUnit(quantity: Double, fromUnit: String, toUnit: String): Double? {
        val normFrom = normalizeUnit(fromUnit)
        val normTo = normalizeUnit(toUnit)
        if (normFrom == normTo) return quantity

        val fromFamily = getUnitFamily(normFrom)
        val toFamily = getUnitFamily(normTo)
        if (fromFamily == UnitFamily.UNKNOWN || fromFamily != toFamily) return null

        return when (fromFamily) {
            UnitFamily.VOLUME -> {
                val inMl = when (normFrom) {
                    "ml" -> quantity
                    "liters" -> quantity * 1000.0
                    "cups" -> quantity * 240.0
                    else -> return null
                }
                when (normTo) {
                    "ml" -> inMl
                    "liters" -> inMl / 1000.0
                    "cups" -> inMl / 240.0
                    else -> null
                }
            }
            UnitFamily.WEIGHT -> {
                val inGrams = when (normFrom) {
                    "grams" -> quantity
                    "kilograms" -> quantity * 1000.0
                    else -> return null
                }
                when (normTo) {
                    "grams" -> inGrams
                    "kilograms" -> inGrams / 1000.0
                    else -> null
                }
            }
            UnitFamily.DURATION -> {
                val inMinutes = when (normFrom) {
                    "minutes" -> quantity
                    "hours" -> quantity * 60.0
                    else -> return null
                }
                when (normTo) {
                    "minutes" -> inMinutes
                    "hours" -> inMinutes / 60.0
                    else -> null
                }
            }
            UnitFamily.COUNT -> {
                // Count units (pieces vs tablets vs servings) only combine if exact normalized match
                if (normFrom == normTo) quantity else null
            }
            UnitFamily.UNKNOWN -> null
        }
    }

    /**
     * Calculates total consumption in a target unit for entries matching a category/predicate,
     * converting compatible units and ignoring incompatible units.
     */
    fun sumCompatibleConsumption(
        entries: List<ConsumptionEntry>,
        targetUnit: String
    ): Double {
        var sum = 0.0
        for (entry in entries) {
            val converted = convertUnit(entry.quantity, entry.unit, targetUnit)
            if (converted != null) {
                sum += converted
            }
        }
        return sum
    }

    // --- Expense & Multi-Currency Calculations ---

    /**
     * Aggregates expenses (excluding income, debt, and receivable entries) strictly grouped by currency.
     * Never sums different currencies together without an explicit exchange rate.
     */
    fun aggregateExpensesByCurrency(expenses: List<ExpenseEntry>): Map<String, Double> {
        val result = linkedMapOf<String, Double>()
        for (expense in expenses) {
            if (expense.isExpense) {
                val curr = expense.currency.uppercase(Locale.US)
                result[curr] = (result[curr] ?: 0.0) + expense.amount
            }
        }
        return result
    }

    /**
     * Aggregates income entries strictly grouped by currency.
     */
    fun aggregateIncomesByCurrency(entries: List<ExpenseEntry>): Map<String, Double> {
        val result = linkedMapOf<String, Double>()
        for (entry in entries) {
            if (entry.isIncome) {
                val curr = entry.currency.uppercase(Locale.US)
                result[curr] = (result[curr] ?: 0.0) + entry.amount
            }
        }
        return result
    }

    /**
     * Aggregates active (unsettled) debt entries (بدهکاری) strictly grouped by currency using remainingAmount.
     */
    fun aggregateDebtsByCurrency(entries: List<ExpenseEntry>): Map<String, Double> {
        val result = linkedMapOf<String, Double>()
        for (entry in entries) {
            if (entry.isDebt && !entry.isSettled) {
                val curr = entry.currency.uppercase(Locale.US)
                result[curr] = (result[curr] ?: 0.0) + entry.remainingAmount
            }
        }
        return result
    }

    /**
     * Aggregates active (unsettled) receivable entries (طلبکاری) strictly grouped by currency using remainingAmount.
     */
    fun aggregateReceivablesByCurrency(entries: List<ExpenseEntry>): Map<String, Double> {
        val result = linkedMapOf<String, Double>()
        for (entry in entries) {
            if (entry.isReceivable && !entry.isSettled) {
                val curr = entry.currency.uppercase(Locale.US)
                result[curr] = (result[curr] ?: 0.0) + entry.remainingAmount
            }
        }
        return result
    }

    /**
     * Aggregates net balance (Income - Expense) strictly grouped by currency.
     * Personal account transfers (isTransfer), debts, and receivables are excluded.
     */
    fun aggregateNetBalanceByCurrency(entries: List<ExpenseEntry>): Map<String, Double> {
        val result = linkedMapOf<String, Double>()
        for (entry in entries) {
            if (entry.isTransfer || (!entry.isIncome && !entry.isExpense)) continue
            val curr = entry.currency.uppercase(Locale.US)
            val signed = if (entry.isIncome) entry.amount else -entry.amount
            result[curr] = (result[curr] ?: 0.0) + signed
        }
        return result
    }

    /**
     * Converts expenses to a single target currency ONLY when explicit exchange rates are provided.
     * Returns null if any expense currency lacks a documented rate in [ratesToTarget].
     */
    fun sumExpensesWithExchangeRates(
        expenses: List<ExpenseEntry>,
        targetCurrency: String,
        ratesToTarget: Map<String, Double>
    ): Double? {
        var total = 0.0
        val normTarget = targetCurrency.uppercase(Locale.US)
        for (expense in expenses) {
            if (!expense.isExpense) continue
            val curr = expense.currency.uppercase(Locale.US)
            if (curr == normTarget) {
                total += expense.amount
            } else {
                val rate = ratesToTarget[curr] ?: return null
                if (rate <= 0.0) return null
                total += expense.amount * rate
            }
        }
        return total
    }

    data class CurrencyWheelOption(
        val unitCode: String,      // "TOMAN", "HEZAR_TOMAN", "MILLION_TOMAN", "IRR", "USD", "EUR", "AED"
        val labelFa: String,       // "تومان", "هزار تومان", "میلیون تومان", "ریال", ...
        val shortBadge: String,    // "تومان", "×هزار", "×میلیون", "ریال", "$", "€", "AED"
        val storedCurrency: String,// "TOMAN", "IRR", "USD", "EUR", "AED"
        val multiplierToStored: Double = 1.0
    )

    val currencyWheelOptions: List<CurrencyWheelOption> = listOf(
        CurrencyWheelOption("TOMAN", "تومان", "تومان", "TOMAN", 1.0),
        CurrencyWheelOption("HEZAR_TOMAN", "هزار تومان", "هزار تومان", "TOMAN", 1_000.0),
        CurrencyWheelOption("MILLION_TOMAN", "میلیون تومان", "میلیون تومان", "TOMAN", 1_000_000.0),
        CurrencyWheelOption("IRR", "ریال", "ریال", "IRR", 1.0),
        CurrencyWheelOption("USD", "دلار ($)", "$", "USD", 1.0),
        CurrencyWheelOption("EUR", "یورو (€)", "€", "EUR", 1.0),
        CurrencyWheelOption("AED", "درهم (AED)", "درهم", "AED", 1.0)
    )

    /**
     * Resolves a user-entered numeric amount and selected wheel unit into (finalAmount, storedCurrency).
     * Golden Rule:
     * - Selecting "TOMAN" NEVER multiplies by 1,000 (1000 + TOMAN -> 1000.0 TOMAN).
     * - Selecting "HEZAR_TOMAN" explicitly multiplies by 1,000 (600 + HEZAR_TOMAN -> 600,000.0 TOMAN).
     * - Selecting "MILLION_TOMAN" explicitly multiplies by 1,000,000 (5 + MILLION_TOMAN -> 5,000,000.0 TOMAN).
     * - 1 Toman = 10 Rials (IRR).
     */
    fun resolveEnteredAmountAndCurrency(
        rawInputAmount: Double,
        selectedUnitCode: String
    ): Pair<Double, String> {
        if (rawInputAmount <= 0.0) return 0.0 to "TOMAN"
        val code = selectedUnitCode.trim().uppercase(Locale.US)
        return when (code) {
            "TOMAN" -> rawInputAmount to "TOMAN"
            "HEZAR_TOMAN" -> (rawInputAmount * 1_000.0) to "TOMAN"
            "MILLION_TOMAN" -> (rawInputAmount * 1_000_000.0) to "TOMAN"
            "IRR_TO_TOMAN" -> (rawInputAmount / 10.0) to "TOMAN"
            "IRR" -> rawInputAmount to "IRR"
            "USD", "EUR", "GBP", "AED" -> rawInputAmount to code
            else -> rawInputAmount to code
        }
    }

    /**
     * Enforces the financial decimal precision contract:
     * - Rounds to 2 decimal places to eliminate floating-point drift (e.g. 0.1 + 0.2 != 0.3).
     * - Handles NaN, Infinite, and negative zero safely.
     */
    fun roundFinancialAmount(amount: Double): Double {
        if (amount.isNaN() || amount.isInfinite()) return 0.0
        val clamped = amount.coerceAtLeast(0.0)
        return kotlin.math.round(clamped * 100.0) / 100.0
    }

    /**
     * Converts between Iranian currency units using the exact legal ratio:
     * 1 Toman = 10 Rials (10 IRR = 1 TOMAN).
     * Returns null if conversion between foreign currencies is requested without an exchange rate.
     */
    fun convertCurrencyExact(
        amount: Double,
        fromUnit: String,
        toUnit: String
    ): Double? {
        val from = fromUnit.trim().uppercase(Locale.US)
        val to = toUnit.trim().uppercase(Locale.US)
        if (from == to) return amount

        val inToman: Double = when (from) {
            "TOMAN" -> amount
            "HEZAR_TOMAN" -> amount * 1_000.0
            "MILLION_TOMAN" -> amount * 1_000_000.0
            "IRR", "RIAL" -> amount / 10.0
            else -> return null
        }

        return when (to) {
            "TOMAN" -> inToman
            "HEZAR_TOMAN" -> inToman / 1_000.0
            "MILLION_TOMAN" -> inToman / 1_000_000.0
            "IRR", "RIAL" -> inToman * 10.0
            else -> null
        }
    }

    /**
     * Normalizes explicit unit multipliers in Persian text ONLY when "هزار", "میلیون", or "میلیارد"
     * is explicitly present in [rawTextContext].
     *
     * CRITICAL RULE:
     * - Never multiplies numbers when [rawTextContext] is blank or only contains "تومان" / "تومن"!
     * - Entering 1000 with unit "TOMAN" stays 1,000 TOMAN (never becomes 1,000,000).
     * - Saying "۶۰۰ هزار تومان" scales 600 by 1,000 -> 600,000 TOMAN.
     * - Saying "۵ میلیون تومان" scales 5 by 1,000,000 -> 5,000,000 TOMAN.
     */
    fun normalizeColloquialTomanAmount(
        rawAmount: Double,
        currency: String,
        rawTextContext: String = ""
    ): Double {
        if (rawAmount <= 0.0) return 0.0
        if (rawTextContext.isBlank()) return rawAmount

        val hasBillion = rawTextContext.contains("میلیارد")
        val hasMillion = rawTextContext.contains("میلیون") || rawTextContext.contains("ملیون")
        val hasThousand = rawTextContext.contains("هزار")

        if (hasBillion && rawAmount < 1_000_000.0) {
            return rawAmount * 1_000_000_000.0
        }
        if (hasMillion && rawAmount < 1_000_000.0) {
            return rawAmount * 1_000_000.0
        }
        if (hasThousand && rawAmount < 1_000_000.0) {
            return rawAmount * 1_000.0
        }
        return rawAmount
    }

    private val persianWordNumbers: List<Pair<String, Double>> = listOf(
        "نهصد" to 900.0,
        "هشتصد" to 800.0,
        "هفتصد" to 700.0,
        "ششصد" to 600.0,
        "شیشصد" to 600.0,
        "پانصد" to 500.0,
        "پونصد" to 500.0,
        "چهارصد" to 400.0,
        "چارصد" to 400.0,
        "سیصد" to 300.0,
        "دویست" to 200.0,
        "صد و پنجاه" to 150.0,
        "یکصد" to 100.0,
        "صد" to 100.0,
        "نود" to 90.0,
        "هشتاد" to 80.0,
        "هفتاد" to 70.0,
        "شصت" to 60.0,
        "پنجاه" to 50.0,
        "چهل" to 40.0,
        "سی" to 30.0,
        "بیست و پنج" to 25.0,
        "بیست" to 20.0,
        "پانزده" to 15.0,
        "پونزده" to 15.0,
        "چهارده" to 14.0,
        "سیزده" to 13.0,
        "دوازده" to 12.0,
        "یازده" to 11.0,
        "ده" to 10.0,
        "نه" to 9.0,
        "هشت" to 8.0,
        "هفت" to 7.0,
        "شش" to 6.0,
        "شیش" to 6.0,
        "پنج" to 5.0,
        "چهار" to 4.0,
        "سه" to 3.0,
        "دو" to 2.0,
        "یک" to 1.0,
        "یه" to 1.0
    )

    private fun parseSpokenOrNumericToken(token: String): Double? {
        val clean = toEnglishDigits(token).replace(",", "").replace("،", "").trim()
        clean.toDoubleOrNull()?.let { return it }
        if (clean.contains("یک و نیم") || clean.contains("یه و نیم")) return 1.5
        if (clean.contains("دو و نیم")) return 2.5
        if (clean.contains("نیم")) return 0.5
        var sum = 0.0
        var matched = false
        var working = clean
        for ((word, value) in persianWordNumbers) {
            val regex = Regex("""(?:^|\s|\b)${Regex.escape(word)}(?:\s|\b|$)""")
            if (regex.containsMatchIn(working)) {
                sum += value
                matched = true
                working = working.replaceFirst(word, " ")
            }
        }
        return if (matched && sum > 0.0) sum else null
    }

    /**
     * Extracts a numeric amount from a Persian spoken or typed clause, supporting:
     * - Digits with commas ("500,000", "1,000", "1000")
     * - Compound million/thousand phrases ("۲ میلیون و ۵۰۰ هزار", "یک میلیون و دویست هزار", "یک و نیم میلیون")
     * - Spoken Persian number words ("ششصد هزار تومان", "پانصد هزار تومن", "یک میلیون تومان")
     * - Strict adherence to unit words: "۱۰۰۰ تومان" -> 1000.0, "۶۰۰ هزار تومان" -> 600,000.0.
     */
    fun extractSpokenOrTypedCurrencyAmount(clause: String, currency: String): Double? {
        val norm = toEnglishDigits(clause)
            .replace('ي', 'ی')
            .replace('ك', 'ک')
            .replace(",", "")
            .replace("،", "")
            .replace("٬", "")
            .trim()

        // 1. Check "یک و نیم میلیون" / "نیم میلیون" / "نیم میلیارد"
        if (norm.contains("یک و نیم میلیون") || norm.contains("یه و نیم میلیون") || norm.contains("1.5 میلیون")) {
            return 1_500_000.0
        }
        if (norm.contains("دو و نیم میلیون") || norm.contains("2.5 میلیون")) {
            return 2_500_000.0
        }
        if (norm.contains("نیم میلیون") || norm.contains("نیم ملیون")) {
            return 500_000.0
        }

        // 2. Check compound "X میلیون و Y (هزار)?" (both digits and words, e.g. "2 میلیون و 500 هزار" or "دو میلیون و پانصد هزار")
        val compoundSplit = norm.split(Regex("""\s*(?:میلیون|ملیون)\s+و\s*"""), limit = 2)
        if (compoundSplit.size == 2) {
            val leftPart = compoundSplit[0].trim().split(Regex("""\s+""")).takeLast(3).joinToString(" ")
            val rightPartRaw = compoundSplit[1].trim()
            val millionsVal = parseSpokenOrNumericToken(leftPart)
            val rightTokens = rightPartRaw
                .replace(Regex("""(هزار|تومان|تومن|ریال).*"""), "")
                .trim()
            val remainderVal = parseSpokenOrNumericToken(rightTokens)
            if (millionsVal != null && millionsVal > 0.0) {
                val remScaled = when {
                    remainderVal == null -> 0.0
                    remainderVal < 1000.0 -> remainderVal * 1_000.0
                    else -> remainderVal
                }
                return millionsVal * 1_000_000.0 + remScaled
            }
        }

        // 3. Check explicit digits in the text
        val numberRegex = Regex("""(\d+(?:\.\d+)?)""")
        val firstNum = numberRegex.find(norm)?.groupValues?.get(1)?.toDoubleOrNull()
        if (firstNum != null && firstNum > 0.0) {
            return normalizeColloquialTomanAmount(firstNum, currency, norm)
        }

        // 4. Check standalone "هزار تومان" / "میلیون تومان" without preceding digit
        val beforeUnit = norm
            .replace(Regex("""(میلیارد|میلیون|ملیون|هزار|تومان|تومن|ریال|دلار|یورو|درهم).*"""), "")
            .trim()
            .split(Regex("""\s+"""))
            .takeLast(4)
            .joinToString(" ")
        val spokenVal = parseSpokenOrNumericToken(beforeUnit) ?: parseSpokenOrNumericToken(norm)
        if (spokenVal != null && spokenVal > 0.0) {
            return normalizeColloquialTomanAmount(spokenVal, currency, norm)
        }

        if (norm.contains("میلیون تومان") || norm.contains("میلیون تومن")) return 1_000_000.0
        if (norm.contains("هزار تومان") || norm.contains("هزار تومن")) return 1_000.0

        return null
    }

    /**
     * Parses relative Persian date expressions into day offsets from [todayEpochDay]:
     * - "امروز" / "امشب" -> 0
     * - "دیروز" / "دیشب" -> -1
     * - "پریروز" -> -2
     * - "فردا" -> +1
     * - "پس‌فردا" / "پس فردا" -> +2
     * - "هفته بعد" / "هفته آینده" -> +7
     * - "هفته پیش" / "هفته قبل" -> -7
     * - "آخر ماه" -> days remaining until the end of the current Jalali month
     */
    fun extractRelativeDateOffsetDays(text: String, todayEpochDay: Long = currentLocalEpochDay()): Int {
        val norm = text.replace('ي', 'ی').replace('ك', 'ک')
        return when {
            norm.contains("پس‌فردا") || norm.contains("پس فردا") -> 2
            norm.contains("پریروز") -> -2
            norm.contains("فردا") -> 1
            norm.contains("دیروز") || norm.contains("دیشب") -> -1
            norm.contains("هفته بعد") || norm.contains("هفته آینده") -> 7
            norm.contains("هفته پیش") || norm.contains("هفته قبل") || norm.contains("هفته گذشته") -> -7
            norm.contains("ماه بعد") || norm.contains("ماه آینده") -> 30
            norm.contains("آخر ماه") || norm.contains("پایان ماه") -> {
                val jalali = epochDayToJalali(todayEpochDay)
                val maxDayOfMonth = when (jalali.month) {
                    in 1..6 -> 31
                    in 7..11 -> 30
                    12 -> if (isJalaliLeapYear(jalali.year)) 30 else 29
                    else -> 30
                }
                (maxDayOfMonth - jalali.day).coerceAtLeast(0)
            }
            else -> 0
        }
    }

    // --- Telegram-Style Frequent Transaction Titles Engine ---

    data class FrequentTransactionTitle(
        val title: String,
        val emoji: String,
        val transactionType: String, // "EXPENSE" or "INCOME"
        val category: String,
        val usageCount: Int = 1,
        val isPinned: Boolean = false
    )

    fun resolveEmojiForTitle(title: String, category: String = "", transactionType: String = "EXPENSE"): String {
        val t = title.lowercase(Locale.US)
        return when {
            t.contains("gemini") || t.contains("جمینای") || t.contains("هوش مصنوعی") || t.contains("ai") -> "🤖"
            t.contains("غذا") || t.contains("ناهار") || t.contains("شام") || t.contains("ساندویچ") || t.contains("پیتزا") || t.contains("رستوران") -> "🍔"
            t.contains("قهوه") || t.contains("اسپرسو") || t.contains("لاته") || t.contains("کافه") || t.contains("چای") -> "☕"
            t.contains("تاکسی") || t.contains("اسنپ") || t.contains("تپسی") || t.contains("مترو") || t.contains("کرایه") -> "🚕"
            t.contains("بنزین") || t.contains("سوخت") || t.contains("ماشین") || t.contains("خودرو") -> "⛽"
            t.contains("خرید") || t.contains("سوپرمارکت") || t.contains("فروشگاه") || t.contains("میوه") || t.contains("نان") -> "🛒"
            t.contains("حقوق") || t.contains("دستمزد") || t.contains("یاری") || t.contains("واریز") -> "💰"
            t.contains("فروش") || t.contains("پروژه") || t.contains("خدمات") || t.contains("فریلنس") || t.contains("طراحی") || t.contains("برنامه") -> "💻"
            t.contains("قبض") || t.contains("اجاره") || t.contains("شارژ") || t.contains("اینترنت") || t.contains("اشتراک") -> "🧾"
            t.contains("دارو") || t.contains("دکتر") || t.contains("پزشک") || t.contains("درمان") || t.contains("آزمایش") -> "💊"
            t.contains("ورزش") || t.contains("باشگاه") -> "🏋️"
            t.contains("کتاب") || t.contains("آموزش") || t.contains("دوره") -> "📚"
            t.contains("سود") || t.contains("سرمایه") || t.contains("طلا") || t.contains("سهام") -> "📈"
            t.contains("هدیه") || t.contains("پاداش") || t.contains("عیدی") -> "🎁"
            transactionType.equals("INCOME", ignoreCase = true) -> "💵"
            category.contains("غذا") -> "🍔"
            category.contains("حمل") -> "🚕"
            category.contains("خرید") -> "🛒"
            category.contains("قبوض") -> "🧾"
            category.contains("سلامت") -> "💊"
            else -> "💳"
        }
    }

    val defaultFrequentTitles: List<FrequentTransactionTitle> = listOf(
        // EXPENSE (پرداخت) defaults
        FrequentTransactionTitle("غذا", "🍔", "EXPENSE", "غذا و کافه", usageCount = 12, isPinned = true),
        FrequentTransactionTitle("تاکسی", "🚕", "EXPENSE", "حمل‌ونقل", usageCount = 10, isPinned = true),
        FrequentTransactionTitle("خرید", "🛒", "EXPENSE", "خرید", usageCount = 9, isPinned = true),
        FrequentTransactionTitle("قهوه", "☕", "EXPENSE", "غذا و کافه", usageCount = 7, isPinned = false),
        FrequentTransactionTitle("قبض و اشتراک", "🧾", "EXPENSE", "قبوض و اجاره", usageCount = 6, isPinned = false),
        FrequentTransactionTitle("بنزین", "⛽", "EXPENSE", "حمل‌ونقل", usageCount = 5, isPinned = false),
        FrequentTransactionTitle("دارو و درمان", "💊", "EXPENSE", "سلامت و درمان", usageCount = 4, isPinned = false),
        // INCOME (دریافت) defaults
        FrequentTransactionTitle("فروش Gemini", "🤖", "INCOME", "سرمایه‌گذاری", usageCount = 12, isPinned = true),
        FrequentTransactionTitle("فروش خدمات", "💻", "INCOME", "سرمایه‌گذاری", usageCount = 10, isPinned = true),
        FrequentTransactionTitle("حقوق", "💰", "INCOME", "سایر", usageCount = 9, isPinned = true),
        FrequentTransactionTitle("پروژه آزاد", "💻", "INCOME", "سرمایه‌گذاری", usageCount = 6, isPinned = false),
        FrequentTransactionTitle("سود بانکی", "📈", "INCOME", "سرمایه‌گذاری", usageCount = 5, isPinned = false),
        FrequentTransactionTitle("پاداش", "🎁", "INCOME", "سایر", usageCount = 4, isPinned = false)
    )

    /**
     * Ranks and filters frequent transaction titles strictly by [transactionType] ("EXPENSE" vs "INCOME"),
     * combining saved/pinned items with actual historical transaction usage counts and excluding deleted titles.
     */
    fun getRankedFrequentTitlesForType(
        transactionType: String,
        historicalExpenses: List<ExpenseEntry>,
        customFrequentItems: List<FrequentTransactionTitle> = defaultFrequentTitles,
        deletedTitlesForType: Set<String> = emptySet()
    ): List<FrequentTransactionTitle> {
        val normType = if (transactionType.equals("INCOME", ignoreCase = true)) "INCOME" else "EXPENSE"
        val deletedNorm = deletedTitlesForType.map { it.trim().lowercase(Locale.US) }.toSet()
        fun isDeletedKey(key: String): Boolean {
            return key in deletedNorm || "${normType.lowercase(Locale.US)}|$key" in deletedNorm
        }

        val mergedMap = linkedMapOf<String, FrequentTransactionTitle>()

        // 1. Seed from custom/default frequent items matching normType
        for (item in customFrequentItems) {
            val itemType = if (item.transactionType.equals("INCOME", ignoreCase = true)) "INCOME" else "EXPENSE"
            if (itemType != normType) continue
            val key = item.title.trim().lowercase(Locale.US)
            if (key.isBlank() || isDeletedKey(key)) continue
            mergedMap[key] = item.copy(transactionType = normType)
        }

        // 2. Learn from actual historical transactions of the same type
        for (entry in historicalExpenses) {
            val entryType = when {
                entry.isIncome -> "INCOME"
                entry.isExpense -> "EXPENSE"
                else -> continue
            }
            if (entryType != normType) continue
            val cleanTitle = entry.title.trim()
            val key = cleanTitle.lowercase(Locale.US)
            if (key.isBlank() || isDeletedKey(key)) continue

            val existing = mergedMap[key]
            if (existing != null) {
                mergedMap[key] = existing.copy(usageCount = existing.usageCount + 1)
            } else {
                mergedMap[key] = FrequentTransactionTitle(
                    title = cleanTitle,
                    emoji = resolveEmojiForTitle(cleanTitle, entry.category, normType),
                    transactionType = normType,
                    category = entry.category.ifBlank { "سایر" },
                    usageCount = 1,
                    isPinned = false
                )
            }
        }

        return mergedMap.values.sortedWith(
            compareByDescending<FrequentTransactionTitle> { it.isPinned }
                .thenByDescending { it.usageCount }
                .thenBy { it.title }
        )
    }

    fun formatCurrencyAmount(amount: Double, currency: String, usePersianDigits: Boolean = true): String {
        val formattedNum = if (abs(amount - amount.roundToLong()) < 0.005) {
            String.format(Locale.US, "%,d", amount.roundToLong())
        } else {
            String.format(Locale.US, "%,.2f", amount)
        }
        val numStr = if (usePersianDigits) toPersianDigits(formattedNum) else formattedNum
        val label = when (currency.uppercase(Locale.US)) {
            "TOMAN" -> "تومان"
            "HEZAR_TOMAN" -> "هزار تومان"
            "MILLION_TOMAN" -> "میلیون تومان"
            "IRR" -> "ریال"
            "EUR" -> "€"
            "USD" -> "$"
            "GBP" -> "£"
            "AED" -> "درهم"
            else -> currency
        }
        return "$numStr $label"
    }

    // --- Habit Streak Calculation ---

    /**
     * Calculates the current streak for a habit according to its schedule (`DAILY`, `WEEKLY_DAYS`, `TIMES_PER_WEEK`),
     * rather than assuming every habit must be completed every day.
     */
    fun calculateHabitStreak(
        habit: Habit,
        completedEpochDays: Set<Long>,
        todayEpochDay: Long,
        weekStartDay: Int = 6 // 6 = Saturday
    ): Int {
        if (completedEpochDays.isEmpty()) return 0

        return when (habit.scheduleType.uppercase(Locale.US)) {
            "DAILY" -> {
                var streak = 0
                var cursor = if (todayEpochDay in completedEpochDays) todayEpochDay else todayEpochDay - 1L
                while (cursor in completedEpochDays) {
                    streak++
                    cursor--
                }
                streak
            }
            "WEEKLY_DAYS" -> {
                val allowedDays = if (habit.scheduleDays.isEmpty()) (1..7).toSet() else habit.scheduleDays.toSet()
                var streak = 0
                var cursor = todayEpochDay
                // If today is a scheduled day and not yet completed today, start checking from yesterday
                if (dayOfWeekIso(cursor) in allowedDays && cursor !in completedEpochDays) {
                    cursor--
                }
                var lookbackLimit = 366 * 2
                while (lookbackLimit > 0) {
                    val dow = dayOfWeekIso(cursor)
                    if (dow in allowedDays) {
                        if (cursor in completedEpochDays) {
                            streak++
                        } else {
                            break
                        }
                    }
                    cursor--
                    lookbackLimit--
                }
                streak
            }
            "TIMES_PER_WEEK" -> {
                val target = habit.targetPerPeriod.coerceAtLeast(1)
                // Group days by week index aligned to weekStartDay
                fun weekStartFor(epochDay: Long): Long {
                    val dow = dayOfWeekIso(epochDay)
                    val diff = (dow - weekStartDay + 7) % 7
                    return epochDay - diff
                }
                val currentWeekStart = weekStartFor(todayEpochDay)
                var weekCursor = currentWeekStart
                var streakWeeks = 0

                // Count completions in current week
                val currentWeekCount = (0L..6L).count { (weekCursor + it) in completedEpochDays }
                if (currentWeekCount >= target) {
                    streakWeeks++
                }
                weekCursor -= 7L
                var maxWeeks = 104
                while (maxWeeks > 0) {
                    val count = (0L..6L).count { (weekCursor + it) in completedEpochDays }
                    if (count >= target) {
                        streakWeeks++
                        weekCursor -= 7L
                        maxWeeks--
                    } else {
                        break
                    }
                }
                streakWeeks
            }
            else -> 0
        }
    }
}
