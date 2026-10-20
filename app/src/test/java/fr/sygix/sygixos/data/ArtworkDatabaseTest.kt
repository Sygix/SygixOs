/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ArtworkDatabaseTest {
    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext
    @get:Rule val folder = org.junit.rules.TemporaryFolder()
    @get:Rule val schemas = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), ArtworkDatabase::class.java)

    @Test fun indexKeepsMoreThan64AssociationsAndPurgesExpiredPrograms() = runTest {
        val db = Room.inMemoryDatabaseBuilder(context, ArtworkDatabase::class.java).build()
        try {
            val dao = db.artwork()
            dao.image(ArtworkImage("image", "app", "content://test", "content://test", 1280, 720, "hash", 100, "valid"))
            repeat(100) { n ->
                dao.program(ArtworkProgram("preview", "$n", "app", "identity$n", "full$n", null, 100, 200))
                dao.associate(ArtworkAssociation("image", "identity$n", null, 100))
            }
            assertEquals(100, dao.associations("image").size)
            dao.expirePrograms(200)
            dao.removeOrphanAssociations()
            assertTrue(dao.programs("preview").isEmpty())
            assertTrue(dao.associations("image").isEmpty())
        } finally { db.close() }
    }

    @Test fun initialSchemaKeepsProgramsCandidatesImageLinksAndDatesAfterReopening() = runTest {
        schemas.createDatabase("initial", 1).use { db ->
            db.execSQL("INSERT INTO tv_programs VALUES ('preview','1','app','identity','full','series',100,200)")
            db.execSQL("INSERT INTO program_image_candidates VALUES ('preview','1',0,'content://fixture')")
            db.execSQL("INSERT INTO program_images VALUES ('image','app','content://fixture','content://fixture',1280,720,'hash',100,'valid',170,NULL)")
            db.execSQL("INSERT INTO program_image_links VALUES ('image','identity','series',120)")
        }
        val db = ArtworkDatabase.create(context, "initial")
        try {
            val dao = db.artwork()
            assertEquals("identity", dao.programs("preview").single().imageFingerprint)
            assertEquals("content://fixture", dao.candidates("preview", "1").single().uri)
            val image = dao.images().single()
            assertEquals(listOf(1280L, 720L, 100L, 170L), listOf(image.width.toLong(), image.height.toLong(), image.fetchedAt, image.retryAt))
            assertEquals(120L, dao.associations("image").single().confirmedAt)
            db.openHelper.writableDatabase.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
        } finally { db.close() }
    }

    @Test fun unavailableMigrationRecreatesTheEntireDisposableIndex() = runTest {
        schemas.createDatabase("missing", 1).use { db ->
            db.execSQL("INSERT INTO program_images VALUES ('image','app','content://fixture','content://fixture',1280,720,'hash',100,'valid',0,NULL)")
            db.execSQL("CREATE TABLE unknown_index_state (value TEXT)")
            db.version = 2
        }
        val db = ArtworkDatabase.create(context, "missing")
        try {
            assertTrue(db.artwork().images().isEmpty())
            db.openHelper.writableDatabase.query("SELECT name FROM sqlite_master WHERE name='unknown_index_state'").use { assertFalse(it.moveToFirst()) }
        } finally { db.close() }
    }

    @Test fun unreadableIndexIsRecreatedBeforeTheNextProviderConfirmation() = runTest {
        schemas.createDatabase("broken", 1).use { it.execSQL("DROP TABLE program_images") }
        val disk = coil.disk.DiskCache.Builder().directory(folder.newFolder()).build()
        ArtworkRepository(context, ArtworkDatabase.create(context, "broken"), disk, ImageBytesReader { _, _ -> artworkBytes() }).use { repository ->
            repository.confirm("preview", emptyList())
            assertNotNull(repository.resolve(fr.sygix.sygixos.model.ProgramArtwork("app", listOf("content://fixture/image"), "fiction"), fr.sygix.sygixos.domain.ArtworkPolicy.Hero).first)
        }
    }
}
