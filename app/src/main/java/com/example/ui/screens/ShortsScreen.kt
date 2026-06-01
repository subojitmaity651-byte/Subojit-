package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.Video
import com.example.ui.components.YouTubePlayerView
import com.example.ui.viewmodel.VideoViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShortsScreen(
    viewModel: VideoViewModel,
    modifier: Modifier = Modifier
) {
    val shortsFeed by remember { mutableStateOf(emptyList<Video>()) }
    var loadedShortsState by remember { mutableStateOf<List<Video>>(emptyList()) }

    LaunchedEffect(Unit) {
        // Collect shorts list from ViewModel repository
        loadedShortsState = viewModel.homeFeedState.value.let { state ->
            if (state is com.example.ui.viewmodel.UiState.Success) {
                state.data.filter { it.isShort }
            } else {
                emptyList()
            }
        }.ifEmpty {
            // Static preseed backup if list empty or loading
            listOf(
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
                    description = "Capturing a satisfying double shot espresso pull using a bottomless portafilter.",
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
                    description = "Enjoy the pleasing clicks of linear switches.",
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
                    title = "Views from the Swiss Alps Express 🏔️ #shorts #travel",
                    description = "Breathtaking snowy slopes in Switzerland panoramic train.",
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
        }
    }

    if (loadedShortsState.isEmpty()) {
        Box(modifier = modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.Red)
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { loadedShortsState.size })

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("shorts_pager")
    ) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val shortVideo = loadedShortsState[page]
            ShortPageItem(
                video = shortVideo,
                viewModel = viewModel,
                isActive = pagerState.currentPage == page
            )
        }
    }
}

@Composable
fun ShortPageItem(
    video: Video,
    viewModel: VideoViewModel,
    isActive: Boolean
) {
    var isLiked by remember { mutableStateOf(false) }
    var isDisliked by remember { mutableStateOf(false) }
    var isSubscribed by remember { mutableStateOf(false) }
    var showCommentsSheet by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Embed Player in full screen background (if page is active, load video!)
        if (isActive) {
            YouTubePlayerView(
                videoId = video.id,
                modifier = Modifier.fillMaxSize(),
                isEmbeddable = video.isEmbeddable
            )
        } else {
            // Fallback preview while scrolling
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(video.thumbnail)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Overlay with gradient bottom to ensure title/controls are readable
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.4f),
                            Color.Black.copy(alpha = 0.8f)
                        ),
                        startY = 400f
                    )
                )
        )

        // Details details overlay at the bottom left
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, end = 80.dp, bottom = 90.dp)
        ) {
            // Channel Avatar & Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(video.channelAvatar)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                )
                Text(
                    text = "@${video.channelTitle.replace(" ", "").lowercase()}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                // Sub action
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSubscribed) Color.DarkGray else Color.White)
                        .clickable { isSubscribed = !isSubscribed }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isSubscribed) "Subscribed" else "Subscribe",
                        color = if (isSubscribed) Color.White else Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = video.title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Audio indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                Text(
                    text = "Original Audio - ${video.channelTitle}",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Action Toolbar overlay floating on the bottom right margin
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 90.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Like
            IconButtonWithText(
                icon = Icons.Default.ThumbUp,
                text = if (isLiked) "Active" else video.likeCount,
                tint = if (isLiked) Color.Red else Color.White,
                onClick = {
                    isLiked = !isLiked
                    if (isLiked) isDisliked = false
                }
            )

            // Dislike
            IconButtonWithText(
                icon = Icons.Default.ThumbDown,
                text = "Dislike",
                tint = if (isDisliked) Color.Red else Color.White,
                onClick = {
                    isDisliked = !isDisliked
                    if (isDisliked) isLiked = false
                }
            )

            // Comments button
            IconButtonWithText(
                icon = Icons.Default.Comment,
                text = "Comments",
                tint = Color.White,
                onClick = { showCommentsSheet = true }
            )

            // Playlist save
            IconButtonWithText(
                icon = Icons.Default.PlaylistAdd,
                text = "Save",
                tint = Color.White,
                onClick = {
                    // Quick add to favorites
                    viewModel.selectVideo(video)
                    viewModel.toggleFavorite()
                }
            )

            // Share
            IconButtonWithText(
                icon = Icons.Default.Share,
                text = "Share",
                tint = Color.White,
                onClick = { /* Share dialog */ }
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Music Disc Spinning animation icon representation
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.DarkGray)
                    .padding(3.dp)
                    .clip(CircleShape)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(video.channelAvatar)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }

    // Modal Bottom Sheet or Simple Dialog for local Comments preview
    if (showCommentsSheet) {
        AlertDialog(
            onDismissRequest = { showCommentsSheet = false },
            title = { Text("Shorts Comments", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().height(280.dp)) {
                    CommentsListLayout(
                        comments = listOf(
                            com.example.model.Comment("c1", video.id, "Sarah Miller", "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150&q=80", "This short is so well produced! ASMR keyboard clicks are highly satisfying 💖", "1d ago"),
                            com.example.model.Comment("c2", video.id, "Dave Peterson", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&q=80", "What switches are you using on that keyboard?", "12h ago")
                        ),
                        onPostComment = { /* Quick comment */ }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showCommentsSheet = false }) {
                    Text("Done", color = Color.Red)
                }
            }
        )
    }
}

@Composable
fun IconButtonWithText(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Color.Black.copy(alpha = 0.4f), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = text,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
