/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class HeroCapsuleTest {

    @get:Rule
    val compose = createComposeRule()

    private val clock = MutableStateFlow("21:47")

    private fun press(key: Key) {
        compose.onRoot().performKeyInput {
            keyDown(key)
            keyUp(key)
        }
        compose.waitForIdle()
    }

    private fun setupHome(launchable: Boolean = false) {
        val items = if (launchable) listOf(heroItem("h1", "Programme", sourcePackage = "com.source")) else emptyList()
        compose.setContent {
            TestHome(
                catalog = catalogOf(dock = listOf(app("com.dock", "Dock")), grid = listOf(app("com.grid", "Grid"))),
                hero = heroStateOf(items = items),
                clock = clock,
            )
        }
        compose.waitForIdle()
        compose.onNodeWithTag(if (launchable) "hero-open" else "zone-hero").assertIsFocused()
    }

    @Test
    fun `capsule holds the clock then the gear at the top right of the hero`() {
        setupHome()
        compose.onNode(hasTestTag("hero-clock") and hasAnyAncestor(hasTestTag("hero-capsule"))).assertTextEquals("21:47")
        compose.onNode(hasTestTag("settings-gear") and hasAnyAncestor(hasTestTag("hero-capsule"))).assertExists()
        val capsule = compose.onNodeWithTag("hero-capsule").fetchSemanticsNode().boundsInRoot
        val root = compose.onRoot().fetchSemanticsNode().size
        assertTrue(capsule.left > root.width / 2f)
        assertTrue(capsule.bottom < root.height / 4f)
        val clockBounds = compose.onNodeWithTag("hero-clock").fetchSemanticsNode().boundsInRoot
        val gearBounds = compose.onNodeWithTag("settings-gear").fetchSemanticsNode().boundsInRoot
        assertTrue(clockBounds.right <= gearBounds.left)
    }

    @Test
    fun `only the gear takes the focus and left or right keep it there`() {
        setupHome(launchable = true)
        compose.onNodeWithTag("hero-clock").assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Focused))
        press(Key.DirectionUp)
        compose.onNodeWithTag("settings-gear").assertIsFocused()
        press(Key.DirectionLeft)
        compose.onNodeWithTag("settings-gear").assertIsFocused()
        press(Key.DirectionRight)
        compose.onNodeWithTag("settings-gear").assertIsFocused()
        press(Key.DirectionUp)
        compose.onNodeWithTag("settings-gear").assertIsFocused()
        press(Key.DirectionDown)
        compose.onNodeWithTag("hero-open").assertIsFocused()
        compose.onNodeWithTag("settings-gear").assertIsNotFocused()
    }

    @Test
    fun `new clock value is shown`() {
        setupHome()
        compose.runOnIdle { clock.value = "21:48" }
        compose.onNodeWithTag("hero-clock").assertTextEquals("21:48")
    }

    @Test
    fun `capsule is off screen in the grid view`() {
        setupHome()
        press(Key.DirectionDown)
        press(Key.DirectionDown)
        compose.onNodeWithTag("app-tile-com.grid").assertIsFocused()
        assertFalse(compose.onNodeWithTag("hero-capsule").fetchSemanticsNode().boundsInRoot.bottom > 0f)
    }
}
