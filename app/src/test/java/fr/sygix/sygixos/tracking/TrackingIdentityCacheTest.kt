/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TrackingIdentityCacheTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun providerIdentitiesSurviveRestartAndRoundTripBetweenProviders() = runTest {
        val file = temporary.newFile()
        val beta = TrackingContent(TrackingIdentity("betaseries", TrackingType.EPISODE, "10"), TrackingIdentity("betaseries", TrackingType.SHOW, "20"), "Beta fixture")
        val trakt = TrackingContent(TrackingIdentity("trakt", TrackingType.EPISODE, "10"), TrackingIdentity("trakt", TrackingType.SHOW, "30"), "Trakt fixture")
        val writer = TrackingIdentityCache(file, Dispatchers.Unconfined)
        writer.put("local", "fingerprint", beta)
        writer.put("local", "fingerprint", trakt)
        val reader = TrackingIdentityCache(file, Dispatchers.Unconfined)
        for (content in listOf(beta, trakt, beta)) {
            assertEquals(LocalTrackingIdentity(content.identity, content.parent), reader.get("local", "fingerprint", content.identity.provider))
        }
        assertNull(reader.get("local", "changed", "trakt"))
    }

    @Test fun unversionedAmbiguousIdentitiesAreDiscardedRatherThanAssignedToBetaSeries() = runTest {
        val file = temporary.newFile().apply { writeText("""{"local":{"fingerprint":"fingerprint","type":"MOVIE","id":"42","parent":"42"}}""") }
        val cache = TrackingIdentityCache(file, Dispatchers.Unconfined)
        assertNull(cache.get("local", "fingerprint", "betaseries"))
        assertNull(cache.get("local", "fingerprint", "trakt"))
        val content = TrackingContent(TrackingIdentity("trakt", TrackingType.MOVIE, "42"), title = "Fixture")
        cache.put("local", "fingerprint", content)
        assertEquals(content.identity, TrackingIdentityCache(file, Dispatchers.Unconfined).get("local", "fingerprint", "trakt")!!.identity)
        assertNull(TrackingIdentityCache(file, Dispatchers.Unconfined).get("local", "fingerprint", "betaseries"))
    }
}
