/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseVersionTest {

    private fun code(tag: String) = ReleaseVersion.parse(tag)?.versionCode

    @Test
    fun `tags convert with the build formula`() {
        assertEquals(164L, code("v0.0.1-rc.4"))
        assertEquals(199L, code("v0.0.1"))
        assertEquals(100200335L, code("v1.2.3-beta.5"))
        assertEquals(261L, code("v0.0.2-rc.1"))
        assertEquals(101L, code("v0.0.1-alpha.1"))
        assertEquals(2_099_999_999L, code("v20.999.999"))
        assertEquals(129L, code("v0.0.1-alpha.29"))
    }

    @Test
    fun `dot before the stage number is optional`() {
        assertEquals(code("v0.0.2-rc.1"), code("v0.0.2-rc1"))
    }

    @Test
    fun `malformed tags are rejected`() {
        listOf("v1.0", "0.0.3", "v0.0.3-rc.30", "v21.0.0", "nightly", "v0.0.1-pre.1", "v1.0.0-rc.1-extra", "V0.0.1", "v0.0.1-rc.").forEach {
            assertNull(it, ReleaseVersion.parse(it))
        }
    }

    @Test
    fun `name drops the v prefix and suffix marks a prerelease`() {
        val rc = requireNotNull(ReleaseVersion.parse("v0.0.2-rc.1"))
        assertEquals("0.0.2-rc.1", rc.name)
        assertTrue(rc.suffixed)
        assertFalse(requireNotNull(ReleaseVersion.parse("v0.0.2")).suffixed)
    }

    @Test
    fun `a final version beats every prerelease of the same number`() {
        assertTrue(code("v0.0.1")!! > code("v0.0.1-rc.29")!!)
        assertTrue(code("v0.0.1-rc.1")!! > code("v0.0.1-beta.29")!!)
        assertTrue(code("v0.0.1-beta.1")!! > code("v0.0.1-alpha.29")!!)
    }
}
