/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class TrackingSyncTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private fun database() = Room.databaseBuilder(context, TrackingDatabase::class.java,
        context.noBackupFilesDir.resolve("${UUID.randomUUID()}.db").absolutePath).build()
    private val movie = TrackingContent(TrackingIdentity("betaseries", TrackingType.MOVIE, "1"), title = "Example")
    private val episode = TrackingContent(TrackingIdentity("betaseries", TrackingType.EPISODE, "2"),
        TrackingIdentity("betaseries", TrackingType.SHOW, "3"), "Example", season = 1, episode = 1, airDate = "2020-01-01")
    private val session = object : TrackingSessionStore {
        override suspend fun read() = "fixture-session"
        override suspend fun write(token: String?) = Unit
    }
    private fun configuration() = TrackingResponse(200, "{apiHost:\"https://api.betaseries.com\",apiKey:\"fixture-key\"}")

    @Test fun lostMutationResponseIsReadBeforeReplayAndMatchingStateIsNotSentTwice() = runTest {
        val db = database()
        val repository = TrackingRepository(db)
        val account = repository.bind("fixture-session", TrackingAccount("A", "Example"))
        repository.record(account.incarnation, movie, TrackingAction.SEEN)
        var writes = 0
        var remote = 0
        var readsFail = true
        val provider = BetaSeriesProvider(trackingApi(TrackingTransport { request ->
            when {
                request.url.endsWith('/') -> configuration()
                request.method == "POST" -> { writes++; remote = 1; throw TrackingException(TrackingFailure.NETWORK) }
                readsFail -> throw TrackingException(TrackingFailure.NETWORK)
                else -> TrackingResponse(200, "{\"movie\":{\"id\":1,\"user\":{\"status\":$remote}}}")
            }
        })) { LocalDate.of(2026, 1, 1) }
        assertFalse(TrackingSyncCoordinator(repository, provider, session).drain(account.incarnation))
        assertTrue(repository.pending(account.incarnation).single().possiblySent)
        readsFail = false
        assertTrue(TrackingSyncCoordinator(repository, provider, session).drain(account.incarnation))
        assertEquals(1, writes)
        assertTrue(repository.pending(account.incarnation).isEmpty())
        db.close()
    }

    @Test fun failedReconciliationDoesNotSendUncertainMutationBlindly() = runTest {
        val db = database()
        val repository = TrackingRepository(db)
        val account = repository.bind("fixture-session", TrackingAccount("A", "Example"))
        val intent = repository.record(account.incarnation, movie, TrackingAction.SEEN)
        repository.markSent(intent)
        var writes = 0
        val provider = BetaSeriesProvider(trackingApi(TrackingTransport { request ->
            if (request.url.endsWith('/')) configuration() else {
                if (request.method == "POST") writes++
                throw TrackingException(TrackingFailure.NETWORK)
            }
        })) { LocalDate.of(2026, 1, 1) }
        assertFalse(TrackingSyncCoordinator(repository, provider, session).drain(account.incarnation))
        assertEquals(0, writes)
        assertEquals(1, repository.pending(account.incarnation).size)
        db.close()
    }

    @Test fun reserveFailureAfterConfirmationOnlyRetriesReserve() = runTest {
        val db = database()
        val repository = TrackingRepository(db)
        val account = repository.bind("fixture-session", TrackingAccount("A", "Example"))
        repository.record(account.incarnation, episode, TrackingAction.SEEN)
        var writes = 0
        var reserveCalls = 0
        val provider = BetaSeriesProvider(trackingApi(TrackingTransport { request -> when {
            request.url.endsWith('/') -> configuration()
            request.method == "POST" -> { writes++; TrackingResponse(200, "{}") }
            "episodes/display" in request.url -> TrackingResponse(200, "{\"episode\":{\"id\":2,\"user\":{\"seen\":true}}}")
            "episodes/list" in request.url -> { reserveCalls++; throw TrackingException(TrackingFailure.NETWORK) }
            else -> error("Unexpected endpoint")
        } })) { LocalDate.of(2026, 1, 1) }
        val coordinator = TrackingSyncCoordinator(repository, provider, session)
        assertFalse(coordinator.drain(account.incarnation))
        assertTrue(repository.pending(account.incarnation).isEmpty())
        assertFalse(coordinator.drain(account.incarnation))
        assertEquals(1, writes)
        assertEquals(2, reserveCalls)
        db.close()
    }

    @Test fun newerIntentDuringSendSurvivesAndConvergesWithoutConcurrentSends() = runTest {
        val db = database()
        val repository = TrackingRepository(db)
        val account = repository.bind("fixture-session", TrackingAccount("A", "Example"))
        repository.record(account.incarnation, movie, TrackingAction.SEEN)
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var writes = 0
        var remote = 0
        val provider = BetaSeriesProvider(trackingApi(TrackingTransport { request -> when {
            request.url.endsWith('/') -> configuration()
            request.method == "POST" -> {
                writes++
                remote = request.fields.getValue("state").toInt()
                if (writes == 1) { entered.complete(Unit); release.await() }
                TrackingResponse(200, "{}")
            }
            else -> TrackingResponse(200, "{\"movie\":{\"id\":1,\"user\":{\"status\":$remote}}}")
        } })) { LocalDate.of(2026, 1, 1) }
        val coordinator = TrackingSyncCoordinator(repository, provider, session)
        val first = async { coordinator.drain(account.incarnation) }
        entered.await()
        repository.record(account.incarnation, movie.copy(seen = true, movieState = 1), TrackingAction.UNSEEN)
        val second = async { coordinator.drain(account.incarnation) }
        release.complete(Unit)
        first.await()
        assertTrue(second.await())
        assertEquals(0, remote)
        assertEquals(2, writes)
        assertTrue(repository.pending(account.incarnation).isEmpty())
        db.close()
    }

    @Test fun deniedAccessPausesQueueWithoutDiscardingIntentOrSendingAgain() = runTest {
        val db = database()
        val repository = TrackingRepository(db)
        val account = repository.bind("fixture-session", TrackingAccount("A", "Example"))
        repository.record(account.incarnation, movie, TrackingAction.SEEN)
        var calls = 0
        val provider = BetaSeriesProvider(trackingApi(TrackingTransport { request ->
            if (request.url.endsWith('/')) configuration() else { calls++; throw TrackingException(TrackingFailure.ACCESS) }
        })) { LocalDate.of(2026, 1, 1) }
        val coordinator = TrackingSyncCoordinator(repository, provider, session)
        assertTrue(coordinator.drain(account.incarnation))
        val before = calls
        assertTrue(coordinator.drain(account.incarnation))
        assertEquals(before, calls)
        assertTrue(repository.account()!!.accessDenied)
        assertEquals(1, repository.pending(account.incarnation).size)
        db.close()
    }

    @Test fun retryAfterSurvivesAnotherTriggerWithoutMakingEarlyRequests() = runTest {
        val db = database()
        val repository = TrackingRepository(db)
        val account = repository.bind("fixture-session", TrackingAccount("A", "Example"))
        val intent = repository.record(account.incarnation, movie, TrackingAction.SEEN)
        repository.markSent(intent)
        var calls = 0
        var now = 1000L
        val provider = BetaSeriesProvider(trackingApi(TrackingTransport { request ->
            if (request.url.endsWith('/')) configuration() else { calls++; throw TrackingRetryException(90_000) }
        })) { LocalDate.of(2026, 1, 1) }
        val coordinator = TrackingSyncCoordinator(repository, provider, session, clock = { now })
        assertFalse(coordinator.drain(account.incarnation))
        assertEquals(91_000L, repository.account()!!.retryAt)
        assertFalse(coordinator.drain(account.incarnation))
        assertEquals(1, calls)
        now = 91_000L
        assertFalse(coordinator.drain(account.incarnation))
        assertEquals(2, calls)
        db.close()
    }

    @Test fun lateAccountAResponseDoesNotRecreatePurgedDataOrTouchAccountB() = runTest {
        val db = database()
        val repository = TrackingRepository(db)
        val account = repository.bind("fixture-session", TrackingAccount("A", "Example"))
        repository.record(account.incarnation, movie, TrackingAction.SEEN)
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var writes = 0
        val provider = BetaSeriesProvider(trackingApi(TrackingTransport { request -> when {
            request.url.endsWith('/') -> configuration()
            request.method == "POST" -> { writes++; entered.complete(Unit); release.await(); TrackingResponse(200, "{}") }
            else -> error("Old account must not read after invalidation")
        } })) { LocalDate.of(2026, 1, 1) }
        var allowed = true
        val coordinator = TrackingSyncCoordinator(repository, provider, session, allowed = { allowed })
        val sending = async { coordinator.drain(account.incarnation) }
        entered.await()
        allowed = false
        repository.purge()
        val next = repository.bind("new-session", TrackingAccount("B", "Example"))
        release.complete(Unit)
        assertTrue(sending.await())
        assertEquals("B", repository.account()!!.accountId)
        assertTrue(repository.pending(next.incarnation).isEmpty())
        assertTrue(repository.contents(account.incarnation).isEmpty())
        assertEquals(1, writes)
        db.close()
    }

    @Test fun workerWithoutActivityDrainsPersistentQueueAndStopsWhenEmpty() = runTest {
        val db = database()
        val repository = TrackingRepository(db)
        val account = repository.bind("fixture-session", TrackingAccount("A", "Example"))
        repository.record(account.incarnation, movie, TrackingAction.SEEN)
        var calls = 0
        val provider = BetaSeriesProvider(trackingApi(TrackingTransport { request -> when {
            request.url.endsWith('/') -> configuration()
            request.method == "POST" -> { calls++; TrackingResponse(200, "{}") }
            else -> { calls++; TrackingResponse(200, "{\"movie\":{\"id\":1,\"user\":{\"status\":1}}}") }
        } })) { LocalDate.of(2026, 1, 1) }
        val coordinator = TrackingSyncCoordinator(repository, provider, session)
        val factory = object : androidx.work.WorkerFactory() {
            override fun createWorker(appContext: android.content.Context, workerClassName: String,
                workerParameters: androidx.work.WorkerParameters): androidx.work.ListenableWorker =
                TrackingSyncWorker(appContext, workerParameters, coordinator::drain)
        }
        val worker = androidx.work.testing.TestListenableWorkerBuilder<TrackingSyncWorker>(context)
            .setWorkerFactory(factory).setInputData(androidx.work.workDataOf("incarnation" to account.incarnation)).build()
        assertEquals(androidx.work.ListenableWorker.Result.success(), worker.doWork())
        val before = calls
        assertEquals(androidx.work.ListenableWorker.Result.success(), worker.doWork())
        assertEquals(before, calls)
        assertTrue(repository.pending(account.incarnation).isEmpty())
        db.close()
    }

    @Test fun configurationDiscoveryCannotDispatchAfterDisconnectOrAccountReplacement() = runTest {
        for (replacement in listOf(false, true)) {
            val db = database()
            val repository = TrackingRepository(db)
            val account = repository.bind("fixture-session", TrackingAccount("A", "Example"))
            repository.record(account.incarnation, movie, TrackingAction.SEEN)
            val entered = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            var allowed = true
            var writes = 0
            val provider = BetaSeriesProvider(trackingApi(TrackingTransport { request ->
                if (request.url.endsWith('/')) { entered.complete(Unit); release.await(); configuration() }
                else { if (request.method == "POST") writes++; TrackingResponse(200, "{}") }
            })) { LocalDate.of(2026, 1, 1) }
            val coordinator = TrackingSyncCoordinator(repository, provider, session, allowed = { allowed })
            val sending = async { coordinator.drain(account.incarnation) }
            entered.await()
            if (replacement) repository.bind("new-session", TrackingAccount("B", "Example"))
            else { allowed = false; repository.purge() }
            release.complete(Unit)
            sending.await()
            assertEquals(0, writes)
            if (replacement) assertEquals("B", repository.account()?.accountId)
            db.close()
        }
    }

    @Test fun sameAccountTokenRenewalDoesNotTurnAnObsoleteReadIntoAnAccessRefusal() = runTest {
        for (reserveOnly in listOf(false, true)) {
            val db = database()
            val repository = TrackingRepository(db)
            val account = repository.bind("fixture-session", TrackingAccount("A", "Example"))
            val intent = repository.record(account.incarnation, if (reserveOnly) episode else movie, TrackingAction.SEEN)
            repository.markSent(intent)
            if (reserveOnly) repository.confirm(intent, episode.copy(seen = true))
            val entered = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            var activeToken = "fixture-session"
            val renewableSession = object : TrackingSessionStore {
                override suspend fun read() = activeToken
                override suspend fun write(token: String?) { activeToken = requireNotNull(token) }
            }
            var oldTokenCalls = 0
            var remote = 0
            val provider = BetaSeriesProvider(trackingApi(TrackingTransport { request ->
                if (request.url.endsWith('/')) { entered.complete(Unit); release.await(); configuration() }
                else {
                    if (request.headers["X-BetaSeries-Token"] == "fixture-session") oldTokenCalls++
                    when {
                        "episodes/list" in request.url -> TrackingResponse(200, "{\"shows\":[]}")
                        "shows/display" in request.url -> TrackingResponse(200, "{\"show\":{\"id\":3,\"title\":\"Example\",\"user\":{\"archived\":false}}}")
                        "shows/seasons" in request.url -> TrackingResponse(200, "{\"seasons\":[]}")
                        request.method == "POST" -> { remote = request.fields.getValue("state").toInt(); TrackingResponse(200, "{}") }
                        else -> TrackingResponse(200, "{\"movie\":{\"id\":1,\"user\":{\"status\":$remote}}}")
                    }
                }
            })) { LocalDate.of(2026, 1, 1) }
            val coordinator = TrackingSyncCoordinator(repository, provider, renewableSession)
            val oldRun = async { coordinator.drain(account.incarnation) }
            entered.await()
            activeToken = "renewed-session"
            repository.bind(activeToken, TrackingAccount("A", "Example"))
            release.complete(Unit)
            oldRun.await()
            assertEquals(0, oldTokenCalls)
            assertFalse(repository.account()!!.accessDenied)
            assertEquals(if (reserveOnly) 0 else 1, repository.pending(account.incarnation).size)
            assertTrue(coordinator.drain(account.incarnation))
            assertTrue(repository.pending(account.incarnation).isEmpty())
            assertTrue(repository.reserves(account.incarnation).isEmpty())
            assertFalse(repository.account()!!.accessDenied)
            db.close()
        }
    }
}
