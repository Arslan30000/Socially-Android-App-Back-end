package com.example.i230572_i230689

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sm = SessionManager(this)

        Handler(Looper.getMainLooper()).postDelayed({
            if (sm.isLoggedIn()) {
                startActivity(Intent(this, ThirdActivity::class.java))
            } else {
                startActivity(Intent(this, FourthActivity::class.java))
            }
            finish()
        }, 5000)
    }
}
