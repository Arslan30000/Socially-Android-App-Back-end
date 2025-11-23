package com.example.i230572_i230689

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class EightActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ChatsAdapter
    private lateinit var sessionManager: SessionManager
    private lateinit var dbHelper: LocalDbHelper

    private val chats = mutableListOf<Chat>()
    private val usersMap = mutableMapOf<String, User>()
    private val messagesMap = mutableMapOf<String, Message>()

    private var isSearching = false

    private var statusRefreshHandler: android.os.Handler? = null
    private var statusRefreshRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.chat)

        sessionManager = SessionManager(this)
        dbHelper = LocalDbHelper(this, sessionManager.getUserId().toString())

        recyclerView = findViewById(R.id.chats_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = ChatsAdapter(
            chats,
            sessionManager.getUserId().toString(),
            onChatClick = { chat, user ->
                val intent = Intent(this, NinthActivity::class.java)
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
        val onlineRv = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.online_recycler_view)
        onlineRv.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false)
        onlineRv.adapter = OnlineUserAdapter(mutableListOf()) { user ->
            val intent = Intent(this, NinthActivity::class.java)
            intent.putExtra("chatId", "")
            intent.putExtra("otherUserId", user.uid)
            startActivity(intent)
        }

        loadData() // Initial load
        setupSearch()
    }

    override fun onResume() {
        super.onResume()
        if (isOnline()) {
            setStatus("online")
            startStatusRefresh()
            // Do not call loadData() here to prevent flickering
            // Data is loaded in onCreate and refreshed by polling if online
        }
    }

    override fun onPause() {
        super.onPause()
        stopStatusRefresh()
        setStatus("offline") // Set status to offline when the activity is paused
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopStatusRefresh()
    }

    private fun isOnline(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            else -> false
        }
    }

    private fun loadData() {
        loadChatsFromCache()
        if (isOnline()) {
            loadChatsFromNetwork()
            loadOnlineFollowers()
        } else {
            Toast.makeText(this, "You are offline. Showing cached chats.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadChatsFromCache() {
        val cachedData = dbHelper.getConversationsWithUsers()
        if (cachedData.isNotEmpty()) {
            val cachedChats = cachedData.map { it.first }
            val cachedUsers = cachedData.map { it.second }.associateBy { it.uid }
            
            chats.clear()
            chats.addAll(cachedChats)
            usersMap.clear()
            usersMap.putAll(cachedUsers)
            
            updateAdapterSorted(cachedChats)
            Log.d("EightActivity", "Loaded ${cachedChats.size} chats from cache.")
        }
    }

    private fun loadChatsFromNetwork() {
        val token = sessionManager.getToken() ?: return
        val url = BuildConfig.BASE_URL + "get_recent_chats.php"
        val rq = Volley.newRequestQueue(this)
        val req = object : StringRequest(Method.GET, url,
            { response ->
                try {
                    val obj = JSONObject(response.trim())
                    if (obj.optBoolean("success", false)) {
                        val arr = obj.optJSONArray("conversations")
                        val networkData = mutableListOf<Pair<Chat, User>>()
                        val networkMessages = mutableMapOf<String, Message>()

                        if (arr != null) {
                            for (i in 0 until arr.length()) {
                                val it = arr.getJSONObject(i)
                                val convId = it.optInt("conversationId", -1)
                                val otherId = it.optInt("userId", -1)
                                val username = it.optString("username", "")
                                val profileBase64 = it.optString("profileImage", "")
                                val lastMsg = it.optString("lastMessage", "")
                                val lastMsgId = it.optInt("lastMessageId", -1)
                                val lastAt = it.optString("lastMessageAt", "")

                                if (convId == -1 || otherId == -1) continue

                                val lastMillis = parseMySqlDatetimeToMillis(lastAt)
                                val profileImagePath = saveImageToLocalCache(profileBase64, "pfp_$otherId")

                                val chat = Chat(chatId = convId.toString(), participants = mapOf(sessionManager.getUserId().toString() to true, otherId.toString() to true), lastMessage = lastMsg, lastMessageTime = lastMillis)
                                val user = User(uid = otherId.toString(), username = username, imageBase64 = profileImagePath)
                                networkData.add(Pair(chat, user))

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
                                    networkMessages[lastMsgId.toString()] = m
                                }
                            }
                        }
                        
                        val networkChats = networkData.map { it.first }
                        val networkUsers = networkData.map { it.second }.associateBy { it.uid }

                        chats.clear()
                        chats.addAll(networkChats)
                        usersMap.clear()
                        usersMap.putAll(networkUsers)
                        messagesMap.clear()
                        messagesMap.putAll(networkMessages)

                        updateAdapterSorted(networkChats)
                        dbHelper.upsertConversations(networkData)
                        
                        val userIds = usersMap.keys.toList()
                        if (userIds.isNotEmpty()) {
                            fetchOnlineStatusesForChats(userIds)
                        }
                    }
                } catch (e: Exception) { e.printStackTrace() }
            },
            { error -> error.printStackTrace() }) {
            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf("Authorization" to "Bearer $token")
            }
        }
        req.retryPolicy = DefaultRetryPolicy(15000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        rq.add(req)
    }
    
    private fun saveImageToLocalCache(base64String: String, fileName: String): String {
        if (base64String.isEmpty()) return ""
        return try {
            val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
            val file = File(filesDir, "$fileName.jpg")
            FileOutputStream(file).use {
                it.write(imageBytes)
            }
            file.absolutePath
        } catch (e: Exception) {
            Log.e("ImageCache", "Failed to save image $fileName: ${e.message}")
            ""
        }
    }
    
    private fun startStatusRefresh() {
        if (statusRefreshHandler == null) {
            statusRefreshHandler = android.os.Handler(android.os.Looper.getMainLooper())
        }
        stopStatusRefresh()
        statusRefreshRunnable = Runnable {
            if (isOnline()) {
                val userIds = usersMap.keys.toList()
                if (userIds.isNotEmpty()) {
                    fetchOnlineStatusesForChats(userIds)
                }
                loadOnlineFollowers()
            }
            statusRefreshHandler?.postDelayed(statusRefreshRunnable!!, 5000)
        }
        statusRefreshHandler?.post(statusRefreshRunnable!!)
    }
    
    private fun stopStatusRefresh() {
        statusRefreshRunnable?.let { statusRefreshHandler?.removeCallbacks(it) }
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
                return mutableMapOf("Authorization" to "Bearer ${sessionManager.getToken()}")
            }
        }
        req.retryPolicy = DefaultRetryPolicy(8000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
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
                        var changed = false
                        for (userId in userIds) {
                            val statusObj = statuses?.optJSONObject(userId)
                            if (statusObj != null) {
                                val user = usersMap[userId]
                                if (user != null) {
                                    val newStatus = statusObj.optString("status", "offline")
                                    if (user.onlineStatus != newStatus) {
                                        user.onlineStatus = newStatus
                                        user.lastSeen = statusObj.optString("last_seen", null)
                                        changed = true
                                    }
                                }
                            }
                        }
                        if (changed) {
                            adapter.notifyDataSetChanged() // Notify adapter if any status changed
                        }
                    }
                } catch (e: Exception) { e.printStackTrace() }
            },
            { error -> error.printStackTrace() }) {
            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf("Authorization" to "Bearer $token")
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
                                fetchOnlineStatuses(followers)
                            }
                        }
                    }
                } catch (e: Exception) { e.printStackTrace() }
            },
            { error -> error.printStackTrace() }) {
            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf("Authorization" to "Bearer ${sessionManager.getToken()}"
                )
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
                        var changed = false
                        for (f in followers) {
                            val s = statuses?.optJSONObject(f.uid)
                            if (s != null) {
                                val newStatus = s.optString("status", "offline")
                                if (f.onlineStatus != newStatus) {
                                    f.onlineStatus = newStatus
                                    f.lastSeen = s.optString("last_seen", null)
                                    changed = true
                                }
                                if (newStatus == "online") {
                                    online.add(f)
                                }
                            }
                        }
                        if (changed) {
                            runOnUiThread {
                                val rv = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.online_recycler_view)
                                val adapter = rv.adapter as? OnlineUserAdapter
                                adapter?.updateData(online)
                            }
                        }
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }, { e -> e.printStackTrace() }) {
            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf("Authorization" to "Bearer $token")
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
                val query = s.toString().lowercase().trim()
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
        val token = sessionManager.getToken() ?: return
        val url = BuildConfig.BASE_URL + "search_users.php?q=${java.net.URLEncoder.encode(query, "UTF-8")}"
        val rq = Volley.newRequestQueue(this)
        val req = object : StringRequest(Method.GET, url,
            { response ->
                try {
                    val obj = JSONObject(response.trim())
                    val searchResults = mutableListOf<User>()
                    val searchUsersMap = mutableMapOf<String, User>()
                    if (obj.optBoolean("success", false)) {
                        val arr = obj.optJSONArray("users")
                        if (arr != null && arr.length() > 0) {
                            val it = arr.getJSONObject(0)
                            val uid = it.optInt("id", -1)
                            if (uid != -1) {
                                val user = User(uid = uid.toString(), username = it.optString("username", ""), imageBase64 = it.optString("profileImage", ""))
                                searchResults.add(user)
                                searchUsersMap[user.uid] = user
                            }
                        }
                    }
                    updateSearchAdapter(searchResults, searchUsersMap)
                } catch (e: Exception) { e.printStackTrace() }
            },
            { error -> error.printStackTrace() }) {
            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf("Authorization" to "Bearer $token")
            }
        }
        req.retryPolicy = DefaultRetryPolicy(8000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        rq.add(req)
    }

    private fun updateSearchAdapter(searchResults: List<User>, searchUsersMap: Map<String, User>) {
        val searchChats = searchResults.map { user ->
            Chat(
                chatId = "search_${user.uid}",
                participants = mapOf(sessionManager.getUserId().toString() to true, user.uid to true),
                lastMessage = "Start a conversation with ${user.username}",
                lastMessageTime = System.currentTimeMillis()
            )
        }
        adapter.updateData(searchChats, searchUsersMap, emptyMap())
    }
}
