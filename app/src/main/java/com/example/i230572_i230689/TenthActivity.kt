package com.example.i230572_i230689

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.SurfaceView
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoCanvas
import org.json.JSONObject

class TenthActivity : AppCompatActivity() {

    private var rtcEngine: RtcEngine? = null
    private var channelName: String? = null
    private var callType: String? = null
    private val agoraAppId: String = "4e2534f9f9dc48b98e9f2153a207dcf8" 

    private lateinit var localVideoContainer: FrameLayout
    private lateinit var remoteVideoContainer: FrameLayout
    private lateinit var sessionManager: SessionManager

    private var isMicMuted = false
    private var isVideoDisabled = false

    private val PERMISSION_REQ_ID = 22
    private val REQUESTED_PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.call_page)

        sessionManager = SessionManager(this)
        channelName = intent.getStringExtra("channel_name")
        callType = intent.getStringExtra("call_type")

        localVideoContainer = findViewById(R.id.local_video_container)
        remoteVideoContainer = findViewById(R.id.remote_video_container)

        if (checkPermissions()) {
            initializeAndJoinChannel()
        } else {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, PERMISSION_REQ_ID)
        }

        setupButtonListeners()
    }

    private fun checkPermissions(): Boolean {
        return REQUESTED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQ_ID) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                initializeAndJoinChannel()
            } else {
                Toast.makeText(this, "Permissions are required for calls.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun initializeAndJoinChannel() {
        try {
            val config = RtcEngineConfig()
            config.mContext = baseContext
            config.mAppId = agoraAppId
            config.mEventHandler = object : IRtcEngineEventHandler() {
                override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
                    runOnUiThread { Toast.makeText(applicationContext, "Joined Channel: $channel", Toast.LENGTH_SHORT).show() }
                }

                override fun onUserJoined(uid: Int, elapsed: Int) {
                    runOnUiThread { setupRemoteVideo(uid) }
                }

                override fun onUserOffline(uid: Int, reason: Int) {
                    runOnUiThread { onRemoteUserLeft() }
                }
            }
            rtcEngine = RtcEngine.create(config)
        } catch (e: Exception) {
            Toast.makeText(this, "RTC Engine Init Failed: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val options = ChannelMediaOptions()
        // Corrected: Use property access for Agora SDK v4.x
        options.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
        options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER

        if (callType == "video") {
            rtcEngine?.enableVideo()
            setupLocalVideo()
        } else {
            rtcEngine?.enableAudio()
            // For audio calls, hide the video containers
            localVideoContainer.visibility = View.GONE
            remoteVideoContainer.visibility = View.GONE
        }

        // Join the channel
        rtcEngine?.joinChannel(null, channelName, 0, options)
    }

    private fun setupLocalVideo() {
        val surfaceView = SurfaceView(baseContext)
        localVideoContainer.addView(surfaceView)
        rtcEngine?.setupLocalVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, 0))
        rtcEngine?.startPreview()
    }

    private fun setupRemoteVideo(uid: Int) {
        val surfaceView = SurfaceView(baseContext)
        remoteVideoContainer.addView(surfaceView)
        rtcEngine?.setupRemoteVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, uid))
    }

    private fun onRemoteUserLeft() {
        remoteVideoContainer.removeAllViews()
    }

    private fun setupButtonListeners() {
        findViewById<ImageButton>(R.id.end_call_button).setOnClickListener {
            endCallOnServer()
            finish()
        }

        findViewById<ImageButton>(R.id.mute_button).setOnClickListener {
            isMicMuted = !isMicMuted
            rtcEngine?.muteLocalAudioStream(isMicMuted)
            it.isSelected = isMicMuted
        }

        val switchCameraButton = findViewById<ImageButton>(R.id.switch_camera_button)
        val disableVideoButton = findViewById<ImageButton>(R.id.disable_video_button)

        if (callType == "video") {
            switchCameraButton.setOnClickListener {
                rtcEngine?.switchCamera()
            }
            disableVideoButton.setOnClickListener {
                isVideoDisabled = !isVideoDisabled
                rtcEngine?.muteLocalVideoStream(isVideoDisabled)
                it.isSelected = isVideoDisabled
                localVideoContainer.visibility = if (isVideoDisabled) View.GONE else View.VISIBLE
            }
        } else {
            // Hide video-specific buttons for audio calls
            switchCameraButton.visibility = View.GONE
            disableVideoButton.visibility = View.GONE
        }
    }

    private fun endCallOnServer() {
        val token = sessionManager.getToken() ?: return
        val url = BuildConfig.BASE_URL + "end_call.php"
        val rq = Volley.newRequestQueue(this)
        val obj = JSONObject()
        obj.put("channel_name", channelName)

        val req = object : StringRequest(Method.POST, url, { _ -> }, { _ -> }) {
            override fun getBody(): ByteArray = obj.toString().toByteArray()
            override fun getBodyContentType(): String = "application/json"
            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf("Authorization" to "Bearer $token")
            }
        }
        rq.add(req)
    }

    override fun onDestroy() {
        super.onDestroy()
        rtcEngine?.leaveChannel()
        RtcEngine.destroy()
        rtcEngine = null
    }
}
