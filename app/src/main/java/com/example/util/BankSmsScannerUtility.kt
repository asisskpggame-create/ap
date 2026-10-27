package com.example.util

import android.content.Context
import android.database.Cursor
import android.net.Uri
import com.example.sms.BankSmsNormalizer
import com.example.sms.BankSmsParseStatus
import com.example.sms.BankSmsParser

/**
 * Represents a parsed Iranian bank SMS ready for 1-tap confirmation into [com.example.data.model.ExpenseEntry].
 * All parsing is delegated to the canonical [com.example.sms.BankSmsParser] engine (single source of truth).
 */
data class ParsedBankSmsResult(
    val idempotencyKey: String,
    val senderAddress: String,
    val bankName: String,
    val rawBody: String,
    val timestampMillis: Long,
    val localEpochDay: Long,
    val transactionType: String, // "EXPENSE", "INCOME", or "TRANSFER"
    val rawAmountNumber: Double,
    val amountInToman: Double,
    val wasInRial: Boolean,
    val balanceInToman: Double? = null,
    val feeInToman: Double? = null,
    val cardOrAccountSuffix: String = "",
    val parseStatus: BankSmsParseStatus = BankSmsParseStatus.SUCCESS,
    val statusMessageFa: String = "",
    val suggestedTitle: String,
    val suggestedCategory: String
)

/**
 * On-Device Bank SMS Inbox Scanner Utility delegating to [BankSmsParser] as the single source of truth.
 */
object BankSmsScannerUtility {

    fun looksLikeFinancialSms(body: String, sender: String = ""): Boolean {
        if (body.isBlank()) return false
        val res = BankSmsParser.parse(body, sender)
        return res.status == BankSmsParseStatus.SUCCESS || res.status == BankSmsParseStatus.NEEDS_REVIEW
    }

    /**
     * Parses a raw Persian/English bank SMS string into a structured [ParsedBankSmsResult]
     * using the canonical [BankSmsParser] engine.
     * Returns null if no valid financial transaction is detected.
     */
    fun parseBankSms(
        rawBody: String,
        sender: String = "",
        timestampMillis: Long = System.currentTimeMillis()
    ): ParsedBankSmsResult? {
        val res = BankSmsParser.parse(rawBody, sender)
        if (!res.isValidTransaction) return null

        val localDay = PersianDateAndUnitUtils.currentLocalEpochDay(timestampMillis)
        val idemKey = res.fingerprint.ifBlank { BankSmsNormalizer.computeFingerprint(rawBody, sender) }

        return ParsedBankSmsResult(
            idempotencyKey = idemKey,
            senderAddress = sender,
            bankName = res.bankName,
            rawBody = rawBody.trim(),
            timestampMillis = timestampMillis,
            localEpochDay = localDay,
            transactionType = res.transactionType.code,
            rawAmountNumber = res.rawAmountDetected,
            amountInToman = res.amountInToman,
            wasInRial = res.wasInRial,
            balanceInToman = res.balanceInToman,
            feeInToman = res.feeInToman,
            cardOrAccountSuffix = res.cardOrAccountSuffix,
            parseStatus = res.status,
            statusMessageFa = res.statusMessageFa,
            suggestedTitle = res.suggestedTitle,
            suggestedCategory = res.suggestedCategory
        )
    }

    /**
     * Reads recent SMS messages from `content://sms/inbox` on-device when `READ_SMS` is granted.
     * Returns only parsed financial transactions sorted newest-first, deduplicated by idempotencyKey.
     */
    fun scanDeviceSmsInbox(
        context: Context,
        maxMessagesToInspect: Int = 60,
        existingIdempotencyKeys: Set<String> = emptySet()
    ): List<ParsedBankSmsResult> {
        val results = mutableListOf<ParsedBankSmsResult>()
        val seenKeys = mutableSetOf<String>()
        val inboxUri = Uri.parse("content://sms/inbox")
        val projection = arrayOf("_id", "address", "body", "date")

        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                inboxUri,
                projection,
                null,
                null,
                "date DESC"
            )
            if (cursor != null) {
                val addressIdx = cursor.getColumnIndex("address")
                val bodyIdx = cursor.getColumnIndex("body")
                val dateIdx = cursor.getColumnIndex("date")
                var inspected = 0

                while (cursor.moveToNext() && inspected < maxMessagesToInspect) {
                    inspected++
                    val sender = if (addressIdx >= 0) cursor.getString(addressIdx).orEmpty() else ""
                    val body = if (bodyIdx >= 0) cursor.getString(bodyIdx).orEmpty() else ""
                    val dateMillis = if (dateIdx >= 0) cursor.getLong(dateIdx) else System.currentTimeMillis()

                    val parsed = parseBankSms(
                        rawBody = body,
                        sender = sender,
                        timestampMillis = dateMillis
                    )
                    if (parsed != null &&
                        parsed.idempotencyKey !in existingIdempotencyKeys &&
                        seenKeys.add(parsed.idempotencyKey)
                    ) {
                        results.add(parsed)
                    }
                }
            }
        } catch (_: SecurityException) {
            // Permission not granted; return empty list safely
        } catch (_: Exception) {
            // Device without telephony provider
        } finally {
            cursor?.close()
        }
        return results
    }
}
