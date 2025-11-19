package com.example.i230572_i230689

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val data = remoteMessage.data
        val type = data["type"]

        if (type == "incoming_call") {
            val callerId = data["caller_id"]
            val channelName = data["channel_name"]
            val callType = data["call_type"]

            val intent = Intent(this, IncomingCallActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("caller_id", callerId)
                putExtra("channel_name", channelName)
                putExtra("call_type", callType)
            }
            startActivity(intent)
        } else {
            // Handle regular chat notifications
            val title = remoteMessage.notification?.title ?: "New Message"
            val body = remoteMessage.notification?.body ?: "You have a new message"
            showNotification(title, body)
        }
    }

    private fun showNotification(title: String, body: String) {
        val intent = Intent(this, NinthActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "chat_channel"
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.socially_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Chat Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    override fun onNewToken(token: String) {
        val sessionManager = SessionManager(this)
        if (sessionManager.isLoggedIn()) {
            sendTokenToServer(token)
        }
    }

    private fun sendTokenToServer(token: String) {
        val sessionManager = SessionManager(this)
        val url = BuildConfig.BASE_URL + "update_fcm_token.php"
        val rq = Volley.newRequestQueue(this)
        val req = object : StringRequest(Method.POST, url,
            { response -> /* Token updated */ },
            { error -> error.printStackTrace() }) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["fcm_token"] = token
                return params
            }
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer ${sessionManager.getToken()}"
                return headers
            }
        }
        rq.add(req)
    }
}
