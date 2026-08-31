package com.upxuu.xucms.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A markdown file listed by the CMS. `type` distinguishes posts from talks. */
@Serializable
data class NoteSummary(
  val name: String,
  val sha: String,
  val title: String? = null,
  val date: String? = null,
  val type: String? = null,
) {
  val displayTitle: String
    get() = title?.takeIf { it.isNotBlank() } ?: name.removeSuffix(".md")
}

@Serializable
data class NoteContent(val content: String, val sha: String)

@Serializable
data class NotePutBody(val content: String, val sha: String? = null)

@Serializable
data class ShaHolder(val sha: String)

@Serializable
data class NotePutResponse(val content: ShaHolder? = null, val sha: String? = null) {
  val newSha: String? get() = content?.sha ?: sha
}

@Serializable
data class DeleteBody(val sha: String)

@Serializable
data class GalleryImage(val name: String, val path: String, val sha: String)

@Serializable
data class UploadImageBody(val filename: String, val content: String)

@Serializable
data class UploadImageResponse(@SerialName("url") val url: String? = null)

/** What kind of document a screen is editing. Endpoints differ only by segment. */
enum class NoteKind(val listPath: String, val itemPath: String, val label: String) {
  POST("/api/posts", "/api/post", "文章"),
  TALK("/api/talks", "/api/talk", "说说"),
}
