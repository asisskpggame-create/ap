package com.example

import android.content.Context
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
}
