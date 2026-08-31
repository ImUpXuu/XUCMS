package com.upxuu.xucms.editor.markdown

import com.upxuu.xucms.editor.model.InlineMark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InlineMarkdownTest {

  @Test
  fun `bold markers are stripped and recorded as a span`() {
    val parsed = InlineMarkdown.parse("hello **world**")
    assertEquals("hello world", parsed.text)
    assertEquals(1, parsed.marks.size)
    val span = parsed.marks.first()
    assertEquals(InlineMark.BOLD, span.mark)
    assertEquals(6, span.start)
    assertEquals(11, span.end)
  }

  @Test
  fun `italic underscore form is supported`() {
    val parsed = InlineMarkdown.parse("_soft_ edge")
    assertEquals("soft edge", parsed.text)
    assertEquals(InlineMark.ITALIC, parsed.marks.single().mark)
  }

  @Test
  fun `code span contents are opaque`() {
    val parsed = InlineMarkdown.parse("run `a * b` now")
    assertEquals("run a * b now", parsed.text)
    val span = parsed.marks.single()
    assertEquals(InlineMark.CODE, span.mark)
    assertEquals("a * b", parsed.text.substring(span.start, span.end))
  }

  @Test
  fun `link label becomes text and href is kept`() {
    val parsed = InlineMarkdown.parse("see [docs](https://example.com/a)")
    assertEquals("see docs", parsed.text)
    val span = parsed.marks.single()
    assertEquals(InlineMark.LINK, span.mark)
    assertEquals("https://example.com/a", span.href)
  }

  @Test
  fun `strikethrough round trips`() {
    val source = "~~gone~~ kept"
    val parsed = InlineMarkdown.parse(source)
    assertEquals("gone kept", parsed.text)
    assertEquals(source, InlineMarkdown.serialize(parsed.text, parsed.marks))
  }

  @Test
  fun `nested emphasis round trips`() {
    val parsed = InlineMarkdown.parse("**bold and *both* here**")
    assertEquals("bold and both here", parsed.text)
    val reserialized = InlineMarkdown.serialize(parsed.text, parsed.marks)
    val reparsed = InlineMarkdown.parse(reserialized)
    assertEquals(parsed.text, reparsed.text)
    assertTrue(reparsed.marks.any { it.mark == InlineMark.BOLD })
    assertTrue(reparsed.marks.any { it.mark == InlineMark.ITALIC })
  }

  @Test
  fun `plain text with special characters is escaped on the way out`() {
    val serialized = InlineMarkdown.serialize("2 * 3 = 6", emptyList())
    assertEquals("2 \\* 3 = 6", serialized)
    assertEquals("2 * 3 = 6", InlineMarkdown.parse(serialized).text)
  }

  @Test
  fun `unclosed marker stays literal`() {
    val parsed = InlineMarkdown.parse("a ** b")
    assertEquals("a ** b", parsed.text)
    assertTrue(parsed.marks.isEmpty())
  }

  @Test
  fun `link round trips through serialize`() {
    val source = "go [here](https://x.dev) now"
    val parsed = InlineMarkdown.parse(source)
    assertEquals(source, InlineMarkdown.serialize(parsed.text, parsed.marks))
  }
}
