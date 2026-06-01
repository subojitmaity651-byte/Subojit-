package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.VideoRepository
import com.example.data.local.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ViewTube", appName)
  }

  @Test
  fun generateEnvFileFromSystemEnv() {
    val youtubeKey = System.getenv("YOUTUBE_API_KEY") ?: ""
    val geminiKey = System.getenv("GEMINI_API_KEY") ?: ""
    
    val userDir = System.getProperty("user.dir") ?: ""
    println("DEBUG_TELEMETRY: user.dir = $userDir")
    
    val parentDir = java.io.File(userDir)
    // If the working directory is under /app/applet/app, go one level up to the root of the project
    val rootDir = if (parentDir.name == "app" || parentDir.name == "applet") parentDir.parentFile else parentDir
    val projectRoot = if (rootDir?.name == "app") rootDir.parentFile else rootDir
    
    if (projectRoot != null) {
      val envFile = java.io.File(projectRoot, ".env")
      envFile.writeText("""
        GEMINI_API_KEY=$geminiKey
        YOUTUBE_API_KEY=$youtubeKey
      """.trimIndent())
      println("DEBUG_TELEMETRY: Successfully wrote .env to ${envFile.absolutePath}")
    } else {
      println("DEBUG_TELEMETRY_ERROR: Could not locate project root directory.")
    }
  }

  @Test
  fun runTelemetryAndLogResults() = runBlocking {
    // FRESH LIVE RUN FORCE ACTION
    println("=== START LIVE YOUTUBE TELEMETRY TEST ===")
    
    val envKey = System.getenv("YOUTUBE_API_KEY")
    if (envKey != null && envKey.isNotEmpty()) {
        println("DIAGNOSTICS_KEY_FIRST_6: ${envKey.take(6)}")
        println("DIAGNOSTICS_KEY_LAST_4: ${envKey.takeLast(4)}")
        println("DIAGNOSTICS_KEY_LENGTH: ${envKey.length}")
    } else {
        println("DIAGNOSTICS_KEY_EMPTY_OR_NULL")
    }
    
    val geminiKey = System.getenv("GEMINI_API_KEY")
    println("DEBUG_TELEMETRY: System.getenv(\"GEMINI_API_KEY\") = ${if (geminiKey != null) "FOUND (Length: ${geminiKey.length})" else "NOT FOUND"}")
    
    // List all environment variables to be absolutely sure
    System.getenv().forEach { (k, v) ->
        if (k.contains("YOUTUBE", ignoreCase = true) || k.contains("API", ignoreCase = true) || k.contains("KEY", ignoreCase = true)) {
            println("DEBUG_TELEMETRY_ENV: $k = ${if (v.isNotEmpty()) "FOUND (Length: ${v.length})" else "EMPTY"}")
        }
    }

    // Explicit standalone Retrofit instance to catch precise HTTP bodies
    val okHttpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    val moshi = com.squareup.moshi.Moshi.Builder()
        .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
        .build()

    val apiService = retrofit2.Retrofit.Builder()
        .baseUrl("https://www.googleapis.com/")
        .client(okHttpClient)
        .addConverterFactory(retrofit2.converter.moshi.MoshiConverterFactory.create(moshi))
        .build()
        .create(com.example.data.api.YouTubeApiService::class.java)

    val apiKey = envKey ?: ""

    // 1. Standalone Trending Feed Request
    try {
      println("Querying trending videos with API Key...")
      val trendingResponse = apiService.getTrendingVideos(maxResults = 5, key = apiKey)
      println("SUCCESS: Raw trending items count: ${trendingResponse.items.size}")
      trendingResponse.items.forEachIndexed { i, it ->
         println("  - [$i] Video ID: ${it.id} | Title: ${it.snippet?.title}")
      }
    } catch (e: retrofit2.HttpException) {
      val code = e.code()
      val message = e.message()
      val errBody = e.response()?.errorBody()?.string() ?: "Empty body"
      println("DEBUG_YOUTUBE_LIVE_TRENDING_ERROR_CODE: $code")
      println("DEBUG_YOUTUBE_LIVE_TRENDING_ERROR_MSG: $message")
      println("DEBUG_YOUTUBE_RAW_TRENDING_ERROR_BODY: $errBody")
    } catch (e: Exception) {
      println("DEBUG_YOUTUBE_LIVE_TRENDING_GENERAL_FAIL: ${e.message}")
    }

    // 2. Standalone Search Query Request
    try {
      val q = "new song Hindi"
      println("Querying search videos for '$q' with API Key...")
      val searchResponse = apiService.searchVideos(query = q, maxResults = 5, key = apiKey)
      println("SUCCESS: Raw search items count: ${searchResponse.items.size}")
      searchResponse.items.forEachIndexed { i, it ->
         println("  - [$i] Search Video ID: ${it.id?.videoId} | Title: ${it.snippet?.title}")
      }
    } catch (e: retrofit2.HttpException) {
      val code = e.code()
      val message = e.message()
      val errBody = e.response()?.errorBody()?.string() ?: "Empty body"
      println("DEBUG_YOUTUBE_LIVE_SEARCH_ERROR_CODE: $code")
      println("DEBUG_YOUTUBE_LIVE_SEARCH_ERROR_MSG: $message")
      println("DEBUG_YOUTUBE_RAW_SEARCH_ERROR_BODY: $errBody")
    } catch (e: Exception) {
      println("DEBUG_YOUTUBE_LIVE_SEARCH_GENERAL_FAIL: ${e.message}")
    }

    val dummyDao = object : VideoDao {
      override fun getFavorites(): Flow<List<FavoriteVideoEntity>> = flowOf(emptyList())
      override suspend fun insertFavorite(favorite: FavoriteVideoEntity) {}
      override suspend fun deleteFavoriteById(id: String) {}
      override fun isFavorite(id: String): Flow<Boolean> = flowOf(false)
      override fun getPlaylists(): Flow<List<PlaylistEntity>> = flowOf(emptyList())
      override suspend fun insertPlaylist(playlist: PlaylistEntity): Long = 0L
      override suspend fun deletePlaylist(playlistId: Long) {}
      override suspend fun deletePlaylistItems(playlistId: Long) {}
      override fun getPlaylistItems(playlistId: Long): Flow<List<PlaylistItemEntity>> = flowOf(emptyList())
      override suspend fun insertPlaylistItem(item: PlaylistItemEntity) {}
      override suspend fun deletePlaylistItem(playlistId: Long, videoId: String) {}
      override fun getPlaylistItemCount(playlistId: Long): Flow<Int> = flowOf(0)
      override fun getWatchHistory(): Flow<List<WatchHistoryEntity>> = flowOf(emptyList())
      override suspend fun insertWatchHistory(historyEntry: WatchHistoryEntity) {}
      override suspend fun deleteFromHistory(videoId: String) {}
      override suspend fun clearHistory() {}
      override fun getSubscriptions(): Flow<List<SubscriptionEntity>> = flowOf(emptyList())
      override suspend fun insertSubscription(subscription: SubscriptionEntity) {}
      override suspend fun deleteSubscription(channelId: String) {}
      override fun isSubscribed(channelId: String): Flow<Boolean> = flowOf(false)
    }

    try {
      println("Initializing VideoRepository...")
      val repository = VideoRepository(dummyDao)
      
      val apiKeyFlag = repository.isApiKeyConfigured()
      println("DEBUG_TELEMETRY: isApiKeyConfigured() = $apiKeyFlag")
      
      // Call Trending videos
      println("DEBUG_TELEMETRY: Calling getTrendingFeed('All')...")
      val trendingResult = repository.getTrendingFeed("All")
      println("DEBUG_TELEMETRY: getTrendingFeed Result size: ${trendingResult.size}")
      
      // Call Search videos
      val query = "new song Hindi"
      println("DEBUG_TELEMETRY: Calling searchVideos('$query')...")
      val searchResult = repository.searchVideos(query)
      println("DEBUG_TELEMETRY: searchVideos('$query') Result size: ${searchResult.size}")
      
    } catch (e: Exception) {
      println("DEBUG_TELEMETRY_ERROR: Test execution encountered exception: ${e.message}")
      e.printStackTrace()
    }
    
    println("=== END LIVE YOUTUBE TELEMETRY TEST ===")
  }
}

