package com.upxuu.xucms.editor.markdown

import com.upxuu.xucms.editor.model.InlineMark
import com.upxuu.xucms.editor.model.MarkSpan
import com.upxuu.xucms.editor.model.MarkSpans

/** Plain text plus the emphasis ranges recovered from (or written back to) markdown. */
data class InlineText(val text: String, val marks: List<MarkSpan>)

/**
 * Inline markdown, hand-rolled. Handles `**bold**`, `__bold__`, `*italic*`,
 * `_italic_`, `~~strike~~`, `` `code` ``, `[label](href)` and escapes. Code spans win
 * over everything inside them, matching CommonMark's precedence closely enough for
 * a notes editor without pulling in a full parser.
 */
object InlineMarkdown {

  private val MARKERS = charArrayOf('\\', '`', '!', '[', '~', '*', '_')

  fun parse(source: String): InlineText {
    // Text with no markers at all is the common case — most lines of prose contain
    // none — so return it without walking character by character or allocating.
    if (source.none { it in MARKERS }) return InlineText(source, emptyList())

    val out = StringBuilder(source.length)
    val marks = mutableListOf<MarkSpan>()
    var i = 0

    while (i < source.length) {
      val c = source[i]

      // Escape: the next character is literal.
      if (c == '\\' && i + 1 < source.length && !source[i + 1].isLetterOrDigit()) {
        out.append(source[i + 1])
        i += 2
        continue
      }

      // Code span — opaque contents.
      if (c == '`') {
        val fence = countRun(source, i, '`')
        val closing = source.indexOf("`".repeat(fence), i + fence)
        if (closing > 0) {
          val body = source.substring(i + fence, closing)
          val start = out.length
          out.append(body)
          marks += MarkSpan(start, out.length, InlineMark.CODE)
          i = closing + fence
          continue
        }
      }

      // Image: rendered inline as its alt text; block-level images are handled by
      // the block parser, so this only fires for images mixed into a sentence.
      if (c == '!' && i + 1 < source.length && source[i + 1] == '[') {
        val link = readLink(source, i + 1)
        if (link != null) {
          val inner = parse(link.label)
          val start = out.length
          out.append(inner.text)
          marks += MarkSpans.shift(inner.marks, start)
          i = link.end
          continue
        }
      }

      if (c == '[') {
        val link = readLink(source, i)
        if (link != null) {
          val inner = parse(link.label)
          val start = out.length
          out.append(inner.text)
          marks += MarkSpans.shift(inner.marks, start)
          marks += MarkSpan(start, out.length, InlineMark.LINK, link.href)
          i = link.end
          continue
        }
      }

      if (c == '~' && countRun(source, i, '~') >= 2) {
        val span = readDelimited(source, i, "~~")
        if (span != null) {
          val inner = parse(span.body)
          val start = out.length
          out.append(inner.text)
          marks += MarkSpans.shift(inner.marks, start)
          marks += MarkSpan(start, out.length, InlineMark.STRIKE)
          i = span.end
          continue
        }
      }

      if (c == '*' || c == '_') {
        val run = countRun(source, i, c)
        val strongToken = "$c$c"
        if (run >= 2) {
          val span = readDelimited(source, i, strongToken)
          if (span != null) {
            val inner = parse(span.body)
            val start = out.length
            out.append(inner.text)
            marks += MarkSpans.shift(inner.marks, start)
            marks += MarkSpan(start, out.length, InlineMark.BOLD)
            i = span.end
            continue
          }
        }
        val span = readDelimited(source, i, c.toString())
        if (span != null && span.body.isNotBlank()) {
          val inner = parse(span.body)
          val start = out.length
          out.append(inner.text)
          marks += MarkSpans.shift(inner.marks, start)
          marks += MarkSpan(start, out.length, InlineMark.ITALIC)
          i = span.end
          continue
        }
      }

      out.append(c)
      i++
    }

    return InlineText(out.toString(), MarkSpans.normalize(marks, out.length))
  }

  fun serialize(text: String, marks: List<MarkSpan>): String {
    if (text.isEmpty()) return ""
    val normalized = MarkSpans.normalize(marks, text.length)
    if (normalized.isEmpty()) return escape(text)

    // Emit markers at every boundary, opening in a stable order and closing in
    // reverse so the output nests correctly.
    val boundaries = sortedSetOf(0, text.length)
    normalized.forEach { boundaries += it.start; boundaries += it.end }
    val points = boundaries.toList()

    val out = StringBuilder()
    val open = ArrayDeque<MarkSpan>()

    for (idx in 0 until points.size - 1) {
      val from = points[idx]
      val to = points[idx + 1]
      val active = normalized.filter { it.start <= from && it.end >= to }
        .sortedWith(compareBy({ it.start }, { -(it.end) }, { it.mark.ordinal }))

      // Close marks that no longer apply (innermost first).
      while (open.isNotEmpty() && open.last() !in active) {
        out.append(closeToken(open.removeLast()))
      }
      // Reopen anything that was closed out of order.
      val stillOpen = open.toList()
      for (span in active) {
        if (span !in stillOpen) {
          open.addLast(span)
          out.append(openToken(span))
        }
      }

      val slice = text.substring(from, to)
      val inCode = active.any { it.mark == InlineMark.CODE }
      out.append(if (inCode) slice else escape(slice))
    }
    while (open.isNotEmpty()) out.append(closeToken(open.removeLast()))
    return out.toString()
  }

  private fun openToken(span: MarkSpan): String = when (span.mark) {
    InlineMark.BOLD -> "**"
    InlineMark.ITALIC -> "*"
    InlineMark.STRIKE -> "~~"
    InlineMark.CODE -> "`"
    InlineMark.LINK -> "["
  }

  private fun closeToken(span: MarkSpan): String = when (span.mark) {
    InlineMark.BOLD -> "**"
    InlineMark.ITALIC -> "*"
    InlineMark.STRIKE -> "~~"
    InlineMark.CODE -> "`"
    InlineMark.LINK -> "](${span.href})"
  }

  private fun escape(raw: String): String {
    val out = StringBuilder(raw.length)
    for (c in raw) {
      if (c in "*_`~[]\\") out.append('\\')
      out.append(c)
    }
    return out.toString()
  }

  private fun countRun(source: String, from: Int, c: Char): Int {
    var n = 0
    while (from + n < source.length && source[from + n] == c) n++
    return n
  }

  private data class Delimited(val body: String, val end: Int)

  private fun readDelimited(source: String, from: Int, token: String): Delimited? {
    val bodyStart = from + token.length
    if (bodyStart >= source.length) return null
    var i = bodyStart
    while (i < source.length) {
      if (source[i] == '\\') { i += 2; continue }
      if (source.startsWith(token, i)) {
        if (i == bodyStart) return null
        return Delimited(source.substring(bodyStart, i), i + token.length)
      }
      i++
    }
    return null
  }

  private data class Link(val label: String, val href: String, val end: Int)

  private fun readLink(source: String, from: Int): Link? {
    if (from >= source.length || source[from] != '[') return null
    var depth = 0
    var i = from
    var labelEnd = -1
    while (i < source.length) {
      when {
        source[i] == '\\' -> i++
        source[i] == '[' -> depth++
        source[i] == ']' -> {
          depth--
          if (depth == 0) { labelEnd = i; break }
        }
      }
      i++
    }
    if (labelEnd < 0 || labelEnd + 1 >= source.length || source[labelEnd + 1] != '(') return null
    val hrefEnd = source.indexOf(')', labelEnd + 2)
    if (hrefEnd < 0) return null
    return Link(
      label = source.substring(from + 1, labelEnd),
      href = source.substring(labelEnd + 2, hrefEnd).trim(),
      end = hrefEnd + 1,
    )
  }
}
