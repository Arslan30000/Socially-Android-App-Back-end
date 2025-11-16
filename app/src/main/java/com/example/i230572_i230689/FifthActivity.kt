package com.example.i230572_i230689

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import de.hdodenhof.circleimageview.CircleImageView
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class FifthActivity : AppCompatActivity() {

    private lateinit var recyclerViewStory: RecyclerView
    private lateinit var storyAdapter: StoryAdapter
    private val storyList = mutableListOf<Story>()


    private lateinit var postsRecyclerView: RecyclerView
    private lateinit var postAdapter: PostAdapter
    private val postList = mutableListOf<Post>()

    private lateinit var sessionManager: SessionManager

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val imageUri = result.data?.data
            if (imageUri != null) {
                try {
                    val inputStream = contentResolver.openInputStream(imageUri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    uploadStory(bitmap)
                } catch (e: Exception) {
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_page)

        sessionManager = SessionManager(this)

        if (!sessionManager.isLoggedIn()) {
            Log.d("FifthActivity", "User is not logged in. Redirecting to login.")
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        Log.d("FifthActivity", "User ${sessionManager.getUsername()} is logged in. Setting up UI.")
        setupRecyclerView()
        setupBottomNavigationBar()

        postsRecyclerView = findViewById(R.id.posts_recyclerview)
        postsRecyclerView.layoutManager = LinearLayoutManager(this)
        postAdapter = PostAdapter(this, postList)
        postsRecyclerView.adapter = postAdapter
    }

    override fun onResume() {
        super.onResume()
        if (sessionManager.isLoggedIn()) {
            fetchStoriesForFeed()
            fetchPostFeed()
        } else {
            Log.d("FifthActivity", "User is not logged in on resume. Redirecting to login.")
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }


    private fun setupRecyclerView() {
        recyclerViewStory = findViewById(R.id.recycler_view_story)
        recyclerViewStory.setHasFixedSize(true)
        recyclerViewStory.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        storyAdapter = StoryAdapter(this, storyList) {
            openGallery()
        }
        recyclerViewStory.adapter = storyAdapter
    }
    private fun fetchStoriesForFeed() {
        val token = sessionManager.getToken() ?: return
        val url = "https://nonactinically-unkindhearted-shelli.ngrok-free.dev/instagram_api/get_stories.php"
        val rq = Volley.newRequestQueue(this)

        val req = object : StringRequest(Method.GET, url,
            { response ->
                try {
                    val obj = JSONObject(response.trim())
                    if (obj.optBoolean("success", false)) {
                        val yourStoryObj = obj.optJSONObject("your_story")
                        val storiesArray = obj.optJSONArray("stories")

                        storyList.clear()

                        if (yourStoryObj != null) {
                            storyList.add(Story(
                                storyId = yourStoryObj.optInt("id", -1).toString(),
                                userId = yourStoryObj.optInt("userId", 0).toString(),
                                username = yourStoryObj.optString("username", "Your Story"),
                                userProfilePicture = yourStoryObj.optString("userProfilePicture", ""),
                                storyImage = "",
                                timestamp = 0L,
                                isAddButton = true
                            ))
                        }

                        if (storiesArray != null) {
                            for (i in 0 until storiesArray.length()) {
                                val storyObj = storiesArray.getJSONObject(i)
                                storyList.add(Story(
                                    storyId = storyObj.optInt("id", 0).toString(),
                                    userId = storyObj.optInt("userId", 0).toString(),
                                    username = storyObj.optString("username", "User"),
                                    userProfilePicture = storyObj.optString("userProfilePicture", ""),
                                    storyImage = storyObj.optString("storyImage", ""),
                                    timestamp = storyObj.optLong("timestamp", 0L),
                                    isAddButton = false
                                ))
                            }
                        }

                        storyAdapter.notifyDataSetChanged()
                    } else {
                        Log.e("StoriesFeed", obj.optString("message", "Failed to fetch stories"))
                    }
                } catch (e: Exception) {
                    Log.e("StoriesFeed", "Error parsing response: ${e.message}")
                }
            },
            { error ->
                Log.e("StoriesFeed", "Network error: ${error.message}")
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer $token"
                headers["Content-Type"] = "application/x-www-form-urlencoded"
                return headers
            }
        }

        req.retryPolicy = DefaultRetryPolicy(15000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        rq.add(req)
    }
    private fun fetchPostFeed() {
        val token = sessionManager.getToken() ?: return
        val url = "https://nonactinically-unkindhearted-shelli.ngrok-free.dev/instagram_api/get_feed.php"
        val rq = Volley.newRequestQueue(this)

        val req = object : StringRequest(Method.GET, url,
            { response ->
                try {
                    val obj = JSONObject(response.trim())
                    if (obj.optBoolean("success", false)) {
                        val postsArray = obj.optJSONArray("posts")

                        val posts = mutableListOf<Post>()
                        if (postsArray != null) {
                            for (i in 0 until postsArray.length()) {
                                val postObj = postsArray.getJSONObject(i)
                                posts.add(Post(
                                    postId = postObj.optInt("postId", 0).toString(),
                                    userId = postObj.optInt("userId", 0).toString(),
                                    postImage = postObj.optString("postImage", ""),
                                    caption = postObj.optString("caption", ""),
                                    timestamp = postObj.optLong("timestamp", 0L),
                                    username = postObj.optString("username", "User"),
                                    userProfileImage = postObj.optString("userProfileImage", ""),
                                    likesCount = 0,
                                    commentsCount = 0
                                ))
                            }
                        }

                        postList.clear()
                        postList.addAll(posts)
                        postAdapter.notifyDataSetChanged()
                    } else {
                        Log.d("PostFeed", obj.optString("message", "No posts found"))
                        postList.clear()
                        postAdapter.notifyDataSetChanged()
                    }
                } catch (e: Exception) {
                    Log.e("PostFeed", "Error parsing response: ${e.message}")
                }
            },
            { error ->
                Log.e("PostFeed", "Network error: ${error.message}")
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer $token"
                headers["Content-Type"] = "application/x-www-form-urlencoded"
                return headers
            }
        }

        req.retryPolicy = DefaultRetryPolicy(15000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        rq.add(req)
    }


    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private fun uploadStory(bitmap: Bitmap) {
        val token = sessionManager.getToken() ?: return

        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos)
        val bitmapString = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)

        val url = "https://nonactinically-unkindhearted-shelli.ngrok-free.dev/instagram_api/upload_story.php"
        val rq = Volley.newRequestQueue(this)

        val req = object : StringRequest(Method.POST, url,
            { response ->
                try {
                    val obj = JSONObject(response.trim())
                    if (obj.optBoolean("success", false)) {
                        Toast.makeText(this@FifthActivity, "Story Uploaded!", Toast.LENGTH_SHORT).show()
                        // Refresh the feed to show the new story circle
                        fetchStoriesForFeed()
                    } else {
                        Toast.makeText(this@FifthActivity, obj.optString("message", "Upload failed."), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@FifthActivity, "Invalid server response", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this@FifthActivity, "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
            }) {

            override fun getParams(): MutableMap<String, String> {
                val map = HashMap<String, String>()
                map["storyImage"] = bitmapString
                map["timestamp"] = System.currentTimeMillis().toString()
                return map
            }

            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer $token"
                return headers
            }
        }

        req.retryPolicy = DefaultRetryPolicy(30000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        rq.add(req)
    }

    private fun setupBottomNavigationBar() {
        findViewById<ImageView>(R.id.search_icon).setOnClickListener {
            startActivity(Intent(this, SixthActivity::class.java))
        }
        findViewById<CircleImageView>(R.id.profile_icon).setOnClickListener {
            startActivity(Intent(this, LastActivity::class.java))
        }
        findViewById<ImageView>(R.id.share).setOnClickListener {
            startActivity(Intent(this, EightActivity::class.java))
        }
        findViewById<ImageView>(R.id.like_icon).setOnClickListener {
            startActivity(Intent(this, EleventhActivity::class.java))
        }
        val addPostIcon = findViewById<ImageView>(R.id.post_icon)
        addPostIcon.setOnClickListener {
            val intent = Intent(this, AddPostActivity::class.java)
            intent.putExtra("USER_ID", sessionManager.getUserId())
            startActivity(intent)
        }
    }
}
