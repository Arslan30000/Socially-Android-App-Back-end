package com.example.assigment_1
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity

class TwelfthActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.your_activity)
        val follow_btn: RelativeLayout= findViewById(R.id.following_tab)
        follow_btn.setOnClickListener {
            val intent = Intent(this, EleventhActivity::class.java)
            startActivity(intent)
            finish()
        }

        val searchBtn: ImageView = findViewById(R.id.search_icon)
        searchBtn.setOnClickListener {
            val intent = Intent(this, SeventhActivity::class.java)
            startActivity(intent)
            finish()
        }
        val homebtn: ImageView = findViewById(R.id.home_icon)
        homebtn.setOnClickListener {
            val intent = Intent(this, FifthActivity::class.java)
            startActivity(intent)
            finish()
        }
        val m_1_: RelativeLayout = findViewById(R.id.m_1)
        m_1_.setOnClickListener {
            val intent = Intent(this, NinthActivity::class.java)
            startActivity(intent)

        }
        val m_2_: RelativeLayout = findViewById(R.id.m_2)
        m_2_.setOnClickListener {
            val intent = Intent(this, NinthActivity::class.java)
            startActivity(intent)

        }
        val f: RelativeLayout= findViewById(R.id.follow_b)
        f.setOnClickListener {
            val intent = Intent(this, TwentyOneActivity::class.java)
            startActivity(intent)

        }
        val post_btn: ImageView = findViewById(R.id.post_icon)
        post_btn.setOnClickListener {
            val intent = Intent(this, FifteenthActivity::class.java)
            startActivity(intent)
            finish()
        }
        val P: de.hdodenhof.circleimageview.CircleImageView= findViewById(R.id.profile_icon)
        P.setOnClickListener {
            val intent = Intent(this, LastActivity::class.java)
            startActivity(intent)

        }




    }

}
