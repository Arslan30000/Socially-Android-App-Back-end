package com.example.i230572_i230689
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class EightAvtivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.chat)

        val Chatbtn: RelativeLayout = findViewById(R.id.p1)
        Chatbtn.setOnClickListener {
            val intent = Intent(this, NinthActivity::class.java)
            startActivity(intent)

        }
        val P: TextView = findViewById(R.id.username)
        P.setOnClickListener {
            val intent = Intent(this, LastActivity::class.java)
            startActivity(intent)

        }
        val Chatbtn_2: RelativeLayout = findViewById(R.id.p2)
        Chatbtn_2.setOnClickListener {
            val intent = Intent(this, NinthActivity::class.java)
            startActivity(intent)

        }
        val Chatbtn_3: RelativeLayout = findViewById(R.id.p3)
        Chatbtn_3.setOnClickListener {
            val intent = Intent(this, NinthActivity::class.java)
            startActivity(intent)

        }
        val Chatbtn_4: RelativeLayout = findViewById(R.id.p4)
        Chatbtn_4.setOnClickListener {
            val intent = Intent(this, NinthActivity::class.java)
            startActivity(intent)

        }
        val Chatbtn_5: RelativeLayout = findViewById(R.id.p5)
        Chatbtn_5.setOnClickListener {
            val intent = Intent(this, NinthActivity::class.java)
            startActivity(intent)

        }
        val Chatbtn_6: RelativeLayout = findViewById(R.id.p6)
        Chatbtn_6.setOnClickListener {
            val intent = Intent(this, NinthActivity::class.java)
            startActivity(intent)

        }

        val Back_btn: ImageView= findViewById(R.id.back_icon)
        Back_btn.setOnClickListener {
            val intent = Intent(this, FifthActivity::class.java)
            startActivity(intent)
            finish()
        }
        val profile_f: ImageView= findViewById(R.id.profile_1)
        profile_f.setOnClickListener {
            val intent = Intent(this, SeventeenActivity::class.java)
            startActivity(intent)

        }
        val profile_f_4: ImageView= findViewById(R.id.profile_4)
        profile_f_4.setOnClickListener {
            val intent = Intent(this, SeventeenActivity::class.java)
            startActivity(intent)

        }
        val profile_f_5: ImageView= findViewById(R.id.profile_5)
        profile_f_5.setOnClickListener {
            val intent = Intent(this, SeventeenActivity::class.java)
            startActivity(intent)

        }
        val profile_f_6: ImageView= findViewById(R.id.profile_6)
        profile_f_6.setOnClickListener {
            val intent = Intent(this, SeventeenActivity::class.java)
            startActivity(intent)

        }
        val camera_1: ImageView= findViewById(R.id.camera_1)
        camera_1.setOnClickListener {
            val intent = Intent(this, SixteenActivity::class.java)
            startActivity(intent)

        }
        val camera_2: ImageView= findViewById(R.id.camera_2)
        camera_2.setOnClickListener {
            val intent = Intent(this, SixteenActivity::class.java)
            startActivity(intent)

        }
        val camera_3: ImageView= findViewById(R.id.camera_3)
        camera_3.setOnClickListener {
            val intent = Intent(this, SixteenActivity::class.java)
            startActivity(intent)

        }
        val camera_4: ImageView= findViewById(R.id.camera_4)
        camera_4.setOnClickListener {
            val intent = Intent(this, SixteenActivity::class.java)
            startActivity(intent)

        }
        val camera_5: ImageView= findViewById(R.id.camera_5)
        camera_5.setOnClickListener {
            val intent = Intent(this, SixteenActivity::class.java)
            startActivity(intent)

        }
        val camera_6: ImageView= findViewById(R.id.camera_6)
        camera_1.setOnClickListener {
            val intent = Intent(this, SixteenActivity::class.java)
            startActivity(intent)

        }
        val camera_7: ImageView= findViewById(R.id.camera_icon)
        camera_7.setOnClickListener {
            val intent = Intent(this, SixteenActivity::class.java)
            startActivity(intent)

        }





    }
}