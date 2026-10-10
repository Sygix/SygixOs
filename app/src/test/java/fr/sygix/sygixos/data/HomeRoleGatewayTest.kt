/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import fr.sygix.sygixos.model.SystemControlState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HomeRoleGatewayTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private class Source : HomeRoleSource {
        var available = true
        var held = false
        override fun available() = available
        override fun held() = held
        override fun requestIntent() = Intent("test.role")
    }

    private fun gateway(
        source: HomeRoleSource = Source(),
        dialogs: MutableList<String?> = mutableListOf(),
        screens: MutableList<String?> = mutableListOf(),
        dialog: (Intent) -> Boolean = { true },
        resolves: (Intent) -> Boolean = { true },
        open: (Intent) -> Boolean = { true },
        defaultHome: () -> String? = { null },
    ) = AndroidHomeRoleGateway(
        context = context,
        launchDialog = { dialogs.add(it.action); dialog(it) },
        source = source,
        resolves = resolves,
        open = { screens.add(it.action); open(it) },
        defaultHome = defaultHome,
    )

    @Test
    fun `role dialogue is launched through the result launcher and held role is inert`() {
        val source = Source()
        val dialogs = mutableListOf<String?>()
        val screens = mutableListOf<String?>()
        val gateway = gateway(source, dialogs, screens)
        assertEquals(SystemControlState.INACTIVE, gateway.state())
        assertTrue(dialogs.isEmpty())
        assertTrue(gateway.request())
        assertEquals(listOf("test.role"), dialogs)
        assertTrue(screens.isEmpty())
        assertEquals(SystemControlState.INACTIVE, gateway.state())
        source.held = true
        assertEquals(SystemControlState.ACTIVE, gateway.state())
        assertFalse(gateway.request())
        assertEquals(1, dialogs.size)
    }

    @Test
    fun `unavailable role uses resolvable fallback and rereads default home`() {
        var default: String? = null
        val dialogs = mutableListOf<String?>()
        val screens = mutableListOf<String?>()
        val gateway = gateway(
            Source().apply { available = false }, dialogs, screens,
            resolves = { it.action == Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS }, defaultHome = { default },
        )
        assertEquals(SystemControlState.INACTIVE, gateway.state())
        assertTrue(gateway.request())
        assertTrue(dialogs.isEmpty())
        assertEquals(listOf(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS), screens)
        default = context.packageName
        assertEquals(SystemControlState.ACTIVE, gateway.state())
    }

    @Test
    fun `failed dialogue falls back through both screens without crashing`() {
        val dialogs = mutableListOf<String?>()
        val screens = mutableListOf<String?>()
        val gateway = gateway(dialogs = dialogs, screens = screens, dialog = { false }, open = {
            if (it.action != Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS) throw SecurityException()
            true
        })
        assertTrue(gateway.request())
        assertEquals(listOf("test.role"), dialogs)
        assertEquals(listOf(Settings.ACTION_HOME_SETTINGS, Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS), screens)
        assertEquals(SystemControlState.INACTIVE, gateway.state())
    }

    @Test
    fun `no role or fallback remains unavailable and never opens anything`() {
        val dialogs = mutableListOf<String?>()
        val screens = mutableListOf<String?>()
        val gateway = gateway(
            Source().apply { available = false }, dialogs, screens,
            resolves = { throw SecurityException() }, defaultHome = { throw SecurityException() },
        )
        assertEquals(SystemControlState.UNAVAILABLE, gateway.state())
        assertFalse(gateway.request())
        assertTrue(dialogs.isEmpty())
        assertTrue(screens.isEmpty())
    }

    @Test
    fun `failed role dialogue without fallback becomes unavailable`() {
        val source = Source()
        val gateway = gateway(source, dialog = { false }, resolves = { false }, open = { false })
        assertFalse(gateway.request())
        assertEquals(SystemControlState.UNAVAILABLE, gateway.state())
        source.held = true
        assertEquals(SystemControlState.ACTIVE, gateway.state())
    }

    @Test
    fun `role launcher only launches while an activity is bound`() {
        val launcher = RoleRequestLauncher()
        val launched = mutableListOf<String?>()
        assertFalse(launcher.launch(Intent("test.role")))
        val bound: (Intent) -> Unit = { launched.add(it.action) }
        launcher.bind(bound)
        assertTrue(launcher.launch(Intent("test.role")))
        launcher.unbind {}
        assertTrue(launcher.launch(Intent("test.role")))
        launcher.unbind(bound)
        assertFalse(launcher.launch(Intent("test.role")))
        launcher.bind { throw android.content.ActivityNotFoundException() }
        assertFalse(launcher.launch(Intent("test.role")))
        assertEquals(listOf("test.role", "test.role"), launched)
    }
}
