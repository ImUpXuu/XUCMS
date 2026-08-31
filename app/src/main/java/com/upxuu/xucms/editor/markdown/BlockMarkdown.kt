package com.upxuu.xucms.editor.markdown

import com.upxuu.xucms.editor.model.Block
import com.upxuu.xucms.editor.model.BlockType

/**
 * Block-level markdown, hand-rolled. Round-trips the subset the app writes:
 * headings, paragraphs, bullet/ordered/task lists with two-space nesting,
 * blockquotes, fenced code, thematic breaks and standalone images.
 */
object BlockMarkdown {

  fun parse(markdown: String): List<Block> {
    val lines = markdown.replace("\r\n", "\n").replace('\r', '\n').split("\n")
    val blocks = mutableListOf<Block>()
    var i = 0

    while (i < lines.size) {
      val raw = lines[i]
      val line = raw.trimEnd()
      val trimmed = line.trim()

      if (trimmed.isEmpty()) { i++; continue }

      // Fenced code block.
      val fence = fenceToken(trimmed)
      if (fence != null) {
        val language = trimmed.removePrefix(fence).trim()
        val body = mutableListOf<String>()
        i++
        while (i < lines.size && fenceToken(lines[i].trim()) == null) {
          body += lines[i]
          i++
        }
        if (i < lines.size) i++ // consume closing fence
        blocks += Block.of(BlockType.CODE, body.joinToString("\n"), language = language)
        continue
      }

      // Thematic break.
      if (isThematicBreak(trimmed)) {
        blocks += Block.of(BlockType.DIVIDER)
        i++
        continue
      }

      // Standalone image.
      val image = standaloneImage(trimmed)
      if (image != null) {
        blocks += Block.of(BlockType.IMAGE, imageUrl = image.second, imageAlt = image.first)
        i++
        continue
      }

      // ATX heading.
      val hashes = trimmed.takeWhile { it == '#' }.length
      if (hashes in 1..3 && trimmed.length > hashes && trimmed[hashes] == ' ') {
        val type = when (hashes) {
          1 -> BlockType.H1
          2 -> BlockType.H2
          else -> BlockType.H3
        }
        blocks += inlineBlock(type, trimmed.substring(hashes + 1).trim())
        i++
        continue
      }
      if (hashes >= 4 && trimmed.length > hashes && trimmed[hashes] == ' ') {
        blocks += inlineBlock(BlockType.H3, trimmed.substring(hashes + 1).trim())
        i++
        continue
      }

      // Blockquote — consecutive `>` lines fold into one block.
      if (trimmed.startsWith(">")) {
        val body = mutableListOf<String>()
        while (i < lines.size && lines[i].trim().startsWith(">")) {
          body += lines[i].trim().removePrefix(">").removePrefix(" ")
          i++
        }
        blocks += inlineBlock(BlockType.QUOTE, body.joinToString("\n").trim())
        continue
      }

      // List items.
      val listItem = parseListItem(line)
      if (listItem != null) {
        blocks += inlineBlock(
          type = listItem.type,
          text = listItem.text,
          indent = listItem.indent,
          checked = listItem.checked,
        )
        i++
        continue
      }

      // Paragraph: keep consuming until a blank line or another block starts.
      val paragraph = mutableListOf(trimmed)
      i++
      while (i < lines.size) {
        val next = lines[i]
        val nextTrimmed = next.trim()
        if (nextTrimmed.isEmpty()) break
        if (startsNewBlock(next, nextTrimmed)) break
        paragraph += nextTrimmed
        i++
      }
      blocks += inlineBlock(BlockType.PARAGRAPH, paragraph.joinToString("\n"))
    }

    return if (blocks.isEmpty()) listOf(Block.paragraph()) else blocks
  }

  fun serialize(blocks: List<Block>): String {
    val out = StringBuilder()
    var previous: Block? = null

    for (block in blocks) {
      val prev = previous
      if (prev != null && needsBlankLine(prev, block)) out.append('\n')

      when (block.type) {
        BlockType.DIVIDER -> out.append("---\n")
        BlockType.IMAGE -> out.append("![${block.imageAlt}](${block.imageUrl})\n")
        BlockType.CODE -> {
          out.append("```").append(block.language.trim()).append('\n')
          if (block.text.isNotEmpty()) out.append(block.text).append('\n')
          out.append("```\n")
        }
        BlockType.QUOTE -> {
          val body = InlineMarkdown.serialize(block.text, block.marks)
          body.split("\n").forEach { out.append("> ").append(it).append('\n') }
        }
        BlockType.H1, BlockType.H2, BlockType.H3 -> {
          val hashes = when (block.type) {
            BlockType.H1 -> "#"
            BlockType.H2 -> "##"
            else -> "###"
          }
          out.append(hashes).append(' ')
            .append(InlineMarkdown.serialize(block.text, block.marks)).append('\n')
        }
        BlockType.BULLET, BlockType.ORDERED, BlockType.TODO -> {
          val pad = "  ".repeat(block.indent)
          val marker = when (block.type) {
            BlockType.ORDERED -> "1. "
            BlockType.TODO -> if (block.checked) "- [x] " else "- [ ] "
            else -> "- "
          }
          out.append(pad).append(marker)
            .append(InlineMarkdown.serialize(block.text, block.marks)).append('\n')
        }
        BlockType.PARAGRAPH -> {
          val body = InlineMarkdown.serialize(block.text, block.marks)
          out.append(body).append('\n')
        }
      }
      previous = block
    }
    return out.toString().trimEnd('\n')
  }

  private fun needsBlankLine(previous: Block, next: Block): Boolean {
    // Consecutive list items of the same family stay tight; everything else is
    // separated so the markdown reads well outside the app too.
    if (previous.type.isList && next.type.isList) return false
    return true
  }

  private fun inlineBlock(
    type: BlockType,
    text: String,
    indent: Int = 0,
    checked: Boolean = false,
  ): Block {
    val parsed = InlineMarkdown.parse(text)
    return Block.of(type, parsed.text, parsed.marks, indent = indent, checked = checked)
  }

  private fun fenceToken(trimmed: String): String? = when {
    trimmed.startsWith("```") -> "```"
    trimmed.startsWith("~~~") -> "~~~"
    else -> null
  }

  private fun isThematicBreak(trimmed: String): Boolean {
    if (trimmed.length < 3) return false
    val c = trimmed[0]
    if (c != '-' && c != '*' && c != '_') return false
    return trimmed.all { it == c || it == ' ' } && trimmed.count { it == c } >= 3
  }

  private val imageRegex = Regex("^!\\[([^\\]]*)]\\(([^)\\s]+)\\)$")

  private fun standaloneImage(trimmed: String): Pair<String, String>? =
    imageRegex.matchEntire(trimmed)?.let { it.groupValues[1] to it.groupValues[2] }

  private data class ListItem(
    val type: BlockType,
    val text: String,
    val indent: Int,
    val checked: Boolean,
  )

  private val orderedRegex = Regex("^(\\s*)(\\d+)[.)]\\s+(.*)$")
  private val bulletRegex = Regex("^(\\s*)([-*+])\\s+(.*)$")

  private fun parseListItem(line: String): ListItem? {
    bulletRegex.matchEntire(line)?.let { m ->
      val indent = m.groupValues[1].replace("\t", "  ").length / 2
      var body = m.groupValues[3]
      var type = BlockType.BULLET
      var checked = false
      when {
        body.startsWith("[ ] ") -> { type = BlockType.TODO; body = body.removePrefix("[ ] ") }
        body.startsWith("[x] ", ignoreCase = true) -> {
          type = BlockType.TODO; checked = true; body = body.drop(4)
        }
        body == "[ ]" -> { type = BlockType.TODO; body = "" }
        body.equals("[x]", ignoreCase = true) -> { type = BlockType.TODO; checked = true; body = "" }
      }
      return ListItem(type, body.trim(), indent, checked)
    }
    orderedRegex.matchEntire(line)?.let { m ->
      val indent = m.groupValues[1].replace("\t", "  ").length / 2
      return ListItem(BlockType.ORDERED, m.groupValues[3].trim(), indent, false)
    }
    return null
  }

  private fun startsNewBlock(line: String, trimmed: String): Boolean {
    if (fenceToken(trimmed) != null) return true
    if (isThematicBreak(trimmed)) return true
    if (standaloneImage(trimmed) != null) return true
    if (trimmed.startsWith(">")) return true
    if (parseListItem(line) != null) return true
    val hashes = trimmed.takeWhile { it == '#' }.length
    return hashes in 1..6 && trimmed.length > hashes && trimmed[hashes] == ' '
  }
}
