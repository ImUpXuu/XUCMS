package com.example.talk

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.data.Api
import com.example.data.TalkItem
import com.example.data.TalkPutBody
import com.example.data.TokenManager
import com.example.data.UploadImageBody
import com.example.gallery.GallerySelectionModal
import com.example.navigation.Screen
import com.example.util.FrontmatterParser
import com.example.util.ImageUtil
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorDefaults
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
    
    val richTextState = rememberRichTextState()
    
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    
    var currentSha by remember { mutableStateOf(sha) }
    var isLoading by remember { mutableStateOf(isNew.not()) }
    var isSaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var showMeta by remember { mutableStateOf(false) }
    
    var showGalleryModal by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    
    val prefs = com.example.LocalAppPreferences.current
    var isRawMode by remember { mutableStateOf(false) }
    var rawText by remember { mutableStateOf("") }
    var autoSaveStatus by remember { mutableStateOf("") }

    // List bottom sheet state
    var showListSheet by remember { mutableStateOf(false) }
    var talks by remember { mutableStateOf<List<TalkItem>>(emptyList()) }
    var isTalksLoading by remember { mutableStateOf(false) }
    var isTimelineView by remember { mutableStateOf(false) }
    var drafts by remember { mutableStateOf<List<String>>(emptyList()) }

    val imageUploadLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                for ((index, uri) in uris.withIndex()) {
                    uploadProgress = "正在上传 (${index + 1}/${uris.size})..."
                    try {
                        val base64 = ImageUtil.compressAndEncode(context, uri)
                        val fname = ImageUtil.generateImageFilename(context, uri)
                        val res = Api.uploadImage(token, UploadImageBody(fname, base64))
                        if (res.isSuccess) {
                            val baseUrl = Api.BASE_URL.removeSuffix("/")
                            val url = "$baseUrl/img/${fname}"
                            if (isRawMode) {
                                rawText += "\n![]($url)\n"
                            } else {
                                rawText = richTextState.toMarkdown()
                                isRawMode = true
                                rawText += "\n![]($url)\n"
                                android.widget.Toast.makeText(context, "已切换至源码模式以防图片格式受损", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                uploadProgress = null
            }
        }
    }

    fun loadData(targetFilename: String) {
        scope.launch {
            isLoading = true
            error = null
            val res = Api.getTalk(token, targetFilename)
            isLoading = false
            if (res.isSuccess) {
                val detail = res.getOrThrow()
                currentSha = detail.sha
                val fm = FrontmatterParser.parseFrontmatter(detail.content)
                title = fm.title
                date = fm.published
                tags = fm.tags.joinToString(", ")
                richTextState.setMarkdown(fm.body)
                rawText = fm.body
            } else {
                error = res.exceptionOrNull()?.message
            }
        }
    }

    LaunchedEffect(filename) {
        if (!isNew) {
            loadData(filename!!)
        } else {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            date = sdf.format(Date())
            title = ""
            tags = ""
            richTextState.setMarkdown("")
            val draftStr = prefs?.getDraft("talk_new")
            if (!draftStr.isNullOrEmpty()) {
                val fm = FrontmatterParser.parseFrontmatter(draftStr)
                title = fm.title
                date = fm.published
                tags = fm.tags.joinToString(", ")
                richTextState.setMarkdown(fm.body)
                rawText = fm.body
            }
        }
    }

    LaunchedEffect(title, date, tags, richTextState.annotatedString, rawText, isRawMode) {
        if (isLoading || isSaving) return@LaunchedEffect
        if (title.isBlank() && rawText.isBlank()) return@LaunchedEffect
        kotlinx.coroutines.delay(30000)
        autoSaveStatus = "保存中..."
        val currentMd = if (isRawMode) rawText else richTextState.toMarkdown()
        val fmStr = FrontmatterParser.buildTalkFrontmatter(
            title = title,
            date = date,
            tags = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            body = currentMd
        )
        prefs?.saveDraft("talk_${filename ?: "new"}", fmStr)
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        autoSaveStatus = "已自动保存草稿: ${sdf.format(Date())}"
    }

    val previewFilename = if (title.isBlank()) "输入标题后将自动生成文件名" else FrontmatterParser.generateFilename(title, date)

    if (showGalleryModal) {
        GallerySelectionModal(
            token = token,
            onInsert = { imgs ->
                val baseUrl = Api.BASE_URL.removeSuffix("/")
                val appended = imgs.joinToString("\n") { "![${it.name}]($baseUrl/img/${it.path})" }
                if (isRawMode) {
                    rawText += "\n$appended\n"
                } else {
                    rawText = richTextState.toMarkdown()
                    isRawMode = true
                    rawText += "\n$appended\n"
                    android.widget.Toast.makeText(context, "已切换至源码模式以防图片格式受损", android.widget.Toast.LENGTH_SHORT).show()
                }
                showGalleryModal = false
            },
            onDismiss = { showGalleryModal = false }
        )
    }

    if (showListSheet) {
        LaunchedEffect(Unit) {
            isTalksLoading = true
            drafts = prefs?.getAllDraftKeys() ?: emptyList()
            val res = Api.getTalks(token)
            isTalksLoading = false
            if (res.isSuccess) {
                talks = res.getOrNull()?.sortedByDescending { it.date ?: it.name } ?: emptyList()
            }
        }
        ModalBottomSheet(onDismissRequest = { showListSheet = false }) {
            Column(modifier = Modifier.fillMaxWidth().height(500.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("说说列表", style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = { isTimelineView = !isTimelineView }) {
                        Icon(if (isTimelineView) Icons.Default.ViewDay else Icons.Default.List, contentDescription = "切换视图")
                    }
                }
                
                if (drafts.contains("draft_talk_new")) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable { showListSheet = false; navController.navigate(Screen.TalkEdit.createRoute(null, null)) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("继续编辑新的说说", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Badge { Text("未发布") }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                if (isTalksLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (talks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("暂无说说", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                        items(talks) { talk ->
                            fun onSelect() {
                                showListSheet = false
                                navController.navigate(Screen.TalkEdit.createRoute(talk.name, talk.sha)) {
                                    popUpTo(Screen.TalkEdit.route) { inclusive = true }
                                }
                            }
                            if (isTimelineView) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).clickable { onSelect() }
                                ) {
                                    Column(
                                        modifier = Modifier.width(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(modifier = Modifier.size(12.dp).background(MaterialTheme.colorScheme.primary, CircleShape).padding(vertical = 12.dp))
                                        Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outlineVariant))
                                    }
                                    Column(modifier = Modifier.padding(bottom = 16.dp)) {
                                        Text(talk.date ?: "-", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                        ) {
                                            Text(talk.title ?: talk.name, modifier = Modifier.padding(12.dp), fontWeight = FontWeight.SemiBold)
                                            if (drafts.contains("draft_talk_${talk.name}")) {
                                                Badge(modifier = Modifier.padding(bottom = 12.dp, start = 12.dp)) { Text("缓存") }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable { onSelect() },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(talk.title ?: talk.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                            if (drafts.contains("draft_talk_${talk.name}")) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Badge { Text("缓存") }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(talk.date ?: "-", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            Surface(shadowElevation = 2.dp) {
                Column {
                    TopAppBar(
                        title = {
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                placeholder = { Text("说说标题 (可选)") },
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
                            TextButton(onClick = { showListSheet = true }) {
                                Text("📋 列表")
                            }
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        isSaving = true
                                        val fm = FrontmatterParser.buildTalkFrontmatter(
                                            title = title,
                                            date = date,
                                            tags = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                                            body = richTextState.toMarkdown()
                                        )
                                        val targetFilename = if (isNew) previewFilename else filename!!
                                        val res = Api.putTalk(token, targetFilename, TalkPutBody(fm, currentSha))
                                        isSaving = false
                                        if (res.isSuccess) {
                                            prefs?.clearDraft("talk_${filename ?: "new"}")
                                            if (isNew) {
                                                navController.navigate(Screen.TalkEdit.createRoute(null, null)) {
                                                    popUpTo(Screen.TalkEdit.route) { inclusive = true }
                                                }
                                            } else {
                                                navController.popBackStack()
                                            }
                                        } else {
                                            val e = res.exceptionOrNull()
                                            if (e is Api.ApiException && e.code == 401) {
                                                tokenManager.clearToken()
                                                navController.navigate(Screen.Login.route) { popUpTo(0) }
                                            } else {
                                                error = "保存失败: ${e?.message}"
                                            }
                                        }
                                    }
                                },
                                enabled = !isLoading && !isSaving
                            ) {
                                Text(if (isSaving) "保存中" else "保存", fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                    Text(
                        text = previewFilename,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                }
                
                // Toolbar
                if (uploadProgress != null) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(uploadProgress!!, style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.CenterHorizontally).padding(4.dp))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isRawMode) {
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            TextButton(onClick = { expanded = true }) { Text("H ▼") }
                            androidx.compose.material3.DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text("H1 大标题") },
                                    onClick = { 
                                        richTextState.toggleSpanStyle(androidx.compose.ui.text.SpanStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold))
                                        expanded = false 
                                    }
                                )
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text("H2 中标题") },
                                    onClick = { 
                                        richTextState.toggleSpanStyle(androidx.compose.ui.text.SpanStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold))
                                        expanded = false 
                                    }
                                )
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text("H3 小标题") },
                                    onClick = { 
                                        richTextState.toggleSpanStyle(androidx.compose.ui.text.SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold))
                                        expanded = false 
                                    }
                                )
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text("P 正文") },
                                    onClick = { 
                                        richTextState.toggleSpanStyle(androidx.compose.ui.text.SpanStyle(fontSize = 16.sp))
                                        expanded = false 
                                    }
                                )
                            }
                        }
                        TextButton(onClick = {
                            val md = richTextState.toMarkdown() + "\n---\n"
                            richTextState.setMarkdown(md) 
                        }) { Text("---") }
                        IconButton(onClick = { richTextState.toggleSpanStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold)) }) { Icon(Icons.Default.FormatBold, "Bold") }
                        IconButton(onClick = { richTextState.toggleSpanStyle(androidx.compose.ui.text.SpanStyle(fontStyle = FontStyle.Italic)) }) { Icon(Icons.Default.FormatItalic, "Italic") }
                        IconButton(onClick = { richTextState.toggleSpanStyle(androidx.compose.ui.text.SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)) }) { Icon(Icons.Default.FormatUnderlined, "Underline") }
                        IconButton(onClick = { richTextState.toggleSpanStyle(androidx.compose.ui.text.SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)) }) { Icon(Icons.Default.FormatStrikethrough, "Strike") }
                        IconButton(onClick = { richTextState.toggleUnorderedList() }) { Icon(Icons.Default.FormatListBulleted, "UL") }
                        IconButton(onClick = { richTextState.toggleOrderedList() }) { Icon(Icons.Default.FormatListNumbered, "OL") }
                    } else {
                        TextButton(onClick = { rawText += "\n---\n" }) { Text("---") }
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { imageUploadLauncher.launch("image/*") }) { Icon(Icons.Default.AddPhotoAlternate, "上传图片") }
                    IconButton(onClick = { showGalleryModal = true }) { Icon(Icons.Default.PhotoLibrary, "图库") }
                    Spacer(Modifier.weight(1f))
                    Row(
                        modifier = Modifier.clickable { 
                            if (isRawMode) {
                                richTextState.setMarkdown(rawText)
                            } else {
                                rawText = richTextState.toMarkdown()
                            }
                            isRawMode = !isRawMode 
                        }.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isRawMode) "渲染" else "源码", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }

                if (autoSaveStatus.isNotEmpty()) {
                    Text(autoSaveStatus, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 16.dp))
                }

                Divider()

                if (isRawMode) {
                    OutlinedTextField(
                        value = rawText,
                        onValueChange = { rawText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        placeholder = { Text("使用 Markdown 语法编写说说...") }
                    )
                } else {
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
                        placeholder = { Text("开始编写说说...") }
                    )
                }
                
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
                            OutlinedTextField(
                                value = date,
                                onValueChange = { date = it },
                                label = { Text("发布时间") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = tags,
                                onValueChange = { tags = it },
                                label = { Text("标签 (逗号分隔)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}
