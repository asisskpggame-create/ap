package com.example.personallifeos.sms

import java.security.MessageDigest

object IranianBankSmsEngine {

    /**
     * Converts Persian and Arabic digits to standard ASCII digits.
     */
    fun normalizeDigits(input: String): String {
        val fa = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val ar = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        var result = input
        for (i in 0..9) {
            result = result.replace(fa[i], ('0' + i)).replace(ar[i], ('0' + i))
        }
        return result
    }

    /**
     * Checks if the message is an OTP, promotional, or non-financial system SMS.
     */
    fun isOtpOrServiceSms(rawText: String): Boolean {
        val text = rawText.lowercase()
        val otpKeywords = listOf(
            "رمز پویا", "رمز یکبار مصرف", "کد فعالسازی", "کد ورود",
            "کد تایید", "otp", "رمز اینترنتی", "رمز دوم",
            "جشنواره", "برنده شدید", "تسهیلات ویژه", "افتتاح حساب شما با موفقیت",
            "رمز ایستا", "رمز عبور"
        )
        return otpKeywords.any { text.contains(it) }
    }

    /**
     * Parses an Iranian Bank SMS text into a structured BankSmsParsedResult.
     */
    fun parse(rawSms: String): BankSmsParsedResult {
        val cleanText = normalizeDigits(rawSms).trim()

        if (isOtpOrServiceSms(cleanText)) {
            val hash = calculateHash("OTP_" + cleanText.take(50))
            return BankSmsParsedResult(
                bank = detectBank(cleanText),
                direction = SmsTransactionDirection.UNKNOWN,
                amountRial = 0L,
                amountToman = 0L,
                idempotencyKey = hash,
                isOtpOrServiceMessage = true,
                suggestedCategory = "اطلاع‌رسانی",
                summaryFa = "پیامک سیستمی یا رمز پویا (فاقد گردش مالی)"
            )
        }

        val bank = detectBank(cleanText)
        val direction = detectDirection(cleanText)
        val (amountRial, amountToman) = extractAmount(cleanText)
        val (balanceRial, balanceToman) = extractBalance(cleanText)
        val accountOrCard = extractAccountOrCard(cleanText)
        val destination = extractDestination(cleanText)
        val trackingCode = extractTrackingCode(cleanText)
        val dateTime = extractDateTime(cleanText)
        val merchant = extractMerchantOrTerminal(cleanText)
        val suggestedCategory = guessCategory(cleanText, destination, merchant, direction)

        val hashPayload = "${bank.name}_${direction.name}_${amountRial}_${trackingCode ?: dateTime ?: cleanText.take(40)}"
        val idempotencyKey = calculateHash(hashPayload)

        val summaryFa = buildString {
            append(bank.faName)
            append(" • ")
            append(direction.faName)
            append(": ")
            append(formatMoney(amountToman))
            append(" تومان")
            if (balanceToman != null) {
                append(" (مانده: ")
                append(formatMoney(balanceToman))
                append(" ت)")
            }
        }

        return BankSmsParsedResult(
            bank = bank,
            direction = direction,
            amountRial = amountRial,
            amountToman = amountToman,
            balanceRial = balanceRial,
            balanceToman = balanceToman,
            accountOrCard = accountOrCard,
            destination = destination,
            trackingCode = trackingCode,
            dateTimeString = dateTime,
            merchantOrTerminal = merchant,
            suggestedCategory = suggestedCategory,
            idempotencyKey = idempotencyKey,
            isOtpOrServiceMessage = false,
            summaryFa = summaryFa
        )
    }

    fun detectBank(text: String): BankType {
        // Special Refah format: "رفاه حساب ..." or "بمانده...R"
        if (text.contains("رفاه") || (text.contains("بمانده") && text.contains("R"))) return BankType.REFAH

        // Special BluBank: "بلو", "بلوبانک", "پرید", "انتقال پل"
        if (text.contains("بلوبانک") || text.contains("از حساب شما پرید") || text.contains("انتقال پل") || (text.startsWith("بلو") && text.lines().size > 1)) return BankType.BLUBANK

        // Special Pasargad / Wepod: "ویپاد", "Wepod", "311."
        if (text.contains("ویپاد") || text.contains("wepod") || text.contains("311.8000")) return BankType.VEPOD
        if (text.contains("پاسارگاد")) return BankType.PASARGAD

        // Saderat algebraic compressed format: numbers followed by amount with + or -
        val saderatAlgebraic = Regex("^[0-9]{10,16}\\s*\\n\\s*([0-9,]+)[+-]", RegexOption.MULTILINE)
        if (saderatAlgebraic.containsMatchIn(text) || text.contains("صادرات") || text.contains("سامانه سپهر")) return BankType.SADERAT

        // Institutions
        if (text.contains("کوثر") || text.contains("505801")) return BankType.KOSAR
        if (text.contains("ملل") || text.contains("عسکریه") || text.contains("606256")) return BankType.MELAL
        if (text.contains("نور") && text.contains("ملی")) return BankType.NOOR

        // Major Banks
        if (text.contains("ملی ایران") || text.contains("بانک ملی") || text.contains("سداد")) return BankType.MELLI
        if (text.contains("ملت") || text.contains("بهسازان") || text.contains("610433")) return BankType.MELLAT
        if (text.contains("تجارت") || text.contains("*بانک تجارت*")) return BankType.TEJARAT
        if (text.contains("سپه") || text.contains("589210") || text.contains("ردیف:")) return BankType.SEPAH
        if (text.contains("مسکن") || text.contains("ناواکو")) return BankType.MASKAN
        if (text.contains("کشاورزی") || text.contains("مهرگستر") || text.contains("603770") || text.contains("639217")) return BankType.KESHAVARZI
        if (text.contains("توسعه تعاون") || text.contains("502908")) return BankType.TOSEE_TAAVON
        if (text.contains("صنعت و معدن")) return BankType.SANAT_MADAN
        if (text.contains("توسعه صادرات")) return BankType.TOSEE_SADERAT
        if (text.contains("پست بانک") || text.contains("627760")) return BankType.POST_BANK
        if (text.contains("مهر ایران")) return BankType.MEHR_IRAN
        if (text.contains("رسالت")) return BankType.RESALAT
        if (text.contains("سامان") || text.contains("کد ارجاع:") || text.contains("621986")) return BankType.SAMAN
        if (text.contains("پارسیان") || text.contains("لوتوس")) return BankType.PARSIAN
        if (text.contains("آینده") || text.contains("توسن")) return BankType.AYANDEH
        if (text.contains("شهر") || text.contains("shahr") || text.contains("504706") || text.contains("کد رهگیری:")) return BankType.SHAHR
        if (text.contains("گردشگری") || text.contains("کد مرجع:")) return BankType.GARDESHGARI
        if (text.contains("اقتصاد نوین") || text.contains("627412")) return BankType.EGHTESAD_NOVIN
        if (text.contains("سینا") || text.contains("شماره ارجاع:")) return BankType.SINA
        if (text.contains("بانک دی") || text.contains("دی\n") || text.contains("502938")) return BankType.DAY
        if (text.contains("کارآفرین") || text.contains("مانده حساب:")) return BankType.KARAFARIN
        if (text.contains("ایران زمین") || text.contains("505785")) return BankType.IRAN_ZAMIN
        if (text.contains("سرمایه") || text.contains("639607")) return BankType.SARMAYE
        if (text.contains("خاورمیانه") || text.contains("شناسه پیگیری:")) return BankType.KHAVARMIANEH

        return BankType.UNKNOWN
    }

    fun detectDirection(text: String): SmsTransactionDirection {
        // Special case: Saderat algebraic (+ is deposit, - is withdrawal)
        val saderatMatch = Regex("([0-9,]+)([+-])").find(text)
        if (saderatMatch != null) {
            val sign = saderatMatch.groupValues[2]
            return if (sign == "+") SmsTransactionDirection.DEPOSIT else SmsTransactionDirection.WITHDRAWAL
        }

        if (text.contains("انتقال پل") || text.contains("سامانه پل")) return SmsTransactionDirection.TRANSFER_POL
        if (text.contains("انتقال پایا") || text.contains("پایا")) return SmsTransactionDirection.TRANSFER_PAYA
        if (text.contains("انتقال به کارت") || text.contains("انتقال کارت") || text.contains("کارت به کارت")) return SmsTransactionDirection.TRANSFER_CARD

        // Anchor match: جهت واریز ("واریز", "افزایش", "بستانکار", "نشست", "+")
        if (text.contains("واریز") || text.contains("واریزمبلغ") || text.contains("افزایش") || text.contains("بستانکار") || text.contains("نشست") || text.contains("واریز مبلغ")) {
            return SmsTransactionDirection.DEPOSIT
        }

        // Anchor match: جهت برداشت ("برداشت", "خرید", "کسر", "بدهکار", "پرید", "-")
        if (text.contains("خرید") || text.contains("خريد") || text.contains("پایانه")) return SmsTransactionDirection.PURCHASE
        if (text.contains("برداشت") || text.contains("برداشت پول") || text.contains("پرید") || text.contains("کسر") || text.contains("بدهکار")) {
            return SmsTransactionDirection.WITHDRAWAL
        }

        return SmsTransactionDirection.WITHDRAWAL
    }

    fun extractAmount(text: String): Pair<Long, Long> {
        // 1. Look for algebraic format: 1,698,500+ or 2,500,000-
        val algebraic = Regex("([0-9]{1,3}(?:,[0-9]{3})+|[0-9]+)[+-]").find(text)
        if (algebraic != null) {
            val rawNum = algebraic.groupValues[1].replace(",", "").toLongOrNull() ?: 0L
            return Pair(rawNum, rawNum / 10)
        }

        // 2. Refah format: برداشت1,960,000R or واریز1,960,000R
        val refahRegex = Regex("(?:برداشت|واریز|مبلغ|خرید)\\s*([0-9]{1,3}(?:,[0-9]{3})+|[0-9]+)\\s*R", RegexOption.IGNORE_CASE)
        val refahMatch = refahRegex.find(text)
        if (refahMatch != null) {
            val rawNum = refahMatch.groupValues[1].replace(",", "").toLongOrNull() ?: 0L
            return Pair(rawNum, rawNum / 10)
        }

        // 3. BluBank informal: 900,000 ریال از حساب شما پرید
        val bluRegex = Regex("([0-9]{1,3}(?:,[0-9]{3})+|[0-9]+)\\s*(?:ریال|تومان)?\\s*از حساب شما پرید")
        val bluMatch = bluRegex.find(text)
        if (bluMatch != null) {
            val rawNum = bluMatch.groupValues[1].replace(",", "").toLongOrNull() ?: 0L
            return Pair(rawNum, rawNum / 10)
        }

        // 4. Standard anchored pattern: (واریز|برداشت|مبلغ|خرید|کسر|نشست|بستانکار|بدهکار) [: ]+ ([0-9,]+) (ریال|تومان)?
        val standardRegex = Regex("(?:واریز|برداشت|مبلغ|خرید|خريد|انتقال وجه|انتقال پل|انتقال|کسر|نشست|بستانکار|بدهکار)\\s*(?:مبلغ|وجه)?[:\\s]+([0-9]{1,3}(?:,[0-9]{3})+|[0-9]+)\\s*(ریال|تومان)?")
        val stdMatch = standardRegex.find(text)
        if (stdMatch != null) {
            val rawNum = stdMatch.groupValues[1].replace(",", "").toLongOrNull() ?: 0L
            val unit = stdMatch.groupValues[2]
            return if (unit == "تومان") {
                Pair(rawNum * 10, rawNum)
            } else {
                Pair(rawNum, rawNum / 10)
            }
        }

        // 5. Fallback: find comma-separated numbers excluding balance anchors
        val anyNumberRegex = Regex("([0-9]{1,3}(?:,[0-9]{3})+)")
        val allNumbers = anyNumberRegex.findAll(text).map { it.groupValues[1] }.toList()
        for (numStr in allNumbers) {
            val idx = text.indexOf(numStr)
            val prefix = if (idx > 12) text.substring(idx - 12, idx) else text.take(idx)
            if (!prefix.contains("مانده") && !prefix.contains("موجودی") && !prefix.contains("بمانده")) {
                val rawNum = numStr.replace(",", "").toLongOrNull() ?: 0L
                return Pair(rawNum, rawNum / 10)
            }
        }

        return Pair(0L, 0L)
    }

    fun extractBalance(text: String): Pair<Long?, Long?> {
        // Refah: بمانده1,231,509R
        val refahBal = Regex("بمانده\\s*([0-9]{1,3}(?:,[0-9]{3})+|[0-9]+)\\s*R?", RegexOption.IGNORE_CASE).find(text)
        if (refahBal != null) {
            val num = refahBal.groupValues[1].replace(",", "").toLongOrNull()
            if (num != null) return Pair(num, num / 10)
        }

        // Anchors: "مانده:", "موجودی:", "مانده حساب:", "بمانده"
        val balRegex = Regex("(?:مانده حساب|مانده|موجودی|بمانده)[:\\s]*([0-9]{1,3}(?:,[0-9]{3})+|[0-9]+)\\s*(ریال|تومان)?")
        val match = balRegex.find(text)
        if (match != null) {
            val num = match.groupValues[1].replace(",", "").toLongOrNull()
            if (num != null) {
                val unit = match.groupValues[2]
                return if (unit == "تومان") Pair(num * 10, num) else Pair(num, num / 10)
            }
        }
        return Pair(null, null)
    }

    fun extractAccountOrCard(text: String): String? {
        // Anchors: "کارت:", "حساب:", "سپرده:", "به:", "از:", "به کارت:", "از کارت:", "به حساب:", "از حساب:", "به سپرده:", "از سپرده:"
        val cardRegex = Regex("(?:به کارت|از کارت|به حساب|از حساب|به سپرده|از سپرده|کارت|حساب|سپرده|به|از)[:\\s]+([0-9.*-]{4,28})")
        val match = cardRegex.find(text)
        return match?.groupValues?.get(1)?.trim()
    }

    fun extractDestination(text: String): String? {
        // Anchors: e.g. "به: علیرضا یوسفی", "انتقال از: ویپاد"
        val destRegex = Regex("(?:به|انتقال به|واریز به|انتقال از)[:\\s]+([\\u0600-\\u06FF\\s]{3,30})")
        val match = destRegex.find(text)
        val candidate = match?.groupValues?.get(1)?.trim()
        if (candidate != null && !candidate.contains("حساب") && !candidate.contains("کارت") && !candidate.contains("سپرده")) {
            return candidate
        }
        return null
    }

    fun extractTrackingCode(text: String): String? {
        // Anchors: "پیگیری:", "کد ارجاع:", "شماره پیگیری:", "ردیف:", "سند:", "کد مرجع:", "کد رهگیری:", "شماره ارجاع:", "شناسه پیگیری:"
        val trackRegex = Regex("(?:پیگیری|کد پیگیری|شماره پیگیری|کد ارجاع|ارجاع|شماره ارجاع|ردیف|سند|شماره سند|کد مرجع|کد رهگیری|شناسه پیگیری)[:\\s]+([0-9]+)")
        val match = trackRegex.find(text)
        return match?.groupValues?.get(1)
    }

    fun extractDateTime(text: String): String? {
        val dateRegex = Regex("([0-9]{2,4}[/.-][0-9]{1,2}[/.-][0-9]{1,2}(?:[\\s-]*[0-9]{1,2}:[0-9]{1,2})?)")
        val match = dateRegex.find(text)
        return match?.groupValues?.get(1)
    }

    fun extractMerchantOrTerminal(text: String): String? {
        val termRegex = Regex("(?:پایانه|فروشگاه|خرید از|پايانه)[:\\s]*([\\u0600-\\u06FFA-Za-z0-9.·\\s]{4,40})")
        val match = termRegex.find(text)
        return match?.groupValues?.get(1)?.trim()
    }

    private fun guessCategory(text: String, dest: String?, merchant: String?, direction: SmsTransactionDirection): String {
        val full = "$text ${dest ?: ""} ${merchant ?: ""}".lowercase()

        if (full.contains("اسنپ") || full.contains("تپسی") || full.contains("تاکسی") || full.contains("مترو")) {
            return "حمل و نقل"
        }
        if (full.contains("کافه") || full.contains("قهوه") || full.contains("رستوران") || full.contains("فست فود") || full.contains("پیتزا")) {
            return "کافه و رستوران"
        }
        if (full.contains("کوروش") || full.contains("دیلی مارکت") || full.contains("هایپرمی") || full.contains("سوپرمارکت") || full.contains("لبنیات")) {
            return "سوپرمارکت و مواد غذایی"
        }
        if (full.contains("داروخانه") || full.contains("درمانگاه") || full.contains("دکتر") || full.contains("بیمارستان")) {
            return "سلامت و درمان"
        }
        if (full.contains("بنزین") || full.contains("جایگاه سوخت") || full.contains("پمپ بنزین")) {
            return "خودرو و سوخت"
        }
        if (full.contains("قبض") || full.contains("برق") || full.contains("گاز") || full.contains("آب") || full.contains("همراه اول") || full.contains("ایرانسل")) {
            return "قبوض و ارتباطات"
        }
        if (direction == SmsTransactionDirection.TRANSFER_POL || direction == SmsTransactionDirection.TRANSFER_CARD || direction == SmsTransactionDirection.TRANSFER_PAYA) {
            return "انتقال مالی"
        }
        if (direction == SmsTransactionDirection.DEPOSIT) {
            return "واریز به حساب"
        }
        return "خرید عمومی"
    }

    private fun calculateHash(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.take(8).joinToString("") { "%02x".format(it) }
    }

    private fun formatMoney(amount: Long): String {
        return "%,d".format(amount)
    }

    /**
     * Preloaded real-world samples covering ALL 33 bank and neo-bank templates from the user specification.
     */
    val SAMPLE_SMS_TEMPLATES: Map<String, String> = mapOf(
        "ملی (سداد)" to """بانک ملی ایران
واریز: ۱,۲۵۰,۰۰۰ ریال
به: ۰۱*****۱۲۳۴
مانده: ۴۵,۸۹۰,۰۰۰ ریال
۱۴۰۳/۰۸/۱۴-۱۱:۳۵
پیگیری: ۸۸۷۷۱۱""",

        "ملت (بهسازان)" to """بانک ملت
برداشت از: ۶۱۰۴۳۳******۱۲۳۴
مبلغ: ۵۰۰,۰۰۰ ریال
مانده: ۸,۴۲۰,۰۰۰ ریال
تاریخ: ۱۴۰۳/۰۹/۱۰
ساعت: ۱۸:۴۵
پیگیری: ۹۸۷۶۵۴""",

        "صادرات (جبری)" to """300325283197
1,698,500+
1403/8/5-11:43
مانده:32,317,981""",

        "تجارت" to """*بانک تجارت*
برداشت: ۷۲۰,۰۰۰ ریال
از طريق: شتاب
مانده: ۳۱,۴۰۱,۷۹۳ ریال
1403/09/29 14:22""",

        "سپه (داتین)" to """بانک سپه
برداشت: ۱,۵۰۰,۰۰۰ ریال
از: ۵۸۹۲۱۰******۷۸۹۰
مانده: ۹,۸۰۰,۰۰۰ ریال
ردیف: ۳۴۲۱
۱۴۰۳/۰۹/۰۸ ۱۳:۴۰""",

        "بلوبانک (محاوره‌ای)" to """بلو
برداشت پول
علیرضا عزیز، 900,000 ریال از حساب شما پرید.
موجودی: 64,161,452 ریال
۱۰:۴۵
۱۴۰۳.۰۸.۰۵""",

        "بلوبانک (سامانه پل)" to """بلو
انتقال پل
مبلغ: ۱,۵۰۰,۰۰۰ ریال
به: علیرضا یوسفی
مانده: ۶۲,۶۶۱,۴۵۲ ریال
کد پیگیری: ۹۸۷۶۵۴۳۲۱
1403/09/10 11:15""",

        "ویپاد (پاسارگاد)" to """بانک پاسارگاد
واریز مبلغ 2,000,000 ریال به حساب 311.8000.987654.1
انتقال از: ویپاد
موجودی: 6,800,000 ریال
شماره پیگیری: 457812
1403/09/14-11:05""",

        "مسکن (ناواکو)" to """بانک مسکن
واریز: ۲,۰۰۰,۰۰۰ ریال
به حساب: ۴۱*****۷۸۹
مانده: ۱۲,۳۰۰,۰۰۰ ریال
۱۴۰۳/۰۹/۰۴-۱۵:۳۰
شماره پیگیری: ۴۴۲۲۱۱""",

        "کشاورزی (مهرگستر)" to """بانک کشاورزی
برداشت از: ۶۰۳۷۷۰******۱۲۳۴
مبلغ: ۱,۵۰۰,۰۰۰ ریال
مانده: ۶,۸۵۰,۰۰۰ ریال
تاریخ: ۱۴۰۳/۰۸/۲۲ ساعت: ۱۰:۱۴
پیگیری: ۵۵۸۴۹۳""",

        "توسعه تعاون" to """بانک توسعه تعاون
واریز: ۵۰۰,۰۰۰ ریال
به کارت: ۵۰۲۹۰۸******۳۲۱۱
موجودی: ۳,۲۰۰,۰۰۰ ریال
پیگیری: ۱۲۴۵۷۸
۱۴۰۳/۰۹/۱۰ ۰۹:۱۵""",

        "صنعت و معدن" to """بانک صنعت و معدن
برداشت مبلغ: ۱۰,۰۰۰,۰۰۰ ریال
از حساب: ۰۱*****۴۵۶
مانده: ۱۱۰,۰۰۰,۰۰۰ ریال
سند: ۹۰۸۱۷۲
۱۴۰۳/۰۸/۱۵ ۱۱:۲۰""",

        "توسعه صادرات" to """بانک توسعه صادرات
واریز: ۵۰,۰۰۰,۰۰۰ ریال
به حساب: ۰۱*****۳۴۵
مانده: ۱۸۰,۰۰۰,۰۰۰ ریال
پیگیری: ۷۸۹۱۲۳
۱۴۰۳/۰۹/۰۱ ۱۰:۴۰""",

        "پست بانک" to """پست بانک
برداشت: ۴۵۰,۰۰۰ ریال
از: ۶۲۷۷۶۰******۴۴۳۳
مانده: ۱,۸۰۰,۰۰۰ ریال
۱۴۰۳/۰۹/۰۸ ۱۳:۴۵
ارجاع: ۷۸۹۰۱۲""",

        "رفاه کارگران" to """رفاه حساب 62****13
برداشت1,960,000R کد2000
خريد ازپايانه بانک تجارت الكترونيك پارش·پ039799
بمانده1,231,509R
1399/12/26-16:56""",

        "مهر ایران" to """بانک مهر ایران
واریز: ۳,۰۰۰,۰۰۰ ریال
به سپرده: ۱-۱۲۳۴۵۶-۴۰-۳۰۰۱
مانده: ۴۵,۰۰۰,۰۰۰ ریال
کد پیگیری: ۸۹۶۵۴۱
۱۴۰۳/۰۹/۰۳ ۱۷:۰۲""",

        "رسالت" to """بانک قرض الحسنه رسالت
برداشت از حساب: ۱۰*****۲۳۴۵
مبلغ: ۱,۲۰۰,۰۰۰ ریال
مانده: ۵,۴۰۰,۰۰۰ ریال
تاریخ: ۱۴۰۳/۰۹/۱۲ ۱۲:۰۰
کد پیگیری: ۶۶۵۴۳۲""",

        "پاسارگاد" to """بانک پاسارگاد
واریز مبلغ ۱,۰۰۰,۰۰۰ ریال به حساب ۲۳۴.۸۰۰۰.۱۲۳۴۵۶.۱
موجودی: ۱۵,۴۰۰,۰۰۰ ریال
شماره پیگیری: ۸۸۷۷۶۶
۱۴۰۳/۰۹/۰۵-۱۶:۱۰""",

        "سامان" to """خرید: ۳۴۰,۰۰۰ ریال
از کارت: ۶۲۱۹۸۶******۹۰۱۲
مانده: ۴,۲۵۰,۰۰۰ ریال
کد ارجاع: ۱۲۳۴۵۶۷۸
۱۴۰۳/۰۹/۱۵ ۲۱:۰۵""",

        "پارسیان (لوتوس)" to """بانک پارسیان
انتقال به کارت
مبلغ: ۲,۵۰۰,۰۰۰ ریال
از حساب: ۰۱*****۹۸۷۶
مانده: ۱۸,۲۰۰,۰۰۰ ریال
شماره سند: ۴۵۹۸۷۶
۱۴۰۳/۰۹/۱۱ ۱۰:۱۵""",

        "آینده (توسن)" to """بانک آینده
برداشت: ۷۵۰,۰۰۰ ریال
از سپرده: ۰۲*****۹۰۱۲
مانده: ۱۸,۳۵۰,۰۰۰ ریال
کد پیگیری: ۳۴۵۱۲۶
۱۴۰۳/۰۹/۰۲ ۲۰:۱۰""",

        "بانک شهر" to """بانک شهر
واریز مبلغ: ۵,۰۰۰,۰۰۰ ریال
به کارت: ۵۰۴۷۰۶******۷۷۸۸
موجودی: ۲۲,۱۰۰,۰۰۰ ریال
کد رهگیری: ۹۹۸۸۷۷
۱۴۰۳/۰۸/۳۰ ۰۸:۵۰""",

        "گردشگری" to """بانک گردشگری
واریز: ۲,۲۰۰,۰۰۰ ریال
به حساب: ۱۱۰*****۶۵۴
مانده: ۱۶,۵۰۰,۰۰۰ ریال
کد مرجع: ۸۸۷۶۵۴
۱۴۰۳/۰۹/۰۹ ۲۱:۱۵""",

        "اقتصاد نوین" to """بانک اقتصاد نوین
خرید: ۱,۱۰۰,۰۰۰ ریال
از کارت: ۶۲۷۴۱۲******۵۵۴۴
مانده: ۹,۸۰۰,۰۰۰ ریال
پیگیری: ۱۲۳۹۸۷
۱۴۰۳/۰۹/۰۸ ۱۹:۲۵""",

        "سینا" to """بانک سینا
برداشت از حساب: ۱۲۳*****۴۵
مبلغ: ۶۰۰,۰۰۰ ریال
مانده: ۴,۱۰۰,۰۰۰ ریال
شماره ارجاع: ۳۴۲۵۱۶
۱۴۰۳/۰۹/۰۶ ۱۴:۳۵""",

        "بانک دی" to """بانک دی
واریز: ۸۰۰,۰۰۰ ریال
به: ۵۰۲۹۳۸******۱۱۲۲
مانده: ۲,۹۰۰,۰۰۰ ریال
ردیف: ۹۸۴۵
۱۴۰۳/۰۸/۲۵ ۱۶:۴۰""",

        "کارآفرین" to """بانک کارآفرین
برداشت مبلغ: ۳,۴۰۰,۰۰۰ ریال
از حساب: ۵۴*****۱۱۲
مانده حساب: ۵۴,۰۰۰,۰۰۰ ریال
شماره پیگیری: ۳۳۴۴۵۵
۱۴۰۳/۰۸/۲۷ ۱۱:۴۵""",

        "ایران زمین" to """بانک ایران زمین
واریز: ۹۵۰,۰۰۰ ریال
به: ۵۰۵۷۸۵******۳۴۱۲
موجودی: ۶,۳۰۰,۰۰۰ ریال
کد ارجاع: ۷۸۹۳۲۱
۱۴۰۳/۰۹/۰۱ ۱۰:۰۵""",

        "سرمایه" to """بانک سرمایه
برداشت: ۳۰۰,۰۰۰ ریال
از کارت: ۶۳۹۶۰۷******۸۸۹۹
مانده: ۷,۲۰۰,۰۰۰ ریال
کد پیگیری: ۶۵۴۹۸۷
۱۴۰۳/۰۸/۲۲ ۱۸:۲۰""",

        "خاورمیانه" to """بانک خاورمیانه
انتقال وجه
مبلغ: ۵۰,۰۰۰,۰۰۰ ریال
از حساب: ۱۰۰*****۰۱
مانده: ۱,۴۵۰,۰۰۰,۰۰۰ ریال
شناسه پیگیری: ۸۷۴۵۱۲
۱۴۰۳/۰۹/۱۴ ۱۳:۱۰""",

        "مؤسسه ملل" to """موسسه اعتباری ملل
واریز: ۷۰۰,۰۰۰ ریال
به کارت: ۶۰۶۲۵۶******۷۷۱۱
مانده: ۲,۳۰۰,۰۰۰ ریال
پیگیری: ۸۸۹۹۰۰
۱۴۰۳/۰۹/۰۷ ۱۶:۵۰""",

        "کوثر (ادغام در سپه)" to """بانک سپه (کوثر سابق)
برداشت: ۴۰۰,۰۰۰ ریال
از: ۵۰۵۸۰۱******۲۲۳۳
مانده: ۵,۱۰۰,۰۰۰ ریال
ردیف: ۷۸۱۲
۱۴۰۳/۰۹/۰۳ ۱۱:۵۰""",

        "نور (انتقال به ملی)" to """بانک ملی ایران
واریز: ۸۵۰,۰۰۰ ریال
به: ۰۱*****۹۹۰۰
مانده: ۱۲,۶۵۰,۰۰۰ ریال
۱۴۰۳/۰۹/۰۲-۰۹:۱۴
پیگیری: ۳۳۴۴۱۱"""
    )
}
