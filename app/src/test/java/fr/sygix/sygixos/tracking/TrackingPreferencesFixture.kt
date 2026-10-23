/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import kotlinx.coroutines.flow.MutableStateFlow

internal class TrackingPreferencesFixture : TrackingSettingsStore {
    override val country = MutableStateFlow<String?>(null)
    override val observation = MutableStateFlow(false)
    override val players = MutableStateFlow<Map<String, String>>(emptyMap())
    override suspend fun confirmCountry(country: String) { this.country.value = country }
    override suspend fun observation(enabled: Boolean) { observation.value = enabled }
    override suspend fun preferred(content: TrackingIdentity) = players.value[TrackingPreferences.playerKey(content)]
    override suspend fun remember(content: TrackingIdentity, packageName: String) { players.value += TrackingPreferences.playerKey(content) to packageName }
}
