package com.upxuu.xucms.editor

import com.upxuu.xucms.editor.model.BlockType

/**
 * Every control the editor toolbar can show. The user chooses which of these
 * appear and in what order, so each needs a stable id to persist by.
 */
enum class ToolbarAction(val id: String, val label: String) {
  STYLE_H1("h1", "H1 标题"),
  STYLE_H2("h2", "H2 标题"),
  STYLE_H3("h3", "H3 标题"),
  STYLE_BODY("body", "正文"),
  BOLD("bold", "加粗"),
  ITALIC("italic", "斜体"),
  STRIKE("strike", "删除线"),
  INLINE_CODE("code", "行内代码"),
  LINK("link", "链接"),
  BULLET_LIST("ul", "无序列表"),
  ORDERED_LIST("ol", "有序列表"),
  TODO_LIST("todo", "任务列表"),
  QUOTE("quote", "引用"),
  CODE_BLOCK("codeblock", "代码块"),
  INDENT("indent", "增加缩进"),
  OUTDENT("outdent", "减少缩进"),
  DIVIDER("hr", "分割线"),
  UPLOAD_IMAGE("upload", "上传图片"),
  GALLERY("gallery", "图库"),
  UNDO("undo", "撤销"),
  REDO("redo", "重做");

  /** Block type this action applies, for the ones that set a paragraph style. */
  val blockType: BlockType?
    get() = when (this) {
      STYLE_H1 -> BlockType.H1
      STYLE_H2 -> BlockType.H2
      STYLE_H3 -> BlockType.H3
      STYLE_BODY -> BlockType.PARAGRAPH
      BULLET_LIST -> BlockType.BULLET
      ORDERED_LIST -> BlockType.ORDERED
      TODO_LIST -> BlockType.TODO
      QUOTE -> BlockType.QUOTE
      CODE_BLOCK -> BlockType.CODE
      else -> null
    }

  /** Style actions render as a text chip; everything else as an icon. */
  val isTextChip: Boolean
    get() = this == STYLE_H1 || this == STYLE_H2 || this == STYLE_H3 || this == STYLE_BODY

  companion object {
    fun fromId(id: String): ToolbarAction? = entries.firstOrNull { it.id == id }

    /** Shown by default: the controls a notes app needs within one reach. */
    val defaultEnabled: List<ToolbarAction> = listOf(
      STYLE_H1, STYLE_H2, STYLE_H3, STYLE_BODY,
      BOLD, ITALIC, STRIKE, INLINE_CODE, LINK,
      BULLET_LIST, ORDERED_LIST, TODO_LIST, QUOTE, CODE_BLOCK,
      DIVIDER, UPLOAD_IMAGE, GALLERY,
      UNDO, REDO,
    )

    /** Available but off until the user adds them. */
    val defaultHidden: List<ToolbarAction> = entries.filterNot { it in defaultEnabled }
  }
}

/**
 * Toolbar layout the user configured. [enabled] is ordered; [rows] is 1 or 2.
 * With two rows the enabled actions are split evenly, which lets someone keep
 * every control visible without horizontal scrolling.
 */
data class ToolbarLayout(
  val enabled: List<ToolbarAction> = ToolbarAction.defaultEnabled,
  val rows: Int = 1,
) {
  val hidden: List<ToolbarAction> get() = ToolbarAction.entries.filterNot { it in enabled }

  /** Actions split into the rows they should render in. */
  fun rowsOfActions(): List<List<ToolbarAction>> {
    if (rows <= 1 || enabled.size < 2) return listOf(enabled)
    val half = (enabled.size + 1) / 2
    return listOf(enabled.take(half), enabled.drop(half))
  }

  fun serialize(): String = enabled.joinToString(",") { it.id }

  companion object {
    fun deserialize(raw: String?, rows: Int): ToolbarLayout {
      val actions = raw?.split(',')
        ?.mapNotNull { ToolbarAction.fromId(it.trim()) }
        ?.distinct()
        .orEmpty()
      return ToolbarLayout(
        enabled = actions.ifEmpty { ToolbarAction.defaultEnabled },
        rows = rows.coerceIn(1, 2),
      )
    }
  }
}
