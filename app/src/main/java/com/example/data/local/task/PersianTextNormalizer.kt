package com.example.data.local.task

import java.util.Locale

/**
 * Utility for normalizing Persian and Arabic text in database queries and indexes.
 *
 * Persian Language Search Considerations in SQLite/Room:
 * 1. Letter variants: Arabic 'ي' (\u064A) and 'ك' (\u0643) vs Persian 'ی' (\u06CC) and 'ک' (\u06A9)
 * 2. Zero-Width Non-Joiner (ZWNJ / نیم‌فاصله \u200C): e.g. "می‌روم" vs "میروم" or "کتاب‌ها" vs "کتابها"
 * 3. Diacritics (Harakat / اعراب): فتحه، ضمه، کسره، تنوین، تشدید (\u064B..\u0652)
 * 4. Tatweel / Kashida ('ـ' \u0640)
 * 5. Alef variants ('آ', 'أ', 'إ', 'ٱ' -> 'ا')
 * 6. Persian/Arabic numerals ('۰'..'۹' and '٠'..'٩' -> '0'..'9')
 */
object PersianTextNormalizer {

    private val PERSIAN_ARABIC_DIGIT_MAP = mapOf(
        '۰' to '0', '۱' to '1', '۲' to '2', '۳' to '3', '۴' to '4',
        '۵' to '5', '۶' to '6', '۷' to '7', '۸' to '8', '۹' to '9',
        '٠' to '0', '١' to '1', '٢' to '2', '٣' to '3', '٤' to '4',
        '٥' to '5', '٦' to '6', '٧' to '7', '٨' to '8', '٩' to '9'
    )

    /**
     * Normalizes a text string for uniform, error-free Persian database searching.
     */
    fun normalizeForSearch(input: String?, replaceZwnjWithSpace: Boolean = false): String {
        if (input.isNullOrBlank()) return ""

        val sb = StringBuilder(input.length)
        for (char in input) {
            when (char) {
                // Unify Yeh variants to standard Persian Yeh
                '\u064A', '\u0649', '\u06D2' -> sb.append('\u06CC') // ي, ى, ے -> ی

                // Unify Kaf variants to standard Persian Keheh
                '\u0643' -> sb.append('\u06A9') // ك -> ک

                // Unify Alef variants
                '\u0622', '\u0623', '\u0625', '\u0671' -> sb.append('\u0627') // آ, أ, إ, ٱ -> ا

                // Unify Heh / Teh Marbuta
                '\u0629', '\u06C0' -> sb.append('\u0647') // ة, ۀ -> ه

                // Zero-Width Non-Joiner (نیم‌فاصله)
                '\u200C', '\u200B', '\u200D', '\uFEFF' -> {
                    if (replaceZwnjWithSpace) sb.append(' ')
                }

                // Remove Tatweel / Kashida
                '\u0640' -> { /* omit */ }

                // Remove Harakat (Arabic/Persian diacritics)
                in '\u064B'..'\u065F', '\u0670' -> { /* omit */ }

                // Map Persian / Arabic numerals to ASCII digits
                in PERSIAN_ARABIC_DIGIT_MAP -> sb.append(PERSIAN_ARABIC_DIGIT_MAP[char])

                else -> sb.append(char)
            }
        }

        return sb.toString()
            .trim()
            .lowercase(Locale.ROOT)
            .replace("\\s+".toRegex(), " ")
    }

    /**
     * Strips all spaces and ZWNJ to allow continuous phrase matching regardless of spacing.
     */
    fun normalizeNoSpace(input: String?): String {
        return normalizeForSearch(input, replaceZwnjWithSpace = false).replace(" ", "")
    }

    /**
     * Builds a comprehensive Persian search index containing spaced, joined, and continuous tokens.
     */
    fun buildSearchIndex(vararg fields: String?): String {
        val raw = fields.filterNotNull().joinToString(" ")
        val spaced = normalizeForSearch(raw, replaceZwnjWithSpace = true)
        val joined = normalizeForSearch(raw, replaceZwnjWithSpace = false)
        val continuous = normalizeNoSpace(raw)
        return "$spaced $joined $continuous"
    }
}

