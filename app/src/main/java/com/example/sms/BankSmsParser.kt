package com.example.sms

import com.example.util.PersianDateAndUnitUtils
import java.util.Locale

object BankSmsParser {

    /**
     * Primary parsing engine for Iranian bank transaction SMS messages.
     * Completely deterministic and on-device. Zero network or AI calls.
     */
    fun parse(
        rawSms: String,
        senderAddress: String = "",
        timestampMillis: Long = System.currentTimeMillis()
    ): BankSmsParseResult {
        val normalized = BankSmsNormalizer.normalize(rawSms)
        if (normalized.isBlank()) {
            return BankSmsParseResult(
                status = BankSmsParseStatus.UNRECOGNIZED,
                bankName = "نامشخص",
                bankCode = "UNKNOWN",
                transactionType = BankTransactionType.UNKNOWN,
                rawAmountDetected = 0.0,
                wasInRial = false,
                amountInToman = 0.0,
                timestampMillis = timestampMillis,
                confidence = 0.0,
                recognitionReasons = listOf("پیام خالی است")
            )
        }

        val lowerText = normalized.lowercase(Locale.ROOT)
        val lowerSender = senderAddress.lowercase(Locale.ROOT)

        // 1. Check for OTP / Security verification SMS
        val hasOtpKeyword = BankSmsPatternRegistry.OTP_KEYWORDS.any { lowerText.contains(it) || lowerSender.contains(it) }
        val hasExplicitTxStatement = lowerText.contains("برداشت") || lowerText.contains("واریز") || lowerText.contains("خرید")
        if (hasOtpKeyword && !hasExplicitTxStatement) {
            return BankSmsParseResult(
                status = BankSmsParseStatus.IGNORED_OTP,
                bankName = "سرویس رمز پویا",
                bankCode = "OTP",
                transactionType = BankTransactionType.UNKNOWN,
                rawAmountDetected = 0.0,
                wasInRial = false,
                amountInToman = 0.0,
                timestampMillis = timestampMillis,
                confidence = 1.0,
                recognitionReasons = listOf("پیامک رمز پویا یا کد تایید تشخیص داده شد و نادیده گرفته شد")
            )
        }

        // 2. Check for Failed Transactions
        if (BankSmsPatternRegistry.FAILED_KEYWORDS.any { lowerText.contains(it) }) {
            return BankSmsParseResult(
                status = BankSmsParseStatus.IGNORED_NON_FINANCIAL,
                bankName = "تراکنش ناموفق",
                bankCode = "FAILED_TX",
                transactionType = BankTransactionType.UNKNOWN,
                rawAmountDetected = 0.0,
                wasInRial = false,
                amountInToman = 0.0,
                timestampMillis = timestampMillis,
                confidence = 0.95,
                recognitionReasons = listOf("تراکنش ناموفق بوده و به موجودی اضافه یا کسر نمی‌شود")
            )
        }

        // 3. Check for Promotional / Advertisements
        val isPromotional = BankSmsPatternRegistry.PROMOTIONAL_KEYWORDS.any { lowerText.contains(it) }
        if (isPromotional && !hasExplicitTxStatement) {
            return BankSmsParseResult(
                status = BankSmsParseStatus.IGNORED_NON_FINANCIAL,
                bankName = "پیام تبلیغاتی",
                bankCode = "PROMO",
                transactionType = BankTransactionType.UNKNOWN,
                rawAmountDetected = 0.0,
                wasInRial = false,
                amountInToman = 0.0,
                timestampMillis = timestampMillis,
                confidence = 0.90,
                recognitionReasons = listOf("پیامک تبلیغاتی یا اطلاع‌رسانی فاقد تراکنش مالی است")
            )
        }

        // 4. Identify Bank from Registry
        val matchedBank = BankSmsPatternRegistry.findBank(senderAddress, normalized)
        val bankName = matchedBank?.bankName ?: "بانک عضو شتاب"
        val bankCode = matchedBank?.bankCode ?: "SHETAB"

        // 5. Extract Card or Account Mask
        val cardRegex = Regex("""(?:کارت|حساب|به کارت|از کارت|به حساب|از حساب|\*)\s*([0-9\*xX]{4,16})""")
        val cardMatch = cardRegex.find(normalized)
        val cardMask = cardMatch?.groupValues?.get(1)?.let {
            val digits = it.filter { ch -> ch.isDigit() }
            if (digits.length >= 4) digits.takeLast(4) else digits
        } ?: ""

        // 6. Extract Tracking/Reference Code
        val trackingRegex = Regex("""(?:پیگیری|مرجع|رهگیری|کد پیگیری|شماره ارجاع)\s*[:\s]*([0-9a-zA-Z]+)""")
        val trackingCode = trackingRegex.find(normalized)?.groupValues?.get(1) ?: ""

        // 7. Extract Balance (مانده / موجودی) carefully to avoid mixing with transaction amount
        val balanceRegex = Regex("""(?:مانده|موجودی)(?:\s+کل|\s+حساب|\s+کارت)?\s*[:\s]*([0-9,،\.]+)\s*(ریال|تومان)?""")
        val balanceMatch = balanceRegex.find(normalized)
        var balanceRaw: Double? = null
        var balanceWasInRial = true
        if (balanceMatch != null) {
            balanceRaw = balanceMatch.groupValues[1]
                .replace(",", "")
                .replace("،", "")
                .replace(".", "")
                .toDoubleOrNull()
            if (balanceMatch.groupValues[2].contains("تومان")) {
                balanceWasInRial = false
            }
        }

        // 8. Try Specific Bank Pattern Rules
        var detectedRawAmount: Double? = null
        var wasInRial = true
        var detectedTxType = BankTransactionType.UNKNOWN
        var matchedTemplateId = "GENERIC"
        var suggestedCat = "خرید"
        val reasons = mutableListOf<String>()

        if (matchedBank != null) {
            for (rule in matchedBank.patterns) {
                val match = rule.regex.find(normalized)
                if (match != null) {
                    val rawStr = match.groupValues[1]
                        .replace(",", "")
                        .replace("،", "")
                        .replace(".", "")
                    val parsedVal = rawStr.toDoubleOrNull()
                    if (parsedVal != null && parsedVal > 0.0) {
                        detectedRawAmount = parsedVal
                        detectedTxType = rule.transactionType
                        matchedTemplateId = rule.templateId
                        suggestedCat = rule.defaultCategory
                        wasInRial = if (match.groupValues.size > 2 && match.groupValues[2].contains("تومان")) {
                            false
                        } else {
                            rule.isRial
                        }
                        reasons.add("الگوی اختصاصی ${rule.templateId} منطبق شد")
                        break
                    }
                }
            }
        }

        // 9. Generic Fallback Parsing if specific rule didn't capture amount
        if (detectedRawAmount == null) {
            val isExplicitToman = lowerText.contains("تومان") || lowerText.contains("تومن")
            wasInRial = !isExplicitToman

            // Determine transaction direction
            val isWithdrawal = lowerText.contains("برداشت") || lowerText.contains("خرید") || lowerText.contains("کاهش") || lowerText.contains("پرداخت")
            val isDeposit = lowerText.contains("واریز") || lowerText.contains("افزایش") || lowerText.contains("بستانکار") || lowerText.contains("سود")
            val isTransfer = lowerText.contains("انتقال") || lowerText.contains("پایا") || lowerText.contains("ساتنا") || lowerText.contains("کارت به کارت")

            detectedTxType = when {
                isTransfer -> BankTransactionType.TRANSFER
                isDeposit && !isWithdrawal -> BankTransactionType.INCOME
                isWithdrawal && !isDeposit -> BankTransactionType.EXPENSE
                lowerText.contains("+") && !lowerText.contains("-") -> BankTransactionType.INCOME
                lowerText.contains("-") && !lowerText.contains("+") -> BankTransactionType.EXPENSE
                else -> BankTransactionType.UNKNOWN
            }

            // Extract candidate numbers, ensuring we do NOT pick the balance, tracking code, or card number
            val lines = normalized.lines().map { it.trim() }.filter { it.isNotEmpty() }
            for (line in lines) {
                val lineLower = line.lowercase(Locale.ROOT)
                val isBalanceLine = lineLower.contains("مانده") || lineLower.contains("موجودی")
                val isAmountLine = lineLower.contains("مبلغ") || lineLower.contains("برداشت") || lineLower.contains("واریز") || lineLower.contains("خرید")

                // Filter out card and tracking strings
                val cleanLine = line
                    .replace(Regex("""\d{4}[*×xX]+\d{4}"""), " ")
                    .replace(Regex("""\d{2,4}/\d{1,2}/\d{1,2}"""), " ")
                    .replace(Regex("""\d{1,2}:\d{2}(:\d{2})?"""), " ")

                val nums = BankSmsNormalizer.parseFinancialNumbers(cleanLine)
                if (nums.isNotEmpty()) {
                    if (isAmountLine) {
                        detectedRawAmount = nums.first()
                        reasons.add("مبلغ از سطر اختصاصی تراکنش استخراج شد")
                        break
                    } else if (!isBalanceLine && detectedRawAmount == null) {
                        detectedRawAmount = nums.first()
                    }
                }
            }

            if (detectedRawAmount == null) {
                val allNums = BankSmsNormalizer.parseFinancialNumbers(
                    normalized
                        .replace(Regex("""\d{4}[*×xX]+\d{4}"""), " ")
                        .replace(Regex("""\d{2,4}/\d{1,2}/\d{1,2}"""), " ")
                        .replace(Regex("""\d{1,2}:\d{2}(:\d{2})?"""), " ")
                )
                if (allNums.isNotEmpty()) {
                    // If balance was found, remove it from candidate numbers
                    val filtered = if (balanceRaw != null) allNums.filter { it != balanceRaw } else allNums
                    detectedRawAmount = filtered.firstOrNull()
                }
            }
        }

        // 10. Check if this was purely a Balance Inquiry (no actual transaction)
        if (detectedRawAmount == null && balanceRaw != null) {
            return BankSmsParseResult(
                status = BankSmsParseStatus.IGNORED_NON_FINANCIAL,
                bankName = bankName,
                bankCode = bankCode,
                transactionType = BankTransactionType.UNKNOWN,
                rawAmountDetected = 0.0,
                wasInRial = balanceWasInRial,
                amountInToman = 0.0,
                balanceInToman = if (balanceWasInRial) (balanceRaw / 10.0) else balanceRaw,
                cardOrAccountMask = cardMask,
                timestampMillis = timestampMillis,
                confidence = 0.90,
                recognitionReasons = listOf("پیامک صرفاً اعلام موجودی حساب است و تراکنش جدیدی ندارد")
            )
        }

        if (detectedRawAmount == null || detectedRawAmount <= 0.0) {
            return BankSmsParseResult(
                status = BankSmsParseStatus.UNRECOGNIZED,
                bankName = bankName,
                bankCode = bankCode,
                transactionType = detectedTxType,
                rawAmountDetected = 0.0,
                wasInRial = wasInRial,
                amountInToman = 0.0,
                timestampMillis = timestampMillis,
                confidence = 0.0,
                recognitionReasons = listOf("هیچ مبلغ معتبری در پیامک شناسایی نشد")
            )
        }

        // Calculate exact Toman amount
        val amountToman = PersianDateAndUnitUtils.roundFinancialAmount(
            if (wasInRial) (detectedRawAmount / 10.0) else detectedRawAmount
        )
        val finalBalanceToman = balanceRaw?.let {
            PersianDateAndUnitUtils.roundFinancialAmount(if (balanceWasInRial) (it / 10.0) else it)
        }

        val suggestedTitle = when (detectedTxType) {
            BankTransactionType.INCOME -> "واریز به $bankName"
            BankTransactionType.EXPENSE -> "خرید / برداشت $bankName"
            BankTransactionType.TRANSFER -> "انتقال وجه $bankName"
            BankTransactionType.REFUND -> "برگشت وجه به $bankName"
            BankTransactionType.FEE -> "کارمزد بانکی $bankName"
            BankTransactionType.UNKNOWN -> "تراکنش بانکی $bankName"
        }

        val confidence = when {
            matchedTemplateId != "GENERIC" && detectedTxType != BankTransactionType.UNKNOWN -> 0.98
            detectedTxType != BankTransactionType.UNKNOWN -> 0.85
            else -> 0.50
        }

        val status = if (confidence >= 0.80 && detectedTxType != BankTransactionType.UNKNOWN) {
            BankSmsParseStatus.SUCCESS
        } else {
            BankSmsParseStatus.NEEDS_REVIEW
        }

        // Generate stable idempotency fingerprint
        val fingerprint = BankSmsParseResult.computeFingerprint(
            bankCode = bankCode,
            transactionType = detectedTxType,
            amountToman = amountToman,
            cardOrAccountMask = cardMask,
            trackingCode = trackingCode,
            approxDateOrTime = (timestampMillis / (1000 * 60 * 60)).toString() // 1-hour window for matching SMS
        )

        return BankSmsParseResult(
            status = status,
            bankName = bankName,
            bankCode = bankCode,
            transactionType = detectedTxType,
            rawAmountDetected = detectedRawAmount,
            wasInRial = wasInRial,
            amountInToman = amountToman,
            balanceInToman = finalBalanceToman,
            cardOrAccountMask = cardMask,
            trackingCode = trackingCode,
            suggestedTitle = suggestedTitle,
            suggestedCategory = suggestedCat,
            timestampMillis = timestampMillis,
            confidence = confidence,
            recognitionReasons = reasons,
            fingerprint = fingerprint
        )
    }
}
