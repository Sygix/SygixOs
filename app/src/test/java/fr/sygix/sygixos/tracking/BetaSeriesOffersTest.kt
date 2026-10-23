/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BetaSeriesOffersTest {
    @Test fun seasonOfferDoesNotBecomeAnEpisodeOrRegionConfirmation() {
        val offers = BetaSeriesOffers.show(JSONObject("""{"platforms":{"svods":[{"id":1,"name":"Fixture service","link_url":"https://www.betaseries.com/platform/fixture","available":{"first":1,"last":3}}]}}"""))
        assertEquals(OfferScope.SEASONS, offers.single().scope)
        assertEquals(RegionalAvailability.Unconfirmed, offers.single().availability)
        assertEquals(1, offers.single().firstSeason)
        assertEquals(3, offers.single().lastSeason)
    }

    @Test fun missingOffersAndUntrustedLinksDoNotInventAvailability() {
        assertTrue(BetaSeriesOffers.show(JSONObject()).isEmpty())
        val offers = BetaSeriesOffers.movie(JSONObject("""{"platforms_svod":[{"id":2,"name":"Fixture service","link_url":"https://user:fixture@example.org/content","logo":"http://example.org/logo"}]}"""))
        assertEquals(OfferScope.MOVIE, offers.single().scope)
        assertNull(offers.single().url)
        assertNull(offers.single().logo)
        assertEquals(RegionalAvailability.Unconfirmed, offers.single().availability)
    }
}
