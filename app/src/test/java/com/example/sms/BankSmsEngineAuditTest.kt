package com.example.sms

import com.example.data.model.ExpenseEntry
import com.example.util.PersianDateAndUnitUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class BankSmsEngineAuditTest {

    @Test
    fun `verify BankSmsNormalizer digits Arabic chars RTL control chars and decimal preservation`() {
        val raw = "\u200Fبانك صادرات\u200E\nبرداشت از كارت ۶۰۳۷****۱۲۳۴\nمبلغ: ۲/۵۰۰/۰۰۰ ريال\nمانده: ۱۵.۴۰۰.۰۰۰ ریال"
        val normalized = BankSmsNormalizer.normalize(raw)

        // Invisible RTL/LTR marks removed, Arabic Kaf/Ye unified, digits converted to ASCII, thousand separators unified
        assertTrue(!normalized.contains("\u200F") && !normalized.contains("\u200E"))
        assertTrue(normalized.contains("بانک صادرات"))
        assertTrue(normalized.contains("کارت"))
        assertTrue(normalized.contains("ریال"))
        assertTrue(normalized.contains("2/500/000") || normalized.contains("2,500,000"))
        assertTrue(normalized.contains("15.400.000") || normalized.contains("15,400,000"))

        // True decimal numbers MUST be preserved and never stripped
        val decimalNorm = BankSmsNormalizer.normalize("1250.50 USD")
        assertTrue(decimalNorm.contains("1250.50"))
        val parsedDecimal = BankSmsNormalizer.parseFinancialAmountBigDecimal("1,250.50")
        assertNotNull(parsedDecimal)
        assertEquals(0, BigDecimal("1250.50").compareTo(parsedDecimal))

        // Multi-dot thousand separators MUST be parsed as integer 15,400,000
        val parsedMultiDot = BankSmsNormalizer.parseFinancialAmountBigDecimal("15.400.000")
        assertNotNull(parsedMultiDot)
        assertEquals(0, BigDecimal("15400000").compareTo(parsedMultiDot))
    }

    @Test
    fun `verify shared sender 982000 disambiguates Bank Melli vs Bank Mellat by body content`() {
        val melliSmsFromSharedSender = """
            بانک ملی ایران
            خرید کارت: 6037****8899
            مبلغ: 1,200,000 ریال
            مانده: 9,800,000 ریال
        """.trimIndent()
        val melliParsed = BankSmsParser.parse(melliSmsFromSharedSender, senderNumber = "+982000")
        assertEquals(BankSmsParseStatus.SUCCESS, melliParsed.status)
        assertEquals("بانک ملی ایران", melliParsed.bankName)
        assertEquals(BankTransactionType.EXPENSE, melliParsed.transactionType)
        assertEquals(120_000.0, melliParsed.amountInToman, 0.001)
        assertEquals("8899", melliParsed.cardOrAccountSuffix)

        val mellatSmsFromSharedSender = """
            بانک ملت
            برداشت مبلغ 3,400,000 ریال
            حساب 123456
            مانده 20,000,000 ریال
        """.trimIndent()
        val mellatParsed = BankSmsParser.parse(mellatSmsFromSharedSender, senderNumber = "+982000")
        assertEquals(BankSmsParseStatus.SUCCESS, mellatParsed.status)
        assertEquals("بانک ملت", mellatParsed.bankName)
        assertEquals(340_000.0, mellatParsed.amountInToman, 0.001)
    }

    @Test
    fun `verify refund reversal fee and self transfer classification`() {
        // 1. Refund / Reversal ("برگشت خرید" / "اصلاحیه") MUST be classified as INCOME, never EXPENSE
        val refundSms = """
            بانک ملت
            برگشت خرید با کارت 6104****4321
            مبلغ: 850,000 ریال
            مانده: 5,850,000 ریال
        """.trimIndent()
        val refundResult = BankSmsParser.parse(refundSms)
        assertEquals(BankSmsParseStatus.SUCCESS, refundResult.status)
        assertEquals(BankTransactionType.INCOME, refundResult.transactionType)
        assertEquals(85_000.0, refundResult.amountInToman, 0.001)

        // 2. Bank Fee ("کسر کارمزد") -> EXPENSE + feeInToman populated
        val feeOnlySms = """
            بانک سامان
            کسر کارمزد خدمات پیامک
            مبلغ: 150,000 ریال
            مانده: 12,000,000 ریال
        """.trimIndent()
        val feeResult = BankSmsParser.parse(feeOnlySms)
        assertEquals(BankSmsParseStatus.SUCCESS, feeResult.status)
        assertEquals(BankTransactionType.EXPENSE, feeResult.transactionType)
        assertEquals(15_000.0, feeResult.amountInToman, 0.001)

        // 3. Transfer with separate fee line
        val transferWithFeeSms = """
            بلوبانک
            انتقال وجه کارت به کارت
            مبلغ: 2,000,000 تومان
            کارمزد: 600 تومان
            موجودی: 8,500,000 تومان
        """.trimIndent()
        val transferResult = BankSmsParser.parse(transferWithFeeSms)
        assertEquals(BankSmsParseStatus.SUCCESS, transferResult.status)
        assertEquals(BankTransactionType.TRANSFER, transferResult.transactionType)
        assertEquals(2_000_000.0, transferResult.amountInToman, 0.001)
        assertEquals(600.0, transferResult.feeInToman ?: 0.0, 0.001)
    }

    @Test
    fun `verify missing currency unit returns NEEDS_REVIEW and never silently divides by 10`() {
        val noCurrencySms = """
            بانک پاسارگاد
            برداشت از حساب
            مبلغ: 500,000
            مانده: 4,500,000
        """.trimIndent()
        val res = BankSmsParser.parse(noCurrencySms)
        assertEquals(BankSmsParseStatus.NEEDS_REVIEW, res.status)
        assertEquals(BankTransactionType.EXPENSE, res.transactionType)
        assertEquals(500_000.0, res.amountInToman, 0.001)
        assertTrue(res.statusMessageFa.contains("واحد پول"))
    }

    @Test
    fun `verify multi-bank Iranian fixtures across 13 banks and non-financial rejections`() {
        data class BankFixture(
            val sms: String,
            val expectedBank: String,
            val expectedType: BankTransactionType,
            val expectedToman: Double
        )

        val fixtures = listOf(
            BankFixture("بانک ملت\nخرید با کارت 6104****1111\nمبلغ: 4,200,000 ریال\nمانده: 10,000,000 ریال", "بانک ملت", BankTransactionType.EXPENSE, 420_000.0),
            BankFixture("بانک ملی ایران\nواریز به حساب\nمبلغ: 25,000,000 ریال\nمانده: 60,000,000 ریال", "بانک ملی ایران", BankTransactionType.INCOME, 2_500_000.0),
            BankFixture("بلوبانک\nخرید از اسنپ فود\nمبلغ 380,000 تومان\nموجودی 4,100,000 تومان", "بلوبانک سامان", BankTransactionType.EXPENSE, 380_000.0),
            BankFixture("بانک سامان\nبرداشت: 1,500,000 ریال\nمانده: 9,000,000 ریال", "بانک سامان", BankTransactionType.EXPENSE, 150_000.0),
            BankFixture("بانک پاسارگاد\nواریز سود سپرده\nمبلغ: 6,000,000 ریال\nمانده: 120,000,000 ریال", "بانک پاسارگاد", BankTransactionType.INCOME, 600_000.0),
            BankFixture("بانک تجارت\nخرید پایانه فروش\nمبلغ: 950,000 ریال\nمانده: 7,500,000 ریال", "بانک تجارت", BankTransactionType.EXPENSE, 95_000.0),
            BankFixture("بانک صادرات ایران\nبرداشت مبلغ 2,100,000 ریال\nمانده 14,000,000 ریال", "بانک صادرات", BankTransactionType.EXPENSE, 210_000.0),
            BankFixture("بانک پارسیان\nواریز مبلغ 8,000,000 ریال\nمانده 30,000,000 ریال", "بانک پارسیان", BankTransactionType.INCOME, 800_000.0),
            BankFixture("بانک کشاورزی\nبرداشت از کارت\nمبلغ: 1,750,000 ریال\nمانده: 11,250,000 ریال", "بانک کشاورزی", BankTransactionType.EXPENSE, 175_000.0),
            BankFixture("بانک قرض الحسنه رسالت\nبرداشت: 3,000,000 ریال\nمانده: 15,000,000 ریال", "بانک رسالت", BankTransactionType.EXPENSE, 300_000.0),
            BankFixture("بانک مسکن\nواریز به حساب\nمبلغ: 12,000,000 ریال\nمانده: 45,000,000 ریال", "بانک مسکن", BankTransactionType.INCOME, 1_200_000.0),
            BankFixture("بانک سپه\nخرید کارت\nمبلغ: 540,000 ریال\nمانده: 6,460,000 ریال", "بانک سپه", BankTransactionType.EXPENSE, 54_000.0),
            BankFixture("بانک آینده\nواریز وجه\nمبلغ: 9,500,000 ریال\nمانده: 29,500,000 ریال", "بانک آینده", BankTransactionType.INCOME, 950_000.0)
        )

        fixtures.forEach { fx ->
            val res = BankSmsParser.parse(fx.sms)
            assertEquals("Failed status for ${fx.expectedBank}", BankSmsParseStatus.SUCCESS, res.status)
            assertEquals("Failed bank name for ${fx.expectedBank}", fx.expectedBank, res.bankName)
            assertEquals("Failed tx type for ${fx.expectedBank}", fx.expectedType, res.transactionType)
            assertEquals("Failed amount for ${fx.expectedBank}", fx.expectedToman, res.amountInToman, 0.001)
            assertTrue("Fingerprint must not be blank", res.fingerprint.isNotBlank())
        }
    }

    @Test
    fun `verify BigDecimal financial aggregation and account balance tracking with transfers`() {
        val entries = listOf(
            ExpenseEntry(
                id = "1",
                title = "واریز حقوق",
                amount = 10_000_000.0,
                currency = "TOMAN",
                transactionType = "INCOME",
                notes = "[کارت اصلی]"
            ),
            ExpenseEntry(
                id = "2",
                title = "خرید روزانه",
                amount = 1_500_000.0,
                currency = "TOMAN",
                transactionType = "EXPENSE",
                notes = "[کارت اصلی]"
            ),
            ExpenseEntry(
                id = "3",
                title = "انتقال به پس‌انداز",
                amount = 3_000_000.0,
                currency = "TOMAN",
                transactionType = "TRANSFER",
                notes = "[کارت اصلی -> پس‌انداز]"
            )
        )

        // Net balance across all accounts excludes TRANSFER (10,000,000 - 1,500,000 = 8,500,000)
        val netByCurrency = PersianDateAndUnitUtils.aggregateNetBalanceByCurrency(entries)
        assertEquals(8_500_000.0, netByCurrency["TOMAN"] ?: 0.0, 0.001)

        // Per-account balances reflect transfer deduction from source and addition to destination
        val accountBalances = PersianDateAndUnitUtils.aggregateAccountBalances(
            entries = entries,
            accounts = listOf("کارت اصلی", "پس‌انداز"),
            defaultCurrency = "TOMAN"
        )
        assertEquals(5_500_000.0, accountBalances["کارت اصلی"] ?: 0.0, 0.001)
        assertEquals(3_000_000.0, accountBalances["پس‌انداز"] ?: 0.0, 0.001)
    }
}
