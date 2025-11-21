package com.example.i230572_i230689

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class ChatDbHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    companion object {
        private const val DB_NAME = "chat_cache.db"
        private const val DB_VERSION = 1
        private const val T_MESSAGES = "messages"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val sql = """
            CREATE TABLE $T_MESSAGES (
              id TEXT PRIMARY KEY,
              conversation_id TEXT,
              sender_id TEXT,
              receiver_id TEXT,
              content TEXT,
              type TEXT,
              attachment_url TEXT,
              is_seen INTEGER DEFAULT 0,
              is_deleted INTEGER DEFAULT 0,
              is_edited INTEGER DEFAULT 0,
              vanish_on_close INTEGER DEFAULT 0,
              created_at TEXT
            )
        """.trimIndent()
        db.execSQL(sql)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $T_MESSAGES")
        onCreate(db)
    }

    fun upsertMessage(m: Message) {
        val db = writableDatabase
        val cv = ContentValues()
        cv.put("id", m.messageId)
        cv.put("conversation_id", m.chatId)
        cv.put("sender_id", m.senderId)
        cv.put("receiver_id", m.receiverId)
        cv.put("content", m.text)
        cv.put("type", m.type)
        cv.put("attachment_url", m.attachmentUrl)
        cv.put("is_seen", if (m.isSeen) 1 else 0)
        cv.put("is_deleted", if (m.isDeleted) 1 else 0)
        cv.put("is_edited", if (m.isEdited) 1 else 0)
        cv.put("vanish_on_close", if (m.vanishOnClose) 1 else 0)
        cv.put("created_at", m.timestamp.toString())
        db.insertWithOnConflict(T_MESSAGES, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun deleteMessage(messageId: String) {
        val db = writableDatabase
        db.delete(T_MESSAGES, "id = ?", arrayOf(messageId))
    }

    fun markMessageEdited(messageId: String, newContent: String) {
        val db = writableDatabase
        val cv = ContentValues()
        cv.put("content", newContent)
        cv.put("is_edited", 1)
        db.update(T_MESSAGES, cv, "id = ?", arrayOf(messageId))
    }

    fun getMessagesForConversation(convId: String): List<Message> {
        val db = readableDatabase
        val cursor = db.query(T_MESSAGES, null, "conversation_id = ?", arrayOf(convId), null, null, "created_at ASC")
        val out = mutableListOf<Message>()
        while (cursor.moveToNext()) {
            val m = Message(
                messageId = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                senderId = cursor.getString(cursor.getColumnIndexOrThrow("sender_id")),
                receiverId = cursor.getString(cursor.getColumnIndexOrThrow("receiver_id")),
                text = cursor.getString(cursor.getColumnIndexOrThrow("content")) ?: "",
                imageBase64 = "",
                postId = "",
                timestamp = cursor.getString(cursor.getColumnIndexOrThrow("created_at")).toLongOrNull() ?: 0L,
                chatId = cursor.getString(cursor.getColumnIndexOrThrow("conversation_id")),
                attachmentUrl = cursor.getString(cursor.getColumnIndexOrThrow("attachment_url")) ?: "",
                type = cursor.getString(cursor.getColumnIndexOrThrow("type")) ?: "text",
                isSeen = cursor.getInt(cursor.getColumnIndexOrThrow("is_seen")) == 1,
                isDeleted = cursor.getInt(cursor.getColumnIndexOrThrow("is_deleted")) == 1,
                vanishOnClose = cursor.getInt(cursor.getColumnIndexOrThrow("vanish_on_close")) == 1
            )
            out.add(m)
        }
        cursor.close()
        return out
    }
}
