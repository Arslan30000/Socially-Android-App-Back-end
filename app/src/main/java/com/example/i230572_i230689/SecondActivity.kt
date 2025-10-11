package com.example.i230572_i230689

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class SecondActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var dbRef: DatabaseReference
    private var imageUri: Uri? = null
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sign_up)

        auth = FirebaseAuth.getInstance()
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
            if (isPasswordVisible) {
                passwordInput.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                passwordToggle.setImageResource(R.drawable.visibility)
            } else {
                passwordInput.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                passwordToggle.setImageResource(R.drawable.visibility)
            }
            passwordInput.setSelection(passwordInput.text.length)
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
                            Toast.makeText(
                                this@SecondActivity,
                                "Username already taken!",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            val uid = auth.currentUser?.uid ?: dbRef.push().key!!
                            val userMap = mapOf(
                                "username" to username,
                                "name" to name,
                                "lastname" to lastname,
                                "date" to date,
                                "email" to email,
                                "password" to password,
                                "imageUrl" to (imageUri?.toString() ?: "")
                            )
                            dbRef.child(uid).setValue(userMap)
                            Toast.makeText(this@SecondActivity, "Account Created!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@SecondActivity, FourthActivity::class.java))
                            finish()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@SecondActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 101 && resultCode == Activity.RESULT_OK) {
            imageUri = data?.data
            val profileImg: ImageView = findViewById(R.id.profile_image)
            profileImg.setImageURI(imageUri)
        }
    }
}
