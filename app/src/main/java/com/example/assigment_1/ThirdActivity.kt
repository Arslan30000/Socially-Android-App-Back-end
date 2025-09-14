package com.example.assigment_1
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ThirdActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)
        val backBtn: TextView = findViewById(R.id.sign_up_button)
        backBtn.setOnClickListener {
            val intent = Intent(this, SecondActivity::class.java)
            startActivity(intent)
        }
        val nextbtn: ImageButton = findViewById(R.id.login_button)
        nextbtn.setOnClickListener {
            val intent = Intent(this, FifthActivity::class.java)
            startActivity(intent)

        }
        val btn3: TextView=findViewById(R.id.Switch_btn)
        btn3.setOnClickListener {
            val intent = Intent(this, FourthActivity::class.java)
            startActivity(intent)

        }

    }
}