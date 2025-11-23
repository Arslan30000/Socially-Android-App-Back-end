package com.example.i230572_i230689

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
    private lateinit var dbHelper: LocalDbHelper
    private val handler = Handler(Looper.getMainLooper())
    private val pollIntervalMs: Long = 15_000
    private var pollRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.your_activity)

        sessionManager = SessionManager(this)
        dbHelper = LocalDbHelper(this, sessionManager.getUserId().toString())
        recyclerView = findViewById(R.id.followRequestRecycler)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = RequestAdapter(requestsList, 
            onAcceptClick = { request ->
                acceptFollowRequest(request)
            },
            onRejectClick = { request ->
                rejectFollowRequest(request)
            }
        )
        recyclerView.adapter = adapter

        loadFollowRequests()
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
                loadFollowRequestsFromServer()
            }
            handler.postDelayed(pollRunnable!!, pollIntervalMs)
        }
        handler.post(pollRunnable!!)
    }

    private fun stopPolling() {
        pollRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun loadFollowRequests() {
        // For now, we will only load from network as there's no offline cache for requests yet
        if (isOnline()) {
            loadFollowRequestsFromServer()
        } else {
            Toast.makeText(this, "You are offline.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadFollowRequestsFromServer() {
        val token = sessionManager.getToken() ?: return
        val url = BuildConfig.BASE_URL + "get_follow_requests.php"
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
                return mutableMapOf("Authorization" to "Bearer $token")
            }
        }
        req.retryPolicy = DefaultRetryPolicy(15000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        rq.add(req)
    }

    private fun acceptFollowRequest(request: FollowRequest) {
        val token = sessionManager.getToken() ?: return
        val url = BuildConfig.BASE_URL + "accept_follow_request.php"
        val rq = Volley.newRequestQueue(this)

        val req = object : StringRequest(Method.POST, url,
            { response ->
                try {
                    val obj = JSONObject(response.trim())
                    if (obj.optBoolean("success", false)) {
                        requestsList.remove(request)
                        adapter.notifyDataSetChanged()
                        sendBroadcast(Intent("follow_request_accepted"))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            { error -> error.printStackTrace() }) {
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf("from_user_id" to request.uid)
            }
            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf("Authorization" to "Bearer $token")
            }
        }
        req.retryPolicy = DefaultRetryPolicy(15000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        rq.add(req)
    }

    private fun rejectFollowRequest(request: FollowRequest) {
        val token = sessionManager.getToken() ?: return
        val url = BuildConfig.BASE_URL + "reject_follow_request.php"
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
                return hashMapOf("from_user_id" to request.uid)
            }
            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf("Authorization" to "Bearer $token")
            }
        }
        req.retryPolicy = DefaultRetryPolicy(15000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        rq.add(req)
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
        stopPolling()
    }
}
