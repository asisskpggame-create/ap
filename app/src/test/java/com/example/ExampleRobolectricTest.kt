package com.example

import android.content.Context
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.util.WorldClassOsUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context and verify encrypted backup roundtrip`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Personal Life OS", appName)

    val sampleJson = """{"exportVersion":2,"tasks":[{"title":"تست بکاپ رمزنگاری‌شده"}]}"""
    val encrypted = WorldClassOsUtils.EncryptedBackupManager.encryptPayload(sampleJson)
    assertTrue(encrypted.contains("PERSONAL_LIFE_OS_AES256_GCM_V1"))
    val decrypted = WorldClassOsUtils.EncryptedBackupManager.decryptPayload(encrypted)
    assertEquals(sampleJson, decrypted)
  }

  @Test
  fun `verify colloquial Toman voice parsing and Iranian bank SMS parser`() {
    val parsedVoice1 = com.example.ai.GeminiAssistantService.parsePersianCommandDeterministic(
      rawText = "۵۰۰ تومان سیگار خریدم",
      todayEpochDay = 20000L,
      defaultCurrency = "TOMAN"
    )
    assertEquals(1, parsedVoice1.operations.size)
    assertEquals("AddExpense", parsedVoice1.operations[0].operationType)
    assertEquals(500_000.0, parsedVoice1.operations[0].amount, 0.01)
    assertEquals("TOMAN", parsedVoice1.operations[0].currency)
    assertEquals("خرید سیگار", parsedVoice1.operations[0].title)

    val parsedVoiceWords = com.example.ai.GeminiAssistantService.parsePersianCommandDeterministic(
      rawText = "پانصد تومن سیگار خریدم",
      todayEpochDay = 20000L,
      defaultCurrency = "TOMAN"
    )
    assertEquals(500_000.0, parsedVoiceWords.operations[0].amount, 0.01)
    assertEquals("خرید سیگار", parsedVoiceWords.operations[0].title)

    val parsedEuro = com.example.ai.GeminiAssistantService.parsePersianCommandDeterministic(
      rawText = "یه قهوه ۳ یورویی خریدم",
      todayEpochDay = 20000L,
      defaultCurrency = "TOMAN"
    )
    assertEquals(3.0, parsedEuro.operations[0].amount, 0.01)
    assertEquals("EUR", parsedEuro.operations[0].currency)

    val smsParsed = com.example.ui.components.IranianBankSmsParser.parseBankSms(
      "بانک ملت\nخرید با کارت\nمبلغ: 5,000,000 ریال\nمانده: 42,000,000 ریال"
    )
    assertEquals(500_000.0, smsParsed?.amountInToman ?: 0.0, 0.01)
    assertEquals("بانک ملت", smsParsed?.bankName)
  }

  @get:org.junit.Rule
  val composeTestRule = androidx.compose.ui.test.junit4.createAndroidComposeRule<MainActivity>()

  @Test
  fun `launch MainActivity and navigate all screens without crash`() {
    composeTestRule.waitForIdle()
    // Dismiss onboarding if visible
    try {
      composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("complete_onboarding_button")).performClick()
      composeTestRule.waitForIdle()
    } catch (_: Throwable) {}

    // Navigate bottom tabs
    listOf(
      "bottom_nav_tasks",
      "bottom_nav_daily_log",
      "bottom_nav_habits",
      "bottom_nav_ai_assistant",
      "bottom_nav_home",
      "nav_reports_button",
      "nav_history_button",
      "nav_settings_button",
      "voice_capture_fab"
    ).forEach { tag ->
      try {
        composeTestRule.onNode(androidx.compose.ui.test.hasTestTag(tag)).performClick()
        composeTestRule.waitForIdle()
      } catch (e: Throwable) {
        throw RuntimeException("Failed on tag: $tag", e)
      }
    }
  }
}
