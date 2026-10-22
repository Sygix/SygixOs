/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import coil.decode.BitmapFactoryDecoder
import coil.decode.Decoder
import fr.sygix.sygixos.data.ArtworkFixture
import fr.sygix.sygixos.data.ImageBytesReader
import fr.sygix.sygixos.data.ProgramImageFetcher
import fr.sygix.sygixos.data.artworkBytes
import fr.sygix.sygixos.domain.ArtworkAge
import fr.sygix.sygixos.domain.PixelSize
import fr.sygix.sygixos.model.ProgramArtwork
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class ShelfArtworkRefreshTest {
    @get:Rule val compose = createComposeRule()
    @get:Rule val folder = TemporaryFolder()
    private val cleanup = mutableListOf<() -> Unit>()
    private val gate = CompletableDeferred<Unit>()

    @After fun close() { gate.complete(Unit); coil.Coil.reset(); cleanup.reversed().forEach { it() } }

    @Test fun rejectionOfAnUnusedCandidateDoesNotBlacklistTheDisplayedPoster() {
        val reads = AtomicInteger()
        val fixture = ArtworkFixture(folder.newFolder(), ImageBytesReader { uri, _ ->
            if (uri.path == "/unused") artworkBytes(48, 48) else { reads.incrementAndGet(); artworkBytes() }
        }, dispatcher = kotlinx.coroutines.Dispatchers.Main.immediate)
        cleanup += fixture::close
        coil.Coil.setImageLoader(fixture.loader)
        val uri = "content://fixture/displayed"
        val unused = "content://fixture/unused"
        val source = ProgramArtwork("fixture.app", listOf(uri, unused), "fiction")
        compose.setContent { CompositionLocalProvider(LocalProgramArtwork provides fixture.repository) {
            ShelfPanel(listOf(uri), PixelSize(800, 450), Modifier.size(400.dp, 225.dp), mapOf(uri to source))
        } }
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("shelf-poster").fetchSemanticsNodes().isNotEmpty() }
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main)
        cleanup += { scope.cancel() }
        val rejected = scope.async { fixture.repository.resolve(source.withCandidates(listOf(unused)), fr.sygix.sygixos.domain.ArtworkPolicy.Hero) }
        compose.waitUntil(5_000) { compose.waitForIdle(); rejected.isCompleted }
        org.junit.Assert.assertNull(runBlocking { rejected.await() }.first)
        compose.waitForIdle()
        compose.onNodeWithTag("shelf-poster").assertExists()
        org.junit.Assert.assertEquals(1, reads.get())
    }

    @Test fun previousShelfImageStaysVisibleWhileTheNewVersionIsBeingDecoded() {
        val bytes = AtomicReference(artworkBytes())
        val reads = AtomicInteger()
        val fixture = ArtworkFixture(folder.newFolder(), ImageBytesReader { _, _ -> reads.incrementAndGet(); bytes.get() }, dispatcher = kotlinx.coroutines.Dispatchers.Main.immediate)
        val gated = AtomicBoolean()
        val entered = CompletableDeferred<Unit>()
        val decoded = AtomicInteger()
        val error = AtomicReference<Throwable?>()
        val loader = fixture.loader.newBuilder().eventListener(object : coil.EventListener {
            override fun onError(request: coil.request.ImageRequest, result: coil.request.ErrorResult) { error.set(result.throwable) }
        }).components {
            add(fr.sygix.sygixos.data.ProgramArtworkInterceptor(fixture.repository))
            add(ProgramImageFetcher.Factory(fixture.disk, fixture.repository))
            add(Decoder.Factory { result, options, imageLoader ->
                val delegate = BitmapFactoryDecoder.Factory().create(result, options, imageLoader)
                Decoder {
                    if (options.parameters.value<Boolean>(ProgramImageFetcher.INDEXED) == true && gated.get()) {
                        entered.complete(Unit)
                        gate.await()
                    }
                    requireNotNull(delegate).decode().also { decoded.incrementAndGet() }
                }
            })
        }.build()
        fixture.repository.imageLoader = { loader }
        cleanup += fixture::close
        cleanup += loader::shutdown
        coil.Coil.setImageLoader(loader)
        val uri = "content://fixture/shelf"
        val source = ProgramArtwork("fixture.app", listOf(uri), "fiction")
        compose.setContent { CompositionLocalProvider(LocalProgramArtwork provides fixture.repository) {
            ShelfPanel(listOf(uri), PixelSize(800, 450), Modifier.size(400.dp, 225.dp), mapOf(uri to source))
        } }
        try { compose.waitUntil(5_000) { compose.onAllNodesWithTag("shelf-poster").fetchSemanticsNodes().isNotEmpty() } }
        catch (failure: Throwable) { throw AssertionError("Shelf fixture failed", error.get() ?: failure) }
        gated.set(true)
        bytes.set(artworkBytes(1280, 720))
        fixture.now.addAndGet(ArtworkAge.FRESH_MS)
        val expiry = kotlinx.coroutines.MainScope().launch { fixture.repository.checkExpiry() }
        cleanup += { expiry.cancel() }
        try { compose.waitUntil(5_000) { compose.waitForIdle(); entered.isCompleted } }
        catch (failure: Throwable) {
            val images = runBlocking { fixture.db.artwork().images().map { listOf(it.state, it.width, it.fetchedAt, it.retryAt) } }
            throw AssertionError("reads=${reads.get()} decoded=${decoded.get()} event=${fixture.repository.signal(source.identity).value} images=$images", error.get() ?: failure)
        }
        compose.onNodeWithTag("shelf-poster").assertExists()
        gate.complete(Unit)
        compose.waitUntil(5_000) { compose.waitForIdle(); decoded.get() >= 2 }
        compose.onNodeWithTag("shelf-poster").assertExists()
    }
}
