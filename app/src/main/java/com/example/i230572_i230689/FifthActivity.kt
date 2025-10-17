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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.ByteArrayOutputStream

class FifthActivity : AppCompatActivity() {

    private lateinit var recyclerViewStory: RecyclerView
    private lateinit var storyAdapter: StoryAdapter
    private lateinit var storyList: MutableList<Story>
    private lateinit var auth: FirebaseAuth

    // ActivityResultLauncher for picking an image from the gallery
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val imageUri = result.data?.data
            if (imageUri != null) {
                // User has selected an image, now we need to process and upload it
                val inputStream = contentResolver.openInputStream(imageUri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                uploadStory(bitmap)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_page)

        auth = FirebaseAuth.getInstance()
        setupRecyclerView()
        setupBottomNavigationBar()

        // Fetch user data and then set up the stories
        fetchUserDataAndSetupStories()
    }

    private fun setupRecyclerView() {
        recyclerViewStory = findViewById(R.id.recycler_view_story)
        recyclerViewStory.setHasFixedSize(true)
        recyclerViewStory.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        storyList = mutableListOf()
        // Pass the galleryLauncher to the adapter
        storyAdapter = StoryAdapter(this, storyList) {
            openGallery()
        }
        recyclerViewStory.adapter = storyAdapter
    }

    private fun fetchUserDataAndSetupStories() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e("FifthActivity", "User not logged in.")
            // Optionally, redirect to login screen
            return
        }

        val userId = currentUser.uid
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(userId)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val username = snapshot.child("name").getValue(String::class.java) ?: "User"
                // Fetch the profile picture bitmap string
                val pfpBitmapString = snapshot.child("imageBase64").getValue(String::class.java)

                // The first item is always the user's "add story" button
                val userStory = Story(
                    userId = userId,
                    username = username, // Use fetched username
                    userProfilePicture = pfpBitmapString, // Pass the PFP bitmap string
                    isAddButton = true
                )
                storyList.add(userStory)

                // TODO: Fetch stories from other users that are less than 24 hours old
                // For now, we just update the adapter with the user's story circle
                storyAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Failed to load user data: ${error.message}")
                // Add a default story item even if the fetch fails
                storyList.add(Story(userId = userId, username = "Your Story", isAddButton = true))
                storyAdapter.notifyDataSetChanged()
            }
        })
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private fun uploadStory(bitmap: Bitmap) {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos) // Compress the image
        val byteArray = baos.toByteArray()
        val bitmapString = Base64.encodeToString(byteArray, Base64.DEFAULT)

        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid
        val storyRef = FirebaseDatabase.getInstance().getReference("stories")
        val storyId = storyRef.push().key ?: ""

        val newStory = Story(
            storyId = storyId,
            userId = userId,
            storyImage = bitmapString, // The bitmap string of the story image
            timestamp = System.currentTimeMillis()
        )

        storyRef.child(storyId).setValue(newStory).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Story Uploaded!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Upload failed.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupBottomNavigationBar() {
        // --- Your existing code for button listeners ---
        val searchBtn: ImageView = findViewById(R.id.search_icon)
        searchBtn.setOnClickListener { startActivity(Intent(this, SixthActivity::class.java)) }

        val profileBtn: de.hdodenhof.circleimageview.CircleImageView = findViewById(R.id.profile_icon)
        profileBtn.setOnClickListener { startActivity(Intent(this, LastActivity::class.java)) }

        val messageBtn: ImageView = findViewById(R.id.share)
        messageBtn.setOnClickListener { startActivity(Intent(this, EightActivity::class.java)) }

        val likeBtn: ImageView = findViewById(R.id.like_icon)
        likeBtn.setOnClickListener { startActivity(Intent(this, EleventhActivity::class.java)) }

        val createPostBtn: ImageView = findViewById(R.id.post_icon)
        createPostBtn.setOnClickListener { startActivity(Intent(this, FifteenthActivity::class.java)) }

        val cameraBtn: ImageView = findViewById(R.id.camera)
        cameraBtn.setOnClickListener { startActivity(Intent(this, SixteenActivity::class.java)) }
    }
}
