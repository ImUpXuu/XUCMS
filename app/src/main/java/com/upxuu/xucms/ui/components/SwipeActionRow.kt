package com.upxuu.xucms.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * One row, two gestures: swipe right to delete, swipe left to open properties.
 *
 * Built on `SwipeToDismissBox` rather than a hand-rolled `pointerInput`. An
 * earlier custom implementation consumed pointer events itself and so competed
 * with the enclosing list, which broke vertical scrolling and taps. The framework
 * component uses `AnchoredDraggable`, which already resolves that conflict.
 *
 * `confirmValueChange` reports the gesture and then returns false, so the row
 * always springs back to its resting position instead of latching — a swipe by
 * accident leaves the list exactly as it was, and the caller confirms anything
 * destructive separately. The threshold is deliberately more than half the row so
 * a brush while scrolling cannot reach it.
 */
@Composable
fun SwipeActionRow(
  onDelete: () -> Unit,
  onSettings: () -> Unit,
  modifier: Modifier = Modifier,
  deleteLabel: String = "删除",
  settingsLabel: String = "属性",
  content: @Composable () -> Unit,
) {
  val delete = rememberUpdatedState(onDelete)
  val settings = rememberUpdatedState(onSettings)

  val state = rememberSwipeToDismissBoxState(
    positionalThreshold = { width -> width * 0.5f },
    confirmValueChange = { value ->
      when (value) {
        SwipeToDismissBoxValue.StartToEnd -> delete.value()
        SwipeToDismissBoxValue.EndToStart -> settings.value()
        SwipeToDismissBoxValue.Settled -> Unit
      }
      false
    },
  )

  SwipeToDismissBox(
    state = state,
    modifier = modifier.fillMaxWidth(),
    backgroundContent = {
      val deleting = state.dismissDirection != SwipeToDismissBoxValue.EndToStart
      val container = if (deleting) {
        MaterialTheme.colorScheme.errorContainer
      } else {
        MaterialTheme.colorScheme.secondaryContainer
      }
      val tint = if (deleting) {
        MaterialTheme.colorScheme.onErrorContainer
      } else {
        MaterialTheme.colorScheme.onSecondaryContainer
      }
      Box(
        modifier = Modifier
          .fillMaxSize()
          .clip(MaterialTheme.shapes.medium)
          .background(container)
          .padding(horizontal = 20.dp),
        contentAlignment = if (deleting) Alignment.CenterStart else Alignment.CenterEnd,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center,
        ) {
          Icon(
            imageVector = if (deleting) Icons.Outlined.Delete else Icons.Outlined.Tune,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(19.dp),
          )
          Spacer(Modifier.width(8.dp))
          Text(
            text = if (deleting) deleteLabel else settingsLabel,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = tint,
          )
        }
      }
    },
    content = { content() },
  )
}
