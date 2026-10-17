/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import fr.sygix.sygixos.domain.Release
import fr.sygix.sygixos.domain.TsuyuRelease
import fr.sygix.sygixos.domain.TsuyuReleases
import fr.sygix.sygixos.domain.UpdateSelector
import fr.sygix.sygixos.domain.UpdateUrlPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import org.json.JSONException
import org.json.JSONObject

object TsuyuReleaseSelector {

    const val APK_NAME = "tsuyu.apk"
    private const val UPLOADED = "uploaded"
    private val Sha256Digest = Regex("sha256:([0-9a-fA-F]{64})")
    private val PreTag = Regex("-(alpha|beta|rc)([.\\-]?\\d+)?$", RegexOption.IGNORE_CASE)

    fun select(releases: List<Release>, includePrereleases: Boolean): TsuyuRelease? =
        releases.asSequence().mapNotNull(::eligible).firstOrNull { includePrereleases || !it.prerelease }

    fun eligible(release: Release): TsuyuRelease? {
        if (release.draft || release.tag.isBlank()) return null
        val asset = release.assets.firstOrNull { it.name == APK_NAME } ?: return null
        if (asset.state != UPLOADED || asset.size <= 0 || asset.size > UpdateSelector.MAX_APK_BYTES) return null
        val sha256 = asset.digest?.let { Sha256Digest.matchEntire(it) }?.groupValues?.get(1)?.lowercase() ?: return null
        if (!UpdateUrlPolicy.isAllowed(asset.downloadUrl)) return null
        return TsuyuRelease(
            tag = release.tag,
            prerelease = release.prerelease || PreTag.containsMatchIn(release.tag),
            apkUrl = asset.downloadUrl,
            size = asset.size,
            sha256 = sha256,
        )
    }
}

interface TsuyuReleaseStore {
    suspend fun load(): TsuyuRelease?
    suspend fun save(release: TsuyuRelease?)
}

class TsuyuReleaseSource(private val source: ReleaseSource, private val store: TsuyuReleaseStore) : TsuyuReleases {

    override suspend fun refresh(includePrereleases: Boolean): Result<TsuyuRelease?> {
        val selected = source.list().map { TsuyuReleaseSelector.select(it, includePrereleases) }.getOrElse { return Result.failure(it) }
        return try {
            store.save(selected)
            Result.success(selected)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun lastKnown(): TsuyuRelease? = store.load()

    companion object {
        const val API_BASE = "https://api.github.com/repos/Sygix/Tsuyu"
    }
}

class TsuyuReleasePrefs(private val context: Context) : TsuyuReleaseStore {

    private val key = stringPreferencesKey("tsuyu_last_release")

    override suspend fun load(): TsuyuRelease? = context.dataStore.data.first()[key]?.let(::decode)

    override suspend fun save(release: TsuyuRelease?) {
        context.dataStore.edit { if (release == null) it.remove(key) else it[key] = encode(release) }
    }

    internal companion object {
        fun encode(release: TsuyuRelease): String = JSONObject()
            .put("tag", release.tag)
            .put("pre", release.prerelease)
            .put("url", release.apkUrl)
            .put("size", release.size)
            .put("sha256", release.sha256)
            .toString()

        fun decode(value: String): TsuyuRelease? = try {
            val json = JSONObject(value)
            TsuyuRelease(
                tag = json.getString("tag"),
                prerelease = json.getBoolean("pre"),
                apkUrl = json.getString("url"),
                size = json.getLong("size"),
                sha256 = json.getString("sha256"),
            )
        } catch (e: JSONException) {
            null
        }
    }
}
