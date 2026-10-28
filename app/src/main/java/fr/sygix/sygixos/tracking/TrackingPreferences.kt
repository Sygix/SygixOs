/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged

private val Context.trackingData by preferencesDataStore(name = "tracking-preferences")

interface TrackingSettingsStore : PlaybackPreferences {
    val country: kotlinx.coroutines.flow.Flow<String?>
    val players: kotlinx.coroutines.flow.Flow<Map<String, String>>
    val provider: kotlinx.coroutines.flow.Flow<String> get() = kotlinx.coroutines.flow.flowOf(TrackingProviderKind.BETASERIES.id)
    suspend fun confirmCountry(country: String)
    suspend fun selectProvider(provider: String) = Unit
}

class TrackingPreferences(context: Context) : TrackingSettingsStore {
    private val data = context.applicationContext.trackingData
    private val countryKey = stringPreferencesKey("country")
    private val providerKey = stringPreferencesKey("provider")
    override val country = data.data.map { it[countryKey] }.distinctUntilChanged()
    override val provider = data.data.map { values ->
        values[providerKey]?.let(TrackingProviderKind::fromId)?.id ?: TrackingProviderKind.BETASERIES.id
    }.distinctUntilChanged()
    override val players = data.data.map { preferences -> preferences.asMap().entries.mapNotNull { (key, value) ->
        (value as? String)?.takeIf { key.name.startsWith("player:") }?.let { key.name to it }
    }.toMap() }
    override suspend fun preferred(content: TrackingIdentity): String? = data.data.first()[key(content)]
    override suspend fun remember(content: TrackingIdentity, packageName: String) { data.edit { it[key(content)] = packageName } }
    override suspend fun confirmCountry(country: String) { require(country in java.util.Locale.getISOCountries()); data.edit { it[countryKey] = country } }
    override suspend fun selectProvider(provider: String) {
        require(TrackingProviderKind.fromId(provider) != null)
        data.edit { it[providerKey] = provider }
    }
    private fun key(content: TrackingIdentity) = stringPreferencesKey(playerKey(content))
    companion object { fun playerKey(content: TrackingIdentity) = "player:${content.provider}:${content.type}:${content.id}" }
}
