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
import com.google.firebase.database.FirebaseDatabase
import de.hdodenhof.circleimageview.CircleImageView

class PostAdapter(
    private val context: Context,
    private val posts: List<Post>
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userPfp: CircleImageView = itemView.findViewById(R.id.post_user_pfp)
        val username: TextView = itemView.findViewById(R.id.post_username)
        val postImage: ImageView = itemView.findViewById(R.id.post_image)
        val likeButton: ImageView = itemView.findViewById(R.id.post_like_button)
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

        holder.username.text = post.username
        holder.caption.text = post.caption
        holder.likesCount.text = "${post.likesCount} likes"

        setImageFromBase64(post.userProfileImage, holder.userPfp, R.drawable.profile_image)
        setImageFromBase64(post.postImage, holder.postImage, R.drawable.socially_logo)

        holder.likeButton.setOnClickListener {
            val newLikesCount = post.likesCount + 1
            val postRef = FirebaseDatabase.getInstance().getReference("posts").child(post.postId)
            postRef.child("likesCount").setValue(newLikesCount).addOnSuccessListener {
                holder.likesCount.text = "$newLikesCount likes"
                holder.likeButton.setImageResource(R.drawable.like_fillled)
                holder.likeButton.isEnabled = false // Optional: disable after liking
            }
        }
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
