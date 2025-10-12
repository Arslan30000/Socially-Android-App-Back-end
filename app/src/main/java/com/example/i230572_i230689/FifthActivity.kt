package com.example.i230572_i230689
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.i230572_i230689.*
import com.google.firebase.database.*
import com.google.firebase.auth.FirebaseAuth
import android.util.*

class FifthActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_page)
        val db = FirebaseDatabase.getInstance().getReference("users")
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            db.child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val followers = snapshot.child("followers").value
                    val following = snapshot.child("following").value
                    val followRequests = snapshot.child("followRequests").value
                    val posts = snapshot.child("posts").value
                    val stories = snapshot.child("stories").value

                    Log.d("FirebaseData", "Followers: $followers")
                    Log.d("FirebaseData", "Following: $following")
                    Log.d("FirebaseData", "FollowRequests: $followRequests")
                    Log.d("FirebaseData", "Posts: $posts")
                    Log.d("FirebaseData", "Stories: $stories")
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseError", error.message)
                }
            })
        } else {
            Log.e("FirebaseError", "User not logged in")
        }
        val Searchbtn: ImageView = findViewById(R.id.search_icon)
        Searchbtn.setOnClickListener {
            val intent = Intent(this, SixthActivity::class.java)
            startActivity(intent)

        }

        val Profile_btn: de.hdodenhof.circleimageview.CircleImageView = findViewById(R.id.profile_icon)
        Profile_btn.setOnClickListener {
            val intent = Intent(this, LastActivity::class.java)
            startActivity(intent)

        }
        val messagebtn: ImageView = findViewById(R.id.share)
        messagebtn.setOnClickListener {
            val intent = Intent(this, EightAvtivity::class.java)
            startActivity(intent)

        }
        val like_btn: ImageView = findViewById(R.id.like_icon)
        like_btn.setOnClickListener {
            val intent = Intent(this, EleventhActivity::class.java)
            startActivity(intent)

        }
        val create_post_btn: ImageView = findViewById(R.id.post_icon)
        create_post_btn.setOnClickListener {
            val intent = Intent(this, FifteenthActivity::class.java)
            startActivity(intent)

        }
        val camera_btn: ImageView = findViewById(R.id.camera)
        camera_btn.setOnClickListener {
            val intent = Intent(this, SixteenActivity::class.java)
            startActivity(intent)

        }
        val story_btn_1: LinearLayout  = findViewById(R.id.s_1)
        story_btn_1.setOnClickListener {
            val intent = Intent(this, NineteenActivity::class.java)
            startActivity(intent)

        }
        val story_btn_2: LinearLayout  = findViewById(R.id.s_2)
        story_btn_2.setOnClickListener {
            val intent = Intent(this, SeventeenActivity::class.java)
            startActivity(intent)

        }
        val story_btn_3: LinearLayout  = findViewById(R.id.s_3)
        story_btn_3.setOnClickListener {
            val intent = Intent(this, SeventeenActivity::class.java)
            startActivity(intent)

        }
        val story_btn_4: LinearLayout  = findViewById(R.id.s_4)
        story_btn_4.setOnClickListener {
            val intent = Intent(this, SeventeenActivity::class.java)
            startActivity(intent)

        }
        val story_btn_5: LinearLayout = findViewById(R.id.s_5)
        story_btn_5.setOnClickListener {
            val intent = Intent(this, SeventeenActivity::class.java)
            startActivity(intent)

        }
        val story_btn_6: LinearLayout = findViewById(R.id.s_6)
        story_btn_6.setOnClickListener {
            val intent = Intent(this, SeventeenActivity::class.java)
            startActivity(intent)

        }
        val story_btn_7: LinearLayout = findViewById(R.id.s_7)
        story_btn_7.setOnClickListener {
            val intent = Intent(this, SeventeenActivity::class.java)
            startActivity(intent)

        }

    }
}
