package com.example.i230572_i230689

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.InputType
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class NinthActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var messageInput: EditText
    private lateinit var sendButton: ImageView
    private lateinit var galleryButton: ImageView
    private lateinit var sendVideoIconButton: ImageView
    private lateinit var videoCallButton: ImageView
    private lateinit var audioCallButton: ImageView
    private lateinit var messageAdapter: MessageAdapter
    private val messages = mutableListOf<Message>()
    private val usersMap = mutableMapOf<String, User>()
    private lateinit var otherUserId: String
    private var chatId: String? = null
    private lateinit var sessionManager: SessionManager
    private lateinit var dbHelper: LocalDbHelper
    private var vanishToggleState = false
    private var isSending = false

    private var statusRefreshHandler: Handler? = null
    private var statusRefreshRunnable: Runnable? = null
    private var messageRefreshHandler: Handler? = null
    private var messageRefreshRunnable: Runnable? = null
    private var callCheckHandler: Handler? = null
    private var callCheckRunnable: Runnable? = null
    private var incomingCallDialog: AlertDialog? = null


    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleAttachment(uri, "image")
            }
        }
    }

    private val pickVideoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleAttachment(uri, "video")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.chat_page)

        chatId = intent.getStringExtra("chatId")
        otherUserId = intent.getStringExtra("otherUserId") ?: ""

        sessionManager = SessionManager(this)
        dbHelper = LocalDbHelper(this, sessionManager.getUserId().toString())
        val currentUserId = sessionManager.getUserId().toString()

        recyclerView = findViewById(R.id.chat_recycler_view)
        messageInput = findViewById(R.id.message_input)
        sendButton = findViewById(R.id.share_icon)
        galleryButton = findViewById(R.id.gallery_icon)
        sendVideoIconButton = findViewById(R.id.send_video_icon)
        videoCallButton = findViewById(R.id.video_call_icon)
        audioCallButton = findViewById(R.id.audio_call_icon)


        val vanishSwitch = findViewById<android.widget.Switch>(R.id.vanish_toggle)
        vanishToggleState = vanishSwitch.isChecked
        vanishSwitch.setOnCheckedChangeListener { _, isChecked -> vanishToggleState = isChecked }

        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        recyclerView.layoutManager = layoutManager

        messageAdapter = MessageAdapter(messages, currentUserId, usersMap, onMessageLongClick = { msg ->
            showEditDeleteDialog(msg)
        })
        recyclerView.adapter = messageAdapter

        loadOtherUser()
        loadMessages()
        
        sendButton.setOnClickListener { sendMessage() }
        galleryButton.setOnClickListener { openGalleryForImage() }
        sendVideoIconButton.setOnClickListener { openGalleryForVideo() }
        videoCallButton.setOnClickListener { initiateCall("video") }
        audioCallButton.setOnClickListener { initiateCall("audio") }
    }

    override fun onResume() {
        super.onResume()
        if (isOnline()) {
            setStatus("online")
        }
        startStatusRefresh()
        startMessageRefresh()
        startCallChecking()
    }

    override fun onPause() {
        super.onPause()
        stopStatusRefresh()
        stopMessageRefresh()
        stopCallChecking()
        if (isOnline()) {
            setStatus("offline") // Set status to offline when the activity is paused
        }
        if (!chatId.isNullOrEmpty()) {
            callVanishOnClose(chatId!!)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopStatusRefresh()
        stopMessageRefresh()
        stopCallChecking()
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

    private fun loadMessages() {
        val conversationIdentifier = chatId ?: otherUserId
        val cachedMessages = dbHelper.getMessagesForConversation(conversationIdentifier)
        updateMessages(cachedMessages)
        Log.d("NinthActivity", "Loaded ${cachedMessages.size} messages from cache for $conversationIdentifier.")

        if (isOnline() && !chatId.isNullOrEmpty()) {
            fetchMessagesFromNetwork()
        }
    }

    private fun fetchMessagesFromNetwork() {
        val url = BuildConfig.BASE_URL + "get_messages.php?conversation_id=$chatId&limit=200"
        val rq = Volley.newRequestQueue(this)
        val req = object : StringRequest(Method.GET, url,
            { response ->
                try {
                    val obj = JSONObject(response.trim())
                    if (obj.optBoolean("success", false)) {
                        val arr = obj.optJSONArray("messages")
                        val newMessages = mutableListOf<Message>()
                        if (arr != null) {
                            for (i in 0 until arr.length()) {
                                val it = arr.getJSONObject(i)
                                newMessages.add(Message(
                                    messageId = it.optString("id"),
                                    senderId = it.optString("sender_id"),
                                    receiverId = it.optString("receiver_id"),
                                    text = it.optString("content"),
                                    timestamp = it.optLong("created_at"),
                                    chatId = it.optString("conversation_id"),
                                    attachmentUrl = it.optString("attachment_url"),
                                    type = it.optString("type"),
                                    isSeen = it.optInt("is_seen", 0) == 1,
                                    isDeleted = it.optInt("is_deleted", 0) == 1,
                                    isEdited = it.optInt("is_edited", 0) == 1,
                                    vanishOnClose = it.optInt("vanish_on_close", 0) == 1
                                ))
                            }
                        }
                        updateMessages(newMessages)
                        dbHelper.upsertMessages(newMessages)
                        Log.d("NinthActivity", "Fetched and cached ${newMessages.size} messages.")
                        markSeen(chatId!!)
                    }
                } catch (e: Exception) { e.printStackTrace() }
            },
            { error -> error.printStackTrace() }) {
            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf("Authorization" to "Bearer ${sessionManager.getToken()}")
            }
        }
        rq.add(req)
    }

    private fun sendMessage() {
        val text = messageInput.text.toString().trim()
        if (text.isEmpty()) {
            Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        val url = BuildConfig.BASE_URL + "send_message.php"
        val payload = JSONObject().apply {
            if (!chatId.isNullOrEmpty() && chatId!!.all { it.isDigit() }) put("conversation_id", chatId!!.toInt())
            put("receiver_id", otherUserId.toInt())
            put("content", text)
            put("type", "text")
            put("vanish_on_close", if (vanishToggleState) 1 else 0)
        }

        if (isOnline()) {
            sendMessageToServer(url, payload)
        } else {
            Toast.makeText(this, "You are offline. Message will be sent later.", Toast.LENGTH_SHORT).show()
            dbHelper.queueAction("send_message", url, payload)
            messageInput.text.clear()
        }
    }

    private fun sendMessageToServer(url: String, payload: JSONObject) {
        if (isSending) return
        isSending = true
        sendButton.isEnabled = false
        val token = sessionManager.getToken() ?: return
        
        val rq = Volley.newRequestQueue(this)
        val req = object : StringRequest(Method.POST, url,
            { response ->
                try {
                    val r = JSONObject(response.trim())
                    if (r.optBoolean("success", false)) {
                        messageInput.text.clear()
                        fetchMessagesFromNetwork()
                    }
                } catch (e: Exception) { e.printStackTrace() }
                finally {
                    isSending = false
                    sendButton.isEnabled = true
                }
            },
            { error -> 
                error.printStackTrace()
                isSending = false
                sendButton.isEnabled = true
                Toast.makeText(this, "Failed to send message. Please try again.", Toast.LENGTH_SHORT).show()
            }) {
            override fun getBody(): ByteArray = payload.toString().toByteArray()
            override fun getBodyContentType(): String = "application/json"
            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf("Authorization" to "Bearer $token")
            }
        }
        req.retryPolicy = DefaultRetryPolicy(15000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        rq.add(req)
    }

    private fun initiateCall(callType: String) {
        val channelName = UUID.randomUUID().toString()
        val token = sessionManager.getToken() ?: return
        val url = BuildConfig.BASE_URL + "initiate_call.php"
        val rq = Volley.newRequestQueue(this)

        val callPayload = JSONObject().apply {
            put("receiver_id", otherUserId.toInt())
            put("channel_name", channelName)
            put("call_type", callType)
        }

        val req = object : StringRequest(Method.POST, url,
            { response ->
                try {
                    val res = JSONObject(response.trim())
                    if (res.optBoolean("success", false)) {
                        val intent = Intent(this@NinthActivity, TenthActivity::class.java).apply {
                            putExtra("channel_name", channelName)
                            putExtra("call_type", callType)
                            putExtra("agora_token", "")
                        }
                        startActivity(intent)
                    } else {
                        Toast.makeText(this@NinthActivity, res.optString("message", "User is busy or unavailable."), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this@NinthActivity, "Failed to parse server response.", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                error.printStackTrace()
                Toast.makeText(this@NinthActivity, "Network error: Could not initiate call.", Toast.LENGTH_SHORT).show()
            }) {
            override fun getBody(): ByteArray = callPayload.toString().toByteArray()
            override fun getBodyContentType(): String = "application/json"
            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf("Authorization" to "Bearer $token")
            }
        }
        rq.add(req)
    }

    private fun startCallChecking() {
        if (callCheckHandler == null) {
            callCheckHandler = Handler(Looper.getMainLooper())
        }
        stopCallChecking()
        callCheckRunnable = Runnable {
            if (!isDestroyed && isOnline()) {
                checkForIncomingCalls()
            }
            callCheckHandler?.postDelayed(callCheckRunnable!!, 5000)
        }
        callCheckHandler?.post(callCheckRunnable!!)
    }

    private fun stopCallChecking() {
        callCheckRunnable?.let { callCheckHandler?.removeCallbacks(it) }
    }

    private fun checkForIncomingCalls() {
        if (incomingCallDialog != null && incomingCallDialog!!.isShowing) {
            return
        }

        val token = sessionManager.getToken() ?: return
        val url = BuildConfig.BASE_URL + "check_for_call.php"
        val rq = Volley.newRequestQueue(this)

        val req = object : StringRequest(Method.GET, url,
            { response ->
                try {
                    val res = JSONObject(response.trim())
                    if (res.optBoolean("success") && res.optBoolean("call_waiting")) {
                        val callerName = res.getString("caller_name")
                        val channelName = res.getString("channel_name")
                        val callType = res.getString("call_type")
                        showIncomingCallDialog(callerName, channelName, callType)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            { /* Suppress errors */ }) {
            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf("Authorization" to "Bearer $token")
            }
        }
        rq.add(req)
    }

    private fun showIncomingCallDialog(callerName: String, channelName: String, callType: String) {
        val callTypeName = if (callType == "video") "Video" else "Audio"
        
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Incoming $callTypeName Call")
        builder.setMessage("$callerName is calling you.")
        builder.setPositiveButton("Accept") { dialog, _ ->
            val intent = Intent(this, TenthActivity::class.java).apply {
                putExtra("channel_name", channelName)
                putExtra("call_type", callType)
                putExtra("agora_token", "")
            }
            startActivity(intent)
            dialog.dismiss()
        }
        builder.setNegativeButton("Decline") { dialog, _ ->
            endCall(channelName)
            dialog.dismiss()
        }
        builder.setCancelable(false)

        incomingCallDialog = builder.create()
        incomingCallDialog?.show()
    }

    private fun endCall(channelName: String) {
        val token = sessionManager.getToken() ?: return
        val url = BuildConfig.BASE_URL + "end_call.php"
        val rq = Volley.newRequestQueue(this)

        val payload = JSONObject().apply {
            put("channel_name", channelName)
        }

        val req = object : StringRequest(Method.POST, url,
            { /* Success */ },
            { /* Suppress errors */ }) {
            override fun getBody(): ByteArray = payload.toString().toByteArray()
            override fun getBodyContentType(): String = "application/json"
            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf("Authorization" to "Bearer $token")
            }
        }
        rq.add(req)
    }
    
    private fun startStatusRefresh() {
        if (statusRefreshHandler == null) {
            statusRefreshHandler = Handler(Looper.getMainLooper())
        }
        stopStatusRefresh() 
        statusRefreshRunnable = Runnable {
            if (!isDestroyed && otherUserId.isNotEmpty() && isOnline()) {
                fetchUserOnlineStatus(otherUserId)
            }
            statusRefreshHandler?.postDelayed(statusRefreshRunnable!!, 5000)
        }
        statusRefreshHandler?.post(statusRefreshRunnable!!)
    }
    
    private fun stopStatusRefresh() {
        statusRefreshRunnable?.let { statusRefreshHandler?.removeCallbacks(it) }
    }

    private fun startMessageRefresh() {
        if (messageRefreshHandler == null) {
            messageRefreshHandler = Handler(Looper.getMainLooper())
        }
        stopMessageRefresh()
        messageRefreshRunnable = Runnable {
            if (!isDestroyed && !chatId.isNullOrEmpty() && isOnline()) {
                fetchMessagesFromNetwork()
            }
            messageRefreshHandler?.postDelayed(messageRefreshRunnable!!, 5000) // Poll every 5 seconds
        }
        messageRefreshHandler?.post(messageRefreshRunnable!!)
    }

    private fun stopMessageRefresh() {
        messageRefreshRunnable?.let { messageRefreshHandler?.removeCallbacks(it) }
    }

    private fun setStatus(status: String) {
        val token = sessionManager.getToken() ?: return
        val url = BuildConfig.BASE_URL + "set_status.php"
        val rq = Volley.newRequestQueue(this)
        val req = object : StringRequest(Method.POST, url,
            { _ -> },
            { e -> e.printStackTrace() }) {
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

    private fun loadOtherUser() {
        val url = BuildConfig.BASE_URL + "get_profile.php?user_id=$otherUserId"
        val rq = Volley.newRequestQueue(this)

        val req = object : StringRequest(
            Method.GET, url,
            { response ->
                try {
                    val obj = JSONObject(response.trim())
                    if (obj.optBoolean("success", false)) {
                        val userObj = obj.optJSONObject("user")
                        if (userObj != null) {
                            val user = User(
                                uid = userObj.optString("id", otherUserId),
                                name = userObj.optString("name", ""),
                                lastname = userObj.optString("lastname", ""),
                                username = userObj.optString("username", ""),
                                imageBase64 = userObj.optString("imageBase64", "")
                            )
                            usersMap[otherUserId] = user
                            
                            findViewById<TextView>(R.id.chat_name).text =
                                "${user.name} ${user.lastname}".trim()
                            
                            fetchUserOnlineStatus(otherUserId)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            { error -> error.printStackTrace() }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer ${sessionManager.getToken()}"
                return headers
            }
        }

        rq.add(req)
    }
    
    private fun fetchUserOnlineStatus(userId: String) {
        val token = sessionManager.getToken() ?: return
        val url = BuildConfig.BASE_URL + "get_statuses.php?user_ids=$userId"
        val rq = Volley.newRequestQueue(this)
        val req = object : StringRequest(Method.GET, url,
            { response ->
                try {
                    val obj = JSONObject(response.trim())
                    if (obj.optBoolean("success", false)) {
                        val statuses = obj.optJSONObject("statuses")
                        val statusObj = statuses?.optJSONObject(userId)
                        if (statusObj != null) {
                            val user = usersMap[userId]
                            if (user != null) {
                                val newStatus = statusObj.optString("status", "offline")
                                if (user.onlineStatus != newStatus) {
                                    user.onlineStatus = newStatus
                                    user.lastSeen = statusObj.optString("last_seen", null)
                                    updateChatHeaderStatus(newStatus) // Update UI immediately
                                }
                            }
                        }
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
    
    private fun updateChatHeaderStatus(status: String) {
        try {
            val statusView = findViewById<TextView>(R.id.chat_status)
            if (statusView != null) {
                statusView.text = if (status == "online") "Online" else "Offline"
                statusView.setTextColor(if (status == "online") 0xFF4CAF50.toInt() else 0xFF9E9E9E.toInt())
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun updateMessages(newMessages: List<Message>) {
        if (messages != newMessages) {
            messages.clear()
            messages.addAll(newMessages)
            messageAdapter.notifyDataSetChanged()
            recyclerView.scrollToPosition(messages.size - 1)
        }
    }

    private fun openGalleryForImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun openGalleryForVideo() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        pickVideoLauncher.launch(intent)
    }

    private fun handleAttachment(uri: Uri, type: String) {
        if (!isOnline()) {
            Toast.makeText(this, "Cannot send attachments while offline.", Toast.LENGTH_SHORT).show()
            return
        }
        
        val inputStream = contentResolver.openInputStream(uri)
        val bytes = inputStream?.readBytes()
        inputStream?.close()

        if (bytes != null) {
            uploadAttachment(bytes, type)
        }
    }

    private fun uploadAttachment(bytes: ByteArray, type: String) {
        Thread {
            try {
                val uploadUrl = if (type == "image") {
                    BuildConfig.BASE_URL + "upload_chat_media.php"
                } else {
                    BuildConfig.BASE_URL + "upload_video.php"
                }
                val boundary = "----AndroidUpload${System.currentTimeMillis()}"
                val urlObj = URL(uploadUrl)
                val conn = urlObj.openConnection() as HttpURLConnection
                conn.doOutput = true
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                conn.setRequestProperty("Authorization", "Bearer ${sessionManager.getToken()}")

                val out = DataOutputStream(conn.outputStream)
                val filename = if (type == "image") "img_${System.currentTimeMillis()}.jpg" else "vid_${System.currentTimeMillis()}.mp4"
                val fileField = if (type == "image") "file" else "video"

                out.writeBytes("--$boundary\r\n")
                out.writeBytes("Content-Disposition: form-data; name=\"$fileField\"; filename=\"$filename\"\r\n")
                out.writeBytes("Content-Type: ${if (type == "image") "image/jpeg" else "video/mp4"}\r\n\r\n")
                out.write(bytes)
                out.writeBytes("\r\n")
                out.writeBytes("--$boundary--\r\n")
                out.flush()
                out.close()

                val respCode = conn.responseCode
                val respStream = if (respCode in 200..299) conn.inputStream else conn.errorStream
                val respText = respStream.bufferedReader().use { it.readText() }
                val json = JSONObject(respText.trim())

                if (json.optBoolean("success", false)) {
                    val urlPath = json.optString("url", "")
                    if (urlPath.isNotEmpty()) {
                        sendMessageWithAttachment(urlPath, type)
                    } else {
                        runOnUiThread { Toast.makeText(this@NinthActivity, "Upload failed: no url", Toast.LENGTH_SHORT).show() }
                    }
                } else {
                    runOnUiThread { Toast.makeText(this@NinthActivity, json.optString("message", "Upload failed"), Toast.LENGTH_SHORT).show() }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread { Toast.makeText(this@NinthActivity, "Upload error", Toast.LENGTH_SHORT).show() }
            }
        }.start()
    }

    private fun sendMessageWithAttachment(attachmentUrl: String, type: String) {
        val url = BuildConfig.BASE_URL + "send_message.php"
        val payload = JSONObject().apply {
            if (!chatId.isNullOrEmpty() && chatId!!.all { it.isDigit() }) put("conversation_id", chatId!!.toInt())
            put("receiver_id", otherUserId.toInt())
            put("content", "")
            put("type", type)
            put("attachment_url", attachmentUrl)
            put("vanish_on_close", if (vanishToggleState) 1 else 0)
        }
        sendMessageToServer(url, payload)
    }

    private fun markSeen(convId: String) {
        val token = sessionManager.getToken() ?: return
        val url = BuildConfig.BASE_URL + "mark_seen.php"
        val rq = Volley.newRequestQueue(this)
        val obj = JSONObject()
        obj.put("conversation_id", convId.toInt())
        val req = object : StringRequest(Method.POST, url,
            { _ -> },
            { error -> error.printStackTrace() }) {
            override fun getBody(): ByteArray? = obj.toString().toByteArray()
            override fun getBodyContentType(): String = "application/json"
            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf("Authorization" to "Bearer ${sessionManager.getToken()}")
            }
        }
        rq.add(req)
    }

    private fun callVanishOnClose(convId: String) {
        val token = sessionManager.getToken() ?: return
        val url = BuildConfig.BASE_URL + "vanish_on_close.php"
        val rq = Volley.newRequestQueue(this)
        val obj = JSONObject()
        obj.put("conversation_id", convId.toInt())
        val req = object : StringRequest(Method.POST, url,
            { _ -> },
            { error -> error.printStackTrace() }) {
            override fun getBody(): ByteArray? = obj.toString().toByteArray()
            override fun getBodyContentType(): String = "application/json"
            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf("Authorization" to "Bearer ${sessionManager.getToken()}")
            }
        }
        rq.add(req)
    }

    private fun showEditDeleteDialog(msg: Message) {
        val currentId = sessionManager.getUserId().toString()
        if (msg.senderId != currentId || msg.messageId.startsWith("pending_")) return
        
        val options = mutableListOf<String>()
        options.add("Edit")
        options.add("Delete")

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Message options")
        builder.setItems(options.toTypedArray()) { _, which ->
            when (options[which]) {
                "Edit" -> showEditInputDialog(msg)
                "Delete" -> confirmDelete(msg)
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun showEditInputDialog(msg: Message) {
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        input.setText(msg.text)
        AlertDialog.Builder(this)
            .setTitle("Edit message")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newContent = input.text.toString().trim()
                if (newContent.isNotEmpty()) editMessageOnServer(msg, newContent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDelete(msg: Message) {
        AlertDialog.Builder(this)
            .setTitle("Delete message")
            .setMessage("Delete this message? This can be done within 5 minutes of sending.")
            .setPositiveButton("Delete") { _, _ -> deleteMessageOnServer(msg) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun editMessageOnServer(msg: Message, newContent: String) {
        val url = BuildConfig.BASE_URL + "edit_message.php"
        val payload = JSONObject().apply {
            put("message_id", msg.messageId)
            put("content", newContent)
        }
        
        if (isOnline()) {
            val token = sessionManager.getToken() ?: return
            val rq = Volley.newRequestQueue(this)
            val req = object : StringRequest(Method.POST, url,
                { response ->
                    try {
                        val r = JSONObject(response.trim())
                        if (r.optBoolean("success", false)) {
                            fetchMessagesFromNetwork()
                        } else {
                            Toast.makeText(this, r.optString("message", "Failed to edit"), Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                },
                { error -> error.printStackTrace() }) {
                override fun getBody(): ByteArray = payload.toString().toByteArray()
                override fun getBodyContentType(): String = "application/json"
                override fun getHeaders(): MutableMap<String, String> {
                    return mutableMapOf("Authorization" to "Bearer $token")
                }
            }
            rq.add(req)
        } else {
            dbHelper.queueAction("edit_message", url, payload)
            Toast.makeText(this, "Offline. Edit will be synced later.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteMessageOnServer(msg: Message) {
        val url = BuildConfig.BASE_URL + "delete_message.php"
        val payload = JSONObject().apply {
            put("message_id", msg.messageId)
        }

        if (isOnline()) {
            val token = sessionManager.getToken() ?: return
            val rq = Volley.newRequestQueue(this)
            val req = object : StringRequest(Method.POST, url,
                { response ->
                    try {
                        val r = JSONObject(response.trim())
                        if (r.optBoolean("success", false)) {
                            fetchMessagesFromNetwork()
                        } else {
                            Toast.makeText(this, r.optString("message", "Failed to delete"), Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                },
                { error -> error.printStackTrace() }) {
                override fun getBody(): ByteArray = payload.toString().toByteArray()
                override fun getBodyContentType(): String = "application/json"
                override fun getHeaders(): MutableMap<String, String> {
                    return mutableMapOf("Authorization" to "Bearer $token")
                }
            }
            rq.add(req)
        } else {
            dbHelper.queueAction("delete_message", url, payload)
            Toast.makeText(this, "Offline. Deletion will be synced later.", Toast.LENGTH_SHORT).show()
        }
    }
}
