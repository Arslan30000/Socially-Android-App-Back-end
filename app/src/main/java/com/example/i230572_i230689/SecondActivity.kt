package com.example.i230572_i230689

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.Calendar

class SecondActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val dbRef = FirebaseDatabase.getInstance().getReference("users")

    private lateinit var usernameInput: EditText
    private lateinit var nameInput: EditText
    private lateinit var lastnameInput: EditText
    private lateinit var dateInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var profileImg: ImageView
    private lateinit var passwordToggle: ImageView
    private lateinit var signupBtn: ImageButton

    private var encodedImage: String = ""
    private var isPasswordVisible = false
    private val PICK_IMAGE_REQUEST = 1001
    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sign_up)

        auth = FirebaseAuth.getInstance()

        usernameInput = findViewById(R.id.username_input)
        nameInput = findViewById(R.id.name_input)
        lastnameInput = findViewById(R.id.lastname_input)
        dateInput = findViewById(R.id.date_input)
        emailInput = findViewById(R.id.email_input)
        passwordInput = findViewById(R.id.password_input)
        profileImg = findViewById(R.id.profile_image)
        passwordToggle = findViewById(R.id.password_toggle)
        signupBtn = findViewById(R.id.sign_in_icon)

        dateInput.setOnClickListener {
            val calendar = Calendar.getInstance()
            val picker = DatePickerDialog(
                this,
                { _, y, m, d -> dateInput.setText("$d/${m + 1}/$y") },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            picker.show()
        }

        passwordToggle.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            passwordInput.inputType =
                if (isPasswordVisible)
                    android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                else
                    android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            passwordToggle.setBackgroundResource(R.drawable.visibility)
            passwordInput.setSelection(passwordInput.text.length)
        }

        profileImg.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        signupBtn.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val name = nameInput.text.toString().trim()
            val lastname = lastnameInput.text.toString().trim()
            val dob = dateInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (username.isEmpty() || name.isEmpty() || lastname.isEmpty() ||
                dob.isEmpty() || email.isEmpty() || password.isEmpty()
            ) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (encodedImage.isEmpty()) {
                val defaultBitmap = BitmapFactory.decodeResource(resources, R.drawable.profile_image)
                encodedImage = encodeImage(defaultBitmap)
            }

            checkUsernameExists(username) { exists ->
                if (exists) {
                    Toast.makeText(this, "Username already taken", Toast.LENGTH_SHORT).show()
                } else {
                    registerUser(username, name, lastname, dob, email, password)
                }
            }
        }
    }

    private fun checkUsernameExists(username: String, callback: (Boolean) -> Unit) {
        dbRef.orderByChild("username").equalTo(username)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    callback(snapshot.exists())
                }
                override fun onCancelled(error: DatabaseError) {
                    callback(false)
                }
            })
    }

    private fun registerUser(username: String, name: String, lastname: String, dob: String, email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: return@addOnSuccessListener

                val newUser = mapOf(
                    "uid" to uid,
                    "username" to username,
                    "name" to name,
                    "lastname" to lastname,
                    "dob" to dob,
                    "email" to email,
                    "followers" to mutableMapOf<String, Boolean>(),
                    "following" to mutableMapOf<String, Boolean>(),
                    "imageBase64" to encodedImage
                )

                dbRef.child(uid).setValue(newUser)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Account created successfully", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, ThirdActivity::class.java))
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to save user data: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Sign up failed: ${it.message}", Toast.LENGTH_SHORT).show()
                Log.e("FirebaseSignUp", "Error: ${it.message}")
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.data
            try {
                val inputStream: InputStream? = contentResolver.openInputStream(selectedImageUri!!)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                profileImg.setImageBitmap(bitmap)
                encodedImage = encodeImage(bitmap)
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
                Log.e("ImagePicker", "Error: ${e.message}")
            }
        }
    }

    private fun encodeImage(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos)
        val bytes = baos.toByteArray()
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }
}
