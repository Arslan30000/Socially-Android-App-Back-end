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

    // --- 1. ADD THESE NEW VARIABLES FOR THE POST FEED ---
    private lateinit var postsRecyclerView: RecyclerView
    private lateinit var postAdapter: PostAdapter
    private val postList = mutableListOf<Post>()

    private lateinit var auth: FirebaseAuth

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
        setContentView(R.layout.main_page) // Or activity_fifth.xml

        auth = FirebaseAuth.getInstance()

        // --- THIS IS THE NEW, SIMPLIFIED AUTH CHECK ---
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // No user is logged in, redirect to login screen immediately.
            Log.d("FifthActivity", "User is null on create. Redirecting to login.")
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish() // Important: finish this activity
            return // Stop further execution of onCreate
        }

        // --- If we reach here, the user is definitely logged in ---
        Log.d("FifthActivity", "User ${currentUser.uid} is logged in. Setting up UI.")
        setupRecyclerView() // For stories
        setupBottomNavigationBar()

        // Setup Posts RecyclerView
        postsRecyclerView = findViewById(R.id.posts_recyclerview)
        postsRecyclerView.layoutManager = LinearLayoutManager(this)
        postAdapter = PostAdapter(this, postList)
        postsRecyclerView.adapter = postAdapter

        // Data will be fetched in onResume, so no need to call it here.
    }

    override fun onResume() {
        super.onResume()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Fetch fresh data every time the activity is resumed
            fetchStoriesForFeed(currentUser.uid)
            fetchPostFeed()
        } else {
            // This is a safety check in case the user gets logged out while the app is in the background
            Log.d("FifthActivity", "User is null on resume. Redirecting to login.")
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
            // This lambda is called when the "Add Story" item is clicked
            openGallery()
        }
        recyclerViewStory.adapter = storyAdapter
    }
    private fun fetchStoriesForFeed(currentUserId: String) {
        val database = FirebaseDatabase.getInstance().reference

        // First, get the current user's profile for the "Your Story" button.
        database.child("users").child(currentUserId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(userSnapshot: DataSnapshot) {
                val currentUser = userSnapshot.getValue(User::class.java)
                if (currentUser == null) {
                    Log.e("FIREBASE", "Current user not found.")
                    return
                }

                // Now, fetch all stories
                database.child("stories").orderByChild("timestamp").addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(storiesSnapshot: DataSnapshot) {
                        val allStories = storiesSnapshot.children.mapNotNull { it.getValue(Story::class.java) }

                        val userHasStories = allStories.any { it.userId == currentUserId }

                        // Create the "Your Story" circle with the correct PFP
                        val yourStoryCircle = Story(
                            userId = if (userHasStories) currentUserId else "",
                            username = "Your Story",
                            // USE THE EXISTING FIELD with the image from the User object
                            userProfilePicture = currentUser.imageBase64,
                            isAddButton = true
                        )

                        // Get the latest story from each user you follow
                        val followingIds = currentUser.following.keys
                        val followedStories = allStories
                            .filter { followingIds.contains(it.userId) }
                            .groupBy { it.userId }
                            .map { it.value.maxByOrNull { story -> story.timestamp }!! }

                        // Match usernames to get their profile pictures
                        matchStoriesToUserProfiles(yourStoryCircle, followedStories)
                    }
                    override fun onCancelled(error: DatabaseError) {
                        Log.e("FIREBASE", "Failed to get stories: ${error.message}")
                    }
                })
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("FIREBASE", "Failed to get current user: ${error.message}")
            }
        })
    }

    private fun matchStoriesToUserProfiles(yourStoryCircle: Story, followedStories: List<Story>) {
        val usersRef = FirebaseDatabase.getInstance().reference.child("users")
        val usernamesToFetch = followedStories.map { it.username }.distinct()

        if (usernamesToFetch.isEmpty()) {
            // No stories from followed users, just show your own circle
            storyList.clear()
            storyList.add(yourStoryCircle)
            storyAdapter.notifyDataSetChanged()
            return
        }

        // Query to find all users whose username is in our list
        usersRef.orderByChild("username").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(usersSnapshot: DataSnapshot) {
                val userProfileMap = mutableMapOf<String, User>()
                for (userSnap in usersSnapshot.children) {
                    val user = userSnap.getValue(User::class.java)
                    if (user != null && usernamesToFetch.contains(user.username)) {
                        userProfileMap[user.username] = user
                    }
                }

                // Now, combine the data
                val finalStoryList = followedStories.map { story ->
                    val userProfile = userProfileMap[story.username]
                    if (userProfile != null) {
                        // --- THE FIX ---
                        // Populate the EXISTING 'userProfilePicture' field with the image from the User object
                        story.userProfilePicture = userProfile.imageBase64
                    }
                    story
                }

                // Update the RecyclerView
                storyList.clear()
                storyList.add(yourStoryCircle) // Add your own circle first
                storyList.addAll(finalStoryList.sortedByDescending { it.timestamp }) // Then add the rest
                storyAdapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("FIREBASE", "Failed to match user profiles: ${error.message}")
            }
        })
    }
    private fun fetchPostFeed() {
        val postsRef = FirebaseDatabase.getInstance().getReference("posts")

        // 1. Fetch all posts, ordered by timestamp to get the newest first (by sorting later)
        postsRef.orderByChild("timestamp").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(postsSnapshot: DataSnapshot) {
                // THE FIX: Capture the snapshot key as the postId
                val allPosts = postsSnapshot.children.mapNotNull { dataSnapshot ->
                    dataSnapshot.getValue(Post::class.java)?.apply {
                        postId = dataSnapshot.key ?: ""
                    }
                }

                if (allPosts.isEmpty()) {
                    Log.d("PostFeed", "No posts found in database.")
                    postList.clear()
                    postAdapter.notifyDataSetChanged()
                    return // No posts to show
                }

                // 2. Get the unique user IDs from all the posts to fetch their profiles efficiently
                val userIds = allPosts.map { it.userId }.distinct()
                val usersRef = FirebaseDatabase.getInstance().getReference("users")
                val userProfiles = mutableMapOf<String, User>()
                var usersFetched = 0

                // 3. Fetch the profile for each unique user who made a post
                userIds.forEach { userId ->
                    usersRef.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(userSnapshot: DataSnapshot) {
                            userSnapshot.getValue(User::class.java)?.let { userProfiles[userId] = it }
                            usersFetched++

                            // 4. Once all required user profiles are fetched, combine the data
                            if (usersFetched == userIds.size) {
                                val finalFeed = allPosts.map { post ->
                                    val author = userProfiles[post.userId]
                                    if (author != null) {
                                        // Populate the post object with the author's username and profile picture
                                        post.username = author.username
                                        post.userProfileImage = author.imageBase64
                                    }
                                    post // return the modified post
                                }

                                // Update the RecyclerView on the main UI thread
                                postList.clear()
                                postList.addAll(finalFeed.sortedByDescending { it.timestamp })
                                postAdapter.notifyDataSetChanged()
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            usersFetched++ // Increment anyway to prevent the process from getting stuck
                            Log.e("PostFeed", "Failed to fetch profile for user: $userId", error.toException())
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("PostFeed", "Failed to fetch posts: ${error.message}", error.toException())
            }
        })
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
                    val username = snapshot.child("username").getValue(String::class.java) ?: "User"
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
        val addPostIcon = findViewById<ImageView>(R.id.post_icon)
        addPostIcon.setOnClickListener {
            val intent = Intent(this, AddPostActivity::class.java)
            intent.putExtra("USER_ID", auth.currentUser?.uid)
            startActivity(intent)
        }
    }
    //endregion
}
