/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DexMetadataTest {

    private fun zip(vararg entries: Pair<String, Int>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            entries.forEach { (name, size) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(ByteArray(size) { 1 })
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    @Test
    fun `profile with or without its metadata is a dex metadata archive`() {
        assertTrue(DexMetadata.isValid(zip("primary.prof" to 16, "primary.profm" to 16)))
        assertTrue(DexMetadata.isValid(zip("primary.prof" to 16)))
    }

    @Test
    fun `anything else is refused`() {
        assertFalse(DexMetadata.isValid(ByteArray(0)))
        assertFalse(DexMetadata.isValid("not a zip".toByteArray()))
        assertFalse(DexMetadata.isValid(zip("primary.profm" to 16)))
        assertFalse(DexMetadata.isValid(zip("primary.prof" to 16, "classes.dex" to 16)))
        assertFalse(DexMetadata.isValid(zip("primary.prof" to 0)))
        assertFalse(DexMetadata.isValid(zip("../primary.prof" to 16)))
        assertFalse(DexMetadata.isValid(zip("primary.prof" to 16, "primary.prof/" to 0)))
    }

    @Test
    fun `selector keeps the profile asset only with a sha256 digest and an https url`() {
        val profile = asset(name = UpdateSelector.PROFILE_NAME, size = 100, url = "https://github.com/Sygix/SygixOs/releases/download/v0.0.2/app-release.dm")
        val withProfile = release("v0.0.2", assets = listOf(asset(url = "https://github.com/Sygix/SygixOs/releases/download/v0.0.2/app-release.apk"), profile))
        assertEquals(ProfileAsset(profile.downloadUrl, 100, Sha), UpdateSelector.eligible(withProfile)?.profile)
        listOf(
            profile.copy(digest = null),
            profile.copy(digest = "sha256:abc"),
            profile.copy(state = "starter"),
            profile.copy(size = 0),
            profile.copy(size = UpdateSelector.MAX_PROFILE_BYTES + 1),
            profile.copy(downloadUrl = "http://github.com/x.dm"),
        ).forEach { bad ->
            val candidate = UpdateSelector.eligible(release("v0.0.2", assets = listOf(asset(), bad)))
            assertTrue(candidate != null)
            assertNull(candidate?.profile)
        }
    }
}
