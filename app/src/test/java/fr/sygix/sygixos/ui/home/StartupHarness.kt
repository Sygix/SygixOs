/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
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

internal class SlowMotion(override val scaleFactor: Float) : MotionDurationScale

@OptIn(ExperimentalTestApi::class)
abstract class StartupHostTest(motionScale: Float) {

    protected val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>(dispatcher + SlowMotion(motionScale))

    protected val timings = StartupTimings(
        minMs = Motion.SPLASH_MIN_MS,
        visualCapMs = Motion.SPLASH_VISUAL_CAP_MS,
        capMs = Motion.SPLASH_CAP_MS,
    )
    protected val ready = HomeState.Ready(
        catalogOf(dock = listOf(app("com.dock", "Dock")), grid = listOf(app("com.grid", "Grille"))),
        heroStateOf(items = listOf(heroItem("h1", "Programme"))),
    )
    protected val homeState = mutableStateOf<HomeState>(HomeState.Loading)
    protected lateinit var controller: StartupController
    protected var shownAt: Long? = null
    protected var mascotAt: Long? = null
    protected var unavailable = 0
    protected val windowFocused = mutableStateOf(true)
    protected var shownCount = 0
    protected var reactions = 0

    protected fun launch(
        coldStart: Boolean = true,
        animated: Boolean = true,
        mascot: MascotAnimationSource = FakeMascotSource(),
        solidHome: Color? = null,
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
                        onFadeFinished = controller::fadeFinished,
                        onMascotShown = {
                            mascotAt = scheduler.currentTime
                            controller.mascotShown()
                        },
                        onMascotUnavailable = {
                            unavailable++
                            controller.mascotUnavailable()
                        },
                        windowFocused = windowFocused.value,
                    ) { s, interactive, homeHandlesBack ->
                        if (solidHome != null) {
                            Box(Modifier.fillMaxSize().background(solidHome))
                        } else {
                            LauncherHome(
                                catalog = s.catalog,
                                hero = s.hero,
                                onTogglePin = { reactions++ },
                                onOpenApp = { reactions++ },
                                onOpenHero = { reactions++ },
                                glassBlur = false,
                                interactive = interactive,
                                homeHandlesBack = homeHandlesBack,
                                onHeroVisualReady = controller::heroVisualReady,
                            )
                        }
                    }
                }
            }
        }
        repeat(10) { if (shownAt == null && coldStart) frames(1) }
        repeat(10) { if (mascotAt == null && unavailable == 0 && coldStart) frames(1) }
        frames(1)
    }

    protected fun frames(count: Int = 3) = repeat(count) { compose.mainClock.advanceTimeByFrame() }

    protected fun advanceTo(elapsed: Long) {
        val delta = checkNotNull(mascotAt ?: shownAt) + elapsed - scheduler.currentTime
        if (delta > 0) compose.mainClock.advanceTimeBy(delta, ignoreFrameDuration = true)
        frames(1)
    }

    protected fun catalogReady() {
        homeState.value = ready
        Snapshot.sendApplyNotifications()
        controller.catalogReady()
        frames(1)
    }

    protected fun press(keys: List<Key>) {
        keys.forEach { key ->
            compose.onRoot().performKeyInput {
                keyDown(key)
                keyUp(key)
            }
            frames(1)
        }
    }

    protected fun pressBackThroughActivity() {
        compose.activityRule.scenario.onActivity { activity ->
            val now = android.os.SystemClock.uptimeMillis()
            activity.dispatchKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK, 0))
            activity.dispatchKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK, 0))
        }
        frames(1)
    }

    protected fun isActivityFinishing(): Boolean {
        var finishing = false
        compose.activityRule.scenario.onActivity { finishing = it.isFinishing }
        return finishing
    }

    protected fun assertNothingFocused() = compose.onAllNodes(isFocused()).assertCountEquals(0)

    protected fun assertSplash(shown: Boolean) {
        compose.onAllNodesWithTag("startup-splash").assertCountEquals(if (shown) 1 else 0)
    }

    protected fun pixels(): IntArray = compose.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
        IntArray(bitmap.width * bitmap.height).also { bitmap.getPixels(it, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height) }
    }

    protected fun assertBlack(black: Boolean) {
        val blackArgb = Color.Black.toArgb()
        assertEquals(black, pixels().all { it == blackArgb })
    }
}
