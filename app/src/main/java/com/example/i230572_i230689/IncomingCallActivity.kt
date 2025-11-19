package com.example.i230572_i230689

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class IncomingCallActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incoming_call)

        val callerId = intent.getStringExtra("caller_id")
        val channelName = intent.getStringExtra("channel_name")
        val callType = intent.getStringExtra("call_type")

        findViewById<TextView>(R.id.caller_name).text = "Incoming ${callType ?: "video"} call..."

        findViewById<ImageButton>(R.id.accept_button).setOnClickListener {
            val intent = Intent(this, TenthActivity::class.java).apply {
                putExtra("channel_name", channelName)
                putExtra("call_type", callType)
            }
            startActivity(intent)
            finish()
        }

        findViewById<ImageButton>(R.id.decline_button).setOnClickListener {
            // Here you might want to send a notification to the caller that the call was declined.
            // For now, we just close the activity.
            finish()
        }
    }
}
