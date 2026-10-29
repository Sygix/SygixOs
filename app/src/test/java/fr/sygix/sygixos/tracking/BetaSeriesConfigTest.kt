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
    @get:org.junit.Rule val temporary = org.junit.rules.TemporaryFolder()

    @Test fun persistedConfigurationSurvivesRestartAndAgeUntilAnExplicitRefusal() = kotlinx.coroutines.test.runTest {
        val file = temporary.newFile()
        var calls = 0
        val transport = TrackingTransport { ++calls; TrackingResponse(200, """{"apiHost":"https://api.betaseries.com","apiKey":"fixture-client"}""") }
        fun source() = BetaSeriesConfigurationSource(transport, file, { testScheduler.currentTime }, kotlinx.coroutines.test.StandardTestDispatcher(testScheduler))
        val first = source().get()
        val restarted = source()
        assertEquals(first, restarted.get())
        assertEquals(1, calls)
        testScheduler.advanceTimeBy(21 * 24 * 60 * 60_000L)
        assertEquals(first, restarted.get())
        val olderRestart = source()
        assertEquals(first, olderRestart.get())
        assertEquals(1, calls)
        olderRestart.invalidate(first)
        assertFalse(file.exists())
        source().get()
        assertEquals(2, calls)
    }

    @Test fun discoveryOnlyReadsSixOfficialScriptsAndLargeUnrelatedBlocksDoNotChangeConfiguration() = kotlinx.coroutines.test.runTest {
        var scripts = 0
        val transport = TrackingTransport { request ->
            if (request.url.endsWith("/")) TrackingResponse(200, "<script src=\"https://api.betaseries.com/untrusted.js\"></script>" + (1..12).joinToString("") { "<script src=\"/bundle-$it.js\"></script>" })
            else { ++scripts; TrackingResponse(200, "unrelated") }
        }
        val source = BetaSeriesConfigurationSource(transport, null, { testScheduler.currentTime }, kotlinx.coroutines.test.StandardTestDispatcher(testScheduler))
        try { source.get(); fail() } catch (error: TrackingException) { assertEquals(TrackingFailure.CONFIGURATION, error.failure) }
        assertEquals(6, scripts)
        val data = "{}".repeat(500_000) + """JSON.parse('{"apiHost":"https://api.betaseries.com","apiKey":"fixture-client"}');"""
        assertEquals(BetaSeriesConfiguration("https://api.betaseries.com", "fixture-client"), BetaSeriesConfigParser.configuration(data))
    }

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
        val source = BetaSeriesConfigurationSource(TrackingTransport { request ->
            ++calls
            kotlinx.coroutines.delay(100)
            TrackingResponse(200, if (request.url.endsWith("/")) "<script src=\"/reactjs/config-renamed.js\"></script>"
                else """JSON.parse('{"apiHost":"https://api.betaseries.com","apiKey":"fixture-client"}');""")
        }, null, { testScheduler.currentTime }, kotlinx.coroutines.test.StandardTestDispatcher(testScheduler))
        val configs = kotlinx.coroutines.coroutineScope { (1..10).map { async { source.get() } }.map { it.await() } }
        assertEquals(2, calls)
        assertTrue(configs.all { it == BetaSeriesConfiguration("https://api.betaseries.com", "fixture-client") })
    }
}
