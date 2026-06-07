package com.example.data

import android.content.Context

class TokenManager(context: Context) {
    private val prefs = context.getSharedPreferences("admin_key", Context.MODE_PRIVATE)

    fun getToken(): String? = prefs.getString("token", null)

    fun saveToken(token: String) {
        prefs.edit().putString("token", token).apply()
    }

    fun clearToken() {
        prefs.edit().remove("token").apply()
    }

    fun hasToken(): Boolean = getToken() != null
}
