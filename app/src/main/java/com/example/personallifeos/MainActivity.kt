package com.example.personallifeos

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.personallifeos.notification.SmartReminderManager
import com.example.personallifeos.ui.PersonalLifeOsApp
import com.example.personallifeos.ui.theme.PersonalLifeOSTheme
import com.example.personallifeos.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SmartReminderManager.ensureNotificationChannel(this)
        handleNotificationIntent(intent)
        enableEdgeToEdge()
        setContent {
            PersonalLifeOSTheme {
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
    }
}
