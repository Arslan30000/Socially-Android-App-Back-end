package com.example.i230572_i230689
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.InputType
import android.util.Base64
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
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
    private lateinit var otherUserId: String
    private var chatId: String? = null
    private val PICK_IMAGE_REQUEST = 1001
    private lateinit var sessionManager: SessionManager
    private lateinit var cache: ChatDbHelper
    private var vanishToggleState = false
    private var isSending = false

    private var statusRefreshHandler: android.os.Handler? = null
    private var statusRefreshRunnable: Runnable? = null

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

        sendButton.setOnClickListener { sendMessage() }
        galleryButton.setOnClickListener { openGallery() }
    }

    override fun onResume() {
        super.onResume()
        setStatus("online")
        startStatusRefresh()
    }

    override fun onPause() {
        super.onPause()
        stopStatusRefresh()
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
    }
    
    private fun startStatusRefresh() {
        if (statusRefreshHandler == null) {
            statusRefreshHandler = android.os.Handler(android.os.Looper.getMainLooper())
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
        if (!chatId.isNullOrEmpty() && chatId!!.all { it.isDigit() }) {
            val url = BuildConfig.BASE_URL + "get_messages.php?conversation_id=$chatId&limit=200"
            val rq = Volley.newRequestQueue(this)
            val req = object : StringRequest(Method.GET, url,
                { response ->
                    try {
                        val obj = JSONObject(response.trim())
                        if (obj.optBoolean("success", false)) {
                            val arr = obj.optJSONArray("messages")
                            messages.clear()
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
                                        vanishOnClose = it.optInt("vanish_on_close", 0) == 1
                                    )
                                    if (m.type == "image" && m.attachmentUrl.isEmpty() && m.text.isNotEmpty()) {
                                        m.imageBase64 = m.text
                                        m.text = ""
                                    }
                                    messages.add(m)
                                    cache.upsertMessage(m)
                                }
                            }
                            messageAdapter.notifyDataSetChanged()
                            if (messages.isNotEmpty()) recyclerView.scrollToPosition(messages.size - 1)
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
        } else {
            val conv = if (chatId.isNullOrEmpty()) otherUserId else chatId!!
            val cached = cache.getMessagesForConversation(conv)
            messages.clear(); messages.addAll(cached)
            messageAdapter.notifyDataSetChanged()
            if (messages.isNotEmpty()) recyclerView.scrollToPosition(messages.size - 1)
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
                            if (m.type == "image" && m.attachmentUrl.isEmpty() && m.text.isNotEmpty()) {
                                m.imageBase64 = m.text; m.text = ""
                            }
                            // avoid duplicate inserts if message already exists
                            if (messages.none { it.messageId == m.messageId }) {
                                messages.add(m)
                                cache.upsertMessage(m)
                                messageAdapter.notifyItemInserted(messages.size - 1)
                                recyclerView.scrollToPosition(messages.size - 1)
                            }
                            messageInput.text.clear()
                            chatId = m.chatId
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
            // upload binary to server (multipart) then send message with attachment_url
            Thread {
                try {
                    val uploadUrl = BuildConfig.BASE_URL + "upload_media.php"
                    val boundary = "----AndroidUpload${System.currentTimeMillis()}"
                    val urlObj = java.net.URL(uploadUrl)
                    val conn = urlObj.openConnection() as java.net.HttpURLConnection
                    conn.doOutput = true
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                    conn.setRequestProperty("Authorization", "Bearer ${sessionManager.getToken()}")
                    val out = java.io.DataOutputStream(conn.outputStream)
                    val filename = "img_${System.currentTimeMillis()}.jpg"
                    out.writeBytes("--$boundary\r\n")
                    out.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"$filename\"\r\n")
                    out.writeBytes("Content-Type: image/jpeg\r\n\r\n")
                    out.write(bytes)
                    out.writeBytes("\r\n")
                    out.writeBytes("--$boundary--\r\n")
                    out.flush()
                    out.close()
                    val respCode = conn.responseCode
                    val respStream = if (respCode in 200..299) conn.inputStream else conn.errorStream
                    val respText = respStream.bufferedReader().use { it.readText() }
                    val json = org.json.JSONObject(respText.trim())
                    if (json.optBoolean("success", false)) {
                        val urlPath = json.optString("url", "")
                        if (urlPath.isNotEmpty()) {
                            // Prepend BASE_URL to get full attachment URL
                            val fullUrl = BuildConfig.BASE_URL + urlPath
                            sendMessageWithAttachment(fullUrl)
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
    }

    private fun sendMessageWithAttachment(attachmentUrl: String) {
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
        obj.put("type", "image")
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
                                type = mObj.optString("type", "image")
                            )
                            if (messages.none { it.messageId == m.messageId }) {
                                messages.add(m)
                                cache.upsertMessage(m)
                                runOnUiThread {
                                    messageAdapter.notifyItemInserted(messages.size - 1)
                                    recyclerView.scrollToPosition(messages.size - 1)
                                }
                            }
                            chatId = m.chatId
                        }
                    } else {
                        runOnUiThread { Toast.makeText(this@NinthActivity, r.optString("message", "Failed to send image"), Toast.LENGTH_SHORT).show() }
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
                        msg.text = newContent
                        msg.isEdited = true
                        cache.markMessageEdited(msg.messageId, newContent)
                        messageAdapter.notifyDataSetChanged()
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
                        messages.remove(msg)
                        cache.deleteMessage(msg.messageId)
                        messageAdapter.notifyDataSetChanged()
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
