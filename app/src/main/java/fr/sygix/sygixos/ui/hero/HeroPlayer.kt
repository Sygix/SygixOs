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

internal interface HeroVideo {
    val firstFrameRendered: Boolean
    val renderedItem: String?
    val readyItem: String?
    val loadedItem: String?
    val videoAspect: Float
    fun show(id: String, url: String, play: Boolean, loop: Boolean)
    fun park()
    fun clear()
    fun attach(textureView: TextureView)
    fun release()
}

internal fun interface HeroVideoFactory {
    fun create(context: Context, onEnded: () -> Unit, onError: (itemId: String) -> Unit): HeroVideo
}

internal val SystemHeroVideo = HeroVideoFactory { context, onEnded, onError -> HeroPlayer(context, onEnded, onError) }

internal class HeroPlayer(
    private val context: Context,
    private val onEnded: () -> Unit,
    private val onError: (itemId: String) -> Unit,
) : HeroVideo {
    override var firstFrameRendered by mutableStateOf(false)
        private set
    override var renderedItem by mutableStateOf<String?>(null)
        private set
    override var readyItem by mutableStateOf<String?>(null)
        private set
    override var videoAspect by mutableFloatStateOf(16f / 9f)
        private set

    private var exo: ExoPlayer? = null
    private var view: TextureView? = null
    private var itemId: String? = null

    override val loadedItem: String?
        get() = itemId

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
            if (playbackState == Player.STATE_READY) readyItem = itemId
            if (playbackState == Player.STATE_ENDED) onEnded()
        }

        override fun onPlayerError(error: PlaybackException) {
            itemId?.let(onError)
        }
    }

    override fun show(id: String, url: String, play: Boolean, loop: Boolean) {
        val player = exo ?: create().also { exo = it }
        player.repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        if (id != itemId) {
            itemId = id
            firstFrameRendered = false
            renderedItem = null
            readyItem = null
            player.setMediaItem(MediaItem.fromUri(url))
            player.prepare()
        }
        player.playWhenReady = play
    }

    override fun park() {
        exo?.playWhenReady = false
    }

    override fun clear() {
        itemId = null
        firstFrameRendered = false
        renderedItem = null
        readyItem = null
        exo?.apply { stop(); clearMediaItems() }
    }

    override fun attach(textureView: TextureView) {
        view = textureView
        exo?.setVideoTextureView(textureView)
    }

    override fun release() {
        exo?.release()
        exo = null
    }

    private fun create(): ExoPlayer = ExoPlayer.Builder(context).build().apply {
        volume = 0f
        addListener(listener)
        view?.let(::setVideoTextureView)
    }
}
