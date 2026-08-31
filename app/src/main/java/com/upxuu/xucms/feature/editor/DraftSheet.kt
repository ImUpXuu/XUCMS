package com.upxuu.xucms.feature.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.AutoMode
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.upxuu.xucms.data.Draft
import com.upxuu.xucms.data.DraftKind
import com.upxuu.xucms.ui.components.EmptyState
import com.upxuu.xucms.ui.components.FlatCard
import com.upxuu.xucms.ui.components.Pill
import com.upxuu.xucms.ui.components.SwipeActionRow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 草稿管理: the autosave slot and every manual snapshot for this note. Tap to
 * load a version, swipe right to delete (undoable via the snackbar), swipe left
 * to load without dismissing the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DraftSheet(
  drafts: List<Draft>,
  onDismiss: () -> Unit,
  onCreateSnapshot: () -> Unit,
  onRestore: (Draft) -> Unit,
  onDelete: (Draft) -> Unit,
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp)) {
      Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text(
          text = "草稿管理",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(3.dp))
        Text(
          text = "自动草稿只保留最新一份；手动快照会一直留着，直到你删除。",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(14.dp))
        OutlinedButton(
          onClick = onCreateSnapshot,
          shape = MaterialTheme.shapes.medium,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Icon(
            Icons.Outlined.AddCircleOutline,
            contentDescription = null,
            modifier = Modifier.size(17.dp),
          )
          Spacer(Modifier.width(8.dp))
          Text("保存当前内容为快照", fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(12.dp))
      }

      if (drafts.isEmpty()) {
        EmptyState(
          icon = Icons.Outlined.DriveFileRenameOutline,
          title = "还没有草稿",
          hint = "内容有改动时会自动保存一份",
        )
      } else {
        LazyColumn(
          modifier = Modifier.heightIn(max = 380.dp),
          contentPadding = PaddingValues(horizontal = 16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          items(drafts, key = { it.id }) { draft ->
            SwipeActionRow(
              onDelete = { onDelete(draft) },
              onSettings = { onRestore(draft) },
              settingsLabel = "载入",
              modifier = Modifier.animateItem(),
            ) {
              DraftCard(draft = draft, onClick = { onRestore(draft) })
            }
          }
          item(key = "hint") {
            Text(
              text = "右滑删除，左滑载入",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.outline,
              textAlign = TextAlign.Center,
              modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun DraftCard(draft: Draft, onClick: () -> Unit) {
  val auto = draft.type == DraftKind.AUTO
  FlatCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = if (auto) Icons.Outlined.AutoMode else Icons.Outlined.Bookmark,
        contentDescription = null,
        tint = if (auto) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(18.dp),
      )
      Spacer(Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = if (auto) "自动草稿" else draft.label.ifBlank { "手动快照" },
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
          )
          Spacer(Modifier.width(8.dp))
          Pill(
            text = if (auto) "自动" else "手动",
            color = if (auto) {
              MaterialTheme.colorScheme.onSecondaryContainer
            } else {
              MaterialTheme.colorScheme.onPrimaryContainer
            },
            container = if (auto) {
              MaterialTheme.colorScheme.secondaryContainer
            } else {
              MaterialTheme.colorScheme.primaryContainer
            },
          )
        }
        Spacer(Modifier.height(3.dp))
        Text(
          text = "${prettyTimestamp(draft.updatedAt)} · ${summarize(draft.markdown)}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

/** First non-frontmatter line, so a draft is identifiable without opening it. */
private fun summarize(markdown: String): String {
  val body = markdown.substringAfter("---\n", markdown).substringAfter("---\n", markdown)
  val line = body.lineSequence()
    .map { it.trim() }
    .firstOrNull { it.isNotEmpty() }
    ?.trimStart('#', '-', '>', ' ')
  return line?.take(28)?.ifBlank { "空白内容" } ?: "空白内容"
}

private fun prettyTimestamp(millis: Long): String =
  SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(millis))
