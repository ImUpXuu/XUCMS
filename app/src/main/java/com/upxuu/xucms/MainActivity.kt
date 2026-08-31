package com.upxuu.xucms

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.upxuu.xucms.navigation.XucmsApp
import com.upxuu.xucms.ui.theme.XucmsTheme

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    val container = AppContainer(applicationContext)

    setContent {
      val themeMode by container.settings.themeModeFlow.collectAsState()
      CompositionLocalProvider(LocalAppContainer provides container) {
        XucmsTheme(themeMode = themeMode) {
          XucmsApp()
        }
      }
    }
  }
}
