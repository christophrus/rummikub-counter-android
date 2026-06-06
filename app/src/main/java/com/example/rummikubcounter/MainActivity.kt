package com.example.rummikubcounter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.rummikubcounter.data.SettingsDataStore
import com.example.rummikubcounter.ui.theme.RummikubCounterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settingsDataStore = SettingsDataStore(applicationContext)

        enableEdgeToEdge()
        setContent {
            val themeMode by settingsDataStore.themeMode.collectAsState(initial = "system")
            RummikubCounterTheme(
                themeMode = themeMode,
                darkTheme = isSystemInDarkTheme()
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RummikubApp()
                }
            }
        }
    }
}
