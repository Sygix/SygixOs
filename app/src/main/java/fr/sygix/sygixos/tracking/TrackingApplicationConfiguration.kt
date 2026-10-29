/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import android.util.AtomicFile
import java.io.File
import java.net.URI
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

internal class TrackingApplicationConfiguration<T : Any>(
    private val file: File?,
    private val dispatcher: CoroutineDispatcher,
    private val decode: (JSONObject) -> T?,
    private val encode: (T) -> JSONObject,
    private val discover: suspend () -> T,
) {
    private val mutex = Mutex()
    private var cached: T? = null
    private var rejected: T? = null
    private var restored = false

    suspend fun get(): T = withContext(dispatcher) {
        withTimeoutOrNull(30_000) {
            mutex.withLock {
                if (!restored) {
                    cached = read()
                    restored = true
                }
                cached?.let { return@withLock it }
                val value = discover()
                if (value == rejected) throw TrackingException(TrackingFailure.CONFIGURATION)
                write(value)
                cached = value
                value
            }
        } ?: throw TrackingException(TrackingFailure.CONFIGURATION)
    }

    suspend fun invalidate(previous: T) = withContext(dispatcher) {
        mutex.withLock {
            if (!restored) {
                cached = read()
                restored = true
            }
            if (cached == previous || cached == null) {
                rejected = previous
                cached = null
                file?.let { target ->
                    try {
                        AtomicFile(target).delete()
                        if (target.exists()) throw TrackingException(TrackingFailure.STORAGE)
                    } catch (error: TrackingException) { throw error }
                    catch (_: Exception) { throw TrackingException(TrackingFailure.STORAGE) }
                }
            }
        }
    }

    private fun read(): T? {
        val target = file ?: return null
        val text = try { AtomicFile(target).openRead().use { it.readBytes().toString(Charsets.UTF_8) } }
        catch (_: java.io.FileNotFoundException) {
            if (target.exists()) throw TrackingException(TrackingFailure.STORAGE)
            return null
        }
        catch (_: Exception) { throw TrackingException(TrackingFailure.STORAGE) }
        if (text.isBlank()) return null
        return try { decode(JSONObject(text)) }
        catch (_: org.json.JSONException) { null }
    }

    private fun write(value: T) {
        val atomic = file?.let(::AtomicFile) ?: return
        val output = try { atomic.startWrite() }
        catch (_: Exception) { throw TrackingException(TrackingFailure.STORAGE) }
        try {
            output.write(encode(value).toString().toByteArray())
            atomic.finishWrite(output)
        } catch (_: Exception) {
            atomic.failWrite(output)
            throw TrackingException(TrackingFailure.STORAGE)
        }
    }
}

internal class TrackingOfficialResources(private val transport: TrackingTransport, private val host: String) {
    private val visited = mutableSetOf<String>()
    private var scripts = 0

    fun resolve(base: String, path: String): String = try {
        URI(base).resolve(path).toString().also {
            if (!officialTrackingUrl(it) || URI(it).host != host) throw TrackingException(TrackingFailure.CONFIGURATION)
        }
    } catch (error: TrackingException) { throw error }
    catch (_: Exception) { throw TrackingException(TrackingFailure.CONFIGURATION) }

    suspend fun page(): String = text("https://$host/")

    suspend fun script(base: String, path: String): String {
        val url = resolve(base, path)
        if (!visited.add(url)) throw TrackingException(TrackingFailure.CONFIGURATION)
        if (++scripts > 6) throw TrackingException(TrackingFailure.CONFIGURATION)
        return text(url)
    }

    private suspend fun text(initial: String): String {
        var url = resolve(initial, initial)
        repeat(4) {
            val response = transport.execute(TrackingRequest(url, headers = mapOf("User-Agent" to userAgent), maxBytes = 10 * 1024 * 1024))
            if (response.body.toByteArray().size > 10 * 1024 * 1024) throw TrackingException(TrackingFailure.CONFIGURATION)
            if (response.status == 200) return response.body
            if (response.status !in setOf(301, 302, 303, 307, 308)) throw TrackingException(TrackingFailure.CONFIGURATION)
            val location = response.headers.entries.firstOrNull { it.key.equals("location", true) }?.value
                ?: throw TrackingException(TrackingFailure.CONFIGURATION)
            url = resolve(url, location)
        }
        throw TrackingException(TrackingFailure.CONFIGURATION)
    }

    companion object {
        private const val userAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36"
    }
}
