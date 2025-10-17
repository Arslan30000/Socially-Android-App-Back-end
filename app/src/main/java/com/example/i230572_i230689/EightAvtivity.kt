package com.example.i230572_i230689

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class EightActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ChatsAdapter

    private val chats = mutableListOf<Chat>()
    private val usersMap = mutableMapOf<String, User>()
    private val messagesMap = mutableMapOf<String, Message>()

    private val searchResults = mutableListOf<User>()
    private val searchUsersMap = mutableMapOf<String, User>()

    private val currentUserId = FirebaseAuth.getInstance().currentUser!!.uid
    private var isSearching = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.chat)

        recyclerView = findViewById(R.id.chats_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = ChatsAdapter(
            chats,
            currentUserId,
            onChatClick = { chat, user ->
                val intent = Intent(this, NinthActivity::class.java)
                intent.putExtra("chatId", chat.chatId)
                intent.putExtra("otherUserId", user.uid)
                startActivity(intent)
            },
            onChatLongClick = { chat ->
                deleteChat(chat)
            }
        )
        recyclerView.adapter = adapter

        loadCurrentUser()
        setupSearch()
    }

    private fun loadCurrentUser() {
        val userRef = FirebaseDatabase.getInstance().getReference("users/$currentUserId")
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentUser = snapshot.getValue(User::class.java)
                findViewById<TextView>(R.id.username).text = currentUser?.username ?: "User"
                loadChats()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun loadChats() {
        val chatsRef = FirebaseDatabase.getInstance().getReference("users/$currentUserId/chats")
        chatsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                chats.clear()
                usersMap.clear()
                messagesMap.clear()
                val chatList = mutableListOf<Chat>()

                for (snap in snapshot.children) {
                    val chat = snap.getValue(Chat::class.java) ?: continue
                    chatList.add(chat)

                    val otherUserId = chat.participants.keys.firstOrNull { it != currentUserId } ?: continue
                    FirebaseDatabase.getInstance().getReference("users/$otherUserId")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(userSnap: DataSnapshot) {
                                val user = userSnap.getValue(User::class.java) ?: return
                                usersMap[otherUserId] = user
                                if (!isSearching) updateAdapterSorted(chatList)
                            }

                            override fun onCancelled(error: DatabaseError) {}
                        })

                    chat.lastMessage.takeIf { it.isNotEmpty() }?.let { msgId ->
                        FirebaseDatabase.getInstance().getReference("messages/$msgId")
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(msgSnap: DataSnapshot) {
                                    val msg = msgSnap.getValue(Message::class.java) ?: return
                                    messagesMap[msg.messageId] = msg
                                    if (!isSearching) updateAdapterSorted(chatList)
                                }

                                override fun onCancelled(error: DatabaseError) {}
                            })
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun updateAdapterSorted(chatList: List<Chat>) {
        val sortedChats = chatList.sortedByDescending { it.lastMessageTime }
        adapter.updateData(sortedChats, usersMap, messagesMap)
    }

    private fun setupSearch() {
        val searchInput: EditText = findViewById(R.id.search_hint)
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().lowercase()
                if (query.isEmpty()) {
                    isSearching = false
                    updateAdapterSorted(chats)
                } else {
                    isSearching = true
                    performSearch(query)
                }
            }
        })
    }

    private fun performSearch(query: String) {
        searchResults.clear()
        searchUsersMap.clear()

        val userRef = FirebaseDatabase.getInstance().getReference("users/$currentUserId")
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentUser = snapshot.getValue(User::class.java) ?: return
                val connections = currentUser.followers.keys + currentUser.following.keys

                for (uid in connections) {
                    FirebaseDatabase.getInstance().getReference("users/$uid")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(userSnap: DataSnapshot) {
                                val user = userSnap.getValue(User::class.java) ?: return
                                if (user.username.lowercase().contains(query) ||
                                    user.name.lowercase().contains(query) ||
                                    user.lastname.lowercase().contains(query)
                                ) {
                                    if (!searchResults.contains(user)) {
                                        searchResults.add(user)
                                        searchUsersMap[user.uid] = user
                                        updateSearchAdapter()
                                    }
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {}
                        })
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun updateSearchAdapter() {
        val dummyChats = searchResults.map { user ->
            Chat(chatId = user.uid, participants = mapOf(currentUserId to true, user.uid to true))
        }
        adapter.updateData(dummyChats, searchUsersMap, messagesMap)
    }

    private fun deleteChat(chat: Chat) {
        FirebaseDatabase.getInstance()
            .getReference("users/$currentUserId/chats/${chat.chatId}")
            .removeValue()
        chat.participants.keys.firstOrNull { it != currentUserId }?.let { otherId ->
            FirebaseDatabase.getInstance()
                .getReference("users/$otherId/chats/${chat.chatId}")
                .removeValue()
        }
    }
}
