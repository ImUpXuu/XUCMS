package com.upxuu.xucms.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.upxuu.xucms.editor.markdown.BlockMarkdown
import com.upxuu.xucms.editor.model.Block
import com.upxuu.xucms.editor.model.BlockType
import com.upxuu.xucms.editor.model.InlineMark
import com.upxuu.xucms.editor.model.MarkSpan
import com.upxuu.xucms.editor.model.MarkSpans

/**
 * The editor's document state: an ordered list of blocks, one focused block, and
 * an undo stack. Every mutation goes through here so the markdown serializer and
 * the toolbar always agree with what is on screen.
 */
class EditorState(initialMarkdown: String = "") {

  private val _blocks = mutableStateListOf<Block>()
  val blocks: List<Block> get() = _blocks

  /** Id of the block that currently owns the caret, or null when unfocused. */
  var focusedId: Long? by mutableStateOf(null)
    private set

  /** Bumped whenever the focus target should be (re)applied to a text field. */
  var focusRequestToken by mutableStateOf(0)
    private set

  var revision by mutableStateOf(0)
    private set

  private val undoStack = ArrayDeque<List<Block>>()
  private val redoStack = ArrayDeque<List<Block>>()
  private var suppressHistory = false

  val canUndo: Boolean get() = undoStack.isNotEmpty()
  val canRedo: Boolean get() = redoStack.isNotEmpty()

  init {
    setMarkdown(initialMarkdown)
  }

  // ---------------------------------------------------------------- document

  fun setMarkdown(markdown: String) {
    suppressHistory = true
    _blocks.clear()
    _blocks.addAll(BlockMarkdown.parse(markdown))
    suppressHistory = false
    undoStack.clear()
    redoStack.clear()
    focusedId = null
    revision++
  }

  fun toMarkdown(): String = BlockMarkdown.serialize(_blocks.toList())

  val isBlank: Boolean
    get() = _blocks.all { it.type == BlockType.PARAGRAPH && it.text.isBlank() }

  val characterCount: Int get() = _blocks.sumOf { it.text.length }

  val wordCount: Int
    get() = _blocks.sumOf { block ->
      block.text.split(Regex("\\s+")).count { chunk -> chunk.isNotBlank() }
    }

  // ------------------------------------------------------------------ focus

  fun focus(id: Long, caretAtEnd: Boolean = true) {
    focusedId = id
    val index = indexOf(id) ?: return
    val block = _blocks[index]
    if (caretAtEnd) {
      _blocks[index] = block.copy(value = block.value.copy(selection = TextRange(block.text.length)))
    }
    focusRequestToken++
  }

  fun clearFocus() {
    focusedId = null
  }

  private fun indexOf(id: Long): Int? = _blocks.indexOfFirst { it.id == id }.takeIf { it >= 0 }

  fun blockAt(index: Int): Block? = _blocks.getOrNull(index)

  private fun focusedIndex(): Int? = focusedId?.let { indexOf(it) }

  // ---------------------------------------------------------------- editing

  /**
   * Applies a text field change coming from a block's editor, remapping inline
   * marks and running markdown auto-formatting (`# `, `- `, `> `, ``` ``` ```).
   */
  fun onTextChange(id: Long, newValue: TextFieldValue) {
    val index = indexOf(id) ?: return
    val block = _blocks[index]
    val oldText = block.text
    val newText = newValue.text

    if (oldText == newText) {
      _blocks[index] = block.copy(value = newValue)
      return
    }

    pushHistory()

    val edit = diff(oldText, newText)
    val marks = MarkSpans.remap(block.marks, edit.start, edit.oldEnd, edit.insertedLength, newText.length)
    var updated = block.copy(value = newValue, marks = marks)

    // Auto-format: a marker typed at the start of a line converts the block.
    val shortcut = if (block.type != BlockType.CODE) detectShortcut(updated) else null
    if (shortcut != null) {
      updated = shortcut
    }

    _blocks[index] = updated
    revision++
  }

  /** Splits the focused block at the caret, honouring list continuation. */
  fun onEnter(id: Long) {
    val index = indexOf(id) ?: return
    val block = _blocks[index]
    pushHistory()

    if (block.type == BlockType.CODE) {
      // Newlines stay inside code blocks; the text field handles them directly.
      val caret = block.value.selection.start
      val text = block.text.substring(0, caret) + "\n" + block.text.substring(block.value.selection.end)
      _blocks[index] = block.copy(
        value = TextFieldValue(text, TextRange(caret + 1)),
      )
      revision++
      return
    }

    // Enter on an empty list item lifts it out of the list instead of nesting deeper.
    if (block.type.isList && block.text.isEmpty()) {
      _blocks[index] = if (block.indent > 0) {
        block.copy(indent = block.indent - 1)
      } else {
        block.copy(type = BlockType.PARAGRAPH, checked = false)
      }
      revision++
      focus(block.id)
      return
    }

    val caret = block.value.selection.start.coerceIn(0, block.text.length)
    val head = block.text.substring(0, caret)
    val tail = block.text.substring(caret)

    val headMarks = MarkSpans.slice(block.marks, 0, caret)
    val tailMarks = MarkSpans.slice(block.marks, caret, block.text.length)

    val continuedType = when {
      block.type.isList -> block.type
      else -> BlockType.PARAGRAPH
    }

    _blocks[index] = block.copy(marks = headMarks).withText(head, head.length)
    val next = Block.of(
      type = continuedType,
      text = tail,
      marks = tailMarks,
      indent = if (continuedType.isList) block.indent else 0,
    )
    _blocks.add(index + 1, next)
    revision++
    focus(next.id, caretAtEnd = false)
  }

  /**
   * Backspace at offset 0: demote the block, or merge it into the previous one.
   * Returns true when the event was consumed.
   */
  fun onBackspaceAtStart(id: Long): Boolean {
    val index = indexOf(id) ?: return false
    val block = _blocks[index]

    if (block.type != BlockType.PARAGRAPH) {
      pushHistory()
      _blocks[index] = when {
        block.type.isList && block.indent > 0 -> block.copy(indent = block.indent - 1)
        else -> block.copy(type = BlockType.PARAGRAPH, checked = false, indent = 0, language = "")
      }
      revision++
      focus(block.id, caretAtEnd = false)
      return true
    }

    if (index == 0) return false
    val previous = _blocks[index - 1]

    pushHistory()
    if (previous.type.isVoid) {
      _blocks.removeAt(index - 1)
      revision++
      focus(block.id, caretAtEnd = false)
      return true
    }

    val mergedText = previous.text + block.text
    val mergedMarks = MarkSpans.normalize(
      previous.marks + MarkSpans.shift(block.marks, previous.text.length),
      mergedText.length,
    )
    _blocks[index - 1] = previous.copy(
      marks = mergedMarks,
      value = TextFieldValue(mergedText, TextRange(previous.text.length)),
    )
    _blocks.removeAt(index)
    revision++
    focusedId = previous.id
    focusRequestToken++
    return true
  }

  // ------------------------------------------------------------- formatting

  fun toggleInline(mark: InlineMark, href: String = "") {
    val index = focusedIndex() ?: return
    val block = _blocks[index]
    if (block.type == BlockType.CODE || block.type.isVoid) return
    val selection = block.value.selection
    if (selection.collapsed) return
    pushHistory()
    val marks = MarkSpans.toggle(
      spans = block.marks,
      start = selection.min,
      end = selection.max,
      mark = mark,
      href = href,
      textLength = block.text.length,
    )
    _blocks[index] = block.copy(marks = marks)
    revision++
  }

  fun activeMarks(): Set<InlineMark> {
    val index = focusedIndex() ?: return emptySet()
    val block = _blocks[index]
    val selection = block.value.selection
    if (selection.collapsed) {
      return block.marks.filter { it.start < selection.start && it.end >= selection.start }
        .map { it.mark }
        .toSet()
    }
    return InlineMark.entries.filter {
      MarkSpans.covers(block.marks, selection.min, selection.max, it)
    }.toSet()
  }

  fun linkAtCaret(): MarkSpan? {
    val index = focusedIndex() ?: return null
    val block = _blocks[index]
    val caret = block.value.selection.start
    return block.marks.firstOrNull { it.mark == InlineMark.LINK && caret >= it.start && caret <= it.end }
  }

  fun setBlockType(type: BlockType) {
    val index = focusedIndex() ?: return
    val block = _blocks[index]
    pushHistory()
    val target = if (block.type == type && type != BlockType.PARAGRAPH) BlockType.PARAGRAPH else type
    _blocks[index] = block.copy(
      type = target,
      indent = if (target.isList) block.indent else 0,
      checked = if (target == BlockType.TODO) block.checked else false,
    )
    revision++
    focus(block.id)
  }

  fun focusedBlock(): Block? = focusedIndex()?.let { _blocks[it] }

  fun toggleChecked(id: Long) {
    val index = indexOf(id) ?: return
    pushHistory()
    _blocks[index] = _blocks[index].let { it.copy(checked = !it.checked) }
    revision++
  }

  fun indent(delta: Int) {
    val index = focusedIndex() ?: return
    val block = _blocks[index]
    if (!block.type.isList) return
    val next = (block.indent + delta).coerceIn(0, 5)
    if (next == block.indent) return
    pushHistory()
    _blocks[index] = block.copy(indent = next)
    revision++
  }

  fun setCodeLanguage(id: Long, language: String) {
    val index = indexOf(id) ?: return
    pushHistory()
    _blocks[index] = _blocks[index].copy(language = language)
    revision++
  }

  // --------------------------------------------------------- block insertion

  fun insertDivider() = insertVoid(Block.of(BlockType.DIVIDER))

  fun insertImage(url: String, alt: String = "") = insertVoid(Block.of(BlockType.IMAGE, imageUrl = url, imageAlt = alt))

  fun insertImages(urls: List<String>) {
    urls.forEach { insertImage(it) }
  }

  private fun insertVoid(block: Block) {
    pushHistory()
    val index = focusedIndex()
    if (index == null) {
      _blocks.add(block)
      _blocks.add(Block.paragraph())
      revision++
      focus(_blocks.last().id)
      return
    }
    val current = _blocks[index]
    if (current.text.isEmpty() && current.type == BlockType.PARAGRAPH) {
      _blocks[index] = block
    } else {
      _blocks.add(index + 1, block)
    }
    val trailingIndex = indexOf(block.id)!! + 1
    val trailing = _blocks.getOrNull(trailingIndex)
    if (trailing == null || trailing.type.isVoid) {
      val paragraph = Block.paragraph()
      _blocks.add(trailingIndex, paragraph)
      revision++
      focus(paragraph.id)
    } else {
      revision++
      focus(trailing.id, caretAtEnd = false)
    }
  }

  fun removeBlock(id: Long) {
    val index = indexOf(id) ?: return
    pushHistory()
    _blocks.removeAt(index)
    if (_blocks.isEmpty()) _blocks.add(Block.paragraph())
    revision++
    val neighbour = _blocks.getOrNull(index - 1) ?: _blocks.first()
    focus(neighbour.id)
  }

  fun moveBlock(id: Long, delta: Int) {
    val index = indexOf(id) ?: return
    val target = index + delta
    if (target !in _blocks.indices) return
    pushHistory()
    val block = _blocks.removeAt(index)
    _blocks.add(target, block)
    revision++
  }

  /** Appends [markdown] at the end of the document, used for restoring drafts. */
  fun appendMarkdown(markdown: String) {
    pushHistory()
    val parsed = BlockMarkdown.parse(markdown)
    if (_blocks.size == 1 && _blocks.first().text.isEmpty() && _blocks.first().type == BlockType.PARAGRAPH) {
      _blocks.clear()
    }
    _blocks.addAll(parsed)
    revision++
  }

  fun insertText(raw: String) {
    val index = focusedIndex()
    if (index == null) {
      appendMarkdown(raw)
      return
    }
    val block = _blocks[index]
    pushHistory()
    val selection = block.value.selection
    val text = block.text.replaceRange(selection.min, selection.max, raw)
    val marks = MarkSpans.remap(block.marks, selection.min, selection.max, raw.length, text.length)
    _blocks[index] = block.copy(marks = marks, value = TextFieldValue(text, TextRange(selection.min + raw.length)))
    revision++
  }

  // ------------------------------------------------------------------ undo

  private fun pushHistory() {
    if (suppressHistory) return
    undoStack.addLast(_blocks.map { it.copy() })
    if (undoStack.size > 80) undoStack.removeFirst()
    redoStack.clear()
  }

  fun undo() {
    val snapshot = undoStack.removeLastOrNull() ?: return
    redoStack.addLast(_blocks.map { it.copy() })
    _blocks.clear()
    _blocks.addAll(snapshot)
    revision++
  }

  fun redo() {
    val snapshot = redoStack.removeLastOrNull() ?: return
    undoStack.addLast(_blocks.map { it.copy() })
    _blocks.clear()
    _blocks.addAll(snapshot)
    revision++
  }

  // ------------------------------------------------------------- internals

  private data class Edit(val start: Int, val oldEnd: Int, val insertedLength: Int)

  /** Single-range diff; enough because a text field reports one edit at a time. */
  private fun diff(old: String, new: String): Edit {
    var prefix = 0
    val maxPrefix = minOf(old.length, new.length)
    while (prefix < maxPrefix && old[prefix] == new[prefix]) prefix++

    var suffix = 0
    while (
      suffix < maxPrefix - prefix &&
      old[old.length - 1 - suffix] == new[new.length - 1 - suffix]
    ) suffix++

    val oldEnd = old.length - suffix
    val inserted = new.length - suffix - prefix
    return Edit(prefix, oldEnd, inserted)
  }

  /**
   * Converts markdown markers typed at the start of a block into real block types
   * and strips the marker text, so `## ` becomes an H2 without leaving `## ` behind.
   */
  private fun detectShortcut(block: Block): Block? {
    val text = block.text
    val caret = block.value.selection.start
    if (caret == 0) return null

    fun stripped(prefixLength: Int, type: BlockType, checked: Boolean = false, indent: Int = block.indent): Block {
      val rest = text.substring(prefixLength)
      val marks = MarkSpans.remap(block.marks, 0, prefixLength, 0, rest.length)
      return block.copy(
        type = type,
        checked = checked,
        indent = indent,
        marks = marks,
        value = TextFieldValue(rest, TextRange((caret - prefixLength).coerceAtLeast(0))),
      )
    }

    // Only fire when the marker is immediately before the caret.
    val head = text.substring(0, caret)
    return when {
      block.type == BlockType.PARAGRAPH && head == "# " -> stripped(2, BlockType.H1)
      block.type == BlockType.PARAGRAPH && head == "## " -> stripped(3, BlockType.H2)
      block.type == BlockType.PARAGRAPH && head == "### " -> stripped(4, BlockType.H3)
      block.type == BlockType.PARAGRAPH && (head == "- " || head == "* " || head == "+ ") ->
        stripped(2, BlockType.BULLET)
      block.type == BlockType.PARAGRAPH && head == "1. " -> stripped(3, BlockType.ORDERED)
      block.type == BlockType.PARAGRAPH && head == "> " -> stripped(2, BlockType.QUOTE)
      block.type == BlockType.BULLET && (head == "[] " || head == "[ ] ") ->
        stripped(head.length, BlockType.TODO)
      block.type == BlockType.PARAGRAPH && head == "```" -> stripped(3, BlockType.CODE)
      block.type == BlockType.PARAGRAPH && head == "--- " -> null
      else -> null
    }
  }
}
