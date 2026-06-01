package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.VideoRepository
import com.example.data.local.AppDatabase
import com.example.data.local.PlaylistEntity
import com.example.model.Channel
import com.example.model.Comment
import com.example.model.Video
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class VideoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: VideoRepository

    // Screen navigation tabs
    private val _currentTab = MutableStateFlow(Tab.HOME)
    val currentTab: StateFlow<Tab> = _currentTab.asStateFlow()

    // Home feed categories and state
    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _homeFeedState = MutableStateFlow<UiState<List<Video>>>(UiState.Loading)
    val homeFeedState: StateFlow<UiState<List<Video>>> = _homeFeedState.asStateFlow()

    // Playing Video
    private val _playingVideo = MutableStateFlow<Video?>(null)
    val playingVideo: StateFlow<Video?> = _playingVideo.asStateFlow()

    // Fullscreen Mode
    private val _isFullscreen = MutableStateFlow(false)
    val isFullscreen: StateFlow<Boolean> = _isFullscreen.asStateFlow()

    // Search query & results
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<UiState<List<Video>>>(UiState.Idle)
    val searchResults: StateFlow<UiState<List<Video>>> = _searchResults.asStateFlow()

    private val _searchHistory = MutableStateFlow<List<String>>(listOf("lofi hip hop", "coding relaxing beats", "marques brownlee tech", "hilbert hotel", "satisfying slow mo"))
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    // Active playing video stats & comments
    private val _activeComments = MutableStateFlow<List<Comment>>(emptyList())
    val activeComments: StateFlow<List<Comment>> = _activeComments.asStateFlow()

    private val _isPlayingFavorite = MutableStateFlow(false)
    val isPlayingFavorite: StateFlow<Boolean> = _isPlayingFavorite.asStateFlow()

    private val _isPlayingSubscribed = MutableStateFlow(false)
    val isPlayingSubscribed: StateFlow<Boolean> = _isPlayingSubscribed.asStateFlow()

    // User authentication profile
    private val _currentUser = MutableStateFlow<UserProfile?>(UserProfile("subojit_maity", "subojitmaity651@gmail.com", "Subojit Maity", "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150&auto=format&fit=crop&q=80"))
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    // Local DB Observables
    val favorites: StateFlow<List<Video>>
    val playlists: StateFlow<List<PlaylistEntity>>
    val watchHistory: StateFlow<List<Video>>
    val subscriptions: StateFlow<List<Channel>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = VideoRepository(database.videoDao())

        favorites = repository.getFavorites()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        playlists = repository.getPlaylists()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        watchHistory = repository.getWatchHistory()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        subscriptions = repository.getSubscriptions()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Initial loads
        loadHomeFeed()

        // Bind playing video to favorite state
        viewModelScope.launch {
            _playingVideo.collectLatest { video ->
                if (video != null) {
                    repository.isFavorite(video.id).collect { isFav ->
                        _isPlayingFavorite.value = isFav
                    }
                } else {
                    _isPlayingFavorite.value = false
                }
            }
        }

        // Bind playing video to subscribe state
        viewModelScope.launch {
            _playingVideo.collectLatest { video ->
                if (video != null) {
                    repository.isSubscribed(video.channelId).collect { isSub ->
                        _isPlayingSubscribed.value = isSub
                    }
                } else {
                    _isPlayingSubscribed.value = false
                }
            }
        }
    }

    // Changing tab
    fun setTab(tab: Tab) {
        _currentTab.value = tab
    }

    // Changing search category
    fun setCategory(category: String) {
        _selectedCategory.value = category
        loadHomeFeed()
    }

    // Fetching home feed
    fun loadHomeFeed() {
        viewModelScope.launch {
            _homeFeedState.value = UiState.Loading
            try {
                val videos = repository.getTrendingFeed(_selectedCategory.value)
                _homeFeedState.value = UiState.Success(videos)
            } catch (e: Exception) {
                _homeFeedState.value = UiState.Error(e.message ?: "Unknown error loading video feed")
            }
        }
    }

    // Select Video to Play
    fun selectVideo(video: Video) {
        android.util.Log.d("YouTubePlayback", "Selected Video ID: ${video.id}")
        _playingVideo.value = video
        // Adding to local Room watch history
        viewModelScope.launch {
            repository.addToHistory(video)
        }
        // Load comments for selected video
        loadVideoComments(video.id)
    }

    fun closePlayer() {
        _playingVideo.value = null
        _isFullscreen.value = false
    }

    fun toggleFullscreen() {
        _isFullscreen.value = !_isFullscreen.value
    }

    // Loading Comments
    private fun loadVideoComments(videoId: String) {
        viewModelScope.launch {
            val comments = repository.commentsForVideo(videoId)
            _activeComments.value = comments
        }
    }

    // Posting local user comment
    fun postComment(text: String) {
        val video = _playingVideo.value ?: return
        val user = _currentUser.value ?: return
        if (text.isBlank()) return
        viewModelScope.launch {
            val freshComment = repository.addComment(video.id, user.displayName, text)
            // Reload comments
            loadVideoComments(video.id)
        }
    }

    // Search Operations
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun executeSearch(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            _searchResults.value = UiState.Loading
            try {
                // Add to query suggestions list
                if (query.isNotBlank() && !_searchHistory.value.contains(query)) {
                    val currentHistory = _searchHistory.value.toMutableList()
                    currentHistory.add(0, query)
                    _searchHistory.value = currentHistory.take(10)
                }
                val results = repository.searchVideos(query)
                _searchResults.value = UiState.Success(results)
            } catch (e: Exception) {
                _searchResults.value = UiState.Error(e.message ?: "Failed searching YouTube API")
            }
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = UiState.Idle
    }

    // Room Favorites Operation
    fun toggleFavorite() {
        val video = _playingVideo.value ?: return
        viewModelScope.launch {
            val isFavNow = _isPlayingFavorite.value
            if (isFavNow) {
                repository.deleteFavorite(video.id)
            } else {
                repository.saveFavorite(video)
            }
        }
    }

    // Subscriptions logic
    fun toggleSubscriptionForActive() {
        val video = _playingVideo.value ?: return
        viewModelScope.launch {
            val channel = repository.getChannelById(video.channelId).copy(
                title = video.channelTitle,
                avatar = video.channelAvatar
            )
            val isSubNow = _isPlayingSubscribed.value
            if (isSubNow) {
                repository.unsubscribe(channel.id)
            } else {
                repository.subscribe(channel)
            }
        }
    }

    fun toggleSubscription(channel: Channel) {
        viewModelScope.launch {
            val isSub = repository.isSubscribed(channel.id).first()
            if (isSub) {
                repository.unsubscribe(channel.id)
            } else {
                repository.subscribe(channel)
            }
        }
    }

    // Playlists logic
    fun createPlaylist(name: String, desc: String) {
        viewModelScope.launch {
            repository.createPlaylist(name, desc)
        }
    }

    fun addVideoToPlaylist(playlistId: Long, video: Video) {
        viewModelScope.launch {
            repository.addVideoToPlaylist(playlistId, video)
        }
    }

    fun removeVideoFromPlaylist(playlistId: Long, videoId: String) {
        viewModelScope.launch {
            repository.removeVideoFromPlaylist(playlistId, videoId)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
        }
    }

    // Subscribing to dummy creators to populate Subscription tab
    fun populateDefaultSubscriptions() {
        viewModelScope.launch {
            repository.preseededChannels.forEach { channel ->
                repository.subscribe(channel)
            }
        }
    }

    // Auth flows
    fun logIn(username: String, email: String, name: String) {
        _currentUser.value = UserProfile(username, email, name, "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150&auto=format&fit=crop&q=80")
    }

    fun logOut() {
        _currentUser.value = null
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun isApiKeyConfigured() = repository.isApiKeyConfigured()
}

// Sealed state for API loading
sealed interface UiState<out T> {
    object Idle : UiState<Nothing>
    object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

// Screens tabs
enum class Tab {
    HOME, SHORTS, SUBSCRIPTIONS, PROFILE
}

// User Profile model
data class UserProfile(
    val username: String,
    val email: String,
    val displayName: String,
    val avatarUrl: String
)
