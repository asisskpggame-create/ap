package com.example.sms

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.LifeOsDatabase
import com.example.data.model.ExpenseEntry
import com.example.data.repository.LifeOsRepository
import com.example.ui.components.IranianBankSmsParser
import com.example.util.BankSmsScannerUtility
import com.example.util.PersianDateAndUnitUtils
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BankSmsEngineAndAccountingTest {

    private lateinit var context: Context
    private lateinit var db: LifeOsDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, LifeOsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `test slash and dot thousands separators and decimal amounts in BankSmsNormalizer`() {
        assertEquals(2_500_000.0, BankSmsNormalizer.parseDecimalAmount("2/500/000")?.toDouble() ?: 0.0, 0.001)
        assertEquals(2_500_000.0, BankSmsNormalizer.parseDecimalAmount("۲/۵۰۰/۰۰۰")?.toDouble() ?: 0.0, 0.001)
        assertEquals(2_500_000.0, BankSmsNormalizer.parseDecimalAmount("2.500.000")?.toDouble() ?: 0.0, 0.001)
        assertEquals(1250.50, BankSmsNormalizer.parseDecimalAmount("1,250.50")?.toDouble() ?: 0.0, 0.001)
        assertEquals(850_000.0, BankSmsNormalizer.parseDecimalAmount("٨٥٠،٠٠٠")?.toDouble() ?: 0.0, 0.001)
    }

    @Test
    fun `test date time card and reference tokens are never confused with transaction amount`() {
        val sms = """
            بانک ملت
            حساب 123456789
            کارت 6104****5892
            مورخ 1403/08/15 ساعت 14:32:10
            برداشت مبلغ 2/450/000 ریال
            کارمزد: 5,000 ریال
            مانده: 48/500/000 ریال
            پیگیری: 987654321
        """.trimIndent()

        val result = BankSmsParser.parse(sms, "+98982000")
        assertEquals(BankSmsParseStatus.SUCCESS, result.status)
        assertEquals("بانک ملت", result.bankName)
        assertEquals(BankTransactionType.EXPENSE, result.transactionType)
        assertEquals(2_450_000.0, result.rawAmountDetected, 0.001)
        assertEquals(245_000.0, result.amountInToman, 0.001)
        assertEquals(4_850_000.0, result.balanceInToman ?: 0.0, 0.001)
        assertEquals(500.0, result.feeInToman ?: 0.0, 0.001)
        assertEquals("5892", result.cardOrAccountSuffix)
        assertEquals("987654321", result.referenceNumber)
        assertTrue(result.dateTimeText.orEmpty().contains("1403/08/15"))
    }

    @Test
    fun `test missing currency unit marks result as NEEDS_REVIEW with UNKNOWN detectedCurrency`() {
        val ambiguousCurrencySms = """
            بانک سامان
            برداشت از حساب
            مبلغ: 350,000
            مورخ 1403/08/15
        """.trimIndent()

        val result = BankSmsParser.parse(ambiguousCurrencySms)
        assertEquals(BankSmsParseStatus.NEEDS_REVIEW, result.status)
        assertEquals("UNKNOWN", result.detectedCurrency)
        assertFalse(result.wasInRial)
        assertEquals(350_000.0, result.amountInToman, 0.001)
        assertTrue("currency" in result.uncertainFields)
        assertTrue(result.statusMessageFa.contains("واحد پول"))
    }

    @Test
    fun `test shared sender 982000 disambiguates by body first and marks sender-only match as NEEDS_REVIEW`() {
        // Case 1: Sender is 982000, body explicitly says بانک سامان -> Saman wins over Mellat
        val samanOnSharedSender = """
            بانک سامان
            واریز به حساب
            مبلغ: 15,000,000 ریال
            مانده: 45,000,000 ریال
        """.trimIndent()
        val resSaman = BankSmsParser.parse(samanOnSharedSender, "982000")
        assertEquals("بانک سامان", resSaman.bankName)
        assertEquals(BankSmsParseStatus.SUCCESS, resSaman.status)
        assertFalse("bankName" in resSaman.uncertainFields)

        // Case 2: Sender is 982000, body has no bank name -> flagged as NEEDS_REVIEW
        val namelessOnSharedSender = """
            خرید با کارت 5892
            مبلغ: 1,200,000 ریال
            مانده: 8,000,000 ریال
        """.trimIndent()
        val resAmbiguous = BankSmsParser.parse(namelessOnSharedSender, "982000")
        assertEquals(BankSmsParseStatus.NEEDS_REVIEW, resAmbiguous.status)
        assertTrue("bankName" in resAmbiguous.uncertainFields)
    }

    @Test
    fun `test OTP failed balance-only and promotional SMS are strictly rejected`() {
        val otpSms = "بانک ملت\nرمز پویا خرید اینترنتی: 482910\nمبلغ: 2,500,000 ریال"
        assertEquals(BankSmsParseStatus.IGNORED_OTP, BankSmsParser.parse(otpSms).status)
        assertNull(BankSmsScannerUtility.parseBankSms(otpSms))
        assertNull(IranianBankSmsParser.parseBankSms(otpSms))

        val failedSms = "بانک ملی\nتراکنش ناموفق به مبلغ 1,500,000 ریال\nمبلغ کسر شده به حساب بازگشت داده می‌شود"
        assertEquals(BankSmsParseStatus.IGNORED_NON_FINANCIAL, BankSmsParser.parse(failedSms).status)
        assertNull(BankSmsScannerUtility.parseBankSms(failedSms))

        val balanceOnlySms = "بانک پاسارگاد\nمانده حساب شما: 95,400,000 ریال\nمورخ 1403/08/15"
        assertEquals(BankSmsParseStatus.IGNORED_NON_FINANCIAL, BankSmsParser.parse(balanceOnlySms).status)
        assertNull(BankSmsScannerUtility.parseBankSms(balanceOnlySms))

        val promoSms = "مشتری گرامی، امتیاز وام و تسهیلات شما 500,000,000 ریال در قرعه کشی جشنواره فعال شد"
        assertEquals(BankSmsParseStatus.IGNORED_NON_FINANCIAL, BankSmsParser.parse(promoSms).status)
        assertNull(BankSmsScannerUtility.parseBankSms(promoSms))
    }

    @Test
    fun `test refund and reversal SMS is classified as INCOME`() {
        val refundSms = """
            بلوبانک
            برگشت خرید اینترنتی
            مبلغ: 450,000 تومان به حساب شما واریز شد
            موجودی: 3,200,000 تومان
        """.trimIndent()

        val result = BankSmsParser.parse(refundSms, "BluBank")
        assertEquals(BankSmsParseStatus.SUCCESS, result.status)
        assertEquals(BankTransactionType.INCOME, result.transactionType)
        assertEquals(450_000.0, result.amountInToman, 0.001)
    }

    @Test
    fun `test delegation parity between BankSmsParser BankSmsScannerUtility and IranianBankSmsParser`() {
        val raw = "بانک تجارت\nبرداشت: 4,200,000 ریال\nکارت 4123\nمانده: 18,000,000 ریال"
        val core = BankSmsParser.parse(raw)
        val scanner = BankSmsScannerUtility.parseBankSms(raw)
        val sheet = IranianBankSmsParser.parseBankSms(raw)

        assertNotNull(scanner)
        assertNotNull(sheet)
        assertEquals(core.bankName, scanner!!.bankName)
        assertEquals(core.bankName, sheet!!.bankName)
        assertEquals(core.amountInToman, scanner.amountInToman, 0.001)
        assertEquals(core.amountInToman, sheet.amountInToman, 0.001)
        assertEquals(core.fingerprint, scanner.idempotencyKey)
        assertEquals(core.fingerprint, sheet.fingerprint)
    }

    @Test
    fun `test BigDecimal financial precision and IOU remainingAmount after partial payments`() {
        // 0.1 + 0.2 floating point drift check
        val driftEntries = listOf(
            ExpenseEntry(id = "1", title = "A", amount = 0.1, currency = "USD", transactionType = "EXPENSE"),
            ExpenseEntry(id = "2", title = "B", amount = 0.2, currency = "USD", transactionType = "EXPENSE")
        )
        val aggregated = PersianDateAndUnitUtils.aggregateExpensesByCurrency(driftEntries)
        assertEquals(0.30, aggregated["USD"] ?: 0.0, 0.000001)

        // IOU partial payment remainingAmount check
        val debt = ExpenseEntry(
            id = "dbt_1",
            title = "وام قرض‌الحسنه",
            amount = 10_000_000.0,
            paidAmount = 3_500_000.0,
            currency = "TOMAN",
            transactionType = "DEBT"
        )
        val receivable = ExpenseEntry(
            id = "rcv_1",
            title = "طلب پروژه طراحی",
            amount = 5_000_000.0,
            paidAmount = 2_000_000.0,
            currency = "TOMAN",
            transactionType = "RECEIVABLE"
        )

        assertEquals(6_500_000.0, debt.remainingAmount, 0.001)
        assertEquals(3_000_000.0, receivable.remainingAmount, 0.001)

        val debtMap = PersianDateAndUnitUtils.aggregateDebtsByCurrency(listOf(debt))
        val receivableMap = PersianDateAndUnitUtils.aggregateReceivablesByCurrency(listOf(receivable))

        assertEquals(6_500_000.0, debtMap["TOMAN"] ?: 0.0, 0.001)
        assertEquals(3_000_000.0, receivableMap["TOMAN"] ?: 0.0, 0.001)
    }

    @Test
    fun `test persistent Room idempotencyKey deduplicates SMS across new repository instances`() = runBlocking {
        val sms = "بانک ملت\nخرید با کارت 5892\nمبلغ: 2,450,000 ریال\nفروشگاه افق کوروش\nپیگیری: 778899"
        val parsed = BankSmsParser.parse(sms, "982000")
        assertTrue(parsed.fingerprint.isNotBlank())

        // First Repository instance saves the SMS transaction
        val repoInstance1 = LifeOsRepository(cloudDb = null, localDao = db.lifeOsDao())
        val firstRes = repoInstance1.addExpense(
            title = parsed.suggestedTitle,
            category = parsed.suggestedCategory,
            amount = parsed.amountInToman,
            currency = "TOMAN",
            notes = BankSmsNormalizer.maskSensitiveSmsContent(sms),
            inputSource = "SMS_PARSER",
            idempotencyKey = parsed.fingerprint,
            transactionType = parsed.transactionType.code
        )
        assertTrue(firstRes.isSuccess)
        val firstId = firstRes.getOrThrow()

        // Second Repository instance (simulating app restart with empty in-memory cache) attempts to save the same SMS
        val repoInstance2 = LifeOsRepository(cloudDb = null, localDao = db.lifeOsDao())
        val secondRes = repoInstance2.addExpense(
            title = parsed.suggestedTitle,
            category = parsed.suggestedCategory,
            amount = parsed.amountInToman,
            currency = "TOMAN",
            notes = BankSmsNormalizer.maskSensitiveSmsContent(sms),
            inputSource = "SMS_PARSER",
            idempotencyKey = parsed.fingerprint,
            transactionType = parsed.transactionType.code
        )
        assertTrue(secondRes.isSuccess)
        val secondId = secondRes.getOrThrow()

        // Must return the exact same expense ID and keep only 1 row in SQLite
        assertEquals(firstId, secondId)
        val allStored = db.lifeOsDao().getExpensesForUser(repoInstance2.resolveActiveUserId())
        assertEquals(1, allStored.size)
        assertEquals(245_000.0, allStored.first().amount, 0.001)
        assertEquals(parsed.fingerprint, allStored.first().idempotencyKey)
    }
}
