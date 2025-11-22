package com.example.i230572_i230689

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import java.io.File
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
        val onlineIndicator: ImageView? = itemView.findViewById(R.id.online_status_indicator)
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
        val otherUserId = chat.participants.keys.firstOrNull { it != currentUserId } ?: return
        val user = usersMap[otherUserId]

        if (user != null) {
            holder.username.text = if (user.name.isNotEmpty() || user.lastname.isNotEmpty()) {
                "${user.name} ${user.lastname}".trim()
            } else {
                user.username
            }
            
            // Corrected: Load images from file paths using Picasso
            if (user.imageBase64.isNotEmpty()) {
                Picasso.get().load(File(user.imageBase64)).placeholder(R.drawable.profile_image).into(holder.profileImage)
            } else {
                holder.profileImage.setImageResource(R.drawable.profile_image)
            }
            
            holder.onlineIndicator?.visibility = if (user.onlineStatus == "online") View.VISIBLE else View.GONE
        }

        holder.lastMessage.text = chat.lastMessage
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        holder.time.text = sdf.format(Date(chat.lastMessageTime))

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
