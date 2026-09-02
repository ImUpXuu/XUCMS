package com.upxuu.xucms.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.upxuu.xucms.data.VersionManifest

/**
 * Result of an update check. Shows the changelog CI recorded for that build, so the
 * user can see what they are getting rather than just a version number.
 */
@Composable
fun UpdateDialog(
  manifest: VersionManifest,
  installedVersionName: String,
  updateAvailable: Boolean,
  onDismiss: () -> Unit,
  onDownload: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = if (updateAvailable) Icons.Outlined.NewReleases else Icons.Outlined.CheckCircle,
        contentDescription = null,
        tint = if (updateAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
      )
    },
    title = {
      Text(if (updateAvailable) "发现新版本 ${manifest.versionName}" else "已是最新版本")
    },
    text = {
      Column(modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "当前 $installedVersionName",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          if (updateAvailable) {
            Spacer(Modifier.width(6.dp))
            Text(
              text = "→ ${manifest.versionName}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.SemiBold,
            )
          }
        }
        if (manifest.builtAt.isNotBlank()) {
          Text(
            text = "构建于 ${manifest.builtAt} · ${manifest.shortCommit}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
          )
        }

        if (manifest.changes.isNotEmpty()) {
          Spacer(Modifier.height(14.dp))
          Text(
            text = "更新内容",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
          )
          Spacer(Modifier.height(6.dp))
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            manifest.changes.forEach { entry ->
              Row(verticalAlignment = Alignment.Top) {
                Text(
                  text = "·",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.padding(end = 6.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = entry.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                  if (entry.commit.isNotBlank()) {
                    Text(
                      text = entry.commit,
                      style = MaterialTheme.typography.labelSmall,
                      fontFamily = FontFamily.Monospace,
                      color = MaterialTheme.colorScheme.outline,
                    )
                  }
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      if (updateAvailable) {
        TextButton(onClick = onDownload) {
          Text("前往下载", fontWeight = FontWeight.SemiBold)
        }
      } else {
        TextButton(onClick = onDismiss) { Text("好") }
      }
    },
    dismissButton = if (updateAvailable) {
      { TextButton(onClick = onDismiss) { Text("稍后") } }
    } else {
      null
    },
    shape = MaterialTheme.shapes.large,
  )
}
