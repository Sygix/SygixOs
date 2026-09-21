package fr.sygix.sygixos.domain

import fr.sygix.sygixos.model.TvApp
import org.junit.Assert.assertEquals
import org.junit.Test

class AppCatalogTest {

    private val apps = listOf(
        TvApp("com.netflix", "Netflix"),
        TvApp("com.zeta", "zeta"),
        TvApp("com.alpha", "Alpha"),
    )

    @Test
    fun `grid is ordered alphabetically and excludes pinned apps`() {
        val grid = AppCatalog.grid(apps, listOf("com.netflix"))
        assertEquals(listOf("com.alpha", "com.zeta"), grid.map { it.packageName })
    }

    @Test
    fun `dock keeps pin order`() {
        val dock = AppCatalog.dock(apps, listOf("com.zeta", "com.netflix"))
        assertEquals(listOf("com.zeta", "com.netflix"), dock.map { it.packageName })
    }

    @Test
    fun `dock ignores unknown packages`() {
        val dock = AppCatalog.dock(apps, listOf("com.ghost", "com.netflix"))
        assertEquals(listOf("com.netflix"), dock.map { it.packageName })
    }

    @Test
    fun `togglePin adds and removes`() {
        assertEquals(listOf("a"), AppCatalog.togglePinned(emptyList(), "a"))
        assertEquals(emptyList<String>(), AppCatalog.togglePinned(listOf("a"), "a"))
    }
}
