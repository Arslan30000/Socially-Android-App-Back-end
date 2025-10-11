package com.example.i230572_i230689

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ThirdActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: DatabaseReference
    private lateinit var profileImg: ImageView
    private lateinit var usernameTxt: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)

        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance().getReference("users")

        profileImg = findViewById(R.id.profile_image_1)
        usernameTxt = findViewById(R.id.name_text)
        val nextbtn: ImageButton = findViewById(R.id.login_button)
        val switchBtn: TextView = findViewById(R.id.Switch_btn)
        val signupBtn: TextView = findViewById(R.id.sign_up_button)

        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        val imageBase64 = prefs.getString("imageBase64", null)
        val intentUsername = getSharedPreferences("user_session", MODE_PRIVATE).getString("username", null)


        if (!intentUsername.isNullOrEmpty()) {
            usernameTxt.text = intentUsername
        }

        if (!imageBase64.isNullOrEmpty()) {
            val imageBytes = Base64.decode(imageBase64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            profileImg.setImageBitmap(bitmap)
        }

        if (intentUsername.isNullOrEmpty()) {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                db.orderByChild("email").equalTo(currentUser.email)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            for (userSnap in snapshot.children) {
                                val user = userSnap.getValue(User::class.java)
                                if (user != null) {
                                    usernameTxt.text = user.username
                                    if (!user.imageBase64.isNullOrEmpty()) {
                                        profileImg.setImageResource(R.drawable.profile_image)
                                        val bytes = Base64.decode(user.imageBase64, Base64.DEFAULT)
                                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                        profileImg.setImageBitmap(bitmap)
                                    }
                                }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {}
                    })
            }
        }

        nextbtn.setOnClickListener {
            startActivity(Intent(this, FifthActivity::class.java))
        }

        switchBtn.setOnClickListener {
            startActivity(Intent(this, FourthActivity::class.java))
        }

        signupBtn.setOnClickListener {
            startActivity(Intent(this, SecondActivity::class.java))
        }
    }
}
