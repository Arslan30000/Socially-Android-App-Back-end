package com.example.i230572_i230689

import android.content.Context
import android.content.Intent
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

class TwentyActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var dbHelper: LocalDbHelper
    private var targetUserId: Int = 0
    private var isFollowing = false
    private var hasRequested = false
    private lateinit var followButton: RelativeLayout

    private lateinit var postsRecyclerView: RecyclerView
    private lateinit var postAdapter: ProfilePostAdapter
    private val postList = mutableListOf<Post>()

    private lateinit var storiesRecyclerView: RecyclerView
    private lateinit var storyAdapter: ProfileStoryAdapter
    private val storyList = mutableListOf<Story>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile_f)

        sessionManager = SessionManager(this)
        targetUserId = intent.getIntExtra("userId", 0)

        if (targetUserId == 0) {
            finish()
            return
        }
        
        dbHelper = LocalDbHelper(this, targetUserId.toString())

        followButton = findViewById(R.id.Following_)
        
        setupRecyclerViews()
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
            if (isFollowing) {
                // Unfollow logic can be added here
            } else if (!hasRequested) {
                sendFollowRequest()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadUserProfile()
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

    private fun loadUserProfile() {
        loadDataFromCache()
        if (isOnline()) {
            loadUserDataFromNetwork()
        } else {
            Toast.makeText(this, "You are offline. Showing cached profile.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadDataFromCache() {
        val cachedPosts = dbHelper.getPosts()
        if (cachedPosts.isNotEmpty()) {
            postList.clear()
            postList.addAll(cachedPosts)
            postAdapter.notifyDataSetChanged()
        }

        val cachedStories = dbHelper.getStories()
        if (cachedStories.isNotEmpty()) {
            storyList.clear()
            storyList.addAll(cachedStories)
            storyAdapter.notifyDataSetChanged()
        }
    }

    private fun loadUserDataFromNetwork() {
        val token = sessionManager.getToken() ?: return
        val url = BuildConfig.BASE_URL + "get_profile.php?user_id=$targetUserId&include_posts=true&include_stories=true"
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
                        val postsCount = counts.optInt("posts", 0)
                        val imageBase64 = userObj.optString("imageBase64", "")
                        val imagePath = saveImageToLocalCache(imageBase64, "pfp_$targetUserId")

                        isFollowing = relationship?.optBoolean("is_following", false) ?: false
                        hasRequested = relationship?.optBoolean("has_requested", false) ?: false

                        findViewById<TextView>(R.id.title_text).text = username
                        findViewById<TextView>(R.id.name).text = username
                        findViewById<TextView>(R.id.description).text = bio
                        findViewById<TextView>(R.id.no_2).text = followers.toString()
                        findViewById<TextView>(R.id.no_3).text = following.toString()
                        findViewById<TextView>(R.id.no_1).text = postsCount.toString()

                        updateFollowButtonState()

                        if (imagePath.isNotEmpty()) {
                            Picasso.get().load(File(imagePath)).into(findViewById<ImageView>(R.id.profile_main))
                        }

                        val postsArray = obj.optJSONArray("posts")
                        val networkPosts = mutableListOf<Post>()
                        if (postsArray != null) {
                            for (i in 0 until postsArray.length()) {
                                val postObj = postsArray.getJSONObject(i)
                                val postId = postObj.optString("id")
                                networkPosts.add(Post(
                                    postId = postId,
                                    userId = targetUserId.toString(),
                                    postImage = saveImageToLocalCache(postObj.optString("postImage"), "post_$postId"),
                                    caption = postObj.optString("caption"),
                                    timestamp = postObj.optLong("timestamp")
                                ))
                            }
                        }
                        postList.clear()
                        postList.addAll(networkPosts)
                        postAdapter.notifyDataSetChanged()
                        dbHelper.upsertPosts(networkPosts)

                        val storiesArray = obj.optJSONArray("stories")
                        val networkStories = mutableListOf<Story>()
                        if (storiesArray != null) {
                            for (i in 0 until storiesArray.length()) {
                                val storyObj = storiesArray.getJSONObject(i)
                                val storyId = storyObj.optString("id")
                                networkStories.add(Story(
                                    storyId = storyId,
                                    userId = targetUserId.toString(),
                                    storyImage = saveImageToLocalCache(storyObj.optString("storyImage"), "story_$storyId"),
                                    timestamp = storyObj.optLong("timestamp")
                                ))
                            }
                        }
                        storyList.clear()
                        storyList.addAll(networkStories)
                        storyAdapter.notifyDataSetChanged()
                        dbHelper.upsertStories(networkStories)
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

    private fun updateFollowButtonState() {
        val buttonText = findViewById<TextView>(R.id.fo)
        when {
            isFollowing -> {
                buttonText.text = "Unfollow"
                followButton.isEnabled = true 
            }
            hasRequested -> {
                buttonText.text = "Requested"
                followButton.isEnabled = false
            }
            else -> {
                buttonText.text = "Follow"
                followButton.isEnabled = true
            }
        }
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
                return hashMapOf("to_user_id" to targetUserId.toString())
            }
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
        val connectivityManager = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            else -> false
        }
    }
}
