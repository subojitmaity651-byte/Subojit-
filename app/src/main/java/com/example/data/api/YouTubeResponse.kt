package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class YouTubeVideoListResponse(
    @Json(name = "items") val items: List<VideoItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class VideoItem(
    @Json(name = "id") val id: String,
    @Json(name = "snippet") val snippet: Snippet?,
    @Json(name = "statistics") val statistics: Statistics?,
    @Json(name = "contentDetails") val contentDetails: ContentDetails?,
    @Json(name = "status") val status: VideoStatus? = null
)

@JsonClass(generateAdapter = true)
data class VideoStatus(
    @Json(name = "embeddable") val embeddable: Boolean? = true
)

@JsonClass(generateAdapter = true)
data class Snippet(
    @Json(name = "title") val title: String = "",
    @Json(name = "description") val description: String = "",
    @Json(name = "thumbnails") val thumbnails: Thumbnails?,
    @Json(name = "channelId") val channelId: String = "",
    @Json(name = "channelTitle") val channelTitle: String = "",
    @Json(name = "publishedAt") val publishedAt: String = ""
)

@JsonClass(generateAdapter = true)
data class Thumbnails(
    @Json(name = "default") val default: ThumbnailSize?,
    @Json(name = "medium") val medium: ThumbnailSize?,
    @Json(name = "high") val high: ThumbnailSize?
)

@JsonClass(generateAdapter = true)
data class ThumbnailSize(
    @Json(name = "url") val url: String = ""
)

@JsonClass(generateAdapter = true)
data class Statistics(
    @Json(name = "viewCount") val viewCount: String? = "0",
    @Json(name = "likeCount") val likeCount: String? = "0",
    @Json(name = "commentCount") val commentCount: String? = "0"
)

@JsonClass(generateAdapter = true)
data class ContentDetails(
    @Json(name = "duration") val duration: String? = "PT0S"
)

@JsonClass(generateAdapter = true)
data class YouTubeSearchResponse(
    @Json(name = "items") val items: List<SearchItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SearchItem(
    @Json(name = "id") val id: SearchId,
    @Json(name = "snippet") val snippet: Snippet?
)

@JsonClass(generateAdapter = true)
data class SearchId(
    @Json(name = "videoId") val videoId: String = ""
)
