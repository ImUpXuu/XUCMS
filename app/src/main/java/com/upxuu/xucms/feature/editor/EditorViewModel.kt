package com.upxuu.xucms.feature.editor

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.upxuu.xucms.data.ApiException
import com.upxuu.xucms.data.Draft
import com.upxuu.xucms.data.DraftStore
import com.upxuu.xucms.data.ImagePipeline
import com.upxuu.xucms.data.NoteCodec
import com.upxuu.xucms.data.NoteKind
import com.upxuu.xucms.data.PostMeta
import com.upxuu.xucms.data.SettingsStore
import com.upxuu.xucms.data.TalkMeta
import com.upxuu.xucms.data.XucmsApi
import com.upxuu.xucms.editor.EditorState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SaveStage { IDLE, SAVING, SAVED, FAILED }

data class EditorUiState(
  val kind: NoteKind = NoteKind.POST,
  val loading: Boolean = false,
  val postMeta: PostMeta = PostMeta(),
  val talkMeta: TalkMeta = TalkMeta(),
  val filename: String? = null,
  val customFilename: String = "",
  val sha: String? = null,
  val stage: SaveStage = SaveStage.IDLE,
  val message: String? = null,
  val error: String? = null,
  val uploading: Pair<Int, Int>? = null,
  val restoredDraft: Boolean = false,
  val dirty: Boolean = false,
  val drafts: List<Draft> = emptyList(),
  val pendingDraftDelete: Draft? = null,
  val sessionExpired: Boolean = false,
  val finished: Boolean = false,
) {
  val isNew: Boolean get() = filename == null

  val title: String get() = if (kind == NoteKind.POST) postMeta.title else talkMeta.title

  val visibleDrafts: List<Draft>
    get() = drafts.filter { it.id != pendingDraftDelete?.id }

  /** Filename that will actually be written, shown live under the title field. */
  fun targetFilename(): String {
    filename?.let { return it }
    if (customFilename.isNotBlank()) return NoteCodec.ensureMd(customFilename)
    return when (kind) {
      NoteKind.POST -> NoteCodec.filenameFor(postMeta.title, postMeta.published)
      NoteKind.TALK -> NoteCodec.talkFilename(talkMeta.date)
    }
  }
}

class EditorViewModel(
  private val kind: NoteKind,
  private val filename: String?,
  initialSha: String?,
  private val api: XucmsApi,
  private val drafts: DraftStore,
  private val settings: SettingsStore,
) : ViewModel() {

  val editor = EditorState()

  private val _state = MutableStateFlow(
    EditorUiState(kind = kind, filename = filename, sha = initialSha, loading = filename != null),
  )
  val state: StateFlow<EditorUiState> = _state.asStateFlow()

  private var autosaveJob: Job? = null
  private var undoJob: Job? = null

  /**
   * Markdown as it was when the document was opened. Autosave compares against
   * this, so opening a note and leaving without typing creates no draft.
   */
  private var baseline: String = ""

  init {
    load()
  }

  // ------------------------------------------------------------------ loading

  private fun load() {
    viewModelScope.launch {
      val localDraft = drafts.latestFor(kind, filename)

      if (filename == null) {
        val source = localDraft?.markdown ?: defaultTemplate()
        applyMarkdown(source)
        baseline = if (localDraft != null) source else composeMarkdown()
        _state.update {
          it.copy(
            loading = false,
            restoredDraft = localDraft != null,
            sha = localDraft?.sha,
            drafts = drafts.forNote(kind, null),
          )
        }
        return@launch
      }

      val remote = api.load(kind, filename)
      if (remote.isFailure) {
        val failure = remote.exceptionOrNull()
        val expired = (failure as? ApiException)?.code?.let { it == 401 || it == 403 } == true
        if (expired) settings.signOut()
        // Fall back to the local draft so offline editing still works.
        val source = localDraft?.markdown ?: defaultTemplate()
        applyMarkdown(source)
        baseline = source
        _state.update {
          it.copy(
            loading = false,
            error = if (expired) null else failure?.message,
            sessionExpired = expired,
            restoredDraft = localDraft != null,
            drafts = drafts.forNote(kind, filename),
          )
        }
        return@launch
      }

      val content = remote.getOrThrow()
      // A draft that differs from the server copy wins: unsaved typing is worth
      // more than re-fetching what the user already had.
      val useDraft = localDraft != null && localDraft.markdown.trim() != content.content.trim()
      val source = if (useDraft) localDraft!!.markdown else content.content
      applyMarkdown(source)
      baseline = content.content
      _state.update {
        it.copy(
          loading = false,
          sha = content.sha,
          restoredDraft = useDraft,
          drafts = drafts.forNote(kind, filename),
        )
      }
    }
  }

  private fun defaultTemplate(): String = when (kind) {
    NoteKind.POST -> NoteCodec.buildPost(
      PostMeta(published = NoteCodec.now(), category = settings.defaultCategory),
      "",
    )
    NoteKind.TALK -> NoteCodec.buildTalk(TalkMeta(date = NoteCodec.now()), "")
  }

  private fun applyMarkdown(source: String) {
    when (kind) {
      NoteKind.POST -> {
        val (meta, body) = NoteCodec.parsePost(source)
        _state.update { it.copy(postMeta = meta.ensurePublished()) }
        editor.setMarkdown(body)
      }
      NoteKind.TALK -> {
        val (meta, body) = NoteCodec.parseTalk(source)
        _state.update { it.copy(talkMeta = meta.ensureDate()) }
        editor.setMarkdown(body)
      }
    }
  }

  private fun PostMeta.ensurePublished(): PostMeta =
    if (published.isBlank()) copy(published = NoteCodec.now()) else this

  private fun TalkMeta.ensureDate(): TalkMeta =
    if (date.isBlank()) copy(date = NoteCodec.now()) else this

  // ------------------------------------------------------------------ editing

  fun composeMarkdown(): String = when (kind) {
    NoteKind.POST -> NoteCodec.buildPost(_state.value.postMeta, editor.toMarkdown())
    NoteKind.TALK -> NoteCodec.buildTalk(_state.value.talkMeta, editor.toMarkdown())
  }

  /** True when the document actually differs from what was loaded. */
  private fun hasRealChanges(): Boolean = composeMarkdown().trim() != baseline.trim()

  fun updatePost(transform: (PostMeta) -> PostMeta) {
    _state.update { it.copy(postMeta = transform(it.postMeta)) }
    onContentChanged()
  }

  fun updateTalk(transform: (TalkMeta) -> TalkMeta) {
    _state.update { it.copy(talkMeta = transform(it.talkMeta)) }
    onContentChanged()
  }

  fun setCustomFilename(value: String) {
    _state.update { it.copy(customFilename = value) }
    onContentChanged()
  }

  /** Called on every content change; only arms autosave if something really changed. */
  fun onContentChanged() {
    val changed = hasRealChanges()
    _state.update { if (it.dirty == changed) it else it.copy(dirty = changed) }
    if (!changed) {
      autosaveJob?.cancel()
      return
    }
    val seconds = settings.autosaveSeconds
    if (seconds <= 0) return
    autosaveJob?.cancel()
    autosaveJob = viewModelScope.launch {
      delay(seconds * 1000L)
      persistDraft(announce = false)
    }
  }

  /** Writes the autosave slot, replacing whatever was there. No-op when unchanged. */
  fun persistDraft(announce: Boolean = true) {
    if (!hasRealChanges()) return
    val current = _state.value
    drafts.saveAuto(kind, current.filename, current.sha, composeMarkdown())
    _state.update {
      it.copy(
        drafts = drafts.forNote(kind, current.filename),
        message = if (announce) "已保存草稿" else it.message,
      )
    }
  }

  /** User-created snapshot; unlike the autosave slot these accumulate. */
  fun createManualDraft() {
    val current = _state.value
    val label = "手动草稿 ${SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date())}"
    drafts.saveManual(kind, current.filename, current.sha, composeMarkdown(), label)
    _state.update {
      it.copy(drafts = drafts.forNote(kind, current.filename), message = "已创建草稿快照")
    }
  }

  fun restoreDraft(draft: Draft) {
    applyMarkdown(draft.markdown)
    _state.update { it.copy(message = "已载入该草稿", dirty = hasRealChanges()) }
  }

  // -------------------------------------------------- draft delete with undo

  fun requestDraftDelete(draft: Draft) {
    undoJob?.cancel()
    _state.value.pendingDraftDelete?.let { commitDraftDelete(it) }
    _state.update { it.copy(pendingDraftDelete = draft) }
    undoJob = viewModelScope.launch {
      delay(UNDO_WINDOW_MILLIS)
      commitDraftDelete(draft)
    }
  }

  fun undoDraftDelete() {
    undoJob?.cancel()
    undoJob = null
    _state.update { it.copy(pendingDraftDelete = null) }
  }

  fun commitPendingDraftDelete() {
    undoJob?.cancel()
    undoJob = null
    _state.value.pendingDraftDelete?.let { commitDraftDelete(it) }
  }

  private fun commitDraftDelete(draft: Draft) {
    drafts.delete(draft.id)
    _state.update { current ->
      current.copy(
        pendingDraftDelete = if (current.pendingDraftDelete?.id == draft.id) null else current.pendingDraftDelete,
        drafts = drafts.forNote(kind, current.filename),
      )
    }
  }

  // -------------------------------------------------------------------- save

  fun publish() {
    val current = _state.value
    if (kind == NoteKind.POST && current.postMeta.title.isBlank()) {
      _state.update { it.copy(error = "请先填写标题") }
      return
    }
    if (editor.isBlank && kind == NoteKind.TALK) {
      _state.update { it.copy(error = "说说内容不能为空") }
      return
    }

    autosaveJob?.cancel()
    val target = current.targetFilename()
    val payload = composeMarkdown()

    viewModelScope.launch {
      _state.update { it.copy(stage = SaveStage.SAVING, error = null) }
      // Keep an autosave while the request is in flight so a crash mid-save is safe.
      drafts.saveAuto(kind, current.filename, current.sha, payload)

      val result = api.save(kind, target, payload, current.sha)
      if (result.isSuccess) {
        // Published: the autosave slot is obsolete. Manual snapshots are kept —
        // the user made those on purpose.
        drafts.delete(drafts.autoId(kind, current.filename))
        drafts.delete(drafts.autoId(kind, target))
        baseline = payload
        _state.update {
          it.copy(
            stage = SaveStage.SAVED,
            message = "已发布到云端",
            sha = result.getOrNull()?.newSha ?: it.sha,
            filename = target,
            dirty = false,
            finished = true,
          )
        }
      } else {
        val failure = result.exceptionOrNull()
        val expired = (failure as? ApiException)?.code?.let { it == 401 || it == 403 } == true
        if (expired) settings.signOut()
        _state.update {
          it.copy(
            stage = SaveStage.FAILED,
            error = if (expired) null else (failure?.message ?: "保存失败，草稿已留在本地"),
            sessionExpired = expired,
            drafts = drafts.forNote(kind, current.filename),
          )
        }
      }
    }
  }

  // ------------------------------------------------------------------ images

  fun uploadImages(context: Context, uris: List<Uri>) {
    if (uris.isEmpty()) return
    viewModelScope.launch {
      val urls = mutableListOf<String>()
      uris.forEachIndexed { index, uri ->
        _state.update { it.copy(uploading = (index + 1) to uris.size) }
        val prepared = ImagePipeline.prepare(context, uri)
        if (prepared == null) {
          _state.update { it.copy(error = "无法读取所选图片") }
          return@forEachIndexed
        }
        val uploaded = api.uploadImage(prepared.filename, prepared.base64)
        if (uploaded.isSuccess) {
          urls += uploaded.getOrThrow()
        } else {
          _state.update { it.copy(error = uploaded.exceptionOrNull()?.message ?: "图片上传失败") }
        }
      }
      _state.update { it.copy(uploading = null) }
      if (urls.isNotEmpty()) {
        editor.insertImages(urls)
        if (kind == NoteKind.POST && _state.value.postMeta.cover.isBlank()) {
          updatePost { it.copy(cover = urls.first()) }
        }
        onContentChanged()
      }
    }
  }

  fun insertGalleryImages(paths: List<String>) {
    if (paths.isEmpty()) return
    val urls = paths.map { api.imageUrl(it) }
    editor.insertImages(urls)
    if (kind == NoteKind.POST && _state.value.postMeta.cover.isBlank()) {
      updatePost { it.copy(cover = urls.first()) }
    }
    onContentChanged()
  }

  fun dismissMessage() = _state.update { it.copy(message = null) }

  fun dismissError() = _state.update { it.copy(error = null) }

  fun dismissRestoredBanner() = _state.update { it.copy(restoredDraft = false) }

  override fun onCleared() {
    super.onCleared()
    autosaveJob?.cancel()
    undoJob?.cancel()
  }

  companion object {
    const val UNDO_WINDOW_MILLIS = 5_000L
  }
}
