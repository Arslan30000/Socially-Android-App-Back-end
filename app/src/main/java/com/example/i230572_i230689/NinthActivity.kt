package com.example.i230572_i230689

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
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

        loadOtherUser()
        loadMessages()

        sendButton.setOnClickListener { sendMessage() }
        galleryButton.setOnClickListener { openGallery() }
        videoButton.setOnClickListener { startCall(true) }
        audioButton.setOnClickListener { startCall(false) }
    }


    private fun loadOtherUser() {
        FirebaseDatabase.getInstance().getReference("users/$otherUserId")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java) ?: return
                    usersMap[otherUserId] = user

                    findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.profile_pic).apply {
                        if (user.imageBase64.isNotEmpty()) {
                            val bytes = Base64.decode(user.imageBase64, Base64.DEFAULT)
                            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            setImageBitmap(bmp)
                        }
                    }

                    findViewById<TextView>(R.id.chat_name).text = "${user.name} ${user.lastname}"
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }


    private fun loadMessages() {
        FirebaseDatabase.getInstance().getReference("messages")
            .orderByChild("chatId").equalTo(chatId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    messages.clear()
                    for (snap in snapshot.children) {
                        val msg = snap.getValue(Message::class.java) ?: continue
                        messages.add(msg)

                        if (!usersMap.containsKey(msg.senderId)) {
                            FirebaseDatabase.getInstance().getReference("users/${msg.senderId}")
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(userSnap: DataSnapshot) {
                                        val user = userSnap.getValue(User::class.java)
                                        if (user != null) {
                                            usersMap[user.uid] = user
                                            messageAdapter.notifyDataSetChanged()
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {}
                                })
                        }
                    }

                    messageAdapter.notifyDataSetChanged()
                    recyclerView.scrollToPosition(messages.size - 1)
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }


    private fun sendMessage() {
        val text = messageInput.text.toString().trim()
        if (text.isEmpty()) return

        val ref = FirebaseDatabase.getInstance().getReference("messages").push()
        val msgId = ref.key ?: return
        val timestamp = System.currentTimeMillis()

        val msg = Message(
            messageId = msgId,
            senderId = currentUserId,
            receiverId = otherUserId,
            text = text,
            imageBase64 = "",
            postId = "",
            timestamp = timestamp,
            chatId = chatId
        )

        ref.setValue(msg)
        updateChat(msgId, timestamp)
        messageInput.text.clear()
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

            val ref = FirebaseDatabase.getInstance().getReference("messages").push()
            val msgId = ref.key ?: return
            val timestamp = System.currentTimeMillis()

            val msg = Message(
                messageId = msgId,
                senderId = currentUserId,
                receiverId = otherUserId,
                text = "",
                imageBase64 = imgBase64,
                postId = "",
                timestamp = timestamp,
                chatId = chatId
            )

            ref.setValue(msg)
            updateChat(msgId, timestamp)
        }
    }


    private fun updateChat(messageId: String, timestamp: Long) {
        val currentRef = FirebaseDatabase.getInstance().getReference("users/$currentUserId/chats/$chatId")
        val otherRef = FirebaseDatabase.getInstance().getReference("users/$otherUserId/chats/$chatId")

        val data = mapOf(
            "chatId" to chatId,
            "participants" to mapOf(currentUserId to true, otherUserId to true),
            "lastMessage" to messageId,
            "lastMessageTime" to timestamp
        )

        currentRef.setValue(data)
        otherRef.setValue(data)
    }
    private fun startCall(isVideo: Boolean) {
        val otherUser = usersMap[otherUserId] ?: return
        val i = Intent(this, TenthActivity::class.java).apply {
            putExtra("isVideoCall", isVideo)
            putExtra("otherUserId", otherUserId)
            putExtra("username", "${otherUser.name} ${otherUser.lastname}")
            putExtra("imageBase64", otherUser.imageBase64)
        }
        startActivity(i)
    }
}
