package com.example.sms

import java.security.MessageDigest

enum class BankSmsParseStatus {
    SUCCESS,
    NEEDS_REVIEW,
    IGNORED_OTP,
    IGNORED_NON_FINANCIAL,
    UNRECOGNIZED
}

enum class BankTransactionType {
    EXPENSE,
    INCOME,
    TRANSFER,
    REFUND,
    FEE,
    UNKNOWN
}

data class BankSmsParseResult(
    val status: BankSmsParseStatus,
    val bankName: String,
    val bankCode: String,
    val transactionType: BankTransactionType,
    val rawAmountDetected: Double,
    val wasInRial: Boolean,
    val amountInToman: Double,
    val balanceInToman: Double? = null,
    val feeInToman: Double? = null,
    val cardOrAccountMask: String = "",
    val trackingCode: String = "",
    val suggestedTitle: String = "",
    val suggestedCategory: String = "",
    val timestampMillis: Long = System.currentTimeMillis(),
    val confidence: Double = 0.0,
    val recognitionReasons: List<String> = emptyList(),
    val fingerprint: String = ""
) {
    companion object {
        fun computeFingerprint(
            bankCode: String,
            transactionType: BankTransactionType,
            amountToman: Double,
            cardOrAccountMask: String,
            trackingCode: String,
            approxDateOrTime: String
        ): String {
            val raw = "$bankCode|$transactionType|${amountToman.toLong()}|$cardOrAccountMask|$trackingCode|$approxDateOrTime"
            val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }.take(24)
        }
    }
}
