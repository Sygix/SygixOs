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

class UpNextTrackingMemoryTest {
    private val contents = (1..5).map { TrackingContent(TrackingIdentity("fixture", TrackingType.MOVIE, it.toString()), title = "Fixture $it") }

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
