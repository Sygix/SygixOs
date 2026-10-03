/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateSelectorTest {

    private fun proposed(installed: Long, prereleases: Boolean, vararg releases: Release): String? =
        UpdateSelector.proposed(UpdateSelector.known(releases.toList()), prereleases, installed)?.versionName

    @Test
    fun `final version after a prerelease is proposed`() {
        assertEquals("0.0.1", proposed(164, false, release("v0.0.1-rc.4"), release("v0.0.1")))
    }

    @Test
    fun `prerelease is hidden by default`() {
        assertNull(proposed(199, false, release("v0.0.1"), release("v0.0.2-rc.1")))
    }

    @Test
    fun `latest prerelease is proposed when enabled`() {
        assertEquals("0.0.2-rc.2", proposed(199, true, release("v0.0.2-rc.1"), release("v0.0.2-rc.2"), release("v0.0.1")))
    }

    @Test
    fun `installed prerelease with the default switch never gets an older final version`() {
        assertNull(proposed(261, false, release("v0.0.1"), release("v0.0.2-rc.1")))
    }

    @Test
    fun `order follows the version and not the publication date`() {
        assertEquals("0.1.0", proposed(199, true, release("v0.1.0"), release("v0.0.1-rc.5")))
        assertEquals("0.1.0", proposed(199, true, release("v0.0.1-rc.5"), release("v0.1.0")))
    }

    @Test
    fun `same version is up to date`() {
        assertNull(proposed(199, true, release("v0.0.1")))
    }

    @Test
    fun `malformed tags are ignored and the choice is made among the others`() {
        val releases = arrayOf(release("v1.0"), release("nightly"), release("0.0.3"), release("v0.0.3-rc.30"), release("v0.0.2"))
        assertEquals("0.0.2", proposed(199, true, *releases))
    }

    @Test
    fun `release without a verifiable apk is ignored`() {
        val noApk = release("v0.0.4", assets = listOf(asset(name = "other.zip")))
        val notUploaded = release("v0.0.5", assets = listOf(asset(state = "starter")))
        val noDigest = release("v0.0.6", assets = listOf(asset(digest = null)))
        val otherDigest = release("v0.0.7", assets = listOf(asset(digest = "md5:" + "a".repeat(32))))
        val httpUrl = release("v0.0.8", assets = listOf(asset(url = "http://github.com/x/app-release.apk")))
        val emptyAsset = release("v0.0.9", assets = listOf(asset(size = 0)))
        assertEquals("0.0.3", proposed(199, true, noApk, notUploaded, noDigest, otherDigest, httpUrl, emptyAsset, release("v0.0.3")))
        assertNull(proposed(199, true, noApk, notUploaded, noDigest, otherDigest, httpUrl, emptyAsset))
    }

    @Test
    fun `drafts are ignored`() {
        assertNull(proposed(199, true, release("v0.0.2", draft = true)))
    }

    @Test
    fun `release flagged prerelease by the api is a prerelease`() {
        val flagged = release("v0.0.2", prerelease = true)
        assertNull(proposed(199, false, flagged))
        assertEquals("0.0.2", proposed(199, true, flagged))
        assertTrue(requireNotNull(UpdateSelector.eligible(flagged)).prerelease)
    }

    @Test
    fun `empty list proposes nothing`() {
        assertEquals(KnownUpdates.None, UpdateSelector.known(emptyList()))
        assertNull(proposed(1, true))
    }

    @Test
    fun `known keeps the best final and the best of all channels with the digest in lower case`() {
        val known = UpdateSelector.known(
            listOf(
                release("v0.0.1"),
                release("v0.0.2-rc.1"),
                release("v0.0.0", assets = listOf(asset(digest = "sha256:" + "B".repeat(64)))),
            ),
        )
        assertEquals("v0.0.1", known.bestFinal?.tag)
        assertEquals("v0.0.2-rc.1", known.bestAny?.tag)
        assertEquals("b".repeat(64), UpdateSelector.eligible(release("v0.0.0", assets = listOf(asset(digest = "sha256:" + "B".repeat(64)))))?.sha256)
    }

    @Test
    fun `removing a withdrawn version keeps the other known one`() {
        val known = UpdateSelector.known(listOf(release("v0.0.2"), release("v0.0.3-rc.1")))
        val withoutRc = known.without("v0.0.3-rc.1")
        assertEquals("v0.0.2", withoutRc.bestAny?.tag)
        assertEquals("v0.0.2", withoutRc.bestFinal?.tag)
        val withoutFinal = known.without("v0.0.2")
        assertNull(withoutFinal.bestFinal)
        assertEquals("v0.0.3-rc.1", withoutFinal.bestAny?.tag)
    }
}
