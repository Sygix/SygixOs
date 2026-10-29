/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TraktConfigurationTest {
    @get:Rule val temporary = TemporaryFolder()
    private val client = "a".repeat(64)

    @Test fun discoversRenamedRootWithinTwoScriptsAndNeverRefreshesOnlyBecauseOfAge() = runTest {
        val file = temporary.newFile()
        val reads = mutableListOf<String>()
        val transport = TrackingTransport { request ->
            reads += request.url
            assertEquals(10 * 1024 * 1024, request.maxBytes)
            TrackingResponse(200, when (request.url) {
                "https://app.trakt.tv/" -> "import('/_app/immutable/entry/app.fixture-new.js')"
                "https://app.trakt.tv/_app/immutable/entry/app.fixture-new.js" -> "const files=['../nodes/0.fixture-root.js'];"
                "https://app.trakt.tv/_app/immutable/nodes/0.fixture-root.js" -> "analytics('?trakt-api-key=$client');"
                else -> error("Unexpected resource")
            })
        }
        fun source() = TraktConfigurationSource(transport, file, { testScheduler.currentTime }, StandardTestDispatcher(testScheduler))
        val configuration = source().get()
        assertEquals(TraktConfiguration(client), configuration)
        assertEquals(3, reads.size)
        testScheduler.advanceTimeBy(100L * 24 * 60 * 60_000)
        assertEquals(configuration, source().get())
        assertEquals(3, reads.size)
        assertFalse(configuration.toString().contains(client))
    }

    @Test fun simultaneousConsumersShareDiscoveryAndAnUnchangedRejectedClientDoesNotLoop() = runTest {
        var reads = 0
        val source = TraktConfigurationSource(TrackingTransport { request ->
            reads++
            kotlinx.coroutines.delay(10)
            TrackingResponse(200, when {
                request.url.endsWith("/") -> "import('/entry/app.fixture.js')"
                request.url.contains("/entry/") -> "['../nodes/0.fixture.js']"
                else -> "?trakt-api-key=$client"
            })
        }, null, { testScheduler.currentTime }, StandardTestDispatcher(testScheduler))
        val configurations = coroutineScope { (1..8).map { async { source.get() } }.map { it.await() } }
        assertEquals(3, reads)
        assertEquals(1, configurations.distinct().size)
        source.invalidate(configurations.first())
        try { source.get(); fail("Rejected client was accepted again") }
        catch (error: TrackingException) { assertEquals(TrackingFailure.CONFIGURATION, error.failure) }
        assertEquals(6, reads)
    }

    @Test fun extractionRejectsAmbiguityAndNeverRunsJavaScript() {
        assertNull(TraktConfigParser.configuration("window.client=executeSecretCode();"))
        assertNull(TraktConfigParser.configuration("?trakt-api-key=$client&other=1; ?trakt-api-key=${"b".repeat(64)}"))
        assertNull(TraktConfigParser.entry("import('/entry/app.first.js'); import('/entry/app.second.js')"))
        assertEquals("../nodes/0.renamed.js", TraktConfigParser.root("['../nodes/0.renamed.js','../nodes/0.renamed.js']"))
    }

    @Test fun unofficialImportAndRedirectAreRejectedBeforeAnotherRequest() = runTest {
        for (redirect in listOf(false, true)) {
            var reads = 0
            val source = TraktConfigurationSource(TrackingTransport {
                reads++
                if (redirect) TrackingResponse(302, "", headers = mapOf("Location" to "https://example.org/app.js"))
                else TrackingResponse(200, "import('https://example.org/entry/app.fixture.js')")
            }, null, { testScheduler.currentTime }, StandardTestDispatcher(testScheduler))
            try { source.get(); fail("Unofficial resource was accepted") }
            catch (error: TrackingException) { assertEquals(TrackingFailure.CONFIGURATION, error.failure) }
            assertEquals(1, reads)
        }
    }

    @Test fun discoveryDeadlineCancelsTheSharedResourceRead() = runTest {
        var completed = false
        val source = TraktConfigurationSource(TrackingTransport {
            kotlinx.coroutines.delay(31_000)
            completed = true
            TrackingResponse(200, "unused")
        }, null, { testScheduler.currentTime }, StandardTestDispatcher(testScheduler))
        try { source.get(); fail("Unbounded discovery completed") }
        catch (error: TrackingException) { assertEquals(TrackingFailure.CONFIGURATION, error.failure) }
        assertEquals(30_000, testScheduler.currentTime)
        assertFalse(completed)
    }

    @Test fun anOldRefusalCannotInvalidateTheNewlyDiscoveredClient() = runTest {
        var configured = client
        var reads = 0
        val source = TraktConfigurationSource(TrackingTransport { request ->
            reads++
            TrackingResponse(200, when {
                request.url.endsWith("/") -> "import('/entry/app.fixture.js')"
                request.url.contains("/entry/") -> "['../nodes/0.fixture.js']"
                else -> "?trakt-api-key=$configured"
            })
        }, null, { testScheduler.currentTime }, StandardTestDispatcher(testScheduler))
        val previous = source.get()
        source.invalidate(previous)
        configured = "b".repeat(64)
        val current = source.get()
        source.invalidate(previous)
        assertEquals(current, source.get())
        assertEquals(6, reads)
    }
}
