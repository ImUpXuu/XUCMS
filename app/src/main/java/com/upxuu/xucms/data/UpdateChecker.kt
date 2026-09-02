package com.upxuu.xucms.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** Outcome of a check, so the UI can distinguish "up to date" from "newer build". */
sealed interface UpdateStatus {
  data object Idle : UpdateStatus
  data object Checking : UpdateStatus
  data class UpToDate(val manifest: VersionManifest) : UpdateStatus
  data class Available(val manifest: VersionManifest) : UpdateStatus
  data class Failed(val message: String) : UpdateStatus
}

/**
 * Reads `version.json` from the repository over GitHub raw and compares it with the
 * running build.
 *
 * The comparison is on `versionCode` alone: names are for humans, and a monotonic
 * integer is the only thing that cannot be ambiguous. If the fetch fails on the
 * configured source, the other one is tried once — a mirror being down should not
 * look like "no update available".
 */
class UpdateChecker(
  private val context: Context,
  private val settings: SettingsStore,
) {

  private val client = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .build()

  private val json = Json { ignoreUnknownKeys = true; isLenient = true }

  val installedVersionCode: Int
    get() = runCatching {
      @Suppress("DEPRECATION")
      context.packageManager.getPackageInfo(context.packageName, 0).versionCode
    }.getOrDefault(0)

  val installedVersionName: String
    get() = runCatching {
      context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull().orEmpty()

  suspend fun check(): UpdateStatus = withContext(Dispatchers.IO) {
    val preferred = settings.updateSource
    val fallback = UpdateSource.entries.first { it != preferred }

    val manifest = fetch(preferred) ?: fetch(fallback)
      ?: return@withContext UpdateStatus.Failed("无法获取版本信息，请检查网络或切换更新源")

    if (manifest.versionCode > installedVersionCode) {
      UpdateStatus.Available(manifest)
    } else {
      UpdateStatus.UpToDate(manifest)
    }
  }

  private fun fetch(source: UpdateSource): VersionManifest? = runCatching {
    val request = Request.Builder()
      .url(source.rawUrl(REPO, BRANCH, MANIFEST_PATH))
      // Raw mirrors cache aggressively; without this a check can report a build
      // that was already superseded.
      .header("Cache-Control", "no-cache")
      .build()
    client.newCall(request).execute().use { response ->
      if (!response.isSuccessful) return null
      val body = response.body?.string().orEmpty()
      if (body.isBlank()) return null
      json.decodeFromString(VersionManifest.serializer(), body)
    }
  }.getOrNull()

  companion object {
    private const val REPO = "ImUpXuu/XUCMS"
    private const val BRANCH = "main"
    private const val MANIFEST_PATH = "version.json"
  }
}
