package com.example.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.Api
import com.example.data.ImageItem
import kotlinx.coroutines.launch
import java.net.URLEncoder

@Composable
fun GallerySelectionModal(token: String, onInsert: (List<ImageItem>) -> Unit, onDismiss: () -> Unit) {
    var images by remember { mutableStateOf<List<ImageItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val selected = remember { mutableStateListOf<ImageItem>() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        isLoading = true
        val res = Api.getImages(token)
        isLoading = false
        if (res.isSuccess) {
            images = res.getOrNull() ?: emptyList()
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(
                    title = { Text("图库选择") },
                    actions = {
                        TextButton(onClick = onDismiss) { Text("取消") }
                    }
                )
                if (isLoading) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(images) { img ->
                            val proxyUrl = "https://edit.upxuu.com/img/${img.path}"
                            val encodedProxy = URLEncoder.encode(proxyUrl, "UTF-8")
                            val thumbUrl = "https://wsrv.nl/?url=$encodedProxy&w=300&h=300&fit=cover&a=top"
                            val isSelected = selected.contains(img)

                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (isSelected) selected.remove(img) else selected.add(img)
                                    }
                            ) {
                                AsyncImage(
                                    model = thumbUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                                        contentAlignment = Alignment.TopStart
                                    ) {
                                        Checkbox(checked = true, onCheckedChange = null, modifier = Modifier.padding(4.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                Surface(shadowElevation = 8.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("已选 ${selected.size} 张", style = MaterialTheme.typography.bodyLarge)
                        Button(onClick = { onInsert(selected) }, enabled = selected.isNotEmpty()) {
                            Text("插入")
                        }
                    }
                }
            }
        }
    }
}
