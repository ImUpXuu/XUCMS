package com.upxuu.xucms.feature.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.upxuu.xucms.data.GalleryImage
import com.upxuu.xucms.data.XucmsApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GalleryUiState(
  val loading: Boolean = false,
  val images: List<GalleryImage> = emptyList(),
  val query: String = "",
  val selected: Set<String> = emptySet(),
  val error: String? = null,
) {
  val visible: List<GalleryImage>
    get() {
      val needle = query.trim()
      return if (needle.isEmpty()) images
      else images.filter { it.name.contains(needle, ignoreCase = true) || it.path.contains(needle, ignoreCase = true) }
    }
}

class GalleryViewModel(private val api: XucmsApi) : ViewModel() {

  private val _state = MutableStateFlow(GalleryUiState())
  val state: StateFlow<GalleryUiState> = _state.asStateFlow()

  init {
    refresh()
  }

  fun refresh() {
    viewModelScope.launch {
      _state.update { it.copy(loading = true, error = null) }
      val result = api.images()
      _state.update { current ->
        current.copy(
          loading = false,
          images = result.getOrElse { current.images }
            .sortedByDescending { image -> image.path },
          error = result.exceptionOrNull()?.message,
        )
      }
    }
  }

  fun setQuery(value: String) = _state.update { it.copy(query = value) }

  fun toggle(path: String) = _state.update { current ->
    current.copy(
      selected = if (path in current.selected) current.selected - path else current.selected + path,
    )
  }

  fun clearSelection() = _state.update { it.copy(selected = emptySet()) }

  fun urlFor(path: String): String = api.imageUrl(path)

  fun delete(image: GalleryImage) {
    viewModelScope.launch {
      _state.update { it.copy(loading = true) }
      val result = api.deleteImage(image.path, image.sha)
      if (result.isSuccess) {
        _state.update { it.copy(selected = it.selected - image.path) }
        refresh()
      } else {
        _state.update {
          it.copy(loading = false, error = result.exceptionOrNull()?.message ?: "删除失败")
        }
      }
    }
  }

  fun dismissError() = _state.update { it.copy(error = null) }
}
