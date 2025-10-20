package com.example.i230572_i230689

import android.Manifest
import android.content.pm.PackageManager
import android.os.*
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
    private lateinit var remoteView: FrameLayout
    private lateinit var localView: FrameLayout
    private lateinit var videoTimer: TextView
    private lateinit var audioTimer: TextView
    private lateinit var videoEndBtn: ImageButton
    private lateinit var audioEndBtn: ImageButton
    private lateinit var audioLayout: LinearLayout
    private lateinit var videoLayout: RelativeLayout
    private lateinit var receiverName: TextView
    private lateinit var receiverImage: ImageView

    private var isVideoCall = false
    private var isJoined = false
    private var callTimerHandler = Handler(Looper.getMainLooper())
    private var secondsPassed = 0
    private var otherUserId = ""
    private var currentUserId = ""
    private val appId = "91803c0e31234583aab3d8e36549097d"

    private val token = "007eJxTYPBJvcvFHWlhbCXaZtCtUhAosefovD1OE8PlStzdGNMf8iswWBpaGBgnG6QaGxoZm5haGCcmJhmnWKQam5maWBpYmqcUCX7LaAhkZFj17g8DIxSC+CwMJanFJQwMANs2G5M="

    private val uid = 0

    private val channelName = "test"

    private lateinit var callRef: DatabaseReference

    private val rtcEventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            runOnUiThread {
                Toast.makeText(this@TenthActivity, "✅ Joined Channel Successfully", Toast.LENGTH_SHORT).show()
                startTimer()
            }
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            runOnUiThread {
                Toast.makeText(this@TenthActivity, "👤 User Joined", Toast.LENGTH_SHORT).show()
                setupRemoteVideo(uid)
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            runOnUiThread {
                Toast.makeText(this@TenthActivity, "🚫 User Left", Toast.LENGTH_SHORT).show()
                endCall()
            }
        }

        override fun onError(err: Int) {
            runOnUiThread {
                Toast.makeText(this@TenthActivity, "⚠️ Agora Error: $err", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.call_page)

        remoteView = findViewById(R.id.remote_video_view)
        localView = findViewById(R.id.local_video_view)
        videoTimer = findViewById(R.id.video_timer_text)
        audioTimer = findViewById(R.id.audio_timer_text)
        videoEndBtn = findViewById(R.id.video_end_button)
        audioEndBtn = findViewById(R.id.audio_end_button)
        audioLayout = findViewById(R.id.audio_layout)
        videoLayout = findViewById(R.id.video_layout)
        receiverName = findViewById(R.id.receiver_name)
        receiverImage = findViewById(R.id.receiver_image)

        isVideoCall = intent.getBooleanExtra("isVideoCall", true)
        otherUserId = intent.getStringExtra("otherUserId") ?: ""
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        callRef = FirebaseDatabase.getInstance().getReference("calls/$currentUserId")

        requestPermissions()
        setupFirebaseListener()

        videoEndBtn.setOnClickListener { endCall() }
        audioEndBtn.setOnClickListener { endCall() }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (isVideoCall) permissions.add(Manifest.permission.CAMERA)

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 1)
        } else {
            initAgora()
        }
    }

    override fun onRequestPermissionsResult(reqCode: Int, perms: Array<out String>, res: IntArray) {
        if (reqCode == 1 && res.all { it == PackageManager.PERMISSION_GRANTED }) {
            initAgora()
        } else {
            Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show()
            finish()
        }
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

            if (isVideoCall) setupVideoMode() else setupAudioMode()

            val options = ChannelMediaOptions().apply {
                channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
                clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
                autoSubscribeAudio = true
                autoSubscribeVideo = isVideoCall
                publishCameraTrack = isVideoCall
                publishMicrophoneTrack = true
            }

            val res = rtcEngine?.joinChannel(token, channelName, uid, options)
            if (res != 0) {
                Toast.makeText(this, "Join failed: $res", Toast.LENGTH_LONG).show()
            } else {
                isJoined = true
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Agora init error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupVideoMode() {
        audioLayout.visibility = View.GONE
        videoLayout.visibility = View.VISIBLE
        val localSurface = SurfaceView(this)
        localView.addView(localSurface)
        rtcEngine?.enableVideo()
        rtcEngine?.setupLocalVideo(VideoCanvas(localSurface, VideoCanvas.RENDER_MODE_FIT, 0))
        rtcEngine?.startPreview()
    }

    private fun setupAudioMode() {
        videoLayout.visibility = View.GONE
        audioLayout.visibility = View.VISIBLE
        rtcEngine?.disableVideo()
    }

    private fun setupRemoteVideo(uid: Int) {
        remoteView.removeAllViews()
        val surfaceView = SurfaceView(this)
        remoteView.addView(surfaceView)
        rtcEngine?.setupRemoteVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, uid))
    }

    private fun startTimer() {
        callTimerHandler.post(object : Runnable {
            override fun run() {
                secondsPassed++
                val min = secondsPassed / 60
                val sec = secondsPassed % 60
                val time = String.format("%02d:%02d", min, sec)
                if (isVideoCall) videoTimer.text = time else audioTimer.text = time
                callTimerHandler.postDelayed(this, 1000)
            }
        })
    }

    private fun endCall() {
        callRef.removeValue()
        rtcEngine?.leaveChannel()
        RtcEngine.destroy()
        rtcEngine = null
        finish()
    }

    private fun setupFirebaseListener() {
        val otherCallRef = FirebaseDatabase.getInstance().getReference("calls/$otherUserId")
        otherCallRef.child("status").onDisconnect().setValue("ended")

        callRef.child("status").setValue("calling")
        callRef.child("receiver").setValue(otherUserId)

        otherCallRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val status = snapshot.child("status").getValue(String::class.java)
                val receiver = snapshot.child("receiver").getValue(String::class.java)
                if (status == "calling" && receiver == currentUserId && !isJoined) {
                    initAgora()
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        endCall()
    }
}
