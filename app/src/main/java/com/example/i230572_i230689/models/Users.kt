package com.example.i230572_i230689
import com.google.firebase.database.Exclude
data class Post(
    var postId: String = "",
    var imageBase64: String = "",
    var caption: String = "",
    var timestamp: Long = 0
)

data class Story(
    var storyId: String = "",
    var userId: String = "",
    var username: String = "",
    var userProfilePicture: String? = null, // Bitmap string for the user's pfp
    var storyImage: String = "",            // Bitmap string for the story image itself
    var timestamp: Long = 0L,               // Time of upload, crucial for 24-hour logic

    // This local-only flag tells the adapter if this is the user's "Add Story" circle.
    // The @Exclude annotation prevents this field from being saved to Firebase.
    @get:Exclude
    var isAddButton: Boolean = false
) {
    // Add a no-argument constructor, which is required by Firebase for deserialization.
    constructor() : this("", "", "", null, "", 0L, false)
}

data class Message(
    var messageId: String = "",
    var senderId: String = "",
    var receiverId: String = "",
    var text: String = "",
    var imageBase64: String = "",
    var postId: String = "",
    var timestamp: Long = 0,
    val chatId: String = ""
)

data class Chat(
    var chatId: String = "",
    var participants: Map<String, Boolean> = emptyMap(),
    var lastMessage: String = "",
    var lastMessageTime: Long = 0
)

data class User(
    var uid: String = "",
    var name: String = "",
    var lastname: String = "",
    var username: String = "",
    var email: String = "",
    var password: String = "",
    var date: String = "",
    var imageBase64: String = "",
    var followers: Map<String, Boolean> = emptyMap(),
    var following: Map<String, Boolean> = emptyMap(),
    var followRequests: Map<String, Boolean> = emptyMap(),
    var bio: String = "",
    var posts: Map<String, Post> = emptyMap(),
    var stories: Map<String, Story> = emptyMap(),
    var chats: Map<String, Chat> = emptyMap(),
    var messages: Map<String, Map<String, Message>> = emptyMap()
)
