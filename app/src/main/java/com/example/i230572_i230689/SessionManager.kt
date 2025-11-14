package com.example.i230572_i230689

import android.content.Context

class SessionManager(private val ctx: Context) {

    private val prefs = ctx.getSharedPreferences("app_session", Context.MODE_PRIVATE)

    fun saveSession(token: String, userId: Int, username: String) {
        prefs.edit()
            .putString("token", token)
            .putInt("user_id", userId)
            .putString("username", username)
            .apply()
    }

    fun getToken(): String? = prefs.getString("token", null)
    fun getUserId(): Int = prefs.getInt("user_id", -1)
    fun getUsername(): String? = prefs.getString("username", null)

    fun isLoggedIn(): Boolean = !getToken().isNullOrEmpty()

    fun clear() {
        prefs.edit().clear().apply()
    }
}
