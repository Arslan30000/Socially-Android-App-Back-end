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

    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile_page)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("users")

        val uid = auth.currentUser?.uid
        if (uid != null) {
            loadUserData(uid)
        }


        val Searchbtn: ImageView = findViewById(R.id.search_icon)
        Searchbtn.setOnClickListener {
          startActivity(Intent(this, SixthActivity::class.java))
        }

        val like_btn: ImageView = findViewById(R.id.like_icon)
        like_btn.setOnClickListener {
            startActivity(Intent(this, EleventhActivity::class.java))
        }

        val create_post_btn: ImageView = findViewById(R.id.post_icon)
        create_post_btn.setOnClickListener {
            startActivity(Intent(this, FifteenthActivity::class.java))
        }

        val h: ImageView = findViewById(R.id.home_icon)
        h.setOnClickListener {
            startActivity(Intent(this, FifthActivity::class.java))
            finish()
        }

        val c: LinearLayout = findViewById(R.id.s_5)
        c.setOnClickListener {
            startActivity(Intent(this, SixteenActivity::class.java))
        }

        val e: RelativeLayout = findViewById(R.id.Following_)
        e.setOnClickListener {
            startActivity(Intent(this, FourteenthActivity::class.java))
        }
    }

    private fun loadUserData(uid: String) {
        database.child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                if (user != null) {
                    findViewById<TextView>(R.id.name).text = user.username
                    findViewById<TextView>(R.id.description).text = user.bio.ifEmpty { "No bio yet" }
                    findViewById<TextView>(R.id.no_2).text = user.followers.size.toString()
                    findViewById<TextView>(R.id.no_3).text = user.following.size.toString()

                    val profileImageView = findViewById<ImageView>(R.id.profile_main)
                    if (user.imageBase64.isNotEmpty()) {
                        try {
                            val bytes = Base64.decode(user.imageBase64, Base64.DEFAULT)
                            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            profileImageView.setImageBitmap(bmp)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }
}
