package com.example.i230572_i230689

import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Base64
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class TwentyActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private var targetUserId: Int = 0
    private var isFollowing = false
    private var hasRequested = false
    private lateinit var followButton: RelativeLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile_f)

        sessionManager = SessionManager(this)
        targetUserId = intent.getIntExtra("userId", 0)

        if (targetUserId == 0) {
            finish()
            return
        }

        followButton = findViewById(R.id.Following_)
        
        loadUserProfile()

        findViewById<ImageView>(R.id.back_button).setOnClickListener {
            finish()
        }

        findViewById<TextView>(R.id.no_2).setOnClickListener {
            val intent = Intent(this, FollowersListActivity::class.java)
            intent.putExtra("type", "followers")
            intent.putExtra("userId", targetUserId.toString())
            startActivity(intent)
        }

        findViewById<TextView>(R.id.no_3).setOnClickListener {
            val intent = Intent(this, FollowersListActivity::class.java)
            intent.putExtra("type", "following")
            intent.putExtra("userId", targetUserId.toString())
            startActivity(intent)
        }

        followButton.setOnClickListener {
            if (hasRequested || isFollowing) {
                // Can't do anything if already requested or following
                return@setOnClickListener
            }
            sendFollowRequest()
        }
    }

    override fun onResume() {
        super.onResume()
        // refresh profile data when returning to this activity
        loadUserProfile()
    }

    private fun loadUserProfile() {
        val token = sessionManager.getToken() ?: return
        val url = BuildConfig.BASE_URL + "get_profile.php?user_id=$targetUserId"
        val rq = Volley.newRequestQueue(this)

        val req = object : StringRequest(Method.GET, url,
            { response ->
                try {
                    val obj = JSONObject(response.trim())
                    if (obj.optBoolean("success", false)) {
                        val userObj = obj.getJSONObject("user")
                        val counts = obj.getJSONObject("counts")
                        val relationship = obj.optJSONObject("relationship")

                        val username = userObj.optString("username", "Unknown")
                        val bio = userObj.optString("bio", "No bio yet")
                        val followers = counts.optInt("followers", 0)
                        val following = counts.optInt("following", 0)
                        val posts = counts.optInt("posts", 0)
                        val imageBase64 = userObj.optString("imageBase64", "")

                        isFollowing = relationship?.optBoolean("is_following", false) ?: false
                        hasRequested = relationship?.optBoolean("has_requested", false) ?: false

                        findViewById<TextView>(R.id.title_text).text = username
                        findViewById<TextView>(R.id.name).text = username
                        findViewById<TextView>(R.id.description).text = bio
                        findViewById<TextView>(R.id.no_2).text = followers.toString()
                        findViewById<TextView>(R.id.no_3).text = following.toString()
                        findViewById<TextView>(R.id.no_1).text = posts.toString()

                        // Update follow button text
                        updateFollowButtonState()

                        if (imageBase64.isNotEmpty()) {
                            try {
                                val bytes = Base64.decode(imageBase64, Base64.DEFAULT)
                                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                findViewById<ImageView>(R.id.profile_main).setImageBitmap(bmp)
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

    private fun updateFollowButtonState() {
        val buttonText = when {
            isFollowing -> "Unfollow"
            hasRequested -> "Follow Requested"
            else -> "Follow"
        }
        findViewById<TextView>(R.id.fo).text = buttonText
        followButton.isEnabled = !isFollowing && !hasRequested
    }

    private fun sendFollowRequest() {
        val token = sessionManager.getToken() ?: return
        val url = BuildConfig.BASE_URL + "send_follow_request.php"
        val rq = Volley.newRequestQueue(this)

        val req = object : StringRequest(Method.POST, url,
            { response ->
                try {
                    val obj = JSONObject(response.trim())
                    if (obj.optBoolean("success", false)) {
                        hasRequested = true
                        updateFollowButtonState()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            { error -> error.printStackTrace() }) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["to_user_id"] = targetUserId.toString()
                return params
            }

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
