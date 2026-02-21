package com.dtech.music.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.dtech.music.network.OkHttpSingleton

@OptIn(UnstableApi::class)
class PlayerManager(context: Context) {

    private val loadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(10000, 20000, 250, 500)
        .build()

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setLoadControl(loadControl)
        .setMediaSourceFactory(
            DefaultMediaSourceFactory(
                OkHttpDataSource.Factory(OkHttpSingleton.client)
            )
        )
        .build()

    fun play(url: String) {
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.playWhenReady = true
    }

    fun release() {
        player.release()
    }
}
