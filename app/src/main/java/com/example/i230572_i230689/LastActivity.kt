package com.example.i230572_i230689

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.squareup.picasso.Picasso
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class LastActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var dbHelper: LocalDbHelper
    private var profileReceiver: BroadcastReceiver? = null

    private lateinit var postsRecyclerView: RecyclerView
    private lateinit var postAdapter: ProfilePostAdapter
    private val postList = mutableListOf<Post>()

    private lateinit var storiesRecyclerView: RecyclerView
    private lateinit var storyAdapter: ProfileStoryAdapter
    private val storyList = mutableListOf<Story>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile_page)

        sessionManager = SessionManager(this)
        dbHelper = LocalDbHelper(this, sessionManager.getUserId().toString())

        setupRecyclerViews()
        setupBottomNavigationBar()
        loadData()
    }

    override fun onResume() {
        super.onResume()
        if (sessionManager.isLoggedIn()) {
            if (isOnline()) {
                setStatus("online")
            }
            loadData()
        }
    }

    override fun onPause() {
        super.onPause()
        if (isOnline()) {
            setStatus("offline")
        }
    }

    override fun onStart() {
        super.onStart()
        try {
            val filter = IntentFilter()
            filter.addAction("profile_updated")
            filter.addAction("user_unfollowed")
            filter.addAction("follow_request_accepted")
            profileReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (sessionManager.isLoggedIn()) loadData()
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

    private fun setupRecyclerViews() {
        postsRecyclerView = findViewById(R.id.profile_posts_recyclerview)
        postsRecyclerView.layoutManager = GridLayoutManager(this, 3)
        postAdapter = ProfilePostAdapter(this, postList)
        postsRecyclerView.adapter = postAdapter

        storiesRecyclerView = findViewById(R.id.profile_stories_recyclerview)
        storiesRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        storyAdapter = ProfileStoryAdapter(this, storyList)
        storiesRecyclerView.adapter = storyAdapter
    }

    private fun loadData() {
        loadDataFromCache()
        if (isOnline()) {
            loadUserDataFromNetwork()
        } else {
            Toast.makeText(this, "You are offline. Showing cached profile.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadDataFromCache() {
        val currentUserId = sessionManager.getUserId().toString()
        val cachedPosts = dbHelper.getPosts(currentUserId)
        if (cachedPosts.isNotEmpty()) {
            postList.clear()
            postList.addAll(cachedPosts)
            postAdapter.notifyDataSetChanged()
        }

        val cachedStories = dbHelper.getStories(currentUserId)
        if (cachedStories.isNotEmpty()) {
            storyList.clear()
            storyList.addAll(cachedStories)
            storyAdapter.notifyDataSetChanged()
        }
    }

    private fun loadUserDataFromNetwork() {
        val token = sessionManager.getToken() ?: return
        val userId = sessionManager.getUserId()
        val url = BuildConfig.BASE_URL + "get_profile.php?user_id=$userId&include_posts=true&include_stories=true"
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
                        val postsCount = counts.optInt("posts", 0)
                        val imageBase64 = userObj.optString("imageBase64", "")
                        val imagePath = saveImageToLocalCache(imageBase64, "pfp_$userId")

                        findViewById<TextView>(R.id.name).text = username
                        findViewById<TextView>(R.id.title_text).text = username
                        findViewById<TextView>(R.id.description).text = bio
                        findViewById<TextView>(R.id.no_2).text = followers.toString()
                        findViewById<TextView>(R.id.no_3).text = following.toString()
                        findViewById<TextView>(R.id.no_1).text = postsCount.toString()

                        if (imagePath.isNotEmpty()) {
                            Picasso.get().load(File(imagePath)).into(findViewById<ImageView>(R.id.profile_main))
                            Picasso.get().load(File(imagePath)).into(findViewById<ImageView>(R.id.profile_icon))
                        }

                        val postsArray = obj.optJSONArray("posts")
                        val networkPosts = mutableListOf<Post>()
                        if (postsArray != null) {
                            for (i in 0 until postsArray.length()) {
                                val postObj = postsArray.getJSONObject(i)
                                val postId = postObj.optString("id")
                                networkPosts.add(Post(
                                    postId = postId,
                                    userId = userId.toString(),
                                    postImage = saveImageToLocalCache(postObj.optString("postImage"), "post_$postId"),
                                    caption = postObj.optString("caption"),
                                    timestamp = postObj.optLong("timestamp")
                                ))
                            }
                        }
                        // Clear existing posts for this user before upserting new ones
                        dbHelper.deleteAllPosts(userId.toString())
                        dbHelper.upsertPosts(networkPosts)
                        postList.clear()
                        postList.addAll(networkPosts)
                        postAdapter.notifyDataSetChanged()

                        val storiesArray = obj.optJSONArray("stories")
                        val networkStories = mutableListOf<Story>()
                        if (storiesArray != null) {
                            for (i in 0 until storiesArray.length()) {
                                val storyObj = storiesArray.getJSONObject(i)
                                val storyId = storyObj.optString("id")
                                networkStories.add(Story(
                                    storyId = storyId,
                                    userId = userId.toString(),
                                    storyImage = saveImageToLocalCache(storyObj.optString("storyImage"), "story_$storyId"),
                                    timestamp = storyObj.optLong("timestamp")
                                ))
                            }
                        }
                        // Clear existing stories for this user before upserting new ones
                        dbHelper.deleteAllStories(userId.toString())
                        dbHelper.upsertStories(networkStories)
                        storyList.clear()
                        storyList.addAll(networkStories)
                        storyAdapter.notifyDataSetChanged()
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

    private fun setStatus(status: String) {
        val token = sessionManager.getToken() ?: return
        val url = BuildConfig.BASE_URL + "set_status.php"
        val rq = Volley.newRequestQueue(this)
        val req = object : StringRequest(Method.POST, url,
            { _ -> },
            { e -> e.printStackTrace() }) {
            override fun getBody(): ByteArray? {
                val obj = JSONObject()
                obj.put("status", status)
                return obj.toString().toByteArray()
            }

            override fun getBodyContentType(): String = "application/json"
            override fun getHeaders(): MutableMap<String, String> {
                val h = HashMap<String, String>()
                h["Authorization"] = "Bearer ${sessionManager.getToken()}"
                return h
            }
        }
        req.retryPolicy = DefaultRetryPolicy(8000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        rq.add(req)
    }

    private fun setupBottomNavigationBar() {
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
        // Corrected: Removed the listener for the non-existent view
        // findViewById<LinearLayout>(R.id.s_5).setOnClickListener { ... }
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
}