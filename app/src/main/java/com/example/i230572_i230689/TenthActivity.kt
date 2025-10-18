package com.example.i230572_i230689

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.SurfaceView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.agora.rtc2.*
import io.agora.rtc2.video.VideoCanvas

class TenthActivity : AppCompatActivity() {

    private var rtcEngine: RtcEngine? = null
    private val appId = "91803c0e31234583aab3d8e36549097d"
    private val channelName = "test_channel"
    private val token: String? = null
    private val uid = 0
    private var isVideoCall = true

    private lateinit var callerName: TextView
    private lateinit var callerPhoto: ImageView
    private lateinit var callContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.call_page)

        callerName = findViewById(R.id.caller_name)
        callerPhoto = findViewById(R.id.caller_photo)
        callContainer = findViewById(R.id.call_tag)


        isVideoCall = intent.getBooleanExtra("isVideoCall", true)
        val username = intent.getStringExtra("username") ?: "Unknown"
        val imageBase64 = intent.getStringExtra("imageBase64") ?: ""

        callerName.text = username
        if (imageBase64.isNotEmpty()) {
            try {
                val bytes = Base64.decode(imageBase64, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                callerPhoto.setImageBitmap(bmp)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (checkPermissions()) {
            initAgora()
        } else {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, 1)
        }

        findViewById<ImageView>(R.id.decline).setOnClickListener {
            rtcEngine?.leaveChannel()
            rtcEngine?.stopPreview()
            RtcEngine.destroy()
            rtcEngine = null
            finish()
        }
    }

    private fun initAgora() {
        val config = RtcEngineConfig().apply {
            mContext = this@TenthActivity
            mAppId = appId
            mEventHandler = object : IRtcEngineEventHandler() {}
        }

        rtcEngine = RtcEngine.create(config)

        val opts = ChannelMediaOptions().apply {
            channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
            clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
        }

        if (isVideoCall) {
            rtcEngine?.enableVideo()
            val localView = SurfaceView(baseContext)
            localView.setZOrderMediaOverlay(true)
            callContainer.addView(localView)
            rtcEngine?.setupLocalVideo(VideoCanvas(localView, VideoCanvas.RENDER_MODE_HIDDEN, uid))
        } else {
            rtcEngine?.disableVideo()
        }

        rtcEngine?.joinChannel(token, channelName, uid, opts)
    }

    private fun checkPermissions(): Boolean {
        return REQUESTED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    companion object {
        private val REQUESTED_PERMISSIONS = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )
    }
}
