package com.example.i230572_i230689

import android.os.Bundle
import android.text.TextUtils
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*



class NinthActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var messageInput: EditText
    private lateinit var sendButton: ImageView

    private lateinit var messageAdapter: MessageAdapter
    private val messages = mutableListOf<Message>()
    private val usersMap = mutableMapOf<String, User>()

    private val currentUserId = FirebaseAuth.getInstance().currentUser!!.uid
    private lateinit var otherUserId: String
    private lateinit var chatId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.chat_page)

        chatId = intent.getStringExtra("chatId") ?: ""
        otherUserId = intent.getStringExtra("otherUserId") ?: ""

        recyclerView = findViewById(R.id.chat_recycler_view)
        messageInput = findViewById(R.id.message_input)
        sendButton = findViewById(R.id.share_icon)

        recyclerView.layoutManager = LinearLayoutManager(this)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        recyclerView.layoutManager = layoutManager
        messageAdapter = MessageAdapter(messages, currentUserId, usersMap)
        recyclerView.adapter = messageAdapter

        loadOtherUser()
        loadMessages()

        sendButton.setOnClickListener { sendMessage() }
    }

    private fun loadOtherUser() {
        val userRef = FirebaseDatabase.getInstance().getReference("users/$otherUserId")
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java) ?: return
                usersMap[otherUserId] = user
                findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.profile_pic).apply {
                    if (user.imageBase64.isNotEmpty()) {
                        val bytes = android.util.Base64.decode(user.imageBase64, android.util.Base64.DEFAULT)
                        val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        setImageBitmap(bitmap)
                    }
                }
                findViewById<android.widget.TextView>(R.id.chat_name).text = "${user.name} ${user.lastname}"
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun loadMessages() {
        val messagesRef = FirebaseDatabase.getInstance().getReference("messages")
        messagesRef.orderByChild("chatId").equalTo(chatId)
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
        val messagesRef = FirebaseDatabase.getInstance().getReference("messages")
        val newMessageRef = messagesRef.push()
        val messageId = newMessageRef.key ?: return
        val timestamp = System.currentTimeMillis()
        val message = Message(
            messageId = messageId,
            senderId = currentUserId,
            receiverId = otherUserId,
            text = text,
            timestamp = timestamp,
            postId = "",
            chatId = chatId
        )
        newMessageRef.setValue(message)
        val currentUserChatRef = FirebaseDatabase.getInstance()
            .getReference("users/$currentUserId/chats/$chatId")
        val otherUserChatRef = FirebaseDatabase.getInstance()
            .getReference("users/$otherUserId/chats/$chatId")
        val chatData = mapOf(
            "chatId" to chatId,
            "participants" to mapOf(currentUserId to true, otherUserId to true),
            "lastMessage" to messageId,
            "lastMessageTime" to timestamp
        )
        currentUserChatRef.setValue(chatData)
        otherUserChatRef.setValue(chatData)
        messageInput.text.clear()
    }
}
