package com.example.i230572_i230689

import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class LastActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private var profileReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile_page)

        sessionManager = SessionManager(this)

        // If caller provided updated user/counts (after profile edit), apply immediately
        intent.getStringExtra("user_json")?.let { uj ->
            try {
                val userObj = org.json.JSONObject(uj)
                val username = userObj.optString("username", "Unknown")
                val bio = userObj.optString("bio", "No bio yet")
                findViewById<TextView>(R.id.name).text = username
                findViewById<TextView>(R.id.title_text).text = username
                findViewById<TextView>(R.id.description).text = bio
                val imageBase64 = userObj.optString("imageBase64", "")
                if (imageBase64.isNotEmpty()) {
                    try {
                        val bytes = android.util.Base64.decode(imageBase64, android.util.Base64.DEFAULT)
                        val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        findViewById<ImageView>(R.id.profile_main).setImageBitmap(bmp)
                        findViewById<ImageView>(R.id.profile_icon).setImageBitmap(bmp)
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }

        intent.getStringExtra("counts_json")?.let { cj ->
            try {
                val countsObj = org.json.JSONObject(cj)
                val followers = countsObj.optInt("followers", 0)
                val following = countsObj.optInt("following", 0)
                val posts = countsObj.optInt("posts", 0)
                findViewById<TextView>(R.id.no_2).text = followers.toString()
                findViewById<TextView>(R.id.no_3).text = following.toString()
                findViewById<TextView>(R.id.no_1).text = posts.toString()
            } catch (_: Exception) {}
        }

        if (sessionManager.isLoggedIn()) loadUserData()

        findViewById<ImageView>(R.id.search_icon).setOnClickListener {
            startActivity(Intent(this, SixthActivity::class.java))
        }

        findViewById<ImageView>(R.id.like_icon).setOnClickListener {
            startActivity(Intent(this, EleventhActivity::class.java))
        }

        findViewById<ImageView>(R.id.post_icon).setOnClickListener {
            startActivity(Intent(this, FifteenthActivity::class.java))
        }

        findViewById<ImageView>(R.id.home_icon).setOnClickListener {
            startActivity(Intent(this, FifthActivity::class.java))
            finish()
        }

        findViewById<LinearLayout>(R.id.s_5).setOnClickListener {
            startActivity(Intent(this, SixteenActivity::class.java))
        }

        findViewById<RelativeLayout>(R.id.Following_).setOnClickListener {
            startActivity(Intent(this, FourteenthActivity::class.java))
        }
        findViewById<TextView>(R.id.no_2).setOnClickListener {
            val intent = Intent(this, FollowersListActivity::class.java)
            intent.putExtra("type", "followers")
            intent.putExtra("userId", sessionManager.getUserId().toString())
            startActivity(intent)
        }
        findViewById<TextView>(R.id.no_3).setOnClickListener {
            val intent = Intent(this, FollowersListActivity::class.java)
            intent.putExtra("type", "following")
            intent.putExtra("userId", sessionManager.getUserId().toString())
            startActivity(intent)
        }

    }

    override fun onResume() {
        super.onResume()
        if (sessionManager.isLoggedIn()) loadUserData()
    }

    override fun onStart() {
        super.onStart()
        // register receiver to refresh profile only when:
        // 1. profile is updated (profile_updated)
        // 2. user unfollows someone (user_unfollowed)
        // 3. follow request is accepted by someone (follow_request_accepted)
        try {
            val filter = IntentFilter()
            filter.addAction("profile_updated")
            filter.addAction("user_unfollowed")
            filter.addAction("follow_request_accepted")
            profileReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (sessionManager.isLoggedIn()) loadUserData()
                }
            }
            registerReceiver(profileReceiver, filter)
        } catch (_: Exception) {}
    }

    override fun onStop() {
        super.onStop()
        try {
            profileReceiver?.let { unregisterReceiver(it) }
        } catch (_: Exception) {}
        profileReceiver = null
    }

    private fun loadUserData() {
        val token = sessionManager.getToken() ?: return
        val userId = sessionManager.getUserId()
        val url = BuildConfig.BASE_URL + "get_profile.php?user_id=$userId"
        val rq = Volley.newRequestQueue(this)

        val req = object : StringRequest(Method.GET, url,
            { response ->
                try {
                    val obj = JSONObject(response.trim())
                    if (obj.optBoolean("success", false)) {
                        val userObj = obj.getJSONObject("user")
                        val counts = obj.getJSONObject("counts")

                        val username = userObj.optString("username", "Unknown")
                        val bio = userObj.optString("bio", "No bio yet")
                        val followers = counts.optInt("followers", 0)
                        val following = counts.optInt("following", 0)
                        val posts = counts.optInt("posts", 0)
                        val imageBase64 = userObj.optString("imageBase64", "")

                        findViewById<TextView>(R.id.name).text = username
                        findViewById<TextView>(R.id.title_text).text = username
                        findViewById<TextView>(R.id.description).text = bio
                        findViewById<TextView>(R.id.no_2).text = followers.toString()
                        findViewById<TextView>(R.id.no_3).text = following.toString()
                        findViewById<TextView>(R.id.no_1).text = posts.toString()

                        if (imageBase64.isNotEmpty()) {
                            try {
                                val bytes = Base64.decode(imageBase64, Base64.DEFAULT)
                                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                findViewById<ImageView>(R.id.profile_main).setImageBitmap(bmp)
                                findViewById<ImageView>(R.id.profile_icon).setImageBitmap(bmp)
                            } catch (_: Exception) {}
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            { error -> error.printStackTrace() }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer $token"
                return headers
            }
        }

        req.retryPolicy = DefaultRetryPolicy(15000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        rq.add(req)
    }
}
