package com.example.i230572_i230689

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.io.ByteArrayOutputStream
import java.util.UUID

class AddPostActivity : AppCompatActivity() {

    private lateinit var imagePreview: ImageView
    private lateinit var captionInput: EditText
    private var selectedImageUri: Uri? = null

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
        private const val TAG = "AddPostActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_post)

        imagePreview = findViewById(R.id.image_preview)
        captionInput = findViewById(R.id.caption_input)
        val postButton = findViewById<Button>(R.id.post_button)
        val closeButton = findViewById<ImageView>(R.id.close_button)

        closeButton.setOnClickListener {
            finish()
        }

        imagePreview.setOnClickListener {
            openGallery()
        }

        postButton.setOnClickListener {
            uploadPost()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            selectedImageUri = data.data
            imagePreview.setImageURI(selectedImageUri)
        }
    }

    private fun uploadPost() {
        val caption = captionInput.text.toString().trim()
        val imageUri = selectedImageUri

        if (imageUri == null) {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show()
            return
        }

        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser == null) {
            Toast.makeText(this, "Authentication failed. Please log in again.", Toast.LENGTH_LONG).show()
            return
        }
        val userId = firebaseUser.uid

        val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
        val imageBytes = byteArrayOutputStream.toByteArray()
        val imageBase64 = android.util.Base64.encodeToString(imageBytes, android.util.Base64.DEFAULT)

        val postId = UUID.randomUUID().toString()
        val post = Post(
            postId,
            userId,
            imageBase64,
            caption,
            System.currentTimeMillis()
        )

        FirebaseDatabase.getInstance().getReference("posts").child(postId).setValue(post)
            .addOnSuccessListener {
                Toast.makeText(this, "Post uploaded successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { exception ->
                // Log the detailed error message from Firebase
                Log.e(TAG, "Firebase upload failed", exception)
                Toast.makeText(this, "Upload failed: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }
}
