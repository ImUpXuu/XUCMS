package com.upxuu.xucms.data

import android.content.Context
import android.content.SharedPreferences
import com.upxuu.xucms.editor.ToolbarLayout
import com.upxuu.xucms.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App settings and the admin credential. Credentials live in a separate private
 * preferences file so clearing the session never touches user preferences.
 */
class SettingsStore(context: Context) {

  private val prefs: SharedPreferences =
    context.getSharedPreferences("xucms_settings", Context.MODE_PRIVATE)
  private val credentials: SharedPreferences =
    context.getSharedPreferences("xucms_credentials", Context.MODE_PRIVATE)

  private val _themeMode = MutableStateFlow(readThemeMode())
  val themeModeFlow: StateFlow<ThemeMode> = _themeMode.asStateFlow()

  private val _signedIn = MutableStateFlow(credentials.getString(KEY_TOKEN, null) != null)
  val signedInFlow: StateFlow<Boolean> = _signedIn.asStateFlow()

  var baseUrl: String
    get() = prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL).orEmpty().ifBlank { DEFAULT_BASE_URL }
    set(value) {
      prefs.edit().putString(KEY_BASE_URL, value.trim().trimEnd('/')).apply()
    }

  var token: String
    get() = credentials.getString(KEY_TOKEN, "").orEmpty()
    set(value) {
      credentials.edit().putString(KEY_TOKEN, value).apply()
      _signedIn.value = value.isNotBlank()
    }

  val hasToken: Boolean get() = token.isNotBlank()

  var themeMode: ThemeMode
    get() = _themeMode.value
    set(value) {
      prefs.edit().putString(KEY_THEME, value.name).apply()
      _themeMode.value = value
    }

  /** Autosave cadence in seconds; 0 disables the timer (manual save only). */
  var autosaveSeconds: Int
    get() = prefs.getInt(KEY_AUTOSAVE, 8)
    set(value) = prefs.edit().putInt(KEY_AUTOSAVE, value.coerceIn(0, 120)).apply()

  var defaultCategory: String
    get() = prefs.getString(KEY_DEFAULT_CATEGORY, "").orEmpty()
    set(value) = prefs.edit().putString(KEY_DEFAULT_CATEGORY, value.trim()).apply()

  private val _toolbarLayout = MutableStateFlow(readToolbarLayout())
  val toolbarLayoutFlow: StateFlow<ToolbarLayout> = _toolbarLayout.asStateFlow()

  var toolbarLayout: ToolbarLayout
    get() = _toolbarLayout.value
    set(value) {
      prefs.edit()
        .putString(KEY_TOOLBAR_ACTIONS, value.serialize())
        .putInt(KEY_TOOLBAR_ROWS, value.rows.coerceIn(1, 2))
        .apply()
      _toolbarLayout.value = value
    }

  private fun readToolbarLayout(): ToolbarLayout = ToolbarLayout.deserialize(
    raw = prefs.getString(KEY_TOOLBAR_ACTIONS, null),
    rows = prefs.getInt(KEY_TOOLBAR_ROWS, 1),
  )

  fun signOut() {
    credentials.edit().remove(KEY_TOKEN).apply()
    _signedIn.value = false
  }

  private fun readThemeMode(): ThemeMode = runCatching {
    ThemeMode.valueOf(prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name)!!)
  }.getOrDefault(ThemeMode.SYSTEM)

  companion object {
    const val DEFAULT_BASE_URL = "https://edit.upxuu.com"
    private const val KEY_BASE_URL = "base_url"
    private const val KEY_TOKEN = "admin_token"
    private const val KEY_THEME = "theme_mode"
    private const val KEY_AUTOSAVE = "autosave_seconds"
    private const val KEY_DEFAULT_CATEGORY = "default_category"
    private const val KEY_TOOLBAR_ACTIONS = "toolbar_actions"
    private const val KEY_TOOLBAR_ROWS = "toolbar_rows"
  }
}
