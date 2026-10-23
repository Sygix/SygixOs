/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.coroutines.async

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
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

    @Test fun extractsStaticJsonDataWithoutEvaluatingBundleReferences() {
        val bundle = """var dev=JSON.parse('{"apiHost":"https://dev.example.org","apiKey":"fixture-dev"}');var prod=JSON.parse('{"apiHost":"https://api.betaseries.com","apiKey":"fixture-client","nested":{"enabled":true}}');client.init({apiHost:prod.apiHost,apiKey:prod.apiKey});"""
        assertEquals(BetaSeriesConfiguration("https://api.betaseries.com", "fixture-client"), BetaSeriesConfigParser.configuration(bundle))
        assertNull(BetaSeriesConfigParser.configuration("client.init({apiHost:prod.apiHost,apiKey:prod.apiKey});"))
    }

    @Test fun conflictingConfigurationsAreNotSelectedArbitrarily() {
        assertNull(BetaSeriesConfigParser.configuration("""JSON.parse('{"apiHost":"https://api.betaseries.com","apiKey":"fixture-one"}');JSON.parse('{"apiHost":"https://api.betaseries.com","apiKey":"fixture-two"}');"""))
    }

    @Test fun concurrentDiscoverySharesOneHtmlAndRenamedBundleRequest() = kotlinx.coroutines.test.runTest {
        var calls = 0
        val source = BetaSeriesConfigurationSource { request ->
            ++calls
            kotlinx.coroutines.delay(100)
            TrackingResponse(200, if (request.url.endsWith("/")) "<script src=\"/reactjs/config-renamed.js\"></script>"
                else """JSON.parse('{"apiHost":"https://api.betaseries.com","apiKey":"fixture-client"}');""")
        }
        val configs = kotlinx.coroutines.coroutineScope { (1..10).map { async { source.get() } }.map { it.await() } }
        assertEquals(2, calls)
        assertTrue(configs.all { it == BetaSeriesConfiguration("https://api.betaseries.com", "fixture-client") })
    }
}
