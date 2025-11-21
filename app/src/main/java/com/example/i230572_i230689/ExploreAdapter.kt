package com.example.i230572_i230689

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class ExploreAdapter(
    private val context: Context,
    private val posts: List<Post>,
    private val onItemClick: (Post) -> Unit
) : RecyclerView.Adapter<ExploreAdapter.VH>() {

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.explore_image)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(context).inflate(R.layout.explore_item, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = posts[position]
        if (p.postImage.isNotEmpty()) {
            try {
                val bytes = Base64.decode(p.postImage, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                holder.image.setImageBitmap(bmp)
            } catch (e: Exception) {
                holder.image.setImageResource(R.drawable.socially_logo)
            }
        } else {
            holder.image.setImageResource(R.drawable.socially_logo)
        }

        holder.itemView.setOnClickListener { onItemClick(p) }
    }

    override fun getItemCount(): Int = posts.size
}
