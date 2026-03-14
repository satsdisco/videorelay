package com.videorelay.app.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.videorelay.app.ui.home.HomeScreen
import com.videorelay.app.ui.watch.WatchScreen
import com.videorelay.app.ui.shorts.ShortsScreen
import com.videorelay.app.ui.live.LiveScreen
import com.videorelay.app.ui.upload.UploadScreen
import com.videorelay.app.ui.channel.ChannelScreen
import com.videorelay.app.ui.search.SearchScreen
import com.videorelay.app.ui.downloads.DownloadsScreen
import com.videorelay.app.ui.settings.SettingsScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Shorts : Screen("shorts")
    object Upload : Screen("upload")
    object Live : Screen("live")
    object Downloads : Screen("downloads")
    object Watch : Screen("watch/{videoId}") {
        fun createRoute(videoId: String) = "watch/$videoId"
    }
    object Channel : Screen("channel/{pubkey}") {
        fun createRoute(pubkey: String) = "channel/$pubkey"
    }
    object Search : Screen("search?query={query}") {
        fun createRoute(query: String = "") = "search?query=$query"
    }
    object Settings : Screen("settings")
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(Screen.Shorts, "Shorts", Icons.Filled.PlayCircle, Icons.Outlined.PlayCircle),
    BottomNavItem(Screen.Upload, "Upload", Icons.Filled.AddCircle, Icons.Outlined.AddCircle),
    BottomNavItem(Screen.Live, "Live", Icons.Filled.Stream, Icons.Outlined.Stream),
    BottomNavItem(Screen.Downloads, "Saved", Icons.Filled.Download, Icons.Outlined.Download),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoRelayNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Show bottom bar on main screens only
    val showBottomBar = currentDestination?.route in bottomNavItems.map { it.screen.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    tonalElevation = NavigationBarDefaults.Elevation,
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == item.screen.route
                        } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label,
                                )
                            },
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onVideoClick = { videoId ->
                        navController.navigate(Screen.Watch.createRoute(videoId))
                    },
                    onSearchClick = {
                        navController.navigate(Screen.Search.createRoute())
                    },
                    onChannelClick = { pubkey ->
                        navController.navigate(Screen.Channel.createRoute(pubkey))
                    },
                    onSettingsClick = {
                        navController.navigate(Screen.Settings.route)
                    },
                )
            }

            composable(Screen.Shorts.route) {
                ShortsScreen(
                    onChannelClick = { pubkey ->
                        navController.navigate(Screen.Channel.createRoute(pubkey))
                    },
                )
            }

            composable(Screen.Upload.route) {
                UploadScreen(
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Screen.Live.route) {
                LiveScreen(
                    onStreamClick = { /* TODO: live player */ },
                )
            }

            composable(Screen.Downloads.route) {
                DownloadsScreen(
                    onVideoClick = { videoId ->
                        navController.navigate(Screen.Watch.createRoute(videoId))
                    },
                )
            }

            composable(
                route = Screen.Watch.route,
                arguments = listOf(navArgument("videoId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val videoId = backStackEntry.arguments?.getString("videoId") ?: return@composable
                WatchScreen(
                    videoId = videoId,
                    onBack = { navController.popBackStack() },
                    onChannelClick = { pubkey ->
                        navController.navigate(Screen.Channel.createRoute(pubkey))
                    },
                )
            }

            composable(
                route = Screen.Channel.route,
                arguments = listOf(navArgument("pubkey") { type = NavType.StringType }),
            ) { backStackEntry ->
                val pubkey = backStackEntry.arguments?.getString("pubkey") ?: return@composable
                ChannelScreen(
                    pubkey = pubkey,
                    onBack = { navController.popBackStack() },
                    onVideoClick = { videoId ->
                        navController.navigate(Screen.Watch.createRoute(videoId))
                    },
                )
            }

            composable(
                route = Screen.Search.route,
                arguments = listOf(navArgument("query") { defaultValue = "" }),
            ) {
                SearchScreen(
                    onBack = { navController.popBackStack() },
                    onVideoClick = { videoId ->
                        navController.navigate(Screen.Watch.createRoute(videoId))
                    },
                    onChannelClick = { pubkey ->
                        navController.navigate(Screen.Channel.createRoute(pubkey))
                    },
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
