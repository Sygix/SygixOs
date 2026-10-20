/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import fr.sygix.sygixos.model.UpNextContentType
import fr.sygix.sygixos.model.UpNextItem
import fr.sygix.sygixos.model.UpNextSourceEntry
import java.io.File
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.rules.TemporaryFolder
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class UpNextArtworkRenderTest {
    @Before fun resetLoader() { coil.Coil.reset() }
    @get:Rule val compose = createComposeRule()
    @get:Rule val folder = TemporaryFolder()
    private val files = mutableListOf<File>()
    private val cleanup = mutableListOf<() -> Unit>()
    private val item = UpNextItem(1, UpNextSourceEntry("com.source"), UpNextContentType.MOVIE, "Movie")
    private val placeholder = "upnext-card-placeholder-${item.key}"

    @After fun clearFiles() { coil.Coil.reset(); cleanup.reversed().forEach { it() }; files.forEach { it.delete() } }

    private fun loader(service: fr.sygix.sygixos.data.ProgramArtworkService, disk: coil.disk.DiskCache, errors: java.util.concurrent.atomic.AtomicInteger) {
        val context: Context = ApplicationProvider.getApplicationContext()
        val loader = coil.ImageLoader.Builder(context).diskCache(disk).allowHardware(false).components {
            add(coil.intercept.Interceptor { chain -> chain.proceed(chain.request).also { if (it is coil.request.ErrorResult) errors.incrementAndGet() } })
            add(fr.sygix.sygixos.data.ProgramArtworkInterceptor(context, service))
            add(coil.decode.Decoder.Factory { result, _, _ ->
                if (result.source.source().peek().readByteArray().contentEquals(byteArrayOf(1, 2, 3))) coil.decode.Decoder { throw java.io.IOException("Undecodable fixture") } else null
            })
        }.build()
        cleanup += disk::clear
        cleanup += service::close
        cleanup += loader::shutdown
        coil.Coil.setImageLoader(loader)
        compose.setContent {
            CompositionLocalProvider(LocalArtworkChanges provides service.changes, LocalArtworkRetries provides service.retryChanges) {
                UpNextArtwork(item.copy(imageUrl = "https://example.invalid/card.png"), Modifier.size(198.dp, 112.dp))
            }
        }
    }

    private fun imageBytes(): fr.sygix.sygixos.data.ArtworkBytes {
        val bitmap = Bitmap.createBitmap(396, 224, Bitmap.Config.ARGB_8888)
        val output = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        bitmap.recycle()
        return fr.sygix.sygixos.data.ArtworkBytes(output.toByteArray(), fr.sygix.sygixos.domain.ArtworkDimensions(396, 224))
    }

    @Test fun `successful unchanged program confirmation retries an errored card after the common cooldown`() {
        val errors = java.util.concurrent.atomic.AtomicInteger()
        val reads = java.util.concurrent.atomic.AtomicInteger()
        val available = java.util.concurrent.atomic.AtomicBoolean(false)
        val now = java.util.concurrent.atomic.AtomicLong(1_000)
        val bytes = imageBytes()
        val disk = coil.disk.DiskCache.Builder().directory(folder.newFolder()).build()
        val service = fr.sygix.sygixos.data.ProgramArtworkService(fr.sygix.sygixos.data.ArtworkDiskStore(disk),
            fr.sygix.sygixos.data.ArtworkTransport { _, _ -> reads.incrementAndGet(); if (!available.get()) throw java.io.IOException(); bytes }, clock = now::get)
        loader(service, disk, errors)
        try {
            compose.waitUntil(5_000) { compose.waitForIdle(); errors.get() >= 1 }
        } catch (error: Throwable) { throw AssertionError("reads=${reads.get()}, requestErrors=${errors.get()}", error) }
        compose.onNodeWithTag(placeholder).assertExists()
        available.set(true)
        now.set(31_001)
        compose.runOnIdle { service.onProgramsConfirmed() }
        compose.waitUntil(5_000) { compose.onAllNodesWithTag(placeholder).fetchSemanticsNodes().isEmpty() }
        org.junit.Assert.assertEquals(2, reads.get())
        compose.runOnIdle { service.onProgramsConfirmed() }
        compose.waitForIdle()
        org.junit.Assert.assertEquals(2, reads.get())
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test fun `a decoded rollback card disappears at the original seven day deadline`() {
        val scheduler = kotlinx.coroutines.test.TestCoroutineScheduler()
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.test.UnconfinedTestDispatcher(scheduler))
        val original = imageBytes()
        val served = java.util.concurrent.atomic.AtomicReference(original)
        val reads = java.util.concurrent.atomic.AtomicInteger()
        val disk = coil.disk.DiskCache.Builder().directory(folder.newFolder()).build()
        val store = fr.sygix.sygixos.data.ArtworkDiskStore(disk)
        val service = fr.sygix.sygixos.data.ProgramArtworkService(store,
            fr.sygix.sygixos.data.ArtworkTransport { _, _ -> reads.incrementAndGet(); served.get() }, clock = { scheduler.currentTime + 1_000 }, expiryScope = scope)
        val errors = java.util.concurrent.atomic.AtomicInteger()
        loader(service, disk, errors)
        compose.waitUntil(5_000) { compose.onAllNodesWithTag(placeholder).fetchSemanticsNodes().isEmpty() }
        served.set(original.copy(bytes = byteArrayOf(1, 2, 3)))
        val hour = 60 * 60_000L
        compose.runOnIdle {
            scheduler.advanceTimeBy(fr.sygix.sygixos.domain.ArtworkAge.RETAIN_MS - hour)
            scheduler.runCurrent()
        }
        try {
            compose.waitUntil(5_000) { compose.waitForIdle(); scheduler.runCurrent(); service.changes.value >= 3 }
        } catch (error: Throwable) {
            throw AssertionError("revisions=${service.changes.value}, requestErrors=${errors.get()}, reads=${reads.get()}", error)
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithTag(placeholder).fetchSemanticsNodes().isEmpty() }
        compose.runOnIdle { scheduler.advanceTimeBy(hour - 1); scheduler.runCurrent() }
        compose.onNodeWithTag(placeholder).assertDoesNotExist()
        compose.runOnIdle { scheduler.advanceTimeBy(1); scheduler.runCurrent() }
        compose.waitUntil(5_000) { scheduler.runCurrent(); compose.onAllNodesWithTag(placeholder).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag(placeholder).assertExists()
    }

    private fun image(width: Int, height: Int): String {
        val context: Context = ApplicationProvider.getApplicationContext()
        val file = File.createTempFile("artwork", ".png", context.cacheDir).also { files += it }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return file.toURI().toString()
    }

    private fun render(uri: String?) {
        compose.setContent { UpNextArtwork(item.copy(imageUrl = uri), Modifier.size(198.dp, 112.dp)) }
    }

    @Test fun `missing artwork keeps placeholder`() {
        render(null)
        compose.onNodeWithTag(placeholder).assertExists()
    }

    @Test fun `landscape at displayed width replaces placeholder`() {
        render(image(396, 224))
        compose.waitUntil(5_000) { compose.onAllNodesWithTag(placeholder).fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithTag(placeholder).assertDoesNotExist()
    }

    @Test fun `portrait retains sufficient width and replaces placeholder`() {
        render(image(792, 1600))
        compose.waitUntil(5_000) { compose.onAllNodesWithTag(placeholder).fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithTag(placeholder).assertDoesNotExist()
    }

    @Test fun `image narrower than displayed width keeps the placeholder`() {
        val small = item.copy(id = 2, imageUrl = image(395, 224))
        val large = item.copy(id = 3, imageUrl = image(792, 448))
        val showLarge = mutableStateOf(false)
        compose.setContent {
            Column {
                UpNextArtwork(small, Modifier.size(198.dp, 112.dp))
                if (showLarge.value) UpNextArtwork(large, Modifier.size(198.dp, 112.dp))
            }
        }
        compose.waitForIdle()
        compose.runOnIdle { showLarge.value = true }
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("upnext-card-placeholder-${large.key}").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithTag("upnext-card-placeholder-${small.key}").assertExists()
    }
}
