/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import fr.sygix.sygixos.domain.CheckResult
import fr.sygix.sygixos.domain.KnownUpdates
import fr.sygix.sygixos.domain.UpdateCandidate
import fr.sygix.sygixos.domain.UpdateError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONException
import org.json.JSONObject

data class UpdatePersisted(
    val includePrereleases: Boolean = false,
    val lastCheckAt: Long? = null,
    val lastResult: CheckResult? = null,
    val retryAt: Long? = null,
    val known: KnownUpdates = KnownUpdates.None,
)

interface UpdateStore {
    val data: Flow<UpdatePersisted>
    suspend fun setIncludePrereleases(include: Boolean)
    suspend fun setLastCheckAt(at: Long)
    suspend fun setLastResult(result: CheckResult)
    suspend fun setRetryAt(at: Long)
    suspend fun setKnown(known: KnownUpdates)
    suspend fun setRelaunch(relaunch: Boolean)
    suspend fun takeRelaunch(): Boolean
}

class UpdatePrefs(private val context: Context) : UpdateStore {

    private val includeKey = booleanPreferencesKey("update_include_prereleases")
    private val lastCheckKey = longPreferencesKey("update_last_check_at")
    private val lastResultKey = stringPreferencesKey("update_last_result")
    private val retryKey = longPreferencesKey("update_retry_at")
    private val knownKey = stringPreferencesKey("update_known")
    private val relaunchKey = booleanPreferencesKey("update_relaunch")

    override val data: Flow<UpdatePersisted> = context.dataStore.data.map(::read)

    private fun read(prefs: Preferences): UpdatePersisted {
        val retryAt = prefs[retryKey]
        return UpdatePersisted(
            includePrereleases = prefs[includeKey] ?: false,
            lastCheckAt = prefs[lastCheckKey],
            lastResult = prefs[lastResultKey]?.let { decodeResult(it, retryAt) },
            retryAt = retryAt,
            known = prefs[knownKey]?.let(::decodeKnown) ?: KnownUpdates.None,
        )
    }

    override suspend fun setIncludePrereleases(include: Boolean) {
        context.dataStore.edit { it[includeKey] = include }
    }

    override suspend fun setLastCheckAt(at: Long) {
        context.dataStore.edit { it[lastCheckKey] = at }
    }

    override suspend fun setLastResult(result: CheckResult) {
        context.dataStore.edit { it[lastResultKey] = encodeResult(result) }
    }

    override suspend fun setRetryAt(at: Long) {
        context.dataStore.edit { it[retryKey] = at }
    }

    override suspend fun setKnown(known: KnownUpdates) {
        context.dataStore.edit { it[knownKey] = encodeKnown(known) }
    }

    override suspend fun setRelaunch(relaunch: Boolean) {
        context.dataStore.edit { it[relaunchKey] = relaunch }
    }

    override suspend fun takeRelaunch(): Boolean {
        var relaunch = false
        context.dataStore.edit {
            relaunch = it[relaunchKey] ?: false
            it.remove(relaunchKey)
        }
        return relaunch
    }

    internal companion object {
        private const val OK = "ok"
        private const val NO_NETWORK = "no_network"
        private const val TIMEOUT = "timeout"
        private const val RATE_LIMITED = "rate_limited"
        private const val UNAVAILABLE = "unavailable"
        private const val UNREADABLE = "unreadable"
        private const val BEST_FINAL = "final"
        private const val BEST_ANY = "any"

        fun encodeResult(result: CheckResult): String = when (result) {
            CheckResult.Ok -> OK
            is CheckResult.Error -> when (val error = result.error) {
                UpdateError.NoNetwork -> NO_NETWORK
                UpdateError.Timeout -> TIMEOUT
                is UpdateError.RateLimited -> RATE_LIMITED
                is UpdateError.Unavailable -> "$UNAVAILABLE:${error.code}"
                else -> UNREADABLE
            }
        }

        fun decodeResult(value: String, retryAt: Long?): CheckResult? = when {
            value == OK -> CheckResult.Ok
            value == NO_NETWORK -> CheckResult.Error(UpdateError.NoNetwork)
            value == TIMEOUT -> CheckResult.Error(UpdateError.Timeout)
            value == RATE_LIMITED -> CheckResult.Error(UpdateError.RateLimited(retryAt ?: 0L))
            value.startsWith(UNAVAILABLE) -> CheckResult.Error(UpdateError.Unavailable(value.substringAfter(':', "0").toIntOrNull() ?: 0))
            value == UNREADABLE -> CheckResult.Error(UpdateError.Unreadable)
            else -> null
        }

        fun encodeKnown(known: KnownUpdates): String = JSONObject().apply {
            known.bestFinal?.let { put(BEST_FINAL, encodeCandidate(it)) }
            known.bestAny?.let { put(BEST_ANY, encodeCandidate(it)) }
        }.toString()

        fun decodeKnown(value: String): KnownUpdates = try {
            val json = JSONObject(value)
            KnownUpdates(
                bestFinal = json.optJSONObject(BEST_FINAL)?.let(::decodeCandidate),
                bestAny = json.optJSONObject(BEST_ANY)?.let(::decodeCandidate),
            )
        } catch (e: JSONException) {
            KnownUpdates.None
        }

        private fun encodeCandidate(candidate: UpdateCandidate): JSONObject = JSONObject()
            .put("tag", candidate.tag)
            .put("name", candidate.versionName)
            .put("code", candidate.versionCode)
            .put("pre", candidate.prerelease)
            .put("url", candidate.apkUrl)
            .put("size", candidate.size)
            .put("sha256", candidate.sha256)
            .put("html", candidate.htmlUrl)

        private fun decodeCandidate(json: JSONObject): UpdateCandidate = UpdateCandidate(
            tag = json.getString("tag"),
            versionName = json.getString("name"),
            versionCode = json.getLong("code"),
            prerelease = json.getBoolean("pre"),
            apkUrl = json.getString("url"),
            size = json.getLong("size"),
            sha256 = json.getString("sha256"),
            htmlUrl = json.getString("html"),
        )
    }
}
