package com.example.i230572_i230689

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import java.io.File

class ProfilePostAdapter(
    private val context: Context,
    private val posts: List<Post>
) : RecyclerView.Adapter<ProfilePostAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val postImage: ImageView = itemView.findViewById(R.id.explore_image)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.explore_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = posts.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val post = posts[position]
        if (post.postImage.isNotEmpty()) {
            Picasso.get()
                .load(File(post.postImage))
                .placeholder(R.drawable.socially_logo)
                .error(R.drawable.socially_logo)
                .into(holder.postImage)
        } else {
            holder.postImage.setImageResource(R.drawable.socially_logo)
        }
    }
}
