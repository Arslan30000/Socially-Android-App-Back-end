package com.example.i230572_i230689

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class FourthActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_2)

        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance().getReference("users")

        val usernameInput = findViewById<EditText>(R.id.username_input)
        val passInput = findViewById<EditText>(R.id.password_input)
        val loginBtn = findViewById<TextView>(R.id.login_button)
        val signupBtn = findViewById<TextView>(R.id.sign_up_button)

        loginBtn.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val password = passInput.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            db.orderByChild("username").equalTo(username)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            for (userSnap in snapshot.children) {
                                val user = userSnap.getValue(User::class.java)
                                if (user != null && user.password == password) {
                                    val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
                                    prefs.edit().apply {
                                        putString("username", user.username)
                                        putString("name", user.name)
                                        putString("lastname", user.lastname)
                                        putString("email", user.email)
                                        putString("date", user.date)
                                        putString("imageBase64", user.imageBase64)
                                        apply()
                                    }
                                    startActivity(Intent(this@FourthActivity, ThirdActivity::class.java))
                                    finish()
                                    return
                                }
                            }
                            Toast.makeText(this@FourthActivity, "Invalid credentials", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@FourthActivity, "User not found", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }

        signupBtn.setOnClickListener {
            startActivity(Intent(this, SecondActivity::class.java))
        }
    }
}
