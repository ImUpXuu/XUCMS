package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.LocalAppPreferences
import com.example.data.ThemeMode

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF3B82F6),
    primaryContainer = Color(0xFFDBEAFE),
    onPrimary = Color.White,
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    onBackground = Color(0xFF1E293B),
    onSurface = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF64748B),
    outlineVariant = Color(0xFFE2E8F0),
    error = Color(0xFFEF4444)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF60A5FA),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimary = Color.Black,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFF94A3B8),
    outlineVariant = Color(0xFF334155),
    error = Color(0xFFF87171)
)

@Composable
fun MyApplicationTheme(
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val prefs = LocalAppPreferences.current
    val themeMode by prefs?.themeMode?.collectAsState() ?: androidx.compose.runtime.mutableStateOf(ThemeMode.SYSTEM)
    val sysDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        else -> sysDark
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
