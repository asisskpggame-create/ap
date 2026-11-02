package com.example.personallifeos

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.personallifeos.notification.SmartReminderManager
import com.example.personallifeos.ui.PersonalLifeOsApp
import com.example.personallifeos.ui.theme.PersonalLifeOSTheme
import com.example.personallifeos.ui.viewmodel.MainViewModel
import com.example.personallifeos.ui.viewmodel.MainViewModel.AppThemeMode

class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SmartReminderManager.ensureNotificationChannel(this)
        handleNotificationIntent(intent)
        enableEdgeToEdge()
        setContent {
            val themeMode by mainViewModel.themeMode.collectAsState()
            val isDarkTheme = when (themeMode) {
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                AppThemeMode.DARK -> true
                AppThemeMode.LIGHT -> false
                else -> isSystemInDarkTheme()
            }
            PersonalLifeOSTheme(darkTheme = isDarkTheme) {
                PersonalLifeOsApp(viewModel = mainViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val topicId = intent?.getStringExtra(SmartReminderManager.EXTRA_OPEN_TOPIC_ID)?.takeIf { it.isNotBlank() }
        val taskId = intent?.getStringExtra(SmartReminderManager.EXTRA_OPEN_TASK_ID)?.takeIf { it.isNotBlank() }
        if (topicId != null || taskId != null) {
            mainViewModel.handleNotificationDeepLink(topicId, taskId)
        }

        if (intent?.getBooleanExtra("EXTRA_OPEN_VOICE_DIALOG", false) == true) {
            mainViewModel.openVoiceCapture()
        }

        val navTab = intent?.getIntExtra("EXTRA_NAVIGATE_TAB", -1) ?: -1
        if (navTab >= 0) {
            mainViewModel.navigateToTab(navTab)
        }
    }
}
