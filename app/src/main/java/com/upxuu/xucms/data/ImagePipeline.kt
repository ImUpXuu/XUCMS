package com.upxuu.xucms.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

/**
 * Downscales and re-encodes picked images before upload. Phone photos are far
 * larger than a blog needs, and the API takes base64, so shrinking first keeps
 * uploads fast and well under request size limits.
 */
object ImagePipeline {

  private const val MAX_EDGE = 1920
  private const val QUALITY = 86

  data class Prepared(val filename: String, val base64: String)

  fun prepare(context: Context, uri: Uri): Prepared? {
    val bytes = runCatching {
      context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    }.getOrNull() ?: return null

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    val sample = sampleSize(bounds.outWidth, bounds.outHeight)

    val decoded = BitmapFactory.decodeByteArray(
      bytes, 0, bytes.size,
      BitmapFactory.Options().apply { inSampleSize = sample },
    ) ?: return null

    val scaled = scaleToMaxEdge(decoded)
    val out = ByteArrayOutputStream()
    val format = Bitmap.CompressFormat.JPEG
    scaled.compress(format, QUALITY, out)
    if (scaled !== decoded) scaled.recycle()
    decoded.recycle()

    return Prepared(
      filename = generateFilename("jpg"),
      base64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP),
    )
  }

  private fun sampleSize(width: Int, height: Int): Int {
    var sample = 1
    var w = width
    var h = height
    while (w / 2 >= MAX_EDGE && h / 2 >= MAX_EDGE) {
      w /= 2
      h /= 2
      sample *= 2
    }
    return sample
  }

  private fun scaleToMaxEdge(source: Bitmap): Bitmap {
    val longest = maxOf(source.width, source.height)
    if (longest <= MAX_EDGE) return source
    val ratio = MAX_EDGE.toFloat() / longest
    return Bitmap.createScaledBitmap(
      source,
      (source.width * ratio).toInt().coerceAtLeast(1),
      (source.height * ratio).toInt().coerceAtLeast(1),
      true,
    )
  }

  /** `2026/8/31/20260831203000_417.jpg` — date-partitioned, collision-resistant. */
  fun generateFilename(extension: String): String {
    val now = Date()
    val folder = SimpleDateFormat("yyyy/M/d", Locale.US).format(now)
    val stamp = SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(now)
    val salt = Random.nextInt(1000).toString().padStart(3, '0')
    return "$folder/${stamp}_$salt.$extension"
  }
}
