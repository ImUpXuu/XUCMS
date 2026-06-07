package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.data.AppPreferences
import com.example.data.TokenManager
import com.example.navigation.AppNavigation
import com.example.ui.theme.MyApplicationTheme

val LocalAppPreferences = staticCompositionLocalOf<AppPreferences?> { null }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val tokenManager = TokenManager(this)
        val appPreferences = AppPreferences(this)
        setContent {
            CompositionLocalProvider(LocalAppPreferences provides appPreferences) {
                MyApplicationTheme {
                    AppNavigation(tokenManager)
                }
            }
        }
    }
}

