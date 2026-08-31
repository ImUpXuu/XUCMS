package com.upxuu.xucms.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/** Autosaved snapshot versus one the user explicitly created. */
enum class DraftKind { AUTO, MANUAL }

/**
 * A locally cached edit. Drafts live as JSON in the app's private files dir so a
 * crash or process death never loses typing.
 *
 * Two situations are deliberately distinguished by [filename]:
 * - `null` — the note has never been published, so this draft *is* the only copy.
 * - non-null — a published note has local changes; the cloud copy still exists.
 */
@Serializable
data class Draft(
  val id: String,
  val kind: String,
  val draftKind: String = DraftKind.AUTO.name,
  /** Remote filename, or null while the note has never been published. */
  val filename: String? = null,
  val sha: String? = null,
  val markdown: String,
  val updatedAt: Long,
  /** User-visible name for MANUAL snapshots. */
  val label: String = "",
) {
  val noteKind: NoteKind get() = runCatching { NoteKind.valueOf(kind) }.getOrDefault(NoteKind.POST)
  val type: DraftKind get() = runCatching { DraftKind.valueOf(draftKind) }.getOrDefault(DraftKind.AUTO)
  val isUnpublished: Boolean get() = filename == null
}

/** Shape of drafts written by the first release, kept only for migration. */
@Serializable
private data class LegacyDraft(
  val key: String = "",
  val kind: String = NoteKind.POST.name,
  val filename: String? = null,
  val sha: String? = null,
  val markdown: String = "",
  val updatedAt: Long = 0L,
)

/**
 * File-backed draft store. One AUTO draft per note (overwritten by the autosave
 * timer) plus any number of MANUAL snapshots.
 */
class DraftStore(context: Context) {

  private val dir = File(context.filesDir, "drafts").apply { mkdirs() }
  private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

  init {
    migrateLegacyDrafts()
  }

  /**
   * Drafts written before the AUTO/MANUAL split used a `key` field and no `id`,
   * which would fail to deserialize and silently lose the user's writing. Rewrite
   * them in place, treating each as the note's autosave slot.
   */
  private fun migrateLegacyDrafts() {
    runCatching {
      dir.listFiles { file -> file.extension == "json" } ?: return@runCatching
    }.getOrNull()?.forEach { file ->
      runCatching {
        val text = file.readText()
        if (text.contains("\"id\"")) return@runCatching
        val legacy = json.decodeFromString(LegacyDraft.serializer(), text)
        val kind = runCatching { NoteKind.valueOf(legacy.kind) }.getOrDefault(NoteKind.POST)
        val migrated = Draft(
          id = autoId(kind, legacy.filename),
          kind = kind.name,
          draftKind = DraftKind.AUTO.name,
          filename = legacy.filename,
          sha = legacy.sha,
          markdown = legacy.markdown,
          updatedAt = legacy.updatedAt,
        )
        fileFor(migrated.id).writeText(json.encodeToString(Draft.serializer(), migrated))
        if (file.name != "${migrated.id}.json") file.delete()
      }
    }
  }

  /** Stable identity of a note's autosave slot, so it is replaced not accumulated. */
  fun autoId(kind: NoteKind, filename: String?): String =
    "${kind.name}__auto__${slug(filename)}"

  private fun manualId(kind: NoteKind, filename: String?): String =
    "${kind.name}__manual__${slug(filename)}__${System.currentTimeMillis()}"

  private fun slug(filename: String?): String =
    filename?.replace(Regex("[^A-Za-z0-9\\u4e00-\\u9fa5._-]"), "_") ?: "new"

  private fun fileFor(id: String) = File(dir, "$id.json")

  fun saveAuto(kind: NoteKind, filename: String?, sha: String?, markdown: String): Draft =
    write(
      Draft(
        id = autoId(kind, filename),
        kind = kind.name,
        draftKind = DraftKind.AUTO.name,
        filename = filename,
        sha = sha,
        markdown = markdown,
        updatedAt = System.currentTimeMillis(),
      ),
    )

  fun saveManual(
    kind: NoteKind,
    filename: String?,
    sha: String?,
    markdown: String,
    label: String,
  ): Draft = write(
    Draft(
      id = manualId(kind, filename),
      kind = kind.name,
      draftKind = DraftKind.MANUAL.name,
      filename = filename,
      sha = sha,
      markdown = markdown,
      updatedAt = System.currentTimeMillis(),
      label = label,
    ),
  )

  private fun write(draft: Draft): Draft {
    runCatching { fileFor(draft.id).writeText(json.encodeToString(Draft.serializer(), draft)) }
    return draft
  }

  /** Restores a previously deleted draft verbatim, used by the undo window. */
  fun restore(draft: Draft) {
    write(draft)
  }

  fun load(id: String): Draft? = runCatching {
    val file = fileFor(id)
    if (!file.exists()) null else json.decodeFromString(Draft.serializer(), file.readText())
  }.getOrNull()

  fun loadAuto(kind: NoteKind, filename: String?): Draft? = load(autoId(kind, filename))

  /** Newest draft for a note regardless of kind — what "resume editing" should open. */
  fun latestFor(kind: NoteKind, filename: String?): Draft? =
    forNote(kind, filename).maxByOrNull { it.updatedAt }

  fun forNote(kind: NoteKind, filename: String?): List<Draft> =
    all().filter { it.noteKind == kind && it.filename == filename }

  fun delete(id: String) {
    runCatching { fileFor(id).delete() }
  }

  /** Clears every draft (auto and manual) belonging to one note. */
  fun deleteAllFor(kind: NoteKind, filename: String?) {
    forNote(kind, filename).forEach { delete(it.id) }
  }

  fun all(): List<Draft> = runCatching {
    dir.listFiles { file -> file.extension == "json" }
      ?.mapNotNull { file ->
        runCatching { json.decodeFromString(Draft.serializer(), file.readText()) }.getOrNull()
      }
      ?.sortedByDescending { it.updatedAt }
      ?: emptyList()
  }.getOrDefault(emptyList())

  /** Drafts for notes that exist only locally; the home list shows these on top. */
  fun unpublished(): List<Draft> = all().filter { it.isUnpublished }

  /** Filenames of published notes that currently carry local changes. */
  fun filenamesWithLocalChanges(kind: NoteKind): Set<String> =
    all().filter { it.noteKind == kind }.mapNotNull { it.filename }.toSet()
}
