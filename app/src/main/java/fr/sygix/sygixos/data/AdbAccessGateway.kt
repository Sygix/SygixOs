/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Context
import android.provider.Settings
import dadb.AdbKeyPair
import fr.sygix.sygixos.model.AdbAccessState
import java.io.File
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

interface LocalAdbTransport : AutoCloseable {
    suspend fun reachable(): Boolean
    suspend fun authenticate(): String
    suspend fun command(command: String): String
}

class ShellOperationFailure(val output: String) : Exception("Shell operation rejected")

interface AdbAccess {
    val state: StateFlow<AdbAccessState>
    fun debugEnabled(): Boolean
    fun previouslyAuthorized(): Boolean = false
    suspend fun connect(): Result<Unit>
    suspend fun execute(command: String): Result<String>
    fun close()
}

class AdbAccessGateway(
    private val debug: () -> Boolean,
    private val transport: LocalAdbTransport,
    private val authorizationMs: Long = 60_000,
    private val commandMs: Long = 10_000,
    private val authorized: () -> Boolean = { false },
    private val rememberAuthorization: () -> Unit = {},
) : AdbAccess {
    private val mutableState = MutableStateFlow(AdbAccessState.DISABLED)
    override val state: StateFlow<AdbAccessState> = mutableState
    override fun debugEnabled() = debug()
    override fun previouslyAuthorized() = authorized()

    override suspend fun connect(): Result<Unit> {
        close()
        if (!runCatching(debug).getOrDefault(false)) {
            mutableState.value = AdbAccessState.DISABLED
            return Result.failure(IllegalStateException("Debugging disabled"))
        }
        if (!runCatching { transport.reachable() }.getOrDefault(false)) {
            mutableState.value = AdbAccessState.UNREACHABLE
            return Result.failure(IllegalStateException("Local ADB unavailable"))
        }
        mutableState.value = AdbAccessState.WAITING
        return guarded(authorizationMs) {
            val identity = transport.authenticate()
            check(Regex("(?:^|\\s)uid=2000\\(shell\\)").containsMatchIn(identity))
            mutableState.value = AdbAccessState.READY
            rememberAuthorization()
        }.onFailure {
            transport.close()
            mutableState.value = AdbAccessState.REFUSED
        }
    }

    override suspend fun execute(command: String): Result<String> {
        if (mutableState.value != AdbAccessState.READY || !runCatching(debug).getOrDefault(false)) {
            transport.close()
            mutableState.value = AdbAccessState.LOST
            return Result.failure(IllegalStateException("Local ADB lost"))
        }
        return guarded(commandMs) { transport.command(command) }.onFailure {
            if (it !is ShellOperationFailure) {
                transport.close()
                mutableState.value = AdbAccessState.LOST
            }
        }
    }

    private suspend fun <T> guarded(timeout: Long, block: suspend () -> T): Result<T> = supervisorScope {
        val work = async { block() }
        val deadline = launch {
            delay(timeout)
            transport.close()
            work.cancel(CancellationException("Local ADB deadline"))
        }
        try {
            Result.success(work.await())
        } catch (cancelled: CancellationException) {
            transport.close()
            if (cancelled.message != "Local ADB deadline") throw cancelled
            Result.failure(cancelled)
        } catch (failure: Exception) {
            Result.failure(failure)
        } finally {
            deadline.cancel()
        }
    }

    override fun close() {
        transport.close()
        if (mutableState.value == AdbAccessState.READY || mutableState.value == AdbAccessState.WAITING) mutableState.value = AdbAccessState.LOST
    }

    companion object {
        fun create(context: Context) = AdbAccessGateway(
            { Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) != 0 },
            DadbLocalTransport(context.noBackupFilesDir),
            authorized = { File(context.noBackupFilesDir, "local-adb-authorized").exists() },
            rememberAuthorization = { File(context.noBackupFilesDir, "local-adb-authorized").writeText("authorized") },
        )
    }
}

internal class DadbLocalTransport(private val directory: File, private val port: Int = 5555) : LocalAdbTransport {
    private val address = InetAddress.getByAddress(byteArrayOf(127, 0, 0, 1))
    private val session = AtomicReference<OwnedAdbSession?>()
    override suspend fun reachable() = withContext(Dispatchers.IO) {
        runCatching {
            Socket().use { it.connect(InetSocketAddress(address, port), 3_000) }
        }.isSuccess
    }

    override suspend fun authenticate(): String = withContext(Dispatchers.IO) {
        val current = OwnedAdbSession()
        session.getAndSet(current)?.close()
        try {
            val privateKey = File(directory, "local-adb-key")
            val publicKey = File(directory, "local-adb-key.pub")
            if (!privateKey.exists() || !publicKey.exists()) AdbKeyPair.generate(privateKey, publicKey)
            currentCoroutineContext().ensureActive()
            current.connect(address, port, AdbKeyPair.read(privateKey, publicKey))
            val identity = current.shell("id")
            check(identity.exitCode == 0)
            currentCoroutineContext().ensureActive()
            identity.output
        } catch (failure: Throwable) {
            session.compareAndSet(current, null)
            current.close()
            throw failure
        }
    }

    override suspend fun command(command: String): String = withContext(Dispatchers.IO) {
        val response = checkNotNull(session.get()).shell(command)
        if (response.exitCode != 0) throw ShellOperationFailure(response.allOutput)
        response.output.trim()
    }

    override fun close() {
        session.getAndSet(null)?.close()
    }
}

private class OwnedAdbSession : AutoCloseable {
    private val socket = Socket()
    private var connection: SocketAdbConnection? = null
    private var closed = false

    fun connect(address: InetAddress, port: Int, keyPair: AdbKeyPair) {
        socket.soTimeout = 60_000
        socket.connect(InetSocketAddress(address, port), 3_000)
        val established = SocketAdbConnection.connect(socket, keyPair)
        synchronized(this) {
            if (closed) {
                established.close()
                error("Local ADB session closed")
            }
            connection = established
        }
    }

    fun shell(command: String): dadb.AdbShellResponse {
        val current = synchronized(this) { check(!closed); checkNotNull(connection) }
        return current.shell(command)
    }

    override fun close() {
        val established = synchronized(this) {
            closed = true
            connection.also { connection = null }
        }
        runCatching { socket.close() }
        runCatching { established?.close() }
    }
}
