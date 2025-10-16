package com.example.i230572_i230689

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class FourthActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var dbRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_2)

        auth = FirebaseAuth.getInstance()
        dbRef = FirebaseDatabase.getInstance().getReference("users")

        val usernameInput = findViewById<EditText>(R.id.username_input)
        val passInput = findViewById<EditText>(R.id.password_input)
        val loginBtn = findViewById<TextView>(R.id.login_button)
        val signupBtn = findViewById<TextView>(R.id.sign_up_button)
        val forgotBtn = findViewById<TextView>(R.id.forgot_password)

        loginBtn.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val password = passInput.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            dbRef.orderByChild("username").equalTo(username)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            for (userSnap in snapshot.children) {
                                val userData = userSnap.getValue(User::class.java)
                                val email = userData?.email
                                if (email != null) {
                                    auth.signInWithEmailAndPassword(email, password)
                                        .addOnSuccessListener {
                                            startActivity(Intent(this@FourthActivity, ThirdActivity::class.java))
                                            finish()
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(this@FourthActivity, "Login failed: ${it.message}", Toast.LENGTH_SHORT).show()
                                        }
                                } else {
                                    Toast.makeText(this@FourthActivity, "Email not found for this username", Toast.LENGTH_SHORT).show()
                                }
                                return
                            }
                        } else {
                            Toast.makeText(this@FourthActivity, "Username not found", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@FourthActivity, "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        }

        signupBtn.setOnClickListener {
            startActivity(Intent(this, SecondActivity::class.java))
        }

        forgotBtn.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            if (username.isEmpty()) {
                Toast.makeText(this, "Enter your username to reset password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            dbRef.orderByChild("username").equalTo(username)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            for (userSnap in snapshot.children) {
                                val email = userSnap.child("email").getValue(String::class.java)
                                if (email != null) {
                                    auth.sendPasswordResetEmail(email)
                                        .addOnSuccessListener {
                                            Toast.makeText(this@FourthActivity, "Password reset email sent", Toast.LENGTH_SHORT).show()
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(this@FourthActivity, "Failed to send reset email: ${it.message}", Toast.LENGTH_SHORT).show()
                                        }
                                }
                                return
                            }
                        } else {
                            Toast.makeText(this@FourthActivity, "Username not found", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }
    }


}
