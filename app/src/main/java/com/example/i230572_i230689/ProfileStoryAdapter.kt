package com.example.i230572_i230689

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import java.io.File

class ProfileStoryAdapter(
    private val context: Context,
    private val stories: List<Story>
) : RecyclerView.Adapter<ProfileStoryAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val storyImage: CircleImageView = itemView.findViewById(R.id.story_image)
        val storyUsername: TextView = itemView.findViewById(R.id.story_username)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.story_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = stories.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val story = stories[position]
        holder.storyUsername.text = story.username
        if (story.storyImage.isNotEmpty()) {
            Picasso.get()
                .load(File(story.storyImage))
                .placeholder(R.drawable.profile_image)
                .error(R.drawable.profile_image)
                .into(holder.storyImage)
        } else {
            holder.storyImage.setImageResource(R.drawable.profile_image)
        }
    }
}
