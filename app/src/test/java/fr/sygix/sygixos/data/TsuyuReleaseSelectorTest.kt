/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import fr.sygix.sygixos.domain.Release
import fr.sygix.sygixos.domain.ReleaseAsset
import fr.sygix.sygixos.domain.TsuyuRelease
import fr.sygix.sygixos.domain.UpdateSelector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TsuyuReleaseSelectorTest {

    @Test
    fun `the first final release of the api order is selected`() {
        val selected = TsuyuReleaseSelector.select(listOf(tsuyu("v1.2.0"), tsuyu("v1.3.0"), tsuyu("v1.1.0")), includePrereleases = false)
        assertEquals(TsuyuRelease("v1.2.0", false, url("v1.2.0"), SIZE, SHA), selected)
    }

    @Test
    fun `prereleases are skipped unless included`() {
        val releases = listOf(tsuyu("v1.3.0-beta.1", prerelease = true), tsuyu("v1.2.0"))
        assertEquals("v1.2.0", TsuyuReleaseSelector.select(releases, includePrereleases = false)?.tag)
        val included = TsuyuReleaseSelector.select(releases, includePrereleases = true)
        assertEquals("v1.3.0-beta.1", included?.tag)
        assertEquals(true, included?.prerelease)
    }

    @Test
    fun `a suffixed tag is a prerelease even without the flag`() {
        listOf("v2.0.0-alpha.1", "v2.0.0-beta", "v2.0.0-RC2", "v2.0.0-rc.3").forEach { tag ->
            val releases = listOf(tsuyu(tag, prerelease = false), tsuyu("v1.0.0"))
            assertEquals(tag, "v1.0.0", TsuyuReleaseSelector.select(releases, includePrereleases = false)?.tag)
            assertEquals(tag, true, TsuyuReleaseSelector.eligible(releases.first())?.prerelease)
        }
    }

    @Test
    fun `only prereleases with the setting off gives nothing`() {
        assertNull(TsuyuReleaseSelector.select(listOf(tsuyu("v1.0.0-rc.1", prerelease = true)), includePrereleases = false))
    }

    @Test
    fun `drafts are ignored`() {
        val releases = listOf(tsuyu("v1.3.0", draft = true), tsuyu("v1.2.0"))
        assertEquals("v1.2.0", TsuyuReleaseSelector.select(releases, includePrereleases = true)?.tag)
    }

    @Test
    fun `releases without an eligible tsuyu apk are ignored`() {
        val rejected = listOf(
            emptyList(),
            listOf(asset(name = "Tsuyu.apk")),
            listOf(asset(name = UpdateSelector.APK_NAME)),
            listOf(asset(name = "tsuyu.apk.sha256")),
            listOf(asset(digest = null)),
            listOf(asset(digest = "md5:$SHA")),
            listOf(asset(digest = "sha256:abc")),
            listOf(asset(state = "starter")),
            listOf(asset(size = 0)),
            listOf(asset(size = UpdateSelector.MAX_APK_BYTES + 1)),
            listOf(asset(url = "http://github.com/Sygix/Tsuyu/releases/download/v9.0.0/tsuyu.apk")),
        )
        rejected.forEach { assets ->
            val releases = listOf(Release("v9.0.0", false, false, "", assets), tsuyu("v1.0.0"))
            assertEquals(assets.toString(), "v1.0.0", TsuyuReleaseSelector.select(releases, includePrereleases = true)?.tag)
        }
    }

    @Test
    fun `the digest is normalised to lower case`() {
        val release = Release("v1.0.0", false, false, "", listOf(asset(digest = "sha256:" + SHA.uppercase())))
        assertEquals(SHA, TsuyuReleaseSelector.eligible(release)?.sha256)
    }

    private fun tsuyu(tag: String, prerelease: Boolean = false, draft: Boolean = false) =
        Release(tag, draft, prerelease, "https://github.com/Sygix/Tsuyu/releases/tag/$tag", listOf(asset(url = url(tag))))

    private fun asset(
        name: String = TsuyuReleaseSelector.APK_NAME,
        state: String = "uploaded",
        size: Long = SIZE,
        digest: String? = "sha256:$SHA",
        url: String = url("v9.0.0"),
    ) = ReleaseAsset(name, state, size, digest, url)

    private fun url(tag: String) = "https://github.com/Sygix/Tsuyu/releases/download/$tag/tsuyu.apk"

    private companion object {
        const val SIZE = 2_048L
        val SHA = "ab".repeat(32)
    }
}
