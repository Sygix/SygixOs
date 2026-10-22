/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import coil.disk.DiskCache
import fr.sygix.sygixos.domain.ArtworkAge
import fr.sygix.sygixos.domain.ArtworkDimensions
import fr.sygix.sygixos.domain.ArtworkPolicy
import fr.sygix.sygixos.model.ProgramArtwork
import java.io.Closeable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal sealed interface ArtworkEvent {
    val uri: String
    data class ImageReady(override val uri: String, val version: String) : ArtworkEvent
    data class ImageFailed(override val uri: String, val cause: ArtworkFailure) : ArtworkEvent
    data class ImageExpired(override val uri: String) : ArtworkEvent
    data class ProgramsChanged(val identities: Set<String>) : ArtworkEvent { override val uri = "" }
}

@OptIn(coil.annotation.ExperimentalCoilApi::class)
internal class ArtworkRepository(
    internal val context: Context,
    private var database: ArtworkDatabase,
    private val disk: DiskCache,
    private val reader: ImageBytesReader,
    private val scheduler: ArtworkScheduler = ArtworkScheduler(),
    private val clock: () -> Long = System::currentTimeMillis,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val decode: CoroutineDispatcher = ArtworkDecodeDispatcher,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + io),
    private val cardWidth: Int = 396,
    private val requestTimeoutMillis: Long = IMAGE_REQUEST_TIMEOUT_MS,
    private val localReader: ImageBytesReader = LocalProgramImageReader(context.contentResolver),
) : Closeable {
    private val dao get() = database.artwork()
    private class Flight(val job: Deferred<ArtworkImage?>, var subscribers: Int = 0)
    private val pending = mutableMapOf<String, Flight>()
    private val watched = mutableMapOf<String, Pair<ProgramArtwork, ArtworkPolicy>>()
    private val consumers = mutableMapOf<String, Int>()
    private val refreshes = mutableMapOf<String, Job>()
    @Volatile private var foreground = true
    private val signals = mutableMapOf<String, MutableStateFlow<ArtworkEvent?>>()
    private val mutableEvents = MutableSharedFlow<ArtworkEvent>(extraBufferCapacity = 128)
    val events = mutableEvents.asSharedFlow()
    private var timer: Job? = null
    var imageLoader: (() -> coil.ImageLoader)? = null
    private val initialized = scope.async {
        val name = database.openHelper.databaseName
        try { dao.images() } catch (cancelled: CancellationException) { throw cancelled } catch (error: Exception) {
            if (name == null) throw error
            database.close()
            context.deleteDatabase(name)
            database = ArtworkDatabase.create(context, name)
            dao.images()
            android.util.Log.w("ProgramArtwork", "cause=index-recreated")
        }
        val prefs = context.getSharedPreferences("artwork-index", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("coil-format", false)) {
            disk.clear()
            prefs.edit().putBoolean("coil-format", true).commit()
        }
        purge()
    }

    @Synchronized fun signal(identity: String): MutableStateFlow<ArtworkEvent?> = signals.getOrPut(identity) { MutableStateFlow(null) }

    @Synchronized fun watch(source: ProgramArtwork, policy: ArtworkPolicy): Closeable {
        watched[source.identity] = source to policy
        consumers[source.identity] = (consumers[source.identity] ?: 0) + 1
        scope.launch { initialized.await(); schedule() }
        return Closeable { synchronized(this) {
            val count = (consumers[source.identity] ?: 1) - 1
            if (count == 0) { consumers.remove(source.identity); watched.remove(source.identity); refreshes.remove(source.identity)?.cancel() } else consumers[source.identity] = count
        } }
    }

    suspend fun resolve(source: ProgramArtwork, policy: ArtworkPolicy, visible: Boolean = true): Pair<ArtworkImage?, ArtworkFailure?> = withContext(io) {
        initialized.await()
        var failure = ArtworkFailure.ABSENT
        for (uri in source.candidates.flatMap(::visualCandidates).distinct()) {
            val parsed = Uri.parse(uri)
            if (parsed.scheme == "android.resource" && parsed.authority != context.packageName) {
                recordFailure(source, uri, ArtworkFailure.QUALITY, generic = true)
                failure = ArtworkFailure.QUALITY
                continue
            }
            if (parsed.scheme in setOf("file", "android.resource")) return@withContext local(source, uri, policy)
            val known = dao.image(source.packageName, uri)
            val compatible = known != null && compatible(known, source)
            val usable = known?.takeIf { compatible && usable(it) }
            if (usable != null) {
                associate(usable, source)
                if (!ArtworkAge.fresh(usable.fetchedAt, clock()) && usable.retryAt <= clock()) refreshInBackground(source, uri)
                schedule()
                if (policy.accepts(ArtworkDimensions(usable.width, usable.height))) return@withContext usable to null
                failure = ArtworkFailure.QUALITY
                continue
            }
            if (known != null && known.retryAt > clock() && !rejectedWithoutIdentityEvidence(known)) { failure = known.failure(); continue }
            val image = fetch(source, uri, visible)
            if (image != null && compatible(image, source) && policy.accepts(ArtworkDimensions(image.width, image.height))) return@withContext image to null
            failure = if (image != null) ArtworkFailure.QUALITY else dao.image(source.packageName, uri)?.failure() ?: ArtworkFailure.TRANSPORT
        }
        null to failure
    }

    private suspend fun local(source: ProgramArtwork, uri: String, policy: ArtworkPolicy): Pair<ArtworkImage?, ArtworkFailure?> {
        val cancellation = ImageCancellation()
        val result = scheduler.run(source.packageName, requestTimeoutMillis, cancellation::cancel) { localReader.read(Uri.parse(uri), cancellation) }
        val bytes = result.getOrElse { return null to if (it is java.util.concurrent.TimeoutException) ArtworkFailure.TIMEOUT else ArtworkFailure.TRANSPORT }
        val dimensions = try { withContext(decode) { verifiedDimensions(bytes) } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { return null to ArtworkFailure.QUALITY }
        if (!policy.accepts(dimensions)) return null to ArtworkFailure.QUALITY
        return ArtworkImage(imageId(source, uri), source.packageName, uri, uri, dimensions.width, dimensions.height, "local", clock(), "valid") to null
    }

    private suspend fun compatible(image: ArtworkImage, source: ProgramArtwork): Boolean = dao.associations(image.id).any {
        it.identity == fingerprint(source) || source.sharedIdentity != null && it.sharedIdentity == artworkHash(source.sharedIdentity)
    }

    private fun usable(image: ArtworkImage): Boolean = image.state == "valid" && ArtworkAge.usable(image.fetchedAt, clock()) && disk.openSnapshot(image.diskKey)?.use { true } == true

    private fun acceptsAnyPolicy(dimensions: ArtworkDimensions): Boolean =
        listOf(ArtworkPolicy.Hero, ArtworkPolicy.Shelf(640), ArtworkPolicy.Card(cardWidth)).any { it.accepts(dimensions) }

    private suspend fun rejectedWithoutIdentityEvidence(image: ArtworkImage): Boolean =
        image.state == "generic" && acceptsAnyPolicy(ArtworkDimensions(image.width, image.height)) && dao.identities(image.packageName, image.hash) < 3

    private suspend fun associate(image: ArtworkImage, source: ProgramArtwork) {
        dao.associate(ArtworkAssociation(image.id, fingerprint(source), source.sharedIdentity?.let(::artworkHash), clock()))
    }

    private suspend fun fetch(source: ProgramArtwork, uri: String, visible: Boolean): ArtworkImage? {
        val flight = synchronized(this) {
            pending[uri]?.also { it.subscribers++; if (visible) scheduler.promote(uri) } ?: run {
                val job = scope.async(start = CoroutineStart.LAZY) { fetchThroughCoil(source, uri, visible) }
                Flight(job, 1).also { item -> pending[uri] = item; job.invokeOnCompletion { synchronized(this) { if (pending[uri] === item) pending.remove(uri) } }; job.start() }
            }
        }
        val image = try { flight.job.await() } finally {
            synchronized(this) {
                flight.subscribers--
                if (flight.subscribers == 0 && !flight.job.isCompleted) flight.job.cancel()
                if (flight.job.isCompleted && pending[uri] === flight) pending.remove(uri)
            }
        }
        if (image != null && (image.packageName != source.packageName || !compatible(image, source))) {
            return fetch(source, uri, visible)
        }
        return image
    }

    private suspend fun fetchThroughCoil(source: ProgramArtwork, uri: String, visible: Boolean): ArtworkImage? {
        val loader = imageLoader?.invoke() ?: return retrieve(source, uri, visible)
        val request = coil.request.ImageRequest.Builder(context).data(Uri.parse(uri))
            .diskCacheKey(uri).diskCachePolicy(coil.request.CachePolicy.WRITE_ONLY)
            .memoryCachePolicy(coil.request.CachePolicy.DISABLED)
            .setParameter(ProgramImageFetcher.FETCH, source, null)
            .setParameter(ProgramImageFetcher.VISIBLE, visible, null)
            .decoderFactory(coil.decode.Decoder.Factory { _, _, _ -> coil.decode.Decoder {
                val image = dao.image(source.packageName, uri) ?: throw java.io.IOException("Missing image index")
                coil.decode.DecodeResult(android.graphics.drawable.ShapeDrawable().apply { intrinsicWidth = image.width; intrinsicHeight = image.height }, false)
            } }).build()
        val result = loader.execute(request)
        return if (result is coil.request.SuccessResult) dao.image(source.packageName, uri)?.takeIf { it.state == "valid" } else null
    }

    internal suspend fun retrieve(source: ProgramArtwork, uri: String, visible: Boolean): ArtworkImage? {
        val cancellation = ImageCancellation()
        val result = scheduler.run(source.packageName, requestTimeoutMillis, cancellation::cancel, visible, uri) { reader.read(Uri.parse(uri), cancellation) }
        val bytes = result.getOrElse { error ->
            val cause = when (error) {
                is SecurityException -> ArtworkFailure.DENIED
                is java.util.concurrent.TimeoutException, is ArtworkBusyException -> ArtworkFailure.TIMEOUT
                is android.os.RemoteException -> ArtworkFailure.PROVIDER
                is java.io.FileNotFoundException -> ArtworkFailure.ABSENT
                else -> ArtworkFailure.TRANSPORT
            }
            recordFailure(source, uri, cause)
            return null
        }
        return try {
            val hash = artworkHash(bytes)
            val (image, generic, events) = database.withTransaction {
                val events = mutableListOf<ArtworkEvent>()
                val old = dao.image(source.packageName, uri)
                val matching = dao.matching(source.packageName, hash)
                val measured = matching.firstOrNull { it.width > 0 && it.height > 0 }
                val dimensions = measured?.let { ArtworkDimensions(it.width, it.height) } ?: withContext(decode) { verifiedDimensions(bytes) }
                val image = ArtworkImage(imageId(source, uri), source.packageName, uri, uri, dimensions.width, dimensions.height, hash, clock(), "valid")
                val identities = matching.flatMap { dao.associations(it.id) }.mapNotNull { it.sharedIdentity }.toSet() +
                    listOfNotNull(source.sharedIdentity?.let(::artworkHash))
                val generic = !acceptsAnyPolicy(dimensions) || identities.size >= 3
                if (generic) {
                    if (identities.size >= 3) matching.forEach {
                        dao.image(it.copy(state = "generic", retryAt = clock() + GENERIC_RETRY_MS))
                        if (it.state != "generic") events += ArtworkEvent.ImageFailed(it.uri, ArtworkFailure.QUALITY)
                    }
                    if (identities.size >= 3 || old == null || !usable(old)) {
                        dao.image(image.copy(state = "generic", retryAt = clock() + GENERIC_RETRY_MS))
                        associate(image, source)
                    }
                    val failed = failureRow(source, uri, ArtworkFailure.QUALITY, generic = true)
                    if (failed.state != "valid") events += ArtworkEvent.ImageFailed(uri, ArtworkFailure.QUALITY)
                    Triple(old?.takeIf { identities.size < 3 && usable(it) && compatible(it, source) }, true, events)
                } else {
                    disk.writeVerified(uri, bytes)
                    if (old != null && old.hash != hash) dao.clearAssociations(old.id)
                    dao.image(image)
                    associate(image, source)
                    Triple(image, false, listOf(ArtworkEvent.ImageReady(uri, hash)))
                }
            }
            events.distinct().forEach(::publish)
            if (generic) imageFailure(source.packageName, Uri.parse(uri), ArtworkFailure.QUALITY.name)
            schedule()
            image
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) { recordFailure(source, uri, ArtworkFailure.TRANSPORT); null }
    }

    private suspend fun recordFailure(source: ProgramArtwork, uri: String, cause: ArtworkFailure, generic: Boolean = false) {
        val value = database.withTransaction { failureRow(source, uri, cause, generic) }
        imageFailure(source.packageName, Uri.parse(uri), cause.name)
        if (value.state != "valid") publish(ArtworkEvent.ImageFailed(uri, cause))
        schedule()
    }

    private suspend fun failureRow(source: ProgramArtwork, uri: String, cause: ArtworkFailure, generic: Boolean): ArtworkImage {
        val previous = dao.image(source.packageName, uri)
        val retry = clock() + if (generic) GENERIC_RETRY_MS else 30_000
        val value = previous?.copy(state = if (usable(previous)) "valid" else if (generic) "generic" else "failed", retryAt = retry, failureCause = cause.name)
            ?: ArtworkImage(imageId(source, uri), source.packageName, uri, uri, 0, 0, "", 0, if (generic) "generic" else "failed", retry, cause.name)
        dao.image(value)
        return value
    }

    private fun publish(event: ArtworkEvent) {
        mutableEvents.tryEmit(event)
        synchronized(this) { watched.forEach { (identity, pair) -> if (event.uri in pair.first.candidates.flatMap(::visualCandidates)) signal(identity).value = event } }
    }

    @Synchronized private fun refreshInBackground(source: ProgramArtwork, uri: String) {
        if (!foreground || refreshes[source.identity]?.isActive == true) return
        refreshes[source.identity] = scope.launch { fetch(source, uri, true) }
    }

    private suspend fun schedule() {
        val now = clock()
        val uris = synchronized(this) { watched.values.flatMap { it.first.candidates.flatMap(::visualCandidates) }.toSet() }
        val images = dao.images().filter { it.uri in uris }
        val next = images.flatMap { image ->
            listOf(image.fetchedAt + ArtworkAge.FRESH_MS, image.fetchedAt + ArtworkAge.RETAIN_MS, image.retryAt).filter { it > now }
        }.minOrNull() ?: return
        synchronized(this) {
            timer?.cancel()
            timer = scope.launch {
                delay((next - clock()).coerceAtLeast(1))
                synchronized(this@ArtworkRepository) { timer = null }
                checkExpiry()
            }
        }
    }

    suspend fun checkExpiry() {
        val watched = synchronized(this) { watched.values.toList() }
        watched.forEach { (source, policy) ->
            source.candidates.forEach { uri -> dao.image(source.packageName, uri)?.let { image ->
                if (image.state == "valid" && !ArtworkAge.usable(image.fetchedAt, clock())) publish(ArtworkEvent.ImageExpired(uri))
            } }
            if (foreground) resolve(source, policy)
        }
        schedule()
    }

    fun onForeground() { foreground = true; scope.launch { initialized.await(); checkExpiry() } }
    @Synchronized fun onBackground() { foreground = false; refreshes.values.forEach { it.cancel() }; refreshes.clear() }

    suspend fun confirm(table: String, programs: List<Pair<ProgramArtwork, String>>) = withContext(io) {
        initialized.await()
        val previous = dao.programs(table).map { it.imageFingerprint }.toSet()
        val now = clock()
        dao.confirm(table, programs.map { (source, full) ->
            val id = source.publicationId.substringAfter('-')
            ArtworkProgram(table, id, source.packageName, fingerprint(source), artworkHash(full), source.sharedIdentity?.let(::artworkHash), now, now + ArtworkAge.FRESH_MS) to
                source.candidates.mapIndexed { rank, uri -> ArtworkCandidate(table, id, rank, uri) }
        })
        val current = programs.map { fingerprint(it.first) }.toSet()
        if (previous != current) mutableEvents.emit(ArtworkEvent.ProgramsChanged((previous - current) + (current - previous)))
        purge()
    }

    private suspend fun purge() {
        dao.expirePrograms(clock())
        dao.removeOrphanAssociations()
        dao.images().forEach { image ->
            if (image.fetchedAt > 0 && !ArtworkAge.usable(image.fetchedAt, clock()) || dao.associations(image.id).isEmpty() && image.retryAt <= clock()) {
                dao.deleteImage(image.id)
                disk.remove(image.diskKey)
            }
        }
    }

    override fun close() {
        scope.coroutineContext[Job]?.invokeOnCompletion { database.close() }
        scope.cancel()
        scheduler.close()
    }
    companion object { const val GENERIC_RETRY_MS = 60 * 60_000L }
}

internal fun fingerprint(source: ProgramArtwork) = artworkHash(source.identity)
private fun imageId(source: ProgramArtwork, uri: String) = artworkHash(source.packageName + "\u0000" + uri)
private fun ArtworkImage.failure(): ArtworkFailure = failureCause?.let { runCatching { ArtworkFailure.valueOf(it) }.getOrNull() }
    ?: if (state == "generic") ArtworkFailure.QUALITY else ArtworkFailure.TRANSPORT
