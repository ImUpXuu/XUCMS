package com.upxuu.xucms.feature.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.upxuu.xucms.data.PostMeta
import com.upxuu.xucms.data.TalkMeta
import com.upxuu.xucms.ui.components.MetaFields

/** Metadata editor. Everything optional lives here so the writing surface stays bare. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetaSheet(
  state: EditorUiState,
  onDismiss: () -> Unit,
  onPostChange: ((PostMeta) -> PostMeta) -> Unit,
  onTalkChange: ((TalkMeta) -> TalkMeta) -> Unit,
  onFilenameChange: (String) -> Unit,
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .imePadding()
        .padding(horizontal = 20.dp)
        .padding(bottom = 32.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = "属性",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
      )

      Text(
        text = "文件名：${state.targetFilename()}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      if (state.isNew) {
        OutlinedTextField(
          value = state.customFilename,
          onValueChange = onFilenameChange,
          label = { Text("自定义文件名（可留空，无需 .md）") },
          singleLine = true,
          shape = MaterialTheme.shapes.medium,
          modifier = Modifier.fillMaxWidth(),
        )
      }

      MetaFields(
        kind = state.kind,
        postMeta = state.postMeta,
        talkMeta = state.talkMeta,
        onPostChange = onPostChange,
        onTalkChange = onTalkChange,
        includeTitle = false,
      )

      Spacer(Modifier.height(4.dp))
    }
  }
}
