package com.example.i230572_i230689

import android.graphics.BitmapFactory
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
import de.hdodenhof.circleimageview.CircleImageView
import org.json.JSONObject

class NineteenActivity : AppCompatActivity() {

    // Views
    private lateinit var storyImageView: ImageView
    private lateinit var userProfileImageView: CircleImageView
    private lateinit var usernameTextView: TextView

    // Data and State
    private val storiesList = mutableListOf<Story>()
    private var currentStoryIndex = 0
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.my_story)

        sessionManager = SessionManager(this)

        // Initialize Views
        storyImageView = findViewById(R.id.main_image)
        userProfileImageView = findViewById(R.id.pfp_image)
        usernameTextView = findViewById(R.id.story_name)

        // Set up tap listeners for navigation
        val reverseView: View = findViewById(R.id.reverse_view)
        reverseView.setOnClickListener { showPreviousStory() }

        val skipView: View = findViewById(R.id.skip_view)
        skipView.setOnClickListener { showNextStory() }

        // Get User ID and fetch stories
        val userId = intent.getStringExtra("USER_ID")
        if (userId.isNullOrEmpty()) {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        fetchUserStories(userId)
    }

    private fun fetchUserStories(userId: String) {
        val token = sessionManager.getToken() ?: run { Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show(); finish(); return }
        val url = BuildConfig.BASE_URL + "get_stories.php?user_id=$userId"
        val rq = Volley.newRequestQueue(this)

        val req = object : StringRequest(Method.GET, url,
            { response ->
                try {
                    val obj = JSONObject(response.trim())
                    if (obj.optBoolean("success", false)) {
                        val storiesArray = obj.optJSONArray("stories")
                        storiesList.clear()
                        if (storiesArray != null) {
                            for (i in 0 until storiesArray.length()) {
                                val storyObj = storiesArray.getJSONObject(i)
                                val story = Story(
                                    userId = storyObj.optInt("userId", 0).toString(),
                                    username = storyObj.optString("username", "Unknown"),
                                    storyImage = storyObj.optString("storyImage", ""),
                                    userProfilePicture = storyObj.optString("userProfilePicture", ""),
                                    isAddButton = false
                                )
                                storiesList.add(story)
                            }
                        }

                        if (storiesList.isNotEmpty()) {
                            currentStoryIndex = 0
                            displayStory(currentStoryIndex)
                        } else {
                            Toast.makeText(this@NineteenActivity, "No stories found.", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    } else {
                        Toast.makeText(this@NineteenActivity, "No stories found for this user.", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this@NineteenActivity, "Error parsing stories", Toast.LENGTH_SHORT).show()
                    finish()
                }
            },
            { error ->
                Toast.makeText(this@NineteenActivity, "Failed to load stories.", Toast.LENGTH_SHORT).show()
                finish()
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

    /**
     * Displays the story at the given index in the storiesList.
     */
    private fun displayStory(index: Int) {
        if (index < 0 || index >= storiesList.size) return // Safety check

        val story = storiesList[index]

        // Update the UI elements
        usernameTextView.text = story.username
        decodeAndSetImage(story.userProfilePicture, userProfileImageView, R.drawable.profile_image)
        decodeAndSetImage(story.storyImage, storyImageView) // This is the main changing image
    }

    /**
     * Called when the left side of the screen is tapped.
     */
    private fun showPreviousStory() {
        if (currentStoryIndex > 0) {
            currentStoryIndex--
            displayStory(currentStoryIndex)
        }
    }

    /**
     * Called when the right side of the screen is tapped.
     */
    private fun showNextStory() {
        if (currentStoryIndex < storiesList.size - 1) {
            currentStoryIndex++
            displayStory(currentStoryIndex)
        } else {
            finish()
        }
    }

    private fun decodeAndSetImage(base64String: String?, imageView: ImageView, fallbackDrawable: Int? = null) {
        if (base64String.isNullOrEmpty()) {
            if (fallbackDrawable != null) imageView.setImageResource(fallbackDrawable)
            return
        }
        try {
            val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            imageView.setImageBitmap(bitmap)
        } catch (e: Exception) {
            Log.e("ImageDecodeError", "Failed to decode Base64 string: ", e)
            if (fallbackDrawable != null) imageView.setImageResource(fallbackDrawable)
        }
    }
}
