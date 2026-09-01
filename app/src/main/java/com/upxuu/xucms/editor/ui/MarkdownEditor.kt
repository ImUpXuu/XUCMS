package com.upxuu.xucms.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.upxuu.xucms.editor.EditorState
import com.upxuu.xucms.editor.model.Block
import com.upxuu.xucms.editor.model.BlockType
import com.upxuu.xucms.ui.theme.Motion

/**
 * The editing surface: a lazy column of per-block text fields. Each block renders
 * with the same style it will have when published, and markers live in the gutter
 * rather than in the text.
 *
 * The caret is kept clear of the keyboard by measuring the cursor rectangle and
 * scrolling by exactly the overlap, springing rather than jumping — so typing past
 * the bottom of the screen advances one line smoothly instead of snapping.
 */
@Composable
fun MarkdownEditor(
  state: EditorState,
  modifier: Modifier = Modifier,
  listState: LazyListState = rememberLazyListState(),
  placeholder: String = "开始写…",
  onImageClick: (Block) -> Unit = {},
) {
  val keyboard = LocalSoftwareKeyboardController.current
  val density = LocalDensity.current

  // Comfortable gap between the caret line and the toolbar/keyboard edge.
  val bottomGap = with(density) { 20.dp.toPx() }
  val topGap = with(density) { 12.dp.toPx() }

  var viewportBottom by remember { mutableFloatStateOf(0f) }
  var viewportTop by remember { mutableFloatStateOf(0f) }
  var caret by remember { mutableStateOf<Rect?>(null) }

  // Forget the caret as soon as the editor loses focus. Otherwise a stale rectangle
  // from the last edited block would drive a scroll when focus moves to the title
  // field, which is what made opening the keyboard jump to a random place.
  LaunchedEffect(state.focusedId) {
    if (state.focusedId == null) caret = null
  }

  LaunchedEffect(caret, viewportTop, viewportBottom) {
    val cursor = caret ?: return@LaunchedEffect
    if (state.focusedId == null) return@LaunchedEffect
    if (viewportBottom - viewportTop <= 0f) return@LaunchedEffect

    // The keyboard resizes the viewport over several frames. Acting on an
    // intermediate size scrolls by the wrong amount, so wait for it to settle.
    val settledBottom = viewportBottom
    val settledTop = viewportTop
    withFrameNanos { }
    if (settledBottom != viewportBottom || settledTop != viewportTop) return@LaunchedEffect

    val below = cursor.bottom - (viewportBottom - bottomGap)
    val above = (viewportTop + topGap) - cursor.top
    val delta = when {
      below > 1f -> below
      // Only pull back up when the caret is genuinely above the viewport, never to
      // "centre" it — unsolicited upward scrolling feels like the page fighting back.
      above > 1f && above < (viewportBottom - viewportTop) -> -above
      else -> 0f
    }
    if (delta != 0f) {
      listState.animateScrollBy(delta, Motion.gentleSpring())
    }
  }

  // A stable callback identity, so every block does not recompose whenever the
  // caret rectangle changes.
  val reportCaret: (Rect) -> Unit = remember { { rect -> caret = rect } }
  val singleEmptyBlock by remember(state) {
    derivedStateOf {
      val blocks = state.blocks
      blocks.size == 1 && blocks[0].text.isEmpty() && blocks[0].type == BlockType.PARAGRAPH
    }
  }

  LazyColumn(
    modifier = modifier.onGloballyPositioned { coords ->
      val bounds = coords.boundsInWindow()
      viewportTop = bounds.top
      viewportBottom = bounds.bottom
    },
    state = listState,
    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 120.dp),
    verticalArrangement = Arrangement.spacedBy(2.dp),
  ) {
    items(state.blocks, key = { it.id }, contentType = { it.type }) { block ->
      BlockRow(
        state = state,
        block = block,
        showPlaceholder = if (singleEmptyBlock) placeholder else null,
        onImageClick = onImageClick,
        onCaretRect = reportCaret,
      )
    }
    item(key = "tail-spacer") {
      // Tapping below the last block appends a paragraph, like a real notepad.
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(160.dp)
          .clickable {
            val last = state.blocks.lastOrNull()
            if (last != null && last.type == BlockType.PARAGRAPH && last.text.isEmpty()) {
              state.focus(last.id)
            } else {
              state.appendMarkdown("")
              state.blocks.lastOrNull()?.let { state.focus(it.id) }
            }
            keyboard?.show()
          },
      )
    }
  }
}

@Composable
private fun BlockRow(
  state: EditorState,
  block: Block,
  showPlaceholder: String?,
  onImageClick: (Block) -> Unit,
  onCaretRect: (Rect) -> Unit,
) {
  when (block.type) {
    BlockType.DIVIDER -> DividerBlock(state, block)
    BlockType.IMAGE -> ImageBlock(state, block, onImageClick)
    BlockType.CODE -> CodeBlock(state, block, onCaretRect)
    BlockType.QUOTE -> QuoteBlock(state, block, showPlaceholder, onCaretRect)
    BlockType.BULLET, BlockType.ORDERED, BlockType.TODO ->
      ListBlock(state, block, showPlaceholder, onCaretRect)
    else -> TextBlock(state, block, showPlaceholder, onCaretRect)
  }
}

@Composable
private fun BlockField(
  state: EditorState,
  block: Block,
  onCaretRect: (Rect) -> Unit,
  modifier: Modifier = Modifier,
  showPlaceholder: String? = null,
  singleLineEnter: Boolean = true,
) {
  val focusRequester = remember(block.id) { FocusRequester() }
  val style = blockTextStyle(block.type)
  val transformation = rememberMarkTransformation(block.marks)
  val colors = MaterialTheme.colorScheme

  var layout by remember(block.id) { mutableStateOf<TextLayoutResult?>(null) }
  var fieldOrigin by remember(block.id) { mutableStateOf(Rect.Zero) }

  LaunchedEffect(state.focusRequestToken, state.focusedId) {
    if (state.focusedId == block.id) {
      runCatching { focusRequester.requestFocus() }
    }
  }

  // Report where the caret sits in window space whenever it, the text or the
  // layout moves — that is what the scroller needs to keep it above the keyboard.
  val caretOffset = block.value.selection.start
  LaunchedEffect(state.focusedId, caretOffset, layout, fieldOrigin, block.text) {
    if (state.focusedId != block.id) return@LaunchedEffect
    val result = layout ?: return@LaunchedEffect
    val safeOffset = caretOffset.coerceIn(0, result.layoutInput.text.length)
    val cursor = runCatching { result.getCursorRect(safeOffset) }.getOrNull() ?: return@LaunchedEffect
    onCaretRect(cursor.translate(fieldOrigin.left, fieldOrigin.top))
  }

  Box(modifier = modifier) {
    if (showPlaceholder != null && block.text.isEmpty()) {
      Text(
        text = showPlaceholder,
        style = style,
        color = colors.onSurfaceVariant.copy(alpha = 0.55f),
      )
    }
    BasicTextField(
      value = block.value,
      onValueChange = { state.onTextChange(block.id, it) },
      onTextLayout = { layout = it },
      modifier = Modifier
        .fillMaxWidth()
        .onGloballyPositioned { fieldOrigin = it.boundsInWindow() }
        .focusRequester(focusRequester)
        .onFocusChanged { if (it.isFocused) state.focus(block.id, caretAtEnd = false) }
        .onPreviewKeyEvent { event ->
          if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
          when (event.key) {
            Key.Enter, Key.NumPadEnter -> {
              if (!singleLineEnter) return@onPreviewKeyEvent false
              state.onEnter(block.id)
              true
            }
            Key.Backspace -> {
              val selection = block.value.selection
              if (selection.collapsed && selection.start == 0) {
                state.onBackspaceAtStart(block.id)
              } else {
                false
              }
            }
            else -> false
          }
        },
      textStyle = style,
      cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.primary),
      visualTransformation = transformation,
      keyboardOptions = KeyboardOptions(
        imeAction = if (singleLineEnter) ImeAction.Default else ImeAction.None,
      ),
      keyboardActions = KeyboardActions(onAny = { state.onEnter(block.id) }),
      singleLine = false,
    )
  }
}

private fun Rect.translate(dx: Float, dy: Float): Rect =
  Rect(left + dx, top + dy, right + dx, bottom + dy)

@Composable
private fun TextBlock(
  state: EditorState,
  block: Block,
  showPlaceholder: String?,
  onCaretRect: (Rect) -> Unit,
) {
  val topPadding = when (block.type) {
    BlockType.H1 -> 20.dp
    BlockType.H2 -> 16.dp
    BlockType.H3 -> 12.dp
    else -> 2.dp
  }
  Column(modifier = Modifier.fillMaxWidth().padding(top = topPadding, bottom = 2.dp)) {
    BlockField(state, block, onCaretRect, showPlaceholder = showPlaceholder)
    if (block.type == BlockType.H2) {
      Spacer(Modifier.height(6.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), thickness = 2.dp)
    }
  }
}

@Composable
private fun QuoteBlock(
  state: EditorState,
  block: Block,
  showPlaceholder: String?,
  onCaretRect: (Rect) -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(IntrinsicSize.Min)
      .padding(vertical = 6.dp)
      .clip(RoundedCornerShape(4.dp))
      .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
  ) {
    Box(
      modifier = Modifier
        .width(3.dp)
        .fillMaxHeight()
        .background(MaterialTheme.colorScheme.primary),
    )
    BlockField(
      state = state,
      block = block,
      onCaretRect = onCaretRect,
      modifier = Modifier.padding(start = 12.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
      showPlaceholder = showPlaceholder,
    )
  }
}

@Composable
private fun ListBlock(
  state: EditorState,
  block: Block,
  showPlaceholder: String?,
  onCaretRect: (Rect) -> Unit,
) {
  val index = state.blocks.indexOfFirst { it.id == block.id }
  val ordinal = remember(state.revision, block.id, block.indent) {
    if (block.type != BlockType.ORDERED) 0 else {
      var count = 1
      var cursor = index - 1
      while (cursor >= 0) {
        val prev = state.blocks[cursor]
        if (prev.type == BlockType.ORDERED && prev.indent == block.indent) count++
        else if (prev.type.isList && prev.indent > block.indent) Unit
        else break
        cursor--
      }
      count
    }
  }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(start = (block.indent * 20).dp, top = 1.dp, bottom = 1.dp),
    verticalAlignment = Alignment.Top,
  ) {
    when (block.type) {
      BlockType.TODO -> IconButton(
        onClick = { state.toggleChecked(block.id) },
        modifier = Modifier.size(26.dp),
      ) {
        Icon(
          imageVector = if (block.checked) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
          contentDescription = if (block.checked) "已完成" else "未完成",
          tint = if (block.checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
          modifier = Modifier.size(19.dp),
        )
      }
      BlockType.ORDERED -> Text(
        text = "$ordinal.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 3.dp, end = 8.dp),
      )
      else -> Text(
        text = "•",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 3.dp, end = 10.dp),
      )
    }
    BlockField(
      state = state,
      block = block,
      onCaretRect = onCaretRect,
      modifier = Modifier.padding(top = 3.dp),
      showPlaceholder = showPlaceholder,
    )
  }
}

@Composable
private fun CodeBlock(state: EditorState, block: Block, onCaretRect: (Rect) -> Unit) {
  Surface(
    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    shape = RoundedCornerShape(10.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = block.language.ifBlank { "code" },
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontFamily = FontFamily.Monospace,
          modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { state.removeBlock(block.id) }, modifier = Modifier.size(24.dp)) {
          Icon(
            Icons.Outlined.Close,
            contentDescription = "删除代码块",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
          )
        }
      }
      Spacer(Modifier.height(6.dp))
      BlockField(state, block, onCaretRect, singleLineEnter = false)
    }
  }
}

@Composable
private fun DividerBlock(state: EditorState, block: Block) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 14.dp)
      .clickable { state.focus(block.id) },
    verticalAlignment = Alignment.CenterVertically,
  ) {
    HorizontalDivider(
      modifier = Modifier.weight(1f),
      color = MaterialTheme.colorScheme.outlineVariant,
      thickness = 2.dp,
    )
    IconButton(onClick = { state.removeBlock(block.id) }, modifier = Modifier.size(24.dp)) {
      Icon(
        Icons.Outlined.Close,
        contentDescription = "删除分割线",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(15.dp),
      )
    }
  }
}

@Composable
private fun ImageBlock(state: EditorState, block: Block, onImageClick: (Block) -> Unit) {
  Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
    Box {
      AsyncImage(
        model = block.imageUrl,
        contentDescription = block.imageAlt.ifBlank { "插图" },
        contentScale = ContentScale.FillWidth,
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant)
          .clickable { onImageClick(block) },
      )
      Surface(
        modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.scrim,
      ) {
        IconButton(onClick = { state.removeBlock(block.id) }, modifier = Modifier.size(28.dp)) {
          Icon(
            Icons.Outlined.Close,
            contentDescription = "移除图片",
            tint = androidx.compose.ui.graphics.Color.White,
            modifier = Modifier.size(16.dp),
          )
        }
      }
    }
    if (block.imageAlt.isNotBlank()) {
      Spacer(Modifier.height(4.dp))
      Text(
        text = block.imageAlt,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
      )
    }
  }
}
