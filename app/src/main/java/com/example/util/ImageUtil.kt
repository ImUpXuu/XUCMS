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
    fun getExtensionFromUri(context: Context, uri: Uri): String {
        val contentResolver = context.contentResolver
        val type = contentResolver.getType(uri)
        if (type != null) {
            val extension = android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(type)
            if (!extension.isNullOrEmpty()) {
                return extension
            }
        }
        val path = uri.path
        if (path != null) {
            val dot = path.lastIndexOf('.')
            if (dot != -1) {
                return path.substring(dot + 1)
            }
        }
        return "jpg"
    }

    fun compressAndEncode(context: Context, uri: Uri): String {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.use { it.readBytes() } ?: ByteArray(0)
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun generateImageFilename(context: Context? = null, uri: Uri? = null): String {
        val ext = if (context != null && uri != null) getExtensionFromUri(context, uri) else "webp"
        val date = Date()
        val pathSdf = SimpleDateFormat("yyyy/M/d", Locale.getDefault())
        val nameSdf = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
        val random = String.format("%03d", Random.nextInt(1000))
        return "${pathSdf.format(date)}/${nameSdf.format(date)}_$random.$ext"
    }

    fun mdToEditor(md: String): String {
        val regex = Regex("""!\[([^\]]*)\]\(([^)]+)\)""")
        return regex.replace(md) { matchResult ->
            val alt = matchResult.groupValues[1]
            val url = matchResult.groupValues[2]
            "![$alt] ($url)"
        }
    }

    fun editorToMd(editorText: String): String {
        var cleaned = editorText
            .replace("\\!\\[", "![")
            .replace("\\]\\s*\\(", "] (")
            .replace("\\]\\(", "](")
            .replace("\\]", "]")
            .replace("\\(", "(")
            .replace("\\)", ")")
            .replace("\\_", "_")
            .replace("\\*", "*")
            .replace("\\~", "~")
            .replace("\\`", "`")

        val regex = Regex("""!\s*\[([^\]]*)\]\s*\(([^)]+)\)""")
        cleaned = regex.replace(cleaned) { matchResult ->
            val alt = matchResult.groupValues[1].trim()
            val url = matchResult.groupValues[2].trim()
            "![$alt]($url)"
        }
        return cleaned
    }
}
