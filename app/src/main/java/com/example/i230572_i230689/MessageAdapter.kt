package com.example.i230572_i230689

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import java.io.File

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
        val videoContainer: FrameLayout? = itemView.findViewById(R.id.video_container)
        val videoThumbnail: ImageView? = itemView.findViewById(R.id.video_thumbnail)
    }

    inner class ReceivedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: CircleImageView = itemView.findViewById(R.id.profile_pic)
        val text: TextView = itemView.findViewById(R.id.message_bubble)
        val image: ImageView? = itemView.findViewById(R.id.image_message)
        val videoContainer: FrameLayout? = itemView.findViewById(R.id.video_container)
        val videoThumbnail: ImageView? = itemView.findViewById(R.id.video_thumbnail)
    }

    override fun getItemViewType(position: Int): Int {
        val msg = messages[position]
        return if (msg.senderId == currentUserId) TYPE_SENT else TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_SENT) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.chat_bubble_sent, parent, false)
            SentViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.chat_bubble_recieved, parent, false)
            ReceivedViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]

        if (holder is SentViewHolder) {
            bindMessage(holder.itemView.context, holder.text, holder.image, holder.videoContainer, holder.videoThumbnail, message)
            holder.itemView.setOnLongClickListener {
                if (message.senderId == currentUserId) {
                    onMessageLongClick?.invoke(message)
                    true
                } else false
            }
        } else if (holder is ReceivedViewHolder) {
            bindMessage(holder.itemView.context, holder.text, holder.image, holder.videoContainer, holder.videoThumbnail, message)
            val user = usersMap[message.senderId]
            if (user != null && user.imageBase64.isNotEmpty()) {
                if (user.imageBase64.startsWith("file://")) {
                    Picasso.get().load(File(user.imageBase64.substring(7))).placeholder(R.drawable.profile_image).into(holder.profileImage)
                } else {
                    Picasso.get().load(user.imageBase64).placeholder(R.drawable.profile_image).into(holder.profileImage)
                }
            }
        }
    }

    private fun bindMessage(context: Context, textView: TextView, imageView: ImageView?, videoContainer: FrameLayout?, videoThumbnail: ImageView?, message: Message) {
        when (message.type) {
            "image" -> {
                textView.visibility = View.GONE
                videoContainer?.visibility = View.GONE
                imageView?.visibility = View.VISIBLE
                Picasso.get().load(message.attachmentUrl).into(imageView)
            }
            "video" -> {
                textView.visibility = View.GONE
                imageView?.visibility = View.GONE
                videoContainer?.visibility = View.VISIBLE
                videoThumbnail?.setImageResource(R.drawable.socially_logo)
                videoContainer?.setOnClickListener {
                    val intent = Intent(context, VideoPlayerActivity::class.java)
                    intent.putExtra("VIDEO_URL", message.attachmentUrl)
                    context.startActivity(intent)
                }
            }
            else -> {
                imageView?.visibility = View.GONE
                videoContainer?.visibility = View.GONE
                textView.visibility = View.VISIBLE
                textView.text = message.text
            }
        }
    }

    override fun getItemCount(): Int = messages.size
}
