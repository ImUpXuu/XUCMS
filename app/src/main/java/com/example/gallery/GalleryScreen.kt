package com.example.gallery

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.InsertPhoto
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.Api
import com.example.data.ImageItem
import com.example.data.TokenManager
import com.example.data.UploadImageBody
import com.example.util.ImageUtil
import kotlinx.coroutines.launch
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(tokenManager: TokenManager) {
    val token = tokenManager.getToken() ?: ""
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var images by remember { mutableStateOf<List<ImageItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf<Pair<Int, Int>?>(null) } // Current, Total
    var selectedImage by remember { mutableStateOf<ImageItem?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<ImageItem?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    
    fun loadData() {
        scope.launch {
            isLoading = true
            val res = Api.getImages(token)
            isLoading = false
            if (res.isSuccess) {
                images = res.getOrNull() ?: emptyList()
            }
        }
    }

    LaunchedEffect(Unit) { loadData() }

    val filteredImages = remember(images, searchQuery) {
        images.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    val uploadLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                uploadProgress = Pair(0, uris.size)
                var count = 0
                for (uri in uris) {
                    try {
                        val base64 = ImageUtil.compressAndEncode(context, uri)
                        val filename = ImageUtil.generateImageFilename(context, uri)
                        Api.uploadImage(token, UploadImageBody(filename, base64))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    count++
                    uploadProgress = Pair(count, uris.size)
                }
                uploadProgress = null
                loadData()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("图库") },
                actions = {
                    IconButton(onClick = { uploadLauncher.launch("image/*") }) {
                        Icon(Icons.Default.Add, contentDescription = "上传")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (uploadProgress != null) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    "正在上传 (${uploadProgress!!.first}/${uploadProgress!!.second})...",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(8.dp).align(Alignment.CenterHorizontally)
                )
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("搜索图库内文件名") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "搜索") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "清除")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            if (isLoading && images.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredImages.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("无图片记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredImages) { img ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedImage = img },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(14.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.InsertPhoto,
                                    contentDescription = "图片",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = img.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Default.KeyboardArrowRight,
                                    contentDescription = "详情",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedImage != null) {
        val img = selectedImage!!
        val baseUrl = Api.BASE_URL.removeSuffix("/")
        val proxyUrl = "$baseUrl/img/${img.path}"
        ModalBottomSheet(onDismissRequest = { selectedImage = null }) {
            Column(modifier = Modifier.padding(16.dp).padding(bottom = 32.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = proxyUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(img.name, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = {
                        clipboardManager.setText(AnnotatedString(proxyUrl))
                        selectedImage = null
                        android.widget.Toast.makeText(context, "已复制图片链接！", android.widget.Toast.LENGTH_SHORT).show()
                    }) {
                        Text("复制链接")
                    }
                    Button(onClick = {
                        clipboardManager.setText(AnnotatedString("![${img.name}]($proxyUrl)"))
                        selectedImage = null
                        android.widget.Toast.makeText(context, "已复制 Markdown 格式！", android.widget.Toast.LENGTH_SHORT).show()
                    }) {
                        Text("复制 Markdown")
                    }
                    IconButton(onClick = {
                        selectedImage = null
                        showDeleteConfirm = img
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (showDeleteConfirm != null) {
        val img = showDeleteConfirm!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("确认删除") },
            text = { Text("要删除图片 ${img.name} 吗？") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        Api.deleteImage(token, img.path, img.sha)
                        showDeleteConfirm = null
                        loadData()
                    }
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("取消") }
            }
        )
    }
}
