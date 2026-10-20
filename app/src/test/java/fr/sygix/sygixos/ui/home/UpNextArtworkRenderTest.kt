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

    private fun render(read: ImageBytesReader, items: List<UpNextItem> = listOf(item)): ArtworkFixture {
        val fixture = ArtworkFixture(folder.newFolder(), read)
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
        val fixture = render(ImageBytesReader { _, _ -> reads.incrementAndGet(); served.get() })
        compose.waitUntil(5_000) { compose.onAllNodesWithTag(placeholder()).fetchSemanticsNodes().isEmpty() }
        fixture.now.addAndGet(ArtworkAge.FRESH_MS)
        served.set(artworkBytes(color = android.graphics.Color.RED))
        compose.onNodeWithTag(placeholder()).assertDoesNotExist()
        runBlocking { fixture.repository.checkExpiry() }
        compose.waitUntil(5_000) { compose.waitForIdle(); reads.get() >= 2 }
        compose.onNodeWithTag(placeholder()).assertDoesNotExist()
        fixture.repository.onForeground()
        compose.waitForIdle()
        assertEquals(2, reads.get())
    }
}
