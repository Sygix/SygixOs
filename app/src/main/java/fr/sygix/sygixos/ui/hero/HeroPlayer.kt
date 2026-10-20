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
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

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

internal val SystemHeroVideo = HeroVideoFactory { context, onEnded, onError ->
    (context.applicationContext as? fr.sygix.sygixos.SygixOsApp)?.createHeroVideo(onEnded, onError) ?: HeroPlayer(context, onEnded, onError)
}

internal class HeroPlayer(
    private val context: Context,
    private val onEnded: () -> Unit,
    private val onError: (itemId: String) -> Unit,
    private val dataSource: suspend () -> DataSource.Factory = {
        (context.applicationContext as? fr.sygix.sygixos.SygixOsApp)?.videoCache?.dataSource() ?: DefaultDataSource.Factory(context)
    },
) : HeroVideo {
    override var firstFrameRendered by mutableStateOf(false)
        private set
    override var renderedItem by mutableStateOf<String?>(null)
        private set
    override var readyItem by mutableStateOf<String?>(null)
        private set
    override var videoAspect by mutableFloatStateOf(16f / 9f)
        private set

    override var loadedItem by mutableStateOf<String?>(null)
        private set

    private var exo: ExoPlayer? = null
    private var view: TextureView? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var preparing: kotlinx.coroutines.Job? = null
    private data class Request(val id: String, val url: String, val play: Boolean, val loop: Boolean)
    private var requested: Request? = null

    private val listener = object : Player.Listener {
        override fun onRenderedFirstFrame() {
            firstFrameRendered = true
            renderedItem = loadedItem
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            if (videoSize.height <= 0 || videoSize.width <= 0) return
            if (videoSize.width < VisualQuality.MIN_WIDTH_PX) {
                loadedItem?.let(onError)
                return
            }
            videoAspect = videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) readyItem = loadedItem
            if (playbackState == Player.STATE_ENDED) onEnded()
        }

        override fun onPlayerError(error: PlaybackException) {
            val uri = requested?.url?.let(android.net.Uri::parse)
            android.util.Log.w("HeroVideo", "scheme=${uri?.scheme} authority=${uri?.let { fr.sygix.sygixos.data.diagnosticAuthority(it) }} cause=${error.errorCodeName}")
            loadedItem?.let(onError)
        }
    }

    override fun show(id: String, url: String, play: Boolean, loop: Boolean) {
        requested = Request(id, url, play, loop)
        if (exo == null) {
            if (preparing?.isActive != true) preparing = scope.launch {
                val factory = dataSource()
                exo = create(factory)
                requested?.let { show(it.id, it.url, it.play, it.loop) }
            }
            return
        }
        val player = checkNotNull(exo)
        player.repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        if (id != loadedItem) {
            loadedItem = id
            if (id != renderedItem) {
                firstFrameRendered = false
                renderedItem = null
            }
            readyItem = null
            player.setMediaItem(MediaItem.fromUri(url))
            player.prepare()
        }
        player.playWhenReady = play
    }

    override fun park() {
        requested = requested?.copy(play = false)
        exo?.playWhenReady = false
    }

    override fun clear() {
        requested = null
        loadedItem = null
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
        preparing?.cancel()
        requested = null
        loadedItem = null
        exo?.release()
        exo = null
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun create(factory: DataSource.Factory): ExoPlayer = ExoPlayer.Builder(context).setMediaSourceFactory(DefaultMediaSourceFactory(factory)).build().apply {
        volume = 0f
        addListener(listener)
        view?.let(::setVideoTextureView)
    }
}
