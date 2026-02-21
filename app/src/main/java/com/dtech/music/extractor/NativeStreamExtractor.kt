package com.dtech.music.extractor

import android.util.LruCache
import com.dtech.music.network.OkHttpSingleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.regex.Pattern

object NativeStreamExtractor {
    private const val PLAYER_URL = "https://youtubei.googleapis.com/youtubei/v1/player"
    private val MEDIA_TYPE_JSON = "application/json; charset=utf-8".toMediaType()

    // Cache size 100
    private val cache = LruCache<String, String>(100)

    suspend fun getStreamUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        // Check cache
        val cachedUrl = cache.get(videoId)
        if (cachedUrl != null) {
            if (!isExpired(cachedUrl)) {
                return@withContext cachedUrl
            } else {
                cache.remove(videoId)
            }
        }

        val jsonBody = JSONObject()
        val context = JSONObject()
        val client = JSONObject()
        client.put("clientName", "ANDROID")
        client.put("clientVersion", "19.09.37")
        client.put("androidSdkVersion", 34)
        context.put("client", client)
        jsonBody.put("context", context)
        jsonBody.put("videoId", videoId)

        val request = Request.Builder()
            .url(PLAYER_URL)
            .post(jsonBody.toString().toRequestBody(MEDIA_TYPE_JSON))
            .build()

        try {
            val response = OkHttpSingleton.client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val responseBody = response.body?.string() ?: return@withContext null
            val root = JSONObject(responseBody)

            val streamingData = root.optJSONObject("streamingData")
            val adaptiveFormats = streamingData?.optJSONArray("adaptiveFormats")

            var bestUrl: String? = null
            var maxBitrate = -1

            if (adaptiveFormats != null) {
                for (i in 0 until adaptiveFormats.length()) {
                    val format = adaptiveFormats.optJSONObject(i)
                    val mimeType = format.optString("mimeType")
                    val bitrate = format.optInt("bitrate")
                    val url = format.optString("url")

                    if (mimeType.contains("audio/mp4") && url.isNotEmpty()) {
                        if (bitrate > maxBitrate) {
                            maxBitrate = bitrate
                            bestUrl = url
                        }
                    }
                }
            }

            if (bestUrl != null) {
                cache.put(videoId, bestUrl)
                return@withContext bestUrl
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext null
    }

    private fun isExpired(url: String): Boolean {
        try {
            val pattern = Pattern.compile("expire=(\\d+)")
            val matcher = pattern.matcher(url)
            if (matcher.find()) {
                val expireTimeSeconds = matcher.group(1)?.toLong() ?: 0L
                // Check if expired (current time > expire time)
                // Add some buffer (e.g., 5 minutes = 300 seconds) to avoid edge cases
                // If expireTimeSeconds is in the past, it's expired.
                return (System.currentTimeMillis() / 1000) > (expireTimeSeconds - 300)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return true // Assume expired if can't parse
    }
}
