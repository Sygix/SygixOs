package fr.sygix.sygixos.ui.home

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.sygix.sygixos.core.designsystem.Motion
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeGridTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `grid renders each installed app exactly once`() {
        compose.setContent {
            TestHome(
                catalog = catalogOf(
                    dock = listOf(app("com.dock", "Dock")),
                    grid = listOf(app("com.a", "Alpha"), app("com.b", "Beta")),
                ),
                hero = heroStateOf(items = emptyList()),
            )
        }
        compose.waitForIdle()
        listOf("com.a", "com.b").forEach { pkg ->
            assertEquals(1, compose.onAllNodesWithTag("app-tile-$pkg").fetchSemanticsNodes().size)
        }
    }

    @Test
    fun `empty grid shows no apps detected message`() {
        compose.setContent {
            TestHome(
                catalog = catalogOf(dock = emptyList(), grid = emptyList()),
                hero = heroStateOf(items = emptyList()),
            )
        }
        compose.onNodeWithText("Aucune app TV détectée").assertExists()
    }

    @Test
    fun `focused tile with validated posters opens shelf panel`() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            TestHome(
                catalog = catalogOf(dock = emptyList(), grid = listOf(app("com.a", "Alpha"))),
                hero = heroStateOf(
                    items = listOf(heroItem("h1", "Héros", imageUrl = "uri1", sourcePackage = "com.a")),
                    validated = setOf("uri1"),
                ),
                initialZone = Zone.GRID,
            )
        }
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(Motion.SHELF_OPEN_DELAY_MS + Motion.SHELF_EXPAND_MS)
        compose.waitForIdle()
        compose.onNodeWithTag("shelf-panel").assertExists()
    }

    @Test
    fun `tile without validated posters keeps shelf closed`() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            TestHome(
                catalog = catalogOf(dock = emptyList(), grid = listOf(app("com.a", "Alpha"))),
                hero = heroStateOf(
                    items = listOf(heroItem("h1", "Héros", imageUrl = "uri1", sourcePackage = "com.a")),
                    validated = emptySet(),
                ),
                initialZone = Zone.GRID,
            )
        }
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(Motion.SHELF_OPEN_DELAY_MS + Motion.SHELF_EXPAND_MS)
        compose.waitForIdle()
        compose.onNodeWithTag("shelf-panel").assertDoesNotExist()
    }
}
