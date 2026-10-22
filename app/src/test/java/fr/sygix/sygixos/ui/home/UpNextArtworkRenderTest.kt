/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import fr.sygix.sygixos.data.*
import fr.sygix.sygixos.domain.ArtworkAge
import fr.sygix.sygixos.model.UpNextContentType
import fr.sygix.sygixos.model.UpNextItem
import fr.sygix.sygixos.model.UpNextSourceEntry
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class UpNextArtworkRenderTest {
    @get:Rule val compose = createComposeRule()
    @get:Rule val folder = TemporaryFolder()
    private val cleanup = mutableListOf<() -> Unit>()
    private val item = UpNextItem(1, UpNextSourceEntry("fixture.app"), UpNextContentType.MOVIE, "Fiction", imageUrl = "content://fixture/card")
    private fun placeholder(item: UpNextItem = this.item) = "upnext-card-placeholder-${item.key}"

    @After fun close() { coil.Coil.reset(); cleanup.reversed().forEach { it() } }

    private fun render(read: ImageBytesReader, items: List<UpNextItem> = listOf(item), onRendered: (coil.request.SuccessResult) -> Unit = {}): ArtworkFixture {
        val fixture = ArtworkFixture(folder.newFolder(), read, onRendered = onRendered, dispatcher = kotlinx.coroutines.Dispatchers.Main.immediate)
        cleanup += fixture::close
        coil.Coil.setImageLoader(fixture.loader)
        compose.setContent { CompositionLocalProvider(LocalProgramArtwork provides fixture.repository) {
            Column { items.forEach { UpNextArtwork(it, Modifier.size(198.dp, 112.dp)) } }
        } }
        return fixture
    }

    @Test fun missingArtworkKeepsPlaceholder() {
        render(ImageBytesReader { _, _ -> error("No candidate") }, listOf(item.copy(imageUrl = null, posterUrl = null)))
        compose.onNodeWithTag(placeholder()).assertExists()
    }

    @Test fun landscapeAtDisplayedWidthReplacesPlaceholder() {
        render(ImageBytesReader { _, _ -> artworkBytes(396, 224) })
        compose.waitUntil(5_000) { compose.onAllNodesWithTag(placeholder()).fetchSemanticsNodes().isEmpty() }
    }

    @Test fun portraitRetainsSufficientWidthAndReplacesPlaceholder() {
        render(ImageBytesReader { _, _ -> artworkBytes(792, 1600) })
        compose.waitUntil(5_000) { compose.onAllNodesWithTag(placeholder()).fetchSemanticsNodes().isEmpty() }
    }

    @Test fun narrowImageDoesNotAffectOtherCards() {
        val second = item.copy(id = 2, imageUrl = "content://fixture/second", posterUrl = "content://fixture/second")
        render(ImageBytesReader { uri, _ -> if (uri.path == "/card") artworkBytes(395, 224) else artworkBytes() }, listOf(item, second))
        compose.waitUntil(5_000) { compose.onAllNodesWithTag(placeholder(second)).fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithTag(placeholder()).assertExists()
    }

    @Test fun ttlReplacementDoesNotBlankCardOrReloadFreshNeighbor() {
        val reads = AtomicInteger()
        val bytes = artworkBytes()
        val served = java.util.concurrent.atomic.AtomicReference(bytes)
        val rendered = java.util.concurrent.atomic.AtomicReference<String?>()
        val fixture = render(ImageBytesReader { _, _ -> reads.incrementAndGet(); served.get() }, onRendered = { rendered.set(it.memoryCacheKey?.key) })
        compose.waitUntil(5_000) { compose.onAllNodesWithTag(placeholder()).fetchSemanticsNodes().isEmpty() }
        val firstVersion = requireNotNull(rendered.get())
        fixture.now.addAndGet(ArtworkAge.FRESH_MS)
        served.set(artworkBytes(1280, 720))
        compose.onNodeWithTag(placeholder()).assertDoesNotExist()
        val expiry = kotlinx.coroutines.MainScope().launch { fixture.repository.checkExpiry() }
        cleanup += { expiry.cancel() }
        compose.waitUntil(5_000) { compose.waitForIdle(); expiry.isCompleted && reads.get() >= 2 && rendered.get() != firstVersion }
        compose.onNodeWithTag(placeholder()).assertDoesNotExist()
        fixture.repository.onForeground()
        compose.waitForIdle()
        assertEquals(2, reads.get())
    }

    @Test fun jellyfinStyleEpisodeStaysVisibleWhenHeroAndPreviewUseTheSameArtworkWithoutSeriesId() {
        val episode = item.copy(type = UpNextContentType.EPISODE, season = "1", episode = "2", internalProviderId = "episode-id")
        val watchNext = fr.sygix.sygixos.model.HeroItem(
            "watchnext-1", episode.title, sourcePackage = episode.source.packageName,
            imageUrl = episode.imageUrl, season = "1", episode = "2", internalProviderId = "episode-id",
        )
        val preview = watchNext.copy(id = "preview-2", internalProviderId = null, launchUri = "fixture://episode/2")
        val pixels = artworkBytes()
        val fixture = render(ImageBytesReader { _, _ -> pixels }, listOf(episode))
        compose.waitUntil(5_000) { compose.onAllNodesWithTag(placeholder(episode)).fetchSemanticsNodes().isEmpty() }
        val prepared = kotlinx.coroutines.MainScope().async {
            assertNotNull(fixture.repository.resolve(watchNext.artwork, fr.sygix.sygixos.domain.ArtworkPolicy.Hero).first)
            assertNotNull(fixture.repository.resolve(preview.artwork, fr.sygix.sygixos.domain.ArtworkPolicy.Shelf(640)).first)
            fixture.repository.checkExpiry()
        }
        cleanup += { prepared.cancel() }
        compose.waitUntil(5_000) { compose.waitForIdle(); prepared.isCompleted }
        runBlocking { prepared.await() }
        compose.waitForIdle()
        compose.onNodeWithTag(placeholder(episode)).assertDoesNotExist()
        runBlocking {
            assertTrue(fixture.db.artwork().images().all { it.state == "valid" })
            assertEquals(3, fixture.db.artwork().associations(fixture.db.artwork().images().single().id).size)
        }
    }
}
