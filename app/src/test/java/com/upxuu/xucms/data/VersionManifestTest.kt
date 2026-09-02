package com.upxuu.xucms.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionManifestTest {

  private val json = Json { ignoreUnknownKeys = true; isLenient = true }

  @Test
  fun `manifest parses the shape CI writes`() {
    val source = """
      {
        "versionName": "2.1.0",
        "versionCode": 3,
        "commit": "abc123def456",
        "shortCommit": "abc123d",
        "builtAt": "2026-09-02 14:05:33",
        "tag": "build-20260902-1405",
        "apkUrl": "https://example.com/XUCMS-release.apk",
        "releaseUrl": "https://example.com/releases/tag/build-20260902-1405",
        "changes": [
          { "summary": "fix(editor): keep the caret above the keyboard", "commit": "ed059d6" },
          { "summary": "feat(settings): add update check", "commit": "2b3f129" }
        ]
      }
    """.trimIndent()

    val manifest = json.decodeFromString(VersionManifest.serializer(), source)
    assertEquals("2.1.0", manifest.versionName)
    assertEquals(3, manifest.versionCode)
    assertEquals("abc123d", manifest.shortCommit)
    assertEquals(2, manifest.changes.size)
    assertEquals("ed059d6", manifest.changes.first().commit)
  }

  @Test
  fun `unknown fields and missing fields do not break parsing`() {
    // A newer CI could add keys; an older manifest could lack them. Neither should
    // turn into "cannot check for updates".
    val manifest = json.decodeFromString(
      VersionManifest.serializer(),
      """{ "versionCode": 4, "futureField": true }""",
    )
    assertEquals(4, manifest.versionCode)
    assertEquals("", manifest.versionName)
    assertTrue(manifest.changes.isEmpty())
  }

  @Test
  fun `mirror is the default source and both build raw urls`() {
    assertEquals(UpdateSource.MIRROR, UpdateSource.fromId(null))
    assertEquals(UpdateSource.MIRROR, UpdateSource.fromId("nonsense"))
    assertEquals(UpdateSource.OFFICIAL, UpdateSource.fromId("official"))

    assertEquals(
      "https://raw.gh.1s.fan/ImUpXuu/XUCMS/main/version.json",
      UpdateSource.MIRROR.rawUrl("ImUpXuu/XUCMS", "main", "version.json"),
    )
    assertEquals(
      "https://raw.githubusercontent.com/ImUpXuu/XUCMS/main/version.json",
      UpdateSource.OFFICIAL.rawUrl("ImUpXuu/XUCMS", "main", "version.json"),
    )
  }
}
