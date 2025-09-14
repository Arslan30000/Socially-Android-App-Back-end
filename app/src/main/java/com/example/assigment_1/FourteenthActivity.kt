package com.example.assigment_1
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class FourteenthActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.edit_profile)
        val close_btn: TextView = findViewById(R.id.cancel_btn)
        close_btn.setOnClickListener {
            val intent = Intent(this, ThirteenActivity::class.java)
            startActivity(intent)
            finish()
        }
        val done_btn: TextView= findViewById(R.id.done_btn)
        done_btn.setOnClickListener {
            val intent = Intent(this, LastActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}

