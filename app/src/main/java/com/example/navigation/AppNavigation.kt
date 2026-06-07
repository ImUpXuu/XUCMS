package com.example.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import com.example.talk.TalkListScreen
import java.net.URLDecoder
import java.net.URLEncoder

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object PostList : Screen("post_list")
    object PostEdit : Screen("post_edit/{filename}/{sha}") {
        fun createRoute(filename: String?, sha: String?) = "post_edit/${filename?.let { URLEncoder.encode(it, "UTF-8") } ?: "new"}/${sha ?: "empty"}"
    }
    object TalkList : Screen("talk_list")
    object TalkEdit : Screen("talk_edit/{filename}/{sha}") {
        fun createRoute(filename: String?, sha: String?) = "talk_edit/${filename?.let { URLEncoder.encode(it, "UTF-8") } ?: "new"}/${sha ?: "empty"}"
    }
    object Settings : Screen("settings")
}

@Composable
fun AppNavigation(tokenManager: TokenManager) {
    val navController = rememberNavController()
    val startDestination = if (tokenManager.hasToken()) Screen.PostList.route else Screen.Login.route

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomBarScreens = listOf(Screen.PostList, Screen.TalkList, Screen.Settings)
    val showBottomBar = bottomBarScreens.any { currentRoute?.startsWith(it.route) == true }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
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
                        selected = currentRoute == Screen.TalkList.route,
                        onClick = {
                            navController.navigate(Screen.TalkList.route) {
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
        }
    ) { padding ->
        NavHost(navController = navController, startDestination = startDestination, modifier = Modifier.padding(padding)) {
            composable(Screen.Login.route) {
                LoginScreen(tokenManager) {
                    navController.navigate(Screen.PostList.route) {
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
            composable(Screen.TalkList.route) {
                TalkListScreen(tokenManager, navController)
            }
            composable(Screen.TalkEdit.route) { backStackEntry ->
                val encodedFilename = backStackEntry.arguments?.getString("filename")
                val filename = if (encodedFilename == "new") null else encodedFilename?.let { URLDecoder.decode(it, "UTF-8") }
                val sha = backStackEntry.arguments?.getString("sha")?.takeIf { it != "empty" }
                TalkEditScreen(tokenManager, navController, filename, sha)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(tokenManager, navController)
            }
        }
    }
}
