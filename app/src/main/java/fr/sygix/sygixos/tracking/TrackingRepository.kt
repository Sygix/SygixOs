/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import androidx.room.withTransaction
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import java.security.MessageDigest
import java.util.UUID

data class TrackingRepositoryView(
    val account: StoredTrackingAccount?,
    val contents: List<TrackingContent> = emptyList(),
    val pending: Int = 0,
    val incomplete: Boolean = false,
    val pendingIdentities: Set<TrackingIdentity> = emptySet(),
    val undoContexts: List<StoredTrackingUndo> = emptyList(),
)

class TrackingRepository(private val database: TrackingDatabase, private val clock: () -> Long = System::currentTimeMillis) {
    private val dao = database.tracking()
    private val departureGate = Any()
    private var binding: Pair<String, String>? = null
    val view = flow {
        combine(dao.observeAccount(), dao.observeContents(), dao.observeIntents(), dao.observeReserves(), dao.observeUndo()) { _, _, _, _, _ -> Unit }
            .collect { emit(database.withTransaction {
                val account = dao.account() ?: return@withTransaction TrackingRepositoryView(null)
                val intents = dao.intents(account.incarnation).associateBy { it.key }
                val contents = dao.contents(account.incarnation).map { row ->
                    val content = TrackingContentCodec.decode(row.payload)
                    intents[row.key]?.let { effective(content, it.desiredState) } ?: content
                }
                TrackingRepositoryView(account, contents, intents.size, dao.reserves(account.incarnation).any { it.needsFill || !it.complete },
                    contents.filter { it.identity.storageKey in intents }.map { it.identity }.toSet(), dao.undoContexts(account.incarnation))
            }) }
    }.flowOn(Dispatchers.IO)

    suspend fun restore(token: String): StoredTrackingAccount? = database.withTransaction {
        dao.account()?.takeIf { it.sessionHash == hash(token) }?.also { account ->
            synchronized(departureGate) { binding = if (account.accessDenied) null else account.incarnation to account.sessionHash }
        }
    }

    suspend fun bind(token: String, account: TrackingAccount): StoredTrackingAccount = database.withTransaction {
        synchronized(departureGate) { binding = null }
        val previous = dao.account()
        val current = if (previous?.accountId == account.id) previous.copy(name = account.name, sessionHash = hash(token), accessDenied = false,
            revision = previous.revision + 1, failure = null)
            else {
                clear()
                StoredTrackingAccount(accountId = account.id, name = account.name, incarnation = UUID.randomUUID().toString(), sessionHash = hash(token))
            }
        dao.putAccount(current)
        synchronized(departureGate) { binding = current.incarnation to current.sessionHash }
        current
    }

    internal fun depart(incarnation: String, token: String, block: () -> Unit) = synchronized(departureGate) {
        if (binding != (incarnation to hash(token))) throw TrackingSessionChangedException()
        block()
    }

    suspend fun account(): StoredTrackingAccount? = dao.account()
    suspend fun pending(incarnation: String): List<StoredTrackingIntent> = dao.intents(incarnation)
    suspend fun contents(incarnation: String): List<TrackingContent> = dao.contents(incarnation).map { TrackingContentCodec.decode(it.payload) }
    suspend fun reserves(incarnation: String): List<StoredTrackingReserve> = dao.reserves(incarnation).filter { it.needsFill }

    suspend fun snapshot(incarnation: String, startedRevision: Long, snapshot: TrackingSnapshot) = database.withTransaction {
        val account = active(incarnation) ?: return@withTransaction
        val existing = dao.contents(incarnation).associateBy { it.key }
        val reserves = dao.reserves(incarnation).associateBy { it.parentKey }
        val intents = dao.intents(incarnation)
        val incoming = snapshot.contents.associateBy { it.identity.storageKey }
        val protected = dao.intents(incarnation).map { it.key }.toSet() + dao.references(incarnation).map { it.key }
        if (snapshot.failure == null) existing.values.filter { it.key !in incoming && it.key !in protected && it.revision <= startedRevision }
            .forEach { dao.deleteContent(incarnation, it.key) }
        val accepted = snapshot.contents.filter { (existing[it.identity.storageKey]?.revision ?: 0) <= startedRevision }
        val revision = account.revision + 1
        dao.putContents(accepted.map { stored(incarnation, it, revision) })
        accepted.filter { it.identity.type == TrackingType.EPISODE }.groupBy { it.parent }.forEach { (parent, _) ->
            val pending = intents.any { existing[it.key]?.parentKey == parent.storageKey }
            val reserve = reserves[parent.storageKey]
            val confirmedLater = snapshot.contents.any { it.parent == parent && (existing[it.identity.storageKey]?.revision ?: 0) > startedRevision }
            val refill = snapshot.failure != null || reserve?.needsFill == true && (reserve.requestedRevision > startedRevision || pending || confirmedLater)
            dao.putReserve(StoredTrackingReserve(incarnation, parent.storageKey, !refill, refill,
                if (snapshot.failure != null) revision else reserve?.requestedRevision ?: 0))
        }
        if (snapshot.failure == null) {
            val present = snapshot.contents.map { it.parent.storageKey }.toSet()
            val pendingParents = intents.mapNotNull { existing[it.key]?.parentKey }.toSet()
            reserves.keys.filter { it !in present && it !in pendingParents }.forEach { dao.deleteReserve(incarnation, it) }
        }
        dao.putAccount(account.copy(revision = revision, snapshotAt = clock(), failure = snapshot.failure?.name))
        cleanup(incarnation)
    }

    suspend fun patch(incarnation: String, content: TrackingContent) = database.withTransaction {
        val account = active(incarnation) ?: return@withTransaction
        val revision = account.revision + 1
        dao.putAccount(account.copy(revision = revision))
        val siblings = dao.contents(incarnation).filter { it.parentKey == content.parent.storageKey }.map { TrackingContentCodec.decode(it.payload) }
        val patched = siblings.filter { it.identity != content.identity }.map { sibling -> sibling.copy(
            membership = content.membership.takeUnless { it == AccountMembership.UNKNOWN } ?: sibling.membership,
            archived = content.archived ?: sibling.archived)
        } + content
        dao.putContents(patched.map { stored(incarnation, it, revision) })
        if (content.parent.type == TrackingType.SHOW) {
            if (content.archived == true || content.membership == AccountMembership.ABSENT) dao.deleteReserve(incarnation, content.parent.storageKey)
            else if (siblings.any { it.archived == true || it.membership == AccountMembership.ABSENT })
                dao.putReserve(StoredTrackingReserve(incarnation, content.parent.storageKey, false, true, revision))
        }
    }

    suspend fun record(incarnation: String, content: TrackingContent, action: TrackingAction,
        undoRemainingMillis: Long? = null): StoredTrackingIntent = database.withTransaction {
        val account = active(incarnation)?.takeUnless { it.accessDenied } ?: throw TrackingException(TrackingFailure.ACCESS)
        require(action in setOf(TrackingAction.SEEN, TrackingAction.UNSEEN, TrackingAction.RESTORE_MOVIE_STATE))
        val previous = dao.intents(incarnation).firstOrNull { it.key == content.identity.storageKey }
        val state = when (action) { TrackingAction.SEEN -> 1; TrackingAction.UNSEEN -> 0; else -> content.movieState }
        if (previous?.undoHeld == true) {
            if (undoRemainingMillis != null && previous.desiredState == state &&
                dao.undoContexts(incarnation).any { it.key == previous.key && it.revision == previous.revision }) return@withTransaction previous
            throw TrackingException(TrackingFailure.INCOMPLETE)
        }
        val revision = account.revision + 1
        val old = dao.contents(incarnation).firstOrNull { it.key == content.identity.storageKey }
        if (old == null) dao.putContents(listOf(stored(incarnation, content, revision)))
        val intent = StoredTrackingIntent(incarnation, content.identity.storageKey, revision, state,
            if (content.identity.type == TrackingType.MOVIE) content.movieState else if (content.seen) 1 else 0,
            possiblySent = undoRemainingMillis == null && previous?.possiblySent == true, undoHeld = undoRemainingMillis != null)
        dao.putAccount(account.copy(revision = revision))
        dao.putIntent(intent)
        if (undoRemainingMillis != null) {
            dao.putUndo(StoredTrackingUndo(incarnation, intent.key, revision, undoRemainingMillis.coerceAtLeast(0),
                TrackingContentCodec.encode(content), previous?.revision, previous?.desiredState, previous?.previousState, previous?.possiblySent))
            dao.putReference(StoredTrackingReference(incarnation, intent.key, "undo"))
        }
        if (content.identity.type == TrackingType.EPISODE) dao.putReserve(StoredTrackingReserve(incarnation, content.parent.storageKey, false, true, revision))
        intent
    }

    suspend fun undo(intent: StoredTrackingIntent, previous: TrackingContent): Boolean = database.withTransaction {
        val account = active(intent.incarnation) ?: throw TrackingException(TrackingFailure.ACCESS)
        val pending = dao.intents(intent.incarnation).firstOrNull { it.key == intent.key }
        val context = dao.undoContexts(intent.incarnation).firstOrNull { it.key == intent.key && it.revision == intent.revision }
        if (intent.undoHeld && (context == null || pending?.revision != intent.revision)) return@withTransaction false
        if (intent.undoHeld && context != null && pending?.revision == intent.revision) {
            dao.deleteIntent(intent.incarnation, intent.key, intent.revision)
            dao.deleteUndo(intent.incarnation, intent.key, intent.revision)
            if (context.previousIntentRevision != null && context.previousDesiredState != null && context.previousState != null) {
                dao.putIntent(StoredTrackingIntent(intent.incarnation, intent.key, context.previousIntentRevision,
                    context.previousDesiredState, context.previousState, context.previousPossiblySent == true))
            }
            val restored = TrackingContentCodec.decode(context.previousPayload)
            dao.putContents(listOf(stored(intent.incarnation, restored, account.revision + 1)))
            dao.putAccount(account.copy(revision = account.revision + 1))
            if (dao.undoContexts(intent.incarnation).isEmpty()) dao.deleteReferences(intent.incarnation, "undo")
            cleanup(intent.incarnation)
            return@withTransaction true
        }
        if (pending?.revision == intent.revision && !pending.possiblySent) {
            dao.deleteIntent(intent.incarnation, intent.key, intent.revision)
            dao.putAccount(account.copy(revision = account.revision + 1))
            return@withTransaction true
        } else {
            record(intent.incarnation, previous, if (previous.identity.type == TrackingType.MOVIE) TrackingAction.RESTORE_MOVIE_STATE
                else if (previous.seen) TrackingAction.SEEN else TrackingAction.UNSEEN)
            return@withTransaction false
        }
    }

    suspend fun updateUndoRemaining(incarnation: String, key: String, revision: Long, remainingMillis: Long): Boolean = database.withTransaction {
        val held = dao.undoContexts(incarnation).firstOrNull { it.key == key && it.revision == revision } ?: return@withTransaction false
        dao.putUndo(held.copy(remainingMillis = remainingMillis.coerceAtLeast(0)))
        true
    }

    suspend fun releaseUndo(incarnation: String, key: String, revision: Long): Boolean = database.withTransaction {
        val held = dao.undoContexts(incarnation).firstOrNull { it.key == key && it.revision == revision } ?: return@withTransaction false
        val intent = dao.intents(incarnation).firstOrNull { it.key == key && it.revision == revision } ?: return@withTransaction false
        dao.deleteUndo(incarnation, key, revision)
        dao.putIntent(intent.copy(undoHeld = false))
        if (dao.undoContexts(incarnation).isEmpty()) dao.deleteReferences(incarnation, "undo")
        val account = requireNotNull(active(incarnation))
        dao.putAccount(account.copy(revision = account.revision + 1))
        cleanup(incarnation)
        true
    }

    suspend fun undoContext(incarnation: String, key: String, revision: Long): StoredTrackingUndo? =
        dao.undoContexts(incarnation).firstOrNull { it.key == key && it.revision == revision }

    suspend fun restoreUndoReferences(incarnation: String) = database.withTransaction {
        val keys = dao.undoContexts(incarnation).map { it.key }.toSet()
        val existing = dao.references(incarnation).filter { it.owner == "undo" }.map { it.key }.toSet()
        if (keys == existing) return@withTransaction
        dao.deleteReferences(incarnation, "undo")
        keys.forEach { dao.putReference(StoredTrackingReference(incarnation, it, "undo")) }
    }

    suspend fun markSent(intent: StoredTrackingIntent): Boolean = database.withTransaction {
        val account = active(intent.incarnation)?.takeUnless { it.accessDenied } ?: return@withTransaction false
        val current = dao.intents(account.incarnation).firstOrNull { it.key == intent.key && it.revision == intent.revision }
            ?: return@withTransaction false
        if (current.undoHeld || dao.undoContexts(account.incarnation).any { it.key == intent.key && it.revision == intent.revision }) return@withTransaction false
        dao.putIntent(current.copy(possiblySent = true))
        true
    }

    suspend fun confirm(intent: StoredTrackingIntent, content: TrackingContent) = database.withTransaction {
        val account = active(intent.incarnation) ?: return@withTransaction
        dao.undoContexts(intent.incarnation).firstOrNull { it.key == intent.key && it.previousIntentRevision == intent.revision }
            ?.let { context -> dao.putUndo(context.copy(previousIntentRevision = null, previousDesiredState = null,
                previousState = null, previousPossiblySent = null)) }
        val revision = account.revision + 1
        dao.putAccount(account.copy(revision = revision, failure = null))
        dao.putContents(listOf(stored(intent.incarnation, content, revision)))
        dao.deleteIntent(intent.incarnation, intent.key, intent.revision)
        cleanup(intent.incarnation)
    }

    suspend fun fill(incarnation: String, parent: TrackingIdentity, contents: List<TrackingContent>, complete: Boolean, startedRevision: Long) = database.withTransaction {
        val account = active(incarnation) ?: return@withTransaction
        val existing = dao.contents(incarnation).associateBy { it.key }
        val revision = account.revision + 1
        dao.putAccount(account.copy(revision = revision))
        dao.putContents(contents.filter { (existing[it.identity.storageKey]?.revision ?: 0) <= startedRevision }.map { stored(incarnation, it, revision) })
        val changed = account.revision != startedRevision
        dao.putReserve(StoredTrackingReserve(incarnation, parent.storageKey, complete && !changed, !complete || changed,
            if (!complete || changed) account.revision else 0))
        cleanup(incarnation)
    }

    suspend fun references(incarnation: String, owner: String, identities: Set<TrackingIdentity>) = database.withTransaction {
        val account = active(incarnation) ?: return@withTransaction
        if (dao.references(incarnation).filter { it.owner == owner }.map { it.key }.toSet() == identities.map { it.storageKey }.toSet()) return@withTransaction
        dao.putAccount(account.copy(revision = account.revision + 1))
        dao.deleteReferences(incarnation, owner)
        identities.forEach { dao.putReference(StoredTrackingReference(incarnation, it.storageKey, owner)) }
        cleanup(incarnation)
    }

    suspend fun failure(incarnation: String, failure: TrackingFailure?, token: String? = null) = database.withTransaction {
        val account = active(incarnation) ?: return@withTransaction
        if (token != null && account.sessionHash != hash(token)) return@withTransaction
        dao.putAccount(account.copy(revision = account.revision + 1, failure = failure?.name, accessDenied = account.accessDenied || failure == TrackingFailure.ACCESS,
            retryAt = if (failure == null) 0 else account.retryAt))
    }

    suspend fun defer(incarnation: String, retryAt: Long, token: String? = null) = database.withTransaction {
        val account = active(incarnation) ?: return@withTransaction
        if (token != null && account.sessionHash != hash(token)) return@withTransaction
        dao.putAccount(account.copy(revision = account.revision + 1, retryAt = maxOf(retryAt, account.retryAt), failure = TrackingFailure.NETWORK.name))
    }

    suspend fun purge() = database.withTransaction {
        synchronized(departureGate) { binding = null }
        clear()
    }

    private suspend fun active(incarnation: String) = dao.account()?.takeIf { it.incarnation == incarnation }
    private suspend fun clear() { dao.clearAccount(); dao.clearIntents(); dao.clearContents(); dao.clearReferences(); dao.clearReserves(); dao.clearUndo() }

    private suspend fun cleanup(incarnation: String) {
        val intents = dao.intents(incarnation).associateBy { it.key }
        val protected = intents.keys + dao.references(incarnation).map { it.key } + dao.undoContexts(incarnation).map { it.key }
        val entries = dao.contents(incarnation).associateWith { row ->
            val content = TrackingContentCodec.decode(row.payload)
            intents[row.key]?.let { effective(content, it.desiredState) } ?: content
        }
        val filling = dao.reserves(incarnation).filter { it.needsFill }.map { it.parentKey }.toSet()
        val seeds = entries.entries.filter { it.key.parentKey in filling }.groupBy { it.key.parentKey }.values.map { group ->
            group.minBy { if (it.value.seen) 1 else 0 }.key.key
        }.toSet()
        val window = entries.values.filter { it.identity.type == TrackingType.EPISODE && !it.seen && it.airDate != null }
            .groupBy { it.parent }.values.flatMap { it.sortedWith(compareBy<TrackingContent> { it.season }.thenBy { it.episode }).take(6) }
            .map { it.identity.storageKey }.toSet()
        entries.forEach { (row, content) ->
            val consumed = content.seen || content.membership == AccountMembership.ABSENT ||
                content.identity.type == TrackingType.MOVIE && content.movieState != 0 || content.identity.type == TrackingType.EPISODE && row.key !in window
            if (consumed && row.key !in protected && row.key !in seeds) dao.deleteContent(incarnation, row.key)
        }
    }

    private fun stored(incarnation: String, content: TrackingContent, revision: Long) = StoredTrackingContent(incarnation,
        content.identity.storageKey, content.parent.storageKey, TrackingContentCodec.encode(content.copy(sessionGeneration = null)), revision)

    companion object {
        private fun hash(token: String) = MessageDigest.getInstance("SHA-256").digest(TraktSessionEnvelope.binding(token).toByteArray()).joinToString("") { "%02x".format(it) }
        fun effective(content: TrackingContent, state: Int) = content.copy(seen = state == 1,
            movieState = if (content.identity.type == TrackingType.MOVIE) state else content.movieState)
    }
}
