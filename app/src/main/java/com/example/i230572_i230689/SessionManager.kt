package com.example.i230572_i230689

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

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

    fun logout() {
        val currentUserId = getUserId()
        val currentToken = getToken()

        if (currentUserId != -1 && !currentToken.isNullOrEmpty()) {
            // Pass user ID and token to the worker
            val inputData = Data.Builder()
                .putInt("USER_ID", currentUserId)
                .putString("TOKEN", currentToken)
                .build()

            val logoutWorkRequest = OneTimeWorkRequestBuilder<LogoutWorker>()
                .setInputData(inputData)
                .build()
            WorkManager.getInstance(ctx).enqueue(logoutWorkRequest)
        }
        
        // Clear all session data immediately
        prefs.edit().clear().apply()
    }

    // New: Save FCM token
    fun saveFcmToken(fcmToken: String) {
        prefs.edit().putString("fcm_token", fcmToken).apply()
    }

    // New: Get FCM token
    fun getFcmToken(): String? = prefs.getString("fcm_token", null)
}
