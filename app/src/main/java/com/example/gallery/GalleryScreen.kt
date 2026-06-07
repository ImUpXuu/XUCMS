package com.example.gallery

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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

    val uploadLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                uploadProgress = Pair(0, uris.size)
                var count = 0
                for (uri in uris) {
                    try {
                        val base64 = ImageUtil.compressAndEncode(context, uri)
                        val filename = ImageUtil.generateImageFilename()
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
            if (isLoading && images.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(images) { img ->
                        val proxyUrl = "https://edit.upxuu.com/img/${img.path}"
                        val encodedProxy = URLEncoder.encode(proxyUrl, "UTF-8")
                        val thumbUrl = "https://wsrv.nl/?url=$encodedProxy&w=300&h=300&fit=cover&a=top"

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .padding(4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedImage = img }
                        ) {
                            AsyncImage(
                                model = thumbUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                                placeholder = null,
                                error = null
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(img.name, color = Color.White, fontSize = 11.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedImage != null) {
        val img = selectedImage!!
        val proxyUrl = "https://edit.upxuu.com/img/${img.path}"
        ModalBottomSheet(onDismissRequest = { selectedImage = null }) {
            Column(modifier = Modifier.padding(16.dp).padding(bottom = 32.dp)) {
                AsyncImage(
                    model = proxyUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().height(250.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(img.name, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Button(onClick = {
                        clipboardManager.setText(AnnotatedString(proxyUrl))
                        selectedImage = null
                    }) {
                        Text("复制链接")
                    }
                    Button(onClick = {
                        clipboardManager.setText(AnnotatedString("![${img.name}]($proxyUrl)"))
                        selectedImage = null
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
