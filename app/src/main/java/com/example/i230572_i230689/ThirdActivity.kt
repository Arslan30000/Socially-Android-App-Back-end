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

    private lateinit var profileImg: ImageView
    private lateinit var usernameTxt: TextView
    private lateinit var auth: FirebaseAuth
    private lateinit var dbRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)

        profileImg = findViewById(R.id.profile_image_1)
        usernameTxt = findViewById(R.id.name_text)
        val nextBtn: ImageButton = findViewById(R.id.login_button)
        val switchBtn: TextView = findViewById(R.id.Switch_btn)
        val signupBtn: TextView = findViewById(R.id.sign_up_button)

        auth = FirebaseAuth.getInstance()
        dbRef = FirebaseDatabase.getInstance().getReference("users")

        val currentUser = auth.currentUser

        if (currentUser == null) {
            startActivity(Intent(this, FourthActivity::class.java))
            finish()
            return
        }

        dbRef.child(currentUser.uid).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val username = snapshot.child("username").value?.toString()
                val imageBase64 = snapshot.child("imageBase64").value?.toString()  // fixed key

                usernameTxt.text = username ?: "No Name"

                if (!imageBase64.isNullOrEmpty()) {
                    try {
                        val bytes = Base64.decode(imageBase64, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        profileImg.setImageBitmap(bitmap)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } else {
                usernameTxt.text = "No user data found"
            }
        }.addOnFailureListener {
            usernameTxt.text = "Failed to load"
        }

        nextBtn.setOnClickListener {
            startActivity(Intent(this, FifthActivity::class.java))
        }

        switchBtn.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, FourthActivity::class.java))
            finish()
        }

        signupBtn.setOnClickListener {
            startActivity(Intent(this, SecondActivity::class.java))
        }
    }
}
