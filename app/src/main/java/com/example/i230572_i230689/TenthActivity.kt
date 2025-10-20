package com.example.i230572_i230689

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.SurfaceView
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import io.agora.rtc2.*
import io.agora.rtc2.video.VideoCanvas

class TenthActivity : AppCompatActivity() {

    private var rtcEngine: RtcEngine? = null

    private val appId = "3c8d97fe7f6043af99d0c97bf0eaf75c"
    private val channelName = "test_channel"
    private val token: String? = "95dbece7dd15473a8317e1510f80c38f"
    private val uid = 0

    private var isVideoCall = true
    private var otherUserId = ""
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private lateinit var remoteVideoView: FrameLayout
    private lateinit var localVideoView: FrameLayout
    private lateinit var audioLayout: LinearLayout
    private lateinit var callerName: TextView
    private lateinit var callerPhoto: ImageView
    private lateinit var declineBtn: ImageView
    private lateinit var soundBtn: ImageView
    private lateinit var optionsBtn: ImageView
    private lateinit var callTimer: TextView

    private var startTime = 0L
    private val handler = Handler(Looper.getMainLooper())
    private var timerRunning = false

    private lateinit var callRef: DatabaseReference
    private var callListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.call_page)

        remoteVideoView = findViewById(R.id.remote_video_view)
        localVideoView = findViewById(R.id.local_video_view)
        audioLayout = findViewById(R.id.audio_layout)
        callerName = findViewById(R.id.caller_name)
        callerPhoto = findViewById(R.id.caller_photo)
        declineBtn = findViewById(R.id.decline)
        soundBtn = findViewById(R.id.sound)
        optionsBtn = findViewById(R.id.options_5)
        callTimer = findViewById(R.id.time)

        isVideoCall = intent.getBooleanExtra("isVideoCall", true)
        otherUserId = intent.getStringExtra("otherUserId") ?: ""
        val uname = intent.getStringExtra("username") ?: "Unknown"
        val imgBase64 = intent.getStringExtra("imageBase64") ?: ""

        callerName.text = uname
        if (imgBase64.isNotEmpty()) {
            try {
                val bytes = Base64.decode(imgBase64, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                callerPhoto.setImageBitmap(bmp)
            } catch (_: Exception) {}
        }

        if (checkPermissions()) initAgora()
        else ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, 1)

        declineBtn.setOnClickListener { endCall() }
        setupFirebaseCallSignal()
    }

    private fun setupFirebaseCallSignal() {
        callRef = FirebaseDatabase.getInstance().getReference("calls")

        val callData = mapOf(
            "callerId" to currentUserId,
            "receiverId" to otherUserId,
            "isVideo" to isVideoCall,
            "status" to "ongoing"
        )
        val callId = if (currentUserId < otherUserId)
            "${currentUserId}_$otherUserId" else "${otherUserId}_$currentUserId"

        callRef.child(callId).setValue(callData)

        callListener = callRef.child(callId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val status = snapshot.child("status").getValue(String::class.java)
                if (status == "ended") {
                    endCall()
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun loadUserInfoFromFirebase(userId: String) {
        val dbRef = FirebaseDatabase.getInstance().getReference("users").child(userId)
        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return
                val uname = snapshot.child("username").getValue(String::class.java)
                    ?: snapshot.child("name").getValue(String::class.java)
                    ?: "Unknown"
                val imgBase64 = snapshot.child("profileImage").getValue(String::class.java)
                    ?: snapshot.child("imageBase64").getValue(String::class.java)
                    ?: ""
                callerName.text = uname
                if (imgBase64.isNotEmpty()) {
                    try {
                        val bytes = Base64.decode(imgBase64, Base64.DEFAULT)
                        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        callerPhoto.setImageBitmap(bmp)
                    } catch (_: Exception) {}
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun initAgora() {
        try {
            val config = RtcEngineConfig().apply {
                mContext = this@TenthActivity
                mAppId = appId
                mEventHandler = rtcEventHandler
            }
            rtcEngine = RtcEngine.create(config)
            rtcEngine?.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
            rtcEngine?.setClientRole(Constants.CLIENT_ROLE_BROADCASTER)

            val options = ChannelMediaOptions().apply {
                channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
                clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
            }

            if (isVideoCall) setupVideoMode() else setupAudioMode()

            rtcEngine?.joinChannel(token, channelName, uid, options)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupVideoMode() {
        audioLayout.visibility = View.GONE
        remoteVideoView.visibility = View.VISIBLE
        localVideoView.visibility = View.VISIBLE
        rtcEngine?.enableVideo()
        val localView = SurfaceView(baseContext)
        localVideoView.addView(localView)
        rtcEngine?.setupLocalVideo(VideoCanvas(localView, VideoCanvas.RENDER_MODE_HIDDEN, 0))
    }

    private fun setupAudioMode() {
        audioLayout.visibility = View.VISIBLE
        remoteVideoView.visibility = View.GONE
        localVideoView.visibility = View.GONE
        rtcEngine?.disableVideo()
    }

    private val rtcEventHandler = object : IRtcEngineEventHandler() {
        override fun onUserJoined(uid: Int, elapsed: Int) {
            runOnUiThread {
                val remoteView = SurfaceView(baseContext)
                remoteVideoView.addView(remoteView)
                rtcEngine?.setupRemoteVideo(
                    VideoCanvas(remoteView, VideoCanvas.RENDER_MODE_HIDDEN, uid)
                )
            }
        }

        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            runOnUiThread {
                println("Joined channel successfully: $channel (uid=$uid)")
                startTimer()
            }
        }
    }

    private fun startTimer() {
        startTime = System.currentTimeMillis()
        timerRunning = true
        handler.post(object : Runnable {
            override fun run() {
                if (timerRunning) {
                    val elapsed = (System.currentTimeMillis() - startTime) / 1000
                    val minutes = elapsed / 60
                    val seconds = elapsed % 60
                    callTimer.text = String.format("%02d:%02d", minutes, seconds)
                    handler.postDelayed(this, 1000)
                }
            }
        })
    }

    private fun stopTimer() {
        timerRunning = false
        handler.removeCallbacksAndMessages(null)
    }

    private fun checkPermissions(): Boolean {
        return REQUESTED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun endCall() {
        stopTimer()
        val callId = if (currentUserId < otherUserId)
            "${currentUserId}_$otherUserId" else "${otherUserId}_$currentUserId"
        FirebaseDatabase.getInstance().getReference("calls").child(callId)
            .child("status").setValue("ended")

        rtcEngine?.leaveChannel()
        rtcEngine?.stopPreview()
        RtcEngine.destroy()
        rtcEngine = null

        callListener?.let {
            callRef.removeEventListener(it)
        }
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        endCall()
    }

    companion object {
        private val REQUESTED_PERMISSIONS = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.INTERNET
        )
    }
}
