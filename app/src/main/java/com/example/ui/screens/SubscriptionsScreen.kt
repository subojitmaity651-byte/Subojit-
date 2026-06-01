package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Subscriptions
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.Channel
import com.example.model.Video
import com.example.ui.viewmodel.VideoViewModel

@Composable
fun SubscriptionsScreen(
    viewModel: VideoViewModel,
    modifier: Modifier = Modifier
) {
    val subscriptions by viewModel.subscriptions.collectAsState()
    val homeVideosState by viewModel.homeFeedState.collectAsState()

    var filteredChannelId by remember { mutableStateOf<String?>(null) }

    // Popular creators suggestions to help users discover content
    val popularCreators = remember {
        listOf(
            Channel("UCAL3JXZSzUN8vPL_UC_86Sg", "Lofi Girl", "https://images.unsplash.com/photo-1518495973542-4542c06a5843?w=150&q=80", "", "14.3M", "Lofi Girl official"),
            Channel("UCBJycsmduvYELgT-j3yF6eg", "Marques Brownlee", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&q=80", "", "18.7M", "MKBHD"),
            Channel("UCsXVk37bltUxv1m7K6c023g", "Kurzgesagt – In a Nutshell", "https://images.unsplash.com/photo-1627515151676-e17ee8ce6015?w=150&q=80", "", "22.4M", "In a nutshel"),
            Channel("UCHnyfMqiRRG1u-2MsSQLbXA", "Veritasium", "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150&q=80", "", "15.1M", "Veritasium")
        )
    }

    val videos = remember(homeVideosState, filteredChannelId) {
        homeVideosState.let { state ->
            if (state is com.example.ui.viewmodel.UiState.Success) {
                val list = state.data.filter { !it.isShort }
                if (filteredChannelId == null) list else list.filter { it.channelId == filteredChannelId }
            } else {
                emptyList()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (subscriptions.isEmpty()) {
            // Empty State & Suggested Subscriptions row
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Subscriptions,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Don't miss new videos",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Subscribe to your favorite creators to see their latest uploads right here. Start by subscribing to our popular recommendations:",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(30.dp))

                // Popular suggestions grid horizontal
                Text(
                    text = "Recommended Creators",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(popularCreators) { channel ->
                        SuggestedCreatorCard(
                            channel = channel,
                            onSubClick = { viewModel.toggleSubscription(channel) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.populateDefaultSubscriptions() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Auto-Subscribe to All Creators", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            // Subscriptions list horizontal circles on top
            Column(modifier = Modifier.fillMaxSize()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // All channel filter option
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { filteredChannelId = null }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(if (filteredChannelId == null) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "All",
                                    color = if (filteredChannelId == null) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "All Stream",
                                fontSize = 11.sp,
                                maxLines = 1,
                                color = if (filteredChannelId == null) MaterialTheme.colorScheme.onBackground else Color.Gray,
                                fontWeight = if (filteredChannelId == null) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    items(subscriptions) { channel ->
                        val isSelected = filteredChannelId == channel.id
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { filteredChannelId = channel.id }
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(channel.avatar)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = channel.title,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(Color.LightGray)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = channel.title.substringBefore(" ").take(8),
                                fontSize = 11.sp,
                                maxLines = 1,
                                color = if (isSelected) MaterialTheme.colorScheme.onBackground else Color.Gray,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Video upload list
                if (videos.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No recent video uploads config from this channel.", color = Color.Gray, fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().testTag("sub_video_list"),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(videos) { video ->
                            VideoFeedCard(
                                video = video,
                                onClick = { viewModel.selectVideo(video) },
                                onMoreClick = {}
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SuggestedCreatorCard(
    channel: Channel,
    onSubClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(130.dp)
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(channel.avatar)
                    .crossfade(true)
                    .build(),
                contentDescription = channel.title,
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = channel.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Text(
                text = "${channel.subscriberCount} subs",
                fontSize = 10.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onSubClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(30.dp),
                shape = RoundedCornerShape(15.dp)
            ) {
                Text("Subscribe", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
