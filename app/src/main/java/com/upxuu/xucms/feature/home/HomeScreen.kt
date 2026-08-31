package com.upxuu.xucms.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.upxuu.xucms.LocalAppContainer
import com.upxuu.xucms.data.Draft
import com.upxuu.xucms.data.NoteKind
import com.upxuu.xucms.data.NoteSummary
import com.upxuu.xucms.ui.components.EmptyState
import com.upxuu.xucms.ui.components.FlatCard
import com.upxuu.xucms.ui.components.Pill
import com.upxuu.xucms.ui.components.SectionLabel
import com.upxuu.xucms.ui.rememberViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
  onOpenNote: (NoteKind, NoteSummary) -> Unit,
  onOpenDraft: (Draft) -> Unit,
  onCreate: (NoteKind) -> Unit,
  onOpenGallery: () -> Unit,
  onOpenSettings: () -> Unit,
) {
  val container = LocalAppContainer.current
  val viewModel = rememberViewModel("home") {
    HomeViewModel(container.api, container.drafts, container.settings)
  }
  val state by viewModel.state.collectAsState()
  val snackbar = remember { SnackbarHostState() }

  var searching by remember { mutableStateOf(false) }
  var pendingDelete by remember { mutableStateOf<NoteSummary?>(null) }

  LaunchedEffect(state.error) {
    state.error?.let {
      snackbar.showSnackbar(it)
      viewModel.dismissError()
    }
  }

  Scaffold(
    snackbarHost = { SnackbarHost(snackbar) },
    topBar = {
      CenterAlignedTopAppBar(
        title = {
          Text(
            text = "XUCMS",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
          )
        },
        actions = {
          IconButton(onClick = { searching = !searching }) {
            Icon(Icons.Outlined.Search, contentDescription = "搜索")
          }
          IconButton(onClick = onOpenGallery) {
            Icon(Icons.Outlined.PhotoLibrary, contentDescription = "图库")
          }
          IconButton(onClick = onOpenSettings) {
            Icon(Icons.Outlined.Settings, contentDescription = "设置")
          }
        },
        navigationIcon = {
          IconButton(onClick = { viewModel.refresh() }) {
            Icon(Icons.Outlined.Refresh, contentDescription = "刷新")
          }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
          containerColor = MaterialTheme.colorScheme.background,
        ),
      )
    },
    floatingActionButton = {
      ExtendedFloatingActionButton(
        onClick = { onCreate(state.kind) },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = RoundedCornerShape(16.dp),
        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
        text = { Text(if (state.kind == NoteKind.POST) "写文章" else "发说说", fontWeight = FontWeight.SemiBold) },
      )
    },
    containerColor = MaterialTheme.colorScheme.background,
  ) { padding ->
    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
      KindTabs(selected = state.kind, onSelect = viewModel::selectKind)

      AnimatedVisibility(visible = searching) {
        OutlinedTextField(
          value = state.query,
          onValueChange = viewModel::setQuery,
          placeholder = { Text("搜索标题或文件名") },
          leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
          singleLine = true,
          shape = MaterialTheme.shapes.medium,
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
          ),
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        )
      }

      if (state.loading) {
        LinearProgressIndicator(
          modifier = Modifier.fillMaxWidth(),
          color = MaterialTheme.colorScheme.primary,
          trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
      }

      val notes = state.visibleNotes
      val drafts = state.visibleDrafts

      if (notes.isEmpty() && drafts.isEmpty() && !state.loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          EmptyState(
            icon = if (state.kind == NoteKind.POST) Icons.Outlined.Article else Icons.Outlined.ChatBubbleOutline,
            title = if (state.query.isNotBlank()) "没有匹配的内容" else "还没有内容",
            hint = if (state.query.isNotBlank()) "换个关键词试试" else "点击右下角开始第一篇",
          )
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          if (drafts.isNotEmpty()) {
            item(key = "draft-label") {
              SectionLabel("未同步草稿", modifier = Modifier.padding(start = 4.dp))
            }
            items(drafts, key = { it.key }) { draft ->
              DraftRow(
                draft = draft,
                onOpen = { onOpenDraft(draft) },
                onDiscard = { viewModel.discardDraft(draft) },
              )
            }
            item(key = "cloud-label") {
              SectionLabel("云端内容", modifier = Modifier.padding(start = 4.dp))
            }
          }

          items(notes, key = { it.name }) { note ->
            NoteRow(
              note = note,
              hasDraft = state.hasDraftFor(note),
              onOpen = { onOpenNote(state.kind, note) },
              onDelete = { pendingDelete = note },
            )
          }
        }
      }
    }
  }

  pendingDelete?.let { note ->
    AlertDialog(
      onDismissRequest = { pendingDelete = null },
      title = { Text("删除「${note.displayTitle}」？") },
      text = { Text("将从云端永久删除这条内容，无法撤销。") },
      confirmButton = {
        TextButton(onClick = {
          viewModel.delete(note)
          pendingDelete = null
        }) {
          Text("删除", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
        }
      },
      dismissButton = {
        TextButton(onClick = { pendingDelete = null }) { Text("取消") }
      },
      shape = MaterialTheme.shapes.large,
    )
  }
}

@Composable
private fun KindTabs(selected: NoteKind, onSelect: (NoteKind) -> Unit) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 6.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    NoteKind.entries.forEach { kind ->
      val active = kind == selected
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        border = if (active) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.weight(1f).clickable { onSelect(kind) },
      ) {
        Text(
          text = kind.label,
          style = MaterialTheme.typography.labelLarge,
          fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
          color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = androidx.compose.ui.text.style.TextAlign.Center,
          modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        )
      }
    }
  }
}

@Composable
private fun NoteRow(
  note: NoteSummary,
  hasDraft: Boolean,
  onOpen: () -> Unit,
  onDelete: () -> Unit,
) {
  FlatCard(modifier = Modifier.fillMaxWidth(), onClick = onOpen) {
    Row(
      modifier = Modifier.padding(start = 16.dp, end = 6.dp, top = 14.dp, bottom = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = note.displayTitle,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
          )
          if (hasDraft) {
            Spacer(Modifier.width(8.dp))
            Pill(
              text = "有本地改动",
              color = MaterialTheme.colorScheme.onTertiaryContainer,
              container = MaterialTheme.colorScheme.tertiaryContainer,
            )
          }
        }
        Spacer(Modifier.height(4.dp))
        Text(
          text = note.date?.let { prettyDate(it) } ?: note.name,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      IconButton(onClick = onDelete) {
        Icon(
          Icons.Outlined.Delete,
          contentDescription = "删除",
          tint = MaterialTheme.colorScheme.outline,
          modifier = Modifier.size(19.dp),
        )
      }
    }
  }
}

@Composable
private fun DraftRow(draft: Draft, onOpen: () -> Unit, onDiscard: () -> Unit) {
  FlatCard(
    modifier = Modifier.fillMaxWidth(),
    onClick = onOpen,
    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f),
  ) {
    Row(
      modifier = Modifier.padding(start = 16.dp, end = 6.dp, top = 14.dp, bottom = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = Icons.Outlined.DriveFileRenameOutline,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.tertiary,
        modifier = Modifier.size(19.dp),
      )
      Spacer(Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = draftTitle(draft),
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(3.dp))
        Text(
          text = "本地保存于 ${prettyTimestamp(draft.updatedAt)}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      IconButton(onClick = onDiscard) {
        Icon(
          Icons.Outlined.CloudOff,
          contentDescription = "丢弃草稿",
          tint = MaterialTheme.colorScheme.outline,
          modifier = Modifier.size(18.dp),
        )
      }
    }
  }
}

private fun draftTitle(draft: Draft): String {
  val fromFrontmatter = Regex("^title:\\s*(.+)$", RegexOption.MULTILINE)
    .find(draft.markdown)
    ?.groupValues
    ?.getOrNull(1)
    ?.trim()
    ?.trim('"', '\'')
  if (!fromFrontmatter.isNullOrBlank()) return fromFrontmatter
  draft.filename?.let { return it.removeSuffix(".md") }
  val firstLine = draft.markdown.lineSequence()
    .map { it.trim() }
    .firstOrNull { it.isNotEmpty() && !it.startsWith("---") && !it.contains(": ") }
  return firstLine?.take(40)?.trimStart('#', ' ') ?: "未命名草稿"
}

private fun prettyDate(raw: String): String = raw.replace('T', ' ').take(16)

private fun prettyTimestamp(millis: Long): String =
  SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(millis))
