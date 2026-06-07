package com.example.talk

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
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
import com.example.data.TalkItem
import com.example.data.TokenManager
import com.example.navigation.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TalkListScreen(tokenManager: TokenManager, navController: NavController) {
    var talks by remember { mutableStateOf<List<TalkItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var showDeleteConfirm by remember { mutableStateOf<TalkItem?>(null) }
    val token = tokenManager.getToken() ?: ""

    fun loadData() {
        if (!tokenManager.hasToken()) {
            navController.navigate(Screen.Login.route) { popUpTo(0) }
            return
        }
        scope.launch {
            isLoading = true
            error = null
            val res = Api.getTalks(token)
            isLoading = false
            if (res.isSuccess) {
                talks = res.getOrNull() ?: emptyList()
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("说说管理") },
                actions = {
                    IconButton(onClick = { loadData() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                    IconButton(onClick = { navController.navigate(Screen.TalkEdit.createRoute(null, null)) }) {
                        Icon(Icons.Default.Add, contentDescription = "新建")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
        ) {
            if (isLoading && talks.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (error != null) {
                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                    Button(onClick = { loadData() }) { Text("重试") }
                }
            } else if (talks.isEmpty() && !isLoading) {
                Text("暂无说说，点击右上角新建", color = Color.Gray, modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                    items(talks) { talk ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .clickable { navController.navigate(Screen.TalkEdit.createRoute(talk.name, talk.sha)) },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(talk.title ?: talk.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(talk.date ?: "-", fontSize = 13.sp, color = Color(0xFF64748B))
                                }
                                IconButton(onClick = { navController.navigate(Screen.TalkEdit.createRoute(talk.name, talk.sha)) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "编辑", tint = Color.Gray)
                                }
                                IconButton(onClick = { showDeleteConfirm = talk }) {
                                    Icon(Icons.Default.Delete, contentDescription = "删除", tint = Color(0xFFEF4444))
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
                text = { Text("要删除说说 \${showDeleteConfirm?.title ?: showDeleteConfirm?.name} 吗？") },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            val target = showDeleteConfirm!!
                            showDeleteConfirm = null
                            val res = Api.deleteTalk(token, target.name, target.sha)
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
