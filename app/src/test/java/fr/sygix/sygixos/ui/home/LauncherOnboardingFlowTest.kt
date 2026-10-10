/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.sygix.sygixos.data.LauncherPrefs
import fr.sygix.sygixos.data.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class LauncherOnboardingFlowTest {
    @get:Rule val compose = createComposeRule()
    @get:Rule val viewModels = LauncherViewModelRule()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val prefs = LauncherPrefs(context)
    private val gateway = FakeLauncherGateway()
    private var launcher by mutableStateOf<LauncherSystemViewModel?>(null)

    @Before
    fun reset() {
        runBlocking { context.dataStore.edit { it.clear() } }
    }

    private fun press(key: Key) {
        compose.onRoot().performKeyInput { keyDown(key); keyUp(key) }
        compose.waitForIdle()
    }

    private fun onboardingShown() = compose.onAllNodesWithTag("onboarding-launcher").fetchSemanticsNodes().isNotEmpty()

    @Test
    fun `ignored onboarding appears after the permission answer and is never shown again`() {
        launcher = viewModels.create(prefs, gateway, started = false)
        compose.setContent {
            launcher?.let { vm ->
                val state by vm.state.collectAsState()
                TestHome(
                    catalog = catalogOf(dock = listOf(app("com.dock", "Dock")), grid = listOf(app("com.grid", "Grid"))),
                    hero = heroStateOf(emptyList()),
                    launcherState = state,
                    launcherActions = vm.actions,
                )
            }
        }
        compose.runOnIdle { launcher!!.onStartupFinished() }
        compose.waitUntil(5_000) {
            compose.waitForIdle()
            launcher!!.state.value.preferencesLoaded
        }
        compose.waitForIdle()
        assertFalse(launcher!!.state.value.showOnboarding)
        assertFalse(onboardingShown())
        compose.runOnIdle { launcher!!.onPermissionFinished() }
        compose.waitUntil(5_000) { onboardingShown() }
        compose.onNodeWithTag("onboarding-row-home-role").assertIsFocused()
        press(Key.Back)
        compose.onNodeWithTag("onboarding-launcher").assertDoesNotExist()
        compose.onNodeWithTag("zone-hero").assertIsFocused()
        compose.waitUntil(5_000) {
            compose.waitForIdle()
            runBlocking { prefs.launcherSystem.first() }.onboardingDismissed
        }
        assertTrue(gateway.opened.isEmpty())
        assertFalse(runBlocking { prefs.launcherSystem.first() }.bootStart.enabled)
        assertFalse(launcher!!.state.value.bootEnabled)

        runBlocking { prefs.setBootStartEnabled(true, null) }
        compose.runOnIdle {
            launcher = viewModels.create(prefs, gateway)
        }
        compose.waitUntil(5_000) {
            compose.waitForIdle()
            launcher!!.state.value.bootEnabled
        }
        compose.waitForIdle()
        assertFalse(launcher!!.state.value.showOnboarding)
        assertFalse(onboardingShown())
        assertTrue(gateway.opened.isEmpty())
    }
}
