package com.example.i230572_i230689

import android.content.Context

class SessionManager(private val ctx: Context) {
    private val prefs = ctx.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    fun saveToken(token: String) = prefs.edit().putString("token", token).apply()
    fun getToken(): String? = prefs.getString("token", null)
    fun clear() = prefs.edit().clear().apply()
}
