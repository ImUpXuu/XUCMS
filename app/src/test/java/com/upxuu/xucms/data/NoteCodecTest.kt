package com.upxuu.xucms.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteCodecTest {

  @Test
  fun `post frontmatter round trips`() {
    val source = """
      ---
      title: 我的文章
      published: 2026-08-31 12:00:00
      category: 随笔
      tags: [Android, Compose]
      description: 一段摘要
      image: https://cdn.example/a.png
      draft: true
      sticky: 2
      ---

      正文第一段。
    """.trimIndent()

    val (meta, body) = NoteCodec.parsePost(source)
    assertEquals("我的文章", meta.title)
    assertEquals("2026-08-31 12:00:00", meta.published)
    assertEquals("随笔", meta.category)
    assertEquals(listOf("Android", "Compose"), meta.tags)
    assertEquals("一段摘要", meta.description)
    assertEquals("https://cdn.example/a.png", meta.cover)
    assertTrue(meta.draft)
    assertEquals(2, meta.sticky)
    assertEquals("正文第一段。", body)

    val rebuilt = NoteCodec.buildPost(meta, body)
    val (again, bodyAgain) = NoteCodec.parsePost(rebuilt)
    assertEquals(meta, again)
    assertEquals(body, bodyAgain)
  }

  @Test
  fun `talk frontmatter round trips`() {
    val source = "---\ndate: 2026-08-31 09:00:00\ntags: [日常]\n---\n\n今天天气不错。"
    val (meta, body) = NoteCodec.parseTalk(source)
    assertEquals("2026-08-31 09:00:00", meta.date)
    assertEquals(listOf("日常"), meta.tags)
    assertEquals("今天天气不错。", body)

    val (again, _) = NoteCodec.parseTalk(NoteCodec.buildTalk(meta, body))
    assertEquals(meta, again)
  }

  @Test
  fun `content without frontmatter is treated as pure body`() {
    val (meta, body) = NoteCodec.parsePost("# 只有正文\n\n没有元数据。")
    assertEquals("", meta.title)
    assertEquals("# 只有正文\n\n没有元数据。", body)
  }

  @Test
  fun `filename replaces unsafe characters`() {
    assertEquals("a-b-c.md", NoteCodec.filenameFor("a/b c"))
    assertEquals("中文标题.md", NoteCodec.filenameFor("中文标题"))
  }

  @Test
  fun `blank title falls back to a timestamped name`() {
    val name = NoteCodec.filenameFor("", "2026-08-31 12:00:00")
    assertEquals("note-20260831120000.md", name)
  }

  @Test
  fun `talk filename uses the timestamp digits`() {
    assertEquals("20260831090000.md", NoteCodec.talkFilename("2026-08-31 09:00:00"))
  }

  @Test
  fun `ensureMd does not double the extension`() {
    assertEquals("post.md", NoteCodec.ensureMd("post"))
    assertEquals("post.md", NoteCodec.ensureMd("post.md"))
  }

  @Test
  fun `unknown frontmatter keys survive a round trip`() {
    val source = "---\ntitle: T\npublished: 2026-01-01 00:00:00\nlicense: CC-BY\n---\n\nbody"
    val (meta, body) = NoteCodec.parsePost(source)
    val rebuilt = NoteCodec.buildPost(meta, body)
    assertTrue(rebuilt.contains("license: CC-BY"))
  }
}
