package com.example.i230572_i230689

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.hdodenhof.circleimageview.CircleImageView

class OnlineUserAdapter(
    private var users: MutableList<User>,
    private val onClick: ((User) -> Unit)? = null
) : RecyclerView.Adapter<OnlineUserAdapter.Holder>() {

    inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val pic: CircleImageView = itemView.findViewById(R.id.online_user_pic)
        val name: TextView = itemView.findViewById(R.id.online_user_name)
        val dot: ImageView = itemView.findViewById(R.id.online_dot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_online_user, parent, false)
        return Holder(v)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val u = users[position]
        holder.name.text = u.username.ifEmpty { u.name }
        
        if (u.imageBase64.isNotEmpty()) {
            try {
                val bytes = Base64.decode(u.imageBase64, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                holder.pic.setImageBitmap(bmp)
            } catch (e: Exception) { e.printStackTrace() }
        }
        
        // Show green dot if online, hide if offline
        holder.dot.visibility = if (u.onlineStatus == "online") View.VISIBLE else View.GONE
        holder.itemView.setOnClickListener { onClick?.invoke(u) }
    }

    override fun getItemCount(): Int = users.size

    fun updateData(list: List<User>) {
        users.clear()
        users.addAll(list)
        notifyDataSetChanged()
    }
}
