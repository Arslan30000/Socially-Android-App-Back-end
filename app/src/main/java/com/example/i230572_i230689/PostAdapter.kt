package com.example.i230572_i230689

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import java.io.File

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
        val commentsCount: TextView = itemView.findViewById(R.id.post_comments_count)
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
        holder.commentsCount.text = "${post.commentsCount} comments"

        // Corrected: Load images from file paths using Picasso
        if (post.userProfileImage.isNotEmpty()) {
            Picasso.get().load(File(post.userProfileImage)).placeholder(R.drawable.profile_image).into(holder.userPfp)
        } else {
            holder.userPfp.setImageResource(R.drawable.profile_image)
        }

        if (post.postImage.isNotEmpty()) {
            Picasso.get().load(File(post.postImage)).placeholder(R.drawable.socially_logo).into(holder.postImage)
        } else {
            holder.postImage.setImageResource(R.drawable.socially_logo)
        }

        holder.likeButton.setImageResource(if (post.isLiked) R.drawable.like_fillled else R.drawable.like)

        holder.likeButton.setOnClickListener {
            toggleLike(post, holder)
        }

        holder.itemView.findViewById<ImageView>(R.id.post_comment_button).setOnClickListener {
            val intent = Intent(context, CommentsActivity::class.java)
            intent.putExtra("POST_ID", post.postId)
            context.startActivity(intent)
        }
    }

    private fun toggleLike(post: Post, holder: PostViewHolder) {
        val token = SessionManager(context).getToken() ?: return
        val url = if (post.isLiked) {
            BuildConfig.BASE_URL + "unlike_post.php"
        } else {
            BuildConfig.BASE_URL + "like_post.php"
        }
        val rq = Volley.newRequestQueue(context)

        val req = object : StringRequest(Method.POST, url,
            { response ->
                try {
                    val obj = org.json.JSONObject(response.trim())
                    if (obj.optBoolean("success", false)) {
                        post.isLiked = !post.isLiked
                        post.likesCount = obj.optInt("likesCount", post.likesCount)
                        holder.likesCount.text = "${post.likesCount} likes"
                        holder.likeButton.setImageResource(if (post.isLiked) R.drawable.like_fillled else R.drawable.like)
                    }
                } catch (e: Exception) {
                    // Ignore parse error
                }
            },
            { error -> /* Ignore network error for now */ }) {
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf("post_id" to post.postId)
            }

            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf("Authorization" to "Bearer $token")
            }
        }
        rq.add(req)
    }
}
