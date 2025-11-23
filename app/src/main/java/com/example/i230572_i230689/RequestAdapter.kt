package com.example.i230572_i230689

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import android.widget.RelativeLayout
import java.io.File

data class FollowRequest(
    val uid: String = "",
    val username: String = "",
    val imageBase64: String = "",
    val timestamp: Long = 0
)

class RequestAdapter(
    private val requests: List<FollowRequest>,
    private val onAcceptClick: (FollowRequest) -> Unit,
    private val onRejectClick: (FollowRequest) -> Unit
) : RecyclerView.Adapter<RequestAdapter.RequestViewHolder>() {

    inner class RequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: CircleImageView = itemView.findViewById(R.id.profileImage)
        val usernameText: TextView = itemView.findViewById(R.id.usernameText)
        val timeText: TextView = itemView.findViewById(R.id.timeText)
        val acceptBtn: RelativeLayout = itemView.findViewById(R.id.acceptBtn)
        val rejectBtn: RelativeLayout = itemView.findViewById(R.id.rejectBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.request_item, parent, false)
        return RequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val request = requests[position]
        holder.usernameText.text = request.username

        val elapsedHours = (System.currentTimeMillis() - request.timestamp) / (1000 * 60 * 60)
        holder.timeText.text = if (elapsedHours < 24) "${elapsedHours}h ago" else "${elapsedHours / 24}d ago"

        if (request.imageBase64.isNotEmpty()) {
            Picasso.get().load(File(request.imageBase64)).placeholder(R.drawable.profile_image).into(holder.profileImage)
        } else {
            holder.profileImage.setImageResource(R.drawable.profile_image)
        }

        holder.acceptBtn.setOnClickListener { onAcceptClick(request) }
        holder.rejectBtn.setOnClickListener { onRejectClick(request) }
    }

    override fun getItemCount(): Int = requests.size
}
