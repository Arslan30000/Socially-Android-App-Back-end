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
import de.hdodenhof.circleimageview.CircleImageView

// The adapter now accepts a lambda function `onAddStoryClick`
class StoryAdapter(
    private val context: Context,
    private val storyList: List<Story>,
    private val onAddStoryClick: () -> Unit
) : RecyclerView.Adapter<StoryAdapter.StoryViewHolder>() {

    class StoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val storyImage: CircleImageView = itemView.findViewById(R.id.story_image)
        val storyUsername: TextView = itemView.findViewById(R.id.story_username)
        val addStoryButton: ImageView = itemView.findViewById(R.id.story_add)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.story_item, parent, false)
        return StoryViewHolder(view)
    }

    override fun getItemCount(): Int {
        return storyList.size
    }

    override fun onBindViewHolder(holder: StoryViewHolder, position: Int) {
        val story = storyList[position]

        if (story.isAddButton) {
            // This is the "Your Story" circle
            holder.storyUsername.text = "Your Story"
            holder.addStoryButton.visibility = View.VISIBLE

            // Decode and set the user's profile picture
            try {
                if (!story.userProfilePicture.isNullOrEmpty()) {
                    val imageBytes = Base64.decode(story.userProfilePicture, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    holder.storyImage.setImageBitmap(bitmap)
                } else {
                    holder.storyImage.setImageResource(R.drawable.profile_image) // Fallback
                }
            } catch (e: Exception) {
                holder.storyImage.setImageResource(R.drawable.profile_image) // Fallback on error
            }

            // Set the click listener to trigger the lambda function
            holder.addStoryButton.setOnClickListener {
                onAddStoryClick()
            }
            holder.storyImage.setOnClickListener {
                onAddStoryClick() // Also allow clicking the image to add a story
            }

        } else {
            // This is a story from another user
            holder.storyUsername.text = story.username
            holder.addStoryButton.visibility = View.GONE

            // Here you would decode and load story.storyImage
            // For now, it remains a placeholder
            holder.storyImage.setImageResource(R.drawable.profile_image)
        }
    }
}
