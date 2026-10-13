/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Context
import android.provider.Settings
import dadb.AdbKeyPair
import dadb.Dadb
import fr.sygix.sygixos.model.AdbAccessState
import java.io.File
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

class DadbLocalTransport(private val directory: File) : LocalAdbTransport {
    private val address = InetAddress.getByAddress(byteArrayOf(127, 0, 0, 1))
    @Volatile private var connection: Dadb? = null
    override suspend fun reachable() = withContext(Dispatchers.IO) {
        runCatching {
            Socket().use { it.connect(InetSocketAddress(address, 5555), 3_000) }
        }.isSuccess
    }

    override suspend fun authenticate(): String = withContext(Dispatchers.IO) {
        val privateKey = File(directory, "local-adb-key")
        val publicKey = File(directory, "local-adb-key.pub")
        if (!privateKey.exists()) AdbKeyPair.generate(privateKey, publicKey)
        check(publicKey.exists())
        val adb = Dadb.create(address.hostAddress!!, 5555, AdbKeyPair.read(privateKey, publicKey), 3_000, 60_000)
        connection = adb
        val identity = adb.shell("id")
        check(identity.exitCode == 0)
        identity.output
    }

    override suspend fun command(command: String): String = withContext(Dispatchers.IO) {
        val response = checkNotNull(connection).shell(command)
        if (response.exitCode != 0) throw ShellOperationFailure(response.allOutput)
        response.output.trim()
    }

    override fun close() {
        val current = connection
        connection = null
        runCatching { current?.close() }
    }
}
