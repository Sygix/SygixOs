/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import fr.sygix.sygixos.model.UpNextContentType
import fr.sygix.sygixos.model.UpNextItem
import fr.sygix.sygixos.model.UpNextSourceEntry
import java.io.File
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class UpNextArtworkRenderTest {
    @get:Rule val compose = createComposeRule()
    private val files = mutableListOf<File>()
    private val item = UpNextItem(1, UpNextSourceEntry("com.source"), UpNextContentType.MOVIE, "Movie")
    private val placeholder = "upnext-card-placeholder-${item.key}"

    @After fun clearFiles() { files.forEach { it.delete() } }

    private fun image(width: Int, height: Int): String {
        val context: Context = ApplicationProvider.getApplicationContext()
        val file = File.createTempFile("artwork", ".png", context.cacheDir).also { files += it }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return file.toURI().toString()
    }

    private fun render(uri: String?) {
        compose.setContent { UpNextArtwork(item.copy(imageUrl = uri), Modifier.size(198.dp, 112.dp)) }
    }

    @Test fun `missing artwork keeps placeholder`() {
        render(null)
        compose.onNodeWithTag(placeholder).assertExists()
    }

    @Test fun `landscape at twice displayed width replaces placeholder`() {
        render(image(792, 448))
        compose.waitUntil(5_000) { compose.onAllNodesWithTag(placeholder).fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithTag(placeholder).assertDoesNotExist()
    }

    @Test fun `portrait retains sufficient width and replaces placeholder`() {
        render(image(792, 1600))
        compose.waitUntil(5_000) { compose.onAllNodesWithTag(placeholder).fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithTag(placeholder).assertDoesNotExist()
    }

    @Test fun `image narrower than twice the displayed width keeps the placeholder`() {
        val small = item.copy(id = 2, imageUrl = image(600, 338))
        val large = item.copy(id = 3, imageUrl = image(792, 448))
        val showLarge = mutableStateOf(false)
        compose.setContent {
            Column {
                UpNextArtwork(small, Modifier.size(198.dp, 112.dp))
                if (showLarge.value) UpNextArtwork(large, Modifier.size(198.dp, 112.dp))
            }
        }
        compose.waitForIdle()
        compose.runOnIdle { showLarge.value = true }
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("upnext-card-placeholder-${large.key}").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithTag("upnext-card-placeholder-${small.key}").assertExists()
    }
}
