package com.example.i230572_i230689

import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Base64
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
import de.hdodenhof.circleimageview.CircleImageView
import org.json.JSONObject

class UserListAdapter(private val users: MutableList<User>, private val listType: String = "followers", private val currentUserId: Int = 0) :
    RecyclerView.Adapter<UserListAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: CircleImageView = itemView.findViewById(R.id.profile_image)
        val username: TextView = itemView.findViewById(R.id.username_text)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users[position]
        holder.username.text = user.username

        if (!user.imageBase64.isNullOrEmpty()) {
            val bytes = Base64.decode(user.imageBase64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            holder.profileImage.setImageBitmap(bitmap)
        } else {
            holder.profileImage.setImageResource(R.drawable.profile_image)
        }



        holder.username.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, TwentyActivity::class.java)
            intent.putExtra("userId", user.uid.toIntOrNull() ?: 0)
            context.startActivity(intent)
        }

        holder.profileImage.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, TwentyActivity::class.java)
            intent.putExtra("userId", user.uid.toIntOrNull() ?: 0)
            context.startActivity(intent)
        }
    }

    private fun unfollowUser(user: User, holder: ViewHolder, position: Int) {
        val context = holder.itemView.context
        val sessionManager = SessionManager(context)
        val token = sessionManager.getToken() ?: return

        val url = BuildConfig.BASE_URL + "unfollow_user.php"
        val rq = Volley.newRequestQueue(context)

        val req = object : StringRequest(Method.POST, url,
            { response ->
                try {
                    val obj = JSONObject(response.trim())
                    if (obj.optBoolean("success", false)) {
                        Toast.makeText(context, "Unfollowed", Toast.LENGTH_SHORT).show()
                        // remove from list and notify
                        users.removeAt(position)
                        notifyItemRemoved(position)
                        notifyItemRangeChanged(position, users.size)
                        // broadcast so open profile pages can refresh their counts
                        try {
                            val intent = android.content.Intent("user_unfollowed")
                            intent.putExtra("userId", user.uid)
                            context.sendBroadcast(intent)
                        } catch (_: Exception) {}
                    } else {
                        Toast.makeText(context, "Unfollow failed", Toast.LENGTH_SHORT).show()
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
                val params = HashMap<String, String>()
                params["to_user_id"] = user.uid
                return params
            }

            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer $token"
                return headers
            }
        }

        req.retryPolicy = DefaultRetryPolicy(15000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        rq.add(req)
    }

    override fun getItemCount() = users.size
}
