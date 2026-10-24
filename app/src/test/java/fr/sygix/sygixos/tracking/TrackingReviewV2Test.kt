/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import fr.sygix.sygixos.domain.HeroRead
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TrackingReviewV2Test {
    @Test fun partialSnapshotAndRepeatedRestoreRespectTtlWithRealHttpCounts() = runTest {
        val fixture = TrackingMovieFixture(backgroundScope) { testScheduler.currentTime }
        fixture.incomplete = true
        fixture.runtime.controller.restore(); runCurrent()
        assertEquals(TrackingFailure.INCOMPLETE, fixture.runtime.controller.state.value.failure)
        fun lists() = fixture.requests.count { java.net.URI(it.url).path in setOf("/shows/member", "/movies/member", "/episodes/list") }
        assertEquals(3, lists())
        repeat(3) {
            fixture.runtime.controller.restore()
            fixture.runtime.controller.setForeground(false)
            fixture.runtime.controller.setForeground(true)
            runCurrent()
        }
        assertEquals(3, lists())
        fixture.incomplete = false
        val from = fixture.requests.size
        fixture.runtime.actions.perform(fixture.content, TrackingAction.SEEN); runCurrent()
        val mutation = fixture.requests.drop(from)
        assertEquals(1, mutation.count { it.method == "POST" && "/movies/movie" in it.url })
        assertEquals(1, mutation.count { it.method == "GET" && "/movies/movie" in it.url })
        assertEquals(3, lists())
        advanceTimeBy(900_001)
        fixture.runtime.controller.setForeground(false); fixture.runtime.controller.setForeground(true); runCurrent()
        assertEquals(6, lists())
    }

    @Test fun failedMutationKeepsCardAndHeroBeforeAndAfterDismissingError() = runTest {
        val fixture = TrackingMovieFixture(backgroundScope) { testScheduler.currentTime }
        fixture.runtime.controller.restore(); runCurrent()
        val previous = fixture.content
        fixture.mutationFailure = TrackingFailure.NETWORK
        fixture.runtime.actions.perform(previous, TrackingAction.SEEN); runCurrent()
        assertEquals(TrackingFailure.NETWORK, fixture.runtime.actions.notice.value?.failure)
        assertEquals(previous, fixture.content)
        assertEquals(listOf(previous.identity), fixture.runtime.state.value.contents.map { it.identity })
        fixture.runtime.actions.dismiss(); runCurrent()
        assertEquals(previous, fixture.content)
        val heroes = TrackingHeroSource.project(HeroRead(emptyList()), fixture.runtime.state.value, emptyList(), emptyList(), emptySet())
        assertEquals(listOf(previous.identity), heroes.items.mapNotNull { it.tracking?.identity })
    }

    @Test fun returnPromptReplacesCompletedUndoAndQueuesBehindPendingRemoval() = runTest {
        val fixture = TrackingMovieFixture(backgroundScope) { testScheduler.currentTime }
        fixture.runtime.controller.restore(); runCurrent()
        val first = fixture.content
        val next = first.copy(identity = first.identity.copy(id = "43"), title = "Next fixture")
        fixture.runtime.actions.perform(first, TrackingAction.SEEN); runCurrent()
        assertFalse(fixture.runtime.actions.notice.value!!.returnPrompt)
        fixture.runtime.playback.open(PlaybackRequest(next, PlaybackOrigin.UP_NEXT)); runCurrent()
        fixture.runtime.playback.choose("org.jellyfin.androidtv"); runCurrent()
        fixture.runtime.onBackground(); fixture.runtime.onForeground(); runCurrent()
        assertEquals(next.identity, fixture.runtime.actions.notice.value?.content?.identity)
        assertTrue(fixture.runtime.actions.notice.value?.returnPrompt == true)
        fixture.runtime.actions.dismiss()
        fixture.runtime.actions.perform(next, TrackingAction.REMOVE); runCurrent()
        fixture.runtime.actions.returned(first, first.sessionGeneration!!)
        assertEquals(TrackingAction.REMOVE, fixture.runtime.actions.notice.value?.action)
        fixture.runtime.actions.undo(); runCurrent()
        assertTrue(fixture.runtime.actions.notice.value?.returnPrompt == true)
        assertEquals(first.identity, fixture.runtime.actions.notice.value?.content?.identity)
        assertEquals(0, fixture.removals)
    }

    @Test fun settingsPauseUndoDeadlineUntilVisibleAgain() = runTest {
        val fixture = TrackingMovieFixture(backgroundScope) { testScheduler.currentTime }
        fixture.runtime.controller.restore(); runCurrent()
        fixture.runtime.actions.perform(fixture.content, TrackingAction.REMOVE); runCurrent()
        advanceTimeBy(3000)
        fixture.runtime.actions.setSettingsOpen(true)
        advanceTimeBy(60_000); runCurrent()
        assertEquals(7000L, fixture.runtime.actions.notice.value?.remainingMillis)
        assertEquals(0, fixture.removals)
        fixture.runtime.actions.setSettingsOpen(false)
        advanceTimeBy(6999); runCurrent(); assertEquals(0, fixture.removals)
        advanceTimeBy(1); runCurrent(); assertEquals(1, fixture.removals)
    }

    @Test fun pauseOrStopAtTwelveSecondsConfirmsDepartureAndNoDepartureExpiresAtFifteen() = runTest {
        for (pause in listOf(true, false)) {
            val fixture = TrackingMovieFixture(backgroundScope) { testScheduler.currentTime }
            fixture.runtime.controller.restore(); runCurrent()
            fixture.runtime.playback.open(PlaybackRequest(fixture.content, PlaybackOrigin.UP_NEXT)); runCurrent()
            fixture.runtime.playback.choose("org.jellyfin.androidtv"); runCurrent()
            advanceTimeBy(10_000)
            fixture.runtime.playback.onForeground()
            advanceTimeBy(2000)
            if (pause) fixture.runtime.playback.onDeparture() else fixture.runtime.playback.onBackground()
            fixture.runtime.playback.onForeground(); runCurrent()
            assertTrue(fixture.runtime.actions.notice.value?.returnPrompt == true)
        }
        val fixture = TrackingMovieFixture(backgroundScope) { testScheduler.currentTime }
        fixture.runtime.controller.restore(); runCurrent()
        fixture.runtime.playback.open(PlaybackRequest(fixture.content, PlaybackOrigin.UP_NEXT)); runCurrent()
        fixture.runtime.playback.choose("org.jellyfin.androidtv"); runCurrent()
        advanceTimeBy(15_001); runCurrent()
        fixture.runtime.playback.onDeparture(); fixture.runtime.playback.onForeground(); runCurrent()
        assertNull(fixture.runtime.actions.notice.value)
    }

    @Test fun artworkIsConfirmedOnlyWhenPairsChangeAndNeverWithoutAccount() = runTest {
        val anonymous = TrackingMovieFixture(backgroundScope, savedToken = null) { testScheduler.currentTime }
        anonymous.runtime.controller.restore(); runCurrent()
        anonymous.runtime.onForeground(); runCurrent()
        assertTrue(anonymous.artworkCalls.isEmpty())
        val fixture = TrackingMovieFixture(backgroundScope) { testScheduler.currentTime }
        fixture.runtime.controller.restore(); runCurrent()
        assertEquals(1, fixture.artworkCalls.size)
        assertEquals(2, fixture.artworkCalls.single().size)
        val lists = fixture.lists
        fixture.runtime.controller.refresh(); runCurrent()
        fixture.runtime.preferences.confirmCountry("FR"); runCurrent()
        assertTrue(fixture.lists > lists)
        assertEquals(listOf("fr"), fixture.platformCountries)
        assertEquals(1, fixture.artworkCalls.size)
        fixture.runtime.disconnect(); runCurrent()
        assertEquals(2, fixture.artworkCalls.size)
        assertTrue(fixture.artworkCalls.last().isEmpty())
    }

    @Test fun rejectedCandidateDoesNotReportAUserSessionRevocationFailure() = runTest {
        val controller = TrackingController(object : WatchTrackingProvider {
            override suspend fun account(token: String): TrackingAccount = throw TrackingException(TrackingFailure.NETWORK)
            override suspend fun snapshot(token: String) = TrackingSnapshot(emptyList())
            override suspend fun revoke(token: String): Unit = throw TrackingException(TrackingFailure.NETWORK)
        }, object : TrackingSessionStore {
            override suspend fun read(): String? = null
            override suspend fun write(token: String?) {}
        }, backgroundScope, { testScheduler.currentTime })
        controller.connect("fixture-token"); runCurrent()
        assertFalse(controller.state.value.revocationUnconfirmed)
        assertEquals(TrackingFailure.NETWORK, controller.state.value.failure)
        assertEquals(TrackingFailure.PROTOCOL, org.json.JSONException("Fixture").trackingFailure())
    }
}
