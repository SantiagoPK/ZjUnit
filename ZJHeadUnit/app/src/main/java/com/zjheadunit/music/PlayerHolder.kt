package com.zjheadunit.music

import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken

/** Puente entre la interfaz Compose y el servicio de reproducción. */
class PlayerHolder(ctx: Context) {
    private var controller: MediaController? = null

    var isPlaying by mutableStateOf(false)
    var title by mutableStateOf("Sin música")
    var subtitle by mutableStateOf("Elige una carpeta para empezar")
    var position by mutableLongStateOf(0L)
    var duration by mutableLongStateOf(0L)

    init {
        val token = SessionToken(ctx, ComponentName(ctx, PlaybackService::class.java))
        val future = MediaController.Builder(ctx, token).buildAsync()
        future.addListener({
            val c = future.get()
            controller = c
            c.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
                override fun onMediaMetadataChanged(m: MediaMetadata) = sync(m)
            })
            isPlaying = c.isPlaying
            sync(c.mediaMetadata)
        }, ContextCompat.getMainExecutor(ctx))
    }

    private fun sync(m: MediaMetadata) {
        if (m.title != null) {
            title = m.title.toString()
            subtitle = m.artist?.toString() ?: ""
        }
    }

    fun playList(items: List<MediaItem>, startIndex: Int = 0) {
        controller?.run {
            setMediaItems(items, startIndex, 0L)
            prepare()
            play()
        }
    }

    fun toggle() { controller?.let { if (it.isPlaying) it.pause() else it.play() } }
    fun next() { controller?.seekToNextMediaItem() }
    fun previous() { controller?.seekToPreviousMediaItem() }
    fun seekTo(ms: Long) { controller?.seekTo(ms) }

    fun refreshProgress() {
        controller?.let {
            position = it.currentPosition
            duration = it.duration.coerceAtLeast(0L)
        }
    }
}
