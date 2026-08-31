package com.upxuu.xucms.ui.components

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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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

/**
 * Frontmatter fields, shared by the editor's 属性 sheet and the home list's
 * swipe-left quick edit so both surfaces stay identical.
 */
@Composable
fun MetaFields(
  kind: NoteKind,
  postMeta: PostMeta,
  talkMeta: TalkMeta,
  onPostChange: ((PostMeta) -> PostMeta) -> Unit,
  onTalkChange: ((TalkMeta) -> TalkMeta) -> Unit,
  modifier: Modifier = Modifier,
  /** Off in the editor, where the title has its own prominent field. */
  includeTitle: Boolean = true,
) {
  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
    when (kind) {
      NoteKind.POST -> {
        if (includeTitle) {
          OutlinedTextField(
            value = postMeta.title,
            onValueChange = { value -> onPostChange { it.copy(title = value) } },
            label = { Text("标题") },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
          )
        }
        OutlinedTextField(
          value = postMeta.published,
          onValueChange = { value -> onPostChange { it.copy(published = value) } },
          label = { Text("发布时间") },
          singleLine = true,
          shape = MaterialTheme.shapes.medium,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = postMeta.category,
          onValueChange = { value -> onPostChange { it.copy(category = value) } },
          label = { Text("分类") },
          singleLine = true,
          shape = MaterialTheme.shapes.medium,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = postMeta.tags.joinToString(", "),
          onValueChange = { value -> onPostChange { it.copy(tags = splitTags(value)) } },
          label = { Text("标签（逗号分隔）") },
          singleLine = true,
          shape = MaterialTheme.shapes.medium,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = postMeta.description,
          onValueChange = { value -> onPostChange { it.copy(description = value) } },
          label = { Text("摘要") },
          minLines = 2,
          shape = MaterialTheme.shapes.medium,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = postMeta.cover,
          onValueChange = { value -> onPostChange { it.copy(cover = value) } },
          label = { Text("封面图 URL") },
          singleLine = true,
          shape = MaterialTheme.shapes.medium,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = if (postMeta.sticky == 0) "" else postMeta.sticky.toString(),
          onValueChange = { value -> onPostChange { it.copy(sticky = value.toIntOrNull() ?: 0) } },
          label = { Text("置顶权重（留空为不置顶）") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          shape = MaterialTheme.shapes.medium,
          modifier = Modifier.fillMaxWidth(),
        )
        Row(
          modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("标记为草稿", style = MaterialTheme.typography.titleMedium)
            Text(
              text = "开启后文章不会在站点公开显示",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Switch(
            checked = postMeta.draft,
            onCheckedChange = { value -> onPostChange { it.copy(draft = value) } },
          )
        }
      }

      NoteKind.TALK -> {
        if (includeTitle) {
          OutlinedTextField(
            value = talkMeta.title,
            onValueChange = { value -> onTalkChange { it.copy(title = value) } },
            label = { Text("标题（可留空）") },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
          )
        }
        OutlinedTextField(
          value = talkMeta.date,
          onValueChange = { value -> onTalkChange { it.copy(date = value) } },
          label = { Text("发布时间") },
          singleLine = true,
          shape = MaterialTheme.shapes.medium,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = talkMeta.tags.joinToString(", "),
          onValueChange = { value -> onTalkChange { it.copy(tags = splitTags(value)) } },
          label = { Text("标签（逗号分隔）") },
          singleLine = true,
          shape = MaterialTheme.shapes.medium,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    }
  }
}

/** Sheet wrapper used by the home list: same fields plus a save button. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickMetaSheet(
  kind: NoteKind,
  title: String,
  postMeta: PostMeta,
  talkMeta: TalkMeta,
  loading: Boolean,
  saving: Boolean,
  onPostChange: ((PostMeta) -> PostMeta) -> Unit,
  onTalkChange: ((TalkMeta) -> TalkMeta) -> Unit,
  onSave: () -> Unit,
  onDismiss: () -> Unit,
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
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = "修改后会直接写回云端，正文保持不变。",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.height(16.dp))

      if (loading) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
          horizontalArrangement = Arrangement.Center,
        ) {
          CircularProgressIndicator()
        }
      } else {
        MetaFields(
          kind = kind,
          postMeta = postMeta,
          talkMeta = talkMeta,
          onPostChange = onPostChange,
          onTalkChange = onTalkChange,
        )
        Spacer(Modifier.height(20.dp))
        Button(
          onClick = onSave,
          enabled = !saving,
          shape = MaterialTheme.shapes.medium,
          modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
          if (saving) {
            CircularProgressIndicator(
              modifier = Modifier.height(18.dp),
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.onPrimary,
            )
          } else {
            Text("保存", fontWeight = FontWeight.SemiBold)
          }
        }
      }
    }
  }
}

private fun splitTags(raw: String): List<String> =
  raw.split(',', '，').map { it.trim() }.filter { it.isNotEmpty() }
