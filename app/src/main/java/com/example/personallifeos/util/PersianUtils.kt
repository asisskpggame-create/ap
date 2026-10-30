package com.example.personallifeos.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object PersianUtils {

    private val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    fun formatPersianNumber(num: Long): String {
        return formatPersianNumber(num.toString())
    }

    fun formatPersianNumber(text: String): String {
        val sb = StringBuilder()
        for (ch in text) {
            if (ch in '0'..'9') {
                sb.append(persianDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun formatToman(amount: Long): String {
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
        }
        val formatter = DecimalFormat("#,###", symbols)
        val formatted = formatter.format(amount)
        return "${formatPersianNumber(formatted)} تومان"
    }

    fun normalizePersianText(input: String?): String {
        if (input.isNullOrEmpty()) return ""
        return input
            .replace('ك', 'ک')
            .replace('ي', 'ی')
            .replace('۰', '0')
            .replace('۱', '1')
            .replace('۲', '2')
            .replace('۳', '3')
            .replace('۴', '4')
            .replace('۵', '5')
            .replace('۶', '6')
            .replace('۷', '7')
            .replace('۸', '8')
            .replace('۹', '9')
            .trim()
            .lowercase()
    }

    data class ParsedSmsResult(
        val amount: Long,           // in Tomans
        val rawAmount: Long,        // in original currency (typically Rials)
        val merchant: String,
        val bankName: String,
        val type: String,           // "withdrawal" or "deposit"
        val balance: Long? = null,  // in Tomans
        val trackingCode: String? = null,
        val raw: String
    )

    fun parseBankSms(smsText: String): ParsedSmsResult? {
        if (smsText.isBlank()) return null
        val normalized = normalizePersianText(smsText)

        // 1. Detect Bank / Financial Entity
        val bankName = when {
            normalized.contains("بلو") || normalized.contains("blubank") -> "بلوبانک"
            normalized.contains("ویپاد") || normalized.contains("wepod") -> "ویپاد (پاسارگاد)"
            normalized.contains("پل") || normalized.contains("انتقال پل") -> "سامانه پل (پرداخت لحظه‌ای)"
            normalized.contains("ملی") || normalized.contains("سداد") -> "بانک ملی ایران"
            normalized.contains("ملت") || normalized.contains("بهسازان") -> "بانک ملت"
            normalized.contains("صادرات") || normalized.contains("سپهر") -> "بانک صادرات ایران"
            normalized.contains("تجارت") -> "بانک تجارت"
            normalized.contains("کوثر") -> "بانک سپه (کوثر سابق)"
            normalized.contains("نور") -> "بانک ملی (نور سابق)"
            normalized.contains("سپه") -> "بانک سپه"
            normalized.contains("مسکن") || normalized.contains("ناواکو") -> "بانک مسکن"
            normalized.contains("کشاورزی") || normalized.contains("مهرگستر") -> "بانک کشاورزی"
            normalized.contains("توسعه تعاون") -> "بانک توسعه تعاون"
            normalized.contains("صنعت و معدن") -> "بانک صنعت و معدن"
            normalized.contains("توسعه صادرات") -> "بانک توسعه صادرات"
            normalized.contains("پست بانک") -> "پست بانک ایران"
            normalized.contains("رفاه") -> "بانک رفاه کارگران"
            normalized.contains("مهر ایران") -> "بانک مهر ایران"
            normalized.contains("رسالت") -> "بانک قرض‌الحسنه رسالت"
            normalized.contains("پاسارگاد") || normalized.contains("pasargad") -> "بانک پاسارگاد"
            normalized.contains("سامان") -> "بانک سامان"
            normalized.contains("پارسیان") || normalized.contains("لوتوس") -> "بانک پارسیان"
            normalized.contains("آینده") || normalized.contains("ayandeh") -> "بانک آینده"
            normalized.contains("شهر") || normalized.contains("shahr") -> "بانک شهر"
            normalized.contains("گردشگری") -> "بانک گردشگری"
            normalized.contains("اقتصاد نوین") -> "بانک اقتصاد نوین"
            normalized.contains("سینا") -> "بانک سینا"
            normalized.contains("دی") && !normalized.contains("خرید") -> "بانک دی"
            normalized.contains("کارآفرین") -> "بانک کارآفرین"
            normalized.contains("ایران زمین") -> "بانک ایران زمین"
            normalized.contains("سرمایه") -> "بانک سرمایه"
            normalized.contains("خاورمیانه") -> "بانک خاورمیانه"
            normalized.contains("ملل") || normalized.contains("عسکریه") -> "موسسه اعتباری ملل"
            else -> "بانک شتابی"
        }

        // 2. Detect Transaction Direction (Deposit vs Withdrawal)
        // Deposit anchors: واریز, افزایش, بستانکار, نشست, +
        // Withdrawal anchors: برداشت, خرید, کسر, بدهکار, پرید, -
        val isDeposit = when {
            normalized.contains("واریز") || normalized.contains("افزایش") ||
            normalized.contains("بستانکار") || normalized.contains("نشست") ||
            Regex("""[\d,]+\s*\+""").containsMatchIn(normalized) -> true

            normalized.contains("برداشت") || normalized.contains("خرید") ||
            normalized.contains("کسر") || normalized.contains("بدهکار") ||
            normalized.contains("پرید") || Regex("""[\d,]+\s*-""").containsMatchIn(normalized) -> false

            else -> false
        }
        val type = if (isDeposit) "deposit" else "withdrawal"

        // 3. Extract Amount
        // Regex patterns for amounts:
        // a) Standard: 1,500,000 ریال / تومان
        // b) Refah format: برداشت1,960,000R or 1,960,000r
        // c) Algebraic compressed: 1,698,500+ or 1,698,500-
        // d) BluBank format: 900,000 ریال از حساب شما پرید
        // e) Explicit labels: مبلغ: ۱,۵۰۰,۰۰۰ or برداشت: ۱,۵۰۰,۰۰۰
        var rawAmount: Long? = null
        var isTomanUnit = false

        // Check algebraic (e.g. 1,698,500+ or 1,698,500-)
        val algebraicMatch = Regex("""([\d,]{4,})\s*([+-])""").find(normalized)
        if (algebraicMatch != null) {
            rawAmount = algebraicMatch.groupValues[1].replace(",", "").toLongOrNull()
        }

        // Check Refah format with R (e.g. برداشت1,960,000R or بمانده)
        if (rawAmount == null) {
            val refahMatch = Regex("""(برداشت|واریز|مبلغ)\s*([\d,]{4,})\s*[rR]""").find(normalized)
            if (refahMatch != null) {
                rawAmount = refahMatch.groupValues[2].replace(",", "").toLongOrNull()
            }
        }

        // Check with currency suffix (ریال / تومان)
        if (rawAmount == null) {
            val currencyMatch = Regex("""([\d,]{4,})\s*(ریال|تومان)""").find(normalized)
            if (currencyMatch != null) {
                rawAmount = currencyMatch.groupValues[1].replace(",", "").toLongOrNull()
                isTomanUnit = currencyMatch.groupValues[2] == "تومان"
            }
        }

        // Check with prefix label (مبلغ: ..., برداشت: ..., واریز: ...)
        if (rawAmount == null) {
            val labelMatch = Regex("""(مبلغ|برداشت|واریز|خرید)[\s:]*([\d,]{4,})""").find(normalized)
            if (labelMatch != null) {
                rawAmount = labelMatch.groupValues[2].replace(",", "").toLongOrNull()
            }
        }

        // Fallback: any 5+ digit number with commas
        if (rawAmount == null) {
            val genericMatch = Regex("""([\d,]{5,})""").find(normalized)
            if (genericMatch != null) {
                rawAmount = genericMatch.groupValues[1].replace(",", "").toLongOrNull()
            }
        }

        if (rawAmount == null || rawAmount <= 0) return null

        // In Iran banking SMS, amounts are normally in Rials unless explicitly "تومان"
        val amountInTomans = if (isTomanUnit) rawAmount else rawAmount / 10

        // 4. Extract Final Balance (مانده / موجودی / بمانده)
        var balanceInTomans: Long? = null
        val balanceMatch = Regex("""(مانده|موجودی|بمانده|مانده حساب)[\s:]*([\d,]{4,})""").find(normalized)
        if (balanceMatch != null) {
            val rawBal = balanceMatch.groupValues[2].replace(",", "").toLongOrNull()
            if (rawBal != null) {
                balanceInTomans = if (normalized.contains("تومان")) rawBal else rawBal / 10
            }
        }

        // 5. Extract Tracking Code (پیگیری / ارجاع / ردیف / سند / کد رهگیری)
        val trackingMatch = Regex("""(پیگیری|کد ارجاع|شماره پیگیری|ردیف|سند|کد رهگیری|کد مرجع|شماره ارجاع|ارجاع)[\s:]*([a-zA-Z0-9]{4,})""").find(normalized)
        val trackingCode = trackingMatch?.groupValues?.get(2)

        // 6. Extract Merchant / Transaction Type Label
        val merchant = when {
            normalized.contains("پل") || normalized.contains("انتقال پل") -> "انتقال وجه سامانه پل"
            normalized.contains("پرید") -> "برداشت از حساب بلوبانک"
            normalized.contains("خرید") -> "خرید فروشگاهی / اینترنتی"
            normalized.contains("کارت به کارت") || normalized.contains("انتقال به کارت") -> "انتقال وجه کارت به کارت"
            normalized.contains("انتقال") -> "انتقال وجه بانکی"
            normalized.contains("پایانه") || normalized.contains("pos") -> "خرید پایانه فروشگاهی (POS)"
            normalized.contains("خودپرداز") || normalized.contains("atm") -> "برداشت وجه خودپرداز"
            normalized.contains("حقوق") || normalized.contains("واریز") -> "واریز وجه به حساب"
            else -> if (isDeposit) "واریز بانکی" else "برداشت / خرید کارت"
        }

        return ParsedSmsResult(
            amount = amountInTomans,
            rawAmount = rawAmount,
            merchant = merchant,
            bankName = bankName,
            type = type,
            balance = balanceInTomans,
            trackingCode = trackingCode,
            raw = smsText
        )
    }

    fun getPersianDateString(): String {
        return "۱۴۰۵/۰۸/۰۵ (۰۵ آبان)"
    }
}
