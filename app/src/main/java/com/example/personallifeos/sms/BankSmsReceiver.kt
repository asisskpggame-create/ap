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
import com.example.personallifeos.data.local.SmsProcessingHistoryEntity
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

                    val parsed = BankSmsProcessingEngine.parse(body, sender)
                    if (parsed.isOtpOrServiceMessage || parsed.isFailedTransaction || parsed.amountToman <= 0) {
                        continue
                    }

                    // STRICT AUDIT REQUIREMENT 1: Verification status must be AUTO_ACCEPTED.
                    // NEEDS_REVIEW or UNKNOWN direction must be saved to history queue and never auto-inserted into ledger.
                    if (parsed.verificationStatus != SmsVerificationStatus.AUTO_ACCEPTED || parsed.direction == SmsTransactionDirection.UNKNOWN) {
                        Log.w(TAG, "Incoming SMS routed to review queue (status: ${parsed.verificationStatus}): ${parsed.reviewReasonFa}")
                        dao.insertProcessingHistory(
                            SmsProcessingHistoryEntity(
                                id = UUID.randomUUID().toString(),
                                timestampMillis = System.currentTimeMillis(),
                                receivedTimeFa = PersianUtils.getPersianDateString(),
                                senderAddress = sender,
                                senderSource = SmsSenderSource.LIVE_NOTIFICATION.name,
                                detectedBank = parsed.bank.name,
                                bankFaName = parsed.bank.faName,
                                transactionDirection = parsed.direction.name,
                                rawExtractedAmount = parsed.rawExtractedAmount,
                                finalAmountToman = parsed.amountToman,
                                detectedCurrency = parsed.detectedCurrency.name,
                                templateId = parsed.templateId,
                                templateVersion = parsed.templateVersion,
                                verificationStatus = parsed.verificationStatus.name,
                                reviewReasonFa = parsed.reviewReasonFa ?: "جهت نامشخص یا نیازمند بررسی کاربر",
                                processingResult = SmsProcessingResult.NEEDS_REVIEW.name,
                                errorDetails = null,
                                idempotencyKey = parsed.idempotencyKey,
                                linkedExpenseId = null,
                                processingChannel = SmsProcessingChannel.LIVE_RECEIVER.name,
                                maskedCardOrAccount = parsed.accountOrCard?.let { PersianUtils.maskCardNumber(it) },
                                trackingCode = parsed.trackingCode
                            )
                        )
                        continue
                    }

                    // Route through the Central Registration Service
                    val repository = com.example.personallifeos.data.repository.LifeOsRepository(dao)
                    val result = repository.registerSmsTransaction(
                        parsed = parsed,
                        channel = SmsProcessingChannel.LIVE_RECEIVER,
                        senderSource = SmsSenderSource.LIVE_NOTIFICATION
                    )

                    if (result.isSuccess) {
                        Log.i(TAG, "Successfully recorded SMS transaction: ${parsed.summaryFa}")
                        showBankNotification(context, parsed)
                    } else {
                        Log.d(TAG, "SMS skipped or duplicate: ${result.errorMessageFa}")
                    }
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
