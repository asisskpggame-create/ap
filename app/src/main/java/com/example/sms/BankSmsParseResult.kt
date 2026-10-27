package com.example.sms

/**
 * Status of parsing an incoming or pasted SMS message.
 */
enum class BankSmsParseStatus {
    /** Valid financial transaction with high-confidence extraction. */
    SUCCESS,
    /** Looks like a financial SMS, but requires user review/confirmation before saving (e.g. ambiguous currency or amount). */
    NEEDS_REVIEW,
    /** Ignored because it is an OTP / Dynamic Password (رمز پویا / کد تایید). */
    IGNORED_OTP,
    /** Ignored because it is a failed transaction, login alert, balance-only inquiry, or promotional SMS. */
    IGNORED_NON_FINANCIAL,
    /** Unrecognized format or insufficient financial data. */
    UNRECOGNIZED
}

/**
 * Canonical transaction direction extracted from bank SMS.
 */
enum class BankTransactionType(val code: String) {
    EXPENSE("EXPENSE"),
    INCOME("INCOME"),
    TRANSFER("TRANSFER"),
    UNKNOWN("EXPENSE")
}

/**
 * Structured, auditable output of the Iranian Bank SMS Parser.
 */
data class BankSmsParseResult(
    val status: BankSmsParseStatus,
    val bankId: String = "UNKNOWN",
    val bankName: String = "پیامک بانکی",
    val transactionType: BankTransactionType = BankTransactionType.UNKNOWN,
    val amountInToman: Double = 0.0,
    val rawAmountDetected: Double = 0.0,
    val wasInRial: Boolean = true,
    val detectedCurrency: String = "IRR", // "IRR", "TOMAN", or "UNKNOWN" when missing from SMS
    val balanceInToman: Double? = null,
    val feeInToman: Double? = null,
    val cardOrAccountSuffix: String = "",
    val referenceNumber: String? = null,
    val dateTimeText: String? = null,
    val suggestedTitle: String = "",
    val suggestedCategory: String = "سایر",
    val confidence: Float = 0f,
    val matchedRuleId: String = "",
    val fingerprint: String = "",
    val normalizedText: String = "",
    val uncertainFields: List<String> = emptyList(),
    val statusMessageFa: String = ""
) {
    val confidenceScore: Float
        get() = confidence

    val isValidTransaction: Boolean
        get() = (status == BankSmsParseStatus.SUCCESS || status == BankSmsParseStatus.NEEDS_REVIEW) && amountInToman > 0.0
}
