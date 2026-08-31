package com.upxuu.xucms.feature.gallery

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.upxuu.xucms.LocalAppContainer
import com.upxuu.xucms.data.GalleryImage
import com.upxuu.xucms.data.ImagePipeline
import com.upxuu.xucms.ui.components.EmptyState
import com.upxuu.xucms.ui.rememberViewModel
import kotlinx.coroutines.launch

/** Standalone gallery: browse, upload, copy links, delete. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(onBack: () -> Unit) {
  val container = LocalAppContainer.current
  val context = LocalContext.current
  val clipboard = LocalClipboardManager.current
  val scope = rememberCoroutineScope()
  val viewModel = rememberViewModel("gallery") { GalleryViewModel(container.api) }
  val state by viewModel.state.collectAsState()
  val snackbar = remember { SnackbarHostState() }

  var detail by remember { mutableStateOf<GalleryImage?>(null) }
  var pendingDelete by remember { mutableStateOf<GalleryImage?>(null) }
  var uploading by remember { mutableStateOf<Pair<Int, Int>?>(null) }

  val picker = rememberLauncherForActivityResult(
    ActivityResultContracts.GetMultipleContents(),
  ) { uris ->
    if (uris.isEmpty()) return@rememberLauncherForActivityResult
    scope.launch {
      uris.forEachIndexed { index, uri ->
        uploading = (index + 1) to uris.size
        val prepared = ImagePipeline.prepare(context, uri)
        if (prepared != null) {
          container.api.uploadImage(prepared.filename, prepared.base64)
        }
      }
      uploading = null
      viewModel.refresh()
      snackbar.showSnackbar("上传完成")
    }
  }

  LaunchedEffect(state.error) {
    state.error?.let {
      snackbar.showSnackbar(it)
      viewModel.dismissError()
    }
  }

  Scaffold(
    snackbarHost = { SnackbarHost(snackbar) },
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        title = { Text("图库", style = MaterialTheme.typography.titleMedium) },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
          }
        },
        actions = {
          IconButton(onClick = { picker.launch("image/*") }) {
            Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = "上传图片")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background,
        ),
      )
    },
  ) { padding ->
    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
      uploading?.let { (current, total) ->
        LinearProgressIndicator(
          progress = { current.toFloat() / total },
          modifier = Modifier.fillMaxWidth(),
        )
        Text(
          text = "正在上传 $current/$total",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp),
        )
      }

      OutlinedTextField(
        value = state.query,
        onValueChange = viewModel::setQuery,
        placeholder = { Text("搜索文件名") },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
      )

      Box(modifier = Modifier.fillMaxSize()) {
        when {
          state.loading && state.images.isEmpty() ->
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
          state.visible.isEmpty() -> EmptyState(
            icon = Icons.Outlined.BrokenImage,
            title = "图库还没有图片",
            hint = "点击右上角上传",
            modifier = Modifier.align(Alignment.Center),
          )
          else -> LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            items(state.visible, key = { it.path }) { image ->
              AsyncImage(
                model = viewModel.urlFor(image.path),
                contentDescription = image.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                  .aspectRatio(1f)
                  .clip(RoundedCornerShape(10.dp))
                  .background(MaterialTheme.colorScheme.surfaceVariant)
                  .clickable { detail = image },
              )
            }
          }
        }
      }
    }
  }

  detail?.let { image ->
    val url = viewModel.urlFor(image.path)
    ModalBottomSheet(
      onDismissRequest = { detail = null },
      containerColor = MaterialTheme.colorScheme.surface,
    ) {
      Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
        AsyncImage(
          model = url,
          contentDescription = image.name,
          contentScale = ContentScale.Fit,
          modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Spacer(Modifier.height(14.dp))
        Text(image.name, style = MaterialTheme.typography.titleMedium)
        Text(
          text = image.path,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedButton(
            onClick = {
              clipboard.setText(AnnotatedString(url))
              detail = null
              scope.launch { snackbar.showSnackbar("已复制链接") }
            },
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.weight(1f),
          ) {
            Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.size(6.dp))
            Text("链接")
          }
          OutlinedButton(
            onClick = {
              clipboard.setText(AnnotatedString("![](" + url + ")"))
              detail = null
              scope.launch { snackbar.showSnackbar("已复制 Markdown") }
            },
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.weight(1f),
          ) {
            Text("Markdown")
          }
          IconButton(onClick = {
            pendingDelete = image
            detail = null
          }) {
            Icon(
              Icons.Outlined.Delete,
              contentDescription = "删除",
              tint = MaterialTheme.colorScheme.error,
            )
          }
        }
      }
    }
  }

  pendingDelete?.let { image ->
    AlertDialog(
      onDismissRequest = { pendingDelete = null },
      title = { Text("删除图片？") },
      text = { Text("将从云端删除 ${image.name}，引用它的文章会显示为破图。") },
      confirmButton = {
        TextButton(onClick = {
          viewModel.delete(image)
          pendingDelete = null
        }) {
          Text("删除", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
        }
      },
      dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("取消") } },
      shape = MaterialTheme.shapes.large,
    )
  }
}
