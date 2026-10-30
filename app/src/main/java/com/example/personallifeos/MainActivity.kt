package com.example.personallifeos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.personallifeos.ui.PersonalLifeOsApp
import com.example.personallifeos.ui.theme.PersonalLifeOSTheme
import com.example.personallifeos.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PersonalLifeOSTheme {
                val viewModel: MainViewModel = viewModel()
                PersonalLifeOsApp(viewModel = viewModel)
            }
        }
    }
}
