package com.example.i230572_i230689

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.hdodenhof.circleimageview.CircleImageView

class MessageAdapter(
    private val messages: List<Message>,
    private val currentUserId: String,
    private val usersMap: Map<String, User>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_SENT = 1
        private const val TYPE_RECEIVED = 2
    }

    inner class SentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val text: TextView = itemView.findViewById(R.id.message_bubble)
        val image: ImageView? = itemView.findViewById(R.id.image_message)
    }

    inner class ReceivedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: CircleImageView = itemView.findViewById(R.id.profile_pic)
        val text: TextView = itemView.findViewById(R.id.message_bubble)
        val image: ImageView? = itemView.findViewById(R.id.image_message)
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderId == currentUserId) TYPE_SENT else TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_SENT) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.chat_bubble_sent, parent, false)
            SentViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.chat_bubble_recieved, parent, false)
            ReceivedViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]

        if (holder is SentViewHolder) {
            bindMessage(holder.text, holder.image, message)
        } else if (holder is ReceivedViewHolder) {
            bindMessage(holder.text, holder.image, message)
            val user = usersMap[message.senderId]
            if (user != null && user.imageBase64.isNotEmpty()) {
                val bytes = Base64.decode(user.imageBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                holder.profileImage.setImageBitmap(bitmap)
            }
        }
    }
    private fun bindMessage(textView: TextView, imageView: ImageView?, message: Message) {
        if (message.text.isNotEmpty()) {
            textView.visibility = View.VISIBLE
            textView.text = message.text
        } else {
            textView.visibility = View.GONE
        }
        if (message.imageBase64.isNotEmpty()) {
            imageView?.visibility = View.VISIBLE
            val bytes = Base64.decode(message.imageBase64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            imageView?.setImageBitmap(bitmap)

            imageView?.adjustViewBounds = true
            imageView?.scaleType = ImageView.ScaleType.CENTER_CROP
        } else {
            imageView?.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = messages.size
}
