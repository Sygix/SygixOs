/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.hero

import android.content.Context
import android.view.TextureView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import fr.sygix.sygixos.domain.VisualQuality

internal class HeroPlayer(
    private val context: Context,
    private val onEnded: () -> Unit,
    private val onError: (itemId: String) -> Unit,
) {
    var firstFrameRendered by mutableStateOf(false)
        private set
    var renderedItem by mutableStateOf<String?>(null)
        private set
    var videoAspect by mutableFloatStateOf(16f / 9f)
        private set

    private var exo: ExoPlayer? = null
    private var view: TextureView? = null
    private var itemId: String? = null

    private val listener = object : Player.Listener {
        override fun onRenderedFirstFrame() {
            firstFrameRendered = true
            renderedItem = itemId
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            if (videoSize.height <= 0 || videoSize.width <= 0) return
            if (videoSize.width < VisualQuality.MIN_WIDTH_PX) {
                itemId?.let(onError)
                return
            }
            videoAspect = videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) onEnded()
        }

        override fun onPlayerError(error: PlaybackException) {
            itemId?.let(onError)
        }
    }

    fun show(id: String, url: String, play: Boolean, loop: Boolean) {
        val player = exo ?: create().also { exo = it }
        player.repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        if (id != itemId) {
            itemId = id
            firstFrameRendered = false
            renderedItem = null
            player.setMediaItem(MediaItem.fromUri(url))
            player.prepare()
        }
        player.playWhenReady = play
    }

    fun clear() {
        itemId = null
        firstFrameRendered = false
        renderedItem = null
        exo?.apply { stop(); clearMediaItems() }
    }

    fun attach(textureView: TextureView) {
        view = textureView
        exo?.setVideoTextureView(textureView)
    }

    fun release() {
        exo?.release()
        exo = null
    }

    private fun create(): ExoPlayer = ExoPlayer.Builder(context).build().apply {
        volume = 0f
        addListener(listener)
        view?.let(::setVideoTextureView)
    }
}
