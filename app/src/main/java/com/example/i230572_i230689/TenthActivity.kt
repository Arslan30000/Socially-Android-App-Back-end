package com.example.i230572_i230689

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.SurfaceView
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
    private var agoraAppId: String? = "4e2534f9f9dc48b98e9f2153a207dcf8" // Using the App ID directly for simplicity

    private lateinit var localVideoContainer: FrameLayout
    private lateinit var remoteVideoContainer: FrameLayout
    private lateinit var sessionManager: SessionManager

    private val PERMISSION_REQ_ID = 22
    private val REQUESTED_PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA
    )

    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.call_page)
//
//        sessionManager = SessionManager(this)
//        channelName = intent.getStringExtra("channel_name")
//        callType = intent.getStringExtra("call_type")
//
//        localVideoContainer = findViewById(R.id.local_video_container)
//        remoteVideoContainer = findViewById(R.id.remote_video_container)
//
//        if (checkPermissions()) {
//            initializeAndJoinChannel()
//        }
//
//        findViewById<ImageButton>(R.id.end_call_button).setOnClickListener {
//            endCall()
//        }
//
//        findViewById<ImageButton>(R.id.mute_button).setOnClickListener {
//            it.isSelected = !it.isSelected
//            rtcEngine?.muteLocalAudioStream(it.isSelected)
//        }
//    }
//
//    private fun checkPermissions(): Boolean {
//        val notGranted = REQUESTED_PERMISSIONS.filter {
//            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
//        }
//        if (notGranted.isNotEmpty()) {
//            ActivityCompat.requestPermissions(this, notGranted.toTypedArray(), PERMISSION_REQ_ID)
//            return false
//        }
//        return true
//    }
//
//    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == PERMISSION_REQ_ID) {
//            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
//                initializeAndJoinChannel()
//            } else {
//                Toast.makeText(this, "Permissions are required for calls", Toast.LENGTH_SHORT).show()
//                finish()
//            }
//        }
//    }
//
//    private fun initializeAndJoinChannel() {
//        try {
//            val config = RtcEngineConfig()
//            config.mContext = baseContext
//            config.mAppId = agoraAppId
//            config.mEventHandler = object : IRtcEngineEventHandler() {
//                override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
//                    runOnUiThread { Toast.makeText(applicationContext, "Joined Channel", Toast.LENGTH_SHORT).show() }
//                }
//
//                override fun onUserJoined(uid: Int, elapsed: Int) {
//                    runOnUiThread { setupRemoteVideo(uid) }
//                }
//
//                override fun onUserOffline(uid: Int, reason: Int) {
//                    runOnUiThread { onRemoteUserLeft() }
//                }
//            }
//            rtcEngine = RtcEngine.create(config)
//        } catch (e: Exception) {
//            Toast.makeText(this, "RTC Engine Init Failed: ${e.message}", Toast.LENGTH_LONG).show()
//            finish()
//            return
//        }
//
//        val options = ChannelMediaOptions()
//        options.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
//        if (callType == "video") {
//            rtcEngine?.enableVideo()
//            setupLocalVideo()
//        } else {
//            rtcEngine?.enableAudio()
//        }
//
//        rtcEngine?.joinChannel(null, channelName, 0, options)
//    }
//
//    private fun setupLocalVideo() {
//        val surfaceView = SurfaceView(baseContext)
//        localVideoContainer.addView(surfaceView)
//        rtcEngine?.setupLocalVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, 0))
//    }
//
//    private fun setupRemoteVideo(uid: Int) {
//        val surfaceView = SurfaceView(baseContext)
//        remoteVideoContainer.addView(surfaceView)
//        rtcEngine?.setupRemoteVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, uid))
//    }
//
//    private fun onRemoteUserLeft() {
//        remoteVideoContainer.removeAllViews()
//    }
//
//    private fun endCall() {
//        val token = sessionManager.getToken() ?: return
//        val url = BuildConfig.BASE_URL + "end_call.php"
//        val rq = Volley.newRequestQueue(this)
//        val obj = JSONObject()
//        obj.put("channel_name", channelName)
//
//        val req = object : StringRequest(Method.POST, url, { _ -> }, { _ -> }) {
//            override fun getBody(): ByteArray = obj.toString().toByteArray()
//            override fun getBodyContentType(): String = "application/json"
//            override fun getHeaders(): MutableMap<String, String> {
//                val headers = HashMap<String, String>()
//                headers["Authorization"] = "Bearer $token"
//                return headers
//            }
//        }
//        rq.add(req)
//        finish()
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        rtcEngine?.leaveChannel()
//        RtcEngine.destroy()
    }
}
