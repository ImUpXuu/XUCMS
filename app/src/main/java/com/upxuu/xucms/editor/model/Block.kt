package com.upxuu.xucms.editor.model

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/** Block kinds the editor understands. Inline emphasis lives in [Block.marks]. */
enum class BlockType {
  PARAGRAPH,
  H1,
  H2,
  H3,
  BULLET,
  ORDERED,
  TODO,
  QUOTE,
  CODE,
  DIVIDER,
  IMAGE;

  val isList: Boolean get() = this == BULLET || this == ORDERED || this == TODO
  val isHeading: Boolean get() = this == H1 || this == H2 || this == H3
  /** Blocks with no editable text of their own. */
  val isVoid: Boolean get() = this == DIVIDER || this == IMAGE
}

/**
 * One editable line/paragraph. Markdown markers (`## `, `- `, `> `, `**`) are never
 * part of [value]; block markers are implied by [type] and inline emphasis lives in
 * [marks]. That separation is what makes the editing surface read like the rendered
 * result instead of like source text.
 */
data class Block(
  val id: Long,
  val type: BlockType = BlockType.PARAGRAPH,
  val value: TextFieldValue = TextFieldValue(""),
  val marks: List<MarkSpan> = emptyList(),
  /** Nesting depth for list items, 0-based. */
  val indent: Int = 0,
  val checked: Boolean = false,
  /** Info string of a fenced code block, e.g. `kotlin`. */
  val language: String = "",
  val imageUrl: String = "",
  val imageAlt: String = "",
) {
  val text: String get() = value.text

  fun withText(newText: String, caret: Int = newText.length): Block =
    copy(value = TextFieldValue(newText, TextRange(caret.coerceIn(0, newText.length))))

  companion object {
    private var seq = 0L
    fun nextId(): Long = ++seq

    fun paragraph(text: String = ""): Block = of(BlockType.PARAGRAPH, text)

    fun of(
      type: BlockType,
      text: String = "",
      marks: List<MarkSpan> = emptyList(),
      indent: Int = 0,
      checked: Boolean = false,
      language: String = "",
      imageUrl: String = "",
      imageAlt: String = "",
    ): Block = Block(
      id = nextId(),
      type = type,
      marks = MarkSpans.normalize(marks, text.length),
      indent = indent,
      checked = checked,
      language = language,
      imageUrl = imageUrl,
      imageAlt = imageAlt,
    ).withText(text, 0)
  }
}
