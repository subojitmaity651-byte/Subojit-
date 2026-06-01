package com.example.model

data class Video(
    val id: String,
    val title: String,
    val description: String,
    val thumbnail: String,
    val duration: String,
    val channelId: String,
    val channelTitle: String,
    val channelAvatar: String,
    val viewCount: String,
    val likeCount: String,
    val publishedAt: String,
    val category: String,
    val isShort: Boolean = false,
    val isEmbeddable: Boolean = true
)

data class Channel(
    val id: String,
    val title: String,
    val avatar: String,
    val banner: String,
    val subscriberCount: String,
    val description: String,
    val isSubscribed: Boolean = false
)

data class Comment(
    val id: String,
    val videoId: String,
    val authorName: String,
    val authorAvatar: String,
    val text: String,
    val publishedAt: String,
    val likeCount: Int = 0
)
