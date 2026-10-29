/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import fr.sygix.sygixos.domain.*
import fr.sygix.sygixos.model.UpNextPosition
import fr.sygix.sygixos.model.UpNextItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UpNextTrackingMemoryTest {
    private val contents = (1..5).map { TrackingContent(TrackingIdentity("fixture", TrackingType.MOVIE, it.toString()), title = "Fixture $it") }

    @Test fun bothProvidersKeepReaderBadgesAndLocalHideRestoreWithoutChangingAccountState() = runTest {
        for (provider in TrackingProviderKind.entries) {
            val content = TrackingContent(TrackingIdentity(provider.id, TrackingType.MOVIE, "1"), title = "Fixture",
                readerPackage = "fixture.player", readerName = "Preferred player")
            val tracking = MutableStateFlow(TrackingState(account = TrackingAccount("fixture", "Fixture"), contents = listOf(content)))
            val memory = InMemoryUpNextMemory()
            val reader = fr.sygix.sygixos.model.UpNextSourceEntry("fixture.player", "Preferred player", android.graphics.drawable.ColorDrawable(android.graphics.Color.WHITE))
            val controller = UpNextController(failingSource(SecurityException()), MutableStateFlow(true), MutableStateFlow(UpNextPosition.BEFORE_APPS),
                MutableStateFlow(emptySet()), backgroundScope, memory, tracking = tracking, readers = MutableStateFlow(mapOf(reader.packageName to reader)))
            runCurrent()
            val card = (controller.state.value?.content as UpNextContent.Items).items.single()
            assertSame(reader.icon, card.source.icon)
            assertEquals("Preferred player", card.source.label)
            controller.remove(card); runCurrent()
            assertNull(controller.state.value)
            val removed = memory.removed.value.single()
            assertTrue(TrackingHeroSource.project(HeroRead(emptyList()), tracking.value, listOf(removed.key), emptyList(), emptySet()).items.isEmpty())
            memory.restoreRemoved(removed.key); runCurrent()
            assertEquals(card.key, (controller.state.value?.content as UpNextContent.Items).items.single().key)
            assertEquals(listOf(content), tracking.value.contents)
            assertFalse(content.seen)
        }
    }

    @Test fun remoteCardsUsePersistedOpenedOrderBeforeAnySuccessfulLocalRead() = runTest {
        val memory = InMemoryUpNextMemory()
        val fourth = TrackingProjection.card(contents[3])
        memory.recordOpened(UpNextContentKey.of(fourth), 500)
        val controller = UpNextController(failingSource(SecurityException()), MutableStateFlow(true), MutableStateFlow(UpNextPosition.BEFORE_APPS),
            MutableStateFlow(emptySet()), backgroundScope, memory, tracking = account())
        runCurrent()
        assertEquals(fourth.key, (controller.state.value?.content as UpNextContent.Items).items.first().key)
        controller.refresh(); runCurrent()
        assertEquals(fourth.key, (controller.state.value?.content as UpNextContent.Items).items.first().key)
    }

    @Test fun permissionAndReadErrorsKeepRemoteOpeningAndReturnTarget() = runTest {
        for (error in listOf(SecurityException(), java.io.IOException())) {
            val fourth = TrackingProjection.card(contents[3])
            val controller = UpNextController(failingSource(error), MutableStateFlow(true), MutableStateFlow(UpNextPosition.BEFORE_APPS),
                MutableStateFlow(emptySet()), backgroundScope, InMemoryUpNextMemory(), tracking = account(), clock = { 1000 })
            runCurrent()
            controller.opened(fourth); runCurrent()
            controller.refresh(); runCurrent()
            val state = requireNotNull(controller.state.value)
            assertEquals(fourth.key, (state.content as UpNextContent.Items).items.first().key)
            assertEquals(fourth.key, state.returnFocus?.key)
        }
    }

    private fun account() = MutableStateFlow(TrackingState(account = TrackingAccount("fixture", "Fixture"), contents = contents))
    private fun failingSource(error: Exception) = object : UpNextSource {
        override suspend fun load() = Result.failure<List<UpNextItem>>(error)
        override fun changes() = emptyFlow<Unit>()
    }
}
