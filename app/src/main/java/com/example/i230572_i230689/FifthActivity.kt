package com.example.i230572_i230689

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class FifthActivity : AppCompatActivity() {

    private lateinit var recyclerViewStory: RecyclerView
    private lateinit var storyAdapter: StoryAdapter
    private val storyList = mutableListOf<Story>()

    private lateinit var postsRecyclerView: RecyclerView
    private lateinit var postAdapter: PostAdapter
    private val postList = mutableListOf<Post>()

    private lateinit var sessionManager: SessionManager
    private lateinit var dbHelper: LocalDbHelper

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
        dbHelper = LocalDbHelper(this)

        if (!sessionManager.isLoggedIn()) {
            redirectToLogin()
            return
        }

        setupRecyclerViews()
        setupBottomNavigationBar()
        loadData()
    }
    
    private fun redirectToLogin() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onResume() {
        super.onResume()
        if (sessionManager.isLoggedIn()) {
            // Only fetch fresh data on resume if online
            if (isOnline()) {
                fetchStoriesForFeed()
                fetchPostFeed()
                fetchCurrentUserProfileForNav()
            }
        } else {
            redirectToLogin()
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

    private fun loadData() {
        // Load from cache first for an instant UI
        loadDataFromCache()
        // Then, fetch from the network to get the latest data
        if (isOnline()) {
            fetchStoriesForFeed()
            fetchPostFeed()
            fetchCurrentUserProfileForNav()
        } else {
            Toast.makeText(this, "You are offline. Showing cached data.", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadDataFromCache() {
        val cachedPosts = dbHelper.getPosts()
        if (cachedPosts.isNotEmpty()) {
            postList.clear()
            postList.addAll(cachedPosts)
            postAdapter.notifyDataSetChanged()
            Log.d("FifthActivity", "Loaded ${cachedPosts.size} posts from cache.")
        }

        val cachedStories = dbHelper.getStories()
        if (cachedStories.isNotEmpty()) {
            val storiesWithAdd = mutableListOf<Story>()
            val currentUserStory = Story(userId = sessionManager.getUserId().toString(), username = "Your Story", isAddButton = true)
            storiesWithAdd.add(currentUserStory)
            storiesWithAdd.addAll(cachedStories)
            storyList.clear()
            storyList.addAll(storiesWithAdd)
            storyAdapter.notifyDataSetChanged()
            Log.d("FifthActivity", "Loaded ${cachedStories.size} stories from cache.")
        }
    }

    private fun setupRecyclerViews() {
        recyclerViewStory = findViewById(R.id.recycler_view_story)
        recyclerViewStory.setHasFixedSize(true)
        recyclerViewStory.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        storyAdapter = StoryAdapter(this, storyList) { story ->
            if (story.isAddButton) {
                if (story.hasStories) {
                    val intent = Intent(this, NineteenActivity::class.java)
                    intent.putExtra("USER_ID", story.userId)
                    startActivity(intent)
                } else {
                    openGallery()
                }
            } else {
                val intent = Intent(this, NineteenActivity::class.java)
                intent.putExtra("USER_ID", story.userId)
                startActivity(intent)
            }
        }
        recyclerViewStory.adapter = storyAdapter

        postsRecyclerView = findViewById(R.id.posts_recyclerview)
        postsRecyclerView.layoutManager = LinearLayoutManager(this)
        postAdapter = PostAdapter(this, postList)
        postsRecyclerView.adapter = postAdapter
    }

    private fun fetchStoriesForFeed() {
        val token = sessionManager.getToken() ?: return
        val url = BuildConfig.BASE_URL + "get_stories.php"
        val rq = Volley.newRequestQueue(this)

        val req = object : StringRequest(Method.GET, url,
            { response ->
                try {
                    val obj = JSONObject(response.trim())
                    val networkStories = mutableListOf<Story>()
                    val currentUserStory = Story(userId = sessionManager.getUserId().toString(), username = "Your Story", isAddButton = true)
                    networkStories.add(currentUserStory)

                    if (obj.optBoolean("success", false)) {
                        val yourStoryObj = obj.optJSONObject("your_story")
                        if (yourStoryObj != null) {
                            currentUserStory.hasStories = yourStoryObj.optBoolean("hasStories", false)
                            val userPfpBase64 = yourStoryObj.optString("userProfilePicture", "")
                            currentUserStory.userProfilePicture = saveImageToLocalCache(userPfpBase64, "pfp_${currentUserStory.userId}")
                        }

                        val storiesArray = obj.optJSONArray("stories")
                        if (storiesArray != null) {
                            for (i in 0 until storiesArray.length()) {
                                val storyObj = storiesArray.getJSONObject(i)
                                val storyId = storyObj.optString("id")
                                val userId = storyObj.optString("userId")
                                val storyImageBase64 = storyObj.optString("storyImage", "")
                                val userPfpBase64 = storyObj.optString("userProfilePicture", "")

                                networkStories.add(Story(
                                    storyId = storyId,
                                    userId = userId,
                                    username = storyObj.optString("username", "User"),
                                    userProfilePicture = saveImageToLocalCache(userPfpBase64, "pfp_$userId"),
                                    storyImage = saveImageToLocalCache(storyImageBase64, "story_$storyId"),
                                    timestamp = storyObj.optLong("timestamp", 0L),
                                    isAddButton = false
                                ))
                            }
                        }
                    }
                    storyList.clear()
                    storyList.addAll(networkStories)
                    storyAdapter.notifyDataSetChanged()
                    dbHelper.upsertStories(networkStories.filter { !it.isAddButton })
                } catch (e: Exception) {
                    Log.e("StoriesFeed", "Error parsing response: ${e.message}")
                }
            },
            { error ->
                Log.e("StoriesFeed", "Network error: ${error.message}")
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf("Authorization" to "Bearer $token")
            }
        }
        rq.add(req)
    }

    private fun fetchPostFeed() {
        val token = sessionManager.getToken() ?: return
        val url = BuildConfig.BASE_URL + "get_feed.php"
        val rq = Volley.newRequestQueue(this)

        val req = object : StringRequest(Method.GET, url,
            { response ->
                try {
                    val obj = JSONObject(response.trim())
                    if (obj.optBoolean("success", false)) {
                        val postsArray = obj.optJSONArray("posts")
                        val fetchedPosts = mutableListOf<Post>()
                        if (postsArray != null) {
                            for (i in 0 until postsArray.length()) {
                                val postObj = postsArray.getJSONObject(i)
                                val postId = postObj.optString("postId")
                                val userId = postObj.optString("userId")
                                val postImageBase64 = postObj.optString("postImage", "")
                                val userPfpBase64 = postObj.optString("userProfileImage", "")

                                fetchedPosts.add(Post(
                                    postId = postId,
                                    userId = userId,
                                    postImage = saveImageToLocalCache(postImageBase64, "post_$postId"),
                                    caption = postObj.optString("caption"),
                                    timestamp = postObj.optLong("timestamp"),
                                    username = postObj.optString("username"),
                                    userProfileImage = saveImageToLocalCache(userPfpBase64, "pfp_$userId"),
                                    likesCount = postObj.optInt("likesCount"),
                                    commentsCount = postObj.optInt("commentsCount"),
                                    isLiked = postObj.optBoolean("isLiked")
                                ))
                            }
                        }
                        postList.clear()
                        postList.addAll(fetchedPosts)
                        postAdapter.notifyDataSetChanged()
                        dbHelper.upsertPosts(fetchedPosts)
                    }
                } catch (e: Exception) {
                    Log.e("PostFeed", "Error parsing response: ${e.message}")
                }
            },
            { error ->
                Log.e("PostFeed", "Network error: ${error.message}")
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf("Authorization" to "Bearer $token")
            }
        }
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

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private fun uploadStory(bitmap: Bitmap) {
        val token = sessionManager.getToken() ?: return
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos)
        val bitmapString = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
        val url = BuildConfig.BASE_URL + "upload_story.php"
        val rq = Volley.newRequestQueue(this)
        val req = object : StringRequest(Method.POST, url,
            { response ->
                try {
                    val obj = JSONObject(response.trim())
                    if (obj.optBoolean("success", false)) {
                        Toast.makeText(this@FifthActivity, "Story Uploaded!", Toast.LENGTH_SHORT).show()
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
                return hashMapOf("storyImage" to bitmapString, "timestamp" to System.currentTimeMillis().toString())
            }
            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf("Authorization" to "Bearer $token")
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
        findViewById<ImageView>(R.id.post_icon).setOnClickListener {
            startActivity(Intent(this, AddPostActivity::class.java))
        }
    }

    private fun fetchCurrentUserProfileForNav() {
        val token = sessionManager.getToken() ?: return
        val url = BuildConfig.BASE_URL + "get_profile.php"
        val rq = Volley.newRequestQueue(this)
        val req = object : StringRequest(Method.GET, url,
            { response ->
                try {
                    val obj = JSONObject(response.trim())
                    if (obj.optBoolean("success", false)) {
                        val userObj = obj.optJSONObject("user")
                        val imageBase64 = userObj?.optString("imageBase64", "") ?: ""
                        val profileIcon = findViewById<CircleImageView>(R.id.profile_icon)
                        val imagePath = saveImageToLocalCache(imageBase64, "pfp_${sessionManager.getUserId()}")
                        if (imagePath.isNotEmpty()) {
                            Picasso.get().load(File(imagePath)).placeholder(R.drawable.profile_image).into(profileIcon)
                        } else {
                            profileIcon.setImageResource(R.drawable.profile_image)
                        }
                    }
                } catch (e: Exception) {
                    // ignore
                }
            },
            { /* ignore */ }) {
            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf("Authorization" to "Bearer $token")
            }
        }
        rq.add(req)
    }
}
