/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import fr.sygix.sygixos.domain.*
import fr.sygix.sygixos.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class TrackingHeroExclusionsTest {
    private val next = TrackingContent(TrackingIdentity("fixture", TrackingType.EPISODE, "3"), TrackingIdentity("fixture", TrackingType.SHOW, "1"), "Fixture", season = 2, episode = 3)
    private val resumed = next.copy(identity = next.identity.copy(id = "5"), episode = 5)
    private val native = HeroItem("watchnext-7", "Fixture", sourcePackage = "fixture.player")
    private val local = UpNextItem(7, UpNextSourceEntry("fixture.player"), UpNextContentType.EPISODE, "Fixture", season = "2", episode = "5", watchNextType = ProgramKind.CONTINUE, tracking = resumed)
    private val programs = object : HeroContentProvider { override suspend fun load() = listOf(native) }
    private val enrichment = object : TrackingLocalContents {
        override val contents = MutableStateFlow(mapOf(local.key to resumed))
        override fun items() = listOf(local)
    }

    @Test fun localHideCannotBeBypassedByAnEnrichedResume() = runTest {
        val memory = InMemoryUpNextMemory()
        val key = UpNextContentKey.of(TrackingProjection.card(next))
        memory.addRemoved(RemovedUpNextContent(key, "Fixture", emptyList(), null, 1000))
        val state = MutableStateFlow(TrackingState(account = TrackingAccount("fixture", "Fixture"), contents = listOf(next)))
        val items = TrackingHeroSource(programs, state, memory, enrichment).load()
        assertTrue(items.none { it.tracking != null })
        assertTrue(items.any { it.id == native.id })
    }

    @Test fun pendingRemoteRemovalExcludesTheEnrichedTrackingCandidateButKeepsNativeProgram() = runTest {
        val state = MutableStateFlow(TrackingState(account = TrackingAccount("fixture", "Fixture"), pendingRemovals = setOf(next.parent)))
        val items = TrackingHeroSource(programs, state, InMemoryUpNextMemory(), enrichment).load()
        assertTrue(items.none { it.tracking != null })
        assertTrue(items.any { it.id == native.id })
    }

    @Test fun canonicalHeroDoesNotRemoveOrAddAnyNativeTopShelfPublication() = runTest {
        val state = MutableStateFlow(TrackingState(account = TrackingAccount("fixture", "Fixture"), contents = listOf(next)))
        val source = TrackingHeroSource(programs, state, InMemoryUpNextMemory(), enrichment)
        val fallback = object : HeroContentProvider { override suspend fun load() = emptyList<HeroItem>() }
        val feed = fr.sygix.sygixos.data.HeroRepository(source, fallback).load()
        assertEquals(1, feed.items.size)
        assertEquals(resumed.identity, feed.items.single().tracking?.identity)
        assertEquals(listOf(native), feed.shelfItems)
        assertTrue(feed.shelfItems.orEmpty().none { it.tracking != null })
    }
}
