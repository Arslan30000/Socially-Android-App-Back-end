package com.example.i230572_i230689
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.os.Handler
import android.os.Looper
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        Handler(Looper.getMainLooper()).postDelayed({
            val currentUser = auth.currentUser

            if (currentUser != null) {
                val intent = Intent(this, ThirdActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                val intent = Intent(this, FourthActivity::class.java)
                startActivity(intent)
                finish()
            }
        }, 5000)
    }
}