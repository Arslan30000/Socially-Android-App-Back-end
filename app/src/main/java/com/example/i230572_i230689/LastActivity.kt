package com.example.i230572_i230689

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class LastActivity : AppCompatActivity() {

    private lateinit var db: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile_page)

        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance().getReference("users")

        val uid = auth.currentUser?.uid
        if (uid != null) loadUserData(uid)

        findViewById<ImageView>(R.id.search_icon).setOnClickListener {
            startActivity(Intent(this, SixthActivity::class.java))
        }

        findViewById<ImageView>(R.id.like_icon).setOnClickListener {
            startActivity(Intent(this, EleventhActivity::class.java))
        }

        findViewById<ImageView>(R.id.post_icon).setOnClickListener {
            startActivity(Intent(this, FifteenthActivity::class.java))
        }

        findViewById<ImageView>(R.id.home_icon).setOnClickListener {
            startActivity(Intent(this, FifthActivity::class.java))
            finish()
        }

        findViewById<LinearLayout>(R.id.s_5).setOnClickListener {
            startActivity(Intent(this, SixteenActivity::class.java))
        }

        findViewById<RelativeLayout>(R.id.Following_).setOnClickListener {
            startActivity(Intent(this, FourteenthActivity::class.java))
        }
        findViewById<TextView>(R.id.no_2).setOnClickListener {
            val intent = Intent(this, FollowersListActivity::class.java)
            intent.putExtra("type", "followers")
            startActivity(intent)
        }
        findViewById<TextView>(R.id.no_3).setOnClickListener {
            val intent = Intent(this, FollowersListActivity::class.java)
            intent.putExtra("type", "following")
            startActivity(intent)
        }

    }

    private fun loadUserData(uid: String) {
        db.child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return

                val username = snapshot.child("username").value?.toString() ?: "Unknown"
                val bio = snapshot.child("bio").value?.toString() ?: "No bio yet"
                val followers = snapshot.child("followers").childrenCount.toInt()
                val following = snapshot.child("following").childrenCount.toInt()
                val imageBase64 = snapshot.child("imageBase64").value?.toString() ?: ""
                val posts = snapshot.child("posts").childrenCount.toInt()

                findViewById<TextView>(R.id.name).text = username
                findViewById<TextView>(R.id.description).text = bio
                findViewById<TextView>(R.id.no_2).text = followers.toString()
                findViewById<TextView>(R.id.no_3).text = following.toString()
                findViewById<TextView>(R.id.no_1).text = posts.toString()


                if (imageBase64.isNotEmpty()) {
                    try {
                        val bytes = Base64.decode(imageBase64, Base64.DEFAULT)
                        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        findViewById<ImageView>(R.id.profile_main).setImageBitmap(bmp)
                        findViewById<ImageView>(R.id.profile_icon).setImageBitmap(bmp)
                    } catch (_: Exception) {}
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
