package com.example.assigment_1
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class EleventhActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.recent_activity)
        val homebtn: ImageView = findViewById(R.id.home_icon)
        homebtn.setOnClickListener {
            val intent = Intent(this, FifthActivity::class.java)
            startActivity(intent)
            finish()
        }

        val you_button: TextView= findViewById(R.id.tab_you)
        you_button.setOnClickListener {
            val intent = Intent(this, TwelfthActivity::class.java)
            startActivity(intent)
            finish()
        }
        val post_btn: ImageView = findViewById(R.id.post_icon)
        post_btn.setOnClickListener {
            val intent = Intent(this, FifteenthActivity::class.java)
            startActivity(intent)
            finish()
        }
        val P: ImageView = findViewById(R.id.search_icon)
        P.setOnClickListener {
            val intent = Intent(this, SeventhActivity::class.java)
            startActivity(intent)
            finish()
        }
        val P_: de.hdodenhof.circleimageview.CircleImageView = findViewById(R.id.profile_icon)
        P_.setOnClickListener {
            val intent = Intent(this, LastActivity::class.java)
            startActivity(intent)

        }
        val P_1: de.hdodenhof.circleimageview.CircleImageView = findViewById(R.id.p_1)
        P_1.setOnClickListener {
            val intent = Intent(this, SeventeenActivity::class.java)
            startActivity(intent)

        }
        val P_2: de.hdodenhof.circleimageview.CircleImageView = findViewById(R.id.p_2)
        P_2.setOnClickListener {
            val intent = Intent(this, SeventeenActivity::class.java)
            startActivity(intent)

        }

    }

}
