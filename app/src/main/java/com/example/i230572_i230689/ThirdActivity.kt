package com.example.i230572_i230689
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
class ThirdActivity : AppCompatActivity() {
    private lateinit var profileImg: ImageView
    private lateinit var usernameTxt: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)
        profileImg = findViewById(R.id.profile_image_1)
        usernameTxt = findViewById(R.id.name_text)
        val nextbtn: ImageButton = findViewById(R.id.login_button)
        val switchBtn: TextView = findViewById(R.id.Switch_btn)
        val signupBtn: TextView = findViewById(R.id.sign_up_button)
        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        val username = prefs.getString("username", null)
        val imageBase64 = prefs.getString("imageBase64", null)
        usernameTxt.text = username
        if (!imageBase64.isNullOrEmpty()) {
            val bytes = Base64.decode(imageBase64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            profileImg.setImageBitmap(bitmap)
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
