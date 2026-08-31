package com.upxuu.xucms.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight

/**
 * Confirmation for anything destructive. Every delete path in the app goes
 * through this, and the caller still offers an undo afterwards.
 */
@Composable
fun ConfirmDeleteDialog(
  title: String,
  message: String,
  confirmLabel: String = "删除",
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title) },
    text = { Text(message) },
    confirmButton = {
      TextButton(onClick = onConfirm) {
        Text(
          text = confirmLabel,
          color = MaterialTheme.colorScheme.error,
          fontWeight = FontWeight.SemiBold,
        )
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    shape = MaterialTheme.shapes.large,
  )
}
