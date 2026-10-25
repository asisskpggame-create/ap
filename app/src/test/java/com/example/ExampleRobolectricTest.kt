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
