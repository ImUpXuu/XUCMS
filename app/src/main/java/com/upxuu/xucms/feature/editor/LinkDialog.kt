package com.upxuu.xucms.feature.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Link editor. Applies to the current selection; when the caret already sits in a
 * link, the existing target is prefilled so editing is one tap away.
 */
@Composable
fun LinkDialog(
  initialHref: String,
  hasSelection: Boolean,
  onDismiss: () -> Unit,
  onApply: (String) -> Unit,
  onRemove: () -> Unit,
) {
  var href by remember { mutableStateOf(initialHref) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (initialHref.isBlank()) "添加链接" else "编辑链接") },
    text = {
      Column {
        if (!hasSelection) {
          Text(
            text = "请先选中要添加链接的文字。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
          )
          Spacer(Modifier.height(10.dp))
        }
        OutlinedTextField(
          value = href,
          onValueChange = { href = it },
          label = { Text("链接地址") },
          placeholder = { Text("https://") },
          singleLine = true,
          shape = MaterialTheme.shapes.medium,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      TextButton(
        onClick = { onApply(href.trim()) },
        enabled = hasSelection && href.isNotBlank(),
      ) {
        Text("应用", fontWeight = FontWeight.SemiBold)
      }
    },
    dismissButton = {
      if (initialHref.isNotBlank()) {
        TextButton(onClick = onRemove) { Text("移除链接") }
      } else {
        TextButton(onClick = onDismiss) { Text("取消") }
      }
    },
    shape = MaterialTheme.shapes.large,
  )
}
