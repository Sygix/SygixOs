/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.unit.Dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.sygix.sygixos.core.designsystem.Dimens
import fr.sygix.sygixos.core.designsystem.Motion
import fr.sygix.sygixos.core.designsystem.NestedCorner
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class ConcentricLayoutTest {

    @get:Rule
    val compose = createComposeRule()

    private fun bounds(tag: String): Rect = compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot

    private fun px(dp: Dp): Float = dp.value * compose.density.density

    private fun nested(name: String): NestedCorner = Dimens.Nested.first { it.name == name }

    @Test
    fun `dock tiles sit at the declared margin from the dock edges`() {
        compose.setContent {
            TestHome(catalog = catalogOf(dock = listOf(app("com.d0", "D0"), app("com.d1", "D1")), grid = emptyList()), hero = heroStateOf(items = emptyList()))
        }
        compose.waitForIdle()
        val dock = bounds("dock-glass")
        val first = bounds("app-tile-com.d0")
        val last = bounds("app-tile-com.d1")
        val margin = px(nested("dock-tile").margin)
        assertEquals(margin, first.left - dock.left, 1f)
        assertEquals(margin, first.top - dock.top, 1f)
        assertEquals(margin, dock.bottom - first.bottom, 1f)
        assertEquals(margin, dock.right - last.right, 1f)
    }

    @Test
    fun `gear sits at the declared margin from the capsule edges`() {
        compose.setContent {
            TestHome(catalog = catalogOf(dock = emptyList(), grid = emptyList()), hero = heroStateOf(items = emptyList()))
        }
        compose.waitForIdle()
        val capsule = bounds("hero-capsule")
        val gear = bounds("settings-gear")
        val margin = px(nested("capsule-gear").margin)
        assertEquals(px(Dimens.CapsuleHeight), capsule.height, 1f)
        assertEquals(px(Dimens.GearButton), gear.height, 1f)
        assertEquals(margin, gear.top - capsule.top, 1f)
        assertEquals(margin, capsule.bottom - gear.bottom, 1f)
        assertEquals(margin, capsule.right - gear.right, 1f)
    }

    @Test
    fun `menu pills and thumbnail sit at the declared margins from the menu edges`() {
        compose.setContent {
            TestHome(
                catalog = catalogOf(dock = emptyList(), grid = listOf(app("com.a", "Alpha"))),
                hero = heroStateOf(items = emptyList()),
                initialZone = Zone.GRID,
            )
        }
        compose.waitForIdle()
        compose.onNodeWithTag("app-tile-com.a").assertIsFocused()
        compose.mainClock.autoAdvance = false
        compose.onRoot().performKeyInput { keyDown(Key.DirectionCenter) }
        compose.mainClock.advanceTimeBy(Motion.LONG_PRESS_MS + 100)
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        val menu = bounds("menu-glass")
        val pin = bounds("menu-action-pin")
        val hide = bounds("menu-action-hide")
        val thumbnail = bounds("menu-thumbnail")
        val pillMargin = px(nested("menu-pill").margin)
        assertEquals(pillMargin, pin.left - menu.left, 1f)
        assertEquals(pillMargin, menu.right - pin.right, 1f)
        assertEquals(pillMargin, menu.bottom - hide.bottom, 1f)
        assertEquals(px(nested("menu-thumbnail").margin), thumbnail.top - menu.top, 1f)
        assertEquals(px(Dimens.MenuPadding + Dimens.MenuHeaderInsetStart), thumbnail.left - menu.left, 1f)
    }
}
