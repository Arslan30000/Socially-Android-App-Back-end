package com.example.i230572_i230689
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.hdodenhof.circleimageview.CircleImageView

// First, define a data class for your story model
//data class Story(val username: String, val imageUrl: String) // Use drawable resource ID or URL string

class StoryAdapter(private val storyList: List<Story>) :
    RecyclerView.Adapter<StoryAdapter.StoryViewHolder>() {

    // Describes an item view and metadata about its place within the RecyclerView.
    class StoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val storyImage: CircleImageView = itemView.findViewById(R.id.story_image)
        val storyUsername: TextView = itemView.findViewById(R.id.story_username)
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.story_item, parent, false)
        return StoryViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: StoryViewHolder, position: Int) {
        val currentStory = storyList[position]
        holder.storyUsername.text = currentStory.username
        // Here you would load the image using a library like Glide or Picasso
        // For now, we can set a placeholder
        // holder.storyImage.setImageResource(R.drawable.profile_image)
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = storyList.size
}
