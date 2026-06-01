package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.ui.screens.CommentsListLayout
import com.example.ui.viewmodel.VideoViewModel

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CollapsiblePlayerSheet(
    viewModel: VideoViewModel,
    modifier: Modifier = Modifier
) {
    val playingVideo by viewModel.playingVideo.collectAsState()
    val isFullscreen by viewModel.isFullscreen.collectAsState()
    val isFavorite by viewModel.isPlayingFavorite.collectAsState()
    val isSubscribed by viewModel.isPlayingSubscribed.collectAsState()
    val activeComments by viewModel.activeComments.collectAsState()

    var isExpanded by remember { mutableStateOf(true) }
    var isDescExpanded by remember { mutableStateOf(false) }

    // Trigger expansion automatically when a new video is clicked
    LaunchedEffect(playingVideo) {
        if (playingVideo != null) {
            isExpanded = true
        }
    }

    if (playingVideo == null) return

    val video = playingVideo!!

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("active_player_root")
    ) {
        if (isExpanded) {
            // EXPANDED REGULAR OR FULLSCREEN VIEW
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .clickable(enabled = false) {} // block click propagation
            ) {
                // Top controls bar (only if not fullscreen)
                if (!isFullscreen) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { isExpanded = false }) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Minimize", modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "Now Playing",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.closePlayer() }) {
                            Icon(Icons.Default.Close, contentDescription = "Close player")
                        }
                    }
                }

                // Web Player
                val playerModifier = if (isFullscreen) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                }

                YouTubePlayerView(
                    videoId = video.id,
                    modifier = playerModifier,
                    isEmbeddable = video.isEmbeddable,
                    onFullscreenToggle = { viewModel.toggleFullscreen() }
                )

                // Detailed Scroll Content (only if not fullscreen!)
                if (!isFullscreen) {
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(scrollState)
                            .padding(16.dp)
                    ) {
                        // Title
                        Text(
                            text = video.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 24.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Views count
                        Text(
                            text = "${video.viewCount}  •  ${video.publishedAt}",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action Buttons Bar (Like, Share, Save)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Favorite toggle (Thumb Up style)
                            Button(
                                onClick = { viewModel.toggleFavorite() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isFavorite) Color.Red else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "Favorite",
                                        tint = if (isFavorite) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = if (isFavorite) "Liked" else "Like",
                                        fontSize = 13.sp,
                                        color = if (isFavorite) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Share Action
                            Button(
                                onClick = { /* Share interaction */ },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                    Text("Share", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Fullscreen toggle manual button
                            IconButton(
                                onClick = { viewModel.toggleFullscreen() },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape)
                                    .size(36.dp)
                            ) {
                                Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Channel publisher info
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), shape = RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(video.channelAvatar)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = video.channelTitle,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(video.channelTitle, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("14.3M subscribers", fontSize = 11.sp, color = Color.Gray)
                            }

                            Button(
                                onClick = { viewModel.toggleSubscriptionForActive() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSubscribed) Color.DarkGray else Color.Red
                                ),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (isSubscribed) "Subscribed" else "Subscribe",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Expandable Description fold
                        Card(
                            onClick = { isDescExpanded = !isDescExpanded },
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Description", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Icon(
                                        imageVector = if (isDescExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = video.description.ifEmpty { "Enjoy high quality streaming content on ViewTube platform." },
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = if (isDescExpanded) 20 else 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Interactive comment panel
                        Text("Comments (${activeComments.size})", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                        ) {
                            CommentsListLayout(
                                comments = activeComments,
                                onPostComment = { text -> viewModel.postComment(text) }
                            )
                        }
                    }
                }
            }
        } else {
            // MINIMIZED FLOATING HORIZONTAL MINI BAR
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 80.dp) // Sits above horizontal bottom navigation bar
            ) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 8.dp)
                        .testTag("mini_player_bar")
                        .clickable { isExpanded = true },
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Tiny cropped video thumbnail
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(video.thumbnail)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier
                                .width(110.dp)
                                .fillMaxHeight(),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        // Title text details
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = video.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = video.channelTitle,
                                fontSize = 11.sp,
                                color = Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Play indicator & Clear icon
                        IconButton(onClick = { isExpanded = true }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Simulate Play", tint = Color.Red)
                        }

                        IconButton(onClick = { viewModel.closePlayer() }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}
