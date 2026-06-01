package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CollapsiblePlayerSheet
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.Tab
import com.example.ui.viewmodel.VideoViewModel
import com.example.ui.viewmodel.UiState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument

class MainActivity : ComponentActivity() {
    private val viewModel: VideoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Programmatically initialize WebView HTTP cache & code cache directory structures 
        // to prevent Chromium simple_file_enumerator folder resolution error warnings
        try {
            val jsDir = java.io.File(cacheDir, "WebView/Default/HTTP Cache/Code Cache/js")
            val wasmDir = java.io.File(cacheDir, "WebView/Default/HTTP Cache/Code Cache/wasm")
            if (!jsDir.exists()) {
                jsDir.mkdirs()
                android.util.Log.d("ViewTubeInit", "Created WebView Code Cache JS dir: ${jsDir.absolutePath}")
            }
            if (!wasmDir.exists()) {
                wasmDir.mkdirs()
                android.util.Log.d("ViewTubeInit", "Created WebView Code Cache WASM dir: ${wasmDir.absolutePath}")
            }
        } catch (e: Exception) {
            android.util.Log.e("ViewTubeInit", "Failed to pre-create WebView caching subdirectories", e)
        }

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                ViewTubeMainApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ViewTubeMainApp(
    viewModel: VideoViewModel,
    modifier: Modifier = Modifier
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val playingVideo by viewModel.playingVideo.collectAsState()
    val isFullscreen by viewModel.isFullscreen.collectAsState()

    var isSearching by remember { mutableStateOf(false) }

    val navController = rememberNavController()

    Box(modifier = Modifier.size(0.dp)) {
        NavHost(
            navController = navController,
            startDestination = "dummy_launcher"
        ) {
            composable("dummy_launcher") {}
            composable("player/{videoId}") { backStackEntry ->
                val videoId = backStackEntry.arguments?.getString("videoId")
                LaunchedEffect(videoId) {
                    if (videoId != null) {
                        val targetVideo = viewModel.homeFeedState.value.let { state ->
                            if (state is UiState.Success) state.data.find { it.id == videoId } else null
                        }
                        if (targetVideo != null) {
                            viewModel.selectVideo(targetVideo)
                        }
                    }
                }
            }
        }
    }

    // Handle system back gesture
    if (playingVideo != null) {
        BackHandler {
            viewModel.closePlayer()
            try {
                if (navController.currentBackStackEntry?.destination?.route?.startsWith("player") == true) {
                    navController.popBackStack()
                }
            } catch (e: Exception) {
                android.util.Log.e("ViewTubeNav", "Failed to pop backstack", e)
            }
        }
    } else if (isSearching) {
        BackHandler {
            isSearching = false
            viewModel.clearSearch()
        }
    } else if (currentTab != Tab.HOME) {
        BackHandler {
            viewModel.setTab(Tab.HOME)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            // Guard: Hide topbar if fullscreen OR if in search mode
            if (!isFullscreen && !isSearching && (currentTab == Tab.HOME || currentTab == Tab.SUBSCRIPTIONS)) {
                ViewTubeTopAppBar(
                    onSearchClick = { isSearching = true },
                    onProfileClick = { viewModel.setTab(Tab.PROFILE) }
                )
            }
        },
        bottomBar = {
            // Guard: Hide navigation if fullscreen OR if playing video player expanded OR in search mode
            if (!isFullscreen && !isSearching) {
                ViewTubeBottomNavigation(
                    currentTab = currentTab,
                    onTabSelect = { viewModel.setTab(it) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (playingVideo != null || isSearching) 0.dp else innerPadding.calculateBottomPadding())
        ) {
            // Transition Content area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = if (!isFullscreen && !isSearching && (currentTab == Tab.HOME || currentTab == Tab.SUBSCRIPTIONS)) innerPadding.calculateTopPadding() else 0.dp)
            ) {
                if (isSearching) {
                    // Custom search overlay back binds to exit search mode
                    SearchScreen(
                        viewModel = viewModel,
                        onBackClick = { isSearching = false },
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(enabled = false) {}, // block click propagation
                        navController = navController
                    )
                    // Custom floating back arrow in search screen is bound internally
                    val searchQueryState by viewModel.searchQuery.collectAsState()
                    LaunchedEffect(key1 = searchQueryState) {
                        // Quick click handler binding
                    }
                    
                    // Bind Back Arrow from SearchScreen
                    DisposableEffect(Unit) {
                        onDispose {
                            // cleanup on exit
                        }
                    }
                } else {
                    when (currentTab) {
                        Tab.HOME -> {
                            HomeScreen(
                                viewModel = viewModel,
                                onSearchClick = { isSearching = true },
                                navController = navController
                            )
                        }
                        Tab.SHORTS -> {
                            ShortsScreen(
                                viewModel = viewModel
                            )
                        }
                        Tab.SUBSCRIPTIONS -> {
                            SubscriptionsScreen(
                                viewModel = viewModel
                            )
                        }
                        Tab.PROFILE -> {
                            ProfileScreen(
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }

            // Interactive Search mode back listener (closes search if user clears input)
            LaunchedEffect(isSearching) {
                if (!isSearching) {
                    viewModel.clearSearch()
                }
            }

            // Persistent Collapsible Player Overlay
            if (playingVideo != null) {
                CollapsiblePlayerSheet(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewTubeTopAppBar(
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.background
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Iconic Red TV Play Symbol branding
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.clickable { }
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.Red, shape = RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "ViewTube",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Action options Icons toolbar
            IconButton(onClick = { }) {
                Icon(
                    imageVector = Icons.Default.Cast,
                    contentDescription = "Cast stream",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            IconButton(onClick = { }) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray)
                    .clickable(onClick = onProfileClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Profile",
                    tint = Color.Gray,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun ViewTubeBottomNavigation(
    currentTab: Tab,
    onTabSelect: (Tab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .testTag("view_tube_navigation_bar"),
        tonalElevation = 8.dp,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        NavigationBarItem(
            selected = currentTab == Tab.HOME,
            onClick = { onTabSelect(Tab.HOME) },
            icon = {
                Icon(
                    imageVector = if (currentTab == Tab.HOME) Icons.Default.Home else Icons.Outlined.Home,
                    contentDescription = "Home"
                )
            },
            label = { Text("Home", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Red,
                selectedTextColor = Color.Red,
                indicatorColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )

        NavigationBarItem(
            selected = currentTab == Tab.SHORTS,
            onClick = { onTabSelect(Tab.SHORTS) },
            icon = {
                Icon(
                    imageVector = if (currentTab == Tab.SHORTS) Icons.Default.FlashOn else Icons.Outlined.FlashOn,
                    contentDescription = "Shorts"
                )
            },
            label = { Text("Shorts", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Red,
                selectedTextColor = Color.Red,
                indicatorColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )

        NavigationBarItem(
            selected = currentTab == Tab.SUBSCRIPTIONS,
            onClick = { onTabSelect(Tab.SUBSCRIPTIONS) },
            icon = {
                Icon(
                    imageVector = if (currentTab == Tab.SUBSCRIPTIONS) Icons.Default.Subscriptions else Icons.Outlined.Subscriptions,
                    contentDescription = "Subscriptions"
                )
            },
            label = { Text("Subscriptions", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Red,
                selectedTextColor = Color.Red,
                indicatorColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )

        NavigationBarItem(
            selected = currentTab == Tab.PROFILE,
            onClick = { onTabSelect(Tab.PROFILE) },
            icon = {
                Icon(
                    imageVector = if (currentTab == Tab.PROFILE) Icons.Default.VideoLibrary else Icons.Outlined.VideoLibrary,
                    contentDescription = "Profile"
                )
            },
            label = { Text("Library", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Red,
                selectedTextColor = Color.Red,
                indicatorColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}
