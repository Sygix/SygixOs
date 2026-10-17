/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.sygix.sygixos.core.designsystem.tryRequestFocus
import fr.sygix.sygixos.model.TvApp
import fr.sygix.sygixos.ui.home.WithTestArtwork
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class SettingsOffscreenFocusTest {
    @get:Rule val compose = createComposeRule()

    @Test fun hiddenRefocusRevealsAnUncomposedTargetBeforeRequestingFocus() {
        val entries = (0 until 100).map { HiddenEntry.App(HiddenRow(TvApp("com.fixture.app$it", "Application $it"), true, null)) }
        val request = mutableStateOf<HiddenRefocus?>(null)
        compose.setContent {
            WithTestArtwork {
                MaterialTheme {
                    HiddenContent(
                        entries = entries, listState = rememberLazyListState(), focusEnabled = true,
                        contentFocus = remember { FocusRequester() }, onToggle = {}, onUnhideAll = {}, refocus = request.value,
                    )
                }
            }
        }
        compose.onNodeWithTag("hidden-row-com.fixture.app0").assertIsDisplayed()
        compose.onNodeWithTag("hidden-row-com.fixture.app99").assertDoesNotExist()
        compose.runOnIdle { request.value = HiddenRefocus(entries.last().key, 1) }
        compose.waitForIdle()
        compose.onNodeWithTag("hidden-row-com.fixture.app99").assertIsDisplayed().assertIsFocused()
    }

    @Test fun reorderingSourcesKeepsTheFocusedPackageWhenItMovesOutsideTheWindow() {
        val original = (0 until 100).map { SourceRow(TvApp("com.fixture.app$it", "Application $it"), true) }
        val rows = mutableStateOf(original)
        compose.setContent {
            val contentFocus = remember { FocusRequester() }
            WithTestArtwork {
                MaterialTheme {
                    SourcesContent(
                        rows = rows.value, counts = remember { mutableStateOf(emptyMap<String, Int>()) },
                        listState = rememberLazyListState(), focusEnabled = true, contentFocus = contentFocus, onToggle = {},
                    )
                }
            }
            LaunchedEffect(Unit) { withFrameNanos { }; contentFocus.tryRequestFocus() }
        }
        compose.onNodeWithTag("source-row-com.fixture.app0").assertIsFocused()
        compose.runOnIdle { rows.value = original.drop(1) + original.first() }
        compose.waitForIdle()
        compose.onNodeWithTag("source-row-com.fixture.app0").assertIsDisplayed().assertIsFocused()
    }

    @Test fun returningFromASystemScreenRevealsTheUpdateBlockAboveTheLicenseWindow() {
        val update = mutableStateOf(AboutUpdateState())
        lateinit var list: LazyListState
        compose.setContent {
            list = rememberLazyListState()
            MaterialTheme {
                AboutContent(
                    version = "0.0.1", update = update.value, actions = UpdateActions(), listState = list,
                    focusEnabled = true, contentFocus = remember { FocusRequester() },
                )
            }
        }
        val license = SemanticsMatcher("license row") {
            it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("license-row-") == true
        }
        compose.waitUntil(5_000) { compose.onAllNodes(license).fetchSemanticsNodes().isNotEmpty() }
        var lastIndex = 0
        compose.runOnIdle { lastIndex = list.layoutInfo.totalItemsCount - 1 }
        compose.onNodeWithTag("settings-about").performScrollToIndex(lastIndex)
        compose.onNodeWithTag("update-check").assertDoesNotExist()
        compose.runOnIdle { update.value = update.value.copy(systemScreenReturns = 1) }
        compose.waitForIdle()
        compose.onNodeWithTag("update-check").assertIsDisplayed().assertIsFocused()
    }
}
