package com.example.sms

import java.util.Locale

/**
 * Normalizes Iranian bank SMS texts into a standardized representation.
 * - Unifies Persian (۰-۹) and Arabic (٠-٩) numerals into standard ASCII (0-9).
 * - Unifies Arabic Kaf (ك) and Yeh (ي, ى) into standard Persian (ک, ی).
 * - Strips zero-width non-joiners (ZWNJ \u200C) and invisible control characters.
 * - Normalizes Persian comma (،) and standard comma (,) and dot separators in thousands.
 * - Preserves line structures for context-aware multi-line parsing.
 */
object BankSmsNormalizer {

    private val PERSIAN_DIGITS = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    private val ARABIC_DIGITS = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

    fun normalize(rawText: String): String {
        if (rawText.isBlank()) return ""

        val sb = StringBuilder(rawText.length)
        for (ch in rawText) {
            when (ch) {
                in '۰'..'۹' -> sb.append(ch - '۰')
                in '٠'..'٩' -> sb.append(ch - '٠')
                'ي', 'ى' -> sb.append('ی')
                'ك' -> sb.append('ک')
                'ة' -> sb.append('ه')
                '،' -> sb.append(',')
                '\u200C', '\u200D', '\u200E', '\u200F', '\uFEFF' -> sb.append(' ') // Replace ZWNJ and directional marks with single space
                '\t' -> sb.append(' ')
                else -> sb.append(ch)
            }
        }

        // Clean up multiple spaces per line while keeping line breaks intact
        return sb.toString().lines().joinToString("\n") { line ->
            line.trim().replace(Regex("""\s+"""), " ")
        }.trim()
    }

    /**
     * Extracts all raw numeric values that resemble financial amounts or balances.
     * Handles comma/dot separated thousands like 2,500,000 or 2500000 or ۲،۵۰۰،۰۰۰.
     */
    fun parseFinancialNumbers(text: String): List<Double> {
        val norm = normalize(text)
        // Match numbers with thousands separators or raw 4+ digit numbers
        val regex = Regex("""(?<![0-9/:-])([0-9]{1,3}(?:[,،\.][0-9]{3})+|[0-9]{4,14})(?![0-9/:-])""")
        return regex.findAll(norm).mapNotNull { match ->
            val clean = match.groupValues[1]
                .replace(",", "")
                .replace("،", "")
                .replace(".", "")
            clean.toDoubleOrNull()
        }.filter { it >= 100.0 }.toList()
    }
}
