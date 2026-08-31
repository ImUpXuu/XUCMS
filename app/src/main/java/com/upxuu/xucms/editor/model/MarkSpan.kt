package com.upxuu.xucms.editor.model

/** Inline emphasis kinds. Stored as ranges over a block's plain text. */
enum class InlineMark { BOLD, ITALIC, STRIKE, CODE, LINK }

/**
 * An inline style over `[start, end)` of a block's plain text. Markdown markers
 * are never present in the text itself, so [href] carries link targets.
 */
data class MarkSpan(
  val start: Int,
  val end: Int,
  val mark: InlineMark,
  val href: String = "",
) {
  val isEmpty: Boolean get() = end <= start

  fun clamp(length: Int): MarkSpan? {
    val s = start.coerceIn(0, length)
    val e = end.coerceIn(0, length)
    return if (e > s) copy(start = s, end = e) else null
  }
}

object MarkSpans {
  /** Merges touching/overlapping spans of the same kind and drops empty ones. */
  fun normalize(spans: List<MarkSpan>, textLength: Int): List<MarkSpan> {
    val clamped = spans.mapNotNull { it.clamp(textLength) }
    val out = mutableListOf<MarkSpan>()
    for (group in clamped.groupBy { it.mark to it.href }.values) {
      val sorted = group.sortedBy { it.start }
      var current = sorted.first()
      for (next in sorted.drop(1)) {
        current = if (next.start <= current.end) {
          current.copy(end = maxOf(current.end, next.end))
        } else {
          out += current
          next
        }
      }
      out += current
    }
    return out.sortedWith(compareBy({ it.start }, { it.mark.ordinal }))
  }

  /** True when every character in `[start, end)` already carries [mark]. */
  fun covers(spans: List<MarkSpan>, start: Int, end: Int, mark: InlineMark): Boolean {
    if (end <= start) return false
    var cursor = start
    val relevant = spans.filter { it.mark == mark && it.end > start && it.start < end }
      .sortedBy { it.start }
    for (span in relevant) {
      if (span.start > cursor) return false
      cursor = maxOf(cursor, span.end)
      if (cursor >= end) return true
    }
    return cursor >= end
  }

  fun add(spans: List<MarkSpan>, start: Int, end: Int, mark: InlineMark, href: String = "", textLength: Int): List<MarkSpan> {
    if (end <= start) return spans
    return normalize(spans + MarkSpan(start, end, mark, href), textLength)
  }

  /** Removes [mark] from `[start, end)`, splitting spans that straddle the range. */
  fun remove(spans: List<MarkSpan>, start: Int, end: Int, mark: InlineMark, textLength: Int): List<MarkSpan> {
    if (end <= start) return spans
    val out = mutableListOf<MarkSpan>()
    for (span in spans) {
      if (span.mark != mark || span.end <= start || span.start >= end) {
        out += span
        continue
      }
      if (span.start < start) out += span.copy(end = start)
      if (span.end > end) out += span.copy(start = end)
    }
    return normalize(out, textLength)
  }

  fun toggle(spans: List<MarkSpan>, start: Int, end: Int, mark: InlineMark, href: String = "", textLength: Int): List<MarkSpan> =
    if (covers(spans, start, end, mark)) remove(spans, start, end, mark, textLength)
    else add(spans, start, end, mark, href, textLength)

  /**
   * Shifts spans after a single-range text edit. [editStart] and [editEnd] address
   * the replaced range in the old text; [inserted] is the new length at that spot.
   * Text typed at a span's trailing edge extends it, which is what users expect
   * when they keep typing inside bold text.
   */
  fun remap(spans: List<MarkSpan>, editStart: Int, editEnd: Int, inserted: Int, newLength: Int): List<MarkSpan> {
    val delta = inserted - (editEnd - editStart)
    val out = mutableListOf<MarkSpan>()
    for (span in spans) {
      val start = when {
        span.start <= editStart -> span.start
        span.start >= editEnd -> span.start + delta
        else -> editStart
      }
      val end = when {
        span.end < editStart -> span.end
        span.end == editStart -> if (inserted > 0) span.end + inserted else span.end
        span.end >= editEnd -> span.end + delta
        else -> editStart
      }
      val moved = span.copy(start = start, end = end)
      moved.clamp(newLength)?.let { out += it }
    }
    return normalize(out, newLength)
  }

  /** Slices spans to `[from, to)` and rebases them to 0. */
  fun slice(spans: List<MarkSpan>, from: Int, to: Int): List<MarkSpan> =
    spans.mapNotNull { span ->
      val s = maxOf(span.start, from)
      val e = minOf(span.end, to)
      if (e > s) span.copy(start = s - from, end = e - from) else null
    }

  fun shift(spans: List<MarkSpan>, by: Int): List<MarkSpan> =
    spans.map { it.copy(start = it.start + by, end = it.end + by) }
}
