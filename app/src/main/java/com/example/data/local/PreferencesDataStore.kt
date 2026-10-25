package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.UserPreferencesDoc
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "personal_life_os_prefs")

class PreferencesDataStore(private val context: Context) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val WEEK_START_DAY = intPreferencesKey("week_start_day")
        val CALENDAR_DISPLAY = stringPreferencesKey("calendar_display")
        val DEFAULT_CURRENCY = stringPreferencesKey("default_currency")
        val DAILY_WATER_TARGET_ML = intPreferencesKey("daily_water_target_ml")
        val DAILY_SLEEP_TARGET_MIN = intPreferencesKey("daily_sleep_target_min")
        val DAILY_EXERCISE_TARGET_MIN = intPreferencesKey("daily_exercise_target_min")
        val RETAIN_VOICE_TRANSCRIPTS = booleanPreferencesKey("retain_voice_transcripts")
        val RETAIN_CONVERSATIONS = booleanPreferencesKey("retain_conversations")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val LAST_SIX_HOUR_SYNC_MILLIS = longPreferencesKey("last_six_hour_sync_millis")
        val MONTHLY_BUDGET_TARGET = doublePreferencesKey("monthly_budget_target")
        val VAULT_LOCK_ENABLED = booleanPreferencesKey("vault_lock_enabled")
        val VAULT_PIN_CODE = stringPreferencesKey("vault_pin_code")
        val THEME_PALETTE = stringPreferencesKey("theme_palette")
        val LAYOUT_STYLE = stringPreferencesKey("layout_style")
        val DASHBOARD_CARD_ORDER = stringPreferencesKey("dashboard_card_order")
    }

    fun observeLocalPreferences(userId: String): Flow<UserPreferencesDoc> {
        return context.applicationContext.dataStore.data.map { prefs ->
            val orderStr = prefs[Keys.DASHBOARD_CARD_ORDER]
            val cardOrder = if (!orderStr.isNullOrBlank()) {
                orderStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            } else {
                listOf("QUICK_BAR", "FINANCE", "GOALS", "HABITS", "WELLNESS", "CALENDAR")
            }
            UserPreferencesDoc(
                id = "default",
                userId = userId,
                language = prefs[Keys.LANGUAGE] ?: "fa",
                darkTheme = prefs[Keys.DARK_THEME] ?: true,
                weekStartDay = prefs[Keys.WEEK_START_DAY] ?: 6,
                calendarDisplay = prefs[Keys.CALENDAR_DISPLAY] ?: "JALALI",
                defaultCurrency = prefs[Keys.DEFAULT_CURRENCY] ?: "TOMAN",
                dailyWaterTargetMl = prefs[Keys.DAILY_WATER_TARGET_ML] ?: 2500,
                dailySleepTargetMinutes = prefs[Keys.DAILY_SLEEP_TARGET_MIN] ?: 480,
                dailyExerciseTargetMinutes = prefs[Keys.DAILY_EXERCISE_TARGET_MIN] ?: 30,
                retainVoiceTranscripts = prefs[Keys.RETAIN_VOICE_TRANSCRIPTS] ?: true,
                retainConversations = prefs[Keys.RETAIN_CONVERSATIONS] ?: true,
                notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
                onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
                monthlyBudgetTarget = prefs[Keys.MONTHLY_BUDGET_TARGET] ?: 15000000.0,
                vaultLockEnabled = prefs[Keys.VAULT_LOCK_ENABLED] ?: false,
                vaultPinCode = prefs[Keys.VAULT_PIN_CODE] ?: "",
                themePalette = prefs[Keys.THEME_PALETTE] ?: "OBSIDIAN_TEAL",
                layoutStyle = prefs[Keys.LAYOUT_STYLE] ?: "BENTO_GRID",
                dashboardCardOrder = cardOrder
            )
        }
    }

    fun observeLastSyncMillis(): Flow<Long> {
        return context.applicationContext.dataStore.data.map { prefs ->
            prefs[Keys.LAST_SIX_HOUR_SYNC_MILLIS] ?: 0L
        }
    }

    suspend fun saveLastSyncMillis(timestampMillis: Long) {
        context.applicationContext.dataStore.edit { prefs ->
            prefs[Keys.LAST_SIX_HOUR_SYNC_MILLIS] = timestampMillis
        }
    }

    suspend fun saveLocalPreferences(doc: UserPreferencesDoc) {
        context.applicationContext.dataStore.edit { prefs ->
            prefs[Keys.LANGUAGE] = doc.language
            prefs[Keys.DARK_THEME] = doc.darkTheme
            prefs[Keys.WEEK_START_DAY] = doc.weekStartDay
            prefs[Keys.CALENDAR_DISPLAY] = doc.calendarDisplay
            prefs[Keys.DEFAULT_CURRENCY] = doc.defaultCurrency
            prefs[Keys.DAILY_WATER_TARGET_ML] = doc.dailyWaterTargetMl
            prefs[Keys.DAILY_SLEEP_TARGET_MIN] = doc.dailySleepTargetMinutes
            prefs[Keys.DAILY_EXERCISE_TARGET_MIN] = doc.dailyExerciseTargetMinutes
            prefs[Keys.RETAIN_VOICE_TRANSCRIPTS] = doc.retainVoiceTranscripts
            prefs[Keys.RETAIN_CONVERSATIONS] = doc.retainConversations
            prefs[Keys.NOTIFICATIONS_ENABLED] = doc.notificationsEnabled
            prefs[Keys.ONBOARDING_COMPLETED] = doc.onboardingCompleted
            prefs[Keys.MONTHLY_BUDGET_TARGET] = doc.monthlyBudgetTarget
            prefs[Keys.VAULT_LOCK_ENABLED] = doc.vaultLockEnabled
            prefs[Keys.VAULT_PIN_CODE] = doc.vaultPinCode
            prefs[Keys.THEME_PALETTE] = doc.themePalette
            prefs[Keys.LAYOUT_STYLE] = doc.layoutStyle
            prefs[Keys.DASHBOARD_CARD_ORDER] = doc.dashboardCardOrder.joinToString(",")
        }
    }
}
