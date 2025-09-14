package com.example.assigment_1
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
class TwentyActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile_f)
        val Searchbtn: ImageView = findViewById(R.id.search_icon)
        Searchbtn.setOnClickListener {
            val intent = Intent(this, SixthActivity::class.java)
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
        val m: RelativeLayout = findViewById(R.id.message)
        m.setOnClickListener {
            val intent = Intent(this, NinthActivity::class.java)
            startActivity(intent)

        }
        val h_1: LinearLayout= findViewById(R.id.s_1)
        h_1.setOnClickListener {
            val intent = Intent(this, ThirteenActivity::class.java)
            startActivity(intent)

        }
        val h_2: LinearLayout= findViewById(R.id.s_2)
        h_2.setOnClickListener {
            val intent = Intent(this, ThirteenActivity::class.java)
            startActivity(intent)

        }
        val h_3: LinearLayout= findViewById(R.id.s_3)
        h_3.setOnClickListener {
            val intent = Intent(this, ThirteenActivity::class.java)
            startActivity(intent)

        }
        val h_4: LinearLayout= findViewById(R.id.s_4)
        h_4.setOnClickListener {
            val intent = Intent(this, ThirteenActivity::class.java)
            startActivity(intent)

        }
        val h: ImageView = findViewById(R.id.home_icon)
        h.setOnClickListener {
            val intent = Intent(this, FifthActivity::class.java)
            startActivity(intent)
            finish()

        }
    }
}

