package com.example.i230572_i230689

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import java.io.File

class StoryAdapter(
    private val context: Context,
    private val stories: List<Story>,
    private val onStoryClick: (Story) -> Unit
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
            holder.addStoryPlusIcon.visibility = if (story.hasStories) View.GONE else View.VISIBLE
        } else {
            holder.addStoryPlusIcon.visibility = View.GONE
        }

        // Corrected: Load images from file paths using Picasso
        if (story.userProfilePicture?.isNotEmpty() == true) {
            Picasso.get().load(File(story.userProfilePicture)).placeholder(R.drawable.profile_image).into(holder.userImage)
        } else {
            holder.userImage.setImageResource(R.drawable.profile_image)
        }

        holder.itemView.setOnClickListener {
            onStoryClick(story)
        }
    }

    override fun getItemCount(): Int = stories.size
}
