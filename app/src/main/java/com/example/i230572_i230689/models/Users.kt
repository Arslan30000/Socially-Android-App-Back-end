package com.example.i230572_i230689
import com.google.firebase.database.Exclude

data class Story(
    var storyId: String = "",
    var userId: String = "",
    var username: String = "",
    var userProfilePicture: String? = null,
    var storyImage: String = "",
    var timestamp: Long = 0L,
    @get:Exclude
    var isAddButton: Boolean = false,
    @get:Exclude
    var hasStories: Boolean = false
) {
    // Corrected: Added the missing 'hasStories' parameter to the constructor call
    constructor() : this("", "", "", null, "", 0L, false, false)
}

data class Post(
    var postId: String = "",
    var userId: String = "",
    var postImage: String = "",
    var caption: String = "",
    var timestamp: Long = 0L,
    @get:Exclude
    var username: String = "",
    @get:Exclude
    var userProfileImage: String = "",
    @get:Exclude
    var likesCount: Int = 0,
    @get:Exclude
    var isLiked: Boolean = false,
    @get:Exclude
    var commentsCount: Int = 0
) {
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
    val chatId: String = "",
    var attachmentUrl: String = "",
    var type: String = "text",
    var isSeen: Boolean = false,
    var isDeleted: Boolean = false,
    var vanishOnClose: Boolean = false,
    var isEdited: Boolean = false
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
    var messages: Map<String, Map<String, Message>> = emptyMap(),
    var onlineStatus: String = "offline",
    var lastSeen: String? = null
)
