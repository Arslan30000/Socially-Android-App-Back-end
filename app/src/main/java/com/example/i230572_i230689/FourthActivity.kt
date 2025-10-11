package com.example.i230572_i230689

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class FourthActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_2)

        auth = FirebaseAuth.getInstance()

        val backBtn: ImageButton = findViewById(R.id.back_button)
        val logBtn: TextView = findViewById(R.id.login_button)
        val signBtn: TextView = findViewById(R.id.sign_up_button)
        val forgotPass: TextView = findViewById(R.id.forgot_password)
        val usernameInput: EditText = findViewById(R.id.username_input)
        val passwordInput: EditText = findViewById(R.id.password_input)

        // Hide password input by default
        passwordInput.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        backBtn.setOnClickListener {
            startActivity(Intent(this, ThirdActivity::class.java))
            finish()
        }

        // 🔐 LOGIN BUTTON LOGIC
        logBtn.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val dbRef = FirebaseDatabase.getInstance().getReference("users")
            dbRef.orderByChild("username").equalTo(username)
                .get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        var found = false
                        for (userSnap in snapshot.children) {
                            val user = userSnap.getValue(User::class.java)
                            if (user != null && user.username == username && user.password == password) {
                                found = true
                                Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, FifthActivity::class.java))
                                finish()
                                break
                            }
                        }
                        if (!found) Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }


        signBtn.setOnClickListener {
            startActivity(Intent(this, SecondActivity::class.java))
        }


        forgotPass.setOnClickListener {
            showForgotPasswordDialog()
        }
    }

    private fun showForgotPasswordDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Reset Password")

        val emailInput = EditText(this)
        emailInput.hint = "Enter your email"
        builder.setView(emailInput)

        builder.setPositiveButton("Send") { _, _ ->
            val email = emailInput.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Email is required", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            auth.sendPasswordResetEmail(email)
                .addOnSuccessListener {
                    Toast.makeText(this, "Reset email sent to $email", Toast.LENGTH_LONG).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }

        builder.setNegativeButton("Cancel", null)
        builder.show()
    }
}

data class User(
    val username: String = "",
    val email: String = "",
    val password: String = ""
)
