package com.example.i230572_i230689

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.hdodenhof.circleimageview.CircleImageView

class StoryAdapter(
    private val context: Context,
    private val stories: List<Story>,
    private val onAddStoryClick: () -> Unit
) : RecyclerView.Adapter<StoryAdapter.StoryViewHolder>() {

    class StoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userImage: CircleImageView = itemView.findViewById(R.id.story_image)
        val userName: TextView = itemView.findViewById(R.id.story_username)
        val addStoryPlusIcon: ImageView = itemView.findViewById(R.id.story_add)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.story_item, parent, false)
        return StoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: StoryViewHolder, position: Int) {
        val story = stories[position]
        holder.userName.text = story.username

        if (story.isAddButton) {
            // --- THIS IS THE "YOUR STORY" CIRCLE ---
            holder.addStoryPlusIcon.visibility = View.VISIBLE
            // The plus icon ALWAYS opens the gallery to add a new story.
            holder.addStoryPlusIcon.setOnClickListener { onAddStoryClick() }

            // Check if a userId is present. If so, it means the user has stories.
            if (story.userId.isNotEmpty()) {
                // The main image click should open the user's stories for viewing.
                holder.userImage.setOnClickListener {
                    val intent = Intent(context, NineteenActivity::class.java).apply {
                        putExtra("USER_ID", story.userId)
                    }
                    context.startActivity(intent)
                }
            } else {
                // No userId present, so the user has no stories.
                // The main image click can either do nothing or also open the gallery.
                holder.userImage.setOnClickListener { onAddStoryClick() }
            }

        } else {
            // --- THIS IS A STORY FROM A FOLLOWED USER ---
            holder.addStoryPlusIcon.visibility = View.GONE
            // The entire item click opens their stories for viewing.
            holder.itemView.setOnClickListener {
                val intent = Intent(context, NineteenActivity::class.java).apply {
                    putExtra("USER_ID", story.userId)
                }
                context.startActivity(intent)
            }
        }

        // Image decoding logic (remains the same)
        if (!story.userProfilePicture.isNullOrEmpty()) {
            try {
                val imageBytes = Base64.decode(story.userProfilePicture, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                holder.userImage.setImageBitmap(bitmap)
            } catch (e: Exception) {
                // If decoding fails, show a placeholder
                holder.userImage.setImageResource(R.drawable.profile_image)
            }
        } else {
            // If the string is null or empty, show a placeholder
            holder.userImage.setImageResource(R.drawable.profile_image)
        }
    }

    override fun getItemCount(): Int = stories.size
}
