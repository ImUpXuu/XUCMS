package com.example.post

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.data.Api
import com.example.data.PostPutBody
import com.example.data.TokenManager
import com.example.navigation.Screen
import com.example.util.FrontmatterParser
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorDefaults
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostEditScreen(tokenManager: TokenManager, navController: NavController, filename: String?, sha: String?) {
    val isNew = filename == null
    val token = tokenManager.getToken() ?: ""
    val scope = rememberCoroutineScope()
    
    val richTextState = rememberRichTextState()

    var title by remember { mutableStateOf("") }
    var published by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var image by remember { mutableStateOf("") }
    var draft by remember { mutableStateOf(false) }
    var sticky by remember { mutableStateOf("0") }
    
    var currentSha by remember { mutableStateOf(sha) }
    var isLoading by remember { mutableStateOf(isNew.not()) }
    var isSaving by remember { mutableStateOf(false) }
    var showMeta by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (!isNew) {
            val res = Api.getPost(token, filename!!)
            isLoading = false
            if (res.isSuccess) {
                val detail = res.getOrThrow()
                currentSha = detail.sha
                val fm = FrontmatterParser.parseFrontmatter(detail.content)
                title = fm.title
                published = fm.published
                category = fm.category
                tags = fm.tags.joinToString(", ")
                description = fm.description
                image = fm.image
                draft = fm.draft
                sticky = fm.sticky.toString()
                richTextState.setMarkdown(fm.body)
            } else {
                error = res.exceptionOrNull()?.message
            }
        } else {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            published = sdf.format(Date())
        }
    }

    val previewFilename = if (title.isBlank()) "输入标题后将自动生成文件名" else FrontmatterParser.generateFilename(title, published)

    Scaffold(
        topBar = {
            Surface(shadowElevation = 2.dp) {
                Column {
                    TopAppBar(
                        title = {
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                placeholder = { Text("文章标题") },
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
                                        val bodyMarkdown = richTextState.toMarkdown()
                                        val fm = FrontmatterParser.buildPostFrontmatter(
                                            com.example.util.FrontmatterResult(
                                                title = title,
                                                published = published,
                                                category = category,
                                                tags = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                                                description = description,
                                                image = image,
                                                draft = draft,
                                                sticky = sticky.toIntOrNull() ?: 0,
                                                body = bodyMarkdown
                                            )
                                        )
                                        val targetFilename = if (isNew) previewFilename else filename!!
                                        val res = Api.putPost(token, targetFilename, PostPutBody(fm, currentSha))
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
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                }
                
                // Toolbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = { richTextState.toggleSpanStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold)) }) { Icon(Icons.Default.FormatBold, "Bold") }
                    IconButton(onClick = { richTextState.toggleSpanStyle(androidx.compose.ui.text.SpanStyle(fontStyle = FontStyle.Italic)) }) { Icon(Icons.Default.FormatItalic, "Italic") }
                    IconButton(onClick = { richTextState.toggleSpanStyle(androidx.compose.ui.text.SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)) }) { Icon(Icons.Default.FormatUnderlined, "Underline") }
                    IconButton(onClick = { richTextState.toggleSpanStyle(androidx.compose.ui.text.SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)) }) { Icon(Icons.Default.FormatStrikethrough, "Strike") }
                    IconButton(onClick = { richTextState.toggleUnorderedList() }) { Icon(Icons.Default.FormatListBulleted, "UL") }
                    IconButton(onClick = { richTextState.toggleOrderedList() }) { Icon(Icons.Default.FormatListNumbered, "OL") }
                }

                Divider()

                RichTextEditor(
                    state = richTextState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp),
                    colors = RichTextEditorDefaults.richTextEditorColors(
                        containerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    placeholder = { Text("开始编写正文...") }
                )

                Divider()
                
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showMeta = !showMeta }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("▼ 编辑信息（可折叠）", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    }
                    AnimatedVisibility(visible = showMeta) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
                        ) {
                            OutlinedTextField(value = published, onValueChange = { published = it }, label = { Text("发布时间") }, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("分类") }, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(value = tags, onValueChange = { tags = it }, label = { Text("标签 (逗号分隔)") }, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("描述") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(value = image, onValueChange = { image = it }, label = { Text("封面 URL") }, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("草稿", modifier = Modifier.weight(1f))
                                Switch(checked = draft, onCheckedChange = { draft = it })
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(value = sticky, onValueChange = { sticky = it }, label = { Text("置顶") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        }
                    }
                }
            }
        }
    }
}
