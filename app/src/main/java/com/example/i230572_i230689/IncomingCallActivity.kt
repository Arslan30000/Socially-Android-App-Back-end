package com.example.i230572_i230689

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase

class IncomingCallActivity : AppCompatActivity() {

    private lateinit var callerId: String
    private lateinit var channelName: String
    private lateinit var callType: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incoming_call)

        callerId = intent.getStringExtra("caller_id") ?: ""
        channelName = intent.getStringExtra("channel_name") ?: ""
        callType = intent.getStringExtra("call_type") ?: "video"

        findViewById<TextView>(R.id.caller_name).text = "Incoming ${callType.capitalize()} Call"

        findViewById<ImageButton>(R.id.accept_button).setOnClickListener {
            acceptCall()
        }

        findViewById<ImageButton>(R.id.decline_button).setOnClickListener {
            declineCall()
        }
    }

    private fun acceptCall() {
        val intent = Intent(this, TenthActivity::class.java).apply {
            putExtra("channel_name", channelName)
            putExtra("call_type", callType)
        }
        startActivity(intent)
        finish()
    }

    private fun declineCall() {
        // Notify the caller that the call was declined
        val callRef = FirebaseDatabase.getInstance().getReference("calls/$callerId")
        callRef.child("status").setValue("declined")
        finish()
    }
}
