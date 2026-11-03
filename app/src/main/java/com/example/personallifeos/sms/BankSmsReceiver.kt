package com.example.personallifeos.sms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.personallifeos.MainActivity
import com.example.personallifeos.R
import com.example.personallifeos.data.local.AppDatabase
import com.example.personallifeos.data.local.ExpenseEntity
import com.example.personallifeos.util.PersianUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Background receiver for real-time incoming SMS from Iranian banks.
 * Parses transaction, inserts into database, and posts a rich heads-up notification.
 */
class BankSmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val dao = db.lifeOsDao()

                for (sms in messages) {
                    val sender = sms.originatingAddress ?: ""
                    val body = sms.messageBody ?: ""

                    val parsed = IranianBankSmsEngine.parse(body, sender)
                    if (parsed.isOtpOrServiceMessage || parsed.isFailedTransaction || parsed.amountToman <= 0) {
                        continue
                    }

                    // STRICT AUDIT REQUIREMENT 1: Verification status must be AUTO_ACCEPTED.
                    // NEEDS_REVIEW, REJECTED, or DUPLICATE must NEVER be auto-registered into the ledger.
                    if (parsed.verificationStatus != SmsVerificationStatus.AUTO_ACCEPTED) {
                        Log.w(TAG, "Incoming SMS skipped auto-registration (status: ${parsed.verificationStatus}): ${parsed.reviewReasonFa}")
                        continue
                    }

                    // STRICT AUDIT REQUIREMENT 1: UNKNOWN direction must NEVER be registered as an expense!
                    if (parsed.direction == SmsTransactionDirection.UNKNOWN) {
                        Log.w(TAG, "Incoming SMS skipped: direction is UNKNOWN and cannot be auto-registered")
                        continue
                    }

                    val idempotencyKey = parsed.idempotencyKey
                    // STRICT AUDIT REQUIREMENT 3: Persistent database check in addition to memory cache
                    val existingInDb = dao.getExpenseByIdempotencyKey(idempotencyKey)
                    if (existingInDb != null || IranianBankSmsEngine.isDuplicateKey(idempotencyKey)) {
                        Log.d(TAG, "Duplicate bank SMS ignored: $idempotencyKey")
                        continue
                    }

                    IranianBankSmsEngine.markIdempotencyKeyProcessed(idempotencyKey)

                    // STRICT AUDIT REQUIREMENT 1 & 6: Explicit mapping of direction to transaction type
                    val (type, title) = when (parsed.direction) {
                        SmsTransactionDirection.DEPOSIT -> {
                            "INCOME" to "واریز به حساب ${parsed.bank.faName}"
                        }
                        SmsTransactionDirection.TRANSFER_CARD,
                        SmsTransactionDirection.TRANSFER_PAYA,
                        SmsTransactionDirection.TRANSFER_POL -> {
                            "TRANSFER" to (parsed.destination?.let { "انتقال به $it (${parsed.bank.faName})" } ?: "انتقال از حساب ${parsed.bank.faName}")
                        }
                        SmsTransactionDirection.PURCHASE -> {
                            "EXPENSE" to (parsed.merchantOrTerminal?.let { "خرید از $it (${parsed.bank.faName})" } ?: "خرید با کارت ${parsed.bank.faName}")
                        }
                        SmsTransactionDirection.WITHDRAWAL -> {
                            "EXPENSE" to "برداشت از حساب ${parsed.bank.faName}"
                        }
                        SmsTransactionDirection.UNKNOWN -> {
                            // Defensive guard: never auto-register UNKNOWN
                            continue
                        }
                    }

                    val entity = ExpenseEntity(
                        id = UUID.randomUUID().toString(),
                        title = title,
                        amount = parsed.amountToman,
                        category = parsed.suggestedCategory,
                        date = PersianUtils.getPersianDateString(),
                        isSmsParsed = true,
                        bankName = parsed.bank.faName,
                        type = type,
                        accountName = parsed.bank.faName,
                        idempotencyKey = idempotencyKey
                    )

                    val insertedRowId = dao.insertExpense(entity)
                    if (insertedRowId == -1L) {
                        Log.w(TAG, "Duplicate bank SMS ignored by Room unique constraint: $idempotencyKey")
                        continue
                    }

                    IranianBankSmsEngine.markIdempotencyKeyProcessed(idempotencyKey)
                    Log.i(TAG, "Successfully recorded SMS transaction: ${parsed.summaryFa}")

                    showBankNotification(context, parsed)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing incoming SMS in background: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showBankNotification(context: Context, parsed: BankSmsParsedResult) {
        val channelId = "bank_sms_transactions_channel"
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "تراکنش‌های بانکی خودکار",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "ثبت و اعلان بی‌درنگ تراکنش‌های پیامک بانکی"
                enableVibration(true)
            }
            nm.createNotificationChannel(channel)
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAVIGATE_TAB", 2)
        }
        val pi = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedAmount = PersianUtils.formatToman(parsed.amountToman)
        val directionLabel = if (parsed.direction == SmsTransactionDirection.DEPOSIT) "واریز شد" else "برداشت شد"
        val titleText = "تراکنش بانکی ثبت شد (${parsed.bank.faName})"
        val bodyText = "$directionLabel: $formattedAmount تومان • دسته: ${parsed.suggestedCategory}"

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(titleText)
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("${parsed.summaryFa}\nاین تراکنش به‌صورت خودکار در بخش هزینه‌ها ثبت شد."))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        nm.notify(parsed.idempotencyKey.hashCode(), notification)
    }

    companion object {
        private const val TAG = "BankSmsReceiver"
    }
}
