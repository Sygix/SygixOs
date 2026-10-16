/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos

import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SygixOsAppTest {
    @Test
    fun `boot count comes from the global setting and is absent without it`() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val app = object : SygixOsApp() {
            init { attachBaseContext(context) }
        }
        assertNull(app.bootCount())
        Settings.Global.putInt(context.contentResolver, Settings.Global.BOOT_COUNT, 5)
        assertEquals(5, app.bootCount())
    }
}
