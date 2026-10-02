package com.example.a62compass

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.a62compass.ui.navigation.CompassAppNavGraph
import com.example.a62compass.ui.theme.CompassTheme
import com.example.a62compass.ui.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {

    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settings by settingsViewModel.settings.collectAsState()

            CompassTheme(
                appTheme = settings.theme,
                dynamicColor = settings.dynamicColor
            ) {
                CompassAppNavGraph()
            }
        }
    }
}