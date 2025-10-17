package com.example.i230572_i230689

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.*

class ChatsAdapter(
    private var chats: List<Chat>,
    private val currentUserId: String,
    private val onChatClick: (Chat, User) -> Unit,
    private val onChatLongClick: (Chat) -> Unit
) : RecyclerView.Adapter<ChatsAdapter.ChatViewHolder>() {

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: CircleImageView = itemView.findViewById(R.id.profile_image)
        val username: TextView = itemView.findViewById(R.id.username)
        val lastMessage: TextView = itemView.findViewById(R.id.last_message)
        val time: TextView = itemView.findViewById(R.id.time)
    }

    private val usersMap = mutableMapOf<String, User>()
    private val messagesMap = mutableMapOf<String, Message>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.chat_item, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chat = chats[position]
        val otherUserId = chat.participants.keys.first { it != currentUserId }
        val user = usersMap[otherUserId]

        if (user != null) {
            holder.username.text = "${user.name} ${user.lastname}"
            if (user.imageBase64.isNotEmpty()) {
                val bytes = Base64.decode(user.imageBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                holder.profileImage.setImageBitmap(bitmap)
            }
        }

        val lastMsg = messagesMap[chat.lastMessage]
        if (lastMsg != null) {
            holder.lastMessage.text = when {
                lastMsg.text.isNotEmpty() -> lastMsg.text
                lastMsg.imageBase64.isNotEmpty() -> " Image"
                lastMsg.postId.isNotEmpty() -> " Post"
                else -> ""
            }

            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            holder.time.text = sdf.format(Date(lastMsg.timestamp))
        }

        holder.itemView.setOnClickListener {
            if (user != null) onChatClick(chat, user)
        }
        holder.itemView.setOnLongClickListener {
            onChatLongClick(chat)
            true
        }
    }

    override fun getItemCount(): Int = chats.size

    fun updateData(
        newChats: List<Chat>,
        newUsers: Map<String, User>,
        newMessages: Map<String, Message>
    ) {
        chats = newChats
        usersMap.clear()
        usersMap.putAll(newUsers)
        messagesMap.clear()
        messagesMap.putAll(newMessages)
        notifyDataSetChanged()
    }
}
