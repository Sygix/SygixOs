/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit

interface TrackingWorkScheduler {
    fun enqueue(incarnation: String)
    fun cancel(incarnation: String)
}

class AndroidTrackingScheduler(context: Context) : TrackingWorkScheduler {
    private val applicationContext = context.applicationContext
    private val work get() = WorkManager.getInstance(applicationContext)
    override fun enqueue(incarnation: String) {
        val request = OneTimeWorkRequestBuilder<TrackingSyncWorker>()
            .setInputData(workDataOf("incarnation" to incarnation))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build()
        work.enqueueUniqueWork("tracking-$incarnation", ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }
    override fun cancel(incarnation: String) { work.cancelUniqueWork("tracking-$incarnation") }
}

class TrackingSyncCoordinator(
    private val repository: TrackingRepository,
    private val provider: TrackingContentProvider,
    private val store: TrackingSessionStore,
    private val allowed: (String) -> Boolean = { true },
    private val clock: () -> Long = System::currentTimeMillis,
    private val departure: (String, String, () -> Unit) -> Unit = repository::depart,
) {
    private val mutex = Mutex()

    suspend fun drain(incarnation: String): Boolean = withContext(Dispatchers.IO) { mutex.withLock {
        withTimeoutOrNull(180_000) {
            var capturedToken: String? = null
            try {
                val token = store.read() ?: return@withTimeoutOrNull true
                capturedToken = token
                val account = repository.restore(token)?.takeIf { it.incarnation == incarnation && !it.accessDenied }
                    ?: return@withTimeoutOrNull true
                if (!allowed(incarnation)) return@withTimeoutOrNull true
                val pending = repository.pending(account.incarnation)
                val eligible = pending.filterNot { it.undoHeld }
                if (eligible.isEmpty() && availableReserves(incarnation).isEmpty()) return@withTimeoutOrNull true
                if (account.retryAt > clock()) return@withTimeoutOrNull false
                for (intent in eligible.take(20)) {
                    if (!valid(incarnation, token)) return@withTimeoutOrNull true
                    if (withTimeoutOrNull(90_000) { synchronize(intent, token); true } != true) throw TrackingException(TrackingFailure.NETWORK)
                }
                for (reserve in availableReserves(incarnation).take(10)) {
                    if (!valid(incarnation, token)) return@withTimeoutOrNull true
                    val content = repository.contents(incarnation).firstOrNull { it.parent.storageKey == reserve.parentKey } ?: continue
                    val revision = repository.account()?.revision ?: return@withTimeoutOrNull true
                    val desired = repository.pending(incarnation).associate { it.key.substringAfterLast(':') to it.desiredState }
                    val values = permitted(incarnation, token) { provider.reserve(token, content, desired) }
                    if (valid(incarnation, token)) repository.fill(incarnation, content.parent, values, true, revision)
                }
                repository.failure(incarnation, null, token)
                repository.pending(incarnation).all { it.undoHeld } && availableReserves(incarnation).isEmpty()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: TrackingSessionChangedException) { true }
            catch (error: Exception) {
                val failure = error.trackingFailure()
                capturedToken?.let { token ->
                    if (error is TrackingRetryException) repository.defer(incarnation, clock() + error.retryAfterMillis.coerceAtLeast(0), token)
                    repository.failure(incarnation, failure, token)
                }
                failure == TrackingFailure.ACCESS
            }
        } ?: false
    } }

    private suspend fun availableReserves(incarnation: String): List<StoredTrackingReserve> {
        val held = repository.pending(incarnation).filter { it.undoHeld }.map { it.key }.toSet()
        val parents = if (held.isEmpty()) emptySet() else repository.contents(incarnation)
            .filter { it.identity.storageKey in held }.map { it.parent.storageKey }.toSet()
        return repository.reserves(incarnation).filter { it.parentKey !in parents }
    }

    private suspend fun synchronize(intent: StoredTrackingIntent, token: String) = permitted(intent.incarnation, token) {
        val incarnation = intent.incarnation
        val content = repository.contents(incarnation).firstOrNull { it.identity.storageKey == intent.key }
            ?: throw TrackingException(TrackingFailure.STORAGE)
        val original = if (content.identity.type == TrackingType.MOVIE) content.copy(movieState = intent.desiredState) else content
        val action = if (content.identity.type == TrackingType.MOVIE) TrackingAction.RESTORE_MOVIE_STATE
            else if (intent.desiredState == 1) TrackingAction.SEEN else TrackingAction.UNSEEN
        var target = if (intent.possiblySent) provider.readTarget(token, original, action) else null
        if (target == null || !provider.matches(target, action, original)) {
            if (!valid(incarnation, token) || !repository.markSent(intent)) return@permitted
            var failure: Exception? = null
            try { provider.sendMutation(token, original, action) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { failure = error }
            if (!valid(incarnation, token)) return@permitted
            target = try { provider.readTarget(token, original, action) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { throw (failure ?: error) }
        }
        if (!provider.matches(target, action, original)) throw TrackingException(TrackingFailure.INCOMPLETE)
        if (valid(incarnation, token)) repository.confirm(intent, target)
    }

    private suspend fun <T> permitted(incarnation: String, token: String, block: suspend () -> T): T =
        withContext(TrackingRequestAuthorization({ valid(incarnation, token) }, { departure(incarnation, token, it) })) { block() }

    private suspend fun valid(incarnation: String, token: String) = allowed(incarnation) && repository.restore(token)?.let {
        it.incarnation == incarnation && !it.accessDenied
    } == true
}

class TrackingSyncServices private constructor(context: Context) {
    val repository = TrackingRepository(TrackingDatabase.open(context))
    val session = CoordinatedTrackingSessionStore(KeystoreTrackingSession(context))
    val scheduler: TrackingWorkScheduler = AndroidTrackingScheduler(context)
    private val lifecycle = TrackingAccountLifecycle(repository, session, scheduler,
        FileTrackingPurgeMarker(context.noBackupFilesDir.resolve("tracking-invalidated")))
    private val transport = BoundedTrackingTransport(TrackingHttpsTransport())
    internal val api = BetaSeriesApi(transport, BetaSeriesConfigurationSource(transport,
        context.noBackupFilesDir.resolve("tracking-configuration"), System::currentTimeMillis, Dispatchers.Default))
    internal val betaProvider = BetaSeriesProvider(api) { java.time.LocalDate.now() }
    internal val traktApi = TraktApi(transport, TraktConfigurationSource(transport,
        context.noBackupFilesDir.resolve("tracking-configuration-trakt"), System::currentTimeMillis, Dispatchers.Default), session)
    internal val traktProvider = TraktProvider(traktApi)
    val provider = TrackingProviderRegistry(betaProvider, traktProvider)
    val coordinator = TrackingSyncCoordinator(repository, provider, session, allowed = { lifecycle.allowed },
        departure = { incarnation, token, block -> lifecycle.depart { repository.depart(incarnation, token, block) } })

    fun invalidate() = lifecycle.invalidate()
    suspend fun recoverPurge(): Boolean = lifecycle.recoverPurge()

    companion object {
        @Volatile private var instance: TrackingSyncServices? = null
        fun get(context: Context): TrackingSyncServices = instance ?: synchronized(this) {
            instance ?: TrackingSyncServices(context.applicationContext).also { instance = it }
        }
    }
}

class TrackingSyncWorker internal constructor(context: Context, parameters: WorkerParameters,
    private val synchronize: suspend (String) -> Boolean,
) : CoroutineWorker(context, parameters) {
    constructor(context: Context, parameters: WorkerParameters) : this(context, parameters, { incarnation ->
        val services = TrackingSyncServices.get(context.applicationContext)
        services.recoverPurge() || services.coordinator.drain(incarnation)
    })

    override suspend fun doWork(): Result {
        val incarnation = inputData.getString("incarnation") ?: return Result.failure()
        return try {
            if (synchronize(incarnation)) Result.success() else Result.retry()
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { Result.retry() }
    }
}
