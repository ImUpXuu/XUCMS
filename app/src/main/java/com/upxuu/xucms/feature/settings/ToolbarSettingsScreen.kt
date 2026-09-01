package com.upxuu.xucms.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.upxuu.xucms.LocalAppContainer
import com.upxuu.xucms.editor.ToolbarAction
import com.upxuu.xucms.editor.ToolbarLayout
import com.upxuu.xucms.editor.ui.ToolbarControl
import com.upxuu.xucms.ui.components.SectionLabel

/**
 * Toolbar customisation. The top of the screen is the toolbar itself, rendered with
 * the same composable the editor uses, so what the user arranges is literally what
 * they will get. Long-press an item to drag it; tap to remove.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolbarSettingsScreen(onBack: () -> Unit) {
  val container = LocalAppContainer.current
  val settings = container.settings
  val saved = settings.toolbarLayout

  val enabled = remember { mutableStateListOf<ToolbarAction>().apply { addAll(saved.enabled) } }
  var rows by remember { mutableStateOf(saved.rows) }

  // Persist on every change: there is no confirm button, so the preview above and
  // the real toolbar can never disagree.
  fun persist() {
    settings.toolbarLayout = ToolbarLayout(enabled.toList(), rows)
  }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        title = { Text("工具栏", style = MaterialTheme.typography.titleMedium) },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
          }
        },
        actions = {
          IconButton(onClick = {
            enabled.clear()
            enabled.addAll(ToolbarAction.defaultEnabled)
            rows = 1
            persist()
          }) {
            Icon(Icons.Outlined.RestartAlt, contentDescription = "恢复默认")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background,
        ),
      )
    },
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .verticalScroll(rememberScrollState()),
    ) {
      SectionLabel("预览与排序")
      Text(
        text = "长按拖动调整顺序，点按移除。这里的样子就是编辑器里的样子。",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
      )
      Spacer(Modifier.height(10.dp))

      ToolbarPreview(
        enabled = enabled,
        rows = rows,
        onReorder = { from, to ->
          if (from in enabled.indices && to in enabled.indices) {
            enabled.add(to, enabled.removeAt(from))
            persist()
          }
        },
        onRemove = { action ->
          enabled.remove(action)
          persist()
        },
      )

      SectionLabel("行数")
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        listOf(1 to "单行", 2 to "双行").forEach { (value, label) ->
          val active = rows == value
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            border = if (active) {
              null
            } else {
              androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            },
            modifier = Modifier.weight(1f).clickable { rows = value; persist() },
          ) {
            Text(
              text = label,
              style = MaterialTheme.typography.labelLarge,
              fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
              color = if (active) {
                MaterialTheme.colorScheme.onPrimary
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              },
              textAlign = androidx.compose.ui.text.style.TextAlign.Center,
              modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp),
            )
          }
        }
      }
      Text(
        text = "双行会把已启用的功能平均分到两行，适合想一眼看到全部功能的情况。",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
      )

      SectionLabel("未启用")
      val hidden = ToolbarAction.entries.filterNot { it in enabled }
      if (hidden.isEmpty()) {
        Text(
          text = "所有功能都已加入工具栏。",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.outline,
          modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
        )
      } else {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
          hidden.forEach { action ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable {
                  enabled.add(action)
                  persist()
                }
                .padding(horizontal = 6.dp, vertical = 11.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = action.label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
              )
              Icon(
                Icons.Outlined.Add,
                contentDescription = "加入工具栏",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(19.dp),
              )
            }
          }
        }
      }

      Spacer(Modifier.height(40.dp))
    }
  }
}

/**
 * The live preview. Each row is horizontally scrollable exactly like the editor's,
 * and an item being dragged follows the finger while its neighbours shift under it.
 */
@Composable
private fun ToolbarPreview(
  enabled: List<ToolbarAction>,
  rows: Int,
  onReorder: (Int, Int) -> Unit,
  onRemove: (ToolbarAction) -> Unit,
) {
  val haptics = LocalHapticFeedback.current
  var dragging by remember { mutableStateOf<ToolbarAction?>(null) }
  var dragOffset by remember { mutableStateOf(0f) }
  // Centre x of every item, keyed by action, measured in its row's coordinates.
  val centres = remember { mutableStateOf(mapOf<ToolbarAction, Float>()) }

  val layout = ToolbarLayout(enabled, rows)

  Surface(
    modifier = Modifier.fillMaxWidth(),
    color = MaterialTheme.colorScheme.surface,
  ) {
    Column {
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
      layout.rowsOfActions().forEachIndexed { rowIndex, actions ->
        if (rowIndex > 0) {
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        }
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 10.dp, vertical = 5.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          actions.forEach { action ->
            val isDragging = dragging == action
            Box(
              modifier = Modifier
                .zIndex(if (isDragging) 1f else 0f)
                .graphicsLayer {
                  if (isDragging) {
                    translationX = dragOffset
                    scaleX = 1.12f
                    scaleY = 1.12f
                  }
                }
                .onGloballyPositioned { coords ->
                  val centre = coords.positionInParent().x + coords.size.width / 2f
                  centres.value = centres.value + (action to centre)
                }
                .pointerInput(action, enabled.size) {
                  detectDragGesturesAfterLongPress(
                    onDragStart = {
                      dragging = action
                      dragOffset = 0f
                      haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDrag = { _, amount ->
                      dragOffset += amount.x
                      val from = enabled.indexOf(action)
                      val origin = centres.value[action] ?: return@detectDragGesturesAfterLongPress
                      val target = origin + dragOffset
                      // Swap as soon as the dragged item passes a neighbour's centre.
                      val overlapped = centres.value
                        .filterKeys { it != action && it in actions }
                        .minByOrNull { kotlin.math.abs(it.value - target) }
                      if (overlapped != null && kotlin.math.abs(overlapped.value - target) < 24f) {
                        val to = enabled.indexOf(overlapped.key)
                        if (from >= 0 && to >= 0 && from != to) {
                          onReorder(from, to)
                          dragOffset = 0f
                          haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                      }
                    },
                    onDragEnd = { dragging = null; dragOffset = 0f },
                    onDragCancel = { dragging = null; dragOffset = 0f },
                  )
                },
            ) {
              ToolbarControl(
                action = action,
                selected = false,
                enabled = true,
                onClick = { onRemove(action) },
              )
              // A hairline marker so it is obvious a tap removes rather than applies.
              Box(
                modifier = Modifier
                  .align(Alignment.TopEnd)
                  .size(11.dp)
                  .clip(RoundedCornerShape(50))
                  .background(MaterialTheme.colorScheme.error),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  Icons.Outlined.Close,
                  contentDescription = "移除",
                  tint = MaterialTheme.colorScheme.onError,
                  modifier = Modifier.size(8.dp),
                )
              }
            }
          }
        }
      }
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
  }
}
