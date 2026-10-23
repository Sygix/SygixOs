/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import fr.sygix.sygixos.data.LauncherPrefs
import fr.sygix.sygixos.domain.*
import fr.sygix.sygixos.model.UpNextContentType
import fr.sygix.sygixos.testDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.*
import org.junit.Test

class TrackingIdentityMigrationTest {
    private val identity = TrackingIdentity("fixture", TrackingType.SHOW, "42")
    private val legacy = UpNextContentKey(UpNextContentType.EPISODE, "titre localise")
    private val link = UpNextIdentityLink(legacy, legacy.copy(tracking = identity))
    private val remote = TrackingProjection.card(TrackingContent(TrackingIdentity("fixture", TrackingType.EPISODE, "5"), identity, "Original fixture title", season = 2, episode = 5))

    @Test fun enrichedHideSurvivesTheLocalizedLocalProgramDisappearing() = runTest {
        val memory = InMemoryUpNextMemory()
        memory.addRemoved(RemovedUpNextContent(legacy, "Titre localisé", listOf("fixture.player"), null, 123))
        memory.enrich(listOf(link))
        val removed = memory.removed.first().single()
        assertEquals(identity, removed.key.tracking)
        assertEquals(123L, removed.removedAt)
        assertTrue(UpNextFeed.build(listOf(remote), removed = listOf(removed.key)).isEmpty())
    }

    @Test fun persistedOpeningAndHideRetainIdentityAcrossPreferenceRecreation() = runTest {
        val store = testDataStore(backgroundScope)
        val before = LauncherPrefs(store)
        before.recordOpened(legacy, 456)
        before.addRemoved(RemovedUpNextContent(legacy, "Titre localisé", emptyList(), null, 123))
        before.enrich(listOf(link))
        val after = LauncherPrefs(store)
        val opened = after.opened.first()
        assertEquals(identity, opened.keys.single().tracking)
        assertEquals(456L, opened.values.single())
        assertTrue(opened.keys.single().matches(remote))
        assertTrue(after.removed.first().single().key.matches(remote))
        after.restoreRemoved(after.removed.first().single().key)
        assertTrue(after.removed.first().isEmpty())
    }

    @Test fun ambiguousLinksAndMissingMappingsNeverInventAnIdentityOrPurgeMemory() = runTest {
        val memory = InMemoryUpNextMemory()
        memory.recordOpened(legacy, 456)
        memory.enrich(listOf(link, link.copy(enriched = link.enriched.copy(tracking = identity.copy(id = "99")))) )
        assertEquals(mapOf(legacy to 456L), memory.opened.first())
        memory.enrich(emptyList())
        assertEquals(mapOf(legacy to 456L), memory.opened.first())
    }

    @Test fun migrationUpdatesRemoteOnlyOrderingWithoutWaitingForAnotherProviderRead() = runTest {
        val memory = InMemoryUpNextMemory()
        memory.recordOpened(legacy, 456)
        val other = requireNotNull(remote.tracking).copy(identity = TrackingIdentity("fixture", TrackingType.EPISODE, "1"), parent = identity.copy(id = "99"), title = "Other fixture")
        val source = object : UpNextSource {
            override suspend fun load() = Result.failure<List<fr.sygix.sygixos.model.UpNextItem>>(SecurityException())
            override fun changes() = emptyFlow<Unit>()
        }
        val controller = UpNextController(source, MutableStateFlow(true), MutableStateFlow(fr.sygix.sygixos.model.UpNextPosition.BEFORE_APPS),
            MutableStateFlow(emptySet()), backgroundScope, memory,
            tracking = MutableStateFlow(TrackingState(account = TrackingAccount("fixture", "Fixture"), contents = listOf(requireNotNull(remote.tracking), other))))
        runCurrent()
        assertEquals(TrackingProjection.card(other).key, (controller.state.value?.content as UpNextContent.Items).items.first().key)
        memory.enrich(listOf(link)); runCurrent()
        assertEquals(remote.key, (controller.state.value?.content as UpNextContent.Items).items.first().key)
    }

    @Test fun anIncompleteAccountAndAnEmptyLocalReadNeverPurgeEnrichedOpenings() = runTest {
        val memory = InMemoryUpNextMemory()
        memory.recordOpened(link.enriched, 456)
        val source = object : UpNextSource {
            override suspend fun load() = Result.success(emptyList<fr.sygix.sygixos.model.UpNextItem>())
            override fun changes() = emptyFlow<Unit>()
        }
        val controller = UpNextController(source, MutableStateFlow(true), MutableStateFlow(fr.sygix.sygixos.model.UpNextPosition.BEFORE_APPS),
            MutableStateFlow(emptySet()), backgroundScope, memory,
            tracking = MutableStateFlow(TrackingState(account = TrackingAccount("fixture", "Fixture"), failure = TrackingFailure.INCOMPLETE)))
        controller.refresh(); runCurrent()
        assertEquals(mapOf(link.enriched to 456L), memory.opened.first())
    }
}
