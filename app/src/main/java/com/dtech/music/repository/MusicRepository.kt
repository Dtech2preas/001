package com.dtech.music.repository

import com.dtech.music.network.InnerTubeSearchClient
import com.dtech.music.extractor.NativeStreamExtractor

class MusicRepository {
    suspend fun search(query: String) = InnerTubeSearchClient.search(query)
    suspend fun getStreamUrl(videoId: String) = NativeStreamExtractor.getStreamUrl(videoId)
}
