package com.example.i230572_i230689

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.SurfaceView
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import io.agora.rtc2.*
import io.agora.rtc2.video.VideoCanvas
import org.json.JSONObject

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

    private var isVideoCall = false
    private var channelName: String? = null
    private var appId: String? = null

    private val callTimerHandler = Handler(Looper.getMainLooper())
    private var secondsPassed = 0

    private val rtcEventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            runOnUiThread {
                Toast.makeText(this@TenthActivity, "Joined Channel Successfully", Toast.LENGTH_SHORT).show()
                startTimer()
            }
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            runOnUiThread {
                Toast.makeText(this@TenthActivity, "User Joined", Toast.LENGTH_SHORT).show()
                setupRemoteVideo(uid)
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            runOnUiThread {
                Toast.makeText(this@TenthActivity, "User Left", Toast.LENGTH_SHORT).show()
                endCall()
            }
        }

        override fun onError(err: Int) {
            runOnUiThread {
                Toast.makeText(this@TenthActivity, "Agora Error: $err", Toast.LENGTH_LONG).show()
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

        channelName = intent.getStringExtra("channel_name")
        isVideoCall = intent.getStringExtra("call_type") == "video"

        videoEndBtn.setOnClickListener { endCall() }
        audioEndBtn.setOnClickListener { endCall() }

        fetchAgoraConfig()
    }

    private fun fetchAgoraConfig() {
        val url = BuildConfig.BASE_URL + "get_agora_config.php"
        val rq = Volley.newRequestQueue(this)
        val req = object : StringRequest(Method.GET, url,
            { response ->
                try {
                    val obj = JSONObject(response.trim())
                    if (obj.optBoolean("success", false)) {
                        appId = obj.getString("app_id")
                        requestPermissions()
                    } else {
                        Toast.makeText(this, "Failed to get Agora config", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    finish()
                }
            },
            { error ->
                error.printStackTrace()
                finish()
            }) {}
        rq.add(req)
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
        super.onRequestPermissionsResult(reqCode, perms, res)
        if (reqCode == 1 && res.all { it == PackageManager.PERMISSION_GRANTED }) {
            initAgora()
        } else {
            Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun initAgora() {
        if (appId.isNullOrEmpty() || channelName.isNullOrEmpty()) {
            Toast.makeText(this, "Agora config is missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        try {
            val config = RtcEngineConfig().apply {
                mContext = this@TenthActivity
                mAppId = appId
                mEventHandler = rtcEventHandler
            }

            rtcEngine = RtcEngine.create(config)
            rtcEngine?.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)

            if (isVideoCall) {
                setupVideoMode()
            } else {
                setupAudioMode()
            }

            val options = ChannelMediaOptions().apply {
                channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
                clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
                autoSubscribeAudio = true
                autoSubscribeVideo = isVideoCall
                publishCameraTrack = isVideoCall
                publishMicrophoneTrack = true
            }

            // Join channel without a token
            rtcEngine?.joinChannel(null, channelName, 0, options)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Agora init error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupVideoMode() {
        audioLayout.visibility = View.GONE
        videoLayout.visibility = View.VISIBLE
        rtcEngine?.enableVideo()
        val localSurface = SurfaceView(this)
        localView.addView(localSurface)
        rtcEngine?.setupLocalVideo(VideoCanvas(localSurface, VideoCanvas.RENDER_MODE_FIT, 0))
        rtcEngine?.startPreview()
    }

    private fun setupAudioMode() {
        videoLayout.visibility = View.GONE
        audioLayout.visibility = View.VISIBLE
        rtcEngine?.disableVideo()
    }

    private fun setupRemoteVideo(uid: Int) {
        if (isVideoCall) {
            remoteView.removeAllViews()
            val surfaceView = SurfaceView(this)
            remoteView.addView(surfaceView)
            rtcEngine?.setupRemoteVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, uid))
        }
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
        rtcEngine?.leaveChannel()
        RtcEngine.destroy()
        rtcEngine = null
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        endCall()
    }
}
