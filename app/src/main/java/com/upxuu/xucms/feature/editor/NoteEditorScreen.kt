package com.upxuu.xucms.feature.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.HistoryEdu
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.upxuu.xucms.LocalAppContainer
import com.upxuu.xucms.data.DraftKind
import com.upxuu.xucms.data.NoteKind
import com.upxuu.xucms.editor.model.InlineMark
import com.upxuu.xucms.editor.ui.EditorToolbar
import com.upxuu.xucms.editor.ui.MarkdownEditor
import com.upxuu.xucms.feature.gallery.GalleryPickerSheet
import com.upxuu.xucms.ui.components.ConfirmDeleteDialog
import com.upxuu.xucms.ui.components.Pill
import com.upxuu.xucms.ui.components.ThinDivider
import com.upxuu.xucms.ui.rememberViewModel

/**
 * The writing screen: a title line, the block editor, and one toolbar. Metadata,
 * drafts and the gallery live in sheets so they never compete with the text.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
  kind: NoteKind,
  filename: String?,
  initialSha: String?,
  onBack: () -> Unit,
  onSessionExpired: () -> Unit,
) {
  val container = LocalAppContainer.current
  val context = LocalContext.current
  val viewModel = rememberViewModel("editor-${kind.name}-${filename ?: "new"}") {
    EditorViewModel(kind, filename, initialSha, container.api, container.drafts, container.settings)
  }
  val state by viewModel.state.collectAsState()
  val editor = viewModel.editor
  val snackbar = remember { SnackbarHostState() }

  var showMeta by remember { mutableStateOf(false) }
  var showDrafts by remember { mutableStateOf(false) }
  var showGallery by remember { mutableStateOf(false) }
  var showLink by remember { mutableStateOf(false) }

  val imagePicker = rememberLauncherForActivityResult(
    ActivityResultContracts.GetMultipleContents(),
  ) { uris -> viewModel.uploadImages(context, uris) }

  // Every content edit re-evaluates whether the document really differs from the
  // loaded baseline; the ViewModel decides whether that warrants a draft.
  LaunchedEffect(editor) {
    snapshotFlow { editor.revision }.collect { viewModel.onContentChanged() }
  }

  LaunchedEffect(state.message) {
    state.message?.let {
      snackbar.showSnackbar(it)
      viewModel.dismissMessage()
    }
  }
  LaunchedEffect(state.error) {
    state.error?.let {
      snackbar.showSnackbar(it)
      viewModel.dismissError()
    }
  }
  LaunchedEffect(state.sessionExpired) {
    if (state.sessionExpired) onSessionExpired()
  }
  LaunchedEffect(state.finished) {
    if (state.finished) onBack()
  }

  // Dismissing the snackbar is what actually deletes the draft.
  LaunchedEffect(state.pendingDraftDelete) {
    val pending = state.pendingDraftDelete ?: return@LaunchedEffect
    val result = snackbar.showSnackbar(
      message = "已删除该草稿",
      actionLabel = "撤销",
      duration = SnackbarDuration.Short,
    )
    if (result == SnackbarResult.ActionPerformed) {
      viewModel.undoDraftDelete()
    } else {
      viewModel.commitPendingDraftDelete()
    }
  }

  Scaffold(
    snackbarHost = { SnackbarHost(snackbar) },
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = if (state.isNew) "新建${kind.label}" else kind.label,
            style = MaterialTheme.typography.titleMedium,
          )
        },
        navigationIcon = {
          IconButton(onClick = {
            viewModel.persistDraft(announce = false)
            onBack()
          }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
          }
        },
        actions = {
          AnimatedVisibility(
            visible = state.dirty,
            enter = fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.8f),
            exit = fadeOut(tween(140)) + scaleOut(tween(140), targetScale = 0.8f),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Pill(
                text = "未发布",
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                container = MaterialTheme.colorScheme.tertiaryContainer,
              )
              Spacer(Modifier.width(4.dp))
            }
          }
          IconButton(onClick = { showDrafts = true }) {
            Icon(Icons.Outlined.HistoryEdu, contentDescription = "草稿管理")
          }
          IconButton(onClick = { showMeta = true }) {
            Icon(Icons.Outlined.Tune, contentDescription = "属性")
          }
          TextButton(
            onClick = viewModel::publish,
            enabled = state.stage != SaveStage.SAVING && !state.loading,
          ) {
            if (state.stage == SaveStage.SAVING) {
              CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
              Text("发布", fontWeight = FontWeight.Bold)
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background,
        ),
      )
    },
    bottomBar = {
      Column(modifier = Modifier.imePadding()) {
        EditorToolbar(
          state = editor,
          onPickImage = { imagePicker.launch("image/*") },
          onOpenGallery = { showGallery = true },
          onEditLink = { showLink = true },
        )
      }
    },
  ) { padding ->
    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
      if (state.loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator()
        }
        return@Column
      }

      state.uploading?.let { (current, total) ->
        LinearProgressIndicator(
          progress = { current.toFloat() / total },
          modifier = Modifier.fillMaxWidth(),
        )
        Text(
          text = "正在上传图片 $current/$total",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp),
        )
      }

      AnimatedVisibility(
        visible = state.restoredDraft,
        enter = expandVertically(tween(220)) + fadeIn(tween(220)),
        exit = shrinkVertically(tween(180)) + fadeOut(tween(120)),
      ) {
        Surface(color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(start = 18.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "已恢复本地未发布的内容",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { showDrafts = true }) { Text("查看草稿") }
            TextButton(onClick = viewModel::dismissRestoredBanner) { Text("知道了") }
          }
        }
      }

      TitleField(
        state = state,
        onPostTitle = { value -> viewModel.updatePost { it.copy(title = value) } },
        onTalkTitle = { value -> viewModel.updateTalk { it.copy(title = value) } },
      )

      Text(
        text = state.targetFilename(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 2.dp),
      )

      Spacer(Modifier.height(6.dp))
      ThinDivider()

      MarkdownEditor(
        state = editor,
        modifier = Modifier.fillMaxSize(),
        placeholder = if (kind == NoteKind.POST) "从这里开始写…" else "此刻在想什么？",
      )
    }
  }

  if (showMeta) {
    MetaSheet(
      state = state,
      onDismiss = { showMeta = false },
      onPostChange = viewModel::updatePost,
      onTalkChange = viewModel::updateTalk,
      onFilenameChange = viewModel::setCustomFilename,
    )
  }

  if (showDrafts) {
    DraftSheet(
      drafts = state.visibleDrafts,
      onDismiss = { showDrafts = false },
      onCreateSnapshot = viewModel::createManualDraft,
      onRestore = { draft ->
        viewModel.restoreDraft(draft)
        showDrafts = false
      },
      onDelete = viewModel::askDraftDelete,
    )
  }

  state.draftDeleteRequest?.let { draft ->
    ConfirmDeleteDialog(
      title = if (draft.type == DraftKind.AUTO) "删除自动草稿？" else "删除这份快照？",
      message = "草稿只保存在本机，删除后仍有几秒可以撤销。",
      onConfirm = viewModel::confirmDraftDelete,
      onDismiss = viewModel::cancelDraftDelete,
    )
  }

  if (showGallery) {
    GalleryPickerSheet(
      onDismiss = { showGallery = false },
      onPick = { paths ->
        viewModel.insertGalleryImages(paths)
        showGallery = false
      },
    )
  }

  if (showLink) {
    val existing = editor.linkAtCaret()
    val selection = editor.focusedBlock()?.value?.selection
    LinkDialog(
      initialHref = existing?.href.orEmpty(),
      hasSelection = selection?.collapsed == false,
      onDismiss = { showLink = false },
      onApply = { href ->
        editor.toggleInline(InlineMark.LINK, href)
        showLink = false
      },
      onRemove = {
        editor.toggleInline(InlineMark.LINK)
        showLink = false
      },
    )
  }
}

@Composable
private fun TitleField(
  state: EditorUiState,
  onPostTitle: (String) -> Unit,
  onTalkTitle: (String) -> Unit,
) {
  val value = if (state.kind == NoteKind.POST) state.postMeta.title else state.talkMeta.title
  val placeholder = if (state.kind == NoteKind.POST) "标题" else "标题（可留空）"
  Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp)) {
    if (value.isEmpty()) {
      Text(
        text = placeholder,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
      )
    }
    BasicTextField(
      value = value,
      onValueChange = { if (state.kind == NoteKind.POST) onPostTitle(it) else onTalkTitle(it) },
      textStyle = MaterialTheme.typography.headlineMedium.copy(
        color = MaterialTheme.colorScheme.onSurface,
      ),
      cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
      modifier = Modifier.fillMaxWidth(),
    )
  }
}
