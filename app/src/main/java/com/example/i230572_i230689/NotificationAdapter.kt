package com.example.i230572_i230689

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import java.io.File

data class Notification(
    val type: String,
    val userId: String,
    val username: String,
    val userImage: String,
    val postId: String?,
    val postImage: String?,
    val commentText: String?,
    val timestamp: Long
)

class NotificationAdapter(
    private val context: Context,
    private val notifications: List<Notification>
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userPfp: ImageView = itemView.findViewById(R.id.p_1)
        val notificationText: TextView = itemView.findViewById(R.id.text8)
        val postImage: ImageView = itemView.findViewById(R.id.post_image_preview)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.notification_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = notifications.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notification = notifications[position]

        var text = ""
        when (notification.type) {
            "new_follower" -> {
                text = "<b>${notification.username}</b> started following you."
                holder.postImage.visibility = View.GONE
            }
            "like" -> {
                text = "<b>${notification.username}</b> liked your post."
                holder.postImage.visibility = View.VISIBLE
                Picasso.get().load(File(notification.postImage)).into(holder.postImage)
            }
            "comment" -> {
                text = "<b>${notification.username}</b> commented: ${notification.commentText}"
                holder.postImage.visibility = View.VISIBLE
                Picasso.get().load(File(notification.postImage)).into(holder.postImage)
            }
        }
        holder.notificationText.text = android.text.Html.fromHtml(text, android.text.Html.FROM_HTML_MODE_LEGACY)

        if (notification.userImage.isNotEmpty()) {
            Picasso.get().load(File(notification.userImage)).into(holder.userPfp)
        }
    }
}
