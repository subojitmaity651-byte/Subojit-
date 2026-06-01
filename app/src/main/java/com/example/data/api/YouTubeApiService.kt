package com.example.data.api

import retrofit2.http.GET
import retrofit2.http.Query

interface YouTubeApiService {
    @GET("youtube/v3/videos")
    suspend fun getTrendingVideos(
        @Query("part") part: String = "snippet,statistics,contentDetails,status",
        @Query("chart") chart: String = "mostPopular",
        @Query("regionCode") regionCode: String = "US",
        @Query("maxResults") maxResults: Int = 20,
        @Query("key") key: String
    ): YouTubeVideoListResponse

    @GET("youtube/v3/search")
    suspend fun searchVideos(
        @Query("part") part: String = "snippet",
        @Query("q") query: String,
        @Query("type") type: String = "video",
        @Query("maxResults") maxResults: Int = 20,
        @Query("key") key: String
    ): YouTubeSearchResponse

    @GET("youtube/v3/videos")
    suspend fun getVideoDetails(
        @Query("part") part: String = "snippet,statistics,contentDetails,status",
        @Query("id") ids: String,
        @Query("key") key: String
    ): YouTubeVideoListResponse
}
