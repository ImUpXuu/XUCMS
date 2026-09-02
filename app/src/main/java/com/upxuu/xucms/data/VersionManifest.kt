package com.upxuu.xucms.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** One commit that went into a build, as shown in the update dialog. */
@Serializable
data class ChangeEntry(
  val summary: String = "",
  val commit: String = "",
)

/**
 * Contents of `version.json` at the repository root, written by CI on every build.
 *
 * It lives on the default branch rather than as a release asset so it can be read
 * through a GitHub raw mirror — release asset URLs are not raw-mirrorable, and a
 * mirror is what makes the check usable from the mainland.
 */
@Serializable
data class VersionManifest(
  val versionName: String = "",
  val versionCode: Int = 0,
  val commit: String = "",
  @SerialName("shortCommit") val shortCommit: String = "",
  val builtAt: String = "",
  val tag: String = "",
  val apkUrl: String = "",
  val releaseUrl: String = "",
  val changes: List<ChangeEntry> = emptyList(),
)

/**
 * Where to fetch `version.json` from. GitHub raw is often unreachable or very slow
 * from the mainland, so a mirror is the default and the official host is opt-in.
 */
enum class UpdateSource(val id: String, val label: String, val host: String) {
  MIRROR("mirror", "加速源（raw.gh.1s.fan）", "https://raw.gh.1s.fan"),
  OFFICIAL("official", "官方源（raw.githubusercontent.com）", "https://raw.githubusercontent.com");

  /** Raw URL of a path on the default branch. */
  fun rawUrl(repo: String, branch: String, path: String): String =
    "$host/$repo/$branch/$path"

  companion object {
    fun fromId(id: String?): UpdateSource =
      entries.firstOrNull { it.id == id } ?: MIRROR
  }
}
