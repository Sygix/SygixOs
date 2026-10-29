/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import android.util.AtomicFile
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

interface TrackingPurgeMarker {
    val present: Boolean
    fun mark()
    fun clear()
}

class FileTrackingPurgeMarker(file: File) : TrackingPurgeMarker {
    private val atomic = AtomicFile(file)
    override val present get() = atomic.baseFile.exists()
    override fun mark() {
        val output = atomic.startWrite()
        try { output.write(1); atomic.finishWrite(output) }
        catch (error: Exception) { atomic.failWrite(output); throw error }
    }
    override fun clear() {
        atomic.delete()
        if (present) throw TrackingException(TrackingFailure.STORAGE)
    }
}

class TrackingAccountLifecycle(
    private val repository: TrackingRepository,
    private val session: TrackingSessionStore,
    private val scheduler: TrackingWorkScheduler,
    private val marker: TrackingPurgeMarker,
) {
    @Volatile private var invalidated = marker.present
    val allowed: Boolean get() = !invalidated
    private val startupMutex = Mutex()
    private var restoredReferences = false
    private val departureGate = Any()

    fun invalidate() = synchronized(departureGate) { invalidated = true; marker.mark() }

    fun depart(block: () -> Unit) = synchronized(departureGate) {
        if (invalidated) throw TrackingSessionChangedException()
        block()
    }

    suspend fun recoverPurge(): Boolean = withContext(Dispatchers.IO) { startupMutex.withLock {
        if (invalidated) {
            repository.account()?.let { scheduler.cancel(it.incarnation) }
            repository.purge()
            session.write(null)
            marker.clear()
            invalidated = false
            restoredReferences = true
            true
        } else {
            if (!restoredReferences) {
                repository.account()?.let { repository.restoreUndoReferences(it.incarnation) }
                restoredReferences = true
            }
            false
        }
    } }
}
