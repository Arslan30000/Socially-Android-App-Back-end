package com.example.i230572_i230689

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.messaging.FirebaseMessaging
import java.io.ByteArrayOutputStream


class NinthActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var messageInput: EditText
    private lateinit var sendButton: ImageView
    private lateinit var galleryButton: ImageView
    private lateinit var videoButton: ImageView
    private lateinit var audioButton: ImageView
    private lateinit var messageAdapter: MessageAdapter
    private val messages = mutableListOf<Message>()
    private val usersMap = mutableMapOf<String, User>()
    private val currentUserId = FirebaseAuth.getInstance().currentUser!!.uid
    private lateinit var otherUserId: String
    private lateinit var chatId: String
    private val db = FirebaseDatabase.getInstance().reference
    private val PICK_IMAGE_REQUEST = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.chat_page)


        chatId = intent.getStringExtra("chatId") ?: ""
        otherUserId = intent.getStringExtra("otherUserId") ?: ""

        recyclerView = findViewById(R.id.chat_recycler_view)
        messageInput = findViewById(R.id.message_input)
        sendButton = findViewById(R.id.share_icon)
        galleryButton = findViewById(R.id.gallery_icon)
        videoButton = findViewById(R.id.video_icon)
        audioButton = findViewById(R.id.info_icon)

        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        recyclerView.layoutManager = layoutManager
        messageAdapter = MessageAdapter(messages, currentUserId, usersMap)
        recyclerView.adapter = messageAdapter

        FirebaseMessaging.getInstance().subscribeToTopic(currentUserId)

        loadOtherUser()
        loadMessages()
        listenForIncomingCalls()

        sendButton.setOnClickListener { sendMessage() }
        galleryButton.setOnClickListener { openGallery() }
        videoButton.setOnClickListener { startCall(true) }
        audioButton.setOnClickListener { startCall(false) }
    }

    private fun loadOtherUser() {
        db.child("users").child(otherUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java) ?: return
                    usersMap[otherUserId] = user
                    val pic = findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.profile_pic)
                    if (user.imageBase64.isNotEmpty()) {
                        val bytes = Base64.decode(user.imageBase64, Base64.DEFAULT)
                        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        pic.setImageBitmap(bmp)
                    }
                    findViewById<TextView>(R.id.chat_name).text = "${user.name} ${user.lastname}"
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun loadMessages() {
        db.child("messages").orderByChild("chatId").equalTo(chatId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val oldCount = messages.size
                    messages.clear()
                    for (snap in snapshot.children) {
                        val msg = snap.getValue(Message::class.java) ?: continue
                        messages.add(msg)
                    }
                    messageAdapter.notifyDataSetChanged()
                    recyclerView.scrollToPosition(messages.size - 1)

                    if (messages.size > oldCount) {
                        val lastMsg = messages.last()
                        if (lastMsg.senderId != currentUserId) {
                            showNotification("New message", lastMsg.text)
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }
    private fun showNotification(title: String, body: String) {
        val channelId = "chat_notifications"
        val notificationId = System.currentTimeMillis().toInt()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Chat Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)) {
            notify(notificationId, builder.build())
        }
    }

    private fun sendMessage() {
        val text = messageInput.text.toString().trim()
        if (text.isEmpty()) return
        val ref = db.child("messages").push()
        val msgId = ref.key ?: return
        val timestamp = System.currentTimeMillis()
        val msg = Message(msgId, currentUserId, otherUserId, text, "", "", timestamp, chatId)
        ref.setValue(msg)
        updateChat(msgId, timestamp)
        messageInput.text.clear()

        FirebaseMessaging.getInstance().subscribeToTopic(otherUserId)
        FirebaseMessaging.getInstance().send(
            com.google.firebase.messaging.RemoteMessage.Builder("/topics/$otherUserId")
                .setMessageId(msgId)
                .addData("title", "New Message")
                .addData("body", text)
                .build()
        )
    }

    private fun openGallery() {
        val i = Intent(Intent.ACTION_PICK)
        i.type = "image/*"
        startActivityForResult(i, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            val uri = data.data ?: return
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            val bytes = outputStream.toByteArray()
            val imgBase64 = Base64.encodeToString(bytes, Base64.DEFAULT)
            val ref = db.child("messages").push()
            val msgId = ref.key ?: return
            val timestamp = System.currentTimeMillis()
            val msg = Message(msgId, currentUserId, otherUserId, "", imgBase64, "", timestamp, chatId)
            ref.setValue(msg)
            updateChat(msgId, timestamp)
        }
    }

    private fun updateChat(messageId: String, timestamp: Long) {
        val data = mapOf(
            "chatId" to chatId,
            "participants" to mapOf(currentUserId to true, otherUserId to true),
            "lastMessage" to messageId,
            "lastMessageTime" to timestamp
        )
        db.child("users/$currentUserId/chats/$chatId").setValue(data)
        db.child("users/$otherUserId/chats/$chatId").setValue(data)
    }

    private fun startCall(isVideo: Boolean) {
        val otherUser = usersMap[otherUserId] ?: return
        val callId = if (currentUserId < otherUserId) "${currentUserId}_$otherUserId" else "${otherUserId}_$currentUserId"
        val callData = mapOf(
            "callerId" to currentUserId,
            "receiverId" to otherUserId,
            "isVideo" to isVideo,
            "status" to "ringing"
        )
        db.child("calls").child(callId).setValue(callData)
        val i = Intent(this, TenthActivity::class.java).apply {
            putExtra("isVideoCall", isVideo)
            putExtra("otherUserId", otherUserId)
            putExtra("username", "${otherUser.name} ${otherUser.lastname}")
            putExtra("channelName", callId)
        }
        startActivity(i)
    }

    private fun listenForIncomingCalls() {
        db.child("calls").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (callSnap in snapshot.children) {
                    val caller = callSnap.child("callerId").getValue(String::class.java) ?: continue
                    val receiver = callSnap.child("receiverId").getValue(String::class.java) ?: continue
                    val isVideo = callSnap.child("isVideo").getValue(Boolean::class.java) ?: true
                    val status = callSnap.child("status").getValue(String::class.java) ?: "none"
                    if (receiver == currentUserId && status == "ringing") {
                        val i = Intent(this@NinthActivity, TenthActivity::class.java).apply {
                            putExtra("isVideoCall", isVideo)
                            putExtra("otherUserId", caller)
                            putExtra("channelName", callSnap.key)
                        }
                        startActivity(i)
                        callSnap.ref.child("status").setValue("ongoing")
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
