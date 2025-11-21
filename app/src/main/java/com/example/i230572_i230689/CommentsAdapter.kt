package com.example.i230572_i230689

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.hdodenhof.circleimageview.CircleImageView

class CommentsAdapter(
    private val context: Context,
    private val comments: List<Comment>
) : RecyclerView.Adapter<CommentsAdapter.VH>() {

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val pfp: CircleImageView = itemView.findViewById(R.id.comment_user_pfp)
        val username: TextView = itemView.findViewById(R.id.comment_username)
        val text: TextView = itemView.findViewById(R.id.comment_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(context).inflate(R.layout.comment_item, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val c = comments[position]
        holder.username.text = c.username
        holder.text.text = c.text

        if (!c.userProfileImage.isNullOrEmpty()) {
            try {
                val bytes = Base64.decode(c.userProfileImage, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                holder.pfp.setImageBitmap(bmp)
            } catch (e: Exception) {
                holder.pfp.setImageResource(R.drawable.profile_image)
            }
        } else {
            holder.pfp.setImageResource(R.drawable.profile_image)
        }
    }

    override fun getItemCount(): Int = comments.size
}
