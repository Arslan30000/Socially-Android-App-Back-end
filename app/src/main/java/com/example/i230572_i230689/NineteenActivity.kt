package com.example.i230572_i230689

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class NineteenActivity : AppCompatActivity() {

    private lateinit var storyImageView: ImageView
    private lateinit var userProfileImageView: CircleImageView
    private lateinit var usernameTextView: TextView

    private val storiesList = mutableListOf<Story>()
    private var currentStoryIndex = 0
    private lateinit var sessionManager: SessionManager
    private lateinit var dbHelper: LocalDbHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.my_story)

        sessionManager = SessionManager(this)
        dbHelper = LocalDbHelper(this)

        storyImageView = findViewById(R.id.main_image)
        userProfileImageView = findViewById(R.id.pfp_image)
        usernameTextView = findViewById(R.id.story_name)

        findViewById<View>(R.id.reverse_view).setOnClickListener { showPreviousStory() }
        findViewById<View>(R.id.skip_view).setOnClickListener { showNextStory() }

        val userId = intent.getStringExtra("USER_ID")
        if (userId.isNullOrEmpty()) {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadStories(userId)
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

    private fun loadStories(userId: String) {
        val cachedStories = dbHelper.getStories().filter { it.userId == userId }
        if (cachedStories.isNotEmpty()) {
            storiesList.clear()
            storiesList.addAll(cachedStories)
            displayStory(0)
            Log.d("NineteenActivity", "Loaded ${cachedStories.size} stories from cache.")
        }

        if (isOnline()) {
            fetchUserStoriesFromNetwork(userId)
        } else if (cachedStories.isEmpty()) {
            Toast.makeText(this, "You are offline and no stories are cached.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun fetchUserStoriesFromNetwork(userId: String) {
        val token = sessionManager.getToken() ?: return
        val url = BuildConfig.BASE_URL + "get_stories.php?user_id=$userId"
        val rq = Volley.newRequestQueue(this)

        val req = object : StringRequest(Method.GET, url,
            { response ->
                try {
                    val obj = JSONObject(response.trim())
                    if (obj.optBoolean("success", false)) {
                        val storiesArray = obj.optJSONArray("stories")
                        val networkStories = mutableListOf<Story>()
                        if (storiesArray != null) {
                            for (i in 0 until storiesArray.length()) {
                                val storyObj = storiesArray.getJSONObject(i)
                                val storyId = storyObj.optString("id")
                                val storyImageBase64 = storyObj.optString("storyImage", "")
                                val userPfpBase64 = storyObj.optString("userProfilePicture", "")

                                networkStories.add(Story(
                                    storyId = storyId,
                                    userId = storyObj.optString("userId"),
                                    username = storyObj.optString("username", "Unknown"),
                                    storyImage = saveImageToLocalCache(storyImageBase64, "story_$storyId"),
                                    userProfilePicture = saveImageToLocalCache(userPfpBase64, "pfp_$userId"),
                                    timestamp = storyObj.optLong("timestamp", 0L)
                                ))
                            }
                        }

                        if (networkStories.isNotEmpty()) {
                            storiesList.clear()
                            storiesList.addAll(networkStories)
                            currentStoryIndex = 0
                            displayStory(currentStoryIndex)
                            dbHelper.upsertStories(networkStories)
                        } else {
                            if (storiesList.isEmpty()) { // Only show if no cached stories were shown
                                Toast.makeText(this@NineteenActivity, "No stories found.", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            { error ->
                Log.e("NineteenActivity", "Network error: ${error.message}")
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf("Authorization" to "Bearer $token")
            }
        }
        req.retryPolicy = DefaultRetryPolicy(15000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        rq.add(req)
    }

    private fun displayStory(index: Int) {
        if (index < 0 || index >= storiesList.size) return

        val story = storiesList[index]
        usernameTextView.text = story.username

        if (story.userProfilePicture?.isNotEmpty() == true) {
            Picasso.get().load(File(story.userProfilePicture)).placeholder(R.drawable.profile_image).into(userProfileImageView)
        } else {
            userProfileImageView.setImageResource(R.drawable.profile_image)
        }

        if (story.storyImage.isNotEmpty()) {
            Picasso.get().load(File(story.storyImage)).into(storyImageView)
        }
    }

    private fun showPreviousStory() {
        if (currentStoryIndex > 0) {
            currentStoryIndex--
            displayStory(currentStoryIndex)
        }
    }

    private fun showNextStory() {
        if (currentStoryIndex < storiesList.size - 1) {
            currentStoryIndex++
            displayStory(currentStoryIndex)
        } else {
            finish()
        }
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
}
