package com.upxuu.xucms.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * A locally cached edit. Drafts are written to the app's private files dir as JSON
 * so a crash or process death never loses typing, and a draft is kept until the
 * matching save succeeds.
 */
@Serializable
data class Draft(
  val key: String,
  val kind: String,
  /** Remote filename, or null while the note has never been published. */
  val filename: String? = null,
  val sha: String? = null,
  val markdown: String,
  val updatedAt: Long,
) {
  val noteKind: NoteKind get() = runCatching { NoteKind.valueOf(kind) }.getOrDefault(NoteKind.POST)
}

/** File-backed draft store. Small enough that whole-file reads stay cheap. */
class DraftStore(context: Context) {

  private val dir = File(context.filesDir, "drafts").apply { mkdirs() }
  private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = false }

  fun keyFor(kind: NoteKind, filename: String?): String =
    "${kind.name}_${filename?.replace(Regex("[^A-Za-z0-9\\u4e00-\\u9fa5._-]"), "_") ?: "new"}"

  fun save(kind: NoteKind, filename: String?, sha: String?, markdown: String): Draft {
    val draft = Draft(
      key = keyFor(kind, filename),
      kind = kind.name,
      filename = filename,
      sha = sha,
      markdown = markdown,
      updatedAt = System.currentTimeMillis(),
    )
    runCatching {
      File(dir, "${draft.key}.json").writeText(json.encodeToString(Draft.serializer(), draft))
    }
    return draft
  }

  fun load(kind: NoteKind, filename: String?): Draft? = load(keyFor(kind, filename))

  fun load(key: String): Draft? = runCatching {
    val file = File(dir, "$key.json")
    if (!file.exists()) null else json.decodeFromString(Draft.serializer(), file.readText())
  }.getOrNull()

  fun clear(kind: NoteKind, filename: String?) {
    runCatching { File(dir, "${keyFor(kind, filename)}.json").delete() }
  }

  fun clear(key: String) {
    runCatching { File(dir, "$key.json").delete() }
  }

  fun all(): List<Draft> = runCatching {
    dir.listFiles { f -> f.extension == "json" }
      ?.mapNotNull { file -> runCatching { json.decodeFromString(Draft.serializer(), file.readText()) }.getOrNull() }
      ?.sortedByDescending { it.updatedAt }
      ?: emptyList()
  }.getOrDefault(emptyList())

  fun keys(): Set<String> = all().map { it.key }.toSet()
}
