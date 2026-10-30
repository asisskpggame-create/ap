package com.example.personallifeos

import com.example.personallifeos.sms.BankType
import com.example.personallifeos.sms.DetectedCurrencyUnit
import com.example.personallifeos.sms.IranianBankSmsEngine
import com.example.personallifeos.sms.SmsTransactionDirection
import com.example.personallifeos.sms.SmsVerificationStatus
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

        // Manual confirm without resolving UNKNOWN direction must fail validation
        val validationError = IranianBankSmsEngine.validateManualConfirmReview(
            parsed = parsed,
            overrideDirection = SmsTransactionDirection.UNKNOWN,
            overrideCurrency = DetectedCurrencyUnit.RIAL
        )
        assertNotNull("Confirming UNKNOWN direction must be rejected", validationError)

        // Manual confirm with explicit WITHDRAWAL direction and RIAL currency must succeed
        val validConfirm = IranianBankSmsEngine.validateManualConfirmReview(
            parsed = parsed,
            overrideDirection = SmsTransactionDirection.WITHDRAWAL,
            overrideCurrency = DetectedCurrencyUnit.RIAL
        )
        assertNull("Confirming with explicit direction and currency must succeed", validConfirm)
    }

    @Test
    fun `regression 2 - one verified template for a bank does not activate another unverified template of the same bank`() {
        // By default in resetDefaultState(), MELLAT_WITHDRAW v1 is verified, but MELLAT_DEPOSIT v1 is NOT verified
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

        // Version 1 is verified initially
        val v1Result = IranianBankSmsEngine.parse(melliSms)
        assertEquals(SmsVerificationStatus.AUTO_ACCEPTED, v1Result.verificationStatus)

        // Bump MELLI_STD template to version 2 -> v1 verification must be invalidated
        IranianBankSmsEngine.updateTemplateVersion("MELLI_STD", 2)
        val v2Result = IranianBankSmsEngine.parse(melliSms)
        assertEquals(2, v2Result.templateVersion)
        assertFalse(v2Result.isTemplateVerified)
        assertEquals(SmsVerificationStatus.NEEDS_REVIEW, v2Result.verificationStatus)

        // Explicitly verifying v2 restores AUTO_ACCEPTED
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

        // Missing sender when requireSenderMatch == true -> NEEDS_REVIEW
        val missingSenderResult = IranianBankSmsEngine.parse(melliSms, senderAddress = null)
        assertFalse(missingSenderResult.isSenderMatched)
        assertEquals(SmsVerificationStatus.NEEDS_REVIEW, missingSenderResult.verificationStatus)

        // Mismatched sender -> NEEDS_REVIEW
        val wrongSenderResult = IranianBankSmsEngine.parse(melliSms, senderAddress = "+989121234567")
        assertFalse(wrongSenderResult.isSenderMatched)
        assertEquals(SmsVerificationStatus.NEEDS_REVIEW, wrongSenderResult.verificationStatus)

        // Matching sender -> AUTO_ACCEPTED
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

        // Mark idempotency key as recorded
        IranianBankSmsEngine.markIdempotencyKeyProcessed(firstParse.idempotencyKey)

        // Second parse of the same SMS must be marked DUPLICATE
        val secondParse = IranianBankSmsEngine.parse(bluSms, "982000")
        assertEquals(SmsVerificationStatus.DUPLICATE, secondParse.verificationStatus)

        // Manual confirm validator must also block duplicate key
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
}
