package com.upxuu.xucms.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.upxuu.xucms.LocalAppContainer
import com.upxuu.xucms.ui.components.FlatCard
import com.upxuu.xucms.ui.components.SectionLabel
import com.upxuu.xucms.ui.components.SettingRow
import com.upxuu.xucms.ui.components.ThinDivider
import com.upxuu.xucms.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  onBack: () -> Unit,
  onAbout: () -> Unit,
  onSignedOut: () -> Unit,
) {
  val container = LocalAppContainer.current
  val settings = container.settings

  var themeDialog by remember { mutableStateOf(false) }
  var autosaveDialog by remember { mutableStateOf(false) }
  var serverDialog by remember { mutableStateOf(false) }
  var categoryDialog by remember { mutableStateOf(false) }
  var signOutDialog by remember { mutableStateOf(false) }

  var themeMode by remember { mutableStateOf(settings.themeMode) }
  var autosave by remember { mutableStateOf(settings.autosaveSeconds) }
  var baseUrl by remember { mutableStateOf(settings.baseUrl) }
  var defaultCategory by remember { mutableStateOf(settings.defaultCategory) }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        title = { Text("设置", style = MaterialTheme.typography.titleMedium) },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
      SectionLabel("外观")
      FlatCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column {
          SettingRow(
            icon = Icons.Outlined.DarkMode,
            title = "主题",
            subtitle = themeLabel(themeMode),
            onClick = { themeDialog = true },
          )
        }
      }

      SectionLabel("编辑")
      FlatCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column {
          SettingRow(
            icon = Icons.Outlined.Timer,
            title = "自动保存草稿",
            subtitle = if (autosave <= 0) "已关闭（仅手动发布）" else "停止输入 $autosave 秒后保存到本机",
            onClick = { autosaveDialog = true },
          )
          ThinDivider(modifier = Modifier.padding(start = 52.dp))
          SettingRow(
            icon = Icons.Outlined.Category,
            title = "默认分类",
            subtitle = defaultCategory.ifBlank { "未设置" },
            onClick = { categoryDialog = true },
          )
        }
      }

      SectionLabel("账号")
      FlatCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column {
          SettingRow(
            icon = Icons.Outlined.Cloud,
            title = "服务地址",
            subtitle = baseUrl,
            onClick = { serverDialog = true },
          )
          ThinDivider(modifier = Modifier.padding(start = 52.dp))
          SettingRow(
            icon = Icons.Outlined.Logout,
            title = "退出登录",
            subtitle = "清除本机保存的管理密钥",
            onClick = { signOutDialog = true },
          )
        }
      }

      SectionLabel("其他")
      FlatCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        SettingRow(
          icon = Icons.Outlined.Info,
          title = "关于 XUCMS",
          subtitle = "版本与开源信息",
          onClick = onAbout,
        )
      }

      Spacer(Modifier.height(40.dp))
    }
  }

  if (themeDialog) {
    ChoiceDialog(
      title = "主题",
      options = ThemeMode.entries.map { themeLabel(it) },
      selectedIndex = ThemeMode.entries.indexOf(themeMode),
      onDismiss = { themeDialog = false },
      onSelect = { index ->
        val mode = ThemeMode.entries[index]
        settings.themeMode = mode
        themeMode = mode
        themeDialog = false
      },
    )
  }

  if (autosaveDialog) {
    val options = listOf(0, 5, 8, 15, 30)
    ChoiceDialog(
      title = "自动保存间隔",
      options = options.map { if (it == 0) "关闭" else "$it 秒" },
      selectedIndex = options.indexOf(autosave).takeIf { it >= 0 } ?: 2,
      onDismiss = { autosaveDialog = false },
      onSelect = { index ->
        settings.autosaveSeconds = options[index]
        autosave = options[index]
        autosaveDialog = false
      },
    )
  }

  if (serverDialog) {
    TextInputDialog(
      title = "服务地址",
      initial = baseUrl,
      label = "https://edit.example.com",
      onDismiss = { serverDialog = false },
      onConfirm = { value ->
        val cleaned = value.trim().trimEnd('/')
        if (cleaned.isNotBlank()) {
          settings.baseUrl = cleaned
          baseUrl = settings.baseUrl
        }
        serverDialog = false
      },
    )
  }

  if (categoryDialog) {
    TextInputDialog(
      title = "默认分类",
      initial = defaultCategory,
      label = "新建文章时自动填入",
      onDismiss = { categoryDialog = false },
      onConfirm = { value ->
        settings.defaultCategory = value
        defaultCategory = settings.defaultCategory
        categoryDialog = false
      },
    )
  }

  if (signOutDialog) {
    AlertDialog(
      onDismissRequest = { signOutDialog = false },
      title = { Text("退出登录？") },
      text = { Text("本机的管理密钥会被清除，草稿仍会保留。") },
      confirmButton = {
        TextButton(onClick = {
          settings.signOut()
          signOutDialog = false
          onSignedOut()
        }) {
          Text("退出", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
        }
      },
      dismissButton = { TextButton(onClick = { signOutDialog = false }) { Text("取消") } },
      shape = MaterialTheme.shapes.large,
    )
  }
}

private fun themeLabel(mode: ThemeMode): String = when (mode) {
  ThemeMode.SYSTEM -> "跟随系统"
  ThemeMode.LIGHT -> "浅色"
  ThemeMode.DARK -> "深色"
}

@Composable
private fun ChoiceDialog(
  title: String,
  options: List<String>,
  selectedIndex: Int,
  onDismiss: () -> Unit,
  onSelect: (Int) -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        options.forEachIndexed { index, label ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .selectable(selected = index == selectedIndex, onClick = { onSelect(index) })
              .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            RadioButton(selected = index == selectedIndex, onClick = { onSelect(index) })
            Spacer(Modifier.height(0.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge)
          }
        }
      }
    },
    confirmButton = { TextButton(onClick = onDismiss) { Text("完成") } },
    shape = MaterialTheme.shapes.large,
  )
}

@Composable
private fun TextInputDialog(
  title: String,
  initial: String,
  label: String,
  onDismiss: () -> Unit,
  onConfirm: (String) -> Unit,
) {
  var value by remember { mutableStateOf(initial) }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title) },
    text = {
      OutlinedTextField(
        value = value,
        onValueChange = { value = it },
        placeholder = { Text(label) },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
      )
    },
    confirmButton = {
      TextButton(onClick = { onConfirm(value) }) { Text("保存", fontWeight = FontWeight.SemiBold) }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    shape = MaterialTheme.shapes.large,
  )
}
