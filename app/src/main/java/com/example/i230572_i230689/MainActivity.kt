package com.example.i230572_i230689

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    private lateinit var db: DatabaseReference
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = FirebaseDatabase.getInstance().getReference("users")
        prefs = getSharedPreferences("user_session", MODE_PRIVATE)

        Handler(Looper.getMainLooper()).postDelayed({
            val savedUsername = prefs.getString("username", null)

            if (!savedUsername.isNullOrEmpty()) {
                db.orderByChild("username").equalTo(savedUsername)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {
                                for (userSnap in snapshot.children) {
                                    val user = userSnap.getValue(User::class.java)
                                    if (user != null) {
                                        // Save full user data in prefs
                                        prefs.edit().apply {
                                            putString("username", user.username)
                                            putString("name", user.name)
                                            putString("lastname", user.lastname)
                                            putString("email", user.email)
                                            putString("date", user.date)
                                            putString("imageBase64", user.imageBase64)
                                            apply()
                                        }
                                        startActivity(Intent(this@MainActivity, ThirdActivity::class.java))
                                        finish()
                                        return
                                    }
                                }
                            } else {
                                goToLogin()
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            goToLogin()
                        }
                    })
            } else {
                goToLogin()
            }
        }, 5000)
    }

    private fun goToLogin() {
        startActivity(Intent(this, FourthActivity::class.java))
        finish()
    }
}
