package com.example.i230572_i230689
import com.google.firebase.database.Exclude

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
,
    @get:Exclude
    var hasStories: Boolean = false
) {
    // Add a no-argument constructor, which is required by Firebase for deserialization.
    constructor() : this("", "", "", null, "", 0L, false)
}

data class Post(
    var postId: String = "",
    var userId: String = "",
    var postImage: String = "", // The Base64 string of the main post image
    var caption: String = "",
    var timestamp: Long = 0L,

    // We will populate these fields locally in the app, not from the /posts/ node.
    // The @Exclude annotation is crucial to prevent Firebase from trying to save them.
    @get:Exclude
    var username: String = "",
    @get:Exclude
    var userProfileImage: String = "", // Base64 string for the author's PFP

    // You can add these later as you build out features
    @get:Exclude
    var likesCount: Int = 0,
    @get:Exclude
    var isLiked: Boolean = false,
    @get:Exclude
    var commentsCount: Int = 0
) {
    // Required no-argument constructor for Firebase to work
    constructor() : this("", "", "", "", 0L)
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

data class Comment(
    var id: String = "",
    var postId: String = "",
    var userId: String = "",
    var username: String = "",
    var userProfileImage: String = "",
    var text: String = "",
    var timestamp: String = ""
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
