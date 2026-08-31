package com.upxuu.xucms.editor.markdown

import com.upxuu.xucms.editor.model.BlockType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockMarkdownTest {

  @Test
  fun `headings lose their hashes`() {
    val blocks = BlockMarkdown.parse("# Title\n\nBody text")
    assertEquals(BlockType.H1, blocks[0].type)
    assertEquals("Title", blocks[0].text)
    assertEquals(BlockType.PARAGRAPH, blocks[1].type)
    assertEquals("Body text", blocks[1].text)
  }

  @Test
  fun `nested bullets keep their depth`() {
    val blocks = BlockMarkdown.parse("- one\n  - two\n    - three")
    assertEquals(listOf(0, 1, 2), blocks.map { it.indent })
    assertTrue(blocks.all { it.type == BlockType.BULLET })
    assertEquals(listOf("one", "two", "three"), blocks.map { it.text })
  }

  @Test
  fun `task list checked state is parsed`() {
    val blocks = BlockMarkdown.parse("- [ ] todo\n- [x] done")
    assertEquals(BlockType.TODO, blocks[0].type)
    assertEquals(false, blocks[0].checked)
    assertEquals(true, blocks[1].checked)
    assertEquals("done", blocks[1].text)
  }

  @Test
  fun `fenced code keeps language and body verbatim`() {
    val blocks = BlockMarkdown.parse("```kotlin\nval a = 1 * 2\n```")
    assertEquals(BlockType.CODE, blocks.single().type)
    assertEquals("kotlin", blocks.single().language)
    assertEquals("val a = 1 * 2", blocks.single().text)
  }

  @Test
  fun `standalone image becomes an image block`() {
    val blocks = BlockMarkdown.parse("![shot](https://cdn.example/a.png)")
    val block = blocks.single()
    assertEquals(BlockType.IMAGE, block.type)
    assertEquals("https://cdn.example/a.png", block.imageUrl)
    assertEquals("shot", block.imageAlt)
  }

  @Test
  fun `thematic break becomes a divider`() {
    val blocks = BlockMarkdown.parse("before\n\n---\n\nafter")
    assertEquals(BlockType.DIVIDER, blocks[1].type)
  }

  @Test
  fun `each quote line is its own block but serializes back tightly`() {
    val blocks = BlockMarkdown.parse("> line one\n> line two")
    assertEquals(2, blocks.size)
    assertTrue(blocks.all { it.type == BlockType.QUOTE })
    assertEquals(listOf("line one", "line two"), blocks.map { it.text })
    assertEquals("> line one\n> line two", BlockMarkdown.serialize(blocks))
  }

  @Test
  fun `consecutive text lines stay separate blocks`() {
    // Folding them would make styling one line affect its neighbours.
    val blocks = BlockMarkdown.parse("first line\nsecond line")
    assertEquals(2, blocks.size)
    assertEquals(listOf("first line", "second line"), blocks.map { it.text })
  }

  @Test
  fun `document round trips`() {
    val source = """
      # 标题

      正文里有 **粗体**、*斜体* 和 [链接](https://upxuu.com)。

      ## 小节

      - 第一项
        - 嵌套项
      - 第二项

      1. 有序一
      2. 有序二

      - [x] 已完成
      - [ ] 未完成

      > 引用一句

      ```kotlin
      val x = 1
      ```

      ---

      ![图](https://cdn.example/a.png)

      结尾段落。
    """.trimIndent()

    val once = BlockMarkdown.serialize(BlockMarkdown.parse(source))
    val twice = BlockMarkdown.serialize(BlockMarkdown.parse(once))
    assertEquals(once, twice)

    val blocks = BlockMarkdown.parse(once)
    assertTrue(blocks.any { it.type == BlockType.H1 })
    assertTrue(blocks.any { it.type == BlockType.H2 })
    assertTrue(blocks.any { it.type == BlockType.BULLET && it.indent == 1 })
    assertTrue(blocks.any { it.type == BlockType.ORDERED })
    assertTrue(blocks.any { it.type == BlockType.TODO && it.checked })
    assertTrue(blocks.any { it.type == BlockType.QUOTE })
    assertTrue(blocks.any { it.type == BlockType.CODE && it.language == "kotlin" })
    assertTrue(blocks.any { it.type == BlockType.DIVIDER })
    assertTrue(blocks.any { it.type == BlockType.IMAGE })
  }

  @Test
  fun `empty document yields one paragraph`() {
    val blocks = BlockMarkdown.parse("")
    assertEquals(1, blocks.size)
    assertEquals(BlockType.PARAGRAPH, blocks.single().type)
    assertEquals("", blocks.single().text)
  }

  @Test
  fun `ordered list numbering is normalized on write`() {
    val output = BlockMarkdown.serialize(BlockMarkdown.parse("3. a\n4. b"))
    assertEquals("1. a\n1. b", output)
  }
}
