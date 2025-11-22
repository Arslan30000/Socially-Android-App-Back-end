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

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // This is triggered by a push notification from the FCM server.
        // We are not using this for foreground notifications.
        remoteMessage.notification?.let {
            showNotification(this, it.title, it.body)
        }
    }

    companion object {
        /**
         * Creates and shows a simple notification.
         *
         * @param context Context of the app.
         * @param title Title of the notification.
         * @param body Body of the notification.
         */
        fun showNotification(context: Context, title: String?, body: String?) {
            val intent = Intent(context, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )

            val channelId = "default_notification_channel"
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val notificationBuilder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.socially_logo)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setSound(soundUri)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Since android Oreo notification channel is needed.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Default Channel",
                    NotificationManager.IMPORTANCE_HIGH
                )
                notificationManager.createNotificationChannel(channel)
            }

            notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
        }
    }
}
