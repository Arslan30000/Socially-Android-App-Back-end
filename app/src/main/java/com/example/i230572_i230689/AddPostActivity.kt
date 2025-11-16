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
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import java.util.HashMap
import java.io.ByteArrayOutputStream

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

        val sessionManager = SessionManager(this)
        val token = sessionManager.getToken()
        if (token == null) {
            Toast.makeText(this, "Authentication failed. Please log in again.", Toast.LENGTH_LONG).show()
            return
        }

        val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
        val imageBytes = byteArrayOutputStream.toByteArray()
        val imageBase64 = android.util.Base64.encodeToString(imageBytes, android.util.Base64.DEFAULT)

            val url = BuildConfig.BASE_URL + "upload_post.php"
        val rq = Volley.newRequestQueue(this)

        val req = object : StringRequest(Method.POST, url,
            { response ->
                try {
                    // try parse minimal response
                    Toast.makeText(this, "Post uploaded successfully", Toast.LENGTH_SHORT).show()
                    finish()
                } catch (e: Exception) {
                    Log.e(TAG, "Upload parse error", e)
                    Toast.makeText(this, "Invalid server response", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e(TAG, "Network error: ${error.message}")
                Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_LONG).show()
            }) {

            override fun getParams(): MutableMap<String, String> {
                val map = HashMap<String, String>()
                map["postImage"] = imageBase64
                map["caption"] = caption
                map["timestamp"] = System.currentTimeMillis().toString()
                return map
            }

            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer $token"
                headers["Content-Type"] = "application/x-www-form-urlencoded"
                return headers
            }
        }

        req.retryPolicy = DefaultRetryPolicy(30000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        rq.add(req)
    }
}
