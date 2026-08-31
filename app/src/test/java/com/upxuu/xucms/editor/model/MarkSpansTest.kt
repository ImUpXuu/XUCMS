package com.upxuu.xucms.editor.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkSpansTest {

  @Test
  fun `touching spans of the same kind merge`() {
    val merged = MarkSpans.normalize(
      listOf(MarkSpan(0, 3, InlineMark.BOLD), MarkSpan(3, 6, InlineMark.BOLD)),
      textLength = 6,
    )
    assertEquals(listOf(MarkSpan(0, 6, InlineMark.BOLD)), merged)
  }

  @Test
  fun `different kinds stay separate`() {
    val merged = MarkSpans.normalize(
      listOf(MarkSpan(0, 3, InlineMark.BOLD), MarkSpan(0, 3, InlineMark.ITALIC)),
      textLength = 3,
    )
    assertEquals(2, merged.size)
  }

  @Test
  fun `covers requires full coverage`() {
    val spans = listOf(MarkSpan(0, 4, InlineMark.BOLD))
    assertTrue(MarkSpans.covers(spans, 1, 3, InlineMark.BOLD))
    assertFalse(MarkSpans.covers(spans, 2, 6, InlineMark.BOLD))
    assertFalse(MarkSpans.covers(spans, 1, 3, InlineMark.ITALIC))
  }

  @Test
  fun `removing from the middle splits the span`() {
    val result = MarkSpans.remove(
      listOf(MarkSpan(0, 10, InlineMark.BOLD)),
      start = 4,
      end = 6,
      mark = InlineMark.BOLD,
      textLength = 10,
    )
    assertEquals(
      listOf(MarkSpan(0, 4, InlineMark.BOLD), MarkSpan(6, 10, InlineMark.BOLD)),
      result,
    )
  }

  @Test
  fun `toggle adds then removes`() {
    val added = MarkSpans.toggle(emptyList(), 0, 4, InlineMark.BOLD, textLength = 4)
    assertEquals(1, added.size)
    val removed = MarkSpans.toggle(added, 0, 4, InlineMark.BOLD, textLength = 4)
    assertTrue(removed.isEmpty())
  }

  @Test
  fun `insertion before a span shifts it`() {
    val result = MarkSpans.remap(
      listOf(MarkSpan(5, 9, InlineMark.BOLD)),
      editStart = 0,
      editEnd = 0,
      inserted = 3,
      newLength = 12,
    )
    assertEquals(listOf(MarkSpan(8, 12, InlineMark.BOLD)), result)
  }

  @Test
  fun `typing at the end of a span extends it`() {
    val result = MarkSpans.remap(
      listOf(MarkSpan(0, 4, InlineMark.BOLD)),
      editStart = 4,
      editEnd = 4,
      inserted = 2,
      newLength = 6,
    )
    assertEquals(listOf(MarkSpan(0, 6, InlineMark.BOLD)), result)
  }

  @Test
  fun `deleting the whole span drops it`() {
    val result = MarkSpans.remap(
      listOf(MarkSpan(0, 4, InlineMark.BOLD)),
      editStart = 0,
      editEnd = 4,
      inserted = 0,
      newLength = 0,
    )
    assertTrue(result.isEmpty())
  }

  @Test
  fun `slice rebases to zero`() {
    val result = MarkSpans.slice(listOf(MarkSpan(3, 8, InlineMark.ITALIC)), from = 2, to = 6)
    assertEquals(listOf(MarkSpan(1, 4, InlineMark.ITALIC)), result)
  }

  @Test
  fun `spans clamp to the text length`() {
    val result = MarkSpans.normalize(listOf(MarkSpan(0, 99, InlineMark.CODE)), textLength = 5)
    assertEquals(listOf(MarkSpan(0, 5, InlineMark.CODE)), result)
  }
}
