package com.upxuu.xucms.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.upxuu.xucms.ui.theme.Motion

/** Square tappable icon slot with a rounded highlight; used across the toolbar. */
@Composable
fun PressableIcon(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  background: Color = Color.Transparent,
  content: @Composable () -> Unit,
) {
  val interaction = remember { MutableInteractionSource() }
  Box(
    modifier = modifier
      .size(38.dp)
      .clip(RoundedCornerShape(9.dp))
      .background(background)
      .clickable(
        enabled = enabled,
        interactionSource = interaction,
        indication = ripple(),
        onClick = onClick,
      ),
    contentAlignment = Alignment.Center,
  ) { content() }
}

/** A flat card: no elevation, a hairline outline — the app's only container style. */
@Composable
fun FlatCard(
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
  color: Color = MaterialTheme.colorScheme.surface,
  border: Boolean = true,
  content: @Composable () -> Unit,
) {
  Surface(
    modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier,
    shape = MaterialTheme.shapes.medium,
    color = color,
    border = if (border) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
    content = { content() },
  )
}

/** A settings row: label, optional value, chevron. */
@Composable
fun SettingRow(
  icon: ImageVector,
  title: String,
  subtitle: String? = null,
  onClick: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 18.dp, vertical = 15.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(21.dp),
    )
    Spacer(Modifier.size(14.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
      if (subtitle != null) {
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
    Icon(
      imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.outline,
      modifier = Modifier.size(20.dp),
    )
  }
}

/** Section label used above grouped content. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
  Text(
    text = text,
    style = MaterialTheme.typography.labelSmall,
    fontWeight = FontWeight.SemiBold,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = modifier.padding(start = 20.dp, top = 18.dp, bottom = 8.dp),
  )
}

/** Small status pill, e.g. 草稿 / 未同步. Scales in so it does not just pop. */
@Composable
fun Pill(
  text: String,
  color: Color = MaterialTheme.colorScheme.primary,
  container: Color = MaterialTheme.colorScheme.primaryContainer,
) {
  Surface(shape = RoundedCornerShape(6.dp), color = container) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.SemiBold,
      color = color,
      modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
    )
  }
}

/** Centered empty state with a hint line; fades up so an empty list is not abrupt. */
@Composable
fun EmptyState(
  icon: ImageVector,
  title: String,
  hint: String? = null,
  modifier: Modifier = Modifier,
) {
  val entered = remember { Animatable(0f) }
  LaunchedEffect(Unit) { entered.animateTo(1f, Motion.gentleSpring()) }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(32.dp)
      .graphicsLayer {
        alpha = entered.value
        translationY = (1f - entered.value) * 24f
      },
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.outlineVariant,
      modifier = Modifier.size(44.dp),
    )
    Spacer(Modifier.height(14.dp))
    Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    if (hint != null) {
      Spacer(Modifier.height(5.dp))
      Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
    }
  }
}

@Composable
fun ThinDivider(modifier: Modifier = Modifier) {
  HorizontalDivider(modifier = modifier, color = MaterialTheme.colorScheme.outlineVariant)
}
