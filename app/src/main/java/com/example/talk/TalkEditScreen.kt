package com.example.talk

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.data.Api
import com.example.data.TalkPutBody
import com.example.data.TokenManager
import com.example.navigation.Screen
import com.example.util.FrontmatterParser
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TalkEditScreen(tokenManager: TokenManager, navController: NavController, filename: String?, sha: String?) {
    val isNew = filename == null
    val token = tokenManager.getToken() ?: ""
    val scope = rememberCoroutineScope()
    
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    
    var currentSha by remember { mutableStateOf(sha) }
    var isLoading by remember { mutableStateOf(isNew.not()) }
    var isSaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (!isNew) {
            val res = Api.getTalk(token, filename!!)
            isLoading = false
            if (res.isSuccess) {
                val detail = res.getOrThrow()
                currentSha = detail.sha
                val fm = FrontmatterParser.parseFrontmatter(detail.content)
                title = fm.title
                date = fm.published
                tags = fm.tags.joinToString(", ")
                body = fm.body
            } else {
                error = res.exceptionOrNull()?.message
            }
        } else {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            date = sdf.format(Date())
        }
    }

    val previewFilename = if (title.isBlank()) "输入标题后将自动生成文件名" else FrontmatterParser.generateFilename(title, date)

    Scaffold(
        topBar = {
            Surface(shadowElevation = 2.dp) {
                Column {
                    TopAppBar(
                        title = {
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                placeholder = { Text("说说标题") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                            }
                        },
                        actions = {
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        isSaving = true
                                        val fm = FrontmatterParser.buildTalkFrontmatter(
                                            title = title,
                                            date = date,
                                            tags = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                                            body = body
                                        )
                                        val targetFilename = if (isNew) previewFilename else filename!!
                                        val res = Api.putTalk(token, targetFilename, TalkPutBody(fm, currentSha))
                                        isSaving = false
                                        if (res.isSuccess) {
                                            navController.popBackStack()
                                        } else {
                                            val e = res.exceptionOrNull()
                                            if (e is Api.ApiException && e.code == 401) {
                                                tokenManager.clearToken()
                                                navController.navigate(Screen.Login.route) { popUpTo(0) }
                                            } else {
                                                error = "保存失败: \${e?.message}"
                                            }
                                        }
                                    }
                                },
                                enabled = title.isNotBlank() && !isLoading && !isSaving
                            ) {
                                Text(if (isSaving) "保存中" else "保存", fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                    Text(
                        text = previewFilename,
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8),
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                }
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("日期 (YYYY-MM-DD HH:mm:ss)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tags,
                        onValueChange = { tags = it },
                        label = { Text("标签 (逗号分隔)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = body,
                        onValueChange = { body = it },
                        label = { Text("正文") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp)
                    )
                }
            }
        }
    }
}
