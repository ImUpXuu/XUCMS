package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder

object Api {
    private val client = OkHttpClient.Builder().build()
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false }
    private const val BASE_URL = "https://edit.upxuu.com"
    private val mediaType = "application/json".toMediaType()

    class ApiException(message: String, val code: Int) : Exception(message)

    private suspend inline fun <reified T> get(endpoint: String, token: String): Result<T> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("$BASE_URL$endpoint")
                .header("Authorization", "Bearer $token")
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val bodyStr = resp.body?.string() ?: ""
                    Result.success(json.decodeFromString(bodyStr))
                } else {
                    Result.failure(ApiException("HTTP ${resp.code}", resp.code))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend inline fun <reified Req, reified Resp> put(endpoint: String, token: String, body: Req): Result<Resp> = withContext(Dispatchers.IO) {
        try {
            val reqStr = json.encodeToString(body)
            val req = Request.Builder()
                .url("$BASE_URL$endpoint")
                .header("Authorization", "Bearer $token")
                .put(reqStr.toRequestBody(mediaType))
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val bodyStr = resp.body?.string() ?: ""
                    Result.success(json.decodeFromString(bodyStr))
                } else {
                    Result.failure(ApiException("HTTP ${resp.code}", resp.code))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun delete(endpoint: String, token: String, sha: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val reqStr = json.encodeToString(DeleteBody(sha))
            val req = Request.Builder()
                .url("$BASE_URL$endpoint")
                .header("Authorization", "Bearer $token")
                .delete(reqStr.toRequestBody(mediaType))
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(ApiException("HTTP ${resp.code}", resp.code))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPosts(token: String) = get<List<PostItem>>("/api/posts", token)
    suspend fun getPost(token: String, filename: String) = get<PostDetail>("/api/post/${URLEncoder.encode(filename, "UTF-8")}", token)
    suspend fun putPost(token: String, filename: String, body: PostPutBody) = put<PostPutBody, PostPutResponse>("/api/post/${URLEncoder.encode(filename, "UTF-8")}", token, body)
    suspend fun deletePost(token: String, filename: String, sha: String) = delete("/api/post/${URLEncoder.encode(filename, "UTF-8")}", token, sha)

    suspend fun getTalks(token: String) = get<List<TalkItem>>("/api/talks", token)
    suspend fun getTalk(token: String, filename: String) = get<TalkDetail>("/api/talk/${URLEncoder.encode(filename, "UTF-8")}", token)
    suspend fun putTalk(token: String, filename: String, body: TalkPutBody) = put<TalkPutBody, PostPutResponse>("/api/talk/${URLEncoder.encode(filename, "UTF-8")}", token, body)
    suspend fun deleteTalk(token: String, filename: String, sha: String) = delete("/api/talk/${URLEncoder.encode(filename, "UTF-8")}", token, sha)
}
