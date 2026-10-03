/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateUrlPolicyTest {

    @Test
    fun `https is allowed`() {
        assertTrue(UpdateUrlPolicy.isAllowed("https://api.github.com/repos/Sygix/SygixOs/releases"))
        assertTrue(UpdateUrlPolicy.isAllowed("https://release-assets.githubusercontent.com/x/app-release.apk"))
    }

    @Test
    fun `http, missing scheme and other schemes are refused`() {
        listOf(
            "http://api.github.com/repos/Sygix/SygixOs/releases",
            "api.github.com/repos/Sygix/SygixOs/releases",
            "//api.github.com/x",
            "ftp://github.com/x",
            "file:///sdcard/app.apk",
            "content://x/y",
            "https:///nohost",
            "",
            "not a url",
        ).forEach { assertFalse(it, UpdateUrlPolicy.isAllowed(it)) }
        assertFalse(UpdateUrlPolicy.isAllowed(null))
    }

    @Test
    fun `final url in http after a redirect is refused`() {
        assertFalse(UpdateUrlPolicy.isAllowed("http://release-assets.githubusercontent.com/x/app-release.apk"))
    }

    @Test
    fun `release page must be https on github dot com`() {
        assertTrue(UpdateUrlPolicy.isReleasePage("https://github.com/Sygix/SygixOs/releases/tag/v0.0.2"))
        assertFalse(UpdateUrlPolicy.isReleasePage("http://github.com/Sygix/SygixOs/releases/tag/v0.0.2"))
        assertFalse(UpdateUrlPolicy.isReleasePage("https://evil.example/Sygix/SygixOs/releases/tag/v0.0.2"))
        assertFalse(UpdateUrlPolicy.isReleasePage("https://github.com.evil.example/releases/tag/v0.0.2"))
        assertFalse(UpdateUrlPolicy.isReleasePage(""))
    }
}
