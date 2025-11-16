package com.example.i230572_i230689

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class TwelfthActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RequestAdapter
    private val requestsList = mutableListOf<FollowRequest>()
    private lateinit var sessionManager: SessionManager
    private val handler = Handler(Looper.getMainLooper())
    private val pollIntervalMs: Long = 15_000
    private val pollRunnable = object : Runnable {
        override fun run() {
            loadFollowRequests()
            handler.postDelayed(this, pollIntervalMs)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.your_activity)

        sessionManager = SessionManager(this)
        recyclerView = findViewById(R.id.followRequestRecycler)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = RequestAdapter(requestsList) { request ->
            acceptFollowRequest(request)
        }
        recyclerView.adapter = adapter

        loadFollowRequests()
        handler.post(pollRunnable)
        setupNavigation()
    }

    private fun loadFollowRequests() {
        val token = sessionManager.getToken() ?: return
        val url = "https://nonactinically-unkindhearted-shelli.ngrok-free.dev/instagram_api/get_follow_requests.php"
        val rq = Volley.newRequestQueue(this)

        val req = object : StringRequest(Method.GET, url,
            { response ->
                try {
                    val obj = JSONObject(response.trim())
                    if (obj.optBoolean("success", false)) {
                        val arr = obj.optJSONArray("requests")
                        requestsList.clear()
                        if (arr != null) {
                            for (i in 0 until arr.length()) {
                                val r = arr.getJSONObject(i)
                                val uid = r.optInt("from_user_id").toString()
                                val username = r.optString("username")
                                val image = r.optString("imageBase64", "")
                                requestsList.add(FollowRequest(uid = uid, username = username, imageBase64 = image, timestamp = System.currentTimeMillis()))
                            }
                        }
                        adapter.notifyDataSetChanged()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            { error -> error.printStackTrace() }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer ${sessionManager.getToken()}"
                return headers
            }
        }

        req.retryPolicy = DefaultRetryPolicy(15000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        rq.add(req)
    }

    // We replaced realtime listeners with periodic polling via handler/pollRunnable above.

    private fun acceptFollowRequest(request: FollowRequest) {
        val token = sessionManager.getToken() ?: return
        val url = "https://nonactinically-unkindhearted-shelli.ngrok-free.dev/instagram_api/accept_follow_request.php"
        val rq = Volley.newRequestQueue(this)

        val req = object : StringRequest(Method.POST, url,
            { response ->
                try {
                    val obj = JSONObject(response.trim())
                    if (obj.optBoolean("success", false)) {
                        requestsList.remove(request)
                        adapter.notifyDataSetChanged()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            { error -> error.printStackTrace() }) {
            override fun getParams(): MutableMap<String, String> {
                val map = HashMap<String, String>()
                map["from_user_id"] = request.uid
                return map
            }

            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer ${sessionManager.getToken()}"
                return headers
            }
        }

        req.retryPolicy = DefaultRetryPolicy(15000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        rq.add(req)
    }

    private fun showNotification(title: String, body: String) {
        val channelId = "follow_notifications"
        val notificationId = System.currentTimeMillis().toInt()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Follow Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)) {
            notify(notificationId, builder.build())
        }
    }

    private fun setupNavigation() {
        findViewById<RelativeLayout>(R.id.following_tab).setOnClickListener {
            startActivity(Intent(this, EleventhActivity::class.java))
            finish()
        }

        findViewById<ImageView>(R.id.search_icon).setOnClickListener {
            startActivity(Intent(this, SeventhActivity::class.java))
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

        findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.profile_icon).setOnClickListener {
            startActivity(Intent(this, LastActivity::class.java))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(pollRunnable)
    }
}
