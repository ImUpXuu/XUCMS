package com.upxuu.xucms.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * One row, two gestures: drag right to delete, drag left to open properties.
 *
 * Hand-rolled rather than built on `SwipeToDismissBox`, which latches once its
 * threshold is crossed so a half-swipe cannot be pulled back. Here the row follows
 * the finger and always springs home on release.
 *
 * Two things keep it from stealing the list's vertical scroll or firing by
 * accident: the gesture is only claimed after the finger passes the horizontal
 * touch slop (a vertical drag is left to the parent), and the action needs a long,
 * deliberate pull — well past the point where the row visibly moves.
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
  val scope = rememberCoroutineScope()
  val density = LocalDensity.current
  val offset = remember { Animatable(0f) }
  val threshold = with(density) { 168.dp.toPx() }
  val maxDrag = with(density) { 200.dp.toPx() }

  Box(modifier = modifier.fillMaxWidth()) {
    val shift = offset.value
    val progress = (abs(shift) / threshold).coerceIn(0f, 1f)
    val deleting = shift > 0f
    // Only call the action armed once the pull is far enough to fire, so the hint
    // tells the truth about what releasing will do.
    val armed = abs(shift) >= threshold

    if (abs(shift) > 0.5f) {
      Box(
        modifier = Modifier
          .matchParentSize()
          .clip(MaterialTheme.shapes.medium)
          .background(
            (if (deleting) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer)
              .copy(alpha = 0.25f + 0.75f * progress),
          )
          .padding(horizontal = 20.dp),
        contentAlignment = if (deleting) Alignment.CenterStart else Alignment.CenterEnd,
      ) {
        val tint = if (deleting) {
          MaterialTheme.colorScheme.onErrorContainer
        } else {
          MaterialTheme.colorScheme.onSecondaryContainer
        }
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center,
          modifier = Modifier.graphicsLayer {
            val scale = 0.8f + 0.2f * progress
            scaleX = scale
            scaleY = scale
            alpha = 0.3f + 0.7f * progress
          },
        ) {
          Icon(
            imageVector = if (deleting) Icons.Outlined.Delete else Icons.Outlined.Tune,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(19.dp),
          )
          Spacer(Modifier.width(8.dp))
          Text(
            text = when {
              armed && deleting -> deleteLabel
              armed -> settingsLabel
              else -> "继续滑动"
            },
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = tint,
          )
        }
      }
    }

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .graphicsLayer { translationX = offset.value }
        .pointerInput(Unit) {
          awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var dragged = 0f

            // Leave vertical drags to the enclosing list: this returns null when
            // the parent claims the gesture first.
            val slopPassed = awaitHorizontalTouchSlopOrCancellation(down.id) { change, overSlop ->
              dragged = overSlop
              change.consume()
            } ?: return@awaitEachGesture

            scope.launch { offset.snapTo(dragged) }

            horizontalDrag(slopPassed.id) { change ->
              dragged = (dragged + change.positionChange().x).coerceIn(-maxDrag, maxDrag)
              scope.launch { offset.snapTo(dragged) }
              change.consume()
            }

            val released = dragged
            scope.launch {
              offset.animateTo(0f, spring(dampingRatio = 0.78f, stiffness = 620f))
            }
            if (abs(released) >= threshold) {
              if (released > 0f) onDelete() else onSettings()
            }
          }
        },
    ) {
      content()
    }
  }
}
