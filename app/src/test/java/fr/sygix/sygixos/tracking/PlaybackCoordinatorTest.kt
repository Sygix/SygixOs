/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.*
import org.junit.Test

class PlaybackCoordinatorTest {
    private val content = TrackingContent(TrackingIdentity("fixture", TrackingType.EPISODE, "5"), TrackingIdentity("fixture", TrackingType.SHOW, "1"), "Fixture", season = 2, episode = 5)

    @Test fun firstChoiceIsRequiredForSingleAppAndPreferenceIsSharedAcrossSurfaces() = runTest {
        val controller = controller(backgroundScope)
        controller.restore(); runCurrent()
        var saved: String? = null
        var launches = 0
        val coordinator = PlaybackCoordinator(object : PlaybackAppResolver {
            override suspend fun installed() = listOf(PlaybackApp("fixture.player", "Player", true))
        }, object : PlaybackLauncher {
            override fun launch(app: PlaybackApp, request: PlaybackRequest): PlaybackPrecision { ++launches; return PlaybackPrecision.APPLICATION }
        }, object : PlaybackPreferences {
            override suspend fun preferred(content: TrackingIdentity) = saved
            override suspend fun remember(content: TrackingIdentity, packageName: String) { saved = packageName }
        }, controller, TrackingActions(controller, { _, _, _ -> fail("Opening must not mark watched") }, backgroundScope), backgroundScope)
        coordinator.open(PlaybackRequest(content, PlaybackOrigin.HERO)); runCurrent()
        assertEquals(0, launches)
        assertNotNull(coordinator.selection.value)
        coordinator.choose("fixture.player"); runCurrent()
        assertEquals(1, launches)
        coordinator.open(PlaybackRequest(content.copy(identity = content.identity.copy(id = "6")), PlaybackOrigin.UP_NEXT)); runCurrent()
        assertEquals(2, launches)
        assertNull(coordinator.selection.value)
    }

    @Test fun failedLaunchDoesNotPersistPreferenceOrCreateReturnPrompt() = runTest {
        val controller = controller(backgroundScope)
        controller.restore(); runCurrent()
        var saved = false
        val actions = TrackingActions(controller, { _, _, _ -> fail() }, backgroundScope)
        val coordinator = PlaybackCoordinator(object : PlaybackAppResolver {
            override suspend fun installed() = listOf(PlaybackApp("fixture.player", "Player", true))
        }, object : PlaybackLauncher {
            override fun launch(app: PlaybackApp, request: PlaybackRequest): PlaybackPrecision? = null
        }, object : PlaybackPreferences {
            override suspend fun preferred(content: TrackingIdentity): String? = null
            override suspend fun remember(content: TrackingIdentity, packageName: String) { saved = true }
        }, controller, actions, backgroundScope)
        coordinator.open(PlaybackRequest(content, PlaybackOrigin.UP_NEXT)); runCurrent()
        coordinator.choose("fixture.player"); runCurrent()
        coordinator.onBackground(); coordinator.onForeground()
        assertFalse(saved)
        assertNull(actions.notice.value)
        assertNotNull(coordinator.selection.value?.failure)
    }

    private fun controller(scope: kotlinx.coroutines.CoroutineScope) = TrackingController(object : WatchTrackingProvider {
        override suspend fun account(token: String) = TrackingAccount("fixture", "Fixture")
        override suspend fun snapshot(token: String) = TrackingSnapshot(listOf(content))
        override suspend fun revoke(token: String) {}
    }, object : TrackingSessionStore {
        override suspend fun read() = "fixture-token"
        override suspend fun write(token: String?) {}
    }, scope)

    @Test fun installedAppChangesUpdateAnOpenPickerWithoutLaunchingOrRememberingGoneApp() = runTest {
        val controller = controller(backgroundScope)
        controller.restore(); runCurrent()
        val changes = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        var installed = listOf(PlaybackApp("fixture.a", "Alpha", true), PlaybackApp("fixture.b", "Beta", true), PlaybackApp("fixture.c", "Gamma", true))
        var launches = 0
        var remembered = 0
        val coordinator = PlaybackCoordinator(object : PlaybackAppResolver {
            override suspend fun installed() = installed
            override fun changes() = changes
        }, object : PlaybackLauncher {
            override fun launch(app: PlaybackApp, request: PlaybackRequest): PlaybackPrecision { ++launches; return PlaybackPrecision.APPLICATION }
        }, object : PlaybackPreferences {
            override suspend fun preferred(content: TrackingIdentity) = "fixture.b"
            override suspend fun remember(content: TrackingIdentity, packageName: String) { ++remembered }
        }, controller, TrackingActions(controller, { _, _, _ -> fail() }, backgroundScope), backgroundScope)
        coordinator.open(PlaybackRequest(content, PlaybackOrigin.UP_NEXT), choose = true); runCurrent()
        installed = installed.filterNot { it.packageName == "fixture.b" }
        changes.emit(Unit); runCurrent()
        assertEquals(listOf("fixture.a", "fixture.c"), coordinator.selection.value?.apps?.map { it.packageName })
        assertTrue(coordinator.selection.value?.missingPreferred == true)
        coordinator.choose("fixture.b"); runCurrent()
        assertEquals(0, launches)
        assertEquals(0, remembered)
    }
}
