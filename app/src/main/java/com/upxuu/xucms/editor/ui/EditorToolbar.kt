package com.upxuu.xucms.editor.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatIndentDecrease
import androidx.compose.material.icons.automirrored.outlined.FormatIndentIncrease
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.upxuu.xucms.editor.EditorState
import com.upxuu.xucms.editor.ToolbarAction
import com.upxuu.xucms.editor.ToolbarLayout
import com.upxuu.xucms.editor.model.BlockType
import com.upxuu.xucms.editor.model.InlineMark
import com.upxuu.xucms.ui.components.PressableIcon
import com.upxuu.xucms.ui.theme.Motion

/** Icon for an action, or null when it renders as a text chip instead. */
fun toolbarIconFor(action: ToolbarAction): ImageVector? = when (action) {
  ToolbarAction.BOLD -> Icons.Outlined.FormatBold
  ToolbarAction.ITALIC -> Icons.Outlined.FormatItalic
  ToolbarAction.STRIKE -> Icons.Outlined.FormatStrikethrough
  ToolbarAction.INLINE_CODE -> Icons.Outlined.Code
  ToolbarAction.LINK -> Icons.Outlined.Link
  ToolbarAction.BULLET_LIST -> Icons.AutoMirrored.Outlined.FormatListBulleted
  ToolbarAction.ORDERED_LIST -> Icons.Outlined.FormatListNumbered
  ToolbarAction.TODO_LIST -> Icons.Outlined.CheckBox
  ToolbarAction.QUOTE -> Icons.Outlined.FormatQuote
  ToolbarAction.CODE_BLOCK -> Icons.Outlined.DataObject
  ToolbarAction.INDENT -> Icons.AutoMirrored.Outlined.FormatIndentIncrease
  ToolbarAction.OUTDENT -> Icons.AutoMirrored.Outlined.FormatIndentDecrease
  ToolbarAction.DIVIDER -> Icons.Outlined.HorizontalRule
  ToolbarAction.UPLOAD_IMAGE -> Icons.Outlined.Image
  ToolbarAction.GALLERY -> Icons.Outlined.PhotoLibrary
  ToolbarAction.UNDO -> Icons.Default.Undo
  ToolbarAction.REDO -> Icons.Default.Redo
  else -> null
}

/** Short label for the style chips. */
fun toolbarChipLabel(action: ToolbarAction): String = when (action) {
  ToolbarAction.STYLE_H1 -> "H1"
  ToolbarAction.STYLE_H2 -> "H2"
  ToolbarAction.STYLE_H3 -> "H3"
  else -> "正文"
}

/**
 * The strip of controls above the keyboard. Which controls appear, their order and
 * whether they wrap onto a second row all come from [layout], configured in
 * settings, so this composable only renders and dispatches.
 */
@Composable
fun EditorToolbar(
  state: EditorState,
  layout: ToolbarLayout,
  modifier: Modifier = Modifier,
  onPickImage: () -> Unit,
  onOpenGallery: () -> Unit,
  onEditLink: () -> Unit,
) {
  val colors = MaterialTheme.colorScheme
  val block = state.focusedBlock()
  val active = state.activeMarks()

  Surface(modifier = modifier.fillMaxWidth(), color = colors.surface) {
    Column {
      HorizontalDivider(color = colors.outlineVariant)
      layout.rowsOfActions().forEachIndexed { index, actions ->
        if (index > 0) HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.4f))
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 10.dp, vertical = 5.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          actions.forEach { action ->
            ToolbarControl(
              action = action,
              selected = isSelected(action, block?.type, active),
              enabled = isEnabled(action, state),
              onClick = { dispatch(action, state, onPickImage, onOpenGallery, onEditLink) },
            )
          }
        }
      }
    }
  }
}

/** Shared by the toolbar and by the settings screen that previews it. */
@Composable
fun ToolbarControl(
  action: ToolbarAction,
  selected: Boolean,
  enabled: Boolean,
  onClick: () -> Unit,
) {
  val colors = MaterialTheme.colorScheme
  val container by animateColorAsState(
    targetValue = if (selected) colors.primaryContainer else Color.Transparent,
    animationSpec = Motion.normalTween(),
    label = "toolbar-container",
  )
  val contentColor by animateColorAsState(
    targetValue = when {
      !enabled -> colors.onSurfaceVariant.copy(alpha = 0.35f)
      selected -> colors.onPrimaryContainer
      else -> colors.onSurfaceVariant
    },
    animationSpec = Motion.normalTween(),
    label = "toolbar-content",
  )
  val scale by animateFloatAsState(
    targetValue = if (selected) 1.06f else 1f,
    animationSpec = Motion.snappySpring(),
    label = "toolbar-scale",
  )

  val icon = toolbarIconFor(action)
  if (icon == null) {
    Text(
      text = toolbarChipLabel(action),
      style = MaterialTheme.typography.labelLarge,
      fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
      color = contentColor,
      modifier = Modifier
        .scale(scale)
        .clip(RoundedCornerShape(9.dp))
        .background(container)
        .clickable(enabled = enabled, onClick = onClick)
        .padding(horizontal = 11.dp, vertical = 9.dp),
    )
  } else {
    PressableIcon(onClick = onClick, enabled = enabled, background = container) {
      Icon(
        imageVector = icon,
        contentDescription = action.label,
        tint = contentColor,
        modifier = Modifier.size(20.dp).scale(scale),
      )
    }
  }
}

private fun isSelected(
  action: ToolbarAction,
  blockType: BlockType?,
  active: Set<InlineMark>,
): Boolean = when (action) {
  ToolbarAction.BOLD -> InlineMark.BOLD in active
  ToolbarAction.ITALIC -> InlineMark.ITALIC in active
  ToolbarAction.STRIKE -> InlineMark.STRIKE in active
  ToolbarAction.INLINE_CODE -> InlineMark.CODE in active
  ToolbarAction.LINK -> InlineMark.LINK in active
  else -> action.blockType != null && action.blockType == blockType
}

private fun isEnabled(action: ToolbarAction, state: EditorState): Boolean = when (action) {
  ToolbarAction.UNDO -> state.canUndo
  ToolbarAction.REDO -> state.canRedo
  ToolbarAction.INDENT, ToolbarAction.OUTDENT -> state.focusedBlock()?.type?.isList == true
  else -> true
}

private fun dispatch(
  action: ToolbarAction,
  state: EditorState,
  onPickImage: () -> Unit,
  onOpenGallery: () -> Unit,
  onEditLink: () -> Unit,
) {
  when (action) {
    ToolbarAction.BOLD -> state.toggleInline(InlineMark.BOLD)
    ToolbarAction.ITALIC -> state.toggleInline(InlineMark.ITALIC)
    ToolbarAction.STRIKE -> state.toggleInline(InlineMark.STRIKE)
    ToolbarAction.INLINE_CODE -> state.toggleInline(InlineMark.CODE)
    ToolbarAction.LINK -> onEditLink()
    ToolbarAction.DIVIDER -> state.insertDivider()
    ToolbarAction.UPLOAD_IMAGE -> onPickImage()
    ToolbarAction.GALLERY -> onOpenGallery()
    ToolbarAction.UNDO -> state.undo()
    ToolbarAction.REDO -> state.redo()
    ToolbarAction.INDENT -> state.indent(1)
    ToolbarAction.OUTDENT -> state.indent(-1)
    else -> action.blockType?.let { state.setBlockType(it) }
  }
}
