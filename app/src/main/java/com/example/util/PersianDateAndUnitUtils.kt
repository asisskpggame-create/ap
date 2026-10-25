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
     * Aggregates active (unsettled) debt entries (بدهکاری) strictly grouped by currency.
     */
    fun aggregateDebtsByCurrency(entries: List<ExpenseEntry>): Map<String, Double> {
        val result = linkedMapOf<String, Double>()
        for (entry in entries) {
            if (entry.isDebt && !entry.isSettled) {
                val curr = entry.currency.uppercase(Locale.US)
                result[curr] = (result[curr] ?: 0.0) + entry.amount
            }
        }
        return result
    }

    /**
     * Aggregates active (unsettled) receivable entries (طلبکاری) strictly grouped by currency.
     */
    fun aggregateReceivablesByCurrency(entries: List<ExpenseEntry>): Map<String, Double> {
        val result = linkedMapOf<String, Double>()
        for (entry in entries) {
            if (entry.isReceivable && !entry.isSettled) {
                val curr = entry.currency.uppercase(Locale.US)
                result[curr] = (result[curr] ?: 0.0) + entry.amount
            }
        }
        return result
    }

    /**
     * Aggregates net balance (Income - Expense) strictly grouped by currency.
     */
    fun aggregateNetBalanceByCurrency(entries: List<ExpenseEntry>): Map<String, Double> {
        val result = linkedMapOf<String, Double>()
        for (entry in entries) {
            if (!entry.isIncome && !entry.isExpense) continue
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

    /**
     * Normalizes colloquial Iranian Toman amounts:
     * In everyday spoken/written Persian in Iran, saying "۵۰۰ تومان" or "۹۰ تومن" for an expense
     * always means 500,000 Toman (۵۰۰ هزار تومان) or 90,000 Toman (۹۰ هزار تومان),
     * because amounts 1..999 single Tomans do not exist in real-world transactions.
     * Foreign currencies (EUR, USD, GBP, AED) are never multiplied unless "هزار" or "میلیون" is spoken.
     */
    fun normalizeColloquialTomanAmount(
        rawAmount: Double,
        currency: String,
        rawTextContext: String = ""
    ): Double {
        if (rawAmount <= 0.0) return 0.0
        val normCurrency = currency.uppercase(Locale.US)
        val hasMillion = rawTextContext.contains("میلیون") || rawTextContext.contains("ملیون")
        val hasThousand = rawTextContext.contains("هزار") || rawTextContext.contains("تومن") || rawTextContext.contains("تومان")

        if (hasMillion && rawAmount < 10_000.0) {
            return rawAmount * 1_000_000.0
        }
        if (rawTextContext.contains("هزار") && rawAmount < 1_000.0) {
            return rawAmount * 1_000.0
        }
        // In Iran, any TOMAN amount between 1 and 999 (e.g., "۵۰۰ تومان سیگار خریدم" or "۹۰ تومن قهوه")
        // is colloquially in thousands of Tomans (× 1,000 -> 500,000 Toman).
        if (normCurrency == "TOMAN" && rawAmount >= 1.0 && rawAmount < 1_000.0 && (hasThousand || rawTextContext.isBlank())) {
            return rawAmount * 1_000.0
        }
        return rawAmount
    }

    /**
     * Extracts a numeric amount from a Persian spoken or typed clause, supporting:
     * - Digits with commas ("500,000", "500")
     * - Compound million/thousand phrases ("۲ میلیون و ۵۰۰", "یک و نیم میلیون")
     * - Spoken Persian number words ("پانصد تومن", "دویست تومان", "صد و پنجاه تومن", "نود تومن")
     * - Automatic colloquial Toman scaling ("۵۰۰ تومان سیگار خریدم" -> 500,000.0)
     */
    fun extractSpokenOrTypedCurrencyAmount(clause: String, currency: String): Double? {
        val norm = toEnglishDigits(clause).replace(",", "").replace("،", "")

        // 1. Check compound "X میلیون و Y" (e.g. "2 میلیون و 500")
        val compoundMillionRegex = Regex("""(\d+(?:\.\d+)?)\s*(?:میلیون|ملیون)\s*و\s*(\d+(?:\.\d+)?)""")
        val compoundMatch = compoundMillionRegex.find(norm)
        if (compoundMatch != null) {
            val millions = compoundMatch.groupValues[1].toDoubleOrNull() ?: 0.0
            val remainder = compoundMatch.groupValues[2].toDoubleOrNull() ?: 0.0
            val remScaled = if (remainder < 1000.0) remainder * 1_000.0 else remainder
            return millions * 1_000_000.0 + remScaled
        }

        // 2. Check explicit digits
        val numberRegex = Regex("""(\d+(?:\.\d+)?)""")
        val firstNum = numberRegex.find(norm)?.groupValues?.get(1)?.toDoubleOrNull()
        if (firstNum != null && firstNum > 0.0) {
            return normalizeColloquialTomanAmount(firstNum, currency, norm)
        }

        // 3. Check spoken Persian number words (when Android SpeechRecognizer transcribes words instead of digits)
        val spokenMap = listOf(
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
            "صد" to 100.0,
            "نود" to 90.0,
            "هشتاد" to 80.0,
            "هفتاد" to 70.0,
            "شصت" to 60.0,
            "پنجاه" to 50.0,
            "چهل" to 40.0,
            "سی " to 30.0,
            "بیست" to 20.0,
            "پانزده" to 15.0,
            "پونزده" to 15.0,
            "ده " to 10.0
        )
        for ((word, value) in spokenMap) {
            if (norm.contains(word)) {
                return normalizeColloquialTomanAmount(value, currency, norm)
            }
        }

        return null
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
