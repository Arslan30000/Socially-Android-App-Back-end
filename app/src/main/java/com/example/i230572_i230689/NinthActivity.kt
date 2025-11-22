package com.example.i230572_i230689
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.InputType
import android.util.Base64
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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

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
    private lateinit var otherUserId: String
    private var chatId: String? = null
    private lateinit var sessionManager: SessionManager
    private lateinit var cache: ChatDbHelper
    private var vanishToggleState = false
    private var isSending = false

    private var statusRefreshHandler: Handler? = null
    private var statusRefreshRunnable: Runnable? = null
    private var messageRefreshHandler: Handler? = null
    private var messageRefreshRunnable: Runnable? = null

    private lateinit var callListener: ValueEventListener

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
        cache = ChatDbHelper(this)
        val currentUserId = sessionManager.getUserId().toString()

        recyclerView = findViewById(R.id.chat_recycler_view)
        messageInput = findViewById(R.id.message_input)
        sendButton = findViewById(R.id.share_icon)
        galleryButton = findViewById(R.id.gallery_icon)
        videoButton = findViewById(R.id.video_icon)
        audioButton = findViewById(R.id.info_icon)

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
        startStatusRefresh()
        startMessageRefresh()
        listenForIncomingCalls()

        sendButton.setOnClickListener { sendMessage() }
        galleryButton.setOnClickListener { openGallery() }
        videoButton.setOnClickListener { openVideoGallery() }
        audioButton.setOnClickListener { initiateCall("audio") }
    }

    private fun initiateCall(callType: String) {
        val channelName = UUID.randomUUID().toString()
        val callRef = FirebaseDatabase.getInstance().getReference("calls/${otherUserId}")
        val callData = mapOf(
            "callerId" to sessionManager.getUserId(),
            "channelName" to channelName,
            "callType" to callType,
            "status" to "ringing"
        )
        callRef.setValue(callData).addOnSuccessListener {
            val intent = Intent(this, TenthActivity::class.java).apply {
                putExtra("channel_name", channelName)
                putExtra("call_type", callType)
            }
            startActivity(intent)
        }
    }

    private fun listenForIncomingCalls() {
        val currentUserId = sessionManager.getUserId()
        val callRef = FirebaseDatabase.getInstance().getReference("calls/$currentUserId")
        callListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val status = snapshot.child("status").getValue(String::class.java)
                    if (status == "ringing") {
                        val callerId = snapshot.child("callerId").getValue(String::class.java)
                        val channelName = snapshot.child("channelName").getValue(String::class.java)
                        val callType = snapshot.child("callType").getValue(String::class.java)

                        val intent = Intent(this@NinthActivity, IncomingCallActivity::class.java).apply {
                            putExtra("caller_id", callerId)
                            putExtra("channel_name", channelName)
                            putExtra("call_type", callType)
                        }
                        startActivity(intent)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        }
        callRef.addValueEventListener(callListener)
    }

    override fun onResume() {
        super.onResume()
        setStatus("online")
        startStatusRefresh()
        startMessageRefresh()
    }

    override fun onPause() {
        super.onPause()
        stopStatusRefresh()
        stopMessageRefresh()
        setStatus("offline")
        if (!chatId.isNullOrEmpty()) {
            callVanishOnClose(chatId!!)
            messages.clear()
            messageAdapter.notifyDataSetChanged()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopStatusRefresh()
        stopMessageRefresh()
        val currentUserId = sessionManager.getUserId()
        FirebaseDatabase.getInstance().getReference("calls/$currentUserId").removeEventListener(callListener)
    }
    
    private fun startStatusRefresh() {
        if (statusRefreshHandler == null) {
            statusRefreshHandler = Handler(Looper.getMainLooper())
        }
        stopStatusRefresh() // Clear any existing runnable
        statusRefreshRunnable = Runnable {
            if (!isDestroyed && otherUserId.isNotEmpty()) {
                fetchUserOnlineStatus(otherUserId)
            }
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

    private fun startMessageRefresh() {
        if (messageRefreshHandler == null) {
            messageRefreshHandler = Handler(Looper.getMainLooper())
        }
        stopMessageRefresh()
        messageRefreshRunnable = Runnable {
            if (!isDestroyed && !chatId.isNullOrEmpty()) {
                loadMessages()
            }
            messageRefreshHandler?.postDelayed(messageRefreshRunnable!!, 2000) // Poll every 2 seconds
        }
        messageRefreshHandler?.post(messageRefreshRunnable!!)
    }

    private fun stopMessageRefresh() {
        if (messageRefreshHandler != null && messageRefreshRunnable != null) {
            messageRefreshHandler?.removeCallbacks(messageRefreshRunnable!!)
        }
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

                            val pic = findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.profile_pic)
                            if (user.imageBase64.isNotEmpty()) {
                                val bytes = Base64.decode(user.imageBase64, Base64.DEFAULT)
                                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                pic.setImageBitmap(bmp)
                            }

                            findViewById<TextView>(R.id.chat_name).text =
                                "${user.name} ${user.lastname}".trim()
                            
                            // Fetch and display online status
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
                            val status = statusObj.optString("status", "offline")
                            val user = usersMap[userId]
                            if (user != null) {
                                user.onlineStatus = status
                                user.lastSeen = statusObj.optString("last_seen", null)
                                // Update UI to show online status
                                updateChatHeaderStatus(status)
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


    private fun loadMessages() {
        if (chatId.isNullOrEmpty() || !chatId!!.all { it.isDigit() }) {
            // If there's no valid chatId, we can't load messages.
            // Check for cached messages for this user.
            val cached = cache.getMessagesForConversation(otherUserId)
            if (cached.isNotEmpty()) {
                messages.clear()
                messages.addAll(cached)
                messageAdapter.notifyDataSetChanged()
                if (messages.isNotEmpty()) recyclerView.scrollToPosition(messages.size - 1)
            }
            return
        }

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
                                val m = Message(
                                    messageId = it.optString("id", i.toString()),
                                    senderId = it.optString("sender_id", ""),
                                    receiverId = it.optString("receiver_id", ""),
                                    text = it.optString("content", ""),
                                    imageBase64 = "",
                                    postId = "",
                                    timestamp = it.optLong("created_at", 0),
                                    chatId = it.optString("conversation_id", chatId!!),
                                    attachmentUrl = it.optString("attachment_url", ""),
                                    type = it.optString("type", "text"),
                                    isSeen = it.optInt("is_seen", 0) == 1,
                                    isDeleted = it.optInt("is_deleted", 0) == 1,
                                    isEdited = it.optInt("is_edited", 0) == 1,
                                    vanishOnClose = it.optInt("vanish_on_close", 0) == 1
                                )
                                newMessages.add(m)
                            }
                        }
                        
                        // Smartly update the RecyclerView
                        updateMessages(newMessages)
                        
                        markSeen(chatId!!)
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

    private fun updateMessages(newMessages: List<Message>) {
        val currentMessageIds = messages.map { it.messageId }.toSet()
        val newMessagesMap = newMessages.associateBy { it.messageId }
        val newMessagesIds = newMessagesMap.keys

        // Find messages to remove
        val messagesToRemove = messages.filter { it.messageId !in newMessagesIds }
        messagesToRemove.forEach { msg ->
            val index = messages.indexOfFirst { it.messageId == msg.messageId }
            if (index != -1) {
                messages.removeAt(index)
                messageAdapter.notifyItemRemoved(index)
            }
        }

        // Find messages to add or update
        newMessages.forEach { newMessage ->
            val existingMessage = messages.find { it.messageId == newMessage.messageId }
            if (existingMessage == null) {
                // Add new message
                messages.add(newMessage)
                messageAdapter.notifyItemInserted(messages.size - 1)
                recyclerView.scrollToPosition(messages.size - 1)
            } else {
                // Update existing message if content or edited status has changed
                if (existingMessage.text != newMessage.text || existingMessage.isEdited != newMessage.isEdited) {
                    existingMessage.text = newMessage.text
                    existingMessage.isEdited = newMessage.isEdited
                    val index = messages.indexOf(existingMessage)
                    messageAdapter.notifyItemChanged(index)
                }
            }
        }
    }

    private fun sendMessage() {
        val text = messageInput.text.toString().trim()
        if (text.isEmpty()) {
            Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        if (isSending) return
        isSending = true
        sendButton.isEnabled = false
        val token = sessionManager.getToken() ?: run { isSending = false; sendButton.isEnabled = true; return }
        val url = BuildConfig.BASE_URL + "send_message.php"
        val rq = Volley.newRequestQueue(this)
        val obj = JSONObject()
        if (!chatId.isNullOrEmpty() && chatId!!.all { it.isDigit() }) obj.put("conversation_id", chatId!!.toInt())
        obj.put("receiver_id", otherUserId.toInt())
        obj.put("content", text)
        obj.put("type", "text")
        obj.put("vanish_on_close", if (vanishToggleState) 1 else 0)
        val req = object : StringRequest(Method.POST, url,
            { response ->
                try {
                    val r = JSONObject(response.trim())
                    if (r.optBoolean("success", false)) {
                        val mObj = r.optJSONObject("message")
                        if (mObj != null) {
                            val m = Message(
                                messageId = mObj.optString("id", ""),
                                senderId = mObj.optString("sender_id", sessionManager.getUserId().toString()),
                                receiverId = mObj.optString("receiver_id", otherUserId),
                                text = mObj.optString("content", ""),
                                imageBase64 = "",
                                postId = "",
                                timestamp = mObj.optLong("created_at", System.currentTimeMillis()),
                                chatId = mObj.optString("conversation_id", chatId),
                                attachmentUrl = mObj.optString("attachment_url", ""),
                                type = mObj.optString("type", "text")
                            )
                            
                            if (messages.none { it.messageId == m.messageId }) {
                                messages.add(m)
                                cache.upsertMessage(m)
                                messageAdapter.notifyItemInserted(messages.size - 1)
                                recyclerView.scrollToPosition(messages.size - 1)
                            }
                            messageInput.text.clear()
                            if (chatId.isNullOrEmpty()) {
                                chatId = m.chatId
                            }
                        }
                    }
                } catch (e: Exception) { e.printStackTrace() }
                finally {
                    isSending = false
                    sendButton.isEnabled = true
                }
            },
            { error -> error.printStackTrace(); isSending = false; sendButton.isEnabled = true } ) {
            override fun getBody(): ByteArray? = obj.toString().toByteArray()
            override fun getBodyContentType(): String = "application/json"
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer $token"
                return headers
            }
        }
        req.retryPolicy = DefaultRetryPolicy(15000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        rq.add(req)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun openVideoGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        pickVideoLauncher.launch(intent)
    }

    private fun handleAttachment(uri: Uri, type: String) {
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
        if (isSending) return
        isSending = true
        runOnUiThread { sendButton.isEnabled = false }
        val token = sessionManager.getToken() ?: run { isSending = false; runOnUiThread { sendButton.isEnabled = true }; return }
        val url = BuildConfig.BASE_URL + "send_message.php"
        val rq = Volley.newRequestQueue(this)
        val obj = JSONObject()
        if (!chatId.isNullOrEmpty() && chatId!!.all { it.isDigit() }) obj.put("conversation_id", chatId!!.toInt())
        obj.put("receiver_id", otherUserId.toInt())
        obj.put("content", "")
        obj.put("type", type)
        obj.put("attachment_url", attachmentUrl)
        obj.put("vanish_on_close", if (vanishToggleState) 1 else 0)
        val req = object : StringRequest(Method.POST, url,
            { response ->
                try {
                    val r = JSONObject(response.trim())
                    if (r.optBoolean("success", false)) {
                        val mObj = r.optJSONObject("message")
                        if (mObj != null) {
                            val m = Message(
                                messageId = mObj.optString("id", ""),
                                senderId = mObj.optString("sender_id", sessionManager.getUserId().toString()),
                                receiverId = mObj.optString("receiver_id", otherUserId),
                                text = "",
                                imageBase64 = "",
                                postId = "",
                                timestamp = mObj.optLong("created_at", System.currentTimeMillis()),
                                chatId = mObj.optString("conversation_id", chatId),
                                attachmentUrl = mObj.optString("attachment_url", ""),
                                type = mObj.optString("type", type)
                            )
                            if (messages.none { it.messageId == m.messageId }) {
                                messages.add(m)
                                cache.upsertMessage(m)
                                runOnUiThread {
                                    messageAdapter.notifyItemInserted(messages.size - 1)
                                    recyclerView.scrollToPosition(messages.size - 1)
                                }
                            }
                            if (chatId.isNullOrEmpty()) {
                                chatId = m.chatId
                            }
                        }
                    } else {
                        runOnUiThread { Toast.makeText(this@NinthActivity, r.optString("message", "Failed to send attachment"), Toast.LENGTH_SHORT).show() }
                    }
                } catch (e: Exception) { e.printStackTrace() }
                finally {
                    isSending = false
                    runOnUiThread { sendButton.isEnabled = true }
                }
            },
            { error -> error.printStackTrace(); isSending = false; runOnUiThread { sendButton.isEnabled = true } }) {
            override fun getBody(): ByteArray? = obj.toString().toByteArray()
            override fun getBodyContentType(): String = "application/json"
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer $token"
                return headers
            }
        }
        rq.add(req)
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
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer ${sessionManager.getToken()}"
                return headers
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
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer ${sessionManager.getToken()}"
                return headers
            }
        }
        rq.add(req)
    }

    private fun showEditDeleteDialog(msg: Message) {
        val currentId = sessionManager.getUserId().toString()
        if (msg.senderId != currentId) return
        val createdAt = msg.timestamp
        val now = System.currentTimeMillis()
        val createdMillis = if (createdAt < 1000000000000L) createdAt * 1000 else createdAt
        val allowed = now - createdMillis <= 5 * 60 * 1000
        val options = mutableListOf<String>()
        if (allowed) options.add("Edit")
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
        val token = sessionManager.getToken() ?: return
        val url = BuildConfig.BASE_URL + "edit_message.php"
        val rq = Volley.newRequestQueue(this)
        val obj = JSONObject()
        obj.put("message_id", msg.messageId)
        obj.put("content", newContent)
        val req = object : StringRequest(Method.POST, url,
            { response ->
                try {
                    val r = JSONObject(response.trim())
                    if (r.optBoolean("success", false)) {
                        // The polling will handle the UI update
                    } else {
                        Toast.makeText(this, r.optString("message", "Failed to edit"), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) { e.printStackTrace() }
            },
            { error -> error.printStackTrace() }) {
            override fun getBody(): ByteArray? = obj.toString().toByteArray()
            override fun getBodyContentType(): String = "application/json"
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer $token"
                return headers
            }
        }
        rq.add(req)
    }

    private fun deleteMessageOnServer(msg: Message) {
        val token = sessionManager.getToken() ?: return
        val url = BuildConfig.BASE_URL + "delete_message.php"
        val rq = Volley.newRequestQueue(this)
        val obj = JSONObject()
        obj.put("message_id", msg.messageId)
        val req = object : StringRequest(
            Method.POST, url,
            { response ->
                try {
                    val r = JSONObject(response.trim())
                    if (r.optBoolean("success", false)) {
                        // The polling will handle the UI update
                    } else {
                        Toast.makeText(
                            this,
                            r.optString("message", "Failed to delete"),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            { error -> error.printStackTrace() }) {
            override fun getBody(): ByteArray? = obj.toString().toByteArray()
            override fun getBodyContentType(): String = "application/json"
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer $token"
                return headers
            }
        }
        rq.add(req)
    }
}
