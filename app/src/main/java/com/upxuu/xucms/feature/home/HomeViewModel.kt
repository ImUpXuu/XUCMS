package com.upxuu.xucms.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.upxuu.xucms.data.ApiException
import com.upxuu.xucms.data.Draft
import com.upxuu.xucms.data.DraftStore
import com.upxuu.xucms.data.NoteKind
import com.upxuu.xucms.data.NoteSummary
import com.upxuu.xucms.data.SettingsStore
import com.upxuu.xucms.data.XucmsApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
  val kind: NoteKind = NoteKind.POST,
  val loading: Boolean = false,
  val query: String = "",
  val posts: List<NoteSummary> = emptyList(),
  val talks: List<NoteSummary> = emptyList(),
  val drafts: List<Draft> = emptyList(),
  val error: String? = null,
  val sessionExpired: Boolean = false,
) {
  private val current: List<NoteSummary> get() = if (kind == NoteKind.POST) posts else talks

  /** Notes matching the search box, newest first. */
  val visibleNotes: List<NoteSummary>
    get() {
      val needle = query.trim()
      val filtered = if (needle.isEmpty()) current else current.filter {
        it.displayTitle.contains(needle, ignoreCase = true) || it.name.contains(needle, ignoreCase = true)
      }
      return filtered.sortedByDescending { it.date ?: it.name }
    }

  /** Unsynced drafts for the selected tab, so the user can resume anything. */
  val visibleDrafts: List<Draft>
    get() = drafts.filter { it.noteKind == kind }

  fun hasDraftFor(summary: NoteSummary): Boolean =
    drafts.any { it.noteKind == kind && it.filename == summary.name }
}

class HomeViewModel(
  private val api: XucmsApi,
  private val draftStore: DraftStore,
  private val settings: SettingsStore,
) : ViewModel() {

  private val _state = MutableStateFlow(HomeUiState())
  val state: StateFlow<HomeUiState> = _state.asStateFlow()

  init {
    refresh()
  }

  fun selectKind(kind: NoteKind) {
    _state.update { it.copy(kind = kind) }
    if (loadedFor(kind).isEmpty()) refresh()
  }

  private fun loadedFor(kind: NoteKind): List<NoteSummary> =
    if (kind == NoteKind.POST) _state.value.posts else _state.value.talks

  fun setQuery(value: String) {
    _state.update { it.copy(query = value) }
  }

  fun refresh() {
    viewModelScope.launch {
      _state.update { it.copy(loading = true, error = null) }
      val drafts = draftStore.all()
      val posts = api.list(NoteKind.POST)
      val talks = api.list(NoteKind.TALK)

      val failure = posts.exceptionOrNull() ?: talks.exceptionOrNull()
      val expired = (failure as? ApiException)?.code?.let { it == 401 || it == 403 } == true

      _state.update {
        it.copy(
          loading = false,
          drafts = drafts,
          posts = posts.getOrElse { _ -> it.posts },
          talks = talks.getOrElse { _ -> it.talks },
          error = failure?.message.takeIf { _ -> !expired },
          sessionExpired = expired,
        )
      }
      if (expired) settings.signOut()
    }
  }

  fun delete(summary: NoteSummary) {
    val kind = _state.value.kind
    viewModelScope.launch {
      _state.update { it.copy(loading = true) }
      val result = api.delete(kind, summary.name, summary.sha)
      if (result.isSuccess) {
        draftStore.clear(kind, summary.name)
        refresh()
      } else {
        _state.update {
          it.copy(loading = false, error = result.exceptionOrNull()?.message ?: "删除失败")
        }
      }
    }
  }

  fun discardDraft(draft: Draft) {
    draftStore.clear(draft.key)
    _state.update { current -> current.copy(drafts = current.drafts.filterNot { it.key == draft.key }) }
  }

  fun dismissError() {
    _state.update { it.copy(error = null) }
  }
}
