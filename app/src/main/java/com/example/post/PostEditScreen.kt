package com.example.post

import android.os.Build
import android.text.Html
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.example.data.Api
import com.example.data.PostPutBody
import com.example.data.TokenManager
import com.example.data.UploadImageBody
import com.example.gallery.GallerySelectionModal
import com.example.navigation.Screen
import com.example.util.FrontmatterParser
import com.example.util.ImageUtil
import com.example.util.SimpleMarkdownParser
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PostEditScreen(tokenManager: TokenManager, navController: NavController, filename: String?, sha: String?) {
    val isNew = filename == null
    val token = tokenManager.getToken() ?: ""
    val scope = rememberCoroutineScope()
    
    var markdownContent by remember { mutableStateOf("") }

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
    
    var customFilename by remember { mutableStateOf("") }
    
    var showGalleryModal by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    
    val prefs = com.example.LocalAppPreferences.current
    var autoSaveStatus by remember { mutableStateOf("") }
    
    // 即时渲染预览模式
    var showPreview by remember { mutableStateOf(false) }

    val imageUploadLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                for ((index, uri) in uris.withIndex()) {
                    uploadProgress = "正在上传 (${index + 1}/${uris.size})..."
                    try {
                        val base64 = ImageUtil.compressAndEncode(context, uri)
                        val filename = ImageUtil.generateImageFilename(context, uri)
                        val res = Api.uploadImage(token, UploadImageBody(filename, base64))
                        if (res.isSuccess) {
                            val baseUrl = Api.BASE_URL.removeSuffix("/")
                            val url = "$baseUrl/img/${filename}"
                            val imgMd = "![uploaded image]($url)"
                            markdownContent = if (markdownContent.isBlank()) imgMd else "$markdownContent\n$imgMd"
                            if (image.isBlank()) {
                                image = url
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
                markdownContent = fm.body
            } else {
                error = res.exceptionOrNull()?.message
            }
        } else {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            published = sdf.format(Date())
            // local draft restore?
            val draftStr = prefs?.getDraft("post_new")
            if (!draftStr.isNullOrEmpty()) {
                val fm = FrontmatterParser.parseFrontmatter(draftStr)
                title = fm.title
                published = fm.published
                category = fm.category
                tags = fm.tags.joinToString(", ")
                description = fm.description
                image = fm.image
                draft = fm.draft
                sticky = fm.sticky.toString()
                markdownContent = fm.body
            }
        }
    }

    LaunchedEffect(title, published, tags, category, description, image, draft, sticky, markdownContent) {
        if (isLoading || isSaving) return@LaunchedEffect
        // ignore initial empty state
        if (title.isBlank() && markdownContent.isBlank()) return@LaunchedEffect
        kotlinx.coroutines.delay(30000)
        autoSaveStatus = "保存中..."
        val fmStr = FrontmatterParser.buildPostFrontmatter(
            com.example.util.FrontmatterResult(
                title = title,
                published = published,
                category = category,
                tags = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                description = description,
                image = image,
                draft = draft,
                sticky = sticky.toIntOrNull() ?: 0,
                body = markdownContent
            )
        )
        prefs?.saveDraft("post_${filename ?: "new"}", fmStr)
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        autoSaveStatus = "已自动保存草稿: ${sdf.format(Date())}"
    }

    val previewFilename = if (customFilename.isNotBlank()) "${customFilename.trim()}.md" else if (title.isBlank()) "输入标题后将自动生成文件名" else FrontmatterParser.generateFilename(title, published)

    if (showGalleryModal) {
        GallerySelectionModal(
            token = token,
            onInsert = { imgs ->
                val baseUrl = Api.BASE_URL.removeSuffix("/")
                val appended = imgs.joinToString("\n") { "![gallery image]($baseUrl/img/${it.path})" }
                markdownContent = if (markdownContent.isBlank()) appended else "$markdownContent\n$appended"
                if (image.isBlank() && imgs.isNotEmpty()) {
                    image = "$baseUrl/img/${imgs.first().path}"
                }
                showGalleryModal = false
            },
            onDismiss = { showGalleryModal = false }
        )
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
                                                body = markdownContent
                                            )
                                        )
                                        val targetFilename = if (isNew) previewFilename else filename!!
                                        val res = Api.putPost(token, targetFilename, PostPutBody(fm, currentSha))
                                        isSaving = false
                                        if (res.isSuccess) {
                                            prefs?.clearDraft("post_${filename ?: "new"}")
                                            navController.popBackStack()
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
                                enabled = title.isNotBlank() && !isLoading && !isSaving
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
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { expanded = true }) { Text("H ▼") }
                        androidx.compose.material3.DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("# H1 大标题") },
                                onClick = { 
                                    markdownContent += "\n# ${title}\n"
                                    expanded = false 
                                }
                            )
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("## H2 中标题") },
                                onClick = { 
                                    markdownContent += "\n## "
                                    expanded = false 
                                }
                            )
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("### H3 小标题") },
                                onClick = { 
                                    markdownContent += "\n### "
                                    expanded = false 
                                }
                            )
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("P 正文") },
                                onClick = { 
                                    markdownContent += "\n"
                                    expanded = false 
                                }
                            )
                        }
                    }
                    TextButton(onClick = {
                        markdownContent += "\n---\n"
                    }) { Text("---") }
                    IconButton(onClick = { markdownContent += "**bold**" }) { Icon(Icons.Default.FormatBold, "Bold") }
                    IconButton(onClick = { markdownContent += "*italic*" }) { Icon(Icons.Default.FormatItalic, "Italic") }
                    IconButton(onClick = { markdownContent += "<u>underline</u>" }) { Icon(Icons.Default.FormatUnderlined, "Underline") }
                    IconButton(onClick = { markdownContent += "~~strike~~" }) { Icon(Icons.Default.FormatStrikethrough, "Strike") }
                    IconButton(onClick = { markdownContent += "\n- list item" }) { Icon(Icons.Default.FormatListBulleted, "UL") }
                    IconButton(onClick = { markdownContent += "\n1. numbered item" }) { Icon(Icons.Default.FormatListNumbered, "OL") }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { imageUploadLauncher.launch("image/*") }) { Icon(Icons.Default.AddPhotoAlternate, "上传图片") }
                    IconButton(onClick = { showGalleryModal = true }) { Icon(Icons.Default.PhotoLibrary, "图库") }
                    
                    // 预览切换按钮
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { showPreview = !showPreview }) {
                        Text(if (showPreview) "编辑" else "预览")
                    }
                }

                if (autoSaveStatus.isNotEmpty()) {
                    Text(autoSaveStatus, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 16.dp))
                }

                Divider()

                // 编辑区和预览区切换
                if (showPreview) {
                    // 预览模式 - 使用 Android WebView 渲染 HTML
                    val htmlContent = remember(markdownContent) {
                        SimpleMarkdownParser.parse(markdownContent)
                    }
                    AndroidView(
                        factory = { ctx ->
                            android.webkit.WebView(ctx).apply {
                                settings.javaScriptEnabled = false
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                isVerticalScrollBarEnabled = true
                                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        update = { webView ->
                            val fullHtml = """
                                <!DOCTYPE html>
                                <html>
                                <head>
                                    <meta name="viewport" content="width=device-width, initial-scale=1">
                                    <style>
                                        body { 
                                            font-family: sans-serif; 
                                            padding: 8px; 
                                            color: #333;
                                            line-height: 1.6;
                                        }
                                        img { max-width: 100%; height: auto; border-radius: 6px; }
                                        pre { white-space: pre-wrap; word-wrap: break-word; }
                                        a { color: #2196F3; }
                                        blockquote { margin: 8px 0; }
                                    </style>
                                </head>
                                <body>$htmlContent</body>
                                </html>
                            """.trimIndent()
                            webView.loadDataWithBaseURL(null, fullHtml, "text/html", "UTF-8", null)
                        }
                    )
                } else {
                    // 编辑模式
                    TextField(
                        value = markdownContent,
                        onValueChange = { markdownContent = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(16.dp),
                        placeholder = { Text("开始编写正文...") },
                        minLines = 20,
                        maxLines = Int.MAX_VALUE
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
                                value = customFilename,
                                onValueChange = { customFilename = it },
                                label = { Text("自定义文件名 (无需输 .md)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
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
