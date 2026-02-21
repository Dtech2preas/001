package com.dtech.music.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

data class SearchResult(
    val videoId: String,
    val title: String,
    val thumbnail: String
)

object InnerTubeSearchClient {
    private const val SEARCH_URL = "https://youtubei.googleapis.com/youtubei/v1/search"
    private val MEDIA_TYPE_JSON = "application/json; charset=utf-8".toMediaType()

    suspend fun search(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        val jsonBody = JSONObject().apply {
            put("context", JSONObject().apply {
                put("client", JSONObject().apply {
                    put("clientName", "ANDROID")
                    put("clientVersion", "19.09.37")
                    put("androidSdkVersion", 34)
                })
            })
            put("query", query)
        }

        val request = Request.Builder()
            .url(SEARCH_URL)
            .post(jsonBody.toString().toRequestBody(MEDIA_TYPE_JSON))
            .build()

        val results = mutableListOf<SearchResult>()
        try {
            val response = OkHttpSingleton.client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()

            val responseBody = response.body?.string() ?: return@withContext emptyList()

            val root = JSONObject(responseBody)
            val contentsArray = root.optJSONObject("contents")
                ?.optJSONObject("twoColumnSearchResultsRenderer")
                ?.optJSONObject("primaryContents")
                ?.optJSONObject("sectionListRenderer")
                ?.optJSONArray("contents")

            if (contentsArray != null && contentsArray.length() > 0) {
                val itemSection = contentsArray.optJSONObject(0)
                    ?.optJSONObject("itemSectionRenderer")
                val items = itemSection?.optJSONArray("contents")

                if (items != null) {
                    for (i in 0 until items.length()) {
                        val item = items.optJSONObject(i)
                        val videoRenderer = item.optJSONObject("videoRenderer")
                        if (videoRenderer != null) {
                            val videoId = videoRenderer.optString("videoId")
                            val titleObj = videoRenderer.optJSONObject("title")
                            val title = titleObj?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                                        ?: titleObj?.optString("simpleText")

                            val thumbnails = videoRenderer.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                            var thumbnail = ""
                            if (thumbnails != null && thumbnails.length() > 0) {
                                // Prefer medium resolution, usually index 1 if available
                                val index = if (thumbnails.length() > 1) 1 else 0
                                thumbnail = thumbnails.optJSONObject(index)?.optString("url") ?: ""
                            }

                            if (videoId.isNotEmpty() && !title.isNullOrEmpty()) {
                                results.add(SearchResult(videoId, title, thumbnail))
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        results
    }
}
