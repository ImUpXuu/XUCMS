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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * One row, two gestures: swipe right to delete, swipe left to open settings.
 *
 * Both gestures are reported to the caller and the row springs back rather than
 * vanishing. Deletion is confirmed with an undoable snackbar by the caller, so a
 * mis-swipe never destroys anything immediately.
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
  var pending by remember { mutableStateOf<SwipeToDismissBoxValue?>(null) }
  val dismissState = rememberSwipeToDismissBoxState(
    positionalThreshold = { distance -> distance * 0.45f },
  )

  LaunchedEffect(dismissState.currentValue) {
    val value = dismissState.currentValue
    if (value != SwipeToDismissBoxValue.Settled) pending = value
  }

  LaunchedEffect(pending) {
    val value = pending ?: return@LaunchedEffect
    pending = null
    dismissState.reset()
    when (value) {
      SwipeToDismissBoxValue.StartToEnd -> onDelete()
      SwipeToDismissBoxValue.EndToStart -> onSettings()
      SwipeToDismissBoxValue.Settled -> Unit
    }
  }

  SwipeToDismissBox(
    state = dismissState,
    modifier = modifier,
    backgroundContent = {
      val deleting = dismissState.dismissDirection != SwipeToDismissBoxValue.EndToStart
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
    content = { Box(modifier = Modifier.fillMaxWidth()) { content() } },
  )
}
