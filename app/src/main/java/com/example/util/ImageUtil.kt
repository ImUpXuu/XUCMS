package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min
import kotlin.random.Random

object ImageUtil {
    fun compressAndEncode(context: Context, uri: Uri): String {
        val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        val (w, h) = if (bitmap.width > 1920 || bitmap.height > 1920) {
            val ratio = min(1920f / bitmap.width, 1920f / bitmap.height)
            Pair((bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt())
        } else Pair(bitmap.width, bitmap.height)
        val scaled = Bitmap.createScaledBitmap(bitmap, w, h, true)
        val bytes = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.WEBP, 80, bytes)
        return Base64.encodeToString(bytes.toByteArray(), Base64.NO_WRAP)
    }

    fun generateImageFilename(): String {
        val date = Date()
        val pathSdf = SimpleDateFormat("yyyy/M/d", Locale.getDefault())
        val nameSdf = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
        val random = String.format("%03d", Random.nextInt(1000))
        return "${pathSdf.format(date)}/${nameSdf.format(date)}_$random.webp"
    }
}
