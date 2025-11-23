package com.example.i230572_i230689

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class LogoutWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val userId = inputData.getInt("USER_ID", -1)
        val token = inputData.getString("TOKEN")

        if (userId == -1 || token.isNullOrEmpty()) {
            Log.d("LogoutWorker", "User ID or token missing from input data. Skipping offline status update.")
            return Result.success()
        }

        Log.d("LogoutWorker", "Attempting to set user $userId offline.")
        return try {
            val success = setStatusOffline(token)
            if (success) {
                Log.d("LogoutWorker", "User $userId status set to offline successfully.")
                Result.success()
            } else {
                Log.e("LogoutWorker", "Failed to set user $userId status to offline. Retrying.")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("LogoutWorker", "Exception setting user $userId status to offline: ${e.message}")
            Result.retry()
        }
    }

    private suspend fun setStatusOffline(token: String): Boolean {
        return suspendCancellableCoroutine { continuation ->
            val url = BuildConfig.BASE_URL + "set_status.php"
            val rq = Volley.newRequestQueue(applicationContext)
            val obj = JSONObject()
            obj.put("status", "offline")

            val req = object : StringRequest(Method.POST, url,
                { response ->
                    try {
                        val jsonResponse = JSONObject(response)
                        val success = jsonResponse.optBoolean("success", false)
                        continuation.resume(success)
                    } catch (e: Exception) {
                        Log.e("LogoutWorker", "Error parsing response: ${e.message}")
                        continuation.resume(false)
                    }
                },
                { error ->
                    Log.e("LogoutWorker", "Network error: ${error.message}")
                    continuation.resumeWithException(error)
                }) {
                override fun getBody(): ByteArray = obj.toString().toByteArray()
                override fun getBodyContentType(): String = "application/json"
                override fun getHeaders(): MutableMap<String, String> {
                    return mutableMapOf("Authorization" to "Bearer $token")
                }
            }
            rq.add(req)
            continuation.invokeOnCancellation {
                req.cancel()
            }
        }
    }
}
