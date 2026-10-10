/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import fr.sygix.sygixos.ui.MainActivity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LauncherSystemIntentsTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `internal boot and Home requests are distinct and explicitly targeted`() {
        val boot = LauncherSystemIntents.boot(context)
        assertTrue(LauncherSystemIntents.isBoot(boot))
        assertFalse(LauncherSystemIntents.isForegroundHome(boot))
        assertEquals(MainActivity::class.java.name, boot.component?.className)
        val home = LauncherSystemIntents.home(context, true)
        assertTrue(LauncherSystemIntents.isForegroundHome(home))
        assertFalse(LauncherSystemIntents.isBoot(home))
        assertFalse(LauncherSystemIntents.isForegroundHome(LauncherSystemIntents.home(context, false)))
    }

    @Test
    fun `exported activity callers cannot forge boot observation or foreground reset`() {
        val forged = Intent(context, MainActivity::class.java)
            .putExtra("fr.sygix.sygixos.LAUNCHER_SOURCE", "boot")
            .putExtra("fr.sygix.sygixos.LAUNCHER_TOKEN", "forged")
            .putExtra("fr.sygix.sygixos.LAUNCHER_FOREGROUND", true)
        assertFalse(LauncherSystemIntents.isBoot(forged))
        forged.putExtra("fr.sygix.sygixos.LAUNCHER_SOURCE", "accessibility")
        assertFalse(LauncherSystemIntents.isForegroundHome(forged))
        assertFalse(LauncherSystemIntents.isBoot(Intent(Intent.ACTION_MAIN)))
    }
}
