package com.example.data

import kotlinx.serialization.Serializable

@Serializable
data class PostItem(val name: String, val sha: String, val title: String? = null, val date: String? = null, val type: String? = null)

@Serializable
data class PostDetail(val content: String, val sha: String)

@Serializable
data class PostPutBody(val content: String, val sha: String? = null)

@Serializable
data class PostPutResponse(val content: ShaHolder)

@Serializable
data class ShaHolder(val sha: String)

@Serializable
data class TalkItem(val name: String, val sha: String, val title: String? = null, val date: String? = null, val type: String? = null)

@Serializable
data class TalkDetail(val content: String, val sha: String)

@Serializable
data class TalkPutBody(val content: String, val sha: String? = null)

@Serializable
data class DeleteBody(val sha: String)

