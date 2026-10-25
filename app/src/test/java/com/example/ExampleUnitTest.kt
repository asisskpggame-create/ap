package com.example

import com.example.ai.GeminiAssistantService
import com.example.ai.ProposedAiOperation
import com.example.data.model.ExpenseEntry
import com.example.data.model.Habit
import com.example.util.PersianDateAndUnitUtils
import com.example.util.WorldClassOsUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun taskRecurrence_computesNextDueDateCorrectly() {
        val baseDay = 20168L // 1 Farvardin 1404 (31-day month)
        assertEquals(20169L, PersianDateAndUnitUtils.calculateNextOccurrenceEpochDay(baseDay, "DAILY"))
        assertEquals(20175L, PersianDateAndUnitUtils.calculateNextOccurrenceEpochDay(baseDay, "WEEKLY"))
        assertEquals(20199L, PersianDateAndUnitUtils.calculateNextOccurrenceEpochDay(baseDay, "MONTHLY"))
        assertEquals(20173L, PersianDateAndUnitUtils.calculateNextOccurrenceEpochDay(baseDay, "CUSTOM", 5))
        assertNull(PersianDateAndUnitUtils.calculateNextOccurrenceEpochDay(baseDay, "NONE"))
    }

    @Test
    fun consumptionUnitConversion_normalizesVolumeWeightAndRejectsIncompatibleUnits() {
        val waterMl = PersianDateAndUnitUtils.convertUnit(2.0, "لیوان", "ml")
        assertNotNull(waterMl)
        assertEquals(480.0, waterMl!!, 0.01)

        val litersToMl = PersianDateAndUnitUtils.convertUnit(1.5, "لیتر", "ml")
        assertNotNull(litersToMl)
        assertEquals(1500.0, litersToMl!!, 0.01)

        val kgToGrams = PersianDateAndUnitUtils.convertUnit(0.5, "کیلوگرم", "grams")
        assertNotNull(kgToGrams)
        assertEquals(500.0, kgToGrams!!, 0.01)

        // Incompatible units (e.g., weight to volume) must return null and never be mixed
        val incompatible = PersianDateAndUnitUtils.convertUnit(2.0, "کیلوگرم", "ml")
        assertNull(incompatible)
    }

    @Test
    fun expenseCalculations_neverMixDifferentCurrencies() {
        val entries = listOf(
            ExpenseEntry(id = "e1", userId = "u1", title = "دبل (اسپرسو)", category = "COFFEE", amount = 90000.0, currency = "TOMAN", transactionType = "EXPENSE"),
            ExpenseEntry(id = "e2", userId = "u1", title = "سیگار", category = "OTHER", amount = 150000.0, currency = "TOMAN", transactionType = "EXPENSE"),
            ExpenseEntry(id = "e3", userId = "u1", title = "هاست ابری", category = "SUBSCRIPTIONS", amount = 12.5, currency = "EUR", transactionType = "EXPENSE"),
            ExpenseEntry(id = "e4", userId = "u1", title = "کتاب", category = "EDUCATION", amount = 20.0, currency = "USD", transactionType = "EXPENSE"),
            ExpenseEntry(id = "i1", userId = "u1", title = "حقوق ماهانه", category = "OTHER", amount = 1000000.0, currency = "TOMAN", transactionType = "INCOME", notes = "واریزی شرکت")
        )
        val groupedExpenses = PersianDateAndUnitUtils.aggregateExpensesByCurrency(entries)
        assertEquals(3, groupedExpenses.size)
        assertEquals(240000.0, groupedExpenses["TOMAN"] ?: 0.0, 0.01)
        assertEquals(12.5, groupedExpenses["EUR"] ?: 0.0, 0.01)
        assertEquals(20.0, groupedExpenses["USD"] ?: 0.0, 0.01)

        val groupedIncomes = PersianDateAndUnitUtils.aggregateIncomesByCurrency(entries)
        assertEquals(1, groupedIncomes.size)
        assertEquals(1000000.0, groupedIncomes["TOMAN"] ?: 0.0, 0.01)

        val netBalance = PersianDateAndUnitUtils.aggregateNetBalanceByCurrency(entries)
        assertEquals(760000.0, netBalance["TOMAN"] ?: 0.0, 0.01)
    }

    @Test
    fun persianJalaliDateAndNumerals_convertAccurately() {
        // 2025-03-21 is 1 Farvardin 1404 (Nowruz) -> epochDay = 20168
        val jalali = PersianDateAndUnitUtils.epochDayToJalali(20168L)
        assertEquals(1404, jalali.year)
        assertEquals(1, jalali.month)
        assertEquals(1, jalali.day)

        val englishDigits = PersianDateAndUnitUtils.toEnglishDigits("۱۲۳۴۵۶۷۸۹۰")
        assertEquals("1234567890", englishDigits)

        val persianDigits = PersianDateAndUnitUtils.toPersianDigits("1404/01/01")
        assertEquals("۱۴۰۴/۰۱/۰۱", persianDigits)
    }

    @Test
    fun habitStreakCalculations_respectDailyAndWeeklySchedules() {
        val dailyHabit = Habit(
            id = "h1",
            userId = "u1",
            title = "مطالعه",
            category = "شخصی",
            scheduleType = "DAILY",
            targetPerPeriod = 1
        )
        val completedDaily = setOf(19998L, 19999L, 20000L)
        val streak = PersianDateAndUnitUtils.calculateHabitStreak(
            habit = dailyHabit,
            completedEpochDays = completedDaily,
            todayEpochDay = 20000L
        )
        assertEquals(3, streak)

        val weeklyHabit = Habit(
            id = "h2",
            userId = "u1",
            title = "باشگاه",
            category = "ورزش",
            scheduleType = "TIMES_PER_WEEK",
            targetPerPeriod = 3
        )
        val completedWeekly = setOf(19995L, 19997L, 19999L)
        val weeklyStreak = PersianDateAndUnitUtils.calculateHabitStreak(
            habit = weeklyHabit,
            completedEpochDays = completedWeekly,
            todayEpochDay = 20000L
        )
        assertTrue(weeklyStreak >= 1)
    }

    @Test
    fun aiOperationValidationAndFallbackParser_validatesAndExtractsMultiActionCommands() {
        val invalidNegativeExpense = ProposedAiOperation(
            operationType = "AddExpense",
            title = "خرید",
            amount = -500.0,
            currency = "TOMAN",
            humanReadablePreview = "هزینه منفی"
        )
        assertTrue(GeminiAssistantService.validateOperation(invalidNegativeExpense).isFailure)

        val validTaskOp = ProposedAiOperation(
            operationType = "CreateTask",
            title = "تماس با دکتر",
            priority = "HIGH",
            dueDateOffsetDays = 1,
            dueTimeMinutes = 540,
            humanReadablePreview = "تسک تماس با دکتر"
        )
        assertTrue(GeminiAssistantService.validateOperation(validTaskOp).isSuccess)

        val multiActionResult = GeminiAssistantService.parsePersianCommandDeterministic(
            rawText = "دو لیوان آب خوردم و یه تسک برای فردا ساعت ۹ صبح بذار: تماس با دکتر",
            todayEpochDay = 20000L,
            defaultCurrency = "TOMAN"
        )
        assertNotNull(multiActionResult)
        assertTrue(multiActionResult.operations.any { it.operationType == "AddConsumptionEntry" })
        assertTrue(multiActionResult.operations.any { it.operationType == "CreateTask" && it.dueDateOffsetDays == 1 })
    }

    @Test
    fun inlinePersianNlp_extractsDateAndTimeAndPriorityAndDurationWhileTyping() {
        val parsed = WorldClassOsUtils.parseInlinePersianTask("فردا ساعت ۵ عصر جلسه کاری با علی ۴۵ دقیقه #فوری")
        assertEquals(1, parsed.detectedDateOffsetDays)
        assertEquals(17 * 60, parsed.detectedTimeMinutes)
        assertEquals("HIGH", parsed.detectedPriority)
        assertEquals(45, parsed.detectedDurationMinutes)
        assertEquals("کاری", parsed.detectedCategory)
        assertEquals("جلسه کاری با علی", parsed.cleanedTitle)
        assertTrue(parsed.detectedTokensSummary.isNotEmpty())
    }

    @Test
    fun dailySafeToSpend_calculatesRemainingTodayAllowanceBasedOnJalaliMonth() {
        val today = 20168L // 1 Farvardin 1404 (31 days remaining in month)
        val expenses = listOf(
            ExpenseEntry(
                id = "e1",
                userId = "u1",
                title = "دبل (اسپرسو)",
                category = "COFFEE",
                amount = 90_000.0,
                currency = "TOMAN",
                transactionType = "EXPENSE",
                localEpochDay = today
            )
        )
        val state = WorldClassOsUtils.calculateDailySafeToSpend(
            monthlyBudget = 31_000_000.0,
            defaultCurrency = "TOMAN",
            todayEpochDay = today,
            expenses = expenses
        )
        assertEquals(31, state.remainingDaysInMonth)
        assertEquals(1_000_000.0, state.dailyAllowanceBeforeToday, 1.0)
        assertEquals(90_000.0, state.spentToday, 1.0)
        assertEquals(910_000.0, state.remainingTodayAllowance, 1.0)
    }

    @Test
    fun typography_configuresPersianFontAndTabularFiguresAcrossAllStyles() {
        val allStyles = listOf(
            com.example.ui.theme.Typography.displayLarge,
            com.example.ui.theme.Typography.displayMedium,
            com.example.ui.theme.Typography.displaySmall,
            com.example.ui.theme.Typography.headlineLarge,
            com.example.ui.theme.Typography.headlineMedium,
            com.example.ui.theme.Typography.headlineSmall,
            com.example.ui.theme.Typography.titleLarge,
            com.example.ui.theme.Typography.titleMedium,
            com.example.ui.theme.Typography.titleSmall,
            com.example.ui.theme.Typography.bodyLarge,
            com.example.ui.theme.Typography.bodyMedium,
            com.example.ui.theme.Typography.bodySmall,
            com.example.ui.theme.Typography.labelLarge,
            com.example.ui.theme.Typography.labelMedium,
            com.example.ui.theme.Typography.labelSmall
        )
        allStyles.forEach { style ->
            assertEquals("tnum", style.fontFeatureSettings)
            assertNotNull(style.fontFamily)
            assertEquals(
                androidx.compose.ui.text.style.TextDirection.ContentOrRtl,
                style.textDirection
            )
        }
    }

    @Test
    fun themePalettesAndLayoutStyles_resolveAllSixThemesAndFourLayouts() {
        assertEquals(6, com.example.ui.theme.AppThemePalette.entries.size)
        assertEquals(4, com.example.ui.theme.AppLayoutStyle.entries.size)

        com.example.ui.theme.AppThemePalette.entries.forEach { palette ->
            val scheme = com.example.ui.theme.resolveColorScheme(palette.code, palette.isDark)
            assertNotNull(scheme)
            assertEquals(palette, com.example.ui.theme.AppThemePalette.fromCode(palette.code))
        }

        com.example.ui.theme.AppLayoutStyle.entries.forEach { layout ->
            assertEquals(layout, com.example.ui.theme.AppLayoutStyle.fromCode(layout.code))
        }
    }
}


