/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import androidx.compose.runtime.*
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.sygix.sygixos.domain.UpNextContent
import fr.sygix.sygixos.domain.UpNextState
import fr.sygix.sygixos.model.UpNextPosition
import fr.sygix.sygixos.tracking.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "fr-w960dp-h540dp-xhdpi")
class TrackingOfflineHomeTest {
    @get:Rule val compose = createComposeRule()
    private val movie = TrackingContent(TrackingIdentity("betaseries", TrackingType.MOVIE, "42"), title = "Fixture movie")

    @Test fun durableMovieActionRemovesTheRealHomeCardAndUndoRestoresItWithFocusFallback() = runTest {
        val fixture = fixture(this, listOf(movie))
        try {
            show(fixture)
            val card = "upnext-card-${TrackingProjection.card(movie).key}"
            compose.onNodeWithTag(card).assertIsFocused()
            markSeen()
            fixture.runtime.actions.notice.first { it?.locallySaved == true }
            runCurrent(); compose.waitForIdle()
            compose.onNodeWithTag(card).assertDoesNotExist()
            compose.onNodeWithTag("app-tile-fixture.grid").assertIsFocused()
            press(Key.Back); press(Key.DirectionUp); press(Key.DirectionRight)
            compose.onNodeWithTag("tracking-undo").assertIsFocused()
            press(Key.Enter)
            fixture.runtime.controller.state.first { it.pendingCount == 0 }
            runCurrent(); compose.waitForIdle()
            compose.onNodeWithTag(card).assertExists()
            compose.onNodeWithTag("settings-gear").assertIsFocused()
            assertEquals(0, fixture.movieFixture.writes)
        } finally { fixture.close(); runCurrent() }
    }

    @Test fun durableEpisodeActionChangesTheDisplayedEpisodeAndKeepsTheSeriesFocused() = runTest {
        val parent = TrackingIdentity("betaseries", TrackingType.SHOW, "show")
        val episodes = (1..6).map { number -> TrackingContent(TrackingIdentity("betaseries", TrackingType.EPISODE, "episode-$number"),
            parent, "Example", season = 1, episode = number, airDate = "2020-01-01") }
        val fixture = fixture(this, episodes)
        try {
            show(fixture)
            markSeen()
            fixture.runtime.actions.notice.first { it?.locallySaved == true }
            runCurrent(); compose.waitForIdle()
            val next = TrackingProjection.candidates(fixture.runtime.controller.state.value.contents).single()
            assertEquals(2, next.episode)
            compose.onNodeWithTag("upnext-card-${TrackingProjection.card(next).key}").assertIsFocused()
            assertTrue(fixture.runtime.controller.state.value.contents.filter { it.episode != 1 }.none { it.seen })
            assertEquals(1, fixture.runtime.controller.state.value.pendingCount)
            assertEquals(0, fixture.movieFixture.writes)
        } finally { fixture.close(); runCurrent() }
    }

    private fun markSeen() {
        press(Key.Menu)
        compose.onNodeWithTag("tracking-action-player").assertIsFocused()
        press(Key.DirectionDown, "upnext-menu")
        compose.onNodeWithTag("tracking-action-seen").assertIsFocused()
        press(Key.Enter, "upnext-menu")
    }

    private fun show(fixture: Fixture) {
        compose.setContent {
            val state by fixture.viewModel.state.collectAsState()
            val cards = state.tracking.contents.map(TrackingProjection::card)
            val row = cards.takeIf { it.isNotEmpty() }?.let { UpNextState(UpNextContent.Items(it), UpNextPosition.BEFORE_APPS) }
            TestHome(catalogOf(emptyList(), listOf(app("fixture.grid", "Grid"))),
                heroStateOf(state.tracking.contents.map(TrackingProjection::hero)), upNext = row,
                trackingViewModel = fixture.viewModel, trackingUi = state)
        }
        compose.waitForIdle()
        press(Key.DirectionDown)
    }

    private fun press(key: Key, tag: String? = null) {
        val node = if (tag == null) compose.onRoot() else compose.onNodeWithTag(tag)
        node.performKeyInput { keyDown(key); keyUp(key) }
        compose.waitForIdle()
    }

    private suspend fun fixture(test: TestScope, seed: List<TrackingContent>): Fixture {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val database = Room.databaseBuilder(context, TrackingDatabase::class.java,
            context.noBackupFilesDir.resolve("${UUID.randomUUID()}.db").absolutePath).build()
        val repository = TrackingRepository(database) { 1_800_000_000_000L }
        val account = repository.bind("fixture-token", TrackingAccount("1", "Fixture"))
        repository.snapshot(account.incarnation, 0, TrackingSnapshot(seed))
        val scope = CoroutineScope(test.backgroundScope.coroutineContext + UnconfinedTestDispatcher(test.testScheduler))
        val movieFixture = TrackingMovieFixture(scope, repository = repository, scheduler = object : TrackingWorkScheduler {
            override fun enqueue(incarnation: String) = Unit
            override fun cancel(incarnation: String) = Unit
        }) { test.testScheduler.currentTime }
        movieFixture.runtime.controller.restore()
        movieFixture.runtime.controller.state.first { it.account != null && !it.loading }
        test.runCurrent()
        val store = ViewModelStore()
        val viewModel = ViewModelProvider(store, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = TrackingViewModel(movieFixture.runtime) {} as T
        })[TrackingViewModel::class.java]
        return Fixture(movieFixture, viewModel, { store.clear(); scope.cancel(); database.close() })
    }

    private class Fixture(val movieFixture: TrackingMovieFixture, val viewModel: TrackingViewModel, private val cleanup: () -> Unit) : AutoCloseable {
        val runtime get() = movieFixture.runtime
        override fun close() = cleanup()
    }
}
