package com.example.i230572_i230689
import android.content.Intent
import android.os.Bundle
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity

class SeventhActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.searching)
        val closeBtn: RelativeLayout = findViewById(R.id.close_button)
        closeBtn.setOnClickListener {
            val intent = Intent(this, SixthActivity::class.java)
            startActivity(intent)
            finish()
        }


    }
}
