/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import fr.sygix.sygixos.domain.TsuyuMinimumApi
import fr.sygix.sygixos.model.AdbAccessState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withTimeoutOrNull

class TsuyuAccess(
    private val api: ShizukuApi,
    private val connectMs: Long = 10_000,
    private val commandMs: Long = 12_000,
    io: CoroutineDispatcher = Dispatchers.IO,
) : AdbAccess {
    private class Session {
        @Volatile var remote: PrivilegedOpsRemote? = null
        val handles = mutableListOf<AutoCloseable>()
        var released = false
    }

    private val calls = CoroutineScope(SupervisorJob() + io)
    private val lock = Any()
    private var session: Session? = null
    private val mutableState = MutableStateFlow(AdbAccessState.DISABLED)
    override val state: StateFlow<AdbAccessState> = mutableState

    override fun debugEnabled() = api.pingBinder()
    override fun previouslyAuthorized() = api.granted()

    override suspend fun connect(): Result<Unit> {
        close()
        if (!api.pingBinder()) return refuse(AdbAccessState.DISABLED, "Tsuyu stopped")
        val version = runCatching { api.version() }.getOrNull()
        if (version == null || version < TsuyuMinimumApi) return refuse(AdbAccessState.UNREACHABLE, "Tsuyu outdated")
        if (!api.granted()) return refuse(AdbAccessState.REFUSED, "SygixOs not authorized in Tsuyu")
        mutableState.value = AdbAccessState.WAITING
        val current = Session()
        synchronized(lock) { session = current }
        val ready = CompletableDeferred<PrivilegedOpsRemote>()
        try {
            hold(current, api.onBinderDead { ready.completeExceptionally(IllegalStateException("Tsuyu stopped")); lost(current) })
            hold(current, api.bindOps(
                connected = { remote -> current.remote = remote; ready.complete(remote) },
                lost = { ready.completeExceptionally(IllegalStateException("Privileged service lost")); lost(current) },
            ))
        } catch (failure: Exception) {
            drop(current)
            return refuse(AdbAccessState.UNREACHABLE, "Privileged service unavailable")
        }
        val remote = try {
            withTimeoutOrNull(connectMs) { ready.await() }
        } catch (cancelled: CancellationException) {
            drop(current)
            throw cancelled
        } catch (_: Exception) {
            null
        }
        if (remote == null || synchronized(lock) { session !== current }) {
            drop(current)
            return refuse(AdbAccessState.UNREACHABLE, "Privileged service unavailable")
        }
        mutableState.value = AdbAccessState.READY
        return Result.success(Unit)
    }

    override suspend fun execute(command: String): Result<String> {
        val current = synchronized(lock) { session }
        val remote = current?.remote
        if (current == null || remote == null || mutableState.value != AdbAccessState.READY || !api.pingBinder() || !api.granted()) {
            current?.let(::lost)
            mutableState.value = AdbAccessState.LOST
            return Result.failure(IllegalStateException("Tsuyu lost"))
        }
        if (!PrivilegedCommands.allowed(command)) return Result.failure(IllegalArgumentException("Command refused"))
        val call = calls.async { remote.run(command) }
        val reply = try {
            withTimeoutOrNull(commandMs) { call.await() }
        } catch (cancelled: CancellationException) {
            call.cancel()
            throw cancelled
        } catch (_: Exception) {
            null
        }
        val outcome = PrivilegedReply.decode(reply)
        return when {
            outcome == null || outcome.code == PrivilegedReply.Timeout -> {
                call.cancel()
                lost(current)
                Result.failure(IllegalStateException("Tsuyu lost"))
            }
            outcome.code == 0 -> Result.success(outcome.output.trim())
            else -> Result.failure(ShellOperationFailure(outcome.output))
        }
    }

    override fun close() {
        val current = synchronized(lock) { session.also { session = null } }
        if (current != null) release(current)
        if (mutableState.value == AdbAccessState.READY || mutableState.value == AdbAccessState.WAITING) mutableState.value = AdbAccessState.LOST
    }

    private fun lost(current: Session) {
        if (!drop(current)) return
        mutableState.value = AdbAccessState.LOST
    }

    private fun drop(current: Session): Boolean {
        val owned = synchronized(lock) { (session === current).also { if (it) session = null } }
        release(current)
        return owned
    }

    private fun hold(current: Session, handle: AutoCloseable) {
        val late = synchronized(current) { current.handles += handle; current.released }
        if (late) release(current)
    }

    private fun release(current: Session) {
        current.remote = null
        val handles = synchronized(current) {
            current.released = true
            current.handles.toList().also { current.handles.clear() }
        }
        handles.asReversed().forEach { runCatching { it.close() } }
    }

    private fun refuse(state: AdbAccessState, reason: String): Result<Unit> {
        mutableState.value = state
        return Result.failure(IllegalStateException(reason))
    }
}
