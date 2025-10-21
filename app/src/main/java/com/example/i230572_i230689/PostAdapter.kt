package com.example.i230572_i230689

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.hdodenhof.circleimageview.CircleImageView

class PostAdapter(
    private val context: Context,
    private val posts: List<Post>
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userPfp: CircleImageView = itemView.findViewById(R.id.post_user_pfp)
        val username: TextView = itemView.findViewById(R.id.post_username)
        val postImage: ImageView = itemView.findViewById(R.id.post_image)
        val likesCount: TextView = itemView.findViewById(R.id.post_likes_count)
        val caption: TextView = itemView.findViewById(R.id.post_caption)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.post_item, parent, false)
        return PostViewHolder(view)
    }

    override fun getItemCount(): Int = posts.size

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]

        // Set the text data
        holder.username.text = post.username
        holder.caption.text = post.caption
        holder.likesCount.text = "${post.likesCount} likes" // Example

        // Decode and set the user's profile picture
        setImageFromBase64(post.userProfileImage, holder.userPfp, R.drawable.profile_image)

        // Decode and set the main post image
        setImageFromBase64(post.postImage, holder.postImage, R.drawable.socially_logo)
    }

    private fun setImageFromBase64(
        base64String: String,
        imageView: ImageView,
        fallbackDrawable: Int
    ) {
        if (base64String.isNotEmpty()) {
            try {
                val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                imageView.setImageBitmap(bitmap)
            } catch (e: Exception) {
                imageView.setImageResource(fallbackDrawable)
            }
        } else {
            imageView.setImageResource(fallbackDrawable)
        }
    }
}