package com.example.i230572_i230689

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class EleventhActivity: AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotificationAdapter
    private val notifications = mutableListOf<Notification>()
    private lateinit var sessionManager: SessionManager
    private lateinit var dbHelper: LocalDbHelper
    private val handler = Handler(Looper.getMainLooper())
    private var pollRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.recent_activity)
        
        sessionManager = SessionManager(this)
        dbHelper = LocalDbHelper(this, sessionManager.getUserId().toString())

        recyclerView = findViewById(R.id.notifications_recyclerview)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = NotificationAdapter(this, notifications)
        recyclerView.adapter = adapter

        loadNotifications()
        setupNavigation()
    }

    override fun onResume() {
        super.onResume()
        startPolling()
    }

    override fun onPause() {
        super.onPause()
        stopPolling()
    }

    private fun startPolling() {
        stopPolling()
        pollRunnable = Runnable {
            if (isOnline()) {
                loadNotificationsFromServer()
            }
            handler.postDelayed(pollRunnable!!, 10000) // Poll every 10 seconds
        }
        handler.post(pollRunnable!!)
    }

    private fun stopPolling() {
        pollRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun loadNotifications() {
        // For notifications, we will always fetch from the network first
        // as stale notifications are not very useful.
        if (isOnline()) {
            loadNotificationsFromServer()
        } else {
            Toast.makeText(this, "You are offline.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadNotificationsFromServer() {
        val token = sessionManager.getToken() ?: return
        val url = BuildConfig.BASE_URL + "get_notifications.php"
        val rq = Volley.newRequestQueue(this)

        val req = object : StringRequest(Method.GET, url,
            { response ->
                try {
                    val obj = JSONObject(response.trim())
                    if (obj.optBoolean("success", false)) {
                        val arr = obj.optJSONArray("notifications")
                        val networkNotifications = mutableListOf<Notification>()
                        if (arr != null) {
                            for (i in 0 until arr.length()) {
                                val n = arr.getJSONObject(i)
                                networkNotifications.add(Notification(
                                    type = n.optString("type"),
                                    userId = n.optString("user_id"),
                                    username = n.optString("username"),
                                    userImage = saveImageToLocalCache(n.optString("user_image"), "pfp_${n.optString("user_id")}"),
                                    postId = n.optString("post_id", null),
                                    postImage = saveImageToLocalCache(n.optString("post_image", ""), "post_${n.optString("post_id")}"),
                                    commentText = n.optString("comment_text", null),
                                    timestamp = n.optLong("timestamp")
                                ))
                            }
                        }
                        notifications.clear()
                        notifications.addAll(networkNotifications)
                        adapter.notifyDataSetChanged()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            { error -> error.printStackTrace() }) {
            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf("Authorization" to "Bearer $token")
            }
        }
        req.retryPolicy = DefaultRetryPolicy(15000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        rq.add(req)
    }

    private fun saveImageToLocalCache(base64String: String, fileName: String): String {
        if (base64String.isEmpty()) return ""
        return try {
            val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
            val file = File(filesDir, "$fileName.jpg")
            FileOutputStream(file).use {
                it.write(imageBytes)
            }
            file.absolutePath
        } catch (e: Exception) {
            Log.e("ImageCache", "Failed to save image $fileName: ${e.message}")
            ""
        }
    }

    private fun isOnline(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            else -> false
        }
    }

    private fun setupNavigation() {
        findViewById<TextView>(R.id.tab_you).setOnClickListener {
            startActivity(Intent(this, TwelfthActivity::class.java))
            finish()
        }
        findViewById<ImageView>(R.id.home_icon).setOnClickListener {
            startActivity(Intent(this, FifthActivity::class.java))
            finish()
        }
        findViewById<ImageView>(R.id.post_icon).setOnClickListener {
            startActivity(Intent(this, FifteenthActivity::class.java))
            finish()
        }
        findViewById<ImageView>(R.id.search_icon).setOnClickListener {
            startActivity(Intent(this, SeventhActivity::class.java))
            finish()
        }
        findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.profile_icon).setOnClickListener {
            startActivity(Intent(this, LastActivity::class.java))
        }
    }
}
