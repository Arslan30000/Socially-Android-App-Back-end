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
                                      updateUserStructure(user)
                                        saveUserToPrefs(user)
                                        startActivity(Intent(this@MainActivity, ThirdActivity::class.java))
                                        finish()
                                        return
                                    }
                                }
                            } else goToLogin()
                        }
                        override fun onCancelled(error: DatabaseError) {
                            goToLogin()
                        }
                    })
            } else goToLogin()
        }, 2000)
    }

    private fun goToLogin() {
        startActivity(Intent(this, FourthActivity::class.java))
        finish()
    }
    private fun saveUserToPrefs(user: User) {
        val editor = prefs.edit()
        editor.putString("uid", user.uid)
        editor.putString("username", user.username)
        editor.putString("name", user.name)
        editor.putString("lastname", user.lastname)
        editor.putString("email", user.email)
        editor.putString("date", user.date)
        editor.putString("imageBase64", user.imageBase64)
        editor.putString("followers", user.followers.keys.joinToString(","))
        editor.putString("following", user.following.keys.joinToString(","))
        editor.putString("followRequests", user.followRequests.keys.joinToString(","))
        editor.putString("posts", user.posts.keys.joinToString(","))
        editor.putString("stories", user.stories.keys.joinToString(","))
        editor.apply()
    }
    private fun updateUserStructure(user: User) {
        val updates = mutableMapOf<String, Any>()
        if (user.followers.isEmpty()) updates["followers"] = mutableMapOf<String, Boolean>()
        if (user.following.isEmpty()) updates["following"] = mutableMapOf<String, Boolean>()
        if (user.followRequests.isEmpty()) updates["followRequests"] = mutableMapOf<String, Boolean>()
        if (user.posts.isEmpty()) updates["posts"] = mutableMapOf<String, Any>()
        if (user.stories.isEmpty()) updates["stories"] = mutableMapOf<String, Any>()
        if (updates.isNotEmpty()) {
            db.child(user.uid).updateChildren(updates)
        }
    }
}
