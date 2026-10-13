/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DadbLocalTransportTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun closingDuringRealDadbHandshakeClosesSocketAndRejectsLateAuthorization() {
        val address = InetAddress.getByAddress(byteArrayOf(127, 0, 0, 1))
        val executor = Executors.newFixedThreadPool(2)
        val peer = CompletableFuture<Socket>()
        val handshake = CompletableFuture<Unit>()
        ServerSocket(0, 1, address).use { server ->
            val transport = DadbLocalTransport(folder.root, port = server.localPort)
            val remote = executor.submit<Int> {
                val socket = server.accept()
                peer.complete(socket)
                socket.soTimeout = 5_000
                val input = socket.getInputStream()
                val header = input.readNBytes(24)
                check(header.size == 24)
                val size = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN).getInt(12)
                input.readNBytes(size)
                handshake.complete(Unit)
                input.read()
            }
            val authentication = executor.submit<Result<String>> { runBlocking { runCatching { transport.authenticate() } } }
            try {
                handshake.get(5, TimeUnit.SECONDS)
                transport.close()
                val latePayload = "device::features=shell_v2\u0000".toByteArray()
                val command = 0x4e584e43
                val header = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN).putInt(command).putInt(0x01000001).putInt(4096)
                    .putInt(latePayload.size).putInt(latePayload.sumOf { it.toInt() and 255 }).putInt(command xor -1).array()
                runCatching { peer.get().getOutputStream().apply { write(header); write(latePayload); flush() } }
                assertEquals(-1, remote.get(5, TimeUnit.SECONDS))
                assertTrue(authentication.get(5, TimeUnit.SECONDS).isFailure)
            } finally {
                transport.close()
                peer.getNow(null)?.close()
                server.close()
                executor.shutdownNow()
                assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS))
            }
        }
    }

    @Test fun cancellingGatewayDuringRealHandshakeUnblocksWorkerAndClosesSocket() {
        val address = InetAddress.getByAddress(byteArrayOf(127, 0, 0, 1))
        val executor = Executors.newFixedThreadPool(2)
        val peer = CompletableFuture<Socket>()
        val handshake = CompletableFuture<Unit>()
        val job = CompletableFuture<Job>()
        ServerSocket(0, 2, address).use { server ->
            val transport = DadbLocalTransport(folder.root, port = server.localPort)
            val gateway = AdbAccessGateway({ true }, transport)
            val remote = executor.submit<Int> {
                server.accept().use { it.soTimeout = 5_000; check(it.getInputStream().read() == -1) }
                val socket = server.accept()
                peer.complete(socket)
                socket.soTimeout = 5_000
                val input = socket.getInputStream()
                val header = input.readNBytes(24)
                check(header.size == 24)
                val size = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN).getInt(12)
                input.readNBytes(size)
                handshake.complete(Unit)
                input.read()
            }
            val authentication = executor.submit<Unit> {
                runBlocking {
                    val connect = launch { gateway.connect() }
                    job.complete(connect)
                    connect.join()
                }
            }
            try {
                handshake.get(5, TimeUnit.SECONDS)
                job.get().cancel()
                assertEquals(-1, remote.get(5, TimeUnit.SECONDS))
                authentication.get(5, TimeUnit.SECONDS)
                assertNotEquals(fr.sygix.sygixos.model.AdbAccessState.READY, gateway.state.value)
            } finally {
                gateway.close()
                peer.getNow(null)?.close()
                server.close()
                executor.shutdownNow()
                assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS))
            }
        }
    }
}
