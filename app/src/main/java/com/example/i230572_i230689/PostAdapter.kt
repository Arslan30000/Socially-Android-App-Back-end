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
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
// Firebase removed - we use REST endpoints for like/comment
import android.app.AlertDialog
import android.widget.EditText
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

        // Set initial like icon based on isLiked
        if (post.isLiked) {
            holder.likeButton.setImageResource(R.drawable.like_fillled)
            holder.likeButton.isEnabled = true
        } else {
            holder.likeButton.setImageResource(R.drawable.like)
            holder.likeButton.isEnabled = true
        }

        setImageFromBase64(post.userProfileImage, holder.userPfp, R.drawable.profile_image)
        setImageFromBase64(post.postImage, holder.postImage, R.drawable.socially_logo)

        holder.likeButton.setOnClickListener {
            val token = SessionManager(context).getToken() ?: return@setOnClickListener
            val urlLike = BuildConfig.BASE_URL + "like_post.php"
            val urlUnlike = BuildConfig.BASE_URL + "unlike_post.php"
            val rq = Volley.newRequestQueue(context)

            if (post.isLiked) {
                // Send unlike request
                val req = object: StringRequest(Method.POST, urlUnlike,
                    { response ->
                        try {
                            val obj = org.json.JSONObject(response.trim())
                            if (obj.optBoolean("success", false)) {
                                val newCnt = obj.optInt("likesCount", post.likesCount)
                                post.likesCount = newCnt
                                post.isLiked = false
                                holder.likesCount.text = "$newCnt likes"
                                holder.likeButton.setImageResource(R.drawable.like)
                            }
                        } catch (e: Exception) {
                            // ignore
                        }
                    },
                    { error -> /* ignore */ }) {
                    override fun getParams(): MutableMap<String, String> {
                        return hashMapOf("post_id" to post.postId)
                    }

                    override fun getHeaders(): MutableMap<String, String> {
                        val headers = HashMap<String, String>()
                        headers["Authorization"] = "Bearer $token"
                        return headers
                    }
                }
                rq.add(req)
            } else {
                // Send like request
                val req = object: StringRequest(Method.POST, urlLike,
                    { response ->
                        try {
                            val obj = org.json.JSONObject(response.trim())
                            if (obj.optBoolean("success", false)) {
                                val newCnt = obj.optInt("likesCount", post.likesCount + 1)
                                post.likesCount = newCnt
                                post.isLiked = true
                                holder.likesCount.text = "$newCnt likes"
                                holder.likeButton.setImageResource(R.drawable.like_fillled)
                            }
                        } catch (e: Exception) {
                            // ignore
                        }
                    },
                    { error -> /* ignore */ }) {
                    override fun getParams(): MutableMap<String, String> {
                        return hashMapOf("post_id" to post.postId)
                    }

                    override fun getHeaders(): MutableMap<String, String> {
                        val headers = HashMap<String, String>()
                        headers["Authorization"] = "Bearer $token"
                        return headers
                    }
                }
                rq.add(req)
            }
        }

        // Comment button: show a dialog to add a comment
        holder.itemView.findViewById<ImageView>(R.id.post_comment_button).setOnClickListener {
            val ctx = context
            val intent = android.content.Intent(ctx, CommentsActivity::class.java)
            intent.putExtra("POST_ID", post.postId)
            ctx.startActivity(intent)
        }
        //comment logic here
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
