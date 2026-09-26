/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class HeroFocusClaimTest {

    @get:Rule
    val compose = createComposeRule()

    private fun press(key: Key) {
        compose.onRoot().performKeyInput {
            keyDown(key)
            keyUp(key)
        }
        compose.waitForIdle()
    }

    private val plain = heroItem("h1", "Programme")
    private val launchable = heroItem("h1", "Programme", sourcePackage = "com.src")

    @Test
    fun `hero becoming launchable does not steal the focus from the gear`() {
        val hero = mutableStateOf(heroStateOf(items = listOf(plain)))
        compose.setContent {
            TestHome(catalog = catalogOf(dock = emptyList(), grid = emptyList()), hero = hero.value)
        }
        compose.waitForIdle()
        compose.onNodeWithTag("zone-hero").assertIsFocused()
        press(Key.DirectionUp)
        compose.onNodeWithTag("settings-gear").assertIsFocused()

        hero.value = heroStateOf(items = listOf(launchable))
        compose.waitForIdle()
        compose.onNodeWithTag("hero-open").assertExists()
        compose.onNodeWithTag("settings-gear").assertIsFocused()
        press(Key.Enter)
        compose.onNodeWithTag("settings-screen").assertExists()
    }

    @Test
    fun `hero becoming launchable while it owns the focus moves the focus to its open button`() {
        val hero = mutableStateOf(heroStateOf(items = listOf(plain)))
        compose.setContent {
            TestHome(catalog = catalogOf(dock = emptyList(), grid = emptyList()), hero = hero.value)
        }
        compose.waitForIdle()
        compose.onNodeWithTag("zone-hero").assertIsFocused()
        hero.value = heroStateOf(items = listOf(launchable))
        compose.waitForIdle()
        compose.onNodeWithTag("hero-open").assertIsFocused()
        press(Key.DirectionUp)
        compose.onNodeWithTag("settings-gear").assertIsFocused()
        press(Key.DirectionDown)
        compose.onNodeWithTag("hero-open").assertIsFocused()
    }
}
