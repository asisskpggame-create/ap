package com.example

import android.content.Context
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.util.WorldClassOsUtils
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
  fun `verify colloquial Toman voice parsing and Iranian bank SMS parser`() {
    val parsedVoice1 = com.example.ai.GeminiAssistantService.parsePersianCommandDeterministic(
      rawText = "۵۰۰ تومان سیگار خریدم",
      todayEpochDay = 20000L,
      defaultCurrency = "TOMAN"
    )
    assertEquals(1, parsedVoice1.operations.size)
    assertEquals("AddExpense", parsedVoice1.operations[0].operationType)
    assertEquals(500_000.0, parsedVoice1.operations[0].amount, 0.01)
    assertEquals("TOMAN", parsedVoice1.operations[0].currency)
    assertEquals("خرید سیگار", parsedVoice1.operations[0].title)

    val parsedVoiceWords = com.example.ai.GeminiAssistantService.parsePersianCommandDeterministic(
      rawText = "پانصد تومن سیگار خریدم",
      todayEpochDay = 20000L,
      defaultCurrency = "TOMAN"
    )
    assertEquals(500_000.0, parsedVoiceWords.operations[0].amount, 0.01)
    assertEquals("خرید سیگار", parsedVoiceWords.operations[0].title)

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
}


