package com.upxuu.xucms.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/** Thrown for non-2xx responses so callers can special-case 401. */
class ApiException(message: String, val code: Int) : Exception(message)

/**
 * Thin OkHttp client for the blog-admin-workers API. The admin key is sent as a
 * bearer token; the base URL is configurable so self-hosted deployments work.
 */
class XucmsApi(private val settings: SettingsStore) {

  private val client = OkHttpClient.Builder()
    .connectTimeout(20, TimeUnit.SECONDS)
    .readTimeout(40, TimeUnit.SECONDS)
    .writeTimeout(60, TimeUnit.SECONDS)
    .build()

  private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
    encodeDefaults = true
  }

  private val mediaType = "application/json".toMediaType()

  private fun baseUrl(): String = settings.baseUrl.trimEnd('/')

  fun imageUrl(path: String): String = "${baseUrl()}/img/${path.trimStart('/')}"

  /** Performs the call and returns the response body, or throws [ApiException]. */
  private fun execute(path: String, method: String, bodyJson: String?): String {
    val builder = Request.Builder()
      .url(baseUrl() + path)
      .header("Authorization", "Bearer ${settings.token}")
    val body = bodyJson?.toRequestBody(mediaType)
    when (method) {
      "GET" -> builder.get()
      "PUT" -> builder.put(body ?: "".toRequestBody(mediaType))
      "POST" -> builder.post(body ?: "".toRequestBody(mediaType))
      "DELETE" -> if (body != null) builder.delete(body) else builder.delete()
      else -> error("unsupported method $method")
    }
    client.newCall(builder.build()).execute().use { response ->
      val text = response.body?.string().orEmpty()
      if (!response.isSuccessful) throw ApiException(errorMessage(response.code, text), response.code)
      return text
    }
  }

  private fun errorMessage(code: Int, body: String): String = when (code) {
    401, 403 -> "鉴权失败，请重新登录"
    404 -> "内容不存在（可能已在云端被删除）"
    409 -> "云端版本已更新，请先刷新"
    in 500..599 -> "服务端错误 $code"
    else -> body.take(200).ifBlank { "请求失败 $code" }
  }

  private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

  suspend fun list(kind: NoteKind): Result<List<NoteSummary>> = io {
    json.decodeFromString(
      kotlinx.serialization.builtins.ListSerializer(NoteSummary.serializer()),
      execute(kind.listPath, "GET", null),
    )
  }

  suspend fun load(kind: NoteKind, filename: String): Result<NoteContent> = io {
    json.decodeFromString(
      NoteContent.serializer(),
      execute("${kind.itemPath}/${encode(filename)}", "GET", null),
    )
  }

  suspend fun save(
    kind: NoteKind,
    filename: String,
    content: String,
    sha: String?,
  ): Result<NotePutResponse> = io {
    val body = json.encodeToString(NotePutBody.serializer(), NotePutBody(content, sha))
    val text = execute("${kind.itemPath}/${encode(filename)}", "PUT", body)
    if (text.isBlank()) NotePutResponse() else json.decodeFromString(NotePutResponse.serializer(), text)
  }

  suspend fun delete(kind: NoteKind, filename: String, sha: String): Result<Unit> = io {
    val body = json.encodeToString(DeleteBody.serializer(), DeleteBody(sha))
    execute("${kind.itemPath}/${encode(filename)}", "DELETE", body)
    Unit
  }

  suspend fun images(): Result<List<GalleryImage>> = io {
    json.decodeFromString(
      kotlinx.serialization.builtins.ListSerializer(GalleryImage.serializer()),
      execute("/api/images", "GET", null),
    )
  }

  suspend fun deleteImage(path: String, sha: String): Result<Unit> = io {
    val body = json.encodeToString(DeleteBody.serializer(), DeleteBody(sha))
    execute("/api/img/${encode(path)}", "DELETE", body)
    Unit
  }

  /** Uploads a base64 payload and returns the public URL of the stored image. */
  suspend fun uploadImage(filename: String, base64: String): Result<String> = io {
    val body = json.encodeToString(UploadImageBody.serializer(), UploadImageBody(filename, base64))
    val text = execute("/api/upload", "POST", body)
    val parsed = runCatching { json.decodeFromString(UploadImageResponse.serializer(), text) }.getOrNull()
    parsed?.url?.takeIf { it.isNotBlank() } ?: imageUrl(filename)
  }

  /** Cheap credential probe used by the login screen. */
  suspend fun verifyCredentials(): Result<Unit> = io {
    execute(NoteKind.TALK.listPath, "GET", null)
    Unit
  }

  private suspend fun <T> io(block: () -> T): Result<T> = withContext(Dispatchers.IO) {
    runCatching(block)
  }
}
