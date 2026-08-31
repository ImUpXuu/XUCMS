package com.upxuu.xucms.feature.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.upxuu.xucms.LocalAppContainer
import com.upxuu.xucms.ui.components.EmptyState
import com.upxuu.xucms.ui.rememberViewModel

/** Multi-select image picker used from the editor toolbar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryPickerSheet(
  onDismiss: () -> Unit,
  onPick: (List<String>) -> Unit,
) {
  val container = LocalAppContainer.current
  val viewModel = rememberViewModel("gallery-picker") { GalleryViewModel(container.api) }
  val state by viewModel.state.collectAsState()

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      Text(
        text = "从图库插入",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
      )
      OutlinedTextField(
        value = state.query,
        onValueChange = viewModel::setQuery,
        placeholder = { Text("搜索文件名") },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
      )

      Box(modifier = Modifier.weight(1f)) {
        when {
          state.loading && state.images.isEmpty() ->
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
          state.visible.isEmpty() -> EmptyState(
            icon = Icons.Outlined.BrokenImage,
            title = "图库还没有图片",
            hint = "先在编辑器里上传一张试试",
            modifier = Modifier.align(Alignment.Center),
          )
          else -> LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            items(state.visible, key = { it.path }) { image ->
              val selected = image.path in state.selected
              Box(
                modifier = Modifier
                  .aspectRatio(1f)
                  .clip(RoundedCornerShape(10.dp))
                  .background(MaterialTheme.colorScheme.surfaceVariant)
                  .then(
                    if (selected) {
                      Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                    } else {
                      Modifier
                    },
                  )
                  .clickable { viewModel.toggle(image.path) },
              ) {
                AsyncImage(
                  model = viewModel.urlFor(image.path),
                  contentDescription = image.name,
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize(),
                )
                if (selected) {
                  Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                      .align(Alignment.TopEnd)
                      .padding(4.dp)
                      .size(20.dp)
                      .background(Color.White, RoundedCornerShape(50)),
                  )
                }
              }
            }
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "已选 ${state.selected.size} 张",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.weight(1f),
        )
        Button(
          onClick = { onPick(state.selected.toList()) },
          enabled = state.selected.isNotEmpty(),
          shape = MaterialTheme.shapes.medium,
        ) {
          Text("插入", fontWeight = FontWeight.SemiBold)
        }
      }
      Spacer(Modifier.height(12.dp))
    }
  }
}
