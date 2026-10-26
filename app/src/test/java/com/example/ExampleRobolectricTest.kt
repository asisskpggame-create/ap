package com.example

import android.content.Context
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.util.WorldClassOsUtils
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context and verify encrypted backup roundtrip`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Personal Life OS", appName)

    val sampleJson = """{"exportVersion":2,"tasks":[{"title":"تست بکاپ رمزنگاری‌شده"}]}"""
    val encrypted = WorldClassOsUtils.EncryptedBackupManager.encryptPayload(sampleJson)
    assertTrue(encrypted.contains("PERSONAL_LIFE_OS_AES256_GCM_V1"))
    val decrypted = WorldClassOsUtils.EncryptedBackupManager.decryptPayload(encrypted)
    assertEquals(sampleJson, decrypted)
  }

  @Test
  fun `verify exact Toman vs Hezar Toman parsing and Iranian bank SMS parser`() {
    // 1. Exact Toman ("۵۰۰ تومان") MUST stay 500.0 and NEVER multiply by 1000
    val parsedExactToman = com.example.ai.GeminiAssistantService.parsePersianCommandDeterministic(
      rawText = "۵۰۰ تومان سیگار خریدم",
      todayEpochDay = 20000L,
      defaultCurrency = "TOMAN"
    )
    assertEquals(1, parsedExactToman.operations.size)
    assertEquals("AddExpense", parsedExactToman.operations[0].operationType)
    assertEquals(500.0, parsedExactToman.operations[0].amount, 0.01)
    assertEquals("TOMAN", parsedExactToman.operations[0].currency)
    assertEquals("EXPENSE", parsedExactToman.operations[0].transactionType)

    // 2. Explicit Hezar Toman ("۵۰۰ هزار تومان") MUST become 500,000.0 TOMAN
    val parsedHezarToman = com.example.ai.GeminiAssistantService.parsePersianCommandDeterministic(
      rawText = "۵۰۰ هزار تومان سیگار خریدم",
      todayEpochDay = 20000L,
      defaultCurrency = "TOMAN"
    )
    assertEquals(500_000.0, parsedHezarToman.operations[0].amount, 0.01)
    assertEquals("TOMAN", parsedHezarToman.operations[0].currency)

    // 3. Spoken words with explicit هزار ("پانصد هزار تومن") -> 500,000.0
    val parsedVoiceWords = com.example.ai.GeminiAssistantService.parsePersianCommandDeterministic(
      rawText = "پانصد هزار تومن سیگار خریدم",
      todayEpochDay = 20000L,
      defaultCurrency = "TOMAN"
    )
    assertEquals(500_000.0, parsedVoiceWords.operations[0].amount, 0.01)

    val parsedEuro = com.example.ai.GeminiAssistantService.parsePersianCommandDeterministic(
      rawText = "یه قهوه ۳ یورویی خریدم",
      todayEpochDay = 20000L,
      defaultCurrency = "TOMAN"
    )
    assertEquals(3.0, parsedEuro.operations[0].amount, 0.01)
    assertEquals("EUR", parsedEuro.operations[0].currency)

    val smsParsed = com.example.ui.components.IranianBankSmsParser.parseBankSms(
      "بانک ملت\nخرید با کارت\nمبلغ: 5,000,000 ریال\nمانده: 42,000,000 ریال"
    )
    assertEquals(500_000.0, smsParsed?.amountInToman ?: 0.0, 0.01)
    assertEquals("بانک ملت", smsParsed?.bankName)
  }

  @Test
  fun `audit suite P0 P1 verify currency units 32 colloquial Persian sentences frequent titles and atomic update`() = kotlinx.coroutines.runBlocking {
    val today = 20100L

    // --- PART A: Currency Wheel Unit & Exact Conversion Audit (Zero Unwanted 1000x) ---
    val (amtToman, currToman) = com.example.util.PersianDateAndUnitUtils.resolveEnteredAmountAndCurrency(1000.0, "TOMAN")
    assertEquals(1000.0, amtToman, 0.01)
    assertEquals("TOMAN", currToman)

    val (amtHezar, currHezar) = com.example.util.PersianDateAndUnitUtils.resolveEnteredAmountAndCurrency(600.0, "HEZAR_TOMAN")
    assertEquals(600_000.0, amtHezar, 0.01)
    assertEquals("TOMAN", currHezar)

    val (amtMillion, currMillion) = com.example.util.PersianDateAndUnitUtils.resolveEnteredAmountAndCurrency(2.5, "MILLION_TOMAN")
    assertEquals(2_500_000.0, amtMillion, 0.01)
    assertEquals("TOMAN", currMillion)

    val (amtIrr, currIrr) = com.example.util.PersianDateAndUnitUtils.resolveEnteredAmountAndCurrency(1000.0, "IRR")
    assertEquals(1000.0, amtIrr, 0.01)
    assertEquals("IRR", currIrr)
    assertEquals(100.0, com.example.util.PersianDateAndUnitUtils.convertCurrencyExact(1000.0, "IRR", "TOMAN") ?: 0.0, 0.01)
    assertEquals(10_000.0, com.example.util.PersianDateAndUnitUtils.convertCurrencyExact(1000.0, "TOMAN", "IRR") ?: 0.0, 0.01)

    // Ensure normalizeColloquialTomanAmount NEVER multiplies plain TOMAN by 1000
    assertEquals(
      1000.0,
      com.example.util.PersianDateAndUnitUtils.normalizeColloquialTomanAmount(1000.0, "TOMAN", "۱۰۰۰ تومان"),
      0.01
    )
    assertEquals(
      600_000.0,
      com.example.util.PersianDateAndUnitUtils.normalizeColloquialTomanAmount(600.0, "TOMAN", "۶۰۰ هزار تومان"),
      0.01
    )

    // --- PART B: 32 Colloquial Persian Sentences Intent & Amount Classification Audit ---
    data class SentenceAuditCase(
      val sentence: String,
      val expectedTxType: String,
      val expectedAmount: Double,
      val expectedCurrency: String = "TOMAN"
    )

    val colloquialCases = listOf(
      // 1-10: INCOME (دریافت / فروش / حقوق / واریز / گرفتم بابت فروش)
      SentenceAuditCase("۶۰۰ هزار تومان بابت فروش جمینای گرفتم", "INCOME", 600_000.0),
      SentenceAuditCase("۶۰۰ هزار تومن از فروش Gemini دریافت کردم", "INCOME", 600_000.0),
      SentenceAuditCase("۱۵ میلیون تومان حقوق گرفتم", "INCOME", 15_000_000.0),
      SentenceAuditCase("۲ میلیون و ۵۰۰ هزار تومان بابت پروژه طراحی سایت دریافت کردم", "INCOME", 2_500_000.0),
      SentenceAuditCase("۸۰۰ هزار تومان بابت فروش خدمات مشاوره به حسابم واریز شد", "INCOME", 800_000.0),
      SentenceAuditCase("یک و نیم میلیون تومان سود بانکی واریز شد", "INCOME", 1_500_000.0),
      SentenceAuditCase("۴۵۰ هزار تومان بابت فروش اکانت پول گرفتم", "INCOME", 450_000.0),
      SentenceAuditCase("۳ میلیون تومان پاداش دریافت کردم", "INCOME", 3_000_000.0),
      SentenceAuditCase("۹۰۰ هزار تومان بابت تعمیر لپتاپ دستمزد گرفتم", "INCOME", 900_000.0),
      SentenceAuditCase("۱۲۰ دلار بابت پروژه فریلنسری درآمد داشتم", "INCOME", 120.0, "USD"),

      // 11-22: EXPENSE (پرداخت / خرید / خرج / اسنپ گرفتم / کرایه)
      SentenceAuditCase("۳۰۰ هزار تومن بابت اشتراک پرداخت کردم", "EXPENSE", 300_000.0),
      SentenceAuditCase("۲۵۰ هزار تومان غذا خریدم", "EXPENSE", 250_000.0),
      SentenceAuditCase("۸۵ هزار تومان اسنپ گرفتم", "EXPENSE", 85_000.0),
      SentenceAuditCase("۱۲۰ هزار تومان تاکسی گرفتم", "EXPENSE", 120_000.0),
      SentenceAuditCase("۴۰۰ هزار تومان خرید سوپرمارکت انجام دادم", "EXPENSE", 400_000.0),
      SentenceAuditCase("۹۵ هزار تومان قهوه خوردم", "EXPENSE", 95_000.0),
      SentenceAuditCase("۱۸۰ هزار تومان قبض اینترنت پرداخت کردم", "EXPENSE", 180_000.0),
      SentenceAuditCase("۱۵۰ هزار تومان بنزین زدم", "EXPENSE", 150_000.0),
      SentenceAuditCase("۶۵۰ هزار تومان دارو و ویزیت دکتر دادم", "EXPENSE", 650_000.0),
      SentenceAuditCase("۱۰۰۰ تومان کارمزد بانکی کم شد", "EXPENSE", 1000.0),
      SentenceAuditCase("۵۰۰۰ ریال صدقه دادم", "EXPENSE", 5000.0, "IRR"),
      SentenceAuditCase("۲ میلیون تومان اجاره خونه پرداخت کردم", "EXPENSE", 2_000_000.0),

      // 23-26: TRANSFER (انتقال بین حساب‌ها)
      SentenceAuditCase("۵ میلیون تومان از حساب اصلی به پس‌انداز انتقال دادم", "TRANSFER", 5_000_000.0),
      SentenceAuditCase("۳ میلیون تومان کارت به کارت به حساب خودم زدم", "TRANSFER", 3_000_000.0),
      SentenceAuditCase("یک میلیون تومان جابجایی بین حساب انجام دادم", "TRANSFER", 1_000_000.0),
      SentenceAuditCase("۷۰۰ هزار تومان انتقال به کارت پس‌انداز", "TRANSFER", 700_000.0),

      // 27-29: RECEIVABLE (طلب / قرض دادن)
      SentenceAuditCase("۲۰۰ هزار تومان از مشتری طلب دارم", "RECEIVABLE", 200_000.0),
      SentenceAuditCase("یک میلیون تومان به علی قرض دادم", "RECEIVABLE", 1_000_000.0),
      SentenceAuditCase("۴۰۰ هزار تومان از رضا طلبکارم", "RECEIVABLE", 400_000.0),

      // 30-32: DEBT (بدهی / قرض گرفتن)
      SentenceAuditCase("۵۰۰ هزار تومان به علی بدهکارم", "DEBT", 500_000.0),
      SentenceAuditCase("۲ میلیون تومان از محمد قرض گرفتم", "DEBT", 2_000_000.0),
      SentenceAuditCase("۱ میلیون و ۲۰۰ هزار تومان بدهی به صاحبخانه دارم", "DEBT", 1_200_000.0)
    )

    colloquialCases.forEachIndexed { idx, testCase ->
      val parsed = com.example.ai.GeminiAssistantService.parsePersianCommandDeterministic(
        rawText = testCase.sentence,
        todayEpochDay = today,
        defaultCurrency = "TOMAN"
      )
      assertEquals(
        "Case #${idx + 1} failed ops count for: ${testCase.sentence}",
        1,
        parsed.operations.size
      )
      val op = parsed.operations.first()
      assertEquals(
        "Case #${idx + 1} wrong transactionType for: ${testCase.sentence}",
        testCase.expectedTxType,
        op.transactionType
      )
      assertEquals(
        "Case #${idx + 1} wrong amount for: ${testCase.sentence}",
        testCase.expectedAmount,
        op.amount,
        0.01
      )
      assertEquals(
        "Case #${idx + 1} wrong currency for: ${testCase.sentence}",
        testCase.expectedCurrency,
        op.currency
      )
    }

    // Verify specific bug case: "۶۰۰ هزار تومان بابت فروش جمینای گرفتم" clean title is "فروش جمینای"
    val geminiSale = com.example.ai.GeminiAssistantService.parsePersianCommandDeterministic(
      rawText = "۶۰۰ هزار تومان بابت فروش جمینای گرفتم",
      todayEpochDay = today,
      defaultCurrency = "TOMAN"
    ).operations.first()
    assertEquals("INCOME", geminiSale.transactionType)
    assertEquals(600_000.0, geminiSale.amount, 0.01)
    assertTrue("Expected title to contain فروش جمینای but was ${geminiSale.title}", geminiSale.title.contains("فروش جمینای"))

    // Verify ambiguous sentence without direction asks clarification instead of guessing
    val ambiguousNoVerb = com.example.ai.GeminiAssistantService.parsePersianCommandDeterministic(
      rawText = "۵۰۰ هزار تومان با علی",
      todayEpochDay = today,
      defaultCurrency = "TOMAN"
    )
    assertTrue(ambiguousNoVerb.isAmbiguous)
    assertEquals("AskClarification", ambiguousNoVerb.operations.first().operationType)

    // --- PART C: Frequent Titles Separation, Ranking, Pinning & Deletion Audit ---
    val sampleHistory = listOf(
      com.example.data.model.ExpenseEntry(
        id = "1",
        title = "فروش Gemini",
        category = "فروش و خدمات",
        amount = 600_000.0,
        transactionType = "INCOME"
      ),
      com.example.data.model.ExpenseEntry(
        id = "2",
        title = "فروش Gemini",
        category = "فروش و خدمات",
        amount = 600_000.0,
        transactionType = "INCOME"
      ),
      com.example.data.model.ExpenseEntry(
        id = "3",
        title = "قهوه تخصصی",
        category = "غذا و کافه",
        amount = 95_000.0,
        transactionType = "EXPENSE"
      )
    )

    val incomeTitles = com.example.util.PersianDateAndUnitUtils.getRankedFrequentTitlesForType(
      transactionType = "INCOME",
      historicalExpenses = sampleHistory,
      customFrequentItems = com.example.util.PersianDateAndUnitUtils.defaultFrequentTitles,
      deletedTitlesForType = emptySet()
    )
    val expenseTitles = com.example.util.PersianDateAndUnitUtils.getRankedFrequentTitlesForType(
      transactionType = "EXPENSE",
      historicalExpenses = sampleHistory,
      customFrequentItems = com.example.util.PersianDateAndUnitUtils.defaultFrequentTitles,
      deletedTitlesForType = emptySet()
    )

    assertTrue(incomeTitles.all { it.transactionType == "INCOME" })
    assertTrue(expenseTitles.all { it.transactionType == "EXPENSE" })
    assertTrue(incomeTitles.any { it.title == "فروش Gemini" })
    assertTrue(expenseTitles.none { it.title == "فروش Gemini" })
    assertTrue(expenseTitles.any { it.title == "قهوه تخصصی" })

    // Deleting a frequent title removes it from the ranked list
    val afterDeleteIncome = com.example.util.PersianDateAndUnitUtils.getRankedFrequentTitlesForType(
      transactionType = "INCOME",
      historicalExpenses = sampleHistory,
      customFrequentItems = com.example.util.PersianDateAndUnitUtils.defaultFrequentTitles,
      deletedTitlesForType = setOf("INCOME|فروش gemini")
    )
    assertTrue(afterDeleteIncome.none { it.title.equals("فروش Gemini", ignoreCase = true) })

    // --- PART D: Atomic Expense Edit & Delete Consistency ---
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(
      context,
      com.example.data.local.LifeOsDatabase::class.java
    ).allowMainThreadQueries().build()
    val repo = com.example.data.repository.LifeOsRepository(cloudDb = null, localDao = db.lifeOsDao())
    val userId = com.example.data.local.LifeOsDatabase.LOCAL_DEVICE_USER_ID

    val createdId = repo.addExpense(
      title = "فروش جمینای",
      category = "فروش و خدمات",
      amount = 600_000.0,
      currency = "TOMAN",
      localEpochDay = today,
      notes = "ثبت اولیه",
      transactionType = "INCOME"
    ).getOrThrow()

    val originalEntry = db.lifeOsDao().getExpenseById(userId, createdId)!!.toDomain()
    assertEquals(600_000.0, originalEntry.amount, 0.01)
    assertEquals("INCOME", originalEntry.transactionType)

    // Atomic update preserves ID and updates amount/title accurately
    repo.updateExpense(
      originalEntry.copy(
        title = "فروش اشتراک Gemini Pro",
        amount = 750_000.0
      )
    ).getOrThrow()

    val updatedEntry = db.lifeOsDao().getExpenseById(userId, createdId)!!.toDomain()
    assertEquals(createdId, updatedEntry.id)
    assertEquals("فروش اشتراک Gemini Pro", updatedEntry.title)
    assertEquals(750_000.0, updatedEntry.amount, 0.01)

    db.close()
  }

  @get:org.junit.Rule
  val composeTestRule = androidx.compose.ui.test.junit4.createAndroidComposeRule<MainActivity>()

  @Test
  fun `launch MainActivity and navigate all screens without crash`() {
    composeTestRule.waitForIdle()
    // Dismiss onboarding if visible
    try {
      composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("complete_onboarding_button")).performClick()
      composeTestRule.waitForIdle()
    } catch (_: Throwable) {}

    // Verify 4 primary bottom navigation tabs in roadmap order (1. مالی, 2. اهداف و مسیر, 3. داشبورد, 4. AI) + top bar destinations
    val primarySections = AppSection.entries.filter { it.isPrimaryBottomNav }
    assertEquals(
      listOf("DAILY_LOG", "GOALS_AND_HABITS", "HOME", "AI_ASSISTANT"),
      primarySections.map { it.route }
    )
    assertEquals(
      listOf("مالی", "اهداف و مسیر", "داشبورد", "AI"),
      primarySections.map { it.titleFa }
    )

    listOf(
      "bottom_nav_daily_log",
      "bottom_nav_goals_and_habits",
      "bottom_nav_home",
      "bottom_nav_ai_assistant",
      "nav_reports_button",
      "nav_history_button",
      "nav_settings_button",
      "voice_capture_fab"
    ).forEach { tag ->
      try {
        composeTestRule.onNode(androidx.compose.ui.test.hasTestTag(tag)).performClick()
        composeTestRule.waitForIdle()
      } catch (e: Throwable) {
        throw RuntimeException("Failed on tag: $tag", e)
      }
    }
  }

  @Test
  fun `phase2 verify debt repayment plan partial payment idempotency and transfer exclusion`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(
      context,
      com.example.data.local.LifeOsDatabase::class.java
    ).allowMainThreadQueries().build()
    val repo = com.example.data.repository.LifeOsRepository(cloudDb = null, localDao = db.lifeOsDao())
    val userId = com.example.data.local.LifeOsDatabase.LOCAL_DEVICE_USER_ID
    val today = 20050L

    // 1. Personal account transfer must not count as income or expense
    val transferId = repo.addExpense(
      title = "انتقال کارت به کارت",
      category = "انتقال حساب",
      amount = 2_000_000.0,
      currency = "TOMAN",
      localEpochDay = today,
      notes = "انتقال بین حساب شخصی",
      transactionType = "TRANSFER"
    ).getOrThrow()
    val transferEntry = db.lifeOsDao().getExpenseById(userId, transferId)!!.toDomain()
    assertTrue(transferEntry.isTransfer)
    assertEquals(false, transferEntry.isExpense)
    assertEquals(false, transferEntry.isIncome)
    val netMap = com.example.util.PersianDateAndUnitUtils.aggregateNetBalanceByCurrency(listOf(transferEntry))
    assertTrue(netMap.isEmpty())

    // 2. Create a master Debt of 3,000,000 TOMAN and a 3-installment repayment plan in Path
    val debtId = repo.addExpense(
      title = "وام قرض‌الحسنه",
      category = "سایر",
      amount = 3_000_000.0,
      currency = "TOMAN",
      localEpochDay = today,
      notes = "سه قسط",
      transactionType = "DEBT",
      dueEpochDay = today + 30L,
      installmentCount = 3
    ).getOrThrow()
    val debtEntry = db.lifeOsDao().getExpenseById(userId, debtId)!!.toDomain()
    val installmentTasks = repo.createDebtRepaymentPlan(
      debtEntry = debtEntry,
      installmentCount = 3,
      firstDueEpochDay = today + 10L,
      intervalDays = 30
    ).getOrThrow()
    assertEquals(3, installmentTasks.size)
    assertTrue(installmentTasks.all { it.linkedDebtId == debtEntry.id })

    // 3. Completing a task alone in Path without payment confirmation does NOT deduct debt
    repo.completeTask(installmentTasks[0]).getOrThrow()
    val debtAfterTaskOnly = db.lifeOsDao().getExpenseById(userId, debtEntry.id)!!.toDomain()
    assertEquals(0.0, debtAfterTaskOnly.paidAmount, 0.01)
    assertEquals(3_000_000.0, debtAfterTaskOnly.remainingAmount, 0.01)

    // 4. Confirming installment payment deducts 1,000,000 and is idempotent on duplicate retry
    val updatedOnce = repo.recordDebtPartialPayment(
      debtEntry = debtAfterTaskOnly,
      paymentAmount = 1_000_000.0,
      linkedTaskId = installmentTasks[0].id
    ).getOrThrow()
    assertEquals(1_000_000.0, updatedOnce.paidAmount, 0.01)
    assertEquals(2_000_000.0, updatedOnce.remainingAmount, 0.01)

    val updatedDuplicate = repo.recordDebtPartialPayment(
      debtEntry = updatedOnce,
      paymentAmount = 1_000_000.0,
      linkedTaskId = installmentTasks[0].id
    ).getOrThrow()
    assertEquals(1_000_000.0, updatedDuplicate.paidAmount, 0.01)
    assertEquals(2_000_000.0, updatedDuplicate.remainingAmount, 0.01)

    // 5. Full debt settlement marks debt settled and marks all linked repayment tasks COMPLETED
    repo.settleDebtOrReceivable(updatedDuplicate).getOrThrow()
    val settledDebt = db.lifeOsDao().getExpenseById(userId, debtEntry.id)!!.toDomain()
    assertTrue(settledDebt.isSettled)
    assertEquals(0.0, settledDebt.remainingAmount, 0.01)
    val linkedTasksAfterSettle = db.lifeOsDao().getTasksLinkedToDebt(userId, debtEntry.id)
    assertEquals(3, linkedTasksAfterSettle.size)
    assertTrue(linkedTasksAfterSettle.all { it.status == "COMPLETED" })

    db.close()
  }

  @Test
  fun `phase3 verify unified reminder engine reschedule snooze task completion and debt settlement`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.notifications.ReminderScheduler.clearAllTrackedRemindersForTest()

    val db = androidx.room.Room.inMemoryDatabaseBuilder(
      context,
      com.example.data.local.LifeOsDatabase::class.java
    ).allowMainThreadQueries().build()
    val repo = com.example.data.repository.LifeOsRepository(cloudDb = null, localDao = db.lifeOsDao())
    val userId = com.example.data.local.LifeOsDatabase.LOCAL_DEVICE_USER_ID
    val today = com.example.util.PersianDateAndUnitUtils.currentLocalEpochDay()
    val initialTrigger = System.currentTimeMillis() + 3_600_000L

    // 1. Create a task with a reminder -> registered in Unified Reminder Engine
    val taskId = repo.createTask(
      title = "پیگیری قرارداد",
      description = "تماس با واحد مالی",
      category = "کار",
      dueDateEpochDay = today + 1L,
      reminderAtMillis = initialTrigger
    ).getOrThrow()
    val activeTaskReminder = com.example.notifications.ReminderScheduler.getActiveReminderForEntity("TASK", taskId)
    org.junit.Assert.assertNotNull(activeTaskReminder)
    assertEquals(initialTrigger, activeTaskReminder!!.triggerAtMillis)

    // 2. Reschedule task -> updates reminder without creating duplicate
    val newTrigger = initialTrigger + 7_200_000L
    val taskDomain = db.lifeOsDao().getTasksForUser(userId).first { it.id == taskId }.toDomain()
    repo.updateTask(taskDomain.copy(dueDateEpochDay = today + 2L, reminderAtMillis = newTrigger)).getOrThrow()
    val rescheduledReminder = com.example.notifications.ReminderScheduler.getActiveReminderForEntity("TASK", taskId)
    org.junit.Assert.assertNotNull(rescheduledReminder)
    assertEquals(newTrigger, rescheduledReminder!!.triggerAtMillis)
    assertEquals(1, com.example.notifications.ReminderScheduler.getActiveReminders().count { it.entityId == taskId })

    // 3. Snooze reminder -> updates single entry, increments snoozeCount, no duplicate
    val snoozed = com.example.notifications.ReminderScheduler.snoozeReminder(
      context = context,
      notificationId = rescheduledReminder.notificationId,
      snoozeDurationMillis = 10 * 60_000L
    )
    assertEquals(1, snoozed.snoozeCount)
    assertEquals(1, com.example.notifications.ReminderScheduler.getActiveReminders().count { it.entityId == taskId })

    // 4. Complete task -> automatically cancels reminder
    repo.completeTask(taskDomain).getOrThrow()
    org.junit.Assert.assertNull(com.example.notifications.ReminderScheduler.getActiveReminderForEntity("TASK", taskId))

    // 5. Debt reminder & linked task reminders automatically cancelled upon debt settlement
    val debtId = repo.addExpense(
      title = "قسط وام مسکن",
      category = "قبوض و اجاره",
      amount = 5_000_000.0,
      currency = "TOMAN",
      localEpochDay = today,
      transactionType = "DEBT",
      dueEpochDay = today + 5L
    ).getOrThrow()
    org.junit.Assert.assertNotNull(com.example.notifications.ReminderScheduler.getActiveReminderForEntity("DEBT", debtId))

    val debtEntry = db.lifeOsDao().getExpenseById(userId, debtId)!!.toDomain()
    repo.settleDebtOrReceivable(debtEntry).getOrThrow()
    org.junit.Assert.assertNull(com.example.notifications.ReminderScheduler.getActiveReminderForEntity("DEBT", debtId))

    db.close()
  }

  @Test
  fun `phase4 and phase5 verify dashboard insufficient data estimation and controlled ai clarification and analytics`() {
    val today = 20050L

    // 1. Phase 4: Safe-to-spend & End-of-month projection calculation
    val sampleExpenses = listOf(
      com.example.data.model.ExpenseEntry(
        id = "exp_1",
        title = "خرید سوپرمارکت",
        category = "FOOD",
        amount = 1_500_000.0,
        currency = "TOMAN",
        transactionType = "EXPENSE",
        localEpochDay = today - 2L
      ),
      com.example.data.model.ExpenseEntry(
        id = "debt_1",
        title = "وام کالا",
        category = "OTHER",
        amount = 6_000_000.0,
        paidAmount = 2_000_000.0,
        currency = "TOMAN",
        transactionType = "DEBT",
        localEpochDay = today - 5L,
        dueEpochDay = today + 5L,
        installmentCount = 3
      )
    )
    val safeToSpend = WorldClassOsUtils.calculateDailySafeToSpend(
      monthlyBudget = 15_000_000.0,
      expenses = sampleExpenses,
      todayEpochDay = today,
      defaultCurrency = "TOMAN"
    )
    assertEquals(1_500_000.0, safeToSpend.spentLast30Days, 0.01)
    assertTrue(safeToSpend.projectedEndOfMonthSpend > 0.0)

    // 2. Phase 5: Ambiguous commands MUST return AskClarification and not execute blindly
    val ambigDebtSettle = com.example.ai.GeminiAssistantService.parsePersianCommandDeterministic(
      rawText = "تسویه بدهی",
      todayEpochDay = today,
      defaultCurrency = "TOMAN",
      expenses = sampleExpenses
    )
    assertTrue(ambigDebtSettle.isAmbiguous)
    assertEquals("AskClarification", ambigDebtSettle.operations.first().operationType)
    assertTrue(ambigDebtSettle.conversationalReply.contains("وام کالا"))

    val ambigTaskComplete = com.example.ai.GeminiAssistantService.parsePersianCommandDeterministic(
      rawText = "این تسک رو انجام‌شده کن",
      todayEpochDay = today,
      defaultCurrency = "TOMAN",
      tasks = listOf(
        com.example.data.model.Task(
          id = "t_1",
          title = "قسط ۱ از ۳: وام کالا",
          linkedDebtId = "debt_1",
          dueDateEpochDay = today + 2L
        )
      )
    )
    assertTrue(ambigTaskComplete.isAmbiguous)
    assertEquals("AskClarification", ambigTaskComplete.operations.first().operationType)

    // 3. Phase 5: Analytical queries MUST answer from real data with time range / insufficient data notice
    val emptyTopSpendQuery = com.example.ai.GeminiAssistantService.parsePersianCommandDeterministic(
      rawText = "بیشترین هزینه من در کدام دسته بوده؟",
      todayEpochDay = today,
      defaultCurrency = "TOMAN",
      expenses = emptyList()
    )
    assertTrue(emptyTopSpendQuery.conversationalReply.contains("داده کافی نیست"))

    val realTopSpendQuery = com.example.ai.GeminiAssistantService.parsePersianCommandDeterministic(
      rawText = "بیشترین هزینه من در کدام دسته بوده؟",
      todayEpochDay = today,
      defaultCurrency = "TOMAN",
      expenses = sampleExpenses
    )
    assertTrue(realTopSpendQuery.conversationalReply.contains("بازه ۳۰ روز اخیر"))
    assertTrue(realTopSpendQuery.conversationalReply.contains("FOOD"))

    val debtStatusQuery = com.example.ai.GeminiAssistantService.parsePersianCommandDeterministic(
      rawText = "وضعیت بدهی و قسط من چطوره؟",
      todayEpochDay = today,
      defaultCurrency = "TOMAN",
      expenses = sampleExpenses,
      tasks = listOf(
        com.example.data.model.Task(
          id = "t_1",
          title = "قسط ۱ از ۳: وام کالا",
          linkedDebtId = "debt_1",
          status = "PENDING",
          dueDateEpochDay = today + 2L
        )
      )
    )
    assertTrue(debtStatusQuery.conversationalReply.contains("۱ بدهی باز"))
    assertTrue(debtStatusQuery.conversationalReply.contains("۱ قسط فعال"))

    // 4. Phase 5: Sensitive financial operation requires confirmation before saving
    val validExpenseCmd = com.example.ai.GeminiAssistantService.parsePersianCommandDeterministic(
      rawText = "۲۵۰ هزار تومان ناهار خریدم",
      todayEpochDay = today,
      defaultCurrency = "TOMAN"
    )
    assertEquals(false, validExpenseCmd.isAmbiguous)
    assertEquals(1, validExpenseCmd.operations.size)
    assertTrue(validExpenseCmd.operations.first().requiresConfirmation)
  }

  @Test
  fun `verify Room database initialization migrations and financial decimal precision contract`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()

    // 1. Verify in-memory LifeOsDatabase with all migrations and safe callback does not crash or wipe
    val inMemoryDb = androidx.room.Room.inMemoryDatabaseBuilder(
      context,
      com.example.data.local.LifeOsDatabase::class.java
    )
      .addMigrations(
        com.example.data.local.LifeOsDatabase.MIGRATION_1_2,
        com.example.data.local.LifeOsDatabase.MIGRATION_2_3,
        com.example.data.local.LifeOsDatabase.MIGRATION_3_4,
        com.example.data.local.LifeOsDatabase.MIGRATION_4_5,
        com.example.data.local.LifeOsDatabase.MIGRATION_5_6
      )
      .fallbackToDestructiveMigrationOnDowngrade()
      .addCallback(com.example.data.local.LifeOsDatabase.SAFE_SCHEMA_CALLBACK)
      .build()

    val lifeOsDao = inMemoryDb.lifeOsDao()

    val testTask = com.example.data.local.CachedTaskEntity(
      id = "task_test_migration_1",
      userId = "u_local",
      title = "بررسی میگریشن دیتابیس",
      linkedDebtId = "debt_99"
    )
    lifeOsDao.upsertTask(testTask)
    val loadedTasks = lifeOsDao.getTasksForUser("u_local")
    assertEquals(1, loadedTasks.size)
    assertEquals("task_test_migration_1", loadedTasks[0].id)
    assertEquals("debt_99", loadedTasks[0].linkedDebtId)

    val testExpense = com.example.data.local.CachedExpenseEntity(
      id = "exp_test_migration_1",
      userId = "u_local",
      amount = com.example.util.PersianDateAndUnitUtils.roundFinancialAmount(750000.456),
      category = "FOOD",
      title = "ناهار کاری",
      currency = "TOMAN",
      transactionType = "EXPENSE",
      paidAmount = 750000.46,
      localEpochDay = 20100L,
      timestampMillis = System.currentTimeMillis()
    )
    lifeOsDao.upsertExpenseOne(testExpense)
    val loadedExpense = lifeOsDao.getExpenseById("u_local", "exp_test_migration_1")
    assertEquals(750000.46, loadedExpense?.amount ?: 0.0, 0.001)
    assertEquals("EXPENSE", loadedExpense?.transactionType)

    inMemoryDb.close()

    // 2. Verify financial decimal precision contract: 2 decimals, zero float drift
    assertEquals(100.0, com.example.util.PersianDateAndUnitUtils.roundFinancialAmount(100.0), 0.0001)
    assertEquals(100.55, com.example.util.PersianDateAndUnitUtils.roundFinancialAmount(100.548), 0.0001)
    assertEquals(100.54, com.example.util.PersianDateAndUnitUtils.roundFinancialAmount(100.542), 0.0001)
    assertEquals(0.0, com.example.util.PersianDateAndUnitUtils.roundFinancialAmount(0.0), 0.0001)
  }

  @Test
  fun `verify true schema migration from v1 to v6 preserves existing user data without data loss`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val dbFile = context.getDatabasePath("test_migration_preservation.db")
    if (dbFile.exists()) dbFile.delete()

    // 1. Manually construct version 1 database using raw SQLite
    val config = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
      .name("test_migration_preservation.db")
      .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(1) {
        override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
          db.execSQL("""
            CREATE TABLE cached_tasks (
              id TEXT PRIMARY KEY NOT NULL,
              userId TEXT NOT NULL,
              title TEXT NOT NULL,
              description TEXT NOT NULL,
              category TEXT NOT NULL,
              tagsCsv TEXT NOT NULL,
              priority TEXT NOT NULL,
              dueDateEpochDay INTEGER NOT NULL,
              dueTimeMinutes INTEGER NOT NULL,
              reminderAtMillis INTEGER NOT NULL,
              estimatedDurationMinutes INTEGER NOT NULL,
              status TEXT NOT NULL,
              subtasksJson TEXT NOT NULL,
              recurrenceRule TEXT NOT NULL,
              recurrenceIntervalDays INTEGER NOT NULL,
              notes TEXT NOT NULL,
              inputSource TEXT NOT NULL,
              sortOrder INTEGER NOT NULL,
              completedAtMillis INTEGER NOT NULL,
              idempotencyKey TEXT NOT NULL,
              syncedWithCloud INTEGER NOT NULL,
              updatedAtMillis INTEGER NOT NULL
            )
          """.trimIndent())

          db.execSQL("""
            CREATE TABLE cached_expenses (
              id TEXT PRIMARY KEY NOT NULL,
              userId TEXT NOT NULL,
              amount REAL NOT NULL,
              category TEXT NOT NULL,
              title TEXT NOT NULL,
              currency TEXT NOT NULL,
              localEpochDay INTEGER NOT NULL,
              timestampMillis INTEGER NOT NULL,
              linkedConsumptionId TEXT NOT NULL DEFAULT '',
              notes TEXT NOT NULL DEFAULT '',
              inputSource TEXT NOT NULL DEFAULT 'MANUAL',
              idempotencyKey TEXT NOT NULL DEFAULT '',
              syncedWithCloud INTEGER NOT NULL DEFAULT 0
            )
          """.trimIndent())
        }

        override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
      })
      .build()

    val rawHelper = androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory().create(config)
    val v1Db = rawHelper.writableDatabase

    // Insert real user data into Version 1
    v1Db.execSQL("""
      INSERT INTO cached_tasks (id, userId, title, description, category, tagsCsv, priority, dueDateEpochDay, dueTimeMinutes, reminderAtMillis, estimatedDurationMinutes, status, subtasksJson, recurrenceRule, recurrenceIntervalDays, notes, inputSource, sortOrder, completedAtMillis, idempotencyKey, syncedWithCloud, updatedAtMillis)
      VALUES ('task_v1_legacy', 'u_test', 'تسک قدیمی کاربر در نسخه ۱', '', 'عمومی', '', 'HIGH', 20100, -1, -1, 30, 'PENDING', '[]', 'NONE', 0, '', 'MANUAL', 0, -1, '', 0, 1000)
    """.trimIndent())

    v1Db.execSQL("""
      INSERT INTO cached_expenses (id, userId, amount, category, title, currency, localEpochDay, timestampMillis, inputSource, notes, syncedWithCloud)
      VALUES ('exp_v1_legacy', 'u_test', 450000.0, 'FOOD', 'خرید ناهار قدیمی', 'TOMAN', 20100, 2000, 'MANUAL', '', 0)
    """.trimIndent())

    v1Db.close()
    rawHelper.close()

    // 2. Open with LifeOsDatabase at version 6 with all migrations applied
    val migratedDb = androidx.room.Room.databaseBuilder(
      context,
      com.example.data.local.LifeOsDatabase::class.java,
      "test_migration_preservation.db"
    )
      .addMigrations(
        com.example.data.local.LifeOsDatabase.MIGRATION_1_2,
        com.example.data.local.LifeOsDatabase.MIGRATION_2_3,
        com.example.data.local.LifeOsDatabase.MIGRATION_3_4,
        com.example.data.local.LifeOsDatabase.MIGRATION_4_5,
        com.example.data.local.LifeOsDatabase.MIGRATION_5_6
      )
      .fallbackToDestructiveMigrationOnDowngrade()
      .addCallback(com.example.data.local.LifeOsDatabase.SAFE_SCHEMA_CALLBACK)
      .build()

    val lifeOsDao = migratedDb.lifeOsDao()

    // 3. Verify that user data from Version 1 is 100% PRESERVED!
    val preservedTasks = lifeOsDao.getTasksForUser("u_test")
    assertEquals(1, preservedTasks.size)
    val task = preservedTasks[0]
    assertEquals("task_v1_legacy", task.id)
    assertEquals("تسک قدیمی کاربر در نسخه ۱", task.title)
    assertEquals("HIGH", task.priority)
    assertEquals("PENDING", task.status)
    // Newly added columns in v4 and v6 have safe defaults without data corruption
    assertEquals(0, task.actualDurationMinutes)
    assertEquals("", task.linkedDebtId)

    val preservedExpense = lifeOsDao.getExpenseById("u_test", "exp_v1_legacy")
    assertTrue(preservedExpense != null)
    assertEquals(450000.0, preservedExpense!!.amount, 0.01)
    assertEquals("خرید ناهار قدیمی", preservedExpense.title)
    assertEquals("TOMAN", preservedExpense.currency)
    // Newly added columns in v3 and v6 have safe defaults
    assertEquals("EXPENSE", preservedExpense.transactionType)
    assertEquals(0.0, preservedExpense.paidAmount, 0.01)
    assertEquals(-1L, preservedExpense.dueEpochDay)
    assertEquals(1, preservedExpense.installmentCount)
    assertEquals("", preservedExpense.linkedDebtId)

    // 4. Verify that new tables added in migrations (v2 cached_habits, etc.) are fully functional
    val newHabit = com.example.data.local.CachedHabitEntity.fromDomain(
      com.example.data.model.Habit(
        id = "habit_post_migration",
        userId = "u_test",
        title = "مطالعه کتاب",
        category = "PERSONAL_GROWTH"
      )
    )
    lifeOsDao.upsertHabit(newHabit)
    val habits = lifeOsDao.observeCachedHabits("u_test").first()
    assertEquals(1, habits.size)
    assertEquals("habit_post_migration", habits[0].id)

    migratedDb.close()
    if (dbFile.exists()) dbFile.delete()
  }
}




