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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import de.hdodenhof.circleimageview.CircleImageView

class NineteenActivity : AppCompatActivity() {

    // Views
    private lateinit var storyImageView: ImageView
    private lateinit var userProfileImageView: CircleImageView
    private lateinit var usernameTextView: TextView

    // Data and State
    private val storiesList = mutableListOf<Story>()
    private var currentStoryIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.my_story)

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
        val storiesRef = FirebaseDatabase.getInstance().getReference("stories")
        // Query to get all stories by this user, ordered by time
        val query = storiesRef.orderByChild("userId").equalTo(userId)

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    storiesList.clear()
                    // Populate the list with all stories from the user
                    for (storySnapshot in snapshot.children) {
                        storySnapshot.getValue(Story::class.java)?.let { storiesList.add(it) }
                    }

                    if (storiesList.isNotEmpty()) {
                        // Start by showing the first story
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
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@NineteenActivity, "Failed to load stories.", Toast.LENGTH_SHORT).show()
                finish()
            }
        })
    }

    /**
     * Displays the story at the given index in the storiesList.
     */
    private fun displayStory(index: Int) {
        if (index < 0 || index >= storiesList.size) return // Safety check

        val story = storiesList[index]

        // Update the UI elements
        // The username and profile picture only need to be set once, but this is safer
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
        // If on the first story, tapping previous does nothing.
    }

    /**
     * Called when the right side of the screen is tapped.
     */
    private fun showNextStory() {
        if (currentStoryIndex < storiesList.size - 1) {
            // If there are more stories, show the next one
            currentStoryIndex++
            displayStory(currentStoryIndex)
        } else {
            // If it's the last story, close the activity
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
