package com.example.i230572_i230689

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class FifthActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_page)

        // Setup for Buttons and Icons (your existing code)
        setupClickListeners()

        // Setup for the new Stories RecyclerView
        setupStoriesRecyclerView()

        // Fetch data from Firebase (your existing code)
        fetchFirebaseData()
    }

    private fun setupStoriesRecyclerView() {
        // 1. Find the RecyclerView in your layout
        val storyRecyclerView: RecyclerView = findViewById(R.id.story_recycler_view)

        // 2. Create some sample data
        //    (In the future, you can replace this with data fetched from Firebase)
        val stories = listOf(
            Story("Your Story", ""), // You can use a real image URL or resource ID
            Story("karenne", ""),
            Story("zackjohn", ""),
            Story("kieron_d", ""),
            Story("craig_love", ""),
            Story("max_b", ""),
            Story("another_user", "")
        )

        // 3. Create and set the adapter for the RecyclerView
        val storyAdapter = StoryAdapter(stories)
        storyRecyclerView.adapter = storyAdapter

        // 4. Set the Layout Manager to make it a horizontal list
        //    (This is also set in the XML, but setting it here ensures it)
        storyRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
    }

    private fun setupClickListeners() {
        val searchBtn: ImageView = findViewById(R.id.search_icon)
        searchBtn.setOnClickListener {
            startActivity(Intent(this, SixthActivity::class.java))
        }

        val profileBtn: de.hdodenhof.circleimageview.CircleImageView = findViewById(R.id.profile_icon)
        profileBtn.setOnClickListener {
            startActivity(Intent(this, LastActivity::class.java))
        }

        val messageBtn: ImageView = findViewById(R.id.share)
        messageBtn.setOnClickListener {
            startActivity(Intent(this, EightAvtivity::class.java))
        }

        val likeBtn: ImageView = findViewById(R.id.like_icon)
        likeBtn.setOnClickListener {
            startActivity(Intent(this, EleventhActivity::class.java))
        }

        val createPostBtn: ImageView = findViewById(R.id.post_icon)
        createPostBtn.setOnClickListener {
            startActivity(Intent(this, FifteenthActivity::class.java))
        }

        val cameraBtn: ImageView = findViewById(R.id.camera)
        cameraBtn.setOnClickListener {
            startActivity(Intent(this, SixteenActivity::class.java))
        }
    }

    private fun fetchFirebaseData() {
        val db = FirebaseDatabase.getInstance().getReference("users")
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            db.child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val followers = snapshot.child("followers").value
                    val following = snapshot.child("following").value
                    val followRequests = snapshot.child("followRequests").value
                    val posts = snapshot.child("posts").value
                    val stories = snapshot.child("stories").value

                    Log.d("FirebaseData", "Followers: $followers")
                    Log.d("FirebaseData", "Following: $following")
                    Log.d("FirebaseData", "FollowRequests: $followRequests")
                    Log.d("FirebaseData", "Posts: $posts")
                    Log.d("FirebaseData", "Stories: $stories")
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseError", error.message)
                }
            })
        } else {
            Log.e("FirebaseError", "User not logged in")
        }
    }
}
