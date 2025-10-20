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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import de.hdodenhof.circleimageview.CircleImageView
import java.io.ByteArrayOutputStream

class FifthActivity : AppCompatActivity() {

    private lateinit var recyclerViewStory: RecyclerView
    private lateinit var storyAdapter: StoryAdapter
    private val storyList = mutableListOf<Story>()

    private lateinit var auth: FirebaseAuth
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener

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
        // Make sure your main layout file is named main_page.xml or change this
        setContentView(R.layout.main_page)

        auth = FirebaseAuth.getInstance()

        // Setup UI components
        setupRecyclerView()
        setupBottomNavigationBar()

        // Initialize and start listening for authentication changes
        // This is the key to preventing the redirect issue.
        setupAuthStateListener()
    }

    private fun setupAuthStateListener() {
        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user == null) {
                // USER IS NOT LOGGED IN
                Log.d("AUTH_STATE", "User is null, redirecting to login.")
                val intent = Intent(this, MainActivity::class.java) // Change to your login Activity if different
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish() // Close this activity
            } else {
                // USER IS CONFIRMED TO BE LOGGED IN
                // Now it's safe to load data associated with the user
                Log.d("AUTH_STATE", "User is logged in: ${user.uid}. Fetching data.")
                fetchStoriesForFeed(user.uid)
            }
        }
    }

    private fun setupRecyclerView() {
        recyclerViewStory = findViewById(R.id.recycler_view_story)
        recyclerViewStory.setHasFixedSize(true)
        recyclerViewStory.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        storyAdapter = StoryAdapter(this, storyList) {
            // This lambda is called when the "Add Story" item is clicked
            openGallery()
        }
        recyclerViewStory.adapter = storyAdapter
    }
    private fun fetchStoriesForFeed(currentUserId: String) {
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUserId)

        // 1. Fetch current user's data to create the base "Add Story" button
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val username = snapshot.child("name").getValue(String::class.java) ?: "User"
                val pfpBitmapString = snapshot.child("profilePicture").getValue(String::class.java)

                // This is the base object for the leftmost story circle.
                // It will be updated later if the user has an existing story.
                val userAddStory = Story(
                    userId = currentUserId,
                    username = "Your Story",
                    userProfilePicture = pfpBitmapString,
                    isAddButton = true // This will be set to 'false' if we find a story
                )

                // Get the list of users the current user is following
                val followingIds = mutableListOf<String>()
                snapshot.child("following").children.forEach {
                    it.key?.let { id -> followingIds.add(id) }
                }

                // DO NOT add currentUserId here anymore. We handle it separately.

                // 2. Fetch stories from followed users AND the current user to process them
                fetchStoriesFromFollowing(userAddStory, followingIds, currentUserId)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FIREBASE_ERROR", "Failed to load current user data: ${error.message}")
            }
        })
    }
    private fun fetchStoriesFromFollowing(
        userAddStory: Story, // This is the base "Your Story" object
        followingIds: List<String>,
        currentUserId: String
    ) {
        val storiesRef = FirebaseDatabase.getInstance().getReference("stories")
        storiesRef.orderByChild("timestamp")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val followedUserStories = mutableMapOf<String, Story>()
                    var currentUserHasStories = false // Start with false

                    for (storySnapshot in snapshot.children) {
                        val story = storySnapshot.getValue(Story::class.java) ?: continue

                        // Check if the story belongs to the current user
                        if (story.userId == currentUserId) {
                            currentUserHasStories = true
                        }
                        // Check if the story belongs to a followed user
                        else if (story.userId in followingIds) {
                            followedUserStories[story.userId] = story
                        }
                    }

                    // --- KEY LOGIC CHANGE ---
                    // If the user has no stories, we'll clear the userId on the "Add" button
                    // so the adapter knows not to make it clickable to view stories.
                    if (!currentUserHasStories) {
                        userAddStory.userId = "" // Clear the ID if no stories exist
                    }
                    // If they DO have stories, userAddStory.userId (which is the currentUserId) remains.

                    // Prepare the final list
                    storyList.clear()
                    storyList.add(userAddStory) // Add the "Your Story" circle
                    storyList.addAll(followedUserStories.values.sortedByDescending { it.timestamp })

                    storyAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FIREBASE_ERROR", "Failed to load stories: ${error.message}")
                }
            })
    }

    //region Activity Lifecycle
    override fun onStart() {
        super.onStart()
        // Start listening for auth changes when the activity becomes visible
        auth.addAuthStateListener(authStateListener)
    }

    override fun onStop() {
        super.onStop()
        // Stop listening when the activity is not visible to save resources
        if (::authStateListener.isInitialized) {
            auth.removeAuthStateListener(authStateListener)
        }
    }
    //endregion

    //region User Actions
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private fun uploadStory(bitmap: Bitmap) {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos)
        val bitmapString = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)

        val storiesRef = FirebaseDatabase.getInstance().getReference("stories")
        val storyId = storiesRef.push().key ?: ""

        // Fetch current user details to attach to the story
        FirebaseDatabase.getInstance().getReference("users").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val username = snapshot.child("name").getValue(String::class.java) ?: "User"
                    val userPfp = snapshot.child("profilePicture").getValue(String::class.java) ?: ""

                    val newStory = Story(
                        storyId = storyId,
                        userId = userId,
                        username = username,
                        userProfilePicture = userPfp,
                        storyImage = bitmapString,
                        timestamp = System.currentTimeMillis()
                    )

                    storiesRef.child(storyId).setValue(newStory).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this@FifthActivity, "Story Uploaded!", Toast.LENGTH_SHORT).show()
                            // Refresh the feed to show the new story circle
                            fetchStoriesForFeed(userId)
                        } else {
                            Toast.makeText(this@FifthActivity, "Upload failed.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@FifthActivity, "Failed to get user details for story.", Toast.LENGTH_SHORT).show()
                }
            })
    }
    //endregion

    //region Navigation
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
            startActivity(Intent(this, FifteenthActivity::class.java))
        }
        findViewById<ImageView>(R.id.camera).setOnClickListener {
            startActivity(Intent(this, SixteenActivity::class.java))
        }
    }
    //endregion
}
