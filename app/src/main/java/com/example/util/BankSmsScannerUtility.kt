package com.example.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.example.data.model.ExpenseEntry
import kotlin.math.abs

data class BankSmsSuggestion(
    val id: String,
    val senderAddress: String,
    val bankName: String,
    val transactionType: String, // "EXPENSE" or "INCOME"
    val rawAmountDetected: Double,
    val wasInRial: Boolean,
    val amountInToman: Double,
    val balanceInToman: Double?,
    val cardNumber: String,
    val suggestedTitle: String,
    val suggestedCategory: String,
    val rawBody: String,
    val timestampMillis: Long,
    val epochDay: Long,
    val dateDisplay: String
)

object BankSmsScannerUtility {

    private val knownBanks = listOf(
        "بانک ملت" to "بانک ملت",
        "ملت" to "بانک ملت",
        "mellat" to "بانک ملت",
        "بانک ملی" to "بانک ملی",
        "ملی ایران" to "بانک ملی",
        "بلوبانک" to "بلوبانک سامان",
        "بلو" to "بلوبانک سامان",
        "blubank" to "بلوبانک سامان",
        "بانک سامان" to "بانک سامان",
        "saman" to "بانک سامان",
        "پاسارگاد" to "بانک پاسارگاد",
        "pasargad" to "بانک پاسارگاد",
        "تجارت" to "بانک تجارت",
        "tejarat" to "بانک تجارت",
        "صادرات" to "بانک صادرات",
        "saderat" to "بانک صادرات",
        "پارسیان" to "بانک پارسیان",
        "parsian" to "بانک پارسیان",
        "رسالت" to "بانک رسالت",
        "سپه" to "بانک سپه",
        "کشاورزی" to "بانک کشاورزی",
        "مسکن" to "بانک مسکن",
        "شهر" to "بانک شهر",
        "آینده" to "بانک آینده",
        "رفاه" to "بانک رفاه",
        "گردشگری" to "بانک گردشگری",
        "سینا" to "بانک سینا",
        "کارآفرین" to "بانک کارآفرین",
        "ویپاد" to "ویپاد پاسارگاد"
    )

    private val otpExclusionKeywords = listOf(
        "رمز پویا",
        "رمز دوم",
        "کد تایید",
        "کد ورود",
        "کد فعالسازی",
        "کد امنیتی",
        "otp",
        "verification code"
    )

    private val transactionKeywords = listOf(
        "برداشت",
        "واریز",
        "خرید",
        "مبلغ",
        "مانده",
        "موجودی",
        "کارت به کارت",
        "انتقال وجه",
        "پایا",
        "ساتنا",
        "سود",
        "حساب",
        "تراکنش"
    )

    fun hasReadSmsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isLikelyBankTransactionSms(sender: String, body: String): Boolean {
        val normalizedBody = PersianDateAndUnitUtils.toEnglishDigits(body).lowercase()
        val normalizedSender = sender.lowercase()

        // Exclude OTP-only messages that do not have account balance or explicit withdrawal/deposit confirmation
        val hasOtpKeyword = otpExclusionKeywords.any { normalizedBody.contains(it) }
        val hasBalanceIndicator = normalizedBody.contains("مانده") || normalizedBody.contains("موجودی")
        if (hasOtpKeyword && !hasBalanceIndicator) {
            return false
        }

        val hasTxKeyword = transactionKeywords.any { normalizedBody.contains(it) }
        val hasCurrencyOrBank = normalizedBody.contains("ریال") ||
            normalizedBody.contains("تومان") ||
            normalizedBody.contains("بانک") ||
            normalizedBody.contains("حساب") ||
            normalizedBody.contains("کارت") ||
            normalizedSender.contains("bank") ||
            normalizedSender.startsWith("981000") ||
            normalizedSender.startsWith("982000") ||
            normalizedSender.startsWith("983000") ||
            normalizedSender.startsWith("+981000") ||
            normalizedSender.startsWith("+982000") ||
            normalizedSender.startsWith("+983000") ||
            knownBanks.any { (kw, _) -> normalizedBody.contains(kw) || normalizedSender.contains(kw) }

        return hasTxKeyword && hasCurrencyOrBank
    }

    fun parseBankSms(
        rawSms: String,
        id: String = "manual_${rawSms.hashCode()}",
        senderAddress: String = "",
        timestampMillis: Long = System.currentTimeMillis()
    ): BankSmsSuggestion? {
        val normalized = PersianDateAndUnitUtils.toEnglishDigits(rawSms).trim()
        if (normalized.isEmpty()) return null

        val lowerNormalized = normalized.lowercase()
        val lowerSender = senderAddress.lowercase()

        val detectedBank = knownBanks.firstOrNull { (kw, _) ->
            lowerNormalized.contains(kw) || lowerSender.contains(kw)
        }?.second ?: "بانک عضو شتاب"

        // Determine transaction type (INCOME vs EXPENSE)
        val hasDebitKeyword = lowerNormalized.contains("برداشت") ||
            lowerNormalized.contains("خرید") ||
            lowerNormalized.contains("کاهش") ||
            lowerNormalized.contains("پرداخت") ||
            lowerNormalized.contains("بدهکار") ||
            lowerNormalized.contains("انتقال از")

        val hasCreditKeyword = lowerNormalized.contains("واریز") ||
            lowerNormalized.contains("افزایش") ||
            lowerNormalized.contains("بستانکار") ||
            lowerNormalized.contains("سود") ||
            lowerNormalized.contains("برگشت") ||
            lowerNormalized.contains("انتقال به")

        val isIncome = when {
            hasCreditKeyword && !hasDebitKeyword -> true
            hasDebitKeyword && !hasCreditKeyword -> false
            lowerNormalized.contains("+") && !lowerNormalized.contains("-") -> true
            else -> false
        }
        val txType = if (isIncome) "INCOME" else "EXPENSE"

        // Currency detection (95% of Iranian bank SMS are in Rial unless explicitly Toman)
        val explicitlyToman = lowerNormalized.contains("تومان") || lowerNormalized.contains("تومن")
        val wasInRial = !explicitlyToman

        // Clean lines & extract transaction amount vs remaining balance
        val lines = normalized.lines().map { it.trim() }.filter { it.isNotEmpty() }
        var txAmountRaw: Double? = null
        var balanceRaw: Double? = null

        // Regex matching formatted currency amounts (e.g. 2,450,000 or 850000)
        val amountNumberRegex = Regex("""(?<![0-9/:-])([0-9]{1,3}(?:[,،][0-9]{3})+|[0-9]{4,12})(?![0-9/:-])""")

        for (line in lines) {
            val lowerLine = line.lowercase()
            // Skip lines that are purely reference/tracking codes or OTPs
            if (lowerLine.contains("پیگیری") || lowerLine.contains("مرجع") ||
                lowerLine.contains("رهگیری") || lowerLine.contains("رمز")
            ) {
                continue
            }

            val isBalanceLine = lowerLine.contains("مانده") || lowerLine.contains("موجودی")
            val isAmountLine = lowerLine.contains("مبلغ") ||
                lowerLine.contains("برداشت") ||
                lowerLine.contains("واریز") ||
                lowerLine.contains("خرید") ||
                lowerLine.contains("ریال") ||
                lowerLine.contains("تومان")

            // Remove card masks like 6104****1234 or dates like 1403/07/12 before scanning numbers
            val sanitizedLine = line
                .replace(Regex("""\d{4}[*×xX]+\d{4}"""), " ")
                .replace(Regex("""\d{2,4}/\d{1,2}/\d{1,2}"""), " ")
                .replace(Regex("""\d{1,2}:\d{2}(:\d{2})?"""), " ")

            val matches = amountNumberRegex.findAll(sanitizedLine)
                .mapNotNull { it.groupValues[1].replace(",", "").replace("،", "").toDoubleOrNull() }
                .filter { it >= 1000.0 }
                .toList()

            if (matches.isNotEmpty()) {
                if (isBalanceLine && !isAmountLine) {
                    if (balanceRaw == null) balanceRaw = matches.last()
                } else if (isBalanceLine && isAmountLine && matches.size >= 2) {
                    if (txAmountRaw == null) txAmountRaw = matches.first()
                    if (balanceRaw == null) balanceRaw = matches.last()
                } else if (isAmountLine || txAmountRaw == null) {
                    if (txAmountRaw == null) txAmountRaw = matches.first()
                }
            }
        }

        // Fallback if single-line SMS had both numbers
        if (txAmountRaw == null) {
            val sanitizedAll = normalized
                .replace(Regex("""\d{4}[*×xX]+\d{4}"""), " ")
                .replace(Regex("""\d{2,4}/\d{1,2}/\d{1,2}"""), " ")
                .replace(Regex("""\d{1,2}:\d{2}(:\d{2})?"""), " ")

            val allNums = amountNumberRegex.findAll(sanitizedAll)
                .mapNotNull { it.groupValues[1].replace(",", "").replace("،", "").toDoubleOrNull() }
                .filter { it >= 1000.0 }
                .toList()
            if (allNums.isNotEmpty()) {
                txAmountRaw = allNums.first()
                if (allNums.size >= 2 && balanceRaw == null) {
                    balanceRaw = allNums.last()
                }
            }
        }

        val finalRaw = txAmountRaw ?: return null
        val amountToman = if (wasInRial) (finalRaw / 10.0) else finalRaw
        if (amountToman <= 0.0) return null
        val balanceToman = balanceRaw?.let { if (wasInRial) it / 10.0 else it }

        // Extract 4-digit card or account suffix if available
        val cardRegex = Regex("""(?:\*+|کارت\s*|حساب\s*)(\d{4})\b""")
        val cardMatch = cardRegex.find(normalized)
        val cardNum = if (cardMatch != null) {
            "کارت ${PersianDateAndUnitUtils.toPersianDigits(cardMatch.groupValues[1])}"
        } else {
            ""
        }

        // Infer Persian category & smart merchant title aligned with the app's categories
        val (categoryFa, suggestedTitle) = inferCategoryAndTitle(lowerNormalized, detectedBank, isIncome)

        val epochDay = timestampMillis / 86_400_000L
        val dateDisplay = PersianDateAndUnitUtils.formatEpochDay(epochDay, "JALALI")

        return BankSmsSuggestion(
            id = id,
            senderAddress = senderAddress,
            bankName = detectedBank,
            transactionType = txType,
            rawAmountDetected = finalRaw,
            wasInRial = wasInRial,
            amountInToman = amountToman,
            balanceInToman = balanceToman,
            cardNumber = cardNum,
            suggestedTitle = suggestedTitle,
            suggestedCategory = categoryFa,
            rawBody = rawSms.trim(),
            timestampMillis = timestampMillis,
            epochDay = epochDay,
            dateDisplay = dateDisplay
        )
    }

    private fun inferCategoryAndTitle(
        lowerNormalized: String,
        detectedBank: String,
        isIncome: Boolean
    ): Pair<String, String> {
        return when {
            lowerNormalized.contains("اسنپ") || lowerNormalized.contains("تپسی") ||
                lowerNormalized.contains("مترو") || lowerNormalized.contains("پمپ بنزین") ||
                lowerNormalized.contains("سوخت") || lowerNormalized.contains("تاکسی") ->
                "حمل‌ونقل" to when {
                    lowerNormalized.contains("اسنپ") -> "اسنپ ($detectedBank)"
                    lowerNormalized.contains("تپسی") -> "تپسی ($detectedBank)"
                    lowerNormalized.contains("بنزین") || lowerNormalized.contains("سوخت") -> "سوخت و بنزین ($detectedBank)"
                    else -> "کرایه و حمل‌ونقل ($detectedBank)"
                }

            lowerNormalized.contains("رستوران") || lowerNormalized.contains("کافه") ||
                lowerNormalized.contains("نانوایی") || lowerNormalized.contains("غذا") ||
                lowerNormalized.contains("اسنپ فود") || lowerNormalized.contains("فست فود") ->
                "غذا و کافه" to when {
                    lowerNormalized.contains("کافه") -> "کافه ($detectedBank)"
                    lowerNormalized.contains("رستوران") -> "رستوران ($detectedBank)"
                    else -> "غذا و خوراک ($detectedBank)"
                }

            lowerNormalized.contains("افق کوروش") || lowerNormalized.contains("هایپر") ||
                lowerNormalized.contains("سوپرمارکت") || lowerNormalized.contains("دیجی کالا") ||
                lowerNormalized.contains("دیجی‌کالا") || lowerNormalized.contains("فروشگاه") ||
                lowerNormalized.contains("جانبو") || lowerNormalized.contains("شهروند") ->
                "خرید" to when {
                    lowerNormalized.contains("کوروش") -> "خرید از افق کوروش ($detectedBank)"
                    lowerNormalized.contains("دیجی") -> "خرید از دیجی‌کالا ($detectedBank)"
                    lowerNormalized.contains("سوپرمارکت") -> "خرید سوپرمارکت ($detectedBank)"
                    else -> "خرید فروشگاهی ($detectedBank)"
                }

            lowerNormalized.contains("قبض") || lowerNormalized.contains("همراه اول") ||
                lowerNormalized.contains("ایرانسل") || lowerNormalized.contains("رایتل") ||
                lowerNormalized.contains("اینترنت") || lowerNormalized.contains("برق") ||
                lowerNormalized.contains("گاز") || lowerNormalized.contains("آب") ||
                lowerNormalized.contains("اجاره") ->
                "قبوض و اجاره" to "پرداخت قبض / شارژ ($detectedBank)"

            lowerNormalized.contains("داروخانه") || lowerNormalized.contains("بیمارستان") ||
                lowerNormalized.contains("پزشک") || lowerNormalized.contains("درمانگاه") ||
                lowerNormalized.contains("آزمایشگاه") || lowerNormalized.contains("دندانپزشکی") ->
                "سلامت و درمان" to "هزینه درمان و دارو ($detectedBank)"

            lowerNormalized.contains("سینما") || lowerNormalized.contains("استخر") ||
                lowerNormalized.contains("باشگاه") || lowerNormalized.contains("ورزش") ||
                lowerNormalized.contains("بلیط") ->
                "تفریح و سرگرمی" to "تفریح و ورزش ($detectedBank)"

            lowerNormalized.contains("بورس") || lowerNormalized.contains("کارگزاری") ||
                lowerNormalized.contains("صندوق") || lowerNormalized.contains("طلا") ||
                lowerNormalized.contains("سود سپرده") || lowerNormalized.contains("سود بانکی") ->
                "سرمایه‌گذاری" to if (isIncome) "سود سرمایه‌گذاری ($detectedBank)" else "سرمایه‌گذاری ($detectedBank)"

            lowerNormalized.contains("دانشگاه") || lowerNormalized.contains("کتاب") ||
                lowerNormalized.contains("آموزش") || lowerNormalized.contains("دوره") ->
                "آموزش" to "هزینه آموزشی ($detectedBank)"

            isIncome ->
                "سایر" to when {
                    lowerNormalized.contains("حقوق") -> "واریز حقوق ($detectedBank)"
                    lowerNormalized.contains("پایا") || lowerNormalized.contains("ساتنا") -> "واریز پایا/ساتنا ($detectedBank)"
                    else -> "واریز به حساب ($detectedBank)"
                }

            lowerNormalized.contains("کارت به کارت") || lowerNormalized.contains("انتقال") ||
                lowerNormalized.contains("پایا") || lowerNormalized.contains("ساتنا") ->
                "سایر" to "انتقال وجه / کارت به کارت ($detectedBank)"

            lowerNormalized.contains("خرید") ->
                "خرید" to "خرید کارتی ($detectedBank)"

            else ->
                "سایر" to "تراکنش بانکی ($detectedBank)"
        }
    }

    fun scanInboxForBankTransactions(
        context: Context,
        maxScanCount: Int = 120,
        maxResults: Int = 25
    ): List<BankSmsSuggestion> {
        if (!hasReadSmsPermission(context)) {
            return emptyList()
        }

        val results = mutableListOf<BankSmsSuggestion>()
        try {
            val cursor: Cursor? = context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI ?: Uri.parse("content://sms/inbox"),
                arrayOf("_id", "address", "body", "date"),
                null,
                null,
                "date DESC"
            )

            cursor?.use { c ->
                val idIdx = c.getColumnIndex("_id")
                val addrIdx = c.getColumnIndex("address")
                val bodyIdx = c.getColumnIndex("body")
                val dateIdx = c.getColumnIndex("date")

                var scanned = 0
                while (c.moveToNext() && scanned < maxScanCount && results.size < maxResults) {
                    scanned++
                    val id = if (idIdx >= 0) c.getString(idIdx) ?: scanned.toString() else scanned.toString()
                    val sender = if (addrIdx >= 0) c.getString(addrIdx) ?: "" else ""
                    val body = if (bodyIdx >= 0) c.getString(bodyIdx) ?: "" else ""
                    val dateMillis = if (dateIdx >= 0) c.getLong(dateIdx) else System.currentTimeMillis()

                    if (isLikelyBankTransactionSms(sender, body)) {
                        val parsed = parseBankSms(
                            rawSms = body,
                            id = id,
                            senderAddress = sender,
                            timestampMillis = dateMillis
                        )
                        if (parsed != null && parsed.amountInToman > 0) {
                            results.add(parsed)
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }

        return results
    }

    fun isAlreadyLogged(
        suggestion: BankSmsSuggestion,
        existingExpenses: List<ExpenseEntry>
    ): Boolean {
        val smsMarker = "[SMS#${suggestion.id}]"
        return existingExpenses.any { exp ->
            exp.notes.contains(smsMarker) ||
                (exp.localEpochDay == suggestion.epochDay &&
                    exp.transactionType == suggestion.transactionType &&
                    abs(exp.amount - suggestion.amountInToman) < 1.0)
        }
    }

    fun getSampleBankSmsSuggestions(todayEpochDay: Long): List<BankSmsSuggestion> {
        val nowMillis = todayEpochDay * 86_400_000L
        val sampleMessages = listOf(
            Triple(
                "sample_mellat_1",
                "98200033",
                "بانک ملت\nخرید با کارت *۶۱۰۴\nمبلغ: 3,450,000 ریال\nفروشگاه افق کوروش\nمانده: 48,500,000 ریال"
            ),
            Triple(
                "sample_blu_2",
                "BluBank",
                "بلوبانک\nبرداشت 1,250,000 ریال از حساب بابت اسنپ\nکارت *۷۲۱۸\nموجودی: 14,300,000 ریال"
            ),
            Triple(
                "sample_saman_3",
                "9820000",
                "بانک سامان\nواریز پایا به حساب\nمبلغ: 85,000,000 ریال\nبابت حقوق ماهانه\nمانده: 99,300,000 ریال"
            ),
            Triple(
                "sample_pasargad_4",
                "981000852",
                "بانک پاسارگاد\nخرید اینترنتی بسته اینترنت ایرانسل\nکارت *۵۰۲۲\nمبلغ: 980,000 ریال\nمانده: 21,400,000 ریال"
            )
        )
        return sampleMessages.mapNotNull { (id, sender, body) ->
            parseBankSms(
                rawSms = body,
                id = id,
                senderAddress = sender,
                timestampMillis = nowMillis
            )
        }
    }
}
