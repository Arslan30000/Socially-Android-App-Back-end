package com.example.i230572_i230689
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.util.Base64
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import java.io.ByteArrayOutputStream
class SecondActivity : AppCompatActivity() {
    private lateinit var dbRef: DatabaseReference
    private var imageUri: Uri? = null
    private var isPasswordVisible = false
    private var imageBase64: String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sign_up)
        dbRef = FirebaseDatabase.getInstance().getReference("users")
        val backBtn: ImageButton = findViewById(R.id.back_arrow)
        val profileImg: ImageView = findViewById(R.id.profile_image)
        val usernameInput: EditText = findViewById(R.id.username_input)
        val nameInput: EditText = findViewById(R.id.name_input)
        val lastnameInput: EditText = findViewById(R.id.lastname_input)
        val dateInput: EditText = findViewById(R.id.date_input)
        val emailInput: EditText = findViewById(R.id.email_input)
        val passwordInput: EditText = findViewById(R.id.password_input)
        val passwordToggle: ImageView = findViewById(R.id.password_toggle)
        val signInIcon: ImageButton = findViewById(R.id.sign_in_icon)
        backBtn.setOnClickListener {
            startActivity(Intent(this, FourthActivity::class.java))
            finish()
        }
        profileImg.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, 101)
        }
        passwordToggle.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            passwordInput.inputType = if (isPasswordVisible)
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        signInIcon.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val name = nameInput.text.toString().trim()
            val lastname = lastnameInput.text.toString().trim()
            val date = dateInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            dbRef.orderByChild("username").equalTo(username)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            Toast.makeText(this@SecondActivity, "Username already taken!", Toast.LENGTH_SHORT).show()
                        } else {
                            val uid = dbRef.push().key!!
                            val newUser = User(
                                uid = uid,
                                username = username,
                                name = name,
                                lastname = lastname,
                                email = email,
                                password = password,
                                date = date,
                                imageBase64 = imageBase64,
                                followers = mutableMapOf(),
                                following = mutableMapOf(),
                                followRequests = mutableMapOf(),
                                posts = mutableMapOf(),
                                stories = mutableMapOf()
                            )
                            val userRef = dbRef.child(uid)
                            userRef.setValue(newUser)
                                .addOnSuccessListener {
                                    userRef.get().addOnSuccessListener { snapshot ->
                                        val updates = mutableMapOf<String, Any>()
                                        if (!snapshot.hasChild("followers")) updates["followers"] = mapOf<String, Boolean>()
                                        if (!snapshot.hasChild("following")) updates["following"] = mapOf<String, Boolean>()
                                        if (!snapshot.hasChild("followRequests")) updates["followRequests"] = mapOf<String, Boolean>()
                                        if (!snapshot.hasChild("posts")) updates["posts"] = mapOf<String, Boolean>()
                                        if (!snapshot.hasChild("stories")) updates["stories"] = mapOf<String, Boolean>()

                                        if (updates.isNotEmpty()) {
                                            userRef.updateChildren(updates)
                                                .addOnSuccessListener {
                                                    Toast.makeText(this@SecondActivity, "New fields added successfully!", Toast.LENGTH_SHORT).show()
                                                }
                                                .addOnFailureListener { e ->
                                                    Toast.makeText(this@SecondActivity, "Failed to add fields: ${e.message}", Toast.LENGTH_SHORT).show()
                                                }
                                        }
                                    }

                                    saveUserToPrefs(newUser)
                                    Toast.makeText(this@SecondActivity, "Account Created!", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this@SecondActivity, ThirdActivity::class.java))
                                    finish()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this@SecondActivity, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 101 && resultCode == Activity.RESULT_OK) {
            imageUri = data?.data
            val profileImg: ImageView = findViewById(R.id.profile_image)
            profileImg.setImageURI(imageUri)

            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
            imageBase64 = encodeToBase64(bitmap)
        }
    }

    private fun encodeToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
    private fun saveUserToPrefs(user: User) {
        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        prefs.edit().apply {
            putString("uid", user.uid)
            putString("username", user.username)
            putString("name", user.name)
            putString("lastname", user.lastname)
            putString("email", user.email)
            putString("date", user.date)
            putString("imageBase64", user.imageBase64)
            putString("followers", user.followers.keys.joinToString(","))
            putString("following", user.following.keys.joinToString(","))
            putString("followRequests", user.followRequests.keys.joinToString(","))
            putString("posts", user.posts.keys.joinToString(","))
            putString("stories", user.stories.keys.joinToString(","))
            apply()
        }
    }
}
