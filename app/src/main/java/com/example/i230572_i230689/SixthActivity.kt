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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class SixthActivity : AppCompatActivity() {

    private lateinit var dbHelper: LocalDbHelper
    private val posts = mutableListOf<Post>()
    private lateinit var adapter: ExploreAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.search_page)
        
        dbHelper = LocalDbHelper(this)

        setupUI()
        loadPosts()
    }

    private fun setupUI() {
        val homebtn: ImageView = findViewById(R.id.home_icon)
        homebtn.setOnClickListener {
            startActivity(Intent(this, FifthActivity::class.java))
            finish()
        }
        val searchBtn: RelativeLayout = findViewById(R.id.search_in)
        searchBtn.setOnClickListener {
            startActivity(Intent(this, SeventhActivity::class.java))
        }
        val create_post_btn: ImageView = findViewById(R.id.post_icon)
        create_post_btn.setOnClickListener {
            startActivity(Intent(this, FifteenthActivity::class.java))
            finish()
        }
        val like_btn: ImageView = findViewById(R.id.like_icon)
        like_btn.setOnClickListener {
            startActivity(Intent(this, EleventhActivity::class.java))
            finish()
        }
        val Profile_btn: de.hdodenhof.circleimageview.CircleImageView = findViewById(R.id.profile_icon)
        Profile_btn.setOnClickListener {
            startActivity(Intent(this, LastActivity::class.java))
            finish()
        }

        val recyclerView: RecyclerView = findViewById(R.id.explore_posts_recyclerview)
        recyclerView.layoutManager = GridLayoutManager(this, 3)
        adapter = ExploreAdapter(this, posts) { post ->
            val intent = Intent(this, TwentyActivity::class.java)
            intent.putExtra("POST_ID", post.postId)
            startActivity(intent)
        }
        recyclerView.adapter = adapter
    }

    private fun loadPosts() {
        loadPostsFromCache()
        if (isOnline()) {
            fetchAllPostsFromNetwork()
        } else {
            Toast.makeText(this, "You are offline. Showing cached posts.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadPostsFromCache() {
        val cachedPosts = dbHelper.getPosts()
        if (cachedPosts.isNotEmpty()) {
            posts.clear()
            posts.addAll(cachedPosts)
            adapter.notifyDataSetChanged()
            Log.d("SixthActivity", "Loaded ${cachedPosts.size} posts from cache.")
        }
    }

    private fun fetchAllPostsFromNetwork() {
        val sessionManager = SessionManager(this)
        val token = sessionManager.getToken() ?: return
        val url = BuildConfig.BASE_URL + "get_all_posts.php"
        val rq = Volley.newRequestQueue(this)

        val req = object : StringRequest(Method.GET, url,
            { response ->
                try {
                    val obj = JSONObject(response.trim())
                    if (obj.optBoolean("success", false)) {
                        val arr = obj.optJSONArray("posts")
                        val networkPosts = mutableListOf<Post>()
                        if (arr != null) {
                            for (i in 0 until arr.length()) {
                                val o = arr.getJSONObject(i)
                                val postId = o.optString("postId")
                                val postImageBase64 = o.optString("postImage", "")
                                networkPosts.add(Post(
                                    postId = postId,
                                    userId = o.optString("userId"),
                                    postImage = saveImageToLocalCache(postImageBase64, "post_$postId"),
                                    caption = o.optString("caption"),
                                    timestamp = o.optLong("timestamp"),
                                    username = o.optString("username"),
                                    userProfileImage = ""
                                ))
                            }
                        }
                        posts.clear()
                        posts.addAll(networkPosts)
                        adapter.notifyDataSetChanged()
                        dbHelper.upsertPosts(networkPosts)
                        Log.d("SixthActivity", "Fetched and cached ${networkPosts.size} posts.")
                    } else {
                        Log.e("Explore", obj.optString("message", "No posts"))
                    }
                } catch (e: Exception) {
                    Log.e("Explore", "Parse error: ${e.message}")
                }
            },
            { error ->
                Log.e("Explore", "Network error: ${error.message}")
            }) {
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
}
