/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import org.junit.Assert.*
import org.junit.Test

class BetaSeriesConfigTest {
    @Test fun discoversRenamedOfficialBundle() {
        assertEquals(listOf("https://www.betaseries.com/assets/settings-new.js"), BetaSeriesConfigParser.scripts("<script src=\"/assets/settings-new.js\"></script>"))
        assertEquals(BetaSeriesConfiguration("https://api.betaseries.com", "fixture-client"), BetaSeriesConfigParser.configuration("window.config={apiHost:\"https://api.betaseries.com\",apiKey:\"fixture-client\"}"))
    }

    @Test fun refusesUntrustedHostsAndMissingFields() {
        assertTrue(BetaSeriesConfigParser.scripts("<script src=\"https://example.org/bundle.js\"></script>").isEmpty())
        assertNull(BetaSeriesConfigParser.configuration("{apiHost:\"https://example.org\",apiKey:\"fixture-client\"}"))
        assertNull(BetaSeriesConfigParser.configuration("{apiHost:\"https://api.betaseries.com\"}"))
    }
}
