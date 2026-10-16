/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.core.designsystem

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class FocusTargetsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun revealingAnUncomposedItemComposesItAndFocusesItsBoundTarget() {
        val keys = (0 until 200).map { "row-$it" }
        val focus = FocusTargets()
        lateinit var list: LazyListState
        lateinit var scope: CoroutineScope
        compose.setContent {
            list = rememberLazyListState()
            scope = rememberCoroutineScope()
            LazyColumn(state = list) {
                items(keys, key = { it }) { key ->
                    Box(Modifier.testTag(key).fillMaxWidth().height(48.dp).focusRequester(focus.bind(key)).focusable())
                }
            }
        }
        compose.onNodeWithTag("row-150").assertDoesNotExist()
        var revealed = false
        compose.runOnIdle { scope.launch { revealed = focus.reveal("row-150") { list.scrollToItem(150) } } }
        compose.waitForIdle()
        compose.onNodeWithTag("row-150").assertIsDisplayed().assertIsFocused()
        compose.onNodeWithTag("row-0").assertDoesNotExist()
        compose.runOnIdle { scope.launch { revealed = revealed && focus.reveal("row-0") { list.scrollToItem(0) } } }
        compose.waitForIdle()
        compose.onNodeWithTag("row-0").assertIsDisplayed().assertIsFocused()
        compose.runOnIdle {
            assertTrue(revealed)
            assertFalse(focus.focus("row-150"))
        }
    }
}
