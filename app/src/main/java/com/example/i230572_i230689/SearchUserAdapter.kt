package com.example.i230572_i230689

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import org.json.JSONObject
import java.io.File

class SearchUserAdapter(
    private val context: Context,
    private val users: List<User>,
    private val onUserClick: (User) -> Unit
) : RecyclerView.Adapter<SearchUserAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: CircleImageView = itemView.findViewById(R.id.profile_image)
        val username: TextView = itemView.findViewById(R.id.username_text)
        val actionButton: Button = itemView.findViewById(R.id.action_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_user_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users[position]
        holder.username.text = user.username

        if (user.imageBase64.isNotEmpty()) {
            Picasso.get().load(File(user.imageBase64)).placeholder(R.drawable.profile_image).into(holder.profileImage)
        } else {
            holder.profileImage.setImageResource(R.drawable.profile_image)
        }

        holder.actionButton.visibility = View.VISIBLE

        when {
            user.followers["is_following"] == true -> {
                holder.actionButton.text = "Following"
                holder.actionButton.isEnabled = false
                holder.actionButton.setBackgroundColor(Color.LTGRAY)
            }
            user.followRequests["already_requested"] == true -> {
                holder.actionButton.text = "Requested"
                holder.actionButton.isEnabled = false
                holder.actionButton.setBackgroundColor(Color.LTGRAY)
            }
            else -> {
                holder.actionButton.text = "Follow"
                holder.actionButton.isEnabled = true
                holder.actionButton.setBackgroundColor(context.getColor(R.color.brown))
                holder.actionButton.setOnClickListener {
                    sendFollowRequest(user, holder)
                }
            }
        }

        // Corrected: Call onUserClick when the item view is clicked
        holder.itemView.setOnClickListener { onUserClick(user) }
    }

    private fun sendFollowRequest(user: User, holder: ViewHolder) {
        val sessionManager = SessionManager(context)
        val token = sessionManager.getToken() ?: return
        val url = BuildConfig.BASE_URL + "send_follow_request.php"
        val rq = Volley.newRequestQueue(context)

        val req = object : StringRequest(Method.POST, url,
            { response ->
                try {
                    val obj = JSONObject(response.trim())
                    if (obj.optBoolean("success", false)) {
                        Toast.makeText(context, "Follow request sent", Toast.LENGTH_SHORT).show()
                        holder.actionButton.text = "Requested"
                        holder.actionButton.isEnabled = false
                        holder.actionButton.setBackgroundColor(Color.LTGRAY)
                    } else {
                        Toast.makeText(context, "Failed to send request", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            { error ->
                error.printStackTrace()
                Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show()
            }) {
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf("to_user_id" to user.uid)
            }
            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf("Authorization" to "Bearer $token")
            }
        }
        req.retryPolicy = DefaultRetryPolicy(15000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        rq.add(req)
    }

    override fun getItemCount() = users.size
}
