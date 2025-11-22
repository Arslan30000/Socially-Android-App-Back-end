package com.example.i230572_i230689

import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.VideoView
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
        val video: VideoView? = itemView.findViewById(R.id.video_message)
        val playIcon: ImageView? = itemView.findViewById(R.id.play_icon)
    }

    inner class ReceivedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: CircleImageView = itemView.findViewById(R.id.profile_pic)
        val text: TextView = itemView.findViewById(R.id.message_bubble)
        val image: ImageView? = itemView.findViewById(R.id.image_message)
        val video: VideoView? = itemView.findViewById(R.id.video_message)
        val playIcon: ImageView? = itemView.findViewById(R.id.play_icon)
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
            bindMessage(holder.text, holder.image, holder.video, holder.playIcon, message)
            holder.itemView.setOnLongClickListener {
                if (message.senderId == currentUserId) {
                    onMessageLongClick?.invoke(message)
                    true
                } else false
            }
        } else if (holder is ReceivedViewHolder) {
            bindMessage(holder.text, holder.image, holder.video, holder.playIcon, message)
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

    private fun bindMessage(textView: TextView, imageView: ImageView?, videoView: VideoView?, playIcon: ImageView?, message: Message) {
        when (message.type) {
            "image" -> {
                textView.visibility = View.GONE
                videoView?.visibility = View.GONE
                playIcon?.visibility = View.GONE
                imageView?.visibility = View.VISIBLE
                loadImageFromUrl(imageView, message.attachmentUrl)
            }
            "video" -> {
                textView.visibility = View.GONE
                imageView?.visibility = View.GONE
                videoView?.visibility = View.VISIBLE
                playIcon?.visibility = View.VISIBLE
                videoView?.setVideoURI(Uri.parse(message.attachmentUrl))
                videoView?.setOnPreparedListener { mp ->
                    mp.isLooping = true
                    videoView.start() // Autoplay, or you can wait for a click
                }
                playIcon?.setOnClickListener {
                    if (videoView?.isPlaying == true) {
                        videoView.pause()
                        playIcon.setImageResource(R.drawable.ic_play_arrow)
                    } else {
                        videoView?.start()
                        playIcon.setImageResource(R.drawable.ic_pause)
                    }
                }
            }
            else -> {
                imageView?.visibility = View.GONE
                videoView?.visibility = View.GONE
                playIcon?.visibility = View.GONE
                textView.visibility = View.VISIBLE
                textView.text = message.text
            }
        }
    }

    private fun loadImageFromUrl(iv: ImageView?, url: String?) {
        if (iv == null || url.isNullOrEmpty()) return
        Thread {
            try {
                val u = java.net.URL(url)
                val conn = u.openConnection() as java.net.URLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                val `is` = conn.getInputStream()
                val bmp = BitmapFactory.decodeStream(`is`)
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
