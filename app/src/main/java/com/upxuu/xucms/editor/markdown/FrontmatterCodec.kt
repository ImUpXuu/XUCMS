package com.upxuu.xucms.editor.markdown

/**
 * Minimal YAML frontmatter reader/writer. Only the shapes the CMS uses are
 * supported: scalars and flow sequences (`tags: [a, b]`). Unknown keys survive a
 * round trip via [Frontmatter.extras] so hand-edited files are not silently
 * stripped when saved from the app.
 */
data class Frontmatter(
  val fields: Map<String, String> = emptyMap(),
  val lists: Map<String, List<String>> = emptyMap(),
  val extras: List<String> = emptyList(),
) {
  operator fun get(key: String): String = fields[key].orEmpty()
  fun list(key: String): List<String> = lists[key].orEmpty()
  fun bool(key: String): Boolean = fields[key]?.trim()?.lowercase() == "true"
  fun int(key: String): Int = fields[key]?.trim()?.toIntOrNull() ?: 0
}

data class MarkdownDocument(val frontmatter: Frontmatter, val body: String)

object FrontmatterCodec {

  private val knownListKeys = setOf("tags", "categories")

  fun parse(source: String): MarkdownDocument {
    val normalized = source.replace("\r\n", "\n").replace('\r', '\n')
    if (!normalized.startsWith("---")) return MarkdownDocument(Frontmatter(), normalized.trim())

    val lines = normalized.split("\n")
    var end = -1
    for (i in 1 until lines.size) {
      if (lines[i].trimEnd() == "---" || lines[i].trimEnd() == "...") { end = i; break }
    }
    if (end < 0) return MarkdownDocument(Frontmatter(), normalized.trim())

    val fields = linkedMapOf<String, String>()
    val lists = linkedMapOf<String, List<String>>()
    val extras = mutableListOf<String>()

    var i = 1
    while (i < end) {
      val line = lines[i]
      val colon = line.indexOf(':')
      if (line.isBlank()) { i++; continue }
      if (colon <= 0 || line.first().isWhitespace()) {
        // Block sequence continuation ("  - value") belongs to the previous key.
        val owner = fields.keys.lastOrNull()
        val item = line.trim().removePrefix("-").trim()
        if (owner != null && line.trim().startsWith("-") && item.isNotEmpty()) {
          val existing = lists[owner].orEmpty()
          lists[owner] = existing + unquote(item)
          fields.remove(owner)
        } else {
          extras += line
        }
        i++
        continue
      }
      val key = line.substring(0, colon).trim()
      val rawValue = line.substring(colon + 1).trim()
      when {
        rawValue.startsWith("[") && rawValue.endsWith("]") -> {
          lists[key] = rawValue.removeSurrounding("[", "]")
            .split(',')
            .map { unquote(it.trim()) }
            .filter { it.isNotEmpty() }
        }
        rawValue.isEmpty() && key in knownListKeys -> lists[key] = emptyList()
        rawValue.isEmpty() -> fields[key] = ""
        else -> fields[key] = unquote(rawValue)
      }
      i++
    }

    val body = lines.drop(end + 1).joinToString("\n").trim()
    return MarkdownDocument(Frontmatter(fields, lists, extras), body)
  }

  fun build(frontmatter: Frontmatter, body: String): String {
    val out = StringBuilder("---\n")
    frontmatter.fields.forEach { (key, value) ->
      out.append(key).append(": ").append(quoteIfNeeded(value)).append('\n')
    }
    frontmatter.lists.forEach { (key, values) ->
      out.append(key).append(": [")
        .append(values.joinToString(", ") { quoteIfNeeded(it) })
        .append("]\n")
    }
    frontmatter.extras.forEach { out.append(it).append('\n') }
    out.append("---\n\n").append(body.trimEnd()).append('\n')
    return out.toString()
  }

  private fun unquote(value: String): String {
    val trimmed = value.trim()
    return when {
      trimmed.length >= 2 && trimmed.startsWith('"') && trimmed.endsWith('"') -> trimmed.substring(1, trimmed.length - 1)
      trimmed.length >= 2 && trimmed.startsWith('\'') && trimmed.endsWith('\'') -> trimmed.substring(1, trimmed.length - 1)
      else -> trimmed
    }
  }

  private fun quoteIfNeeded(value: String): String {
    val needsQuotes = value.isEmpty() ||
      value.first().isWhitespace() ||
      value.last().isWhitespace() ||
      value.any { it == ':' || it == '#' || it == ',' || it == '[' || it == ']' || it == '"' } ||
      value.first() in "&*!|>%@`{}"
    if (!needsQuotes) return value
    return '"' + value.replace("\\", "\\\\").replace("\"", "\\\"") + '"'
  }
}
