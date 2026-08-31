package com.upxuu.xucms.data

import com.upxuu.xucms.editor.markdown.Frontmatter
import com.upxuu.xucms.editor.markdown.FrontmatterCodec
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Editable metadata of a post, mapped to/from the site's frontmatter keys. */
data class PostMeta(
  val title: String = "",
  val published: String = "",
  val category: String = "",
  val tags: List<String> = emptyList(),
  val description: String = "",
  val cover: String = "",
  val draft: Boolean = false,
  val sticky: Int = 0,
  val extras: List<String> = emptyList(),
)

/** Editable metadata of a talk (a short, timeline-style note). */
data class TalkMeta(
  val title: String = "",
  val date: String = "",
  val tags: List<String> = emptyList(),
  val extras: List<String> = emptyList(),
)

object NoteCodec {

  private val POST_KEYS = setOf(
    "title", "published", "date", "category", "categories", "tags",
    "description", "image", "cover", "draft", "sticky",
  )
  private val TALK_KEYS = setOf("title", "date", "published", "tags")

  fun now(): String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

  /**
   * Frontmatter keys the app does not model are preserved verbatim so hand-written
   * fields (license, series, …) are not silently dropped when saving from mobile.
   */
  private fun passthrough(fm: Frontmatter, known: Set<String>): List<String> {
    val unknownFields = fm.fields.filterKeys { it !in known }
      .map { (key, value) -> "$key: $value" }
    val unknownLists = fm.lists.filterKeys { it !in known }
      .map { (key, values) -> "$key: [${values.joinToString(", ")}]" }
    return unknownFields + unknownLists + fm.extras
  }

  fun parsePost(source: String): Pair<PostMeta, String> {
    val doc = FrontmatterCodec.parse(source)
    val fm = doc.frontmatter
    val meta = PostMeta(
      title = fm["title"],
      published = fm["published"].ifBlank { fm["date"] },
      category = fm["category"].ifBlank { fm.list("categories").firstOrNull().orEmpty() },
      tags = fm.list("tags"),
      description = fm["description"],
      cover = fm["image"].ifBlank { fm["cover"] },
      draft = fm.bool("draft"),
      sticky = fm.int("sticky"),
      extras = passthrough(fm, POST_KEYS),
    )
    return meta to doc.body
  }

  fun buildPost(meta: PostMeta, body: String): String {
    val fields = linkedMapOf(
      "title" to meta.title,
      "published" to meta.published.ifBlank { now() },
    )
    if (meta.category.isNotBlank()) fields["category"] = meta.category
    if (meta.description.isNotBlank()) fields["description"] = meta.description
    if (meta.cover.isNotBlank()) fields["image"] = meta.cover
    if (meta.draft) fields["draft"] = "true"
    if (meta.sticky > 0) fields["sticky"] = meta.sticky.toString()

    return FrontmatterCodec.build(
      Frontmatter(fields = fields, lists = mapOf("tags" to meta.tags), extras = meta.extras),
      body,
    )
  }

  fun parseTalk(source: String): Pair<TalkMeta, String> {
    val doc = FrontmatterCodec.parse(source)
    val fm = doc.frontmatter
    val meta = TalkMeta(
      title = fm["title"],
      date = fm["date"].ifBlank { fm["published"] },
      tags = fm.list("tags"),
      extras = passthrough(fm, TALK_KEYS),
    )
    return meta to doc.body
  }

  fun buildTalk(meta: TalkMeta, body: String): String {
    val fields = linkedMapOf<String, String>()
    if (meta.title.isNotBlank()) fields["title"] = meta.title
    fields["date"] = meta.date.ifBlank { now() }
    return FrontmatterCodec.build(
      Frontmatter(fields = fields, lists = mapOf("tags" to meta.tags), extras = meta.extras),
      body,
    )
  }

  /**
   * Derives a filename from a title. Spaces and filesystem-hostile characters are
   * replaced; CJK is kept because the site serves those paths fine.
   */
  fun filenameFor(title: String, fallbackDate: String = ""): String {
    val cleaned = title.trim()
      .replace(Regex("[/\\\\:*?\"<>|\\s]+"), "-")
      .replace(Regex("-{2,}"), "-")
      .trim('-')
      .take(80)
    if (cleaned.isNotEmpty()) return "$cleaned.md"
    val stamp = fallbackDate.ifBlank { now() }
      .replace(Regex("[^0-9]"), "")
      .take(14)
    return "note-$stamp.md"
  }

  /** Talks are timestamp-named so the timeline stays chronological. */
  fun talkFilename(date: String): String {
    val digits = date.replace(Regex("[^0-9]"), "").take(14)
    val stamp = digits.ifBlank { SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(Date()) }
    return "$stamp.md"
  }

  fun ensureMd(name: String): String {
    val trimmed = name.trim().removeSuffix(".md").removeSuffix(".markdown")
    return "$trimmed.md"
  }
}
