package com.upxuu.xucms.feature.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.upxuu.xucms.data.NoteKind
import com.upxuu.xucms.data.PostMeta
import com.upxuu.xucms.data.TalkMeta

/** Metadata editor. Everything optional lives here so the writing surface stays bare. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetaSheet(
  state: EditorUiState,
  onDismiss: () -> Unit,
  onPostChange: ((PostMeta) -> PostMeta) -> Unit,
  onTalkChange: ((TalkMeta) -> TalkMeta) -> Unit,
  onFilenameChange: (String) -> Unit,
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .imePadding()
        .padding(horizontal = 20.dp)
        .padding(bottom = 32.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = "属性",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 4.dp),
      )

      Text(
        text = "文件名：${state.targetFilename()}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      if (state.isNew) {
        OutlinedTextField(
          value = state.customFilename,
          onValueChange = onFilenameChange,
          label = { Text("自定义文件名（可留空，无需 .md）") },
          singleLine = true,
          shape = MaterialTheme.shapes.medium,
          modifier = Modifier.fillMaxWidth(),
        )
      }

      when (state.kind) {
        NoteKind.POST -> {
          val meta = state.postMeta
          OutlinedTextField(
            value = meta.published,
            onValueChange = { value -> onPostChange { it.copy(published = value) } },
            label = { Text("发布时间") },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = meta.category,
            onValueChange = { value -> onPostChange { it.copy(category = value) } },
            label = { Text("分类") },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = meta.tags.joinToString(", "),
            onValueChange = { value -> onPostChange { it.copy(tags = splitTags(value)) } },
            label = { Text("标签（逗号分隔）") },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = meta.description,
            onValueChange = { value -> onPostChange { it.copy(description = value) } },
            label = { Text("摘要") },
            minLines = 2,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = meta.cover,
            onValueChange = { value -> onPostChange { it.copy(cover = value) } },
            label = { Text("封面图 URL") },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = if (meta.sticky == 0) "" else meta.sticky.toString(),
            onValueChange = { value -> onPostChange { it.copy(sticky = value.toIntOrNull() ?: 0) } },
            label = { Text("置顶权重（留空为不置顶）") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
          )
          Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("标记为草稿", style = MaterialTheme.typography.titleMedium)
              Text(
                "开启后文章不会在站点公开显示",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Switch(
              checked = meta.draft,
              onCheckedChange = { value -> onPostChange { it.copy(draft = value) } },
            )
          }
        }

        NoteKind.TALK -> {
          val meta = state.talkMeta
          OutlinedTextField(
            value = meta.date,
            onValueChange = { value -> onTalkChange { it.copy(date = value) } },
            label = { Text("发布时间") },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = meta.tags.joinToString(", "),
            onValueChange = { value -> onTalkChange { it.copy(tags = splitTags(value)) } },
            label = { Text("标签（逗号分隔）") },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }

      Spacer(Modifier.height(4.dp))
    }
  }
}

private fun splitTags(raw: String): List<String> =
  raw.split(',', '，')
    .map { it.trim() }
    .filter { it.isNotEmpty() }
