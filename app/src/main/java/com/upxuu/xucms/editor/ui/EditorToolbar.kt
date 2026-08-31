package com.upxuu.xucms.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.FormatStrikethrough
import androidx.compose.material.icons.outlined.HorizontalRule
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.upxuu.xucms.editor.EditorState
import com.upxuu.xucms.editor.model.BlockType
import com.upxuu.xucms.editor.model.InlineMark
import com.upxuu.xucms.ui.components.PressableIcon

/**
 * A single scrollable strip of controls that sits above the keyboard. Paragraph
 * style, inline emphasis and insertions are all one tap deep — no nested menus.
 */
@Composable
fun EditorToolbar(
  state: EditorState,
  modifier: Modifier = Modifier,
  onPickImage: () -> Unit,
  onOpenGallery: () -> Unit,
  onEditLink: () -> Unit,
) {
  val block = state.focusedBlock()
  val active = state.activeMarks()
  val colors = MaterialTheme.colorScheme

  Surface(modifier = modifier.fillMaxWidth(), color = colors.surface) {
    Column {
      HorizontalDivider(color = colors.outlineVariant)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState())
          .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        StyleChip("H1", BlockType.H1, block?.type) { state.setBlockType(BlockType.H1) }
        StyleChip("H2", BlockType.H2, block?.type) { state.setBlockType(BlockType.H2) }
        StyleChip("H3", BlockType.H3, block?.type) { state.setBlockType(BlockType.H3) }
        StyleChip("正文", BlockType.PARAGRAPH, block?.type) { state.setBlockType(BlockType.PARAGRAPH) }

        ToolbarSeparator()

        ToolbarIcon(Icons.Outlined.FormatBold, "加粗", InlineMark.BOLD in active) {
          state.toggleInline(InlineMark.BOLD)
        }
        ToolbarIcon(Icons.Outlined.FormatItalic, "斜体", InlineMark.ITALIC in active) {
          state.toggleInline(InlineMark.ITALIC)
        }
        ToolbarIcon(Icons.Outlined.FormatStrikethrough, "删除线", InlineMark.STRIKE in active) {
          state.toggleInline(InlineMark.STRIKE)
        }
        ToolbarIcon(Icons.Outlined.Code, "行内代码", InlineMark.CODE in active) {
          state.toggleInline(InlineMark.CODE)
        }
        ToolbarIcon(Icons.Outlined.Link, "链接", InlineMark.LINK in active, onClick = onEditLink)

        ToolbarSeparator()

        ToolbarIcon(
          Icons.AutoMirrored.Outlined.FormatListBulleted,
          "无序列表",
          block?.type == BlockType.BULLET,
        ) { state.setBlockType(BlockType.BULLET) }
        ToolbarIcon(Icons.Outlined.FormatListNumbered, "有序列表", block?.type == BlockType.ORDERED) {
          state.setBlockType(BlockType.ORDERED)
        }
        ToolbarIcon(Icons.Outlined.CheckBox, "任务列表", block?.type == BlockType.TODO) {
          state.setBlockType(BlockType.TODO)
        }
        ToolbarIcon(Icons.Outlined.FormatQuote, "引用", block?.type == BlockType.QUOTE) {
          state.setBlockType(BlockType.QUOTE)
        }
        ToolbarIcon(Icons.Outlined.DataObject, "代码块", block?.type == BlockType.CODE) {
          state.setBlockType(BlockType.CODE)
        }

        ToolbarSeparator()

        ToolbarIcon(Icons.Outlined.HorizontalRule, "分割线", false) { state.insertDivider() }
        ToolbarIcon(Icons.Outlined.Image, "上传图片", false, onClick = onPickImage)
        ToolbarIcon(Icons.Outlined.PhotoLibrary, "图库", false, onClick = onOpenGallery)

        ToolbarSeparator()

        ToolbarIcon(Icons.Default.Undo, "撤销", false, enabled = state.canUndo) { state.undo() }
        ToolbarIcon(Icons.Default.Redo, "重做", false, enabled = state.canRedo) { state.redo() }
      }
    }
  }
}

@Composable
private fun ToolbarSeparator() {
  Box(
    modifier = Modifier
      .padding(horizontal = 6.dp)
      .width(1.dp)
      .height(20.dp)
      .background(MaterialTheme.colorScheme.outlineVariant),
  )
}

@Composable
private fun StyleChip(
  label: String,
  type: BlockType,
  current: BlockType?,
  onClick: () -> Unit,
) {
  val selected = current == type
  val colors = MaterialTheme.colorScheme
  Text(
    text = label,
    style = MaterialTheme.typography.labelLarge,
    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
    color = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
    modifier = Modifier
      .clip(RoundedCornerShape(9.dp))
      .background(if (selected) colors.primaryContainer else Color.Transparent)
      .clickable(onClick = onClick)
      .padding(horizontal = 11.dp, vertical = 9.dp),
  )
}

@Composable
private fun ToolbarIcon(
  icon: ImageVector,
  label: String,
  selected: Boolean,
  enabled: Boolean = true,
  onClick: () -> Unit = {},
) {
  val colors = MaterialTheme.colorScheme
  val tint = when {
    !enabled -> colors.onSurfaceVariant.copy(alpha = 0.35f)
    selected -> colors.onPrimaryContainer
    else -> colors.onSurfaceVariant
  }
  PressableIcon(
    onClick = onClick,
    enabled = enabled,
    background = if (selected) colors.primaryContainer else Color.Transparent,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = label,
      tint = tint,
      modifier = Modifier.size(20.dp),
    )
  }
}
