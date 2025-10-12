package com.example.i230572_i230689
data class Post(
    var postId: String = "",
    var imageBase64: String = "",
    var caption: String = "",
    var timestamp: Long = 0
)

data class Story(
    var storyId: String = "",
    var imageBase64: String = "",
    var timestamp: Long = 0,
    var expiresAt: Long = 0
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
    var posts: Map<String, Post> = emptyMap(),
    var stories: Map<String, Story> = emptyMap()
)

