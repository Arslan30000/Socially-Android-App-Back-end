package com.example.i230572_i230689

import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.*
import android.os.Bundle
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import de.hdodenhof.circleimageview.CircleImageView

class TwentyActivity : AppCompatActivity() {

    private lateinit var profileImg: CircleImageView
    private lateinit var nameTxt: TextView
    private lateinit var descTxt: TextView
    private lateinit var followersTxt: TextView
    private lateinit var followingTxt: TextView
    private lateinit var followBtn: TextView

    private lateinit var dbRef: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile_f_)

        auth = FirebaseAuth.getInstance()
        dbRef = FirebaseDatabase.getInstance().getReference("users")

        profileImg = findViewById(R.id.profile_main)
        nameTxt = findViewById(R.id.name)
        descTxt = findViewById(R.id.description_2)
        followersTxt = findViewById(R.id.no_2)
        followingTxt = findViewById(R.id.no_3)
        followBtn = findViewById(R.id.fo)

        val userId = intent.getStringExtra("userId")
        val currentUid = auth.currentUser?.uid ?: return
        val finalUserId = userId ?: currentUid

        loadUserProfile(finalUserId, currentUid)

        findViewById<ImageView>(R.id.search_icon).setOnClickListener {
            startActivity(Intent(this, SixthActivity::class.java))
        }

        findViewById<ImageView>(R.id.like_icon).setOnClickListener {
            startActivity(Intent(this, EleventhActivity::class.java))
        }

        findViewById<ImageView>(R.id.post_icon).setOnClickListener {
            startActivity(Intent(this, FifteenthActivity::class.java))
        }

        findViewById<RelativeLayout>(R.id.message).setOnClickListener {
            startActivity(Intent(this, NinthActivity::class.java))
        }

        findViewById<ImageView>(R.id.home_icon).setOnClickListener {
            startActivity(Intent(this, FifthActivity::class.java))
            finish()
        }
    }

    private fun loadUserProfile(userId: String, currentUid: String) {
        dbRef.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return

                val username = snapshot.child("username").getValue(String::class.java) ?: "Unknown"
                val description = snapshot.child("bio").getValue(String::class.java) ?: ""
                val imageBase64 = snapshot.child("imageBase64").getValue(String::class.java)
                val followersCount = snapshot.child("followers").childrenCount
                val followingCount = snapshot.child("following").childrenCount

                nameTxt.text = username
                descTxt.text = description
                followersTxt.text = followersCount.toString()
                followingTxt.text = followingCount.toString()

                if (!imageBase64.isNullOrEmpty()) {
                    val bytes = Base64.decode(imageBase64, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    profileImg.setImageBitmap(bitmap)
                } else {
                    profileImg.setImageResource(R.drawable.profile_image)
                }

                if (userId == currentUid) {
                    followBtn.visibility = Button.GONE
                } else {
                    checkFollowStatus(currentUid, userId)
                    followBtn.setOnClickListener {
                        followUser(currentUid, userId)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun checkFollowStatus(currentUid: String, targetUid: String) {
        dbRef.child(currentUid).child("following").child(targetUid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        followBtn.text = "Following"
                        followBtn.isEnabled = false
                    } else {
                        followBtn.text = "Follow"
                        followBtn.isEnabled = true
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun followUser(currentUid: String, targetUid: String) {
        val updates = hashMapOf<String, Any>(
            "/$currentUid/following/$targetUid" to true,
            "/$targetUid/followers/$currentUid" to true
        )

        dbRef.updateChildren(updates).addOnSuccessListener {
            followBtn.text = "Following"
            followBtn.isEnabled = false
            Toast.makeText(this, "Now following this user", Toast.LENGTH_SHORT).show()
        }
    }
}
