/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import fr.sygix.sygixos.model.UpNextItem

class TrackingProviderRegistry(betaSeries: TrackingContentProvider, trakt: TrackingContentProvider,
    private val preferred: () -> String = { "betaseries" },
) : TrackingContentProvider {
    private val providers = mapOf(betaSeries.id to betaSeries, trakt.id to trakt)
    override val id: String get() = preferred()

    fun forId(id: String): TrackingContentProvider = providers[id] ?: throw TrackingException(TrackingFailure.CONFIGURATION)
    private fun forToken(token: String): TrackingContentProvider = forId(TraktSessionEnvelope.provider(token))
    private fun forContent(token: String, content: TrackingContent): TrackingContentProvider = forToken(token).also { source ->
        if (content.identity.provider != source.id || content.parent.provider != source.id) throw TrackingSessionChangedException()
    }

    override suspend fun account(token: String) = forToken(token).account(token)
    override suspend fun validateSession(token: String) = forToken(token).validateSession(token)
    override suspend fun snapshot(token: String) = forToken(token).snapshot(token)
    override suspend fun revoke(token: String) = forToken(token).revoke(token)
    override suspend fun identify(token: String, item: UpNextItem, known: LocalTrackingIdentity?): TrackingContent? {
        val source = forToken(token)
        return source.identify(token, item, known?.takeIf { it.identity.provider == source.id && it.parent.provider == source.id })
    }
    override suspend fun platforms(country: String) = forId(id).platforms(country)
    override suspend fun episodes(token: String, content: TrackingContent) = forContent(token, content).episodes(token, content)
    override suspend fun seasons(token: String, content: TrackingContent) = forContent(token, content).seasons(token, content)
    override suspend fun mutate(token: String, content: TrackingContent, action: TrackingAction) = forContent(token, content).mutate(token, content, action)
    override suspend fun sendMutation(token: String, content: TrackingContent, action: TrackingAction) = forContent(token, content).sendMutation(token, content, action)
    override suspend fun readTarget(token: String, content: TrackingContent, action: TrackingAction) = forContent(token, content).readTarget(token, content, action)
    override suspend fun verify(token: String, content: TrackingContent, action: TrackingAction) = forContent(token, content).verify(token, content, action)
    override suspend fun reserve(token: String, content: TrackingContent, desired: Map<String, Int>) = forContent(token, content).reserve(token, content, desired)
    override suspend fun offers(token: String, content: TrackingContent, country: String) = forContent(token, content).offers(token, content, country)
    override fun matches(entry: TrackingContent, action: TrackingAction, original: TrackingContent): Boolean =
        entry.identity == original.identity && entry.parent == original.parent && forId(original.identity.provider).matches(entry, action, original)
    override fun confirmOffers(offers: List<PlaybackOffer>, region: RegionalPlatforms?) = forId(id).confirmOffers(offers, region)
}

internal class TrackingObservedSessionStore(private val delegate: TrackingSessionStore, private val observed: (String?) -> Unit) : TrackingSessionStore {
    override suspend fun read(): String? = delegate.read().also(observed)
    override suspend fun write(token: String?) { delegate.write(token); observed(token) }
}
