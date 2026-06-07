package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(getThemeModeFromPrefs())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private fun getThemeModeFromPrefs(): ThemeMode {
        val mode = prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        return try {
            ThemeMode.valueOf(mode)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
        _themeMode.value = mode
    }
    
    fun saveDraft(key: String, content: String) {
        prefs.edit().putString("draft_$key", content).apply()
    }
    
    fun getDraft(key: String): String? {
        return prefs.getString("draft_$key", null)
    }
    
    fun clearDraft(key: String) {
        prefs.edit().remove("draft_$key").apply()
    }

    fun getAllDraftKeys(): List<String> {
        return prefs.all.keys.filter { it.startsWith("draft_") }
    }

    fun getBaseUrl(): String {
        return prefs.getString("base_url", "https://edit.upxuu.com") ?: "https://edit.upxuu.com"
    }

    fun setBaseUrl(url: String) {
        prefs.edit().putString("base_url", url).apply()
    }
}
