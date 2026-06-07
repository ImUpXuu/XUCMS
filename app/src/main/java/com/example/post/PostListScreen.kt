package com.example.post

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.data.Api
import com.example.data.PostItem
import com.example.data.TokenManager
import com.example.navigation.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostListScreen(tokenManager: TokenManager, navController: NavController) {
    var posts by remember { mutableStateOf<List<PostItem>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<PostItem?>(null) }
    var isTimelineView by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val token = tokenManager.getToken() ?: ""

    fun loadData() {
        if (!tokenManager.hasToken()) {
            navController.navigate(Screen.Login.route) { popUpTo(0) }
            return
        }
        scope.launch {
            isLoading = true
            error = null
            val res = Api.getPosts(token)
            isLoading = false
            if (res.isSuccess) {
                posts = res.getOrNull() ?: emptyList()
            } else {
                val e = res.exceptionOrNull()
                if (e is Api.ApiException && e.code == 401) {
                    tokenManager.clearToken()
                    navController.navigate(Screen.Login.route) { popUpTo(0) }
                } else {
                    error = e?.message ?: "获取失败"
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    val filteredPosts = posts
        .filter { it.title?.contains(searchQuery, ignoreCase = true) ?: it.name.contains(searchQuery, ignoreCase = true) }
        .sortedByDescending { it.date ?: it.name }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("文章管理") },
                    actions = {
                        IconButton(onClick = { isTimelineView = !isTimelineView }) {
                            Icon(if (isTimelineView) Icons.Default.ViewDay else Icons.Default.List, contentDescription = "切换视图")
                        }
                        IconButton(onClick = { loadData() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "刷新")
                        }
                        // Hide internal add button since we have a global app-level FAB
                        // Actually, just keep it, doesn't hurt.
                    }
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("搜索文章...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
        ) {
            if (isLoading && posts.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (error != null) {
                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                    Button(onClick = { loadData() }) { Text("重试") }
                }
            } else if (filteredPosts.isEmpty() && !isLoading) {
                Text("还没有文章", color = Color.Gray, modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                    if (isTimelineView) {
                        items(filteredPosts) { post ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min)
                                    .clickable { navController.navigate(Screen.PostEdit.createRoute(post.name, post.sha)) }
                            ) {
                                Column(
                                    modifier = Modifier.width(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(modifier = Modifier.size(12.dp).background(MaterialTheme.colorScheme.primary, CircleShape).padding(vertical = 12.dp))
                                    Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(Color.LightGray))
                                }
                                Column(modifier = Modifier.padding(bottom = 16.dp).weight(1f)) {
                                    Text(post.date ?: "-", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 4.dp))
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                    ) {
                                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text(post.title ?: post.name, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                            IconButton(onClick = { navController.navigate(Screen.PostEdit.createRoute(post.name, post.sha)) }) {
                                                Icon(Icons.Default.Edit, contentDescription = "编辑", tint = Color.Gray)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        items(filteredPosts) { post ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                                    .clickable { navController.navigate(Screen.PostEdit.createRoute(post.name, post.sha)) },
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(post.title ?: post.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(post.date ?: "-", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = { navController.navigate(Screen.PostEdit.createRoute(post.name, post.sha)) }) {
                                        Icon(Icons.Default.Edit, contentDescription = "编辑", tint = Color.Gray)
                                    }
                                    IconButton(onClick = { showDeleteConfirm = post }) {
                                        Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showDeleteConfirm != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = null },
                title = { Text("确认删除") },
                text = { Text("要删除文章 ${showDeleteConfirm?.title ?: showDeleteConfirm?.name} 吗？") },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            val target = showDeleteConfirm!!
                            showDeleteConfirm = null
                            val res = Api.deletePost(token, target.name, target.sha)
                            if (res.isSuccess) {
                                loadData()
                            } else {
                                val e = res.exceptionOrNull()
                                if (e is Api.ApiException && e.code == 401) {
                                    tokenManager.clearToken()
                                    navController.navigate(Screen.Login.route) { popUpTo(0) }
                                }
                            }
                        }
                    }) { Text("删除", color = Color.Red) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = null }) { Text("取消") }
                }
            )
        }
    }
}
