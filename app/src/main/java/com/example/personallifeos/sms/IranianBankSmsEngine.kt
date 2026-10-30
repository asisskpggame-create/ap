package com.example.personallifeos.sms

import java.security.MessageDigest

object IranianBankSmsEngine {

    // Template registry with explicit version numbers (Audit Fix #2)
    private val templateVersions = mutableMapOf(
        "MELLI_STD" to 1,
        "MELLI_TRANSFER" to 1,
        "MELLAT_WITHDRAW" to 1,
        "MELLAT_DEPOSIT" to 1,
        "SADERAT_ALGEBRAIC" to 1,
        "TEJARAT_STD" to 1,
        "SEPAH_STD" to 1,
        "BLUBANK_COLLOQUIAL" to 1,
        "BLUBANK_POL_STD" to 1,
        "VEPOD_STD" to 1,
        "REFAH_COMPACT" to 1,
        "GARDESHGARI_STD" to 1,
        "KESHAVARZI_STD" to 1,
        "SAMAN_STD" to 1,
        "PASARGAD_STD" to 1,
        "MASKAN_STD" to 1,
        "GENERIC_ANCHORED" to 1
    )

    // Verified template keys in format "${bank.name}:${templateId}@v${version}"
    private val verifiedTemplates = mutableSetOf<String>()

    // Bank sender policies configured by user or defaults (Audit Fix #3)
    private val bankConfigs = mutableMapOf<BankType, BankUserConfig>()

    // In-memory processed idempotency keys (in addition to Room persistence)
    private val processedIdempotencyKeys = mutableSetOf<String>()

    init {
        resetDefaultState()
    }

    fun resetDefaultState() {
        verifiedTemplates.clear()
        bankConfigs.clear()
        processedIdempotencyKeys.clear()
        templateVersions["MELLI_STD"] = 1
        templateVersions["MELLI_TRANSFER"] = 1
        templateVersions["MELLAT_WITHDRAW"] = 1
        templateVersions["MELLAT_DEPOSIT"] = 1
        templateVersions["SADERAT_ALGEBRAIC"] = 1
        templateVersions["TEJARAT_STD"] = 1
        templateVersions["SEPAH_STD"] = 1
        templateVersions["BLUBANK_COLLOQUIAL"] = 1
        templateVersions["BLUBANK_POL_STD"] = 1
        templateVersions["VEPOD_STD"] = 1
        templateVersions["REFAH_COMPACT"] = 1
        templateVersions["GARDESHGARI_STD"] = 1
        templateVersions["KESHAVARZI_STD"] = 1
        templateVersions["SAMAN_STD"] = 1
        templateVersions["PASARGAD_STD"] = 1
        templateVersions["MASKAN_STD"] = 1
        templateVersions["GENERIC_ANCHORED"] = 1

        // Default verified core templates at v1
        verifyTemplate(BankType.MELLI, "MELLI_STD", 1)
        verifyTemplate(BankType.MELLAT, "MELLAT_WITHDRAW", 1)
        verifyTemplate(BankType.SADERAT, "SADERAT_ALGEBRAIC", 1)
        verifyTemplate(BankType.TEJARAT, "TEJARAT_STD", 1)
        verifyTemplate(BankType.SEPAH, "SEPAH_STD", 1)
        verifyTemplate(BankType.BLUBANK, "BLUBANK_COLLOQUIAL", 1)
        verifyTemplate(BankType.BLUBANK_POL, "BLUBANK_POL_STD", 1)
        verifyTemplate(BankType.VEPOD, "VEPOD_STD", 1)
        verifyTemplate(BankType.REFAH, "REFAH_COMPACT", 1)
        verifyTemplate(BankType.GARDESHGARI, "GARDESHGARI_STD", 1)
        verifyTemplate(BankType.KESHAVARZI, "KESHAVARZI_STD", 1)
        verifyTemplate(BankType.SAMAN, "SAMAN_STD", 1)
        verifyTemplate(BankType.PASARGAD, "PASARGAD_STD", 1)
        verifyTemplate(BankType.MASKAN, "MASKAN_STD", 1)
    }

    fun getTemplateKey(bank: BankType, templateId: String, version: Int): String {
        return "${bank.name}:${templateId}@v${version}"
    }

    fun verifyTemplate(bank: BankType, templateId: String, version: Int = getTemplateVersion(templateId)) {
        verifiedTemplates.add(getTemplateKey(bank, templateId, version))
    }

    fun updateTemplateVersion(templateId: String, newVersion: Int) {
        templateVersions[templateId] = newVersion
    }

    fun getTemplateVersion(templateId: String): Int {
        return templateVersions[templateId] ?: 1
    }

    fun isTemplateVerified(bank: BankType, templateId: String, version: Int = getTemplateVersion(templateId)): Boolean {
        return verifiedTemplates.contains(getTemplateKey(bank, templateId, version))
    }

    fun configureBankSenderPolicy(
        bank: BankType,
        senderNumber: String,
        requireSenderMatch: Boolean,
        verifyDefaultTemplate: Boolean = true
    ) {
        val current = bankConfigs[bank]
        val defaultTid = defaultTemplateIdForBank(bank)
        val ver = getTemplateVersion(defaultTid)
        val updatedKeys = (current?.verifiedTemplateKeys ?: emptySet()).toMutableSet()
        if (verifyDefaultTemplate) {
            val key = getTemplateKey(bank, defaultTid, ver)
            updatedKeys.add(key)
            verifiedTemplates.add(key)
        }
        bankConfigs[bank] = BankUserConfig(
            bank = bank,
            senderNumber = normalizeDigits(senderNumber).trim(),
            requireSenderMatch = requireSenderMatch,
            verifiedTemplateKeys = updatedKeys
        )
    }

    fun getConfiguredBanks(): List<BankUserConfig> {
        if (bankConfigs.isEmpty()) {
            return listOf(
                BankUserConfig(BankType.MELLI, "98300017", requireSenderMatch = false, verifiedTemplateKeys = setOf(getTemplateKey(BankType.MELLI, "MELLI_STD", 1))),
                BankUserConfig(BankType.BLUBANK, "982000", requireSenderMatch = false, verifiedTemplateKeys = setOf(getTemplateKey(BankType.BLUBANK, "BLUBANK_COLLOQUIAL", 1))),
                BankUserConfig(BankType.MELLAT, "98200033", requireSenderMatch = false, verifiedTemplateKeys = setOf(getTemplateKey(BankType.MELLAT, "MELLAT_WITHDRAW", 1))),
                BankUserConfig(BankType.SADERAT, "98300060", requireSenderMatch = false, verifiedTemplateKeys = setOf(getTemplateKey(BankType.SADERAT, "SADERAT_ALGEBRAIC", 1)))
            )
        }
        return bankConfigs.values.toList()
    }

    fun defaultTemplateIdForBank(bank: BankType): String = when (bank) {
        BankType.MELLI -> "MELLI_STD"
        BankType.MELLAT -> "MELLAT_WITHDRAW"
        BankType.SADERAT -> "SADERAT_ALGEBRAIC"
        BankType.TEJARAT -> "TEJARAT_STD"
        BankType.SEPAH -> "SEPAH_STD"
        BankType.BLUBANK -> "BLUBANK_COLLOQUIAL"
        BankType.BLUBANK_POL -> "BLUBANK_POL_STD"
        BankType.VEPOD -> "VEPOD_STD"
        BankType.REFAH -> "REFAH_COMPACT"
        BankType.GARDESHGARI -> "GARDESHGARI_STD"
        BankType.KESHAVARZI -> "KESHAVARZI_STD"
        BankType.SAMAN -> "SAMAN_STD"
        BankType.PASARGAD -> "PASARGAD_STD"
        BankType.MASKAN -> "MASKAN_STD"
        else -> "GENERIC_ANCHORED"
    }

    fun markIdempotencyKeyProcessed(key: String) {
        if (key.isNotBlank()) {
            processedIdempotencyKeys.add(key)
        }
    }

    fun isDuplicateKey(key: String): Boolean {
        return key.isNotBlank() && processedIdempotencyKeys.contains(key)
    }

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
        return result.replace('ك', 'ک').replace('ي', 'ی')
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

    fun isFailedTransactionSms(rawText: String): Boolean {
        val text = rawText.lowercase()
        val failedKeywords = listOf(
            "ناموفق", "تراکنش ناموفق", "عدم موفقیت", "لغو شد", "با خطا مواجه"
        )
        return failedKeywords.any { text.contains(it) }
    }

    /**
     * Parses an Iranian Bank SMS text into a structured BankSmsParsedResult with full audit checks.
     */
    fun parse(rawSms: String, senderAddress: String? = null): BankSmsParsedResult {
        val cleanText = normalizeDigits(rawSms).trim()
        val cleanSender = senderAddress?.let { normalizeDigits(it).trim() }?.takeIf { it.isNotBlank() }

        if (isOtpOrServiceSms(cleanText)) {
            val hash = calculateHash("OTP_" + cleanText.take(50))
            return BankSmsParsedResult(
                bank = detectBank(cleanText),
                direction = SmsTransactionDirection.UNKNOWN,
                amountRial = 0L,
                amountToman = 0L,
                detectedCurrency = DetectedCurrencyUnit.UNKNOWN,
                verificationStatus = SmsVerificationStatus.REJECTED,
                reviewReasonFa = "پیامک رمز پویا یا اطلاع‌رسانی غیرمالی",
                idempotencyKey = hash,
                isOtpOrServiceMessage = true,
                suggestedCategory = "اطلاع‌رسانی",
                summaryFa = "پیامک سیستمی یا رمز پویا (فاقد گردش مالی)"
            )
        }

        if (isFailedTransactionSms(cleanText)) {
            val hash = calculateHash("FAILED_" + cleanText.take(50))
            return BankSmsParsedResult(
                bank = detectBank(cleanText),
                direction = SmsTransactionDirection.UNKNOWN,
                amountRial = 0L,
                amountToman = 0L,
                detectedCurrency = DetectedCurrencyUnit.UNKNOWN,
                verificationStatus = SmsVerificationStatus.REJECTED,
                reviewReasonFa = "تراکنش ناموفق بانکی (ثبت نشد)",
                idempotencyKey = hash,
                isOtpOrServiceMessage = false,
                isFailedTransaction = true,
                suggestedCategory = "تراکنش ناموفق",
                summaryFa = "تراکنش ناموفق بانکی (رد شده)"
            )
        }

        val bank = detectBank(cleanText)
        val direction = detectDirection(cleanText)
        val amountExtraction = extractAmountWithCurrency(cleanText)
        val amountRial = amountExtraction.first
        val amountToman = amountExtraction.second
        val detectedCurrency = amountExtraction.third

        val (balanceRial, balanceToman) = extractBalance(cleanText)
        val accountOrCard = extractAccountOrCard(cleanText)
        val destination = extractDestination(cleanText)
        val trackingCode = extractTrackingCode(cleanText)
        val dateTime = extractDateTime(cleanText)
        val merchant = extractMerchantOrTerminal(cleanText)
        val suggestedCategory = guessCategory(cleanText, destination, merchant, direction)

        val templateId = detectTemplateId(bank, direction, cleanText)
        val templateVersion = getTemplateVersion(templateId)
        val templateVerified = isTemplateVerified(bank, templateId, templateVersion)

        // Check sender policy (Audit Fix #3)
        val senderCheck = evaluateSenderPolicy(bank, cleanSender)

        val hashPayload = "${bank.name}_${direction.name}_${amountRial}_${trackingCode ?: dateTime ?: cleanText.take(40)}"
        val idempotencyKey = calculateHash(hashPayload)

        val reviewReasons = mutableListOf<String>()
        if (bank == BankType.UNKNOWN) {
            reviewReasons.add("نام بانک مشخص نیست")
        }
        if (direction == SmsTransactionDirection.UNKNOWN) {
            reviewReasons.add("نوع تراکنش (واریز/برداشت) نامشخص است")
        }
        if (detectedCurrency == DetectedCurrencyUnit.UNKNOWN) {
            reviewReasons.add("واحد پول (ریال/تومان) در پیامک تصریح نشده است")
        }
        if (amountToman <= 0L) {
            reviewReasons.add("مبلغ معتبر تراکنش یافت نشد")
        }
        if (!templateVerified) {
            reviewReasons.add("قالب پیامک ($templateId نسخه $templateVersion) تأیید نشده است")
        }
        if (!senderCheck.first) {
            reviewReasons.add(senderCheck.second ?: "سرشماره فرستنده با پروفایل بانک تطابق ندارد")
        }

        val status = when {
            amountToman <= 0L && direction == SmsTransactionDirection.UNKNOWN -> SmsVerificationStatus.REJECTED
            isDuplicateKey(idempotencyKey) -> SmsVerificationStatus.DUPLICATE
            reviewReasons.isNotEmpty() -> SmsVerificationStatus.NEEDS_REVIEW
            else -> SmsVerificationStatus.AUTO_ACCEPTED
        }

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
            detectedCurrency = detectedCurrency,
            templateId = templateId,
            templateVersion = templateVersion,
            isTemplateVerified = templateVerified,
            senderAddress = cleanSender,
            isSenderMatched = senderCheck.first,
            verificationStatus = status,
            reviewReasonFa = if (status == SmsVerificationStatus.DUPLICATE) {
                "این پیامک قبلاً ثبت شده است (تکراری)"
            } else {
                reviewReasons.takeIf { it.isNotEmpty() }?.joinToString("؛ ")
            },
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
            isFailedTransaction = false,
            summaryFa = summaryFa
        )
    }

    /**
     * Audit Fix #1:
     * Validates a manual review confirmation before allowing it to be recorded.
     * Returns null if valid, or an explicit Persian error message if invalid.
     */
    fun validateManualConfirmReview(
        parsed: BankSmsParsedResult,
        overrideDirection: SmsTransactionDirection? = null,
        overrideCurrency: DetectedCurrencyUnit? = null,
        overrideBank: BankType? = null,
        overrideAmountToman: Long? = null
    ): String? {
        if (parsed.isOtpOrServiceMessage || parsed.isFailedTransaction) {
            return "پیامک غیرمالی یا تراکنش ناموفق قابل ثبت نیست."
        }
        if (isDuplicateKey(parsed.idempotencyKey)) {
            return "این تراکنش قبلاً ثبت شده است (مسدود شده توسط کلید ضدتکرار)."
        }
        val finalBank = overrideBank ?: parsed.bank
        if (finalBank == BankType.UNKNOWN) {
            return "لطفاً بانک مربوطه را مشخص کنید."
        }
        val finalDirection = overrideDirection ?: parsed.direction
        if (finalDirection == SmsTransactionDirection.UNKNOWN) {
            return "نوع تراکنش (واریز یا برداشت/هزینه) نامشخص است؛ لطفاً نوع تراکنش را تعیین کنید."
        }
        val finalCurrency = overrideCurrency ?: parsed.detectedCurrency
        if (finalCurrency == DetectedCurrencyUnit.UNKNOWN) {
            return "واحد پول نامشخص است؛ لطفاً واحد پول (ریال یا تومان) را مشخص کنید."
        }
        val finalAmount = overrideAmountToman ?: parsed.amountToman
        if (finalAmount <= 0L) {
            return "مبلغ تراکنش باید بزرگ‌تر از صفر باشد."
        }
        return null
    }

    private fun evaluateSenderPolicy(bank: BankType, cleanSender: String?): Pair<Boolean, String?> {
        val customConfig = bankConfigs[bank]
        if (customConfig != null) {
            val expected = normalizeDigits(customConfig.senderNumber).lowercase()
            if (customConfig.requireSenderMatch) {
                if (cleanSender.isNullOrBlank()) {
                    return Pair(false, "ارسال سرشماره برای ${bank.faName} الزامی است اما سرشماره‌ای دریافت نشد")
                }
                val normalizedActual = cleanSender.lowercase()
                if (!normalizedActual.contains(expected) && !expected.contains(normalizedActual)) {
                    return Pair(false, "سرشماره ($cleanSender) با سرشماره ثبت‌شده (${customConfig.senderNumber}) همخوانی ندارد")
                }
            } else if (!cleanSender.isNullOrBlank() && expected.isNotBlank()) {
                val normalizedActual = cleanSender.lowercase()
                if (!normalizedActual.contains(expected) && !expected.contains(normalizedActual)) {
                    return Pair(false, "سرشماره فرستنده ($cleanSender) با سرشماره انتخابی بانک مطابقت ندارد")
                }
            }
            return Pair(true, null)
        }

        // Default sender policy when user hasn't customized this bank yet:
        // Sender is optional unless provided and explicitly conflicting with known bank senders.
        if (!cleanSender.isNullOrBlank() && bank != BankType.UNKNOWN && bank.defaultSenders.isNotEmpty()) {
            val actualLower = cleanSender.lowercase()
            val matchesDefault = bank.defaultSenders.any { def ->
                val d = def.lowercase()
                actualLower.contains(d) || d.contains(actualLower)
            }
            if (!matchesDefault) {
                return Pair(false, "سرشماره فرستنده ($cleanSender) متعلق به ${bank.faName} نیست")
            }
        }
        return Pair(true, null)
    }

    private fun detectTemplateId(bank: BankType, direction: SmsTransactionDirection, text: String): String {
        return when (bank) {
            BankType.MELLI -> if (direction == SmsTransactionDirection.TRANSFER_CARD || direction == SmsTransactionDirection.TRANSFER_PAYA) "MELLI_TRANSFER" else "MELLI_STD"
            BankType.MELLAT -> if (direction == SmsTransactionDirection.DEPOSIT) "MELLAT_DEPOSIT" else "MELLAT_WITHDRAW"
            BankType.SADERAT -> "SADERAT_ALGEBRAIC"
            BankType.TEJARAT -> "TEJARAT_STD"
            BankType.SEPAH, BankType.KOSAR -> "SEPAH_STD"
            BankType.BLUBANK -> "BLUBANK_COLLOQUIAL"
            BankType.BLUBANK_POL -> "BLUBANK_POL_STD"
            BankType.VEPOD -> "VEPOD_STD"
            BankType.REFAH -> "REFAH_COMPACT"
            BankType.GARDESHGARI -> "GARDESHGARI_STD"
            BankType.KESHAVARZI -> "KESHAVARZI_STD"
            BankType.SAMAN -> "SAMAN_STD"
            BankType.PASARGAD -> "PASARGAD_STD"
            BankType.MASKAN -> "MASKAN_STD"
            else -> "GENERIC_ANCHORED"
        }
    }

    fun detectBank(text: String): BankType {
        if (text.contains("رفاه") || (text.contains("بمانده") && text.contains("R"))) return BankType.REFAH
        if (text.contains("انتقال پل") || (text.contains("بلو") && text.contains("پل"))) return BankType.BLUBANK_POL
        if (text.contains("بلوبانک") || text.contains("از حساب شما پرید") || (text.startsWith("بلو") && text.lines().size > 1)) return BankType.BLUBANK
        if (text.contains("ویپاد") || text.contains("wepod") || text.contains("311.8000")) return BankType.VEPOD
        if (text.contains("پاسارگاد")) return BankType.PASARGAD

        val saderatAlgebraic = Regex("^[0-9]{10,16}\\s*\\n\\s*([0-9,]+)[+-]", RegexOption.MULTILINE)
        if (saderatAlgebraic.containsMatchIn(text) || text.contains("صادرات") || text.contains("سامانه سپهر")) return BankType.SADERAT

        if (text.contains("کوثر") || text.contains("505801")) return BankType.KOSAR
        if (text.contains("ملل") || text.contains("عسکریه") || text.contains("606256")) return BankType.MELAL
        if (text.contains("نور") && text.contains("ملی")) return BankType.NOOR

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

    /**
     * Audit Fix #1:
     * NEVER defaults to WITHDRAWAL when direction is ambiguous. Returns UNKNOWN instead.
     */
    fun detectDirection(text: String): SmsTransactionDirection {
        val saderatMatch = Regex("([0-9,]+)([+-])").find(text)
        if (saderatMatch != null) {
            val sign = saderatMatch.groupValues[2]
            return if (sign == "+") SmsTransactionDirection.DEPOSIT else SmsTransactionDirection.WITHDRAWAL
        }

        if (text.contains("انتقال پل") || text.contains("سامانه پل")) return SmsTransactionDirection.TRANSFER_POL
        if (text.contains("انتقال پایا") || text.contains("پایا")) return SmsTransactionDirection.TRANSFER_PAYA
        if (text.contains("انتقال به کارت") || text.contains("انتقال کارت") || text.contains("کارت به کارت") || text.contains("انتقال وجه")) return SmsTransactionDirection.TRANSFER_CARD

        val hasDeposit = text.contains("واریز") || text.contains("واریزمبلغ") || text.contains("افزایش") || text.contains("بستانکار") || text.contains("نشست") || text.contains("واریز مبلغ")
        val hasWithdraw = text.contains("برداشت") || text.contains("برداشت پول") || text.contains("پرید") || text.contains("کسر") || text.contains("بدهکار") || text.contains("خرید") || text.contains("خريد") || text.contains("پایانه")

        if (hasDeposit && !hasWithdraw) {
            return SmsTransactionDirection.DEPOSIT
        }
        if (hasWithdraw && !hasDeposit) {
            if (text.contains("خرید") || text.contains("خريد") || text.contains("پایانه")) {
                return SmsTransactionDirection.PURCHASE
            }
            return SmsTransactionDirection.WITHDRAWAL
        }

        return SmsTransactionDirection.UNKNOWN
    }

    fun extractAmount(text: String): Pair<Long, Long> {
        val res = extractAmountWithCurrency(text)
        return Pair(res.first, res.second)
    }

    fun extractAmountWithCurrency(text: String): Triple<Long, Long, DetectedCurrencyUnit> {
        // 1. Saderat algebraic format: 1,698,500+ or 2,500,000- (Rial by Saderat standard)
        val algebraic = Regex("([0-9]{1,3}(?:,[0-9]{3})+|[0-9]{4,})[+-]").find(text)
        if (algebraic != null) {
            val rawNum = algebraic.groupValues[1].replace(",", "").toLongOrNull() ?: 0L
            return Triple(rawNum, rawNum / 10, DetectedCurrencyUnit.RIAL)
        }

        // 2. Refah format: برداشت1,960,000R or واریز1,960,000R
        val refahRegex = Regex("(?:برداشت|واریز|مبلغ|خرید)\\s*([0-9]{1,3}(?:,[0-9]{3})+|[0-9]+)\\s*R", RegexOption.IGNORE_CASE)
        val refahMatch = refahRegex.find(text)
        if (refahMatch != null) {
            val rawNum = refahMatch.groupValues[1].replace(",", "").toLongOrNull() ?: 0L
            return Triple(rawNum, rawNum / 10, DetectedCurrencyUnit.RIAL)
        }

        // 3. BluBank informal: 900,000 ریال از حساب شما پرید
        val bluRegex = Regex("([0-9]{1,3}(?:,[0-9]{3})+|[0-9]+)\\s*(ریال|تومان)?\\s*از حساب شما پرید")
        val bluMatch = bluRegex.find(text)
        if (bluMatch != null) {
            val rawNum = bluMatch.groupValues[1].replace(",", "").toLongOrNull() ?: 0L
            val unit = bluMatch.groupValues[2]
            return when (unit) {
                "تومان" -> Triple(rawNum * 10, rawNum, DetectedCurrencyUnit.TOMAN)
                "ریال" -> Triple(rawNum, rawNum / 10, DetectedCurrencyUnit.RIAL)
                else -> Triple(rawNum, rawNum / 10, DetectedCurrencyUnit.UNKNOWN)
            }
        }

        // 4. Standard anchored pattern
        val standardRegex = Regex("(?:واریز|برداشت|مبلغ|خرید|خريد|انتقال وجه|انتقال پل|انتقال|کسر|نشست|بستانکار|بدهکار)\\s*(?:مبلغ|وجه)?[:\\s]+([0-9]{1,3}(?:[,/.][0-9]{3})+|[0-9]+)\\s*(ریال|تومان)?")
        val stdMatch = standardRegex.find(text)
        if (stdMatch != null) {
            val rawNum = stdMatch.groupValues[1].replace(Regex("[,/.]"), "").toLongOrNull() ?: 0L
            val unit = stdMatch.groupValues[2]
            return when {
                unit == "تومان" -> Triple(rawNum * 10, rawNum, DetectedCurrencyUnit.TOMAN)
                unit == "ریال" -> Triple(rawNum, rawNum / 10, DetectedCurrencyUnit.RIAL)
                text.contains("تومان") -> Triple(rawNum * 10, rawNum, DetectedCurrencyUnit.TOMAN)
                text.contains("ریال") -> Triple(rawNum, rawNum / 10, DetectedCurrencyUnit.RIAL)
                else -> Triple(rawNum, rawNum / 10, DetectedCurrencyUnit.UNKNOWN)
            }
        }

        // 5. Fallback: comma-separated numbers excluding balance anchors
        val anyNumberRegex = Regex("([0-9]{1,3}(?:,[0-9]{3})+)")
        val allNumbers = anyNumberRegex.findAll(text).map { it.groupValues[1] }.toList()
        for (numStr in allNumbers) {
            val idx = text.indexOf(numStr)
            val prefix = if (idx > 12) text.substring(idx - 12, idx) else text.take(idx)
            if (!prefix.contains("مانده") && !prefix.contains("موجودی") && !prefix.contains("بمانده")) {
                val rawNum = numStr.replace(",", "").toLongOrNull() ?: 0L
                val currency = when {
                    text.contains("تومان") -> DetectedCurrencyUnit.TOMAN
                    text.contains("ریال") -> DetectedCurrencyUnit.RIAL
                    else -> DetectedCurrencyUnit.UNKNOWN
                }
                val toman = if (currency == DetectedCurrencyUnit.TOMAN) rawNum else rawNum / 10
                return Triple(rawNum, toman, currency)
            }
        }

        return Triple(0L, 0L, DetectedCurrencyUnit.UNKNOWN)
    }

    fun extractBalance(text: String): Pair<Long?, Long?> {
        val refahBal = Regex("بمانده\\s*([0-9]{1,3}(?:,[0-9]{3})+|[0-9]+)\\s*R?", RegexOption.IGNORE_CASE).find(text)
        if (refahBal != null) {
            val num = refahBal.groupValues[1].replace(",", "").toLongOrNull()
            if (num != null) return Pair(num, num / 10)
        }

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
        val cardRegex = Regex("(?:به کارت|از کارت|به حساب|از حساب|به سپرده|از سپرده|کارت|حساب|سپرده|به|از)[:\\s]+([0-9.*-]{4,28})")
        val match = cardRegex.find(text)
        return match?.groupValues?.get(1)?.trim()
    }

    fun extractDestination(text: String): String? {
        val destRegex = Regex("(?:به|انتقال به|واریز به|انتقال از)[:\\s]+([\\u0600-\\u06FF\\s]{3,30})")
        val match = destRegex.find(text)
        val candidate = match?.groupValues?.get(1)?.trim()
        if (candidate != null && !candidate.contains("حساب") && !candidate.contains("کارت") && !candidate.contains("سپرده")) {
            return candidate
        }
        return null
    }

    fun extractTrackingCode(text: String): String? {
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
        if (full.contains("کوروش") || full.contains("دیلی مارکت") || full.contains("هایپرمی") || full.contains("سوپرمارکت") || full.contains("لبنیات") || full.contains("رفاه")) {
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

        "گردشگری" to """بانک گردشگری
واریز: ۲,۲۰۰,۰۰۰ ریال
به حساب: ۱۱۰*****۶۵۴
مانده: ۱۶,۵۰۰,۰۰۰ ریال
کد مرجع: ۸۸۷۶۵۴
۱۴۰۳/۰۹/۰۹ ۲۱:۱۵""",

        "کشاورزی (مهرگستر)" to """بانک کشاورزی
برداشت از: ۶۰۳۷۷۰******۱۲۳۴
مبلغ: ۱,۵۰۰,۰۰۰ ریال
مانده: ۶,۸۵۰,۰۰۰ ریال
تاریخ: ۱۴۰۳/۰۸/۲۲ ساعت: ۱۰:۱۴
پیگیری: ۵۵۸۴۹۳""",

        "رفاه کارگران" to """رفاه حساب 62****13
برداشت1,960,000R کد2000
خريد ازپايانه بانک تجارت الكترونيك پارش·پ039799
بمانده1,231,509R
1399/12/26-16:56""",

        "سامان" to """بانک سامان
خرید: ۳۴۰,۰۰۰ ریال
از کارت: ۶۲۱۹۸۶******۹۰۱۲
مانده: ۴,۲۵۰,۰۰۰ ریال
کد ارجاع: ۱۲۳۴۵۶۷۸
۱۴۰۳/۰۹/۱۵ ۲۱:۰۵""",

        "پیامک مبهم (تست بررسی)" to """بانک ملت
حساب ۶۱۰۴۳۳******۱۲۳۴
مبلغ: ۸۵۰,۰۰۰ ریال
مانده: ۹,۱۰۰,۰۰۰ ریال
تاریخ: ۱۴۰۳/۰۹/۱۸"""
    )
}
