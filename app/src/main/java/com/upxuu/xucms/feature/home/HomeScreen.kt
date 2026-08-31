package com.upxuu.xucms.feature.home

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.upxuu.xucms.LocalAppContainer
import com.upxuu.xucms.data.Draft
import com.upxuu.xucms.data.NoteKind
import com.upxuu.xucms.data.NoteSummary
import com.upxuu.xucms.ui.components.EmptyState
import com.upxuu.xucms.ui.components.FlatCard
import com.upxuu.xucms.ui.components.Pill
import com.upxuu.xucms.ui.components.QuickMetaSheet
import com.upxuu.xucms.ui.components.SectionLabel
import com.upxuu.xucms.ui.components.SwipeActionRow
import com.upxuu.xucms.ui.rememberViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The list. Right-swipe deletes (with a five-second undo), left-swipe opens the
 * note's frontmatter. Only never-published drafts get their own section; a
 * published note with local changes is marked in place instead.
 */
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

  // Returning from the editor may have created or cleared drafts.
  LifecycleResumeEffect(Unit) {
    viewModel.reloadDrafts()
    onPauseOrDispose { }
  }

  LaunchedEffect(state.error) {
    state.error?.let {
      snackbar.showSnackbar(it)
      viewModel.dismissError()
    }
  }
  LaunchedEffect(state.message) {
    state.message?.let {
      snackbar.showSnackbar(it)
      viewModel.dismissMessage()
    }
  }

  // The undo snackbar is the confirmation step: dismissing it commits the delete.
  LaunchedEffect(state.pendingDelete) {
    val pending = state.pendingDelete ?: return@LaunchedEffect
    val result = snackbar.showSnackbar(
      message = "已删除「${pending.label}」",
      actionLabel = "撤销",
      withDismissAction = false,
      duration = SnackbarDuration.Short,
    )
    if (result == SnackbarResult.ActionPerformed) {
      viewModel.undoDelete()
    } else {
      viewModel.commitPendingDelete()
    }
  }

  Scaffold(
    snackbarHost = { SnackbarHost(snackbar) },
    containerColor = MaterialTheme.colorScheme.background,
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
        navigationIcon = {
          IconButton(onClick = viewModel::refresh) {
            Icon(Icons.Outlined.Refresh, contentDescription = "刷新")
          }
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
        text = {
          Text(
            text = if (state.kind == NoteKind.POST) "写文章" else "发说说",
            fontWeight = FontWeight.SemiBold,
          )
        },
      )
    },
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
              SectionLabel("尚未发布", modifier = Modifier.padding(start = 4.dp))
            }
            items(drafts, key = { it.id }) { draft ->
              SwipeActionRow(
                onDelete = { viewModel.requestDelete(draft) },
                onSettings = { onOpenDraft(draft) },
                settingsLabel = "继续写",
              ) {
                DraftRow(draft = draft, onOpen = { onOpenDraft(draft) })
              }
            }
            item(key = "cloud-label") {
              SectionLabel("云端内容", modifier = Modifier.padding(start = 4.dp))
            }
          }

          items(notes, key = { it.name }) { note ->
            SwipeActionRow(
              onDelete = { viewModel.requestDelete(note) },
              onSettings = { viewModel.openQuickMeta(note) },
            ) {
              NoteRow(
                note = note,
                hasLocalChanges = state.hasLocalChanges(note),
                onOpen = { onOpenNote(state.kind, note) },
              )
            }
          }

          item(key = "swipe-hint") {
            Text(
              text = "右滑删除，左滑编辑属性",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.outline,
              textAlign = TextAlign.Center,
              modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
          }
        }
      }
    }
  }

  state.quickMeta?.let { quick ->
    QuickMetaSheet(
      kind = quick.kind,
      title = quick.summary.displayTitle,
      postMeta = quick.postMeta,
      talkMeta = quick.talkMeta,
      loading = quick.loading,
      saving = quick.saving,
      onPostChange = viewModel::updateQuickPost,
      onTalkChange = viewModel::updateQuickTalk,
      onSave = viewModel::saveQuickMeta,
      onDismiss = viewModel::closeQuickMeta,
    )
  }
}

@Composable
private fun KindTabs(selected: NoteKind, onSelect: (NoteKind) -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    NoteKind.entries.forEach { kind ->
      val active = kind == selected
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        border = if (active) {
          null
        } else {
          androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        },
        modifier = Modifier.weight(1f).clickable { onSelect(kind) },
      ) {
        Text(
          text = kind.label,
          style = MaterialTheme.typography.labelLarge,
          fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
          color = if (active) {
            MaterialTheme.colorScheme.onPrimary
          } else {
            MaterialTheme.colorScheme.onSurfaceVariant
          },
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        )
      }
    }
  }
}

@Composable
private fun NoteRow(
  note: NoteSummary,
  hasLocalChanges: Boolean,
  onOpen: () -> Unit,
) {
  FlatCard(modifier = Modifier.fillMaxWidth(), onClick = onOpen) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = note.displayTitle,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f, fill = false),
        )
        if (hasLocalChanges) {
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
  }
}

@Composable
private fun DraftRow(draft: Draft, onOpen: () -> Unit) {
  FlatCard(
    modifier = Modifier.fillMaxWidth(),
    onClick = onOpen,
    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
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
          text = draftTitleOf(draft),
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
    }
  }
}

private fun prettyDate(raw: String): String = raw.replace('T', ' ').take(16)

private fun prettyTimestamp(millis: Long): String =
  SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(millis))
