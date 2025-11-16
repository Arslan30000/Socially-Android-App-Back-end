package com.example.i230572_i230689

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
class SixthActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.search_page)
        val homebtn: ImageView = findViewById(R.id.home_icon)
        homebtn.setOnClickListener {
            val intent = Intent(this, FifthActivity::class.java)
            startActivity(intent)
            finish()
        }
        val searchBtn: RelativeLayout = findViewById(R.id.search_in)
        searchBtn.setOnClickListener {
            val intent = Intent(this, SeventhActivity::class.java)
            startActivity(intent)
        }
        val create_post_btn: ImageView = findViewById(R.id.post_icon)
        create_post_btn.setOnClickListener {
            val intent = Intent(this, FifteenthActivity::class.java)
            startActivity(intent)
            finish()

        }
        val like_btn: ImageView = findViewById(R.id.like_icon)
        like_btn.setOnClickListener {
            val intent = Intent(this, EleventhActivity::class.java)
            startActivity(intent)
            finish()

        }
        val Profile_btn: de.hdodenhof.circleimageview.CircleImageView = findViewById(R.id.profile_icon)
        Profile_btn.setOnClickListener {
            val intent = Intent(this, LastActivity::class.java)
            startActivity(intent)
            finish()

        }
        // Setup explore posts recycler view as a grid (images only)
        val recyclerView: RecyclerView = findViewById(R.id.explore_posts_recyclerview)
        val gridSpan = 3
        recyclerView.layoutManager = androidx.recyclerview.widget.GridLayoutManager(this, gridSpan)
        val posts = mutableListOf<Post>()
        val adapter = ExploreAdapter(this, posts) { post ->
            // clicking an image opens the detailed post on home-like view (open LastActivity or FifthActivity post detail)
            val intent = Intent(this, TwentyActivity::class.java)
            intent.putExtra("POST_ID", post.postId)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        fetchAllPosts(posts, adapter)
    }

    private fun fetchAllPosts(posts: MutableList<Post>, adapter: RecyclerView.Adapter<*>) {
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
                        posts.clear()
                        if (arr != null) {
                            for (i in 0 until arr.length()) {
                                val o = arr.getJSONObject(i)
                                posts.add(Post(
                                    postId = o.optInt("postId",0).toString(),
                                    userId = o.optInt("userId",0).toString(),
                                    postImage = o.optString("postImage",""),
                                    caption = o.optString("caption",""),
                                    timestamp = o.optLong("timestamp",0L),
                                    username = o.optString("username","User"),
                                    userProfileImage = o.optString("userProfileImage",""),
                                    likesCount = 0,
                                    commentsCount = 0,
                                    isLiked = false
                                ))
                            }
                        }
                        adapter.notifyDataSetChanged()
                    } else {
                        Log.e("Explore", obj.optString("message","No posts"))
                    }
                } catch (e: Exception) {
                    Log.e("Explore", "Parse error: ${e.message}")
                }
            },
            { error ->
                Log.e("Explore", "Network error: ${error.message}")
            }) {
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