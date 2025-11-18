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
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class EightActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ChatsAdapter
    private lateinit var sessionManager: SessionManager

    private val chats = mutableListOf<Chat>()
    private val usersMap = mutableMapOf<String, User>()
    private val messagesMap = mutableMapOf<String, Message>()

    private val searchResults = mutableListOf<User>()
    private val searchUsersMap = mutableMapOf<String, User>()

    private var isSearching = false

    private var statusRefreshHandler: android.os.Handler? = null
    private var statusRefreshRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.chat)

        sessionManager = SessionManager(this)

        recyclerView = findViewById(R.id.chats_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = ChatsAdapter(
            chats,
            sessionManager.getUserId().toString(),
            onChatClick = { chat, user ->
                val intent = Intent(this, NinthActivity::class.java)
                // If chatId starts with "search_", it's a temporary ID for a new chat.
                // Pass an empty string to NinthActivity so it knows to create a conversation.
                if (chat.chatId.startsWith("search_")) {
                    intent.putExtra("chatId", "")
                } else {
                    intent.putExtra("chatId", chat.chatId)
                }
                intent.putExtra("otherUserId", user.uid)
                startActivity(intent)
            },
            onChatLongClick = { chat ->
            }
        )
        recyclerView.adapter = adapter
        // setup horizontal online followers bar
        val onlineRv = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.online_recycler_view)
        onlineRv.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false)
        onlineRv.adapter = OnlineUserAdapter(mutableListOf()) { user ->
            // open chat with selected follower
            val intent = Intent(this, NinthActivity::class.java)
            // Pass an empty chatId to signal a new conversation
            intent.putExtra("chatId", "")
            intent.putExtra("otherUserId", user.uid)
            startActivity(intent)
        }

        loadChats()
        loadOnlineFollowers()
        setupSearch()
        startStatusRefresh()
    }

    override fun onResume() {
        super.onResume()
        setStatus("online")
        startStatusRefresh()
        // Refresh chats when returning to the activity
        loadChats()
    }

    override fun onPause() {
        super.onPause()
        stopStatusRefresh()
        setStatus("offline")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopStatusRefresh()
    }
    
    private fun startStatusRefresh() {
        if (statusRefreshHandler == null) {
            statusRefreshHandler = android.os.Handler(android.os.Looper.getMainLooper())
        }
        stopStatusRefresh()
        statusRefreshRunnable = Runnable {
            val userIds = usersMap.keys.toList()
            if (userIds.isNotEmpty()) {
                fetchOnlineStatusesForChats(userIds)
            }
            // Also refresh online followers
            loadOnlineFollowers()
            // Schedule next refresh in 5 seconds
            statusRefreshHandler?.postDelayed(statusRefreshRunnable!!, 5000)
        }
        statusRefreshHandler?.post(statusRefreshRunnable!!)
    }
    
    private fun stopStatusRefresh() {
        if (statusRefreshHandler != null && statusRefreshRunnable != null) {
            statusRefreshHandler?.removeCallbacks(statusRefreshRunnable!!)
        }
    }

    private fun setStatus(status: String) {
        val token = sessionManager.getToken() ?: return
        val url = BuildConfig.BASE_URL + "set_status.php"
        val rq = Volley.newRequestQueue(this)
        val req = object : StringRequest(Method.POST, url,
            { response -> /* ignored */ },
            { error -> error.printStackTrace() }) {
            override fun getBody(): ByteArray? {
                val obj = JSONObject()
                obj.put("status", status)
                return obj.toString().toByteArray()
            }
            override fun getBodyContentType(): String = "application/json"
            override fun getHeaders(): MutableMap<String, String> {
                val h = HashMap<String, String>()
                h["Authorization"] = "Bearer ${sessionManager.getToken()}"
                return h
            }
        }
        req.retryPolicy = DefaultRetryPolicy(8000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        rq.add(req)
    }

    private fun loadChats() {
        val token = sessionManager.getToken() ?: run { return }
        val url = BuildConfig.BASE_URL + "get_recent_chats.php"
        val rq = Volley.newRequestQueue(this)
        val req = object : StringRequest(Method.GET, url,
            { response ->
                try {
                    val obj = JSONObject(response.trim())
                    if (obj.optBoolean("success", false)) {
                        val arr = obj.optJSONArray("conversations")
                        chats.clear(); usersMap.clear(); messagesMap.clear()
                        val list = mutableListOf<Chat>()
                        if (arr != null) {
                            for (i in 0 until arr.length()) {
                                val it = arr.getJSONObject(i)
                                val convId = it.optInt("conversationId", -1)
                                val otherId = it.optInt("userId", -1)
                                val username = it.optString("username", "")
                                val profile = it.optString("profileImage", "")
                                val lastMsg = it.optString("lastMessage", "")
                                val lastMsgId = it.optInt("lastMessageId", -1)
                                val lastAt = it.optString("lastMessageAt", "")

                                if (convId == -1 || otherId == -1) continue

                                val lastMillis = parseMySqlDatetimeToMillis(lastAt)

                                val chat = Chat(chatId = convId.toString(), participants = mapOf(sessionManager.getUserId().toString() to true, otherId.toString() to true), lastMessage = lastMsg, lastMessageTime = lastMillis)
                                list.add(chat)

                                val u = User(uid = otherId.toString(), username = username, imageBase64 = profile)
                                usersMap[otherId.toString()] = u

                                if (lastMsgId != -1 && lastMsg.isNotEmpty()) {
                                    val m = Message(
                                        messageId = lastMsgId.toString(), 
                                        senderId = it.optInt("lastSenderId", -1).toString(), 
                                        receiverId = sessionManager.getUserId().toString(), 
                                        text = lastMsg, 
                                        timestamp = lastMillis,
                                        chatId = convId.toString(),
                                        type = it.optString("lastMessageType", "text"),
                                        isSeen = it.optInt("lastMessageSeen", 0) == 1
                                    )
                                    messagesMap[lastMsgId.toString()] = m
                                }
                            }
                        }
                        updateAdapterSorted(list)
                        // Fetch online statuses for all users in chats
                        val userIds = usersMap.keys.toList()
                        if (userIds.isNotEmpty()) {
                            fetchOnlineStatusesForChats(userIds)
                        }
                    }
                } catch (e: Exception) { e.printStackTrace() }
            },
            { error -> error.printStackTrace() }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer ${sessionManager.getToken()}"
                return headers
            }
        }
        req.retryPolicy = DefaultRetryPolicy(15000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        rq.add(req)
    }
    
    private fun fetchOnlineStatusesForChats(userIds: List<String>) {
        val token = sessionManager.getToken() ?: return
        val idsParam = userIds.joinToString(",")
        val url = BuildConfig.BASE_URL + "get_statuses.php?user_ids=" + java.net.URLEncoder.encode(idsParam, "UTF-8")
        val rq = Volley.newRequestQueue(this)
        val req = object : StringRequest(Method.GET, url,
            { response ->
                try {
                    val obj = JSONObject(response.trim())
                    if (obj.optBoolean("success", false)) {
                        val statuses = obj.optJSONObject("statuses")
                        for (userId in userIds) {
                            val statusObj = statuses?.optJSONObject(userId)
                            if (statusObj != null) {
                                val user = usersMap[userId]
                                if (user != null) {
                                    user.onlineStatus = statusObj.optString("status", "offline")
                                    user.lastSeen = statusObj.optString("last_seen", null)
                                }
                            }
                        }
                        adapter.notifyDataSetChanged()
                    }
                } catch (e: Exception) { e.printStackTrace() }
            },
            { error -> error.printStackTrace() }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer $token"
                return headers
            }
        }
        rq.add(req)
    }

    private fun parseMySqlDatetimeToMillis(dt: String): Long {
        if (dt.isBlank()) return 0L
        return try {
            val fmt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            fmt.timeZone = java.util.TimeZone.getDefault()
            fmt.parse(dt)?.time ?: 0L
        } catch (e: Exception) { 0L }
    }

    // fetch followers and their online statuses, show horizontally above chats
    private fun loadOnlineFollowers() {
        val token = sessionManager.getToken() ?: return
        val url = BuildConfig.BASE_URL + "get_followers.php"
        val rq = Volley.newRequestQueue(this)
        val req = object : StringRequest(Method.GET, url,
            { response ->
                try {
                    val obj = JSONObject(response.trim())
                    if (obj.optBoolean("success", false)) {
                        val arr = obj.optJSONArray("followers")
                        if (arr != null) {
                            val ids = mutableListOf<String>()
                            val followers = mutableListOf<User>()
                            for (i in 0 until arr.length()) {
                                val it = arr.getJSONObject(i)
                                val uid = it.optInt("id", -1)
                                if (uid == -1) continue
                                ids.add(uid.toString())
                                followers.add(User(uid = uid.toString(), username = it.optString("username",""), imageBase64 = it.optString("imageBase64","")))
                            }
                            if (ids.isNotEmpty()) {
                                // call get_statuses.php
                                fetchOnlineStatuses(followers)
                            }
                        }
                    }
                } catch (e: Exception) { e.printStackTrace() }
            },
            { error -> error.printStackTrace() }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer ${sessionManager.getToken()}"
                return headers
            }
        }
        rq.add(req)
    }

    private fun fetchOnlineStatuses(followers: List<User>) {
        val token = sessionManager.getToken() ?: return
        val ids = followers.map { it.uid }
        val idsParam = ids.joinToString(",")
        val su = BuildConfig.BASE_URL + "get_statuses.php?user_ids=" + java.net.URLEncoder.encode(idsParam, "UTF-8")
        val rq2 = Volley.newRequestQueue(this)
        val req2 = object : StringRequest(Method.GET, su,
            { resp2 ->
                try {
                    val o2 = JSONObject(resp2.trim())
                    if (o2.optBoolean("success", false)) {
                        val statuses = o2.optJSONObject("statuses")
                        val online = mutableListOf<User>()
                        for (f in followers) {
                            val s = statuses?.optJSONObject(f.uid)
                            if (s != null) {
                                val status = s.optString("status", "offline")
                                f.onlineStatus = status
                                f.lastSeen = s.optString("last_seen", null)
                                if (status == "online") {
                                    online.add(f)
                                }
                            }
                        }
                        // update horizontal recycler
                        runOnUiThread {
                            val rv = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.online_recycler_view)
                            val adapter = rv.adapter as? OnlineUserAdapter
                            adapter?.updateData(online)
                        }
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }, { e -> e.printStackTrace() }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer $token"
                return headers
            }
        }
        rq2.add(req2)
    }

    private fun updateAdapterSorted(chatList: List<Chat>) {
        val sortedChats = chatList.sortedByDescending { it.lastMessageTime }
        chats.clear()
        chats.addAll(sortedChats)
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
                    // Restore the original chat list
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
        val token = sessionManager.getToken() ?: return
        val url = BuildConfig.BASE_URL + "search_users.php?q=${java.net.URLEncoder.encode(query, "UTF-8") }"
        val rq = Volley.newRequestQueue(this)
        val req = object : StringRequest(Method.GET, url,
            { response ->
                try {
                    val obj = JSONObject(response.trim())
                    if (obj.optBoolean("success", false)) {
                        val arr = obj.optJSONArray("users")
                        if (arr != null) {
                            for (i in 0 until arr.length()) {
                                val it = arr.getJSONObject(i)
                                val uid = it.optInt("id", -1)
                                if (uid == -1) continue
                                val user = User(uid = uid.toString(), username = it.optString("username", ""), imageBase64 = it.optString("profileImage", ""))
                                searchResults.add(user)
                                searchUsersMap[user.uid] = user
                            }
                            updateSearchAdapter()
                        }
                    }
                } catch (e: Exception) { e.printStackTrace() }
            },
            { error -> error.printStackTrace() }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer ${sessionManager.getToken()}"
                return headers
            }
        }
        req.retryPolicy = DefaultRetryPolicy(8000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        rq.add(req)
    }

    private fun updateSearchAdapter() {
        // Create a temporary list of "Chat" objects from the search results
        val searchChats = searchResults.map { user ->
            // Create a temporary Chat object with a special ID to indicate it's for a new conversation
            Chat(
                chatId = "search_${user.uid}",
                participants = mapOf(sessionManager.getUserId().toString() to true, user.uid to true),
                lastMessage = "Start a conversation with ${user.username}",
                lastMessageTime = System.currentTimeMillis()
            )
        }
        // Update the adapter with the search results
        adapter.updateData(searchChats, searchUsersMap, emptyMap())
    }

    private fun deleteChat(chat: Chat) {
        // deletion via REST not implemented on server; consider adding an endpoint
    }
}
