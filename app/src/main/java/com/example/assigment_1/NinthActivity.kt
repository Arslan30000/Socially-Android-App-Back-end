package com.example.assigment_1
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
class NinthActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.chat_page)
        val Call_btn: ImageView = findViewById(R.id.video_icon)
        Call_btn.setOnClickListener {
            val intent = Intent(this, TenthActivity::class.java)
            startActivity(intent)
            finish()
        }
        val Back_btn: ImageView = findViewById(R.id.back_icon)
        Back_btn.setOnClickListener {
            val intent = Intent(this, EightAvtivity::class.java)
            startActivity(intent)
            finish()
        }
        val camera_btn: ImageView = findViewById(R.id.camera_icon)
        camera_btn.setOnClickListener {
            val intent = Intent(this, SixteenActivity::class.java)
            startActivity(intent)
        }
        val Profile_btn: ImageView = findViewById(R.id.profile_pic)
        Profile_btn.setOnClickListener {
            val intent = Intent(this, TwentyActivity::class.java)
            startActivity(intent)
        }
    }



}