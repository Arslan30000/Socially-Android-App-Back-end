package com.example.i230572_i230689
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class SixteenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.camera_page)
        val cancel_btn: ImageView = findViewById(R.id.next)
        cancel_btn.setOnClickListener {
            val intent = Intent(this, FifthActivity::class.java)
            startActivity(intent)
            finish()
        }
        val capture_btn: de.hdodenhof.circleimageview.CircleImageView = findViewById(R.id.capture)
        capture_btn.setOnClickListener {
            val intent = Intent(this, EighteenActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}

