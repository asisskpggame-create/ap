package com.example.sms

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Multi-layer Iranian Bank SMS Parser Engine:
 * Layer 1: Normalization (digits, Arabic/Persian characters, ZWNJ, separators)
 * Layer 2: Non-financial / OTP / Failed Transaction / Balance-Only Exclusion
 * Layer 3: Bank Identification & Refund/Transfer/Template Matching
 * Layer 4: Contextual Amount vs Balance vs Fee Disambiguation (never picking balance, fee, date, or card as amount)
 * Layer 5: Exact Rial -> Toman Conversion (BigDecimal / 10) & Deterministic Idempotency Fingerprinting
 */
object BankSmsParser {

    private val labeledAmountRegex = Regex(
        """(?:مبلغ(?:\s*تراکنش)?|برداشت|واریز|خرید(?:\s*کالا|\s*اینترنتی|\s*پوز)?|پرداخت(?:\s*قبض)?|کاهش|افزایش|انتقال(?:\s*وجه)?|حواله|قسط|سود|حقوق)\s*[:：=-]?\s*([0-9]{1,3}(?:/[0-9]{3})+|[0-9]{1,3}(?:\.[0-9]{3}){2,}|[0-9,]+(?:\.[0-9]{1,2})?)(?![0-9*×xX/-])\s*(ریال|تومان|تومن|rial|irr|toman)?""",
        RegexOption.IGNORE_CASE
    )

    private val maskedCardPattern = Regex("""(?:\b\d{4,6}[*×xX-]+\d{4}\b)|(?:[*×xX]{2,}\s*\d{4}\b)|(?:کارت\s*[:：]?\s*(?:\d{4,6}[*×xX-]+)?(\d{4})\b(?![*×xX-]))""")
    private val accountSuffixPattern = Regex("""(?:حساب|سپرده)\s*[:：]?\s*([0-9./-]{4,20})""")
    private val referenceNumberRegex = Regex("""(?:پیگیری|پيگيري|مرجع|سند|رهگیری|شماره\s*مرجع)\s*[:：#=-]?\s*([0-9]{4,20})""")
    private val dateTimeRegex = Regex("""(\b1[34]\d{2}/\d{1,2}/\d{1,2}(?:\s*[-–]?\s*\d{1,2}:\d{2}(?::\d{2})?)?|\b\d{2}/\d{1,2}/\d{1,2}(?:\s*[-–]?\s*\d{1,2}:\d{2}(?::\d{2})?)?|\b\d{1,2}:\d{2}(?::\d{2})?\b)""")

    private val rialUnitRegex = Regex("""(ریال|ريال|\brial\b|\birr\b)""", RegexOption.IGNORE_CASE)
    private val tomanUnitRegex = Regex("""(تومان|تومن|\btoman\b|\btmn\b)""", RegexOption.IGNORE_CASE)

    private fun convertToTomanExact(rawAmount: BigDecimal, isRial: Boolean): Double {
        val inToman = if (isRial) {
            rawAmount.divide(BigDecimal.TEN, 2, RoundingMode.HALF_UP)
        } else {
            rawAmount.setScale(2, RoundingMode.HALF_UP)
        }
        return inToman.toDouble()
    }

    fun parse(rawSms: String, senderNumber: String = ""): BankSmsParseResult {
        val normalized = BankSmsNormalizer.normalize(rawSms)
        if (normalized.isBlank()) {
            return BankSmsParseResult(
                status = BankSmsParseStatus.UNRECOGNIZED,
                statusMessageFa = "متن پیامک خالی است."
            )
        }

        val lowerText = normalized.lowercase()
        val fingerprint = BankSmsNormalizer.computeFingerprint(rawSms, senderNumber)

        // --- Layer 2: Exclusion of OTPs, Failed Transactions & Non-Financial Alerts ---
        if (BankSmsPatternRegistry.otpExclusionKeywords.any { lowerText.contains(it) }) {
            return BankSmsParseResult(
                status = BankSmsParseStatus.IGNORED_OTP,
                fingerprint = fingerprint,
                normalizedText = normalized,
                statusMessageFa = "این پیامک حاوی رمز پویا یا کد تأیید است و تراکنش مالی محسوب نمی‌شود."
            )
        }

        if (BankSmsPatternRegistry.failedOrCancelledTransactionKeywords.any { lowerText.contains(it) }) {
            return BankSmsParseResult(
                status = BankSmsParseStatus.IGNORED_NON_FINANCIAL,
                fingerprint = fingerprint,
                normalizedText = normalized,
                statusMessageFa = "این پیامک مربوط به یک تراکنش ناموفق یا لغوشده است و نباید ثبت شود."
            )
        }

        if (BankSmsPatternRegistry.nonFinancialAlertKeywords.any { lowerText.contains(it) }) {
            return BankSmsParseResult(
                status = BankSmsParseStatus.IGNORED_NON_FINANCIAL,
                fingerprint = fingerprint,
                normalizedText = normalized,
                statusMessageFa = "این پیامک یک اعلان غیرمالی یا تبلیغاتی است."
            )
        }

        val isRefundOrReversal = BankSmsPatternRegistry.refundReversalKeywords.any { lowerText.contains(it) }
        val isFeeOnlySms = (lowerText.contains("کسر کارمزد") || lowerText.contains("برداشت کارمزد")) &&
            !lowerText.contains("خرید") && !lowerText.contains("انتقال")

        val hasTransactionIndicator = isRefundOrReversal || isFeeOnlySms ||
            lowerText.contains("برداشت") || lowerText.contains("واریز") ||
            lowerText.contains("خرید") || lowerText.contains("پرداخت") ||
            lowerText.contains("انتقال") || lowerText.contains("کاهش") ||
            lowerText.contains("افزایش") || lowerText.contains("بدهکار") ||
            lowerText.contains("بستانکار") || lowerText.contains("حواله") ||
            lowerText.contains("پایا") || lowerText.contains("ساتنا") ||
            lowerText.contains("کارت به کارت") || lowerText.contains("کارمزد")

        val hasBalanceIndicator = lowerText.contains("مانده") || lowerText.contains("موجودی")
        if (hasBalanceIndicator && !hasTransactionIndicator) {
            return BankSmsParseResult(
                status = BankSmsParseStatus.IGNORED_NON_FINANCIAL,
                fingerprint = fingerprint,
                normalizedText = normalized,
                statusMessageFa = "این پیامک صرفاً اعلام موجودی حساب است و تراکنش ورودی یا خروجی ندارد."
            )
        }

        if (!hasTransactionIndicator && !lowerText.contains("مبلغ")) {
            return BankSmsParseResult(
                status = BankSmsParseStatus.UNRECOGNIZED,
                fingerprint = fingerprint,
                normalizedText = normalized,
                statusMessageFa = "نشانه معتبری از تراکنش بانکی در متن پیامک یافت نشد."
            )
        }

        // --- Layer 3: Bank Identification & Card/Account Suffix Extraction ---
        val matchedBank = BankSmsPatternRegistry.findBank(normalized, senderNumber)
        val isSharedAmbiguousSender = BankSmsPatternRegistry.isAmbiguousSharedSender(normalized, senderNumber)
        val bankId = matchedBank?.bankId ?: "GENERIC_IR_BANK"
        val bankName = matchedBank?.displayNameFa ?: detectFallbackBankName(normalized)
        val cardOrAccountSuffix = extractCardOrAccountSuffix(normalized)
        val referenceNumber = referenceNumberRegex.find(normalized)?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
        val dateTimeText = dateTimeRegex.findAll(normalized).map { it.value.trim() }.joinToString(" ").takeIf { it.isNotBlank() }

        // Extract Balance & Fee first so their numbers are never confused with the primary transaction amount
        var balanceRawBd: BigDecimal? = null
        var balanceUnitStr = ""
        val balanceMatch = BankSmsPatternRegistry.getCommonBalanceRegex().find(normalized)
        if (balanceMatch != null) {
            balanceRawBd = BankSmsNormalizer.parseFinancialAmountBigDecimal(balanceMatch.groupValues[1])
            balanceUnitStr = balanceMatch.groupValues.getOrNull(2)?.trim().orEmpty()
        }

        var feeRawBd: BigDecimal? = null
        var feeUnitStr = ""
        val feeMatch = BankSmsPatternRegistry.feeAmountRegex.find(normalized)
        if (feeMatch != null && !isFeeOnlySms) {
            feeRawBd = BankSmsNormalizer.parseFinancialAmountBigDecimal(feeMatch.groupValues[1])
            feeUnitStr = feeMatch.groupValues.getOrNull(2)?.trim().orEmpty()
        }

        val hasExplicitRialInText = rialUnitRegex.containsMatchIn(normalized)
        val hasExplicitTomanInText = tomanUnitRegex.containsMatchIn(normalized)
        val hasAnyExplicitCurrencyInText = hasExplicitRialInText || hasExplicitTomanInText

        // Try bank-specific templates (skip debit templates if SMS is a refund/reversal!)
        var rawAmountBd: BigDecimal? = null
        var matchedRuleId = ""
        var detectedType: BankTransactionType? = null
        var wasInRial = true
        var matchedExplicitUnit = false

        if (matchedBank != null) {
            for (rule in matchedBank.templates) {
                if (isRefundOrReversal && rule.transactionType == BankTransactionType.EXPENSE) {
                    continue
                }
                val m = rule.amountRegex.find(normalized)
                if (m != null) {
                    val amtBd = BankSmsNormalizer.parseFinancialAmountBigDecimal(m.groupValues[1])
                    if (amtBd != null && amtBd > BigDecimal.ZERO) {
                        rawAmountBd = amtBd
                        matchedRuleId = rule.ruleId
                        detectedType = rule.transactionType
                        val unitStr = m.groupValues.getOrNull(2)?.lowercase() ?: ""
                        matchedExplicitUnit = unitStr.isNotBlank()
                        wasInRial = when {
                            tomanUnitRegex.containsMatchIn(unitStr) -> false
                            rialUnitRegex.containsMatchIn(unitStr) -> true
                            hasExplicitTomanInText && !hasExplicitRialInText -> false
                            else -> rule.defaultCurrencyIsRial
                        }
                        break
                    }
                }
            }
        }

        // --- Layer 4: Contextual Amount vs Balance vs Fee Disambiguation ---
        var ambiguousAmountCandidates = false
        if (rawAmountBd == null) {
            val nonBalanceLines = normalized.lines().filterNot { line ->
                val l = line.lowercase().trim()
                (l.startsWith("مانده") || l.startsWith("موجودی")) &&
                    !l.contains("مبلغ") && !l.contains("برداشت") && !l.contains("واریز") && !l.contains("خرید")
            }.joinToString("\n")

            // Remove fee clause before searching for primary transaction amount (unless it's a fee-only SMS)
            val textWithoutFeeAndBalance = if (!isFeeOnlySms) {
                nonBalanceLines.replace(BankSmsPatternRegistry.feeAmountRegex, " ")
                    .replace(BankSmsPatternRegistry.getCommonBalanceRegex(), " ")
            } else {
                nonBalanceLines.replace(BankSmsPatternRegistry.getCommonBalanceRegex(), " ")
            }

            val scrubbedWithoutFeeAndBalance = BankSmsNormalizer.stripNonMonetaryTokens(textWithoutFeeAndBalance)
            val labeledMatch = labeledAmountRegex.find(scrubbedWithoutFeeAndBalance)
            if (labeledMatch != null) {
                val parsedVal = BankSmsNormalizer.parseFinancialAmountBigDecimal(labeledMatch.groupValues[1])
                if (parsedVal != null && parsedVal > BigDecimal.ZERO) {
                    rawAmountBd = parsedVal
                    matchedRuleId = "generic_labeled_amount"
                    val unitStr = labeledMatch.groupValues.getOrNull(2)?.lowercase() ?: ""
                    matchedExplicitUnit = unitStr.isNotBlank() || hasAnyExplicitCurrencyInText
                    wasInRial = when {
                        tomanUnitRegex.containsMatchIn(unitStr) -> false
                        rialUnitRegex.containsMatchIn(unitStr) -> true
                        hasExplicitTomanInText && !hasExplicitRialInText -> false
                        else -> true
                    }
                }
            }

            if (rawAmountBd == null && isFeeOnlySms && feeMatch != null) {
                val parsedFee = BankSmsNormalizer.parseFinancialAmountBigDecimal(feeMatch.groupValues[1])
                if (parsedFee != null && parsedFee > BigDecimal.ZERO) {
                    rawAmountBd = parsedFee
                    matchedRuleId = "fee_only_sms"
                    val unitStr = feeMatch.groupValues.getOrNull(2)?.lowercase() ?: ""
                    matchedExplicitUnit = unitStr.isNotBlank() || hasAnyExplicitCurrencyInText
                    wasInRial = !(tomanUnitRegex.containsMatchIn(unitStr) || (hasExplicitTomanInText && !hasExplicitRialInText))
                }
            }

            if (rawAmountBd == null) {
                // Scrub dates, times, masked cards, accounts, reference numbers, and balance/fee clauses
                val scrubbedForFallback = BankSmsNormalizer.stripNonMonetaryTokens(textWithoutFeeAndBalance)

                val candidateTokens = Regex("""(?<![\d/\.\*])(\d{1,3}(?:[,/]\d{3})+(?:\.\d{1,2})?|\d{1,3}(?:\.\d{3}){2,}|\d+(?:\.\d{1,2})?)(?![\d/\*])""")
                    .findAll(scrubbedForFallback)
                    .mapNotNull { BankSmsNormalizer.parseFinancialAmountBigDecimal(it.groupValues[1]) }
                    .filter { candidate ->
                        candidate >= BigDecimal("100") &&
                            (balanceRawBd == null || candidate.compareTo(balanceRawBd) != 0) &&
                            (feeRawBd == null || candidate.compareTo(feeRawBd) != 0)
                    }
                    .toList()

                if (candidateTokens.isNotEmpty()) {
                    rawAmountBd = candidateTokens.first()
                    matchedRuleId = "generic_fallback_disambiguated"
                    matchedExplicitUnit = hasAnyExplicitCurrencyInText
                    wasInRial = !(hasExplicitTomanInText && !hasExplicitRialInText)
                    if (candidateTokens.distinct().size > 1) {
                        ambiguousAmountCandidates = true
                    }
                }
            }
        }

        val finalRawAmountBd = rawAmountBd
        if (finalRawAmountBd == null || finalRawAmountBd <= BigDecimal.ZERO) {
            return BankSmsParseResult(
                status = BankSmsParseStatus.UNRECOGNIZED,
                bankId = bankId,
                bankName = bankName,
                cardOrAccountSuffix = cardOrAccountSuffix,
                referenceNumber = referenceNumber,
                dateTimeText = dateTimeText,
                fingerprint = fingerprint,
                normalizedText = normalized,
                statusMessageFa = "مبلغ معتبری در این پیامک تشخیص داده نشد."
            )
        }

        // Determine transaction direction with full priority for Refund/Reversal and Explicit Self-Transfer
        val isSelfOrExplicitTransfer = lowerText.contains("انتقال بین حساب") ||
            lowerText.contains("به حساب خود") ||
            (lowerText.contains("انتقال وجه") && !lowerText.contains("برداشت") && !lowerText.contains("واریز"))

        val finalType = when {
            isRefundOrReversal -> BankTransactionType.INCOME
            isSelfOrExplicitTransfer -> BankTransactionType.TRANSFER
            detectedType != null -> detectedType
            else -> {
                val isIncome = lowerText.contains("واریز") ||
                    lowerText.contains("افزایش") ||
                    lowerText.contains("بستانکار") ||
                    lowerText.contains("دریافت") ||
                    lowerText.contains("سود") ||
                    lowerText.contains("حقوق")
                val isDebit = lowerText.contains("برداشت") ||
                    lowerText.contains("خرید") ||
                    lowerText.contains("پرداخت") ||
                    lowerText.contains("کاهش") ||
                    lowerText.contains("بدهکار") ||
                    lowerText.contains("کسر")
                val isTransferKeyword = lowerText.contains("انتقال") ||
                    lowerText.contains("کارت به کارت") ||
                    lowerText.contains("پایا") ||
                    lowerText.contains("ساتنا") ||
                    lowerText.contains("پل")
                when {
                    isIncome && !isDebit -> BankTransactionType.INCOME
                    isTransferKeyword && !isDebit && !isIncome -> BankTransactionType.TRANSFER
                    isDebit -> BankTransactionType.EXPENSE
                    else -> BankTransactionType.UNKNOWN
                }
            }
        }

        val isDirectionUnknown = finalType == BankTransactionType.UNKNOWN &&
            !lowerText.contains("برداشت") && !lowerText.contains("واریز") &&
            !lowerText.contains("خرید") && !lowerText.contains("پرداخت")

        val isCurrencyMissing = !matchedExplicitUnit && !hasAnyExplicitCurrencyInText
        if (isCurrencyMissing) {
            wasInRial = false
        }
        val detectedCurrency = when {
            isCurrencyMissing -> "UNKNOWN"
            wasInRial -> "IRR"
            else -> "TOMAN"
        }

        // --- Layer 5: Exact Rial -> Toman Conversion (BigDecimal) ---
        val amountInToman = convertToTomanExact(finalRawAmountBd, wasInRial)
        val balanceIsRial = when {
            tomanUnitRegex.containsMatchIn(balanceUnitStr) -> false
            rialUnitRegex.containsMatchIn(balanceUnitStr) -> true
            else -> wasInRial
        }
        val balanceInToman = balanceRawBd?.let { convertToTomanExact(it, balanceIsRial) }

        val feeIsRial = when {
            tomanUnitRegex.containsMatchIn(feeUnitStr) -> false
            rialUnitRegex.containsMatchIn(feeUnitStr) -> true
            else -> wasInRial
        }
        val feeInToman = feeRawBd?.let { convertToTomanExact(it, feeIsRial) }

        val (suggestedTitle, suggestedCategory) = inferMerchantAndCategory(
            normalized = normalized,
            bankName = bankName,
            txType = finalType,
            isRefund = isRefundOrReversal,
            isFeeOnly = isFeeOnlySms
        )

        val uncertainFields = buildList {
            if (isCurrencyMissing) add("currency")
            if (isDirectionUnknown) add("transactionType")
            if (ambiguousAmountCandidates) add("amount")
            if (isSharedAmbiguousSender) add("bankName")
        }

        val confidence = when {
            isCurrencyMissing || isDirectionUnknown || ambiguousAmountCandidates -> 0.55f
            isSharedAmbiguousSender -> 0.65f
            matchedRuleId.startsWith("generic_fallback") -> 0.72f
            matchedBank != null -> 0.96f
            else -> 0.85f
        }

        val status = if (confidence >= 0.70f && uncertainFields.isEmpty()) {
            BankSmsParseStatus.SUCCESS
        } else {
            BankSmsParseStatus.NEEDS_REVIEW
        }

        val statusMessageFa = when {
            isCurrencyMissing -> "واحد پول (ریال/تومان) در پیامک ذکر نشده است؛ لطفاً مبلغ و واحد را پیش از ثبت بررسی کنید."
            isDirectionUnknown -> "نوع تراکنش (برداشت یا واریز) در متن پیامک مبهم است؛ لطفاً پیش از ثبت بررسی کنید."
            ambiguousAmountCandidates -> "چند عدد در پیامک وجود دارد؛ لطفاً مبلغ اصلی تراکنش را بررسی و تأیید کنید."
            isSharedAmbiguousSender -> "سرشماره ارسال‌کننده بین چند بانک مشترک است؛ لطفاً نام بانک را بررسی کنید."
            else -> "اطلاعات تراکنش با دقت بالا استخراج شد."
        }

        return BankSmsParseResult(
            status = status,
            bankId = bankId,
            bankName = bankName,
            transactionType = finalType,
            amountInToman = amountInToman,
            rawAmountDetected = finalRawAmountBd.toDouble(),
            wasInRial = wasInRial,
            detectedCurrency = detectedCurrency,
            balanceInToman = balanceInToman,
            feeInToman = feeInToman,
            cardOrAccountSuffix = cardOrAccountSuffix,
            referenceNumber = referenceNumber,
            dateTimeText = dateTimeText,
            suggestedTitle = suggestedTitle,
            suggestedCategory = suggestedCategory,
            confidence = confidence,
            matchedRuleId = matchedRuleId,
            fingerprint = fingerprint,
            normalizedText = normalized,
            uncertainFields = uncertainFields,
            statusMessageFa = statusMessageFa
        )
    }

    private fun extractCardOrAccountSuffix(normalizedText: String): String {
        val cardMatch = maskedCardPattern.find(normalizedText)
        if (cardMatch != null) {
            val digits = Regex("""\d{4}\b""").findAll(cardMatch.value).lastOrNull()?.value
            if (!digits.isNullOrBlank()) return digits
        }
        val accMatch = accountSuffixPattern.find(normalizedText)
        if (accMatch != null) {
            val accRaw = accMatch.groupValues[1].trim()
            val tailDigits = accRaw.filter { it.isDigit() }.takeLast(4)
            if (tailDigits.length >= 3) return tailDigits
        }
        return ""
    }

    private fun detectFallbackBankName(normalizedText: String): String {
        val lower = normalizedText.lowercase()
        for (bank in BankSmsPatternRegistry.banks) {
            if (bank.nameKeywords.any { lower.contains(it.lowercase()) }) {
                return bank.displayNameFa
            }
        }
        return "پیامک بانکی"
    }

    private fun inferMerchantAndCategory(
        normalized: String,
        bankName: String,
        txType: BankTransactionType,
        isRefund: Boolean = false,
        isFeeOnly: Boolean = false
    ): Pair<String, String> {
        val lower = normalized.lowercase()
        if (isRefund) {
            return "بازگشت وجه ($bankName)" to "سایر"
        }
        if (isFeeOnly) {
            return "کارمزد بانکی ($bankName)" to "سایر"
        }
        return when {
            lower.contains("اسنپ") || lower.contains("تپسی") || lower.contains("مترو") || lower.contains("تاکسی") ->
                "سفر شهری ($bankName)" to "حمل‌ونقل"
            lower.contains("پمپ بنزین") || lower.contains("سوخت") || lower.contains("جایگاه") ->
                "سوخت و خودرو ($bankName)" to "حمل‌ونقل"
            lower.contains("کافه") || lower.contains("رستوران") || lower.contains("غذا") || lower.contains("نانوایی") || lower.contains("اسنپ فود") ->
                "غذا و کافه ($bankName)" to "غذا و کافه"
            lower.contains("سوپرمارکت") || lower.contains("فروشگاه") || lower.contains("کوروش") || lower.contains("دیجی کالا") || lower.contains("دیجیکالا") || lower.contains("هایپر") ->
                "خرید فروشگاهی ($bankName)" to "خرید"
            lower.contains("داروخانه") || lower.contains("بیمارستان") || lower.contains("کلینیک") || lower.contains("پزشک") || lower.contains("آزمایشگاه") ->
                "درمان و دارو ($bankName)" to "سلامت و درمان"
            lower.contains("قبض") || lower.contains("همراه اول") || lower.contains("ایرانسل") || lower.contains("رایتل") || lower.contains("اینترنت") || lower.contains("برق") || lower.contains("گاز") ->
                "پرداخت قبض / شارژ ($bankName)" to "قبوض و اجاره"
            txType == BankTransactionType.TRANSFER || lower.contains("کارت به کارت") || lower.contains("پایا") || lower.contains("ساتنا") || lower.contains("پل") ->
                "انتقال وجه ($bankName)" to "انتقال حساب"
            txType == BankTransactionType.INCOME && (lower.contains("حقوق") || lower.contains("دستمزد")) ->
                "واریز حقوق ($bankName)" to "حقوق و دستمزد"
            txType == BankTransactionType.INCOME && lower.contains("سود") ->
                "واریز سود بانکی ($bankName)" to "سرمایه‌گذاری"
            txType == BankTransactionType.INCOME ->
                "واریز به حساب ($bankName)" to "سایر"
            else ->
                "تراکنش $bankName" to "سایر"
        }
    }
}
