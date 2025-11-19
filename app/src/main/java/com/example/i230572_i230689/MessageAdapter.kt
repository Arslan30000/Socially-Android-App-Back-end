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
    private val usersMap: Map<String, User>,
    private val onMessageLongClick: ((Message) -> Unit)? = null
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
        val msg = messages[position]
        return if (msg.senderId == currentUserId) TYPE_SENT else TYPE_RECEIVED
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
            holder.itemView.setOnLongClickListener {
                if (message.senderId == currentUserId) {
                    onMessageLongClick?.invoke(message)
                    true
                } else false
            }
        } else if (holder is ReceivedViewHolder) {
            bindMessage(holder.text, holder.image, message)
            val user = usersMap[message.senderId]
            if (user != null && user.imageBase64.isNotEmpty()) {
                val bytes = Base64.decode(user.imageBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                holder.profileImage.setImageBitmap(bitmap)
            }
            holder.itemView.setOnLongClickListener {
                false
            }
        }
    }

    private fun bindMessage(textView: TextView, imageView: ImageView?, message: Message) {
        val isImageMessage = message.type == "image" && !message.attachmentUrl.isNullOrEmpty()

        if (isImageMessage) {
            // This is an image message
            textView.visibility = View.GONE
            imageView?.visibility = View.VISIBLE
            loadImageFromUrl(imageView, message.attachmentUrl)
        } else {
            // This is a text message
            imageView?.visibility = View.GONE
            textView.visibility = View.VISIBLE
            textView.text = message.text
        }
    }

    private fun loadImageFromUrl(iv: ImageView?, url: String) {
        if (iv == null || url.isEmpty()) return
        // load on background thread, set on UI thread
        Thread {
            try {
                val u = java.net.URL(url)
                val conn = u.openConnection() as java.net.URLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                val `is` = conn.getInputStream()
                val bmp = android.graphics.BitmapFactory.decodeStream(`is`)
                `is`.close()
                if (bmp != null) {
                    iv.post { iv.setImageBitmap(bmp) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    override fun getItemCount(): Int = messages.size
}
