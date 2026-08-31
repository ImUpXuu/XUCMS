package com.upxuu.xucms.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val LightScheme = lightColorScheme(
  primary = Palette.Sky600,
  onPrimary = Color.White,
  primaryContainer = Palette.Sky100,
  onPrimaryContainer = Palette.Sky700,
  secondary = Palette.Slate600,
  onSecondary = Color.White,
  secondaryContainer = Palette.Slate100,
  onSecondaryContainer = Palette.Slate700,
  tertiary = Palette.Amber500,
  onTertiary = Palette.Slate900,
  tertiaryContainer = Palette.Amber200,
  onTertiaryContainer = Palette.Slate800,
  background = Palette.Paper,
  onBackground = Palette.Ink,
  surface = Color.White,
  onSurface = Palette.Slate800,
  surfaceVariant = Palette.Slate050,
  onSurfaceVariant = Palette.Slate500,
  surfaceContainer = Palette.Slate050,
  surfaceContainerHigh = Palette.Slate100,
  surfaceContainerLow = Color.White,
  outline = Palette.Slate300,
  outlineVariant = Palette.Slate200,
  error = Palette.Rose600,
  onError = Color.White,
  errorContainer = Color(0xFFFEE2E2),
  onErrorContainer = Color(0xFF991B1B),
  scrim = Palette.Slate900.copy(alpha = 0.4f),
)

private val DarkScheme = darkColorScheme(
  primary = Palette.Sky400,
  onPrimary = Palette.Slate900,
  primaryContainer = Color(0xFF0C4A6E),
  onPrimaryContainer = Palette.Sky100,
  secondary = Palette.Slate400,
  onSecondary = Palette.Slate900,
  secondaryContainer = Palette.Slate700,
  onSecondaryContainer = Palette.Slate100,
  tertiary = Palette.Amber200,
  onTertiary = Palette.Slate900,
  tertiaryContainer = Color(0xFF78350F),
  onTertiaryContainer = Palette.Amber200,
  background = Color(0xFF0B1120),
  onBackground = Palette.Slate100,
  surface = Color(0xFF111827),
  onSurface = Palette.Slate100,
  surfaceVariant = Color(0xFF16202F),
  onSurfaceVariant = Palette.Slate400,
  surfaceContainer = Color(0xFF16202F),
  surfaceContainerHigh = Color(0xFF1E293B),
  surfaceContainerLow = Color(0xFF0F172A),
  outline = Palette.Slate600,
  outlineVariant = Color(0xFF283549),
  error = Palette.Rose400,
  onError = Palette.Slate900,
  errorContainer = Color(0xFF7F1D1D),
  onErrorContainer = Color(0xFFFECACA),
  scrim = Color.Black.copy(alpha = 0.6f),
)

private val XucmsShapes = Shapes(
  extraSmall = RoundedCornerShape(6.dp),
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(14.dp),
  large = RoundedCornerShape(20.dp),
  extraLarge = RoundedCornerShape(28.dp),
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Composable
fun XucmsTheme(
  themeMode: ThemeMode = ThemeMode.SYSTEM,
  content: @Composable () -> Unit,
) {
  val dark = when (themeMode) {
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
  }
  val scheme = if (dark) DarkScheme else LightScheme

  val view = LocalView.current
  if (!view.isInEditMode) {
    val context = LocalContext.current
    SideEffect {
      (context as? Activity)?.window?.let { window ->
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
        WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !dark
      }
    }
  }

  MaterialTheme(
    colorScheme = scheme,
    typography = XucmsTypography,
    shapes = XucmsShapes,
    content = content,
  )
}
