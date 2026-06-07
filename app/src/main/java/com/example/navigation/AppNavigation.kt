package com.example.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.auth.LoginScreen
import com.example.data.TokenManager
import com.example.post.PostEditScreen
import com.example.post.PostListScreen
import com.example.settings.SettingsScreen
import com.example.talk.TalkEditScreen
import com.example.gallery.GalleryScreen
import java.net.URLDecoder
import java.net.URLEncoder

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object PostList : Screen("post_list")
    object PostEdit : Screen("post_edit/{filename}/{sha}") {
        fun createRoute(filename: String?, sha: String?) = "post_edit/${filename?.let { URLEncoder.encode(it, "UTF-8") } ?: "new"}/${sha ?: "empty"}"
    }
    object TalkEdit : Screen("talk_edit/{filename}/{sha}") {
        fun createRoute(filename: String?, sha: String?) = "talk_edit/${filename?.let { URLEncoder.encode(it, "UTF-8") } ?: "new"}/${sha ?: "empty"}"
    }
    object Gallery : Screen("gallery")
    object Settings : Screen("settings")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(tokenManager: TokenManager) {
    val navController = rememberNavController()
    val startDestination = if (tokenManager.hasToken()) Screen.TalkEdit.createRoute(null, null) else Screen.Login.route

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isLogin = currentRoute == Screen.Login.route
    val isPostEdit = currentRoute?.startsWith("post_edit") == true
    val isTalkEdit = currentRoute?.startsWith("talk_edit") == true

    val showBottomBar = !isLogin
    val showFab = !isLogin && !isPostEdit && !isTalkEdit

    var showFabMenu by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Article, contentDescription = "文章") },
                        label = { Text("文章") },
                        selected = currentRoute == Screen.PostList.route,
                        onClick = {
                            navController.navigate(Screen.PostList.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Chat, contentDescription = "说说") },
                        label = { Text("说说") },
                        selected = isTalkEdit,
                        onClick = {
                            navController.navigate(Screen.TalkEdit.createRoute(null, null)) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = "图库") },
                        label = { Text("图库") },
                        selected = currentRoute == Screen.Gallery.route,
                        onClick = {
                            navController.navigate(Screen.Gallery.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = "设置") },
                        label = { Text("设置") },
                        selected = currentRoute == Screen.Settings.route,
                        onClick = {
                            navController.navigate(Screen.Settings.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            if (showFab) {
                FloatingActionButton(
                    onClick = { showFabMenu = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New")
                }
            }
        }
    ) { padding ->
        if (showFabMenu) {
            ModalBottomSheet(onDismissRequest = { showFabMenu = false }) {
                Column(modifier = Modifier.padding(16.dp).padding(bottom = 32.dp)) {
                    Text("新建", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))
                    ListItem(
                        headlineContent = { Text("📝 新建文章") },
                        modifier = Modifier.clickable {
                            showFabMenu = false
                            navController.navigate(Screen.PostEdit.createRoute(null, null))
                        }
                    )
                    ListItem(
                        headlineContent = { Text("💬 新建说说") },
                        modifier = Modifier.clickable {
                            showFabMenu = false
                            navController.navigate(Screen.TalkEdit.createRoute(null, null))
                        }
                    )
                }
            }
        }

        NavHost(navController = navController, startDestination = startDestination, modifier = Modifier.padding(padding)) {
            composable(Screen.Login.route) {
                LoginScreen(tokenManager) {
                    navController.navigate(Screen.TalkEdit.createRoute(null, null)) {
                        popUpTo(0)
                    }
                }
            }
            composable(Screen.PostList.route) {
                PostListScreen(tokenManager, navController)
            }
            composable(Screen.PostEdit.route) { backStackEntry ->
                val encodedFilename = backStackEntry.arguments?.getString("filename")
                val filename = if (encodedFilename == "new") null else encodedFilename?.let { URLDecoder.decode(it, "UTF-8") }
                val sha = backStackEntry.arguments?.getString("sha")?.takeIf { it != "empty" }
                PostEditScreen(tokenManager, navController, filename, sha)
            }
            composable(Screen.TalkEdit.route) { backStackEntry ->
                val encodedFilename = backStackEntry.arguments?.getString("filename")
                val filename = if (encodedFilename == "new") null else encodedFilename?.let { URLDecoder.decode(it, "UTF-8") }
                val sha = backStackEntry.arguments?.getString("sha")?.takeIf { it != "empty" }
                TalkEditScreen(tokenManager, navController, filename, sha)
            }
            composable(Screen.Gallery.route) {
                GalleryScreen(tokenManager)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(tokenManager, navController)
            }
        }
    }
}
