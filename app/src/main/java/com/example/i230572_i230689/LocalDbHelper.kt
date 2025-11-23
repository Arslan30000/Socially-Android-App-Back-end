package com.example.i230572_i230689

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONObject

data class QueuedAction(
    val id: Long,
    val type: String,
    val url: String,
    val payload: String
)

class LocalDbHelper(private val context: Context, userId: String) : SQLiteOpenHelper(context, DB_NAME_PREFIX + userId + DB_SUFFIX, null, DB_VERSION) {
    companion object {
        private const val DB_NAME_PREFIX = "instagram_cache_user_"
        private const val DB_SUFFIX = ".db"
        private const val DB_VERSION = 1 // Version can be reset for this new architecture
        
        private const val T_MESSAGES = "messages"
        private const val T_POSTS = "posts"
        private const val T_STORIES = "stories"
        private const val T_CONVERSATIONS = "conversations"
        private const val T_QUEUED_ACTIONS = "queued_actions"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $T_MESSAGES (
              messageId TEXT PRIMARY KEY,
              senderId TEXT,
              receiverId TEXT,
              text TEXT,
              timestamp INTEGER,
              chatId TEXT,
              attachmentUrl TEXT,
              type TEXT,
              isSeen INTEGER
            )
        """)

        db.execSQL("""
            CREATE TABLE $T_POSTS (
                postId TEXT PRIMARY KEY,
                userId TEXT,
                postImage TEXT, 
                caption TEXT,
                timestamp INTEGER,
                username TEXT,
                userProfileImage TEXT, 
                likesCount INTEGER,
                isLiked INTEGER,
                commentsCount INTEGER
            )
        """)

        db.execSQL("""
            CREATE TABLE $T_STORIES (
                storyId TEXT PRIMARY KEY,
                userId TEXT,
                username TEXT,
                userProfilePicture TEXT, 
                storyImage TEXT, 
                timestamp INTEGER
            )
        """)
        
        db.execSQL("""
            CREATE TABLE $T_CONVERSATIONS (
                chatId TEXT PRIMARY KEY,
                otherUserId TEXT,
                otherUserName TEXT,
                otherUserImageUrl TEXT, 
                lastMessage TEXT,
                lastMessageTimestamp INTEGER
            )
        """)

        db.execSQL("""
            CREATE TABLE $T_QUEUED_ACTIONS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                type TEXT NOT NULL,
                url TEXT NOT NULL,
                payload TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $T_MESSAGES")
        db.execSQL("DROP TABLE IF EXISTS $T_POSTS")
        db.execSQL("DROP TABLE IF EXISTS $T_STORIES")
        db.execSQL("DROP TABLE IF EXISTS $T_CONVERSATIONS")
        db.execSQL("DROP TABLE IF EXISTS $T_QUEUED_ACTIONS")
        onCreate(db)
    }

    // Action Queue Methods
    fun queueAction(type: String, url: String, payload: JSONObject) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("type", type)
            put("url", url)
            put("payload", payload.toString())
            put("created_at", System.currentTimeMillis())
        }
        db.insert(T_QUEUED_ACTIONS, null, cv)
    }

    fun getQueuedActions(): List<QueuedAction> {
        val db = readableDatabase
        val cursor = db.query(T_QUEUED_ACTIONS, null, null, null, null, null, "created_at ASC")
        val actions = mutableListOf<QueuedAction>()
        while (cursor.moveToNext()) {
            actions.add(QueuedAction(
                id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                type = cursor.getString(cursor.getColumnIndexOrThrow("type")),
                url = cursor.getString(cursor.getColumnIndexOrThrow("url")),
                payload = cursor.getString(cursor.getColumnIndexOrThrow("payload"))
            ))
        }
        cursor.close()
        return actions
    }

    fun deleteQueuedAction(id: Long) {
        val db = writableDatabase
        db.delete(T_QUEUED_ACTIONS, "id = ?", arrayOf(id.toString()))
    }

    // Conversation Methods
    fun upsertConversations(conversations: List<Pair<Chat, User>>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            conversations.forEach { (chat, user) ->
                val cv = ContentValues().apply {
                    put("chatId", chat.chatId)
                    put("otherUserId", user.uid)
                    put("otherUserName", user.username)
                    put("otherUserImageUrl", user.imageBase64)
                    put("lastMessage", chat.lastMessage)
                    put("lastMessageTimestamp", chat.lastMessageTime)
                }
                db.insertWithOnConflict(T_CONVERSATIONS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getConversationsWithUsers(): List<Pair<Chat, User>> {
        val db = readableDatabase
        val cursor = db.query(T_CONVERSATIONS, null, null, null, null, null, "lastMessageTimestamp DESC")
        val results = mutableListOf<Pair<Chat, User>>()
        while (cursor.moveToNext()) {
            val otherUserId = cursor.getString(cursor.getColumnIndexOrThrow("otherUserId")) ?: ""
            val participants = mapOf(otherUserId to true, SessionManager(context).getUserId().toString() to true)
            val chat = Chat(
                chatId = cursor.getString(cursor.getColumnIndexOrThrow("chatId")) ?: "",
                participants = participants,
                lastMessage = cursor.getString(cursor.getColumnIndexOrThrow("lastMessage")) ?: "",
                lastMessageTime = cursor.getLong(cursor.getColumnIndexOrThrow("lastMessageTimestamp"))
            )
            val user = User(
                uid = otherUserId,
                username = cursor.getString(cursor.getColumnIndexOrThrow("otherUserName")) ?: "",
                imageBase64 = cursor.getString(cursor.getColumnIndexOrThrow("otherUserImageUrl")) ?: ""
            )
            results.add(Pair(chat, user))
        }
        cursor.close()
        return results
    }

    // Message Methods
    fun upsertMessages(messages: List<Message>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            messages.forEach { m ->
                val cv = ContentValues().apply {
                    put("messageId", m.messageId)
                    put("senderId", m.senderId)
                    put("receiverId", m.receiverId)
                    put("text", m.text)
                    put("timestamp", m.timestamp)
                    put("chatId", m.chatId)
                    put("attachmentUrl", m.attachmentUrl)
                    put("type", m.type)
                    put("isSeen", if (m.isSeen) 1 else 0)
                }
                db.insertWithOnConflict(T_MESSAGES, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getMessagesForConversation(convId: String): List<Message> {
        val db = readableDatabase
        val cursor = db.query(T_MESSAGES, null, "chatId = ?", arrayOf(convId), null, null, "timestamp ASC")
        val messages = mutableListOf<Message>()
        while (cursor.moveToNext()) {
            messages.add(Message(
                messageId = cursor.getString(cursor.getColumnIndexOrThrow("messageId")),
                senderId = cursor.getString(cursor.getColumnIndexOrThrow("senderId")),
                receiverId = cursor.getString(cursor.getColumnIndexOrThrow("receiverId")),
                text = cursor.getString(cursor.getColumnIndexOrThrow("text")),
                timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")),
                chatId = cursor.getString(cursor.getColumnIndexOrThrow("chatId")),
                attachmentUrl = cursor.getString(cursor.getColumnIndexOrThrow("attachmentUrl")),
                type = cursor.getString(cursor.getColumnIndexOrThrow("type")),
                isSeen = cursor.getInt(cursor.getColumnIndexOrThrow("isSeen")) == 1
            ))
        }
        cursor.close()
        return messages
    }

    // Post Methods
    fun upsertPosts(posts: List<Post>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            posts.forEach { p ->
                val cv = ContentValues().apply {
                    put("postId", p.postId)
                    put("userId", p.userId)
                    put("postImage", p.postImage)
                    put("caption", p.caption)
                    put("timestamp", p.timestamp)
                    put("username", p.username)
                    put("userProfileImage", p.userProfileImage)
                    put("likesCount", p.likesCount)
                    put("isLiked", if (p.isLiked) 1 else 0)
                    put("commentsCount", p.commentsCount)
                }
                db.insertWithOnConflict(T_POSTS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getPosts(forUserId: String? = null): List<Post> {
        val db = readableDatabase
        val selection = if (forUserId != null) "userId = ?" else null
        val selectionArgs = if (forUserId != null) arrayOf(forUserId) else null
        val cursor = db.query(T_POSTS, null, selection, selectionArgs, null, null, "timestamp DESC")
        val posts = mutableListOf<Post>()
        while (cursor.moveToNext()) {
            posts.add(Post(
                postId = cursor.getString(cursor.getColumnIndexOrThrow("postId")),
                userId = cursor.getString(cursor.getColumnIndexOrThrow("userId")),
                postImage = cursor.getString(cursor.getColumnIndexOrThrow("postImage")),
                caption = cursor.getString(cursor.getColumnIndexOrThrow("caption")),
                timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")),
                username = cursor.getString(cursor.getColumnIndexOrThrow("username")),
                userProfileImage = cursor.getString(cursor.getColumnIndexOrThrow("userProfileImage")),
                likesCount = cursor.getInt(cursor.getColumnIndexOrThrow("likesCount")),
                isLiked = cursor.getInt(cursor.getColumnIndexOrThrow("isLiked")) == 1,
                commentsCount = cursor.getInt(cursor.getColumnIndexOrThrow("commentsCount"))
            ))
        }
        cursor.close()
        return posts
    }

    fun deleteAllPosts(forUserId: String? = null) {
        val db = writableDatabase
        val selection = if (forUserId != null) "userId = ?" else null
        val selectionArgs = if (forUserId != null) arrayOf(forUserId) else null
        db.delete(T_POSTS, selection, selectionArgs)
    }

    // Story Methods
    fun upsertStories(stories: List<Story>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            stories.forEach { s ->
                val cv = ContentValues().apply {
                    put("storyId", s.storyId)
                    put("userId", s.userId)
                    put("username", s.username)
                    put("userProfilePicture", s.userProfilePicture)
                    put("storyImage", s.storyImage)
                    put("timestamp", s.timestamp)
                }
                db.insertWithOnConflict(T_STORIES, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getStories(forUserId: String? = null): List<Story> {
        val db = readableDatabase
        val selection = if (forUserId != null) "userId = ?" else null
        val selectionArgs = if (forUserId != null) arrayOf(forUserId) else null
        val cursor = db.query(T_STORIES, null, selection, selectionArgs, null, null, "timestamp DESC")
        val stories = mutableListOf<Story>()
        while (cursor.moveToNext()) {
            stories.add(Story(
                storyId = cursor.getString(cursor.getColumnIndexOrThrow("storyId")),
                userId = cursor.getString(cursor.getColumnIndexOrThrow("userId")),
                username = cursor.getString(cursor.getColumnIndexOrThrow("username")),
                userProfilePicture = cursor.getString(cursor.getColumnIndexOrThrow("userProfilePicture")),
                storyImage = cursor.getString(cursor.getColumnIndexOrThrow("storyImage")),
                timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"))
            ))
        }
        cursor.close()
        return stories
    }

    fun deleteAllStories(forUserId: String? = null) {
        val db = writableDatabase
        val selection = if (forUserId != null) "userId = ?" else null
        val selectionArgs = if (forUserId != null) arrayOf(forUserId) else null
        db.delete(T_STORIES, selection, selectionArgs)
    }
}
