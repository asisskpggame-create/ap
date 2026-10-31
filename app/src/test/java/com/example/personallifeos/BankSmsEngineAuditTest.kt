package com.example.personallifeos

import com.example.personallifeos.data.model.DebtItem
import com.example.personallifeos.data.model.DebtType
import com.example.personallifeos.data.model.ExpenseItem
import com.example.personallifeos.data.model.TransactionType
import com.example.personallifeos.sms.BankType
import com.example.personallifeos.sms.DetectedCurrencyUnit
import com.example.personallifeos.sms.IranianBankSmsEngine
import com.example.personallifeos.sms.SmsTransactionDirection
import com.example.personallifeos.sms.SmsVerificationStatus
import com.example.personallifeos.util.DetectedIntentType
import com.example.personallifeos.util.PersianUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BankSmsEngineAuditTest {

    @Before
    fun setUp() {
        IranianBankSmsEngine.resetDefaultState()
    }

    @Test
    fun `regression 1 - ambiguous SMS with amount and bank but UNKNOWN direction is not auto-accepted and never defaults to expense`() {
        val ambiguousSms = """
            بانک ملت
            حساب ۶۱۰۴۳۳******۱۲۳۴
            مبلغ: ۸۵۰,۰۰۰ ریال
            مانده: ۹,۱۰۰,۰۰۰ ریال
            تاریخ: ۱۴۰۳/۰۹/۱۸
        """.trimIndent()

        val parsed = IranianBankSmsEngine.parse(ambiguousSms)
        assertEquals(BankType.MELLAT, parsed.bank)
        assertEquals(85_000L, parsed.amountToman)
        assertEquals(SmsTransactionDirection.UNKNOWN, parsed.direction)
        assertFalse("UNKNOWN direction must never be marked as expense", parsed.direction.isExpense)
        assertEquals(SmsVerificationStatus.NEEDS_REVIEW, parsed.verificationStatus)

        val validationError = IranianBankSmsEngine.validateManualConfirmReview(
            parsed = parsed,
            overrideDirection = SmsTransactionDirection.UNKNOWN,
            overrideCurrency = DetectedCurrencyUnit.RIAL
        )
        assertNotNull("Confirming UNKNOWN direction must be rejected", validationError)

        val validConfirm = IranianBankSmsEngine.validateManualConfirmReview(
            parsed = parsed,
            overrideDirection = SmsTransactionDirection.WITHDRAWAL,
            overrideCurrency = DetectedCurrencyUnit.RIAL
        )
        assertNull("Confirming with explicit direction and currency must succeed", validConfirm)
    }

    @Test
    fun `regression 2 - one verified template for a bank does not activate another unverified template of the same bank`() {
        assertTrue(IranianBankSmsEngine.isTemplateVerified(BankType.MELLAT, "MELLAT_WITHDRAW", 1))
        assertFalse(IranianBankSmsEngine.isTemplateVerified(BankType.MELLAT, "MELLAT_DEPOSIT", 1))

        val mellatWithdrawSms = """
            بانک ملت
            برداشت از: ۶۱۰۴۳۳******۱۲۳۴
            مبلغ: ۵۰۰,۰۰۰ ریال
            مانده: ۸,۴۲۰,۰۰۰ ریال
        """.trimIndent()

        val mellatDepositSms = """
            بانک ملت
            واریز به: ۶۱۰۴۳۳******۱۲۳۴
            مبلغ: ۵۰۰,۰۰۰ ریال
            مانده: ۸,۹۲۰,۰۰۰ ریال
        """.trimIndent()

        val withdrawResult = IranianBankSmsEngine.parse(mellatWithdrawSms)
        assertEquals(SmsVerificationStatus.AUTO_ACCEPTED, withdrawResult.verificationStatus)

        val depositResult = IranianBankSmsEngine.parse(mellatDepositSms)
        assertEquals("MELLAT_DEPOSIT", depositResult.templateId)
        assertFalse(depositResult.isTemplateVerified)
        assertEquals(SmsVerificationStatus.NEEDS_REVIEW, depositResult.verificationStatus)
    }

    @Test
    fun `regression 3 - bumping template version invalidates previous template verification`() {
        val melliSms = """
            بانک ملی ایران
            واریز: ۱,۲۵۰,۰۰۰ ریال
            به: ۰۱*****۱۲۳۴
            مانده: ۴۵,۸۹۰,۰۰۰ ریال
        """.trimIndent()

        val v1Result = IranianBankSmsEngine.parse(melliSms)
        assertEquals(SmsVerificationStatus.AUTO_ACCEPTED, v1Result.verificationStatus)

        IranianBankSmsEngine.updateTemplateVersion("MELLI_STD", 2)
        val v2Result = IranianBankSmsEngine.parse(melliSms)
        assertEquals(2, v2Result.templateVersion)
        assertFalse(v2Result.isTemplateVerified)
        assertEquals(SmsVerificationStatus.NEEDS_REVIEW, v2Result.verificationStatus)

        IranianBankSmsEngine.verifyTemplate(BankType.MELLI, "MELLI_STD", 2)
        val v2VerifiedResult = IranianBankSmsEngine.parse(melliSms)
        assertEquals(SmsVerificationStatus.AUTO_ACCEPTED, v2VerifiedResult.verificationStatus)
    }

    @Test
    fun `regression 4 - mismatched or missing required sender blocks auto-registration`() {
        IranianBankSmsEngine.configureBankSenderPolicy(
            bank = BankType.MELLI,
            senderNumber = "98300017",
            requireSenderMatch = true,
            verifyDefaultTemplate = true
        )

        val melliSms = """
            بانک ملی ایران
            واریز: ۱,۲۵۰,۰۰۰ ریال
            به: ۰۱*****۱۲۳۴
            مانده: ۴۵,۸۹۰,۰۰۰ ریال
        """.trimIndent()

        val missingSenderResult = IranianBankSmsEngine.parse(melliSms, senderAddress = null)
        assertFalse(missingSenderResult.isSenderMatched)
        assertEquals(SmsVerificationStatus.NEEDS_REVIEW, missingSenderResult.verificationStatus)

        val wrongSenderResult = IranianBankSmsEngine.parse(melliSms, senderAddress = "+989121234567")
        assertFalse(wrongSenderResult.isSenderMatched)
        assertEquals(SmsVerificationStatus.NEEDS_REVIEW, wrongSenderResult.verificationStatus)

        val validSenderResult = IranianBankSmsEngine.parse(melliSms, senderAddress = "98300017")
        assertTrue(validSenderResult.isSenderMatched)
        assertEquals(SmsVerificationStatus.AUTO_ACCEPTED, validSenderResult.verificationStatus)
    }

    @Test
    fun `regression 5 - duplicate SMS registration is strictly blocked via idempotencyKey`() {
        val bluSms = """
            بلو
            برداشت پول
            علیرضا عزیز، 900,000 ریال از حساب شما پرید.
            موجودی: 64,161,452 ریال
        """.trimIndent()

        val firstParse = IranianBankSmsEngine.parse(bluSms, "982000")
        assertEquals(SmsVerificationStatus.AUTO_ACCEPTED, firstParse.verificationStatus)

        IranianBankSmsEngine.markIdempotencyKeyProcessed(firstParse.idempotencyKey)

        val secondParse = IranianBankSmsEngine.parse(bluSms, "982000")
        assertEquals(SmsVerificationStatus.DUPLICATE, secondParse.verificationStatus)

        val manualErr = IranianBankSmsEngine.validateManualConfirmReview(
            parsed = firstParse,
            overrideDirection = SmsTransactionDirection.WITHDRAWAL,
            overrideCurrency = DetectedCurrencyUnit.RIAL
        )
        assertNotNull(manualErr)
    }

    @Test
    fun `regression 6 - OTP and failed transaction SMS are strictly rejected`() {
        val otpSms = "بانک ملت\nرمز پویا: ۱۲۳۴۵۶\nمبلغ: ۵۰۰,۰۰۰ ریال"
        val otpResult = IranianBankSmsEngine.parse(otpSms)
        assertEquals(SmsVerificationStatus.REJECTED, otpResult.verificationStatus)
        assertTrue(otpResult.isOtpOrServiceMessage)

        val failedSms = "بانک ملی\nتراکنش ناموفق\nمبلغ: ۹۰۰,۰۰۰ ریال از حساب کسر نشد."
        val failedResult = IranianBankSmsEngine.parse(failedSms)
        assertEquals(SmsVerificationStatus.REJECTED, failedResult.verificationStatus)
        assertTrue(failedResult.isFailedTransaction)
    }

    @Test
    fun `regression 7 - BigDecimal accounting excludes TRANSFER from income and expense and computes remainingAmount accurately`() {
        val list = listOf(
            ExpenseItem("1", "حقوق", 20_000_000L, "حقوق", "1405/08/01", TransactionType.INCOME),
            ExpenseItem("2", "خرید", 1_500_000L, "خرید", "1405/08/02", TransactionType.EXPENSE),
            ExpenseItem("3", "انتقال داخلی", 5_000_000L, "انتقال", "1405/08/03", TransactionType.TRANSFER)
        )
        assertEquals(20_000_000L, PersianUtils.aggregateIncomesToman(list))
        assertEquals(1_500_000L, PersianUtils.aggregateExpensesToman(list))
        assertEquals(5_000_000L, PersianUtils.aggregateTransfersToman(list))
        assertEquals(18_500_000L, PersianUtils.aggregateNetBalanceToman(list))

        val debts = listOf(
            DebtItem("d1", "وام", DebtType.DEBT, totalAmount = 12_000_000L, paidAmount = 3_000_000L, dueDate = "1405/08/15", isSettled = false, personName = "بانک")
        )
        assertEquals(9_000_000L, PersianUtils.aggregateTotalDebtsRemainingToman(debts))
    }

    @Test
    fun `regression 8 - Colloquial Toman normalizer and Transaction Intent Engine`() {
        val col = PersianUtils.normalizeColloquialTomanAmount("۵۰۰ تومن سیگار خریدم")
        assertNotNull(col)
        assertEquals(500_000L, col!!.amountToman)
        assertTrue(col.wasColloquialMultiplied)

        val intent = PersianUtils.detectTransactionIntent("۳۰۰ تومن به علی قرض دادم")
        assertEquals(DetectedIntentType.RECEIVABLE, intent.intentType)
        assertEquals(300_000L, intent.amountToman)
    }

    @Test
    fun `regression 9 - Mirza sender-locked bank only accepts SMS from its assigned sender number and resolves nameless SMS by sender`() {
        IranianBankSmsEngine.configureBankSenderPolicy(
            bank = BankType.MELLI,
            senderNumber = "98300017",
            requireSenderMatch = true
        )

        // 1. Nameless SMS coming from the exact assigned sender (98300017) is recognized as Bank Melli and auto-accepted
        val namelessDepositFromMelliSender = """
            واریز: ۲,۵۰۰,۰۰۰ ریال
            مانده: ۱۸,۰۰۰,۰۰۰ ریال
            پیگیری: ۵۵۴۴۳۳
        """.trimIndent()
        val validResult = IranianBankSmsEngine.parse(namelessDepositFromMelliSender, senderAddress = "98300017")
        assertEquals(BankType.MELLI, validResult.bank)
        assertEquals(SmsTransactionDirection.DEPOSIT, validResult.direction)
        assertEquals(250_000L, validResult.amountToman)
        assertTrue(validResult.isSenderMatched)
        assertEquals(SmsVerificationStatus.AUTO_ACCEPTED, validResult.verificationStatus)

        // 2. SMS claiming to be Bank Melli but coming from another bank or personal number is strictly blocked from auto-accept
        val spoofedSenderSms = """
            بانک ملی ایران
            برداشت: ۹۰۰,۰۰۰ ریال
            مانده: ۱۷,۱۰۰,۰۰۰ ریال
        """.trimIndent()
        val blockedResult = IranianBankSmsEngine.parse(spoofedSenderSms, senderAddress = "9850009999")
        assertFalse(blockedResult.isSenderMatched)
        assertEquals(SmsVerificationStatus.NEEDS_REVIEW, blockedResult.verificationStatus)
    }

    @Test
    fun `regression 10 - TopicDashboardStats computes exact 60 percent progress when 6 of 10 tasks are completed and tracks overdue and nearest deadline`() {
        val topicId = "topic_uni"
        val tasks = (1..10).map { idx ->
            val isDone = idx <= 6
            com.example.personallifeos.data.model.TaskItem(
                id = "t_$idx",
                title = "مأموریت $idx",
                completed = isDone,
                status = if (isDone) com.example.personallifeos.data.model.TaskStatus.COMPLETED else com.example.personallifeos.data.model.TaskStatus.TODO,
                priority = if (idx == 7) com.example.personallifeos.data.model.TaskPriority.URGENT else com.example.personallifeos.data.model.TaskPriority.MEDIUM,
                category = com.example.personallifeos.data.model.TaskCategory.GROWTH,
                dueDate = if (idx == 7) "۱۴۰۵/۰۸/۰۲" else "۱۴۰۵/۰۸/۱۰",
                topicId = topicId,
                isMajorMission = (idx == 8)
            )
        }

        val stats = com.example.personallifeos.data.model.TopicDashboardStats.compute(
            topicId = topicId,
            allTasks = tasks,
            todayJalali = "۱۴۰۵/۰۸/۰۵"
        )

        assertEquals(10, stats.totalTasks)
        assertEquals(6, stats.completedTasks)
        assertEquals(4, stats.remainingTasks)
        assertEquals(60, stats.progressPercent)
        assertEquals(1, stats.overdueTasks)
        assertEquals(2, stats.urgentOrMajorTasks.size) // idx 7 (URGENT) and idx 8 (isMajorMission)
        assertEquals("۱۴۰۵/۰۸/۰۲", stats.nearestDeadline)
    }

    @Test
    fun `regression 11 - TaskEntity preserves subtasks, status, topicId, and activity log across Room serialization`() {
        val original = com.example.personallifeos.data.model.TaskItem(
            id = "mission_1",
            title = "تدوین راف‌کات",
            completed = false,
            status = com.example.personallifeos.data.model.TaskStatus.IN_PROGRESS,
            priority = com.example.personallifeos.data.model.TaskPriority.HIGH,
            category = com.example.personallifeos.data.model.TaskCategory.WORK,
            dueDate = "۱۴۰۵/۰۸/۰۹",
            description = "پروژه مستند",
            topicId = "topic_film",
            isMajorMission = true,
            subtasks = listOf(
                com.example.personallifeos.data.model.SubtaskItem("s1", "انتخاب موسیقی", true),
                com.example.personallifeos.data.model.SubtaskItem("s2", "اصلاح رنگ", false)
            ),
            activityLog = listOf("۱۴۰۵/۰۸/۰۵: شروع تدوین")
        )

        val entity = com.example.personallifeos.data.local.TaskEntity.fromModel(original)
        val restored = entity.toModel()

        assertEquals(original.id, restored.id)
        assertEquals(original.topicId, restored.topicId)
        assertEquals(com.example.personallifeos.data.model.TaskStatus.IN_PROGRESS, restored.status)
        assertTrue(restored.isMajorMission)
        assertEquals(2, restored.subtasks.size)
        assertEquals("انتخاب موسیقی", restored.subtasks[0].title)
        assertTrue(restored.subtasks[0].isCompleted)
        assertEquals("اصلاح رنگ", restored.subtasks[1].title)
        assertFalse(restored.subtasks[1].isCompleted)
        assertEquals(1, restored.activityLog.size)
    }

    @Test
    fun `regression 12 - IranianBankSmsEngine parses real transaction message and extracts bank, amount, and description for handleAddExpense`() {
        val rawMellatSms = "بانک ملت • برداشت از ۶۱۰۴۳۳******۱۲۳۴ مبلغ ۲۵۰,۰۰۰ ریال مانده: ۱۵,۰۰۰,۰۰۰ ریال کد پیگیری: ۹۸۲۳۴۱"
        val parsed = com.example.personallifeos.sms.IranianBankSmsEngine.parse(rawMellatSms, "98200033")

        assertEquals(com.example.personallifeos.sms.BankType.MELLAT, parsed.bank)
        assertEquals(25000L, parsed.amountToman)
        assertEquals(250000L, parsed.amountRial)
        assertTrue(parsed.direction.isExpense)

        var addedTitle = ""
        var addedAmount = 0L
        var addedCategory = ""
        var addedType = com.example.personallifeos.data.model.TransactionType.EXPENSE

        val handleAddExpense: (String, Long, String, com.example.personallifeos.data.model.TransactionType) -> Unit = { title, amount, category, type ->
            addedTitle = title
            addedAmount = amount
            addedCategory = category
            addedType = type
        }

        val txType = when (parsed.direction) {
            com.example.personallifeos.sms.SmsTransactionDirection.DEPOSIT -> com.example.personallifeos.data.model.TransactionType.INCOME
            else -> com.example.personallifeos.data.model.TransactionType.EXPENSE
        }
        val autoTitle = "برداشت از ${parsed.bank.shortNameFa}"

        handleAddExpense(autoTitle, parsed.amountToman, parsed.suggestedCategory, txType)

        assertEquals("برداشت از ملت", addedTitle)
        assertEquals(25000L, addedAmount)
        assertEquals(com.example.personallifeos.data.model.TransactionType.EXPENSE, addedType)
    }
}
