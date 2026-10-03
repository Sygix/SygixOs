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

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class StartupSplashTest {

    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>(dispatcher)

    private val timings = StartupTimings(
        minMs = Motion.SPLASH_MIN_MS,
        visualCapMs = Motion.SPLASH_VISUAL_CAP_MS,
        capMs = Motion.SPLASH_CAP_MS,
        fadeMs = Motion.SPLASH_FADE_MS.toLong(),
    )
    private val ready = HomeState.Ready(
        catalogOf(dock = listOf(app("com.dock", "Dock")), grid = listOf(app("com.grid", "Grille"))),
        heroStateOf(items = listOf(heroItem("h1", "Programme"))),
    )
    private val homeState = mutableStateOf<HomeState>(HomeState.Loading)
    private lateinit var controller: StartupController
    private var shownAt: Long? = null
    private var shownCount = 0
    private var reactions = 0

    private fun launch(
        coldStart: Boolean = true,
        animated: Boolean = true,
        mascot: MascotAnimationSource = FakeMascotSource(),
    ) {
        controller = StartupController(
            coldStart = coldStart,
            gate = StartupGate(timings, animated) { scheduler.currentTime },
            scope = CoroutineScope(dispatcher),
            dispatcher = dispatcher,
        )
        compose.mainClock.autoAdvance = false
        compose.setContent {
            val phase by controller.phase.collectAsState()
            SygixOsTheme {
                CompositionLocalProvider(LocalAppArtwork provides AppArtworkSource(LocalContext.current.packageManager)) {
                    StartupHost(
                        state = homeState.value,
                        phase = phase,
                        animated = animated,
                        mascot = mascot,
                        onSplashShown = {
                            shownAt = scheduler.currentTime
                            shownCount++
                            controller.splashShown()
                        },
                    ) { s, interactive ->
                        LauncherHome(
                            catalog = s.catalog,
                            hero = s.hero,
                            onTogglePin = { reactions++ },
                            onOpenApp = { reactions++ },
                            onOpenHero = { reactions++ },
                            glassBlur = false,
                            interactive = interactive,
                            onHeroVisualReady = controller::heroVisualReady,
                        )
                    }
                }
            }
        }
        repeat(10) { if (shownAt == null && coldStart) frames(1) }
        frames(1)
    }

    private fun frames(count: Int = 3) = repeat(count) { compose.mainClock.advanceTimeByFrame() }

    private fun advanceTo(elapsed: Long) {
        val delta = checkNotNull(shownAt) + elapsed - scheduler.currentTime
        if (delta > 0) compose.mainClock.advanceTimeBy(delta, ignoreFrameDuration = true)
        frames(1)
    }

    private fun catalogReady() {
        homeState.value = ready
        Snapshot.sendApplyNotifications()
        controller.catalogReady()
        frames(1)
    }

    private fun press(keys: List<Key>) {
        keys.forEach { key ->
            compose.onRoot().performKeyInput {
                keyDown(key)
                keyUp(key)
            }
            frames(1)
        }
    }

    private fun homeAlpha(): Float =
        compose.onNode(SemanticsMatcher.keyIsDefined(StartupHomeAlpha)).fetchSemanticsNode().config[StartupHomeAlpha]

    private fun assertNothingFocused() = compose.onAllNodes(isFocused()).assertCountEquals(0)

    private fun assertSplash(shown: Boolean) {
        compose.onAllNodesWithTag("startup-splash").assertCountEquals(if (shown) 1 else 0)
    }

    private fun assertBlack(black: Boolean) {
        val pixels = compose.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
            IntArray(bitmap.width * bitmap.height).also { bitmap.getPixels(it, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height) }
        }
        val blackArgb = Color.Black.toArgb()
        assertEquals(black, pixels.all { it == blackArgb })
    }

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
        frames()
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
        frames()
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
        frames()
        assertSplash(false)
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `never loaded catalog reveals the black loading screen at 5 s`() {
        launch(mascot = FakeMascotSource(null))
        advanceTo(4_990)
        assertSplash(true)
        advanceTo(5_000 + Motion.SPLASH_FADE_MS.toLong())
        frames()
        assertSplash(false)
        compose.onAllNodesWithTag("zone-hero").assertCountEquals(0)
        assertBlack(true)
        catalogReady()
        frames()
        compose.onNodeWithTag("zone-hero").assertIsFocused()
    }

    @Test
    fun `home is composed but transparent and unfocusable before the fade`() {
        launch()
        advanceTo(100)
        catalogReady()
        controller.heroVisualReady()
        advanceTo(300)
        compose.onNodeWithTag("zone-hero").assertExists()
        compose.onNodeWithTag("settings-gear").assertExists()
        assertEquals(0f, homeAlpha())
        compose.onNodeWithTag("zone-hero").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithTag("settings-gear").performSemanticsAction(SemanticsActions.RequestFocus)
        frames()
        compose.onNodeWithTag("zone-hero").assertIsNotFocused()
        assertNothingFocused()
        advanceTo(590)
        assertEquals(0f, homeAlpha())
        advanceTo(800)
        assertTrue(homeAlpha() in 0.01f..0.99f)
        advanceTo(600 + Motion.SPLASH_FADE_MS.toLong())
        frames()
        assertEquals(1f, homeAlpha())
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
        frames()
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
        frames()
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
        frames()
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
        assertEquals(0f, homeAlpha())
        advanceTo(600)
        assertSplash(false)
        frames(1)
        assertEquals(1f, homeAlpha())
        frames()
        compose.onNodeWithTag("zone-hero").assertIsFocused()
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
        frames()
        assertSplash(false)
        compose.onNodeWithTag("zone-hero").assertIsFocused()
    }
}
