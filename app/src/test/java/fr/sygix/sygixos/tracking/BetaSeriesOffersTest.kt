/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import org.json.JSONObject
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BetaSeriesOffersTest {
    @Test fun changingCountryRechecksOnlyPlatformsAndNeverReloadsAccount() = kotlinx.coroutines.test.runTest {
        val fixture = TrackingMovieFixture(backgroundScope) { testScheduler.currentTime }
        fixture.runtime.controller.restore(); runCurrent()
        val lists = fixture.lists
        val preferences = fixture.runtime.preferences as TrackingPreferencesFixture
        preferences.country.value = "FR"; runCurrent()
        assertEquals("FR", fixture.runtime.state.value.contents.single().offers.single().confirmedCountry)
        val pending = kotlinx.coroutines.CompletableDeferred<Unit>()
        fixture.platformGate = pending
        preferences.country.value = "US"; runCurrent()
        assertNull(fixture.runtime.state.value.contents.single().offers.single().confirmedCountry)
        pending.complete(Unit); runCurrent()
        assertNull(fixture.runtime.state.value.contents.single().offers.single().confirmedCountry)
        assertEquals(listOf("fr", "us"), fixture.platformCountries)
        assertEquals(lists, fixture.lists)
    }

    @Test fun seasonOfferDoesNotBecomeAnEpisodeOrRegionConfirmation() {
        val offers = BetaSeriesOffers.show(JSONObject("""{"platforms":{"svods":[{"id":1,"name":"Fixture service","link_url":"https://www.betaseries.com/platform/fixture","available":{"first":1,"last":3}}]}}"""))
        assertEquals("1", offers.single().id)
        assertNull(offers.single().confirmedCountry)
        assertEquals("FR", BetaSeriesOffers.confirm(offers, RegionalPlatforms("FR", setOf("1"))).single().confirmedCountry)
        assertNull(BetaSeriesOffers.confirm(offers, RegionalPlatforms("US", setOf("2"))).single().confirmedCountry)
    }

    @Test fun missingOffersAndUntrustedLinksDoNotInventAvailability() {
        assertTrue(BetaSeriesOffers.show(JSONObject()).isEmpty())
        val offers = BetaSeriesOffers.movie(JSONObject("""{"platforms_svod":[{"id":2,"name":"Fixture service","link_url":"https://user:fixture@example.org/content","logo":"http://example.org/logo"}]}"""))
        assertEquals(PlaybackOffer("2", "Fixture service"), offers.single())
        assertNull(BetaSeriesOffers.confirm(offers, null).single().confirmedCountry)
    }
}
