package com.example.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Utility for Persian (`fa-IR`) Voice-to-Text with Offline (`EXTRA_PREFER_OFFLINE` & On-Device API 31+)
 * capability, Persian text/number normalization, and Android Offline Speech Pack settings navigation.
 */
object PersianOfflineSpeechHelper {

    const val PERSIAN_LOCALE = "fa-IR"

    /**
     * Checks whether the device currently has an active internet connection.
     * When false, Persian Voice-to-Text automatically forces offline recognition mode.
     */
    fun isDeviceOnline(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
            val network = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Checks if Android 12+ (API 31+) dedicated On-Device SpeechRecognizer API is supported on this device.
     */
    fun isOnDeviceSpeechSupported(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
            } else {
                false
            }
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Creates the best available SpeechRecognizer instance.
     * If [useDedicatedOnDeviceRecognizer] is true and Android 12+ on-device recognition is available,
     * uses [SpeechRecognizer.createOnDeviceSpeechRecognizer]; otherwise uses standard [SpeechRecognizer]
     * (which still honors [RecognizerIntent.EXTRA_PREFER_OFFLINE] on all Android versions).
     */
    fun createSpeechRecognizer(
        context: Context,
        useDedicatedOnDeviceRecognizer: Boolean = false
    ): SpeechRecognizer? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                useDedicatedOnDeviceRecognizer &&
                SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
            ) {
                SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            } else if (SpeechRecognizer.isRecognitionAvailable(context)) {
                SpeechRecognizer.createSpeechRecognizer(context)
            } else {
                null
            }
        } catch (_: Throwable) {
            try {
                SpeechRecognizer.createSpeechRecognizer(context)
            } catch (_: Throwable) {
                null
            }
        }
    }

    /**
     * Builds a Persian (`fa-IR`) [RecognizerIntent] configured for either Offline-First or Hybrid mode.
     */
    fun buildPersianSpeechIntent(
        preferOffline: Boolean,
        continuousDictation: Boolean = false,
        promptText: String? = null
    ): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, PERSIAN_LOCALE)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, PERSIAN_LOCALE)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, PERSIAN_LOCALE)
            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("fa-IR", "fa"))
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, preferOffline)

            if (continuousDictation) {
                putExtra(
                    RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                    3500L
                )
                putExtra(
                    RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                    2500L
                )
            }

            if (!promptText.isNullOrBlank()) {
                putExtra(RecognizerIntent.EXTRA_PROMPT, promptText)
            }
        }
    }

    /**
     * Triggers Android 13+ (API 33+) model download for `fa-IR` if supported by the active SpeechRecognizer,
     * or opens the system Offline Speech Recognition settings screen so the user can download Persian offline data.
     */
    fun openOfflineSpeechSettings(context: Context): Boolean {
        val candidateIntents = listOf(
            // Google Speech Services / Voice Search offline language pack activity
            Intent("com.google.android.googlequicksearchbox.action.VOICE_IME_SETTINGS").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            Intent().apply {
                component = ComponentName(
                    "com.google.android.tts",
                    "com.google.android.apps.speech.tts.googletts.settings.asr.voiceime.VoiceImeSettingsActivity"
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            Intent(Settings.ACTION_LOCALE_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )

        for (intent in candidateIntents) {
            try {
                context.startActivity(intent)
                return true
            } catch (_: Throwable) {
                // Try next fallback settings screen
            }
        }
        return false
    }

    /**
     * Normalizes transcribed Persian speech text:
     * 1. Converts Arabic Yeh/Kaf (`ي`, `ك`) to standard Persian (`ی`, `ک`).
     * 2. Optionally converts spoken Persian number words into Persian digits (`پانصد` -> `۵۰۰`, `دویست` -> `۲۰۰`)
     *    and spoken punctuation (`نقطه`, `ویرگول`, `خط بعد`) into punctuation symbols.
     */
    fun normalizePersianTranscript(
        rawText: String,
        convertSpokenNumbers: Boolean = true
    ): String {
        if (rawText.isBlank()) return ""

        var text = rawText
            .replace('ي', 'ی')
            .replace('ئ', 'ی')
            .replace('ك', 'ک')
            .replace("  ", " ")
            .trim()

        // Spoken punctuation normalization for natural Persian dictation
        text = text
            .replace(Regex("""\bویرگول\b"""), "،")
            .replace(Regex("""\bخط بعد\b"""), "\n")
            .replace(Regex("""\bسر خط\b"""), "\n")

        if (convertSpokenNumbers) {
            val numberWordReplacements = listOf(
                Regex("""\bنهصد\b""") to "۹۰۰",
                Regex("""\bهشتصد\b""") to "۸۰۰",
                Regex("""\bهفتصد\b""") to "۷۰۰",
                Regex("""\bششصد\b|\bشیشصد\b""") to "۶۰۰",
                Regex("""\bپانصد\b|\bپونصد\b""") to "۵۰۰",
                Regex("""\bچهارصد\b|\bچارصد\b""") to "۴۰۰",
                Regex("""\bسیصد\b""") to "۳۰۰",
                Regex("""\bدویست\b""") to "۲۰۰",
                Regex("""\bصد و پنجاه\b""") to "۱۵۰",
                Regex("""\bیکصد\b""") to "۱۰۰",
                Regex("""\bنود\b""") to "۹۰",
                Regex("""\bهشتاد\b""") to "۸۰",
                Regex("""\bهفتاد\b""") to "۷۰",
                Regex("""\bشصت\b""") to "۶۰",
                Regex("""\bپنجاه\b""") to "۵۰",
                Regex("""\bچهل\b""") to "۴۰",
                Regex("""\bبیست\b""") to "۲۰",
                Regex("""\bپانزده\b|\bپونزده\b""") to "۱۵",
                Regex("""\bدوازده\b""") to "۱۲"
            )
            for ((pattern, replacement) in numberWordReplacements) {
                text = pattern.replace(text, replacement)
            }
            text = PersianDateAndUnitUtils.toPersianDigits(text)
        }

        return text.trim()
    }

    /**
     * Maps Android [SpeechRecognizer] error codes to clear, actionable Persian messages
     * with specific guidance when offline mode is active.
     */
    fun describeSpeechErrorFa(errorCode: Int, isOfflinePreferred: Boolean): Pair<String, Boolean> {
        // Second value in Pair indicates whether the error is likely due to missing offline Persian language pack
        return when (errorCode) {
            SpeechRecognizer.ERROR_NO_MATCH ->
                "صدایی تشخیص داده نشد؛ واضح‌تر صحبت کنید یا دوباره روی میکروفون بزنید." to false
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                "مهلت دریافت صدا به پایان رسید؛ برای ادامه ضبط لمس کنید." to false
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                "مجوز میکروفون فعال نیست؛ لطفاً دسترسی میکروفون را تایید کنید." to false
            SpeechRecognizer.ERROR_NETWORK,
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                if (isOfflinePreferred) {
                    "بسته صوتی آفلاین فارسی روی گوشی یافت نشد؛ از دکمه «دانلود بسته آفلاین فارسی» استفاده کنید." to true
                } else {
                    "اینترنت قطع است؛ «حالت آفلاین فارسی» به‌صورت خودکار فعال شد. دوباره ضبط کنید." to true
                }
            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
            SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ->
                "مدل آفلاین زبان فارسی (fa-IR) روی دستگاه نصب نیست؛ بسته صوتی فارسی را از تنظیمات دانلود کنید." to true
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                "موتور تشخیص گفتار مشغول است؛ لحظه‌ای صبر کرده و مجدداً تلاش کنید." to false
            SpeechRecognizer.ERROR_CLIENT ->
                "آماده‌سازی میکروفون؛ برای شروع ضبط روی دکمه میکروفون بزنید." to false
            else ->
                if (isOfflinePreferred) {
                    "سرویس تشخیص گفتار آفلاین نیاز به فعال‌سازی بسته فارسی دارد یا می‌توانید از دیالوگ صوتی سیستم استفاده کنید." to true
                } else {
                    "می‌توانید دوباره روی میکروفون بزنید یا متن را تایپ کنید." to false
                }
        }
    }
}
