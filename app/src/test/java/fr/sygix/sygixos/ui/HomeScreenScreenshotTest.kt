package fr.sygix.sygixos.ui

import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import fr.sygix.sygixos.core.designsystem.SygixOsTheme
import fr.sygix.sygixos.data.Catalog
import fr.sygix.sygixos.model.HeroItem
import fr.sygix.sygixos.model.TvApp
import fr.sygix.sygixos.ui.home.HeroState
import fr.sygix.sygixos.ui.home.HomeGrid
import fr.sygix.sygixos.ui.home.LauncherHome
import fr.sygix.sygixos.ui.home.Zone
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Canevas TV réel : 1080p en xhdpi = 960 x 540 dp. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-xhdpi")
class HomeScreenScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val apps = listOf(
        "Netflix", "Prime Video", "Disney+", "Orange TV", "YouTube", "Canal+", "Jellyfin",
        "Spotify", "Steam Link", "Kodi", "Plex", "Twitch", "Apple TV", "Molotov",
    ).map { TvApp(packageName = "test.$it", label = it) }

    private val catalog = Catalog(dock = apps.take(4), grid = apps)

    private val programs = listOf(
        HeroItem(
            id = "watchnext-1",
            title = "Film X — reprise",
            progress = 0.3f,
            sourcePackage = "test.Jellyfin",
            sourceLabel = "Jellyfin",
            engagement = 500L,
        ),
        HeroItem(id = "watchnext-2", title = "Série Y — épisode 4", progress = 0.75f, sourcePackage = "test.Netflix", sourceLabel = "Netflix"),
        HeroItem(id = "preview-3", title = "Nouveauté Z", sourcePackage = "test.YouTube", sourceLabel = "YouTube", imageUrl = "https://example.com/z.jpg"),
    )

    @Test
    fun heroWithPrograms() {
        composeRule.setContent {
            SygixOsTheme {
                LauncherHome(catalog, hero = HeroState(programs, fromApps = true, loading = false, validated = setOf("https://example.com/z.jpg")), onTogglePin = {}, glassBlur = false)
            }
        }
        composeRule.onRoot().captureRoboImage("build/outputs/screenshots/hero-programs.png")
    }

    @Test
    fun grid() {
        composeRule.setContent {
            SygixOsTheme {
                LauncherHome(
                    catalog,
                    hero = HeroState(emptyList(), fromApps = false, loading = false),
                    onTogglePin = {},
                    initialZone = Zone.GRID,
                    glassBlur = false,
                )
            }
        }
        composeRule.onRoot().captureRoboImage("build/outputs/screenshots/home-grid.png")
    }

    @Test
    fun gridWithShelfPanel() {
        composeRule.setContent {
            SygixOsTheme {
                HomeGrid(
                    catalog = catalog,
                    alpha = 1f,
                    focusEnabled = true,
                    focusRequester = FocusRequester(),
                    shelfPrograms = programs,
                    validatedVisuals = setOf("https://example.com/z.jpg"),
                    onAppFocused = {},
                    onTileFocus = {},
                    onTileClick = {},
                    onTileLongClick = {},
                    initialShelfApp = "test.YouTube",
                )
            }
        }
        composeRule.onRoot().captureRoboImage("build/outputs/screenshots/grid-shelf-panel.png")
    }
}
