/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import android.os.Looper
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.sygix.sygixos.core.designsystem.Dimens
import fr.sygix.sygixos.data.UpdateHarness
import fr.sygix.sygixos.ui.settings.SettingsHarness
import fr.sygix.sygixos.ui.settings.waitForToggle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class UpdateBadgeTest {

    @get:Rule
    val compose = createComposeRule()

    @get:Rule
    val folder = TemporaryFolder()

    private val badge = MutableStateFlow(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var settings: SettingsHarness? = null

    @After
    fun tearDown() {
        settings?.clear()
        scope.cancel()
    }

    private fun press(key: Key) {
        compose.onRoot().performKeyInput {
            keyDown(key)
            keyUp(key)
        }
        compose.waitForIdle()
    }

    private fun bounds(tag: String): Rect = compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot

    private fun showHome() {
        compose.setContent {
            TestHome(
                catalog = catalogOf(dock = listOf(app("com.dock", "Dock")), grid = listOf(app("com.grid", "Grid"))),
                hero = heroStateOf(items = listOf(heroItem("h1", "Programme", sourcePackage = "com.source"))),
                updateBadge = badge,
            )
        }
        compose.waitForIdle()
        compose.onNodeWithTag("hero-open").assertIsFocused()
    }

    @Test
    fun `gear badge follows the proposed version and leaves the capsule unchanged`() {
        showHome()
        compose.onNodeWithTag("update-badge-gear", useUnmergedTree = true).assertDoesNotExist()
        val capsule = bounds("hero-capsule")
        val gear = bounds("settings-gear")
        compose.runOnIdle { badge.value = true }
        compose.waitForIdle()
        compose.onNode(hasTestTag("update-badge-gear") and hasAnyAncestor(hasTestTag("settings-gear")), useUnmergedTree = true).assertExists()
        assertEquals(capsule, bounds("hero-capsule"))
        assertEquals(gear, bounds("settings-gear"))
        compose.runOnIdle { badge.value = false }
        compose.waitForIdle()
        compose.onNodeWithTag("update-badge-gear", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `gear badge is never focused and stays on the corner of the zoomed gear`() {
        badge.value = true
        showHome()
        press(Key.DirectionUp)
        compose.onNodeWithTag("settings-gear").assertIsFocused()
        compose.onNodeWithTag("update-badge-gear", useUnmergedTree = true).assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Focused))
        val gear = bounds("settings-gear")
        val dot = bounds("update-badge-gear")
        val scale = Dimens.GearFocusScale
        assertEquals(gear.center.x + gear.width * scale / 2f, dot.right, 1f)
        assertEquals(gear.center.y - gear.height * scale / 2f, dot.top, 1f)
        assertEquals(12f * scale, dot.width, 1f)
        press(Key.DirectionDown)
        compose.onNodeWithTag("hero-open").assertIsFocused()
        press(Key.DirectionUp)
        compose.onNodeWithTag("settings-gear").assertIsFocused()
    }

    @Test
    fun `prerelease switch updates the switch, the version and both badges without any request`() {
        val updates = UpdateHarness(scope, Dispatchers.IO, folder.root, installedCode = 199)
        updates.publish("v0.0.1", "v0.0.2-rc.1")
        updates.repository.status
        updates.repository.check()
        compose.waitUntil(20_000) {
            shadowOf(Looper.getMainLooper()).idle()
            updates.repository.status.value.lastResult != null
        }
        val requests = updates.transport.calls.size
        val harness = SettingsHarness(updates = updates.repository).also { settings = it }
        val gearBadge = updates.repository.status.map { it.badge }.stateIn(scope, SharingStarted.Eagerly, false)
        compose.setContent {
            val state by harness.viewModel.state.collectAsState()
            TestHome(
                catalog = catalogOf(dock = listOf(app("com.dock", "Dock")), grid = listOf(app("com.grid", "Grid"))),
                hero = heroStateOf(items = listOf(heroItem("h1", "Programme", sourcePackage = "com.source"))),
                settings = state,
                updateBadge = gearBadge,
                update = harness.viewModel.updateActions,
            )
        }
        compose.waitForIdle()
        compose.onNodeWithTag("update-badge-gear", useUnmergedTree = true).assertDoesNotExist()
        press(Key.DirectionUp)
        press(Key.Enter)
        press(Key.DirectionDown)
        press(Key.DirectionDown)
        compose.onNodeWithTag("settings-category-ABOUT").assertIsFocused()
        compose.onNodeWithTag("update-badge-about", useUnmergedTree = true).assertDoesNotExist()
        press(Key.DirectionRight)
        press(Key.DirectionDown)
        compose.onNodeWithTag("update-prereleases").assertIsFocused()
        compose.waitForToggle("update-prereleases", on = false)
        press(Key.Enter)
        compose.waitForToggle("update-prereleases", on = true)
        compose.waitUntil(10_000) { hasText("Préversion 0.0.2-rc.1 disponible", substring = true).matches(compose.onNodeWithTag("update-check").fetchSemanticsNode()) }
        compose.onNode(hasTestTag("update-badge-about"), useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("update-install").assertExists()
        compose.onNodeWithTag("update-prereleases").assertIsFocused()
        press(Key.Back)
        compose.onNode(hasTestTag("update-badge-gear"), useUnmergedTree = true).assertExists()
        assertEquals(requests, updates.transport.calls.size)
        assertTrue(updates.gateway.sessions.isEmpty())
    }
}
