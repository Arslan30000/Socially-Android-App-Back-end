package com.example.i230572_i230689

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.io.ByteArrayOutputStream

class FourteenthActivity : AppCompatActivity() {

    private lateinit var nameField: EditText
    private lateinit var usernameField: EditText
    private lateinit var bioField: EditText
    private lateinit var emailField: EditText
    private lateinit var profilePic: de.hdodenhof.circleimageview.CircleImageView

    private lateinit var auth: FirebaseAuth
    private val db = FirebaseDatabase.getInstance().getReference("users")

    private var encodedImage = ""
    private val PICK_IMAGE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.edit_profile)

        nameField = findViewById(R.id.name_field)
        usernameField = findViewById(R.id.username_field)
        bioField = findViewById(R.id.bio_field)
        emailField = findViewById(R.id.email_field)
        profilePic = findViewById(R.id.profile_pic)

        auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return

        loadUserData(uid)

        findViewById<TextView>(R.id.cancel_btn).setOnClickListener {
            startActivity(Intent(this, LastActivity::class.java))
            finish()
        }

        findViewById<TextView>(R.id.done_btn).setOnClickListener {
            saveChanges(uid)
        }

        findViewById<TextView>(R.id.change_photo).setOnClickListener {
            pickImageFromGallery()
        }
    }

    private fun loadUserData(uid: String) {
        db.child(uid).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val name = snapshot.child("name").value?.toString() ?: ""
                val username = snapshot.child("username").value?.toString() ?: ""
                val email = snapshot.child("email").value?.toString() ?: ""
                val bio = snapshot.child("bio").value?.toString() ?: ""
                val imageBase64 = snapshot.child("imageBase64").value?.toString() ?: ""

                nameField.setText(name)
                usernameField.setText(username)
                emailField.setText(email)
                bioField.setText(bio)

                if (imageBase64.isNotEmpty()) {
                    val bytes = Base64.decode(imageBase64, Base64.DEFAULT)
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    profilePic.setImageBitmap(bmp)
                    encodedImage = imageBase64
                }
            }
        }
    }

    private fun saveChanges(uid: String) {
        val name = nameField.text.toString().trim()
        val username = usernameField.text.toString().trim()
        val email = emailField.text.toString().trim()
        val bio = bioField.text.toString().trim()

        if (name.isEmpty() || username.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        val updates = mapOf(
            "name" to name,
            "username" to username,
            "email" to email,
            "bio" to bio,
            "imageBase64" to encodedImage
        )

        db.child(uid).updateChildren(updates).addOnSuccessListener {
            Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LastActivity::class.java))
            finish()
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to update", Toast.LENGTH_SHORT).show()
        }
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            val imageUri: Uri? = data?.data
            imageUri?.let {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, it)
                profilePic.setImageBitmap(bitmap)
                encodedImage = encodeImage(bitmap)
            }
        }
    }

    private fun encodeImage(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
    }
}
