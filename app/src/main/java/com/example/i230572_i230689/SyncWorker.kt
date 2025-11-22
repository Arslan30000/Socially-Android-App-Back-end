package com.example.i230572_i230689

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class SyncWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {

    private val dbHelper = LocalDbHelper(appContext)
    private val sessionManager = SessionManager(appContext)

    override suspend fun doWork(): Result {
        val queuedActions = dbHelper.getQueuedActions()
        if (queuedActions.isEmpty()) {
            Log.d("SyncWorker", "No actions to sync.")
            return Result.success()
        }

        Log.d("SyncWorker", "Found ${queuedActions.size} actions to sync.")
        var allSucceeded = true

        for (action in queuedActions) {
            try {
                val success = performRequest(action.url, action.payload)
                if (success) {
                    dbHelper.deleteQueuedAction(action.id)
                    Log.d("SyncWorker", "Action ${action.id} ('${action.type}') synced and deleted.")
                } else {
                    allSucceeded = false
                    Log.d("SyncWorker", "Action ${action.id} ('${action.type}') failed to sync. Will retry later.")
                }
            } catch (e: Exception) {
                allSucceeded = false
                Log.e("SyncWorker", "Exception during sync for action ${action.id}: ${e.message}")
            }
        }

        return if (allSucceeded) Result.success() else Result.retry()
    }

    private suspend fun performRequest(url: String, payload: String): Boolean {
        return suspendCancellableCoroutine { continuation ->
            val token = sessionManager.getToken()
            if (token.isNullOrEmpty()) {
                Log.e("SyncWorker", "Cannot sync, user is not logged in.")
                continuation.resume(false)
                return@suspendCancellableCoroutine
            }

            val requestQueue = Volley.newRequestQueue(applicationContext)
            val stringRequest = object : StringRequest(Method.POST, url,
                { response ->
                    try {
                        val isSuccess = response.contains("\"success\":true")
                        if (isSuccess) {
                            Log.d("SyncWorker", "Request to $url successful.")
                            continuation.resume(true)
                        } else {
                            Log.e("SyncWorker", "Request to $url failed with response: $response")
                            continuation.resume(false)
                        }
                    } catch (e: Exception) {
                        Log.e("SyncWorker", "Error parsing response from $url: ${e.message}")
                        continuation.resume(false)
                    }
                },
                { error ->
                    Log.e("SyncWorker", "Network error for $url: ${error.message}")
                    continuation.resume(false) // Resume with false on network error so it can be retried
                }) {
                override fun getBody(): ByteArray {
                    return payload.toByteArray(Charsets.UTF_8)
                }

                override fun getBodyContentType(): String {
                    return "application/json; charset=utf-8"
                }

                override fun getHeaders(): MutableMap<String, String> {
                    return mutableMapOf("Authorization" to "Bearer $token")
                }
            }
            requestQueue.add(stringRequest)

            continuation.invokeOnCancellation {
                stringRequest.cancel()
            }
        }
    }
}
