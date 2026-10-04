/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.sygix.sygixos.core.designsystem.Dimens
import fr.sygix.sygixos.core.designsystem.Motion
import fr.sygix.sygixos.core.designsystem.SygixOsTheme
import fr.sygix.sygixos.data.AppArtworkSource
import fr.sygix.sygixos.data.MascotAnimationSource
import fr.sygix.sygixos.domain.StartupGate
import fr.sygix.sygixos.domain.StartupTimings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class StartupSplashTest : StartupHostTest(motionScale = 1f) {

    @Test
    fun `minimum duration then fade to the focused hero`() {
        val mascot = FakeMascotDrawable()
        launch(mascot = FakeMascotSource(mascot))
        advanceTo(100)
        catalogReady()
        controller.heroVisualReady()
        assertTrue(mascot.isRunning)
        advanceTo(590)
        assertSplash(true)
        compose.onNodeWithTag("startup-splash-mascot").assertExists()
        assertNothingFocused()
        advanceTo(600)
        frames()
        compose.onNodeWithTag("zone-hero").assertIsFocused()
        advanceTo(600 + Motion.SPLASH_FADE_MS.toLong())
        frames(8)
        assertSplash(false)
        compose.onNodeWithTag("zone-hero").assertIsFocused()
        assertFalse(mascot.isRunning)
        assertEquals(1, mascot.starts)
    }

    @Test
    fun `mascot is centered at its native size with a description`() {
        launch()
        frames(20)
        val mascot = compose.onNodeWithTag("startup-splash-mascot").fetchSemanticsNode()
        val root = compose.onRoot().fetchSemanticsNode()
        val density = compose.density.density
        assertEquals(Dimens.SplashMascot.value, mascot.size.width / density, 0.5f)
        assertEquals(Dimens.SplashMascot.value, mascot.size.height / density, 0.5f)
        val center = mascot.boundsInRoot.center
        assertTrue(abs(center.x - root.size.width / 2f) < 1f)
        assertTrue(abs(center.y - root.size.height / 2f) < 1f)
        compose.onNodeWithContentDescription("SygixOs démarre").assertExists()
        assertEquals(180.dp, Dimens.SplashMascot)
    }

    @Test
    fun `waits for the first hero visual`() {
        launch()
        advanceTo(200)
        catalogReady()
        advanceTo(1_490)
        assertSplash(true)
        assertNothingFocused()
        advanceTo(1_500)
        controller.heroVisualReady()
        frames()
        compose.onNodeWithTag("zone-hero").assertIsFocused()
        advanceTo(1_500 + Motion.SPLASH_FADE_MS.toLong())
        frames(8)
        assertSplash(false)
    }

    @Test
    fun `stops waiting for the hero visual at 2 s`() {
        launch()
        advanceTo(200)
        catalogReady()
        advanceTo(1_990)
        assertSplash(true)
        assertNothingFocused()
        advanceTo(2_000)
        frames()
        compose.onNodeWithTag("zone-hero").assertIsFocused()
        compose.onAllNodesWithTag("hero-poster").assertCountEquals(0)
        advanceTo(2_000 + Motion.SPLASH_FADE_MS.toLong())
        frames(8)
        assertSplash(false)
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `never loaded catalog reveals the black loading screen at 5 s`() {
        launch(mascot = FakeMascotSource(null))
        advanceTo(4_990)
        assertSplash(true)
        advanceTo(5_000 + Motion.SPLASH_FADE_MS.toLong())
        frames(8)
        assertSplash(false)
        compose.onAllNodesWithTag("zone-hero").assertCountEquals(0)
        assertBlack(true)
        catalogReady()
        frames()
        compose.onNodeWithTag("zone-hero").assertIsFocused()
    }

    @Test
    fun `home is composed but unfocusable before the fade`() {
        launch()
        advanceTo(100)
        catalogReady()
        controller.heroVisualReady()
        advanceTo(Motion.SPLASH_APPEAR_MS + 100L)
        compose.onNodeWithTag("zone-hero").assertExists()
        compose.onNodeWithTag("settings-gear").assertExists()
        compose.onNodeWithTag("zone-hero").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithTag("settings-gear").performSemanticsAction(SemanticsActions.RequestFocus)
        frames()
        compose.onNodeWithTag("zone-hero").assertIsNotFocused()
        assertNothingFocused()
        advanceTo(590)
        advanceTo(610)
        assertSplash(true)
        advanceTo(600 + Motion.SPLASH_FADE_MS.toLong())
        frames()
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `opaque splash hides the drawn home until the fade, which then reveals it linearly`() {
        val green = Color(0xFF00C800)
        launch(mascot = FakeMascotSource(null), solidHome = green)
        advanceTo(100)
        catalogReady()
        controller.heroVisualReady()
        listOf(300L, 450L, 590L).forEach { elapsed ->
            advanceTo(elapsed)
            assertSplash(true)
            assertBlack(true)
        }
        advanceTo(600 + Motion.SPLASH_FADE_MS / 2L)
        val middle = pixels()
        assertEquals(1, middle.distinct().size)
        val level = (middle[0] shr 8) and 0xFF
        assertTrue("vert à mi-fondu : $level", level in 20..180)
        assertEquals(0, (middle[0] shr 16) and 0xFF)
        advanceTo(600 + Motion.SPLASH_FADE_MS.toLong())
        frames(8)
        assertSplash(false)
        assertTrue(pixels().all { it == green.toArgb() })
    }

    @Test
    fun `hero owns the focus as soon as the fade starts`() {
        launch()
        advanceTo(100)
        catalogReady()
        controller.heroVisualReady()
        advanceTo(600)
        frames()
        assertSplash(true)
        compose.onNodeWithTag("zone-hero").assertIsFocused()
        press(listOf(Key.DirectionUp))
        compose.onNodeWithTag("settings-gear").assertIsFocused()
    }

    @Test
    fun `keys before the fade are ignored`() {
        launch()
        advanceTo(100)
        catalogReady()
        controller.heroVisualReady()
        advanceTo(200)
        press(listOf(Key.DirectionDown, Key.DirectionRight, Key.Enter, Key.DirectionDown, Key.Enter))
        assertNothingFocused()
        assertEquals(0, reactions)
        advanceTo(600 + Motion.SPLASH_FADE_MS.toLong())
        frames(8)
        assertSplash(false)
        compose.onNodeWithTag("zone-hero").assertIsFocused()
        assertEquals(0, reactions)
    }

    @Test
    fun `back to the foreground never shows the splash again`() {
        launch()
        advanceTo(100)
        catalogReady()
        controller.heroVisualReady()
        advanceTo(600 + Motion.SPLASH_FADE_MS.toLong())
        frames(8)
        assertSplash(false)
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        frames()
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        repeat(5) {
            frames(1)
            assertSplash(false)
        }
        assertEquals(1, shownCount)
        compose.onNodeWithTag("zone-hero").assertExists()
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `recreated home shows a plain black screen without the splash`() {
        launch(coldStart = false)
        frames(8)
        assertSplash(false)
        compose.onAllNodesWithTag("startup-splash-mascot").assertCountEquals(0)
        assertEquals(0, shownCount)
        assertBlack(true)
        catalogReady()
        frames()
        compose.onNodeWithTag("zone-hero").assertIsFocused()
    }

    @Test
    fun `disabled animations show a still mascot and no fade`() {
        val mascot = FakeMascotDrawable()
        launch(animated = false, mascot = FakeMascotSource(mascot))
        advanceTo(100)
        catalogReady()
        controller.heroVisualReady()
        advanceTo(590)
        compose.onNodeWithTag("startup-splash-mascot").assertExists()
        assertFalse(mascot.isRunning)
        assertEquals(0, mascot.starts)
        assertSplash(true)
        advanceTo(600)
        assertSplash(false)
        frames(1)
        frames()
        compose.onNodeWithTag("zone-hero").assertIsFocused()
    }

    @Test
    fun `back during the exit fade leaves the app without a home action`() {
        launch()
        advanceTo(100)
        catalogReady()
        controller.heroVisualReady()
        advanceTo(600)
        frames()
        assertSplash(true)
        compose.onNodeWithTag("zone-hero").assertIsFocused()
        press(listOf(Key.DirectionUp))
        compose.onNodeWithTag("settings-gear").assertIsFocused()
        pressBackThroughActivity()
        frames()
        assertTrue(isActivityFinishing())
        assertEquals(0, reactions)
    }

    @Test
    fun `the same fade without back keeps the home in the foreground`() {
        launch()
        advanceTo(100)
        catalogReady()
        controller.heroVisualReady()
        advanceTo(600)
        frames()
        assertSplash(true)
        compose.onNodeWithTag("zone-hero").assertIsFocused()
        press(listOf(Key.DirectionUp))
        compose.onNodeWithTag("settings-gear").assertIsFocused()
        assertFalse(isActivityFinishing())
        assertEquals(Lifecycle.State.RESUMED, compose.activityRule.scenario.state)
    }

    @Test
    fun `back during the splash without animation leaves the app`() {
        launch(animated = false)
        advanceTo(100)
        catalogReady()
        controller.heroVisualReady()
        advanceTo(300)
        assertSplash(true)
        pressBackThroughActivity()
        frames()
        assertTrue(isActivityFinishing())
        assertEquals(0, reactions)
    }

    @Test
    fun `back after the instant replacement returns to the hero`() {
        launch(animated = false)
        advanceTo(100)
        catalogReady()
        controller.heroVisualReady()
        advanceTo(600)
        frames()
        assertSplash(false)
        compose.onNodeWithTag("zone-hero").assertIsFocused()
        press(listOf(Key.DirectionUp))
        compose.onNodeWithTag("settings-gear").assertIsFocused()
        pressBackThroughActivity()
        frames()
        compose.onNodeWithTag("zone-hero").assertIsFocused()
        assertFalse(isActivityFinishing())
        assertEquals(Lifecycle.State.RESUMED, compose.activityRule.scenario.state)
    }

    @Test
    fun `unreadable animation keeps the plain splash and the same timing`() {
        launch(mascot = FakeMascotSource(null))
        frames(10)
        assertSplash(true)
        compose.onAllNodesWithTag("startup-splash-mascot").assertCountEquals(0)
        advanceTo(100)
        catalogReady()
        controller.heroVisualReady()
        advanceTo(590)
        assertSplash(true)
        advanceTo(600 + Motion.SPLASH_FADE_MS.toLong())
        frames(8)
        assertSplash(false)
        compose.onNodeWithTag("zone-hero").assertIsFocused()
    }
}
