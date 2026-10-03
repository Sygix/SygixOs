/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.core.designsystem

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class BadgeTest {

    @get:Rule
    val compose = createComposeRule()

    private var badged by mutableStateOf(true)

    private val firstFocus = FocusRequester()

    @Composable
    private fun Element(tag: String, badge: Boolean, placement: BadgePlacement, requester: FocusRequester? = null) {
        Box(Modifier.testTag(tag).size(120.dp, 48.dp).tvFocusable(focusRequester = requester).focusable()) {
            if (badge) Badge(placement, tag = "$tag-badge")
        }
    }

    private fun show(placement: BadgePlacement = BadgePlacement.CORNER) {
        compose.setContent {
            Row {
                Element("first", badge = false, placement = placement, requester = firstFocus)
                Spacer(Modifier.width(20.dp))
                Element("second", badge = badged, placement = placement)
                Spacer(Modifier.width(20.dp))
                Element("third", badge = false, placement = placement)
            }
        }
        compose.waitForIdle()
    }

    private fun bounds(tag: String): Rect = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot

    private fun press(key: Key) {
        compose.onRoot().performKeyInput {
            keyDown(key)
            keyUp(key)
        }
        compose.waitForIdle()
    }

    @Test
    fun `badge is a six dp blue dot at the top right corner`() {
        show()
        val host = bounds("second")
        val badge = bounds("second-badge")
        val sixDp = 6f * 2
        assertEquals(sixDp, badge.width, 0.5f)
        assertEquals(sixDp, badge.height, 0.5f)
        assertEquals(host.right, badge.right, 0.5f)
        assertEquals(host.top, badge.top, 0.5f)
        assertEquals(0xFF0A84FF.toInt(), SygixColors.Badge.toArgb())
    }

    @Test
    fun `row badge sits at the end of the row, vertically centred`() {
        show(BadgePlacement.ROW_END)
        val host = bounds("second")
        val badge = bounds("second-badge")
        assertEquals(host.right, badge.right, 0.5f)
        assertEquals(host.center.y, badge.center.y, 0.5f)
    }

    @Test
    fun `element keeps its rectangle with or without the badge`() {
        show()
        val withBadge = bounds("second")
        compose.runOnIdle { badged = false }
        compose.waitForIdle()
        compose.onNodeWithTag("second-badge").assertDoesNotExist()
        assertEquals(withBadge, bounds("second"))
        assertEquals(bounds("first").size, withBadge.size)
    }

    @Test
    fun `badge never takes the focus and the d-pad path is unchanged`() {
        show()
        compose.runOnIdle { firstFocus.requestFocus() }
        compose.onNodeWithTag("first").assertIsFocused()
        press(Key.DirectionRight)
        compose.onNodeWithTag("second").assertIsFocused()
        compose.onNodeWithTag("second-badge").assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Focused))
        press(Key.DirectionRight)
        compose.onNodeWithTag("third").assertIsFocused()
        press(Key.DirectionLeft)
        compose.onNodeWithTag("second").assertIsFocused()
        press(Key.DirectionLeft)
        compose.onNodeWithTag("first").assertIsFocused()
    }

    @Test
    fun `badge causes no redraw at rest`() {
        compose.mainClock.autoAdvance = false
        show()
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()
        fun recompositions() = Recomposer.runningRecomposers.value.sumOf { it.changeCount }
        var writes = 0
        val before = recompositions()
        val handle = Snapshot.registerApplyObserver { changed, _ -> writes += changed.size }
        try {
            compose.mainClock.advanceTimeBy(5_000)
            compose.waitForIdle()
        } finally {
            handle.dispose()
        }
        assertEquals(0L, recompositions() - before)
        assertEquals(0, writes)
    }
}
