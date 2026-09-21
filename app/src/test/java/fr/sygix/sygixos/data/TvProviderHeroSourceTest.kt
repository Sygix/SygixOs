package fr.sygix.sygixos.data

import android.content.Context
import android.database.MatrixCursor
import android.media.tv.TvContract
import androidx.test.core.app.ApplicationProvider
import fr.sygix.sygixos.model.HeroItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TvProviderHeroSourceTest {

    private val source = TvProviderHeroSource(ApplicationProvider.getApplicationContext<Context>())

    private fun watchNextCursor(vararg rows: Array<Any?>): MatrixCursor {
        val c = MatrixCursor(
            arrayOf(
                TvContract.WatchNextPrograms._ID,
                TvContract.WatchNextPrograms.COLUMN_PACKAGE_NAME,
                TvContract.WatchNextPrograms.COLUMN_TITLE,
                TvContract.WatchNextPrograms.COLUMN_POSTER_ART_URI,
                TvContract.WatchNextPrograms.COLUMN_THUMBNAIL_URI,
                TvContract.WatchNextPrograms.COLUMN_INTENT_URI,
                TvContract.WatchNextPrograms.COLUMN_LAST_PLAYBACK_POSITION_MILLIS,
                TvContract.WatchNextPrograms.COLUMN_DURATION_MILLIS,
                TvContract.WatchNextPrograms.COLUMN_LAST_ENGAGEMENT_TIME_UTC_MILLIS,
            ),
        )
        rows.forEach { c.addRow(it) }
        return c
    }

    @Test
    fun `watch next row maps to hero item with progress and intent`() {
        val c = watchNextCursor(
            arrayOf(1L, "org.jellyfin.mobiletv", "Film X", "poster://x", null, "intent://detail/1", 30L, 100L, 500L),
        )
        c.moveToFirst()
        val item = source.mapWatchNextRow(c)
        assertEquals("watchnext-1", item?.id)
        assertEquals("Film X", item?.title)
        assertEquals("poster://x", item?.imageUrl)
        assertEquals("org.jellyfin.mobiletv", item?.sourcePackage)
        assertEquals(0.3f, item?.progress)
        assertEquals("intent://detail/1", item?.launchUri)
        assertEquals(500L, item?.engagement)
    }

    @Test
    fun `watch next row falls back to thumbnail when no poster`() {
        val c = watchNextCursor(
            arrayOf(2L, "app.pkg", "Série Y", null, "thumb://y", "intent://detail/2", 0L, 0L, 1L),
        )
        c.moveToFirst()
        val item = source.mapWatchNextRow(c)
        assertEquals("thumb://y", item?.imageUrl)
        assertNull(item?.progress)
    }

    @Test
    fun `watch next row without poster and intent is skipped`() {
        val c = watchNextCursor(
            arrayOf(3L, "app.pkg", "Vide", null, null, null, 0L, 0L, 1L),
        )
        c.moveToFirst()
        assertNull(source.mapWatchNextRow(c))
    }

    @Test
    fun `progress ratio guards against zero duration and completion`() {
        assertNull(HeroOrdering.progressRatio(0L, 0L))
        assertNull(HeroOrdering.progressRatio(100L, 100L))
        assertNull(HeroOrdering.progressRatio(10L, 0L))
        assertEquals(0.5f, HeroOrdering.progressRatio(50L, 100L))
    }

    @Test
    fun `ordering puts in-progress items first then by most recent engagement`() {
        val items = listOf(
            HeroItem("a", "récent", engagement = 900L),
            HeroItem("b", "en cours", progress = 0.4f, engagement = 100L),
            HeroItem("c", "ancien", engagement = 300L),
        )
        val sorted = HeroOrdering.sort(items)
        assertEquals(listOf("b", "a", "c"), sorted.map { it.id })
        assertTrue(sorted.first().progress != null)
    }
}
