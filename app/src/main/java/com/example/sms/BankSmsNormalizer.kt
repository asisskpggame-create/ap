package com.example.sms

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Normalizes raw Iranian bank SMS texts:
 * - Unifies Persian (۰-۹) and Arabic (٠-٩) digits to Latin (0-9).
 * - Unifies Arabic Yeh/Kaf/He/Waw variants (ي، ئ، ى -> ی | ك -> ک | ة -> ه).
 * - Normalizes ZWNJ (نیم‌فاصله), non-breaking spaces, directional marks, and Arabic diacritics.
 * - Normalizes thousands separators (٬, ،, ') vs decimal points (.) without destroying decimals.
 * - Excludes dates, times, card numbers, account numbers, and reference/tracking codes from financial amounts.
 */
object BankSmsNormalizer {

    private val datePattern = Regex("""\b\d{2,4}/\d{1,2}/\d{1,2}\b|\b\d{4}-\d{1,2}-\d{1,2}\b""")
    private val timePattern = Regex("""\b\d{1,2}:\d{2}(?::\d{2})?\b""")
    private val cardMaskPattern = Regex("""\b\d{4,6}[\*\-xX×]+\d{2,4}\b|\b\d{16}\b""")
    private val ibanPattern = Regex("""\b(?i)IR\d{10,24}\b""")
    private val multiSegmentAccountPattern = Regex("""\b\d{1,5}[\.\-]\d{2,8}[\.\-]\d{1,8}(?:[\.\-]\d{1,6})*\b""")
    private val nonAmountLabeledPattern = Regex(
        """(?:کارت|حساب|شبا|پیگیری|مرجع|سند|ترمینال|پذیرنده|شماره|کد|رمز(?:\s*پویا)?|اعتبار|ثانیه)\s*(?:[:：\-]|شماره)?\s*[0-9\*\-\./]+"""
    )

    private val slashThousandsRegex = Regex("""^\d{1,3}(?:/\d{3})+$""")
    private val multiDotThousandsRegex = Regex("""^\d{1,3}(?:\.\d{3}){2,}$""")
    private val standardNumberRegex = Regex("""^[+-]?\d+(?:\.\d+)?$""")

    fun normalize(rawText: String): String {
        if (rawText.isBlank()) return ""
        val sb = StringBuilder(rawText.length)
        for (ch in rawText) {
            when (ch) {
                in '۰'..'۹' -> sb.append((ch.code - '۰'.code + '0'.code).toChar())
                in '٠'..'٩' -> sb.append((ch.code - '٠'.code + '0'.code).toChar())
                'ي', 'ئ', 'ى' -> sb.append('ی')
                'ك' -> sb.append('ک')
                'ة' -> sb.append('ه')
                'ؤ' -> sb.append('و')
                'إ', 'أ' -> sb.append('ا')
                '٬', '،', '’', '\'' -> sb.append(',')
                '٫' -> sb.append('.')
                '：' -> sb.append(':')
                '−', '–', '—' -> sb.append('-')
                '\u200C', '\u00A0', '\u202F', '\u2007' -> sb.append(' ')
                '\u0640', // Arabic Tatweel (Kashida)
                '\u200B', '\u200E', '\u200F', '\u202A', '\u202B', '\u202C', '\u202D', '\u202E', '\uFEFF' -> {
                    // Strip invisible directional marks and tatweel
                }
                in '\u064B'..'\u065F', '\u0670' -> {
                    // Strip Arabic harakat/diacritics
                }
                else -> sb.append(ch)
            }
        }
        return sb.toString()
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .lines()
            .joinToString("\n") { line ->
                line.replace(Regex("""[ \t]+"""), " ").trim()
            }
            .trim()
    }

    /**
     * Parses a single normalized numeric token into an exact [BigDecimal], distinguishing:
     * - Standard comma thousands separators: `2,500,000` -> `2500000`, `1,250.50` -> `1250.50`
     * - Slash thousands separators (3-digit groups only): `2/500/000` -> `2500000`
     * - Multi-dot thousands separators: `2.500.000` -> `2500000`
     * - Decimal points: `1250.50` -> `1250.50` (NEVER strips true decimal points!)
     */
    fun parseDecimalAmount(rawToken: String): BigDecimal? {
        val normalizedToken = normalize(rawToken)
            .trim()
            .trimEnd(',', '.', ':', ';')
            .trimStart('+')
        if (normalizedToken.isBlank()) return null

        val cleaned = when {
            slashThousandsRegex.matches(normalizedToken) -> normalizedToken.replace("/", "")
            multiDotThousandsRegex.matches(normalizedToken) -> normalizedToken.replace(".", "")
            normalizedToken.contains("/") -> return null // Date or account fraction, not a valid amount
            else -> normalizedToken.replace(",", "")
        }

        if (!standardNumberRegex.matches(cleaned)) return null
        return try {
            BigDecimal(cleaned)
        } catch (_: NumberFormatException) {
            null
        }
    }

    /**
     * Converts Rial to Toman using exact [BigDecimal] arithmetic (`1 Toman = 10 Rials`),
     * rounded to at most 2 decimal places with [RoundingMode.HALF_UP].
     */
    fun rialToTomanBigDecimal(rialAmount: BigDecimal): BigDecimal {
        return rialAmount
            .divide(BigDecimal.TEN, 2, RoundingMode.HALF_UP)
            .stripTrailingZeros()
    }

    /**
     * Rounds a [BigDecimal] financial value to 2 decimal places using [RoundingMode.HALF_UP].
     */
    fun roundCurrencyBigDecimal(amount: BigDecimal): Double {
        return amount.setScale(2, RoundingMode.HALF_UP).toDouble()
    }

    /**
     * Masks sensitive sequences (card numbers, IBANs, full account numbers) before any controlled logging.
     */
    fun maskSensitiveSmsContent(text: String): String {
        if (text.isBlank()) return ""
        val norm = normalize(text)
        return norm
            .replace(Regex("""\b(\d{4})\d{8}(\d{4})\b"""), "$1****$2")
            .replace(ibanPattern, "IR**********************")
            .replace(Regex("""(?:رمز(?:\s*پویا)?|کد|پین|OTP|CVV2)\s*[:：\-]?\s*\d{4,8}""", RegexOption.IGNORE_CASE), "[REDACTED_CODE]")
    }

    /**
     * Strips dates, times, card numbers, account numbers, and labeled reference/tracking numbers
     * so they cannot be mistaken for monetary amounts.
     */
    fun stripNonMonetaryTokens(normalizedText: String): String {
        return normalizedText
            .replace(ibanPattern, " ")
            .replace(cardMaskPattern, " ")
            .replace(datePattern, " ")
            .replace(timePattern, " ")
            .replace(multiSegmentAccountPattern, " ")
            .replace(nonAmountLabeledPattern, " ")
    }

    /**
     * Extracts candidate financial numbers from normalized text, preserving decimal precision
     * and ignoring dates, times, card numbers, and reference codes.
     */
    fun parseFinancialNumbers(normalizedText: String): List<Double> {
        if (normalizedText.isBlank()) return emptyList()
        val scrubbed = stripNonMonetaryTokens(normalizedText)
        val tokenRegex = Regex("""(?<![\d/\.\*])(\d{1,3}(?:[,/]\d{3})+(?:\.\d{1,2})?|\d{1,3}(?:\.\d{3}){2,}|\d+(?:\.\d{1,2})?)(?![\d/\*])""")
        return tokenRegex.findAll(scrubbed).mapNotNull { match ->
            val bd = parseDecimalAmount(match.groupValues[1])
            if (bd != null && bd > BigDecimal.ZERO && bd <= BigDecimal("999999999999999")) {
                roundCurrencyBigDecimal(bd)
            } else {
                null
            }
        }.toList()
    }

    fun parseFinancialAmountBigDecimal(rawToken: String): BigDecimal? = parseDecimalAmount(rawToken)

    /**
     * Computes a deterministic SHA-256 idempotency fingerprint from the normalized SMS body and sender.
     */
    fun computeFingerprint(rawText: String, senderNumber: String = ""): String {
        val normBody = normalize(rawText).lowercase()
        val normSender = normalize(senderNumber).lowercase().trim()
        val canonical = "$normSender|$normBody"
        return try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(canonical.toByteArray(Charsets.UTF_8))
            "sms_" + hashBytes.take(16).joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            "sms_${canonical.hashCode()}"
        }
    }
}
