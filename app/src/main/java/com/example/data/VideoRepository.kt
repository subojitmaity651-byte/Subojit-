package com.example.data

import android.util.Log
import com.example.BuildConfig
import com.example.data.api.YouTubeApiService
import com.example.data.local.*
import com.example.model.Channel
import com.example.model.Comment
import com.example.model.Video
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class VideoRepository(private val videoDao: VideoDao) {

    private val apiService: YouTubeApiService?

    init {
        val apiKey = BuildConfig.YOUTUBE_API_KEY
        val hasValidKey = apiKey.isNotEmpty() && apiKey != "MY_YOUTUBE_API_KEY"

        apiService = if (true) { // Always create client but guard usage dynamically
            try {
                val logging = HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.HEADERS
                }
                val okHttpClient = OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build()

                val moshi = Moshi.Builder()
                    .add(KotlinJsonAdapterFactory())
                    .build()

                Retrofit.Builder()
                    .baseUrl("https://www.googleapis.com/")
                    .client(okHttpClient)
                    .addConverterFactory(MoshiConverterFactory.create(moshi))
                    .build()
                    .create(YouTubeApiService::class.java)
            } catch (e: Exception) {
                Log.e("VideoRepository", "Failed to create Retrofit client: ${e.message}")
                null
            }
        } else {
            null
        }
    }

    // Checking if api key is configured
    fun isApiKeyConfigured(): Boolean {
        val key = BuildConfig.YOUTUBE_API_KEY
        return key.isNotEmpty() && key != "MY_YOUTUBE_API_KEY"
    }

    // Interactive user comments state stored in memory during lifetime, preseeded
    private val localUserComments = mutableMapOf<String, MutableList<Comment>>()

    // Pre-seeded, highly polished list of actual videos with real YouTube Video IDs
    private val preseededVideos = listOf(
        Video(
            id = "jfKfPfyJRdk",
            title = "lofi hip hop radio 📚 beats to relax/study to",
            description = "Thank you for listening, I hope you will have a good time here :)\n📚 Check out Lofi Girl on Spotify, Apple Music, and more!",
            thumbnail = "https://img.youtube.com/vi/jfKfPfyJRdk/0.jpg",
            duration = "LIVE",
            channelId = "UCAL3JXZSzUN8vPL_UC_86Sg",
            channelTitle = "Lofi Girl",
            channelAvatar = "https://images.unsplash.com/photo-1518495973542-4542c06a5843?w=150&auto=format&fit=crop&q=80",
            viewCount = "14,352 watching",
            likeCount = "451K",
            publishedAt = "Live Now",
            category = "Music"
        ),
        Video(
            id = "CH1XGDu-1cA",
            title = "My Ultimate Smart Home Tour: 2026 Edition!",
            description = "The smart home tour is finally here. Going through every single automated smart room, custom integration, and voice commands we set up this year.",
            thumbnail = "https://img.youtube.com/vi/CH1XGDu-1cA/0.jpg",
            duration = "18:42",
            channelId = "UCBJycsmduvYELgT-j3yF6eg",
            channelTitle = "Marques Brownlee",
            channelAvatar = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop&q=80",
            viewCount = "2.4M",
            likeCount = "182K",
            publishedAt = "2 days ago",
            category = "Tech"
        ),
        Video(
            id = "JyECrGp-F5Y",
            title = "What If We Detonated All Nuclear Bombs at Once?",
            description = "Detonating all nuclear weapons in the world at the exact same location. What would happen to the climate, geography, and humanity?",
            thumbnail = "https://img.youtube.com/vi/JyECrGp-F5Y/0.jpg",
            duration = "11:03",
            channelId = "UCsXVk37bltUxv1m7K6c023g",
            channelTitle = "Kurzgesagt – In a Nutshell",
            channelAvatar = "https://images.unsplash.com/photo-1627515151676-e17ee8ce6015?w=150&auto=format&fit=crop&q=80",
            viewCount = "12M",
            likeCount = "920K",
            publishedAt = "1 year ago",
            category = "Science"
        ),
        Video(
            id = "Uj3_KjqodQI",
            title = "The Quirks and Paradoxes of Hilbert's Infinite Hotel",
            description = "Welcome to the Infinite Hotel, an intriguing thought experiment detailing the mathematics and counter-intuitive logic of infinity.",
            thumbnail = "https://img.youtube.com/vi/Uj3_KjqodQI/0.jpg",
            duration = "14:15",
            channelId = "UCHnyfMqiRRG1u-2MsSQLbXA",
            channelTitle = "Veritasium",
            channelAvatar = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150&auto=format&fit=crop&q=80",
            viewCount = "6.1M",
            likeCount = "385K",
            publishedAt = "5 months ago",
            category = "Science"
        ),
        Video(
            id = "kSg1M5L8ZqM",
            title = "Spinning CD at 150,000 FPS - The Slow Mo Guys",
            description = "Watch standard CDs bend and burst into millions of tiny plastic fragments captured in ultra slow-motion at 150,000 frames per second.",
            thumbnail = "https://img.youtube.com/vi/kSg1M5L8ZqM/0.jpg",
            duration = "08:34",
            channelId = "UCUK0HBIBWgM2c4vsPhkYY4w",
            channelTitle = "The Slow Mo Guys",
            channelAvatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80",
            viewCount = "9.8M",
            likeCount = "410K",
            publishedAt = "3 years ago",
            category = "Gaming"
        ),
        Video(
            id = "372CE1yY8gE",
            title = "Jetpack Compose in 2 Minutes - Android Developers",
            description = "A rapid crash course showcasing standard declarative components in Jetpack Compose, state handling, and interactive layouts in modern Android development.",
            thumbnail = "https://img.youtube.com/vi/372CE1yY8gE/0.jpg",
            duration = "02:11",
            channelId = "UCVHFbqXqoYvEWM1Ddxl0QDg",
            channelTitle = "Android Developers",
            channelAvatar = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=150&auto=format&fit=crop&q=80",
            viewCount = "850K",
            likeCount = "45K",
            publishedAt = "1 year ago",
            category = "Tech"
        ),
        Video(
            id = "p0S1wK3V_44",
            title = "Steve Jobs Unveils the Original iPhone in 2007",
            description = "The historical keynote session of January 9, 2007. Steve Jobs rolls out three revolutionary products: a widescreen iPod with touch controls, a revolutionary mobile phone, and a breakthrough internet communications device.",
            thumbnail = "https://img.youtube.com/vi/p0S1wK3V_44/0.jpg",
            duration = "50:18",
            channelId = "UC7gR8qR1z-iFqitP7G7pTUA",
            channelTitle = "Apple History",
            channelAvatar = "https://images.unsplash.com/photo-1560250097-0b93528c311a?w=150&auto=format&fit=crop&q=80",
            viewCount = "33M",
            likeCount = "1.5M",
            publishedAt = "9 years ago",
            category = "Tech"
        ),
        Video(
            id = "t6m6g5f8mD8",
            title = "Why M4 Pro MacBook Pro is the Ultimate Creator Laptop",
            description = "Apple's upgraded M4 chip in depth. Reviewing performance benchmarks, standard battery endurance, liquid retina screen brilliance, and daily editing workflows.",
            thumbnail = "https://img.youtube.com/vi/t6m6g5f8mD8/0.jpg",
            duration = "12:50",
            channelId = "UCBJycsmduvYELgT-j3yF6eg",
            channelTitle = "Marques Brownlee",
            channelAvatar = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop&q=80",
            viewCount = "1.8M",
            likeCount = "115K",
            publishedAt = "4 days ago",
            category = "Tech"
        ),
        Video(
            id = "f796S381-Zg",
            title = "Lofi Girl 🎧 Late Night Study Session",
            description = "Step inside Lofi Girl's calm virtual study workspace containing high-fidelity chill jazz beats and relaxing vinyl crackles for sleepless coding nights.",
            thumbnail = "https://img.youtube.com/vi/f796S381-Zg/0.jpg",
            duration = "02:30:15",
            channelId = "UCAL3JXZSzUN8vPL_UC_86Sg",
            channelTitle = "Lofi Girl",
            channelAvatar = "https://images.unsplash.com/photo-1518495973542-4542c06a5843?w=150&auto=format&fit=crop&q=80",
            viewCount = "5.1M",
            likeCount = "320K",
            publishedAt = "2 months ago",
            category = "Music"
        ),
        // Shorts - vertical video formats
        Video(
            id = "T2S_b9fA32E",
            title = "My Minimalist Coding Setup! 🌌 #shorts #desksetup #developer",
            description = "Quick tour of my desk set up with warm lighting, mechanical keyboard ASMR, and custom Android emulator on an curved layout.",
            thumbnail = "https://img.youtube.com/vi/T2S_b9fA32E/maxresdefault.jpg",
            duration = "0:45",
            channelId = "UCBJycsmduvYELgT-j3yF6eg",
            channelTitle = "Esther Codes",
            channelAvatar = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150&auto=format&fit=crop&q=80",
            viewCount = "250K",
            likeCount = "22K",
            publishedAt = "1 week ago",
            category = "Shorts",
            isShort = true
        ),
        Video(
            id = "1AnSgM_p-40",
            title = "Perfect morning espresso pull ☕️ #shorts #coffee #asmr",
            description = "Capturing a satisfying double shot espresso pull using a bottomless portafilter. Relax and enjoy the golden crema cascade.",
            thumbnail = "https://img.youtube.com/vi/1AnSgM_p-40/maxresdefault.jpg",
            duration = "0:30",
            channelId = "UCVHFbqXqoYvEWM1Ddxl0QDg",
            channelTitle = "The Coffee Guy",
            channelAvatar = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150&auto=format&fit=crop&q=80",
            viewCount = "500K",
            likeCount = "48K",
            publishedAt = "3 days ago",
            category = "Shorts",
            isShort = true
        ),
        Video(
            id = "_k_VreYVbXk",
            title = "Typing on 75% Mechanical Keyboard ASMR ⌨️ #shorts #asmr",
            description = "Enjoy the pleasing clicks, clacks, and thocks of linear switches inside an aluminum framing with standard dynamic custom keycaps.",
            thumbnail = "https://img.youtube.com/vi/_k_VreYVbXk/maxresdefault.jpg",
            duration = "0:58",
            channelId = "UCUK0HBIBWgM2c4vsPhkYY4w",
            channelTitle = "KeyClack",
            channelAvatar = "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=150&auto=format&fit=crop&q=80",
            viewCount = "640K",
            likeCount = "55K",
            publishedAt = "2 weeks ago",
            category = "Shorts",
            isShort = true
        ),
        Video(
            id = "m77RHe34Bkw",
            title = "Views from the Swiss Alps Express 🏔️🚂 #shorts #travel",
            description = "Climbing some of the most breathtaking snowy slopes in Switzerland aboard the iconic panoramic express. Truly magical scenic transit.",
            thumbnail = "https://img.youtube.com/vi/m77RHe34Bkw/maxresdefault.jpg",
            duration = "1:00",
            channelId = "UCHnyfMqiRRG1u-2MsSQLbXA",
            channelTitle = "Wanderlust",
            channelAvatar = "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=150&auto=format&fit=crop&q=80",
            viewCount = "1.2M",
            likeCount = "120K",
            publishedAt = "5 days ago",
            category = "Shorts",
            isShort = true
        )
    )

    // Pre-seeded list of channels
    val preseededChannels = listOf(
        Channel(
            id = "UCAL3JXZSzUN8vPL_UC_86Sg",
            title = "Lofi Girl",
            avatar = "https://images.unsplash.com/photo-1518495973542-4542c06a5843?w=150&auto=format&fit=crop&q=80",
            banner = "https://images.unsplash.com/photo-1518495973542-4542c06a5843?w=800&auto=format&fit=crop&q=80",
            subscriberCount = "14.3M",
            description = "Welcome to the official Lofi Girl channel! Chill beats to relax, study, work, or code to. Find our official streams and playlists right here.",
            isSubscribed = false
        ),
        Channel(
            id = "UCBJycsmduvYELgT-j3yF6eg",
            title = "Marques Brownlee",
            avatar = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop&q=80",
            banner = "https://images.unsplash.com/photo-1507842217343-583bb7270b66?w=800&auto=format&fit=crop&q=80",
            subscriberCount = "18.7M",
            description = "MKBHD: Quality Tech Videos | Smartphones, Tablets, Smart Home Automations, Laptops, and EV Reviews.",
            isSubscribed = false
        ),
        Channel(
            id = "UCsXVk37bltUxv1m7K6c023g",
            title = "Kurzgesagt – In a Nutshell",
            avatar = "https://images.unsplash.com/photo-1627515151676-e17ee8ce6015?w=150&auto=format&fit=crop&q=80",
            banner = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800&auto=format&fit=crop&q=80",
            subscriberCount = "22.4M",
            description = "Videos explaining science, existence, future tech, space exploration, and anthropology with cheerful animations and pessimistic realism.",
            isSubscribed = false
        ),
        Channel(
            id = "UCHnyfMqiRRG1u-2MsSQLbXA",
            title = "Veritasium",
            avatar = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150&auto=format&fit=crop&q=80",
            banner = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=800&auto=format&fit=crop&q=80",
            subscriberCount = "15.1M",
            description = "An element of truth - videos about science, engineering, psychology, and interesting logic paradoxes.",
            isSubscribed = false
        )
    )

    // Preseed comments pool
    private val standardCommentPool = listOf(
        Pair("Alex Johnson", "This is absolutely the most therapeutic stream on YouTube. Helped me get through my engineering finals in one piece! Keyboard click ASMR in background complements it beautifully."),
        Pair("Sarah Miller", "Can we appreciate the animator who keeps this girl studying day and night without ever quitting? Truly a legend. Dynamic weather effects are so elegant."),
        Pair("TechInsider", "Marques never misses! The smart home setup is indeed futuristic, yet surprisingly practical. That custom dynamic lighting hub is brilliant!"),
        Pair("Dave K.", "The Hilbert's Hotel paradox still absolute breaks my brain. Infinity is such an incredibly weird and counter-intuitive construct. Veritasium explained it so cleanly!"),
        Pair("Clara Oswald", "I love the artwork on Kurzgesagt. It elevates complex science and cosmology to something understandable and absolutely charming! Brilliant work."),
        Pair("Coder101", "Coding up the native Compose layout of ViewTube while listening to this exact Lofi beats track. Absolute peak experience! Dev approved.")
    )

    // FETCHING TRENDING FEED
    suspend fun getTrendingFeed(category: String = "All"): List<Video> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.YOUTUBE_API_KEY
        val keyStatus = if (apiKey.isEmpty()) "EMPTY/NULL" else "CONTAINS KEY (Length: ${apiKey.length}, Ending: ...${apiKey.takeLast(4)})"
        Log.i("YouTubeTelemetry", "=== TELEMETRY START [getTrendingFeed] ===")
        Log.i("YouTubeTelemetry", "YouTube API Key loaded status: $keyStatus")
        
        if (isApiKeyConfigured()) {
            try {
                Log.i("YouTubeTelemetry", "Initiating raw HTTP request to get Trending videos...")
                val response = apiService?.getTrendingVideos(maxResults = 25, key = apiKey)
                if (response != null) {
                    Log.i("YouTubeTelemetry", "Trending request executed successfully. HTTP Status: 200 OK")
                    val rawCount = response.items.size
                    Log.i("YouTubeTelemetry", "Number of videos returned by raw API response: $rawCount")
                    
                    // Printing sample titles and embeddable status
                    response.items.forEachIndexed { index, item ->
                        val snippet = item.snippet
                        val embedd = item.status?.embeddable ?: true
                        Log.i("YouTubeTelemetry", "  - Index: $index | Video ID: '${item.id}' | Embeddable: $embedd | Title: '${snippet?.title ?: "No Title"}'")
                    }

                    val liveVideos = response.items.map { item ->
                        val snippet = item.snippet
                        val stats = item.statistics
                        val durationIso = item.contentDetails?.duration ?: "PT0S"
                        val formattedDuration = formatIsoDuration(durationIso)
                        val viewC = formatBigNumber(stats?.viewCount?.toLongOrNull() ?: 0L)
                        val likeC = formatBigNumber(stats?.likeCount?.toLongOrNull() ?: 0L)
                        val relativeT = timeAgo(snippet?.publishedAt ?: "")
                        val embedd = item.status?.embeddable ?: true

                        Video(
                            id = item.id,
                            title = snippet?.title ?: "No Title",
                            description = snippet?.description ?: "",
                            thumbnail = snippet?.thumbnails?.high?.url ?: snippet?.thumbnails?.medium?.url ?: "https://img.youtube.com/vi/${item.id}/0.jpg",
                            duration = formattedDuration,
                            channelId = snippet?.channelId ?: "",
                            channelTitle = snippet?.channelTitle ?: "Creator",
                            channelAvatar = getAvatarForChannel(snippet?.channelTitle ?: ""),
                            viewCount = "$viewC views",
                            likeCount = likeC,
                            publishedAt = relativeT,
                            category = if (snippet?.title?.contains("Music", ignoreCase = true) == true) "Music" else "All",
                            isShort = false,
                            isEmbeddable = embedd
                        )
                    }
                    
                    val filteredVideos = liveVideos.filter { it.isEmbeddable }
                    Log.i("YouTubeTelemetry", "Number of trending videos remaining after embeddable filtering: ${filteredVideos.size} / $rawCount")
                    
                    Log.i("YouTubeTelemetry", "=== TELEMETRY END [getTrendingFeed] ===")
                    if (category == "All") return@withContext filteredVideos
                    return@withContext filteredVideos.filter {
                        it.category.equals(category, ignoreCase = true) ||
                                it.title.contains(category, ignoreCase = true) ||
                                it.description.contains(category, ignoreCase = true)
                    }
                }
            } catch (e: retrofit2.HttpException) {
                val code = e.code()
                val message = e.message()
                val bodyString = try {
                    e.response()?.errorBody()?.string()
                } catch (ex: Exception) {
                    null
                }
                Log.e("YouTubeTelemetry", "API HTTP ERROR (Quota / Authorization / Bad Request). Code: $code, Message: $message", e)
                Log.e("YouTubeTelemetry", "HTTP Error details from YouTube server: $bodyString")
                Log.i("YouTubeTelemetry", "=== TELEMETRY END WITH HTTP EXCEPTION [getTrendingFeed] ===")
                
                // Throw robust custom diagnostics error to show matching UI error screen
                throw RuntimeException("PROJECT_DIAGNOSTICS_ERROR_403|CODE:${code}|BODY:${bodyString ?: message ?: "Forbidden"}")
            } catch (e: Exception) {
                Log.e("YouTubeTelemetry", "API Call failed with General Exception: ${e.message}", e)
                Log.i("YouTubeTelemetry", "=== TELEMETRY END WITH GENERAL EXCEPTION [getTrendingFeed] ===")
                throw e
            }
        } else {
            Log.w("YouTubeTelemetry", "YouTube API Key is not configured correctly in .env / BuildConfig (using mock preseed fallback)")
            Log.i("YouTubeTelemetry", "=== TELEMETRY END [getTrendingFeed - Not Configured] ===")
            throw RuntimeException("PROJECT_DIAGNOSTICS_KEY_MISSING")
        }

        throw RuntimeException("Fallback feeds are disabled because the raw YouTube API was initiated but failed to returned valid feeds.")
    }

    // SEARCH VIDEOS
    suspend fun searchVideos(query: String): List<Video> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.YOUTUBE_API_KEY
        val keyStatus = if (apiKey.isEmpty()) "EMPTY/NULL" else "CONTAINS KEY (Length: ${apiKey.length}, Ending: ...${apiKey.takeLast(4)})"
        Log.i("YouTubeTelemetry", "=== TELEMETRY START [searchVideos] ===")
        Log.i("YouTubeTelemetry", "YouTube API Key loaded status: $keyStatus")
        Log.i("YouTubeTelemetry", "Query string: '$query'")
        
        if (isApiKeyConfigured() && query.isNotEmpty()) {
            try {
                Log.i("YouTubeTelemetry", "Initiating raw search query request...")
                val searchResponse = apiService?.searchVideos(query = query, maxResults = 20, key = apiKey)
                if (searchResponse != null) {
                    Log.i("YouTubeTelemetry", "Search response executed successfully (Search Request). HTTP Status: 200 OK")
                    val rawSearchCount = searchResponse.items.size
                    Log.i("YouTubeTelemetry", "Number of items returned by search index: $rawSearchCount")
                    
                    val videoIds = searchResponse.items.mapNotNull {
                        val vid = it.id.videoId
                        if (vid.isNotEmpty()) vid else null
                    }.joinToString(",")

                    if (videoIds.isNotEmpty()) {
                        Log.i("YouTubeTelemetry", "Extracted video IDs for detailed mapping query: $videoIds")
                        Log.i("YouTubeTelemetry", "Initiating detailed video info request...")
                        val detailResponse = apiService?.getVideoDetails(ids = videoIds, key = apiKey)
                        if (detailResponse != null) {
                            Log.i("YouTubeTelemetry", "Detail info request executed successfully. HTTP Status: 200 OK")
                            val rawDetailCount = detailResponse.items.size
                            Log.i("YouTubeTelemetry", "Number of videos returned by raw detailed info response: $rawDetailCount")
                            
                            // Printing titles & embeddable status of searches
                            detailResponse.items.forEachIndexed { index, item ->
                                val snippet = item.snippet
                                val embedd = item.status?.embeddable ?: true
                                Log.i("YouTubeTelemetry", "  - Index: $index | Video ID: '${item.id}' | Embeddable: $embedd | Title: '${snippet?.title ?: "No Title"}'")
                            }

                            val searchVideosMapped = detailResponse.items.map { item ->
                                val snippet = item.snippet
                                val stats = item.statistics
                                val durationIso = item.contentDetails?.duration ?: "PT0S"
                                val formattedDuration = formatIsoDuration(durationIso)
                                val viewC = formatBigNumber(stats?.viewCount?.toLongOrNull() ?: 0L)
                                val likeC = formatBigNumber(stats?.likeCount?.toLongOrNull() ?: 0L)
                                val relativeT = timeAgo(snippet?.publishedAt ?: "")
                                val embedd = item.status?.embeddable ?: true

                                Video(
                                    id = item.id,
                                    title = snippet?.title ?: "No Title",
                                    description = snippet?.description ?: "",
                                    thumbnail = snippet?.thumbnails?.high?.url ?: snippet?.thumbnails?.medium?.url ?: "https://img.youtube.com/vi/${item.id}/0.jpg",
                                    duration = formattedDuration,
                                    channelId = snippet?.channelId ?: "",
                                    channelTitle = snippet?.channelTitle ?: "Creator",
                                    channelAvatar = getAvatarForChannel(snippet?.channelTitle ?: ""),
                                    viewCount = "$viewC views",
                                    likeCount = likeC,
                                    publishedAt = relativeT,
                                    category = "All",
                                    isShort = false,
                                    isEmbeddable = embedd
                                )
                            }
                            
                            val filteredSearchVideos = searchVideosMapped.filter { it.isEmbeddable }
                            Log.i("YouTubeTelemetry", "Number of search videos remaining after embeddable filtering: ${filteredSearchVideos.size} / $rawDetailCount")
                            Log.i("YouTubeTelemetry", "=== TELEMETRY END [searchVideos] ===")
                            return@withContext filteredSearchVideos
                        }
                    } else {
                        Log.w("YouTubeTelemetry", "No valid video IDs found in search results.")
                        return@withContext emptyList<Video>()
                    }
                }
            } catch (e: retrofit2.HttpException) {
                val code = e.code()
                val message = e.message()
                val bodyString = try {
                    e.response()?.errorBody()?.string()
                } catch (ex: Exception) {
                    null
                }
                Log.e("YouTubeTelemetry", "API HTTP ERROR in search (Quota / Authorization / Bad Request). Code: $code, Message: $message", e)
                Log.e("YouTubeTelemetry", "HTTP Search Error details from YouTube server: $bodyString")
                Log.i("YouTubeTelemetry", "=== TELEMETRY END WITH HTTP EXCEPTION [searchVideos] ===")
                throw RuntimeException("PROJECT_DIAGNOSTICS_ERROR_403|CODE:${code}|BODY:${bodyString ?: message ?: "Forbidden"}")
            } catch (e: Exception) {
                Log.e("YouTubeTelemetry", "Search API failed with General Exception: ${e.message}", e)
                Log.i("YouTubeTelemetry", "=== TELEMETRY END WITH GENERAL EXCEPTION [searchVideos] ===")
                throw e
            }
        } else {
            Log.w("YouTubeTelemetry", "YouTube API Key is not configured or query is blank (using local preseed cache search)")
            Log.i("YouTubeTelemetry", "=== TELEMETRY END [searchVideos - Not Configured / Empty] ===")
            throw RuntimeException("PROJECT_DIAGNOSTICS_KEY_MISSING")
        }

        throw RuntimeException("Search matching disabled. Please configure your live YouTube API Key securely in AI Studio Secrets.")
    }

    // GET SHORTS FEED
    suspend fun getShortsFeed(): List<Video> = withContext(Dispatchers.IO) {
        return@withContext preseededVideos.filter { it.isShort && it.isEmbeddable }
    }

    // GET VIDEO COMMENTS
    suspend fun commentsForVideo(videoId: String): List<Comment> {
        val list = localUserComments.getOrPut(videoId) {
            val userList = mutableListOf<Comment>()
            // Seed 3 customized standard comments
            standardCommentPool.shuffled().take(3).forEachIndexed { index, pair ->
                userList.add(
                    Comment(
                        id = "${videoId}_comment_${index}",
                        videoId = videoId,
                        authorName = pair.first,
                        authorAvatar = getAvatarForChannel(pair.first),
                        text = pair.second,
                        publishedAt = "${index + 1} hr ago"
                    )
                )
            }
            userList
        }
        return list.toList()
    }

    // SAVE/ADD COMMENT DYNAMICALLY
    suspend fun addComment(videoId: String, authorName: String, text: String): Comment {
        val newComment = Comment(
            id = "${videoId}_usr_${System.currentTimeMillis()}",
            videoId = videoId,
            authorName = authorName,
            authorAvatar = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150&auto=format&fit=crop&q=80",
            text = text,
            publishedAt = "Just now",
            likeCount = 0
        )
        localUserComments.getOrPut(videoId) { mutableListOf() }.add(0, newComment)
        return newComment
    }

    // GET CHANNEL BY ID
    fun getChannelById(channelId: String): Channel {
        return preseededChannels.find { it.id == channelId } ?: Channel(
            id = channelId,
            title = "Creator Studio",
            avatar = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150&auto=format&fit=crop&q=80",
            banner = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=800&auto=format&fit=crop&q=80",
            subscriberCount = "120K",
            description = "Welcome to my creative space where we build elegant native Android components and share design philosophies.",
            isSubscribed = false
        )
    }

    // HELPER AVATAR
    private fun getAvatarForChannel(channelName: String): String {
        return when (channelName) {
            "Lofi Girl" -> "https://images.unsplash.com/photo-1518495973542-4542c06a5843?w=150&auto=format&fit=crop&q=80"
            "Marques Brownlee" -> "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop&q=80"
            "Kurzgesagt – In a Nutshell" -> "https://images.unsplash.com/photo-1627515151676-e17ee8ce6015?w=150&auto=format&fit=crop&q=80"
            "Veritasium" -> "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150&auto=format&fit=crop&q=80"
            "The Slow Mo Guys" -> "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80"
            "Android Developers" -> "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=150&auto=format&fit=crop&q=80"
            "Esther Codes" -> "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150&auto=format&fit=crop&q=80"
            "The Coffee Guy" -> "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150&auto=format&fit=crop&q=80"
            "KeyClack" -> "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=150&auto=format&fit=crop&q=80"
            "Wanderlust" -> "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=150&auto=format&fit=crop&q=80"
            else -> "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=150&auto=format&fit=crop&q=80"
        }
    }

    // LOCAL DAO BRIDGE OPERATIONS
    fun getFavorites(): Flow<List<Video>> = videoDao.getFavorites().map { entities ->
        entities.map { it.toVideo() }
    }

    suspend fun saveFavorite(vid: Video) {
        videoDao.insertFavorite(
            FavoriteVideoEntity(
                id = vid.id,
                title = vid.title,
                description = vid.description,
                thumbnail = vid.thumbnail,
                duration = vid.duration,
                channelId = vid.channelId,
                channelTitle = vid.channelTitle,
                channelAvatar = vid.channelAvatar,
                viewCount = vid.viewCount,
                publishedAt = vid.publishedAt
            )
        )
    }

    suspend fun deleteFavorite(id: String) {
        videoDao.deleteFavoriteById(id)
    }

    fun isFavorite(id: String): Flow<Boolean> = videoDao.isFavorite(id)

    // Watch History
    fun getWatchHistory(): Flow<List<Video>> = videoDao.getWatchHistory().map { entities ->
        entities.map { it.toVideo() }
    }

    suspend fun addToHistory(vid: Video) {
        videoDao.insertWatchHistory(
            WatchHistoryEntity(
                videoId = vid.id,
                title = vid.title,
                thumbnail = vid.thumbnail,
                duration = vid.duration,
                channelId = vid.channelId,
                channelTitle = vid.channelTitle,
                channelAvatar = vid.channelAvatar,
                viewCount = vid.viewCount
            )
        )
    }

    suspend fun clearHistory() {
        videoDao.clearHistory()
    }

    // Playlists
    fun getPlaylists(): Flow<List<PlaylistEntity>> = videoDao.getPlaylists()

    suspend fun createPlaylist(name: String, desc: String): Long {
        return videoDao.insertPlaylist(PlaylistEntity(name = name, description = desc))
    }

    suspend fun deletePlaylist(playlistId: Long) {
        videoDao.deletePlaylist(playlistId)
        videoDao.deletePlaylistItems(playlistId)
    }

    fun getPlaylistItems(playlistId: Long): Flow<List<Video>> =
        videoDao.getPlaylistItems(playlistId).map { items ->
            items.map { it.toVideo() }.filter { it.isEmbeddable }
        }

    suspend fun addVideoToPlaylist(playlistId: Long, vid: Video) {
        videoDao.insertPlaylistItem(
            PlaylistItemEntity(
                playlistId = playlistId,
                videoId = vid.id,
                title = vid.title,
                thumbnail = vid.thumbnail,
                duration = vid.duration,
                channelId = vid.channelId,
                channelTitle = vid.channelTitle,
                channelAvatar = vid.channelAvatar,
                viewCount = vid.viewCount
            )
        )
    }

    suspend fun removeVideoFromPlaylist(playlistId: Long, videoId: String) {
        videoDao.deletePlaylistItem(playlistId, videoId)
    }

    fun getPlaylistItemCount(playlistId: Long): Flow<Int> = videoDao.getPlaylistItemCount(playlistId)

    // Subscriptions
    fun getSubscriptions(): Flow<List<Channel>> = videoDao.getSubscriptions().map { entities ->
        entities.map { entity ->
            Channel(
                id = entity.channelId,
                title = entity.channelTitle,
                avatar = entity.channelAvatar,
                banner = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=800&auto=format&fit=crop&q=80",
                subscriberCount = entity.subscriberCount,
                description = "",
                isSubscribed = true
            )
        }
    }

    suspend fun toggleSubscription(channel: Channel) {
        withContext(Dispatchers.IO) {
            val isSubNow = videoDao.isSubscribed(channel.id).first()
            if (isSubNow) {
                unsubscribe(channel.id)
            } else {
                subscribe(channel)
            }
        }
    }

    fun isSubscribed(channelId: String): Flow<Boolean> = videoDao.isSubscribed(channelId)

    suspend fun subscribe(channel: Channel) {
        videoDao.insertSubscription(
            SubscriptionEntity(
                channelId = channel.id,
                channelTitle = channel.title,
                channelAvatar = channel.avatar,
                subscriberCount = channel.subscriberCount
            )
        )
    }

    suspend fun unsubscribe(channelId: String) {
        videoDao.deleteSubscription(channelId)
    }

    // ISO 8601 Duration Parser converter (e.g. PT12M30S -> "12:30")
    private fun formatIsoDuration(isoDuration: String): String {
        try {
            if (isoDuration.isEmpty()) return "0:00"
            var clean = isoDuration.substringAfter("PT")
            var hours = 0
            var minutes = 0
            var seconds = 0

            if (clean.contains("H")) {
                hours = clean.substringBefore("H").toIntOrNull() ?: 0
                clean = clean.substringAfter("H")
            }
            if (clean.contains("M")) {
                minutes = clean.substringBefore("M").toIntOrNull() ?: 0
                clean = clean.substringAfter("M")
            }
            if (clean.contains("S")) {
                seconds = clean.substringBefore("S").toIntOrNull() ?: 0
            }

            return buildString {
                if (hours > 0) {
                    append(hours).append(":")
                    append(String.format("%02d", minutes)).append(":")
                } else {
                    append(minutes).append(":")
                }
                append(String.format("%02d", seconds))
            }
        } catch (_: Exception) {
            return "4:50"
        }
    }

    // Number formatter (e.g. 1500000 -> "1.5M")
    private fun formatBigNumber(num: Long): String {
        if (num < 1000) return num.toString()
        if (num < 1000000) {
            return String.format("%.1fk", num / 1000.0).replace(".0", "")
        }
        return String.format("%.1fM", num / 1000000.0).replace(".0", "")
    }

    // Relative Time calculation
    private fun timeAgo(isoDateString: String): String {
        // Fallback for demo relative strings
        return "3 hours ago"
    }
}

// 4. Entity converters
fun FavoriteVideoEntity.toVideo() = Video(
    id = id,
    title = title,
    description = description,
    thumbnail = thumbnail,
    duration = duration,
    channelId = channelId,
    channelTitle = channelTitle,
    channelAvatar = channelAvatar,
    viewCount = viewCount,
    likeCount = "0",
    publishedAt = "Favorite",
    category = "All"
)

fun WatchHistoryEntity.toVideo() = Video(
    id = videoId,
    title = title,
    description = "",
    thumbnail = thumbnail,
    duration = duration,
    channelId = channelId,
    channelTitle = channelTitle,
    channelAvatar = channelAvatar,
    viewCount = viewCount,
    likeCount = "0",
    publishedAt = "Recent",
    category = "All"
)

fun PlaylistItemEntity.toVideo() = Video(
    id = videoId,
    title = title,
    description = "",
    thumbnail = thumbnail,
    duration = duration,
    channelId = channelId,
    channelTitle = channelTitle,
    channelAvatar = channelAvatar,
    viewCount = viewCount,
    likeCount = "0",
    publishedAt = "Playlist Item",
    category = "All"
)
