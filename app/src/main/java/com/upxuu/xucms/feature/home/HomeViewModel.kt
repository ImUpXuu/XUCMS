package com.upxuu.xucms.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.upxuu.xucms.data.ApiException
import com.upxuu.xucms.data.Draft
import com.upxuu.xucms.data.DraftStore
import com.upxuu.xucms.data.NoteCodec
import com.upxuu.xucms.data.NoteKind
import com.upxuu.xucms.data.NoteSummary
import com.upxuu.xucms.data.PostMeta
import com.upxuu.xucms.data.SettingsStore
import com.upxuu.xucms.data.TalkMeta
import com.upxuu.xucms.data.XucmsApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A delete waiting out its undo window; nothing has been removed yet. */
data class PendingDelete(
  val note: NoteSummary? = null,
  val draft: Draft? = null,
) {
  val label: String get() = note?.displayTitle ?: draft?.let { draftTitleOf(it) } ?: ""
}

/** A delete the user has to confirm in a dialog before the undo window starts. */
data class DeleteRequest(
  val note: NoteSummary? = null,
  val draft: Draft? = null,
) {
  val label: String get() = note?.displayTitle ?: draft?.let { draftTitleOf(it) } ?: ""

  val title: String get() = if (note != null) "删除这篇内容？" else "删除这份草稿？"

  val message: String
    get() = if (note != null) {
      "「$label」会从云端删除，本地相关草稿也会一并清除。删除后仍有几秒可以撤销。"
    } else {
      "「$label」只保存在本机，删除后仍有几秒可以撤销。"
    }
}

/** Metadata of one note, loaded on demand for the quick-edit sheet. */
data class QuickMeta(
  val summary: NoteSummary,
  val kind: NoteKind,
  val postMeta: PostMeta = PostMeta(),
  val talkMeta: TalkMeta = TalkMeta(),
  val body: String = "",
  val sha: String? = null,
  val loading: Boolean = true,
  val saving: Boolean = false,
)

data class HomeUiState(
  val kind: NoteKind = NoteKind.POST,
  val loading: Boolean = false,
  val query: String = "",
  val posts: List<NoteSummary> = emptyList(),
  val talks: List<NoteSummary> = emptyList(),
  /** Only drafts of notes that were never published; published notes get a pill. */
  val unpublishedDrafts: List<Draft> = emptyList(),
  val filenamesWithChanges: Set<String> = emptySet(),
  val deleteRequest: DeleteRequest? = null,
  val pendingDelete: PendingDelete? = null,
  val quickMeta: QuickMeta? = null,
  val error: String? = null,
  val message: String? = null,
  val sessionExpired: Boolean = false,
) {
  private val current: List<NoteSummary> get() = if (kind == NoteKind.POST) posts else talks

  val visibleNotes: List<NoteSummary>
    get() {
      val needle = query.trim()
      val hiddenName = pendingDelete?.note?.name
      val filtered = current
        .filter { it.name != hiddenName }
        .filter {
          needle.isEmpty() ||
            it.displayTitle.contains(needle, ignoreCase = true) ||
            it.name.contains(needle, ignoreCase = true)
        }
      return filtered.sortedByDescending { it.date ?: it.name }
    }

  val visibleDrafts: List<Draft>
    get() {
      val hiddenId = pendingDelete?.draft?.id
      return unpublishedDrafts.filter { it.noteKind == kind && it.id != hiddenId }
    }

  fun hasLocalChanges(summary: NoteSummary): Boolean = summary.name in filenamesWithChanges
}

class HomeViewModel(
  private val api: XucmsApi,
  private val draftStore: DraftStore,
  private val settings: SettingsStore,
) : ViewModel() {

  private val _state = MutableStateFlow(HomeUiState())
  val state: StateFlow<HomeUiState> = _state.asStateFlow()

  private var undoJob: Job? = null

  init {
    refresh()
  }

  fun selectKind(kind: NoteKind) {
    _state.update { it.copy(kind = kind) }
  }

  fun setQuery(value: String) = _state.update { it.copy(query = value) }

  fun refresh() {
    viewModelScope.launch {
      _state.update { it.copy(loading = true, error = null) }
      val posts = api.list(NoteKind.POST)
      val talks = api.list(NoteKind.TALK)

      val failure = posts.exceptionOrNull() ?: talks.exceptionOrNull()
      val expired = (failure as? ApiException)?.code?.let { it == 401 || it == 403 } == true

      _state.update { current ->
        current.copy(
          loading = false,
          posts = posts.getOrElse { current.posts },
          talks = talks.getOrElse { current.talks },
          unpublishedDrafts = draftStore.unpublished(),
          filenamesWithChanges = draftStore.filenamesWithLocalChanges(NoteKind.POST) +
            draftStore.filenamesWithLocalChanges(NoteKind.TALK),
          error = failure?.message.takeIf { !expired },
          sessionExpired = expired,
        )
      }
      if (expired) settings.signOut()
    }
  }

  fun reloadDrafts() {
    _state.update {
      it.copy(
        unpublishedDrafts = draftStore.unpublished(),
        filenamesWithChanges = draftStore.filenamesWithLocalChanges(NoteKind.POST) +
          draftStore.filenamesWithLocalChanges(NoteKind.TALK),
      )
    }
  }

  // ------------------------------------------------------- delete with undo

  /** Swiping only asks; the dialog is what starts a delete. */
  fun askDelete(note: NoteSummary) = _state.update { it.copy(deleteRequest = DeleteRequest(note = note)) }

  fun askDelete(draft: Draft) = _state.update { it.copy(deleteRequest = DeleteRequest(draft = draft)) }

  fun cancelDeleteRequest() = _state.update { it.copy(deleteRequest = null) }

  /**
   * Confirmed: hide the row and start a five-second window. Nothing is actually
   * removed until the window closes, so the snackbar's 撤销 still works.
   */
  fun confirmDelete() {
    val request = _state.value.deleteRequest ?: return
    _state.update { it.copy(deleteRequest = null) }
    startUndoWindow(PendingDelete(note = request.note, draft = request.draft))
  }

  private fun startUndoWindow(pending: PendingDelete) {
    // Commit whatever is already waiting; only one row can be pending at a time.
    undoJob?.cancel()
    _state.value.pendingDelete?.let { commit(it) }

    _state.update { it.copy(pendingDelete = pending) }
    undoJob = viewModelScope.launch {
      delay(UNDO_WINDOW_MILLIS)
      commit(pending)
    }
  }

  fun undoDelete() {
    undoJob?.cancel()
    undoJob = null
    _state.update { it.copy(pendingDelete = null) }
  }

  /** Deletes now instead of waiting, used when the snackbar is dismissed. */
  fun commitPendingDelete() {
    undoJob?.cancel()
    undoJob = null
    _state.value.pendingDelete?.let { commit(it) }
  }

  private fun commit(pending: PendingDelete) {
    val kind = _state.value.kind
    _state.update { if (it.pendingDelete == pending) it.copy(pendingDelete = null) else it }

    pending.draft?.let { draft ->
      draftStore.delete(draft.id)
      reloadDrafts()
      return
    }

    val note = pending.note ?: return
    viewModelScope.launch {
      val result = api.delete(kind, note.name, note.sha)
      if (result.isSuccess) {
        draftStore.deleteAllFor(kind, note.name)
        refresh()
      } else {
        _state.update {
          it.copy(error = result.exceptionOrNull()?.message ?: "删除失败，内容已保留")
        }
        refresh()
      }
    }
  }

  // ------------------------------------------------------------ quick meta

  /** Loads just enough of a note to edit its frontmatter without opening the editor. */
  fun openQuickMeta(note: NoteSummary) {
    val kind = _state.value.kind
    _state.update { it.copy(quickMeta = QuickMeta(summary = note, kind = kind)) }
    viewModelScope.launch {
      val result = api.load(kind, note.name)
      if (result.isFailure) {
        _state.update {
          it.copy(quickMeta = null, error = result.exceptionOrNull()?.message ?: "无法读取内容")
        }
        return@launch
      }
      val content = result.getOrThrow()
      _state.update { current ->
        val quick = current.quickMeta ?: return@update current
        when (kind) {
          NoteKind.POST -> {
            val (meta, body) = NoteCodec.parsePost(content.content)
            current.copy(quickMeta = quick.copy(postMeta = meta, body = body, sha = content.sha, loading = false))
          }
          NoteKind.TALK -> {
            val (meta, body) = NoteCodec.parseTalk(content.content)
            current.copy(quickMeta = quick.copy(talkMeta = meta, body = body, sha = content.sha, loading = false))
          }
        }
      }
    }
  }

  fun updateQuickPost(transform: (PostMeta) -> PostMeta) = _state.update { current ->
    val quick = current.quickMeta ?: return@update current
    current.copy(quickMeta = quick.copy(postMeta = transform(quick.postMeta)))
  }

  fun updateQuickTalk(transform: (TalkMeta) -> TalkMeta) = _state.update { current ->
    val quick = current.quickMeta ?: return@update current
    current.copy(quickMeta = quick.copy(talkMeta = transform(quick.talkMeta)))
  }

  fun closeQuickMeta() = _state.update { it.copy(quickMeta = null) }

  fun saveQuickMeta() {
    val quick = _state.value.quickMeta ?: return
    viewModelScope.launch {
      _state.update { current ->
        current.copy(quickMeta = current.quickMeta?.copy(saving = true))
      }
      val payload = when (quick.kind) {
        NoteKind.POST -> NoteCodec.buildPost(quick.postMeta, quick.body)
        NoteKind.TALK -> NoteCodec.buildTalk(quick.talkMeta, quick.body)
      }
      val result = api.save(quick.kind, quick.summary.name, payload, quick.sha)
      if (result.isSuccess) {
        _state.update { it.copy(quickMeta = null, message = "属性已更新") }
        refresh()
      } else {
        _state.update { current ->
          current.copy(
            quickMeta = current.quickMeta?.copy(saving = false),
            error = result.exceptionOrNull()?.message ?: "保存失败",
          )
        }
      }
    }
  }

  fun dismissError() = _state.update { it.copy(error = null) }

  fun dismissMessage() = _state.update { it.copy(message = null) }

  companion object {
    const val UNDO_WINDOW_MILLIS = 5_000L
  }
}

/** Best-effort display name for a draft that has no remote filename yet. */
fun draftTitleOf(draft: Draft): String {
  if (draft.label.isNotBlank()) return draft.label
  Regex("^title:\\s*(.+)$", RegexOption.MULTILINE).find(draft.markdown)
    ?.groupValues?.getOrNull(1)
    ?.trim()
    ?.trim('"', '\'')
    ?.takeIf { it.isNotBlank() }
    ?.let { return it }
  draft.filename?.let { return it.removeSuffix(".md") }
  val firstLine = draft.markdown.lineSequence()
    .map { it.trim() }
    .firstOrNull { it.isNotEmpty() && !it.startsWith("---") && !it.contains(": ") }
  return firstLine?.trimStart('#', ' ')?.take(40) ?: "未命名草稿"
}
