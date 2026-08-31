package com.upxuu.xucms.feature.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.upxuu.xucms.LocalAppContainer
import com.upxuu.xucms.data.SettingsStore
import kotlinx.coroutines.launch

/**
 * Sign-in: server address plus the admin key. Deliberately two fields and one
 * button — there is nothing else to configure before writing.
 */
@Composable
fun LoginScreen(onSignedIn: () -> Unit) {
  val container = LocalAppContainer.current
  val settings = container.settings
  val scope = rememberCoroutineScope()

  var baseUrl by remember { mutableStateOf(settings.baseUrl) }
  var key by remember { mutableStateOf("") }
  var reveal by remember { mutableStateOf(false) }
  var busy by remember { mutableStateOf(false) }
  var error by remember { mutableStateOf<String?>(null) }

  fun submit() {
    val url = normalizeUrl(baseUrl)
    if (url == null) {
      error = "请填写完整的服务地址，例如 https://edit.example.com"
      return
    }
    if (key.isBlank()) {
      error = "请填写管理密钥"
      return
    }
    scope.launch {
      busy = true
      error = null
      val previousUrl = settings.baseUrl
      val previousToken = settings.token
      settings.baseUrl = url
      settings.token = key
      val result = container.api.verifyCredentials()
      busy = false
      if (result.isSuccess) {
        baseUrl = url
        onSignedIn()
      } else {
        settings.baseUrl = previousUrl
        settings.token = previousToken
        error = result.exceptionOrNull()?.message ?: "无法连接，请检查地址与密钥"
      }
    }
  }

  Scaffold { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .imePadding()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 28.dp),
      verticalArrangement = Arrangement.Center,
    ) {
      Spacer(Modifier.height(48.dp))
      Text(
        text = "XUCMS",
        style = MaterialTheme.typography.displaySmall,
        color = MaterialTheme.colorScheme.primary,
      )
      Spacer(Modifier.height(6.dp))
      Text(
        text = "随手记录，云端同步",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Spacer(Modifier.height(36.dp))

      OutlinedTextField(
        value = baseUrl,
        onValueChange = { baseUrl = it; error = null },
        label = { Text("服务地址") },
        placeholder = { Text(SettingsStore.DEFAULT_BASE_URL) },
        leadingIcon = { Icon(Icons.Outlined.Cloud, contentDescription = null) },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth(),
      )

      Spacer(Modifier.height(12.dp))

      OutlinedTextField(
        value = key,
        onValueChange = { key = it; error = null },
        label = { Text("管理密钥") },
        leadingIcon = { Icon(Icons.Outlined.Key, contentDescription = null) },
        trailingIcon = {
          IconButton(onClick = { reveal = !reveal }) {
            Icon(
              imageVector = if (reveal) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
              contentDescription = if (reveal) "隐藏密钥" else "显示密钥",
            )
          }
        },
        visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Go),
        keyboardActions = KeyboardActions(onGo = { submit() }),
        modifier = Modifier.fillMaxWidth(),
      )

      AnimatedVisibility(visible = error != null) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(17.dp),
          )
          Spacer(Modifier.width(7.dp))
          Text(
            text = error.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
          )
        }
      }

      Spacer(Modifier.height(24.dp))

      Button(
        onClick = { submit() },
        enabled = !busy,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        modifier = Modifier.fillMaxWidth().height(50.dp),
      ) {
        if (busy) {
          CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.onPrimary,
          )
        } else {
          Text("登录", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
      }

      Spacer(Modifier.height(20.dp))
      Text(
        text = "密钥只保存在本机私有存储，不会上传到第三方。",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outline,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
      )
      Spacer(Modifier.height(48.dp))
    }
  }
}

/** Accepts `example.com` and upgrades it to https; rejects anything unusable. */
private fun normalizeUrl(input: String): String? {
  val trimmed = input.trim().trimEnd('/')
  if (trimmed.isBlank()) return null
  val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
    trimmed
  } else {
    "https://$trimmed"
  }
  val host = withScheme.removePrefix("https://").removePrefix("http://")
  if (host.isBlank() || !host.contains('.')) return null
  return withScheme
}
