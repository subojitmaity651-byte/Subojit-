package com.example.ui.components

import android.util.Log
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebSettings
import android.webkit.WebChromeClient
import android.webkit.ConsoleMessage
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.net.Uri

/**
 * Configure WebView child views inside parent view hierarchy
 * to resolve Error 153 (Video Player Configuration Error) by correcting
 * missing/null origin issues and user agent blocks.
 */
private fun configureWebViewSettings(view: ViewGroup) {
    for (i in 0 until view.childCount) {
        val child = view.getChildAt(i)
        if (child is WebView) {
            try {
                // Pre-create the Chromium cache folders and use scheduled delays
                // to make sure they aren't deleted/missed during internal Chromium setup
                val context = child.context
                val createCacheDirs = Runnable {
                    try {
                        val cDir = context.cacheDir
                        val jsDir = java.io.File(cDir, "WebView/Default/HTTP Cache/Code Cache/js")
                        val wasmDir = java.io.File(cDir, "WebView/Default/HTTP Cache/Code Cache/wasm")
                        if (!jsDir.exists()) {
                            jsDir.mkdirs()
                            Log.d("YouTubePlayerView", "Created/Verified Code Cache JS dir: ${jsDir.absolutePath}")
                        }
                        if (!wasmDir.exists()) {
                            wasmDir.mkdirs()
                            Log.d("YouTubePlayerView", "Created/Verified Code Cache WASM dir: ${wasmDir.absolutePath}")
                        }
                    } catch (e: Exception) {
                        Log.e("YouTubePlayerView", "Failed to verify WebView directories within delayed task", e)
                    }
                }

                // Execute immediately & schedule delays to defeat any race conditions
                createCacheDirs.run()
                child.postDelayed(createCacheDirs, 100)
                child.postDelayed(createCacheDirs, 500)
                child.postDelayed(createCacheDirs, 1000)
                child.postDelayed(createCacheDirs, 2500)
                child.postDelayed(createCacheDirs, 5000)

                child.webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                        Log.e("YouTubeWebView", "${message.message()} at ${message.sourceId()}:${message.lineNumber()}")
                        return true
                    }
                }

                child.settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    
                    // Allow mixed content if necessary for compatibility
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    
                    // Set modern mobile Chrome user agent to bypass embed blocks
                    userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Mobile Safari/537.36"
                    Log.d("YouTubePlayerView", "Configured custom modern user agent: $userAgentString")
                }
            } catch (e: Exception) {
                Log.e("YouTubePlayerView", "Error configuring WebView settings: ${e.message}", e)
            }
        } else if (child is ViewGroup) {
            configureWebViewSettings(child)
        }
    }
}

/**
 * Robustly extracts the 11-char YouTube Video ID from standard video IDs, short links or long URLs.
 */
fun extractVideoId(input: String): String {
    val trimmed = input.trim()
    if (trimmed.length == 11 && trimmed.matches(Regex("[a-zA-Z0-9_-]{11}"))) {
        return trimmed
    }
    
    // Check known YouTube URL structures
    val urlRegexes = listOf(
        Regex("(?i)v=([a-zA-Z0-9_-]{11})"),
        Regex("(?i)youtu\\.be/([a-zA-Z0-9_-]{11})"),
        Regex("(?i)youtube\\.com/embed/([a-zA-Z0-9_-]{11})"),
        Regex("(?i)youtube\\.com/v/([a-zA-Z0-9_-]{11})"),
        Regex("(?i)shorts/([a-zA-Z0-9_-]{11})")
    )
    
    for (regex in urlRegexes) {
        val match = regex.find(trimmed)
        if (match != null && match.groupValues.size > 1) {
            val matchedId = match.groupValues[1]
            if (matchedId.length == 11) {
                return matchedId
            }
        }
    }
    return trimmed
}

/**
 * Validates whether the given YouTube video ID has a valid format to prevent passing malformed queries.
 */
fun isValidYouTubeVideoId(id: String): Boolean {
    val cleanId = extractVideoId(id)
    return cleanId.isNotEmpty() && cleanId.length == 11 && cleanId.matches(Regex("[a-zA-Z0-9_-]{11}"))
}

/**
 * Helper to launch an intent redirecting to the official YouTube app, or browser fallback.
 */
fun openVideoInYouTube(context: android.content.Context, videoId: String) {
    val cleanId = extractVideoId(videoId)
    try {
        val youtubeAppUri = Uri.parse("vnd.youtube:$cleanId")
        val intent = Intent(Intent.ACTION_VIEW, youtubeAppUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        Log.d("YouTubePlayerView", "Direct redirect: Successfully opened YouTube app for: $cleanId")
    } catch (e: Exception) {
        Log.w("YouTubePlayerView", "YouTube app intent failed: ${e.message}. Launching browser fallback.")
        try {
            val browserUri = Uri.parse("https://www.youtube.com/watch?v=$cleanId")
            val webIntent = Intent(Intent.ACTION_VIEW, browserUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        } catch (ex: Exception) {
            Log.e("YouTubePlayerView", "Browser launch failed: ${ex.message}")
        }
    }
}

@Composable
fun YouTubePlayerView(
    videoId: String,
    modifier: Modifier = Modifier,
    isEmbeddable: Boolean = true,
    onFullscreenToggle: (() -> Unit)? = null
) {
    Log.d("YouTubePlayback", "Video ID = $videoId")
    Log.d("YouTubePlayback", "Embeddable = $isEmbeddable")

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val cleanId = remember(videoId) { extractVideoId(videoId) }
    
    var playerInstance by remember { mutableStateOf<YouTubePlayer?>(null) }
    var loadedVideoId by remember { mutableStateOf<String?>(null) }
    var showOpenInYouTubeButton by remember(cleanId, isEmbeddable) {
        mutableStateOf(!isEmbeddable)
    }
    var showGenericError by remember(cleanId) {
        mutableStateOf(false)
    }
    var loadingError by remember(cleanId, isEmbeddable) {
        mutableStateOf(if (isEmbeddable) null else "This video cannot be embedded. The publisher has restricted playback outside of YouTube.")
    }
    
    // Add real-time logs for Video ID and its Embeddable/Restricted status
    LaunchedEffect(cleanId, isEmbeddable) {
        Log.i("YouTubePlaybackLog", "--------------------------------------------------")
        Log.i("YouTubePlaybackLog", "Video ID: '$cleanId'")
        Log.i("YouTubePlaybackLog", "Embeddable status (Database/API): '$isEmbeddable'")
        Log.i("YouTubePlaybackLog", "--------------------------------------------------")
    }
    
    // Create player view and manage lifecycle cleanly
    val playerView = remember {
        YouTubePlayerView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            
            // Apply WebView bypass settings prior to/post initial load
            configureWebViewSettings(this)
            post {
                configureWebViewSettings(this)
            }
            
            // Explicit manual initialization to override default player options causing Error 153
            enableAutomaticInitialization = false
            
            val options = IFramePlayerOptions.Builder().build()
                
            initialize(object : AbstractYouTubePlayerListener() {
                override fun onReady(youTubePlayer: YouTubePlayer) {
                    Log.d("YouTubePlayback", "Player ready.")
                    playerInstance = youTubePlayer
                }
                
                override fun onError(
                    youTubePlayer: YouTubePlayer,
                    error: PlayerConstants.PlayerError
                ) {
                    Log.e("YouTubePlayback", "Error occurred: ${error.name}")
                    if (error == PlayerConstants.PlayerError.VIDEO_NOT_PLAYABLE_IN_EMBEDDED_PLAYER) {
                        android.widget.Toast.makeText(
                            context,
                            "This video cannot be played in this app. Please watch it on YouTube.",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                        showOpenInYouTubeButton = true
                        loadingError = "This video cannot be embedded. The publisher has restricted playback outside of YouTube."
                    } else {
                        showGenericError = true
                        loadingError = when (error) {
                            PlayerConstants.PlayerError.UNKNOWN -> "An unknown error occurred."
                            PlayerConstants.PlayerError.INVALID_PARAMETER_IN_REQUEST -> "Invalid parameter in request."
                            PlayerConstants.PlayerError.HTML_5_PLAYER -> "HTML5 player error."
                            PlayerConstants.PlayerError.VIDEO_NOT_FOUND -> "The requested video was not found."
                            else -> "This video is currently unavailable."
                        }
                    }
                }

                override fun onApiChange(youTubePlayer: YouTubePlayer) {
                    Log.d("YouTubePlayback", "API Changed")
                }
            }, options)
        }
    }

    // Handle videoId update dynamically
    LaunchedEffect(cleanId, playerInstance, isEmbeddable) {
        val player = playerInstance
        if (player != null) {
            if (!isEmbeddable) {
                loadingError = "This video cannot be embedded. The publisher has restricted playback outside of YouTube."
                showOpenInYouTubeButton = true
                try {
                    player.pause() // Prevent audio leak in background fallback
                } catch (e: Exception) {
                    Log.w("YouTubePlayerView", "Failed to pause on non-embeddable: ${e.message}")
                }
                Log.e("YouTubePlaybackLog", "Playback result for ID '$cleanId': BLOCKED (Flagged non-embeddable)")
            } else if (isValidYouTubeVideoId(cleanId)) {
                loadingError = null
                showOpenInYouTubeButton = false
                showGenericError = false
                if (loadedVideoId != cleanId) {
                    Log.d("YouTubePlayback", "Loading video. ID=$videoId")
                    player.loadVideo(cleanId, 0f)
                    loadedVideoId = cleanId
                    Log.d("YouTubePlaybackLog", "Playback result for ID '$cleanId': SUCCESS")
                }
            } else {
                loadingError = "Malformed or empty video ID"
                showGenericError = true
                try {
                    player.pause() // Prevent audio leak in background error
                } catch (e: Exception) {
                    Log.w("YouTubePlayerView", "Failed to pause on malformed ID: ${e.message}")
                }
                Log.e("YouTubePlaybackLog", "Playback result for ID '$cleanId': ERROR (Malformed Video ID)")
            }
        }
    }

    DisposableEffect(playerView) {
        lifecycleOwner.lifecycle.addObserver(playerView)
        onDispose {
            try {
                lifecycleOwner.lifecycle.removeObserver(playerView)
            } catch (e: Exception) {
                Log.w("YouTubePlayerView", "Failed to remove life cycle observer: ${e.message}")
            }
            
            try {
                playerView.clearFocus()
            } catch (e: Exception) {
                Log.w("YouTubePlayerView", "Failed to clear focus: ${e.message}")
            }
            
            val parentView = playerView.parent as? ViewGroup
            try {
                parentView?.removeView(playerView)
            } catch (e: Exception) {
                Log.w("YouTubePlayerView", "Failed to remove playerView from parent: ${e.message}")
            }
            
            // Post player release with a 300ms delay to the Main Looper Handler.
            // This prevents "unrecoverably broken" InputDispatcher channel crashes by allowing ongoing drag gestures 
            // and input events to finish dispatching before destroying native WebView resources.
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    playerView.release()
                } catch (e: Exception) {
                    Log.w("YouTubePlayerView", "Failed to release delayed: ${e.message}")
                }
            }, 300)
        }
    }

    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Hosting WebView consistently inside AndroidView, not conditionally removing it
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { playerView }
        )

        if (showOpenInYouTubeButton || showGenericError || loadingError != null) {
            // Elegant fallback container overlay when embed fails
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = if (showOpenInYouTubeButton) "Video Content Restricted" else "Playback Error",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = loadingError ?: "This video is unavailable to play inside this application.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Try reload button
                    OutlinedButton(
                        onClick = {
                            loadingError = null 
                            showGenericError = false
                            showOpenInYouTubeButton = !isEmbeddable
                            loadedVideoId = null // This resets state to trigger the LaunchedEffect or allow correct reloading state
                            val player = playerInstance
                            if (player != null && isValidYouTubeVideoId(cleanId)) {
                                player.loadVideo(cleanId, 0f)
                                loadedVideoId = cleanId
                            }
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reload")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Retry")
                    }
                    
                    if (showOpenInYouTubeButton) {
                        // Direct YouTube launch button
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val fallbackIntent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://www.youtube.com/watch?v=$videoId")
                                    )
                                    context.startActivity(fallbackIntent)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Open in YouTube")
                        }
                    }
                }
            }
        }
    }
}

