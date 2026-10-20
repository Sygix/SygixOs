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
    @get:Rule val folder = org.junit.rules.TemporaryFolder()
    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext
    @get:Rule val migrations = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), ArtworkDatabase::class.java)

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

    @Test fun firstMigrationPreservesImageDimensionsDatesAndAssociations() {
        migrations.createDatabase("migration", 1).use { db ->
            db.execSQL("INSERT INTO images VALUES ('image','app','content://test','content://test',1280,720,'hash',100,'valid')")
            db.execSQL("INSERT INTO associations VALUES ('image','identity','series',120)")
        }
        migrations.runMigrationsAndValidate("migration", 3, true, ArtworkDatabase.MIGRATION_1_2, ArtworkDatabase.MIGRATION_2_3).use { db ->
            db.query("SELECT width,height,fetchedAt,retryAt FROM program_images").use {
                assertTrue(it.moveToFirst())
                assertEquals(listOf(1280L, 720L, 100L, 0L), (0..3).map(it::getLong))
            }
            db.query("SELECT confirmedAt FROM program_image_links").use { assertTrue(it.moveToFirst()); assertEquals(120, it.getLong(0)) }
        }
    }

    @Test fun missingMigrationRecreatesIndex() = runTest {
        migrations.createDatabase("missing", 1).use { it.execSQL("INSERT INTO images VALUES ('image','app','content://test','content://test',1280,720,'hash',100,'valid')") }
        val db = Room.databaseBuilder(context, ArtworkDatabase::class.java, "missing")
            .fallbackToDestructiveMigration(dropAllTables = true).build()
        try { assertTrue(db.artwork().images().isEmpty()) } finally { db.close() }
    }

    @Test fun failingMigrationRecreatesIndexBeforeTheNextProviderConfirmation() = runTest {
        migrations.createDatabase("failed-migration", 1).close()
        val broken = Room.databaseBuilder(context, ArtworkDatabase::class.java, "failed-migration")
            .addMigrations(object : androidx.room.migration.Migration(1, 2) {
                override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) { error("Broken migration fixture") }
            }, ArtworkDatabase.MIGRATION_2_3).build()
        val disk = coil.disk.DiskCache.Builder().directory(folder.newFolder()).build()
        ArtworkRepository(context, broken, disk, ImageBytesReader { _, _ -> artworkBytes() }).use { repository ->
            repository.confirm("preview", emptyList())
            assertNotNull(repository.resolve(fr.sygix.sygixos.model.ProgramArtwork("app", listOf("content://fixture/image"), "fiction"), fr.sygix.sygixos.domain.ArtworkPolicy.Hero).first)
        }
    }

    @Test fun tableRenamePreservesProgramsCandidatesAndImageLinks() {
        migrations.createDatabase("rename", 2).use { db ->
            db.execSQL("INSERT INTO programs VALUES ('preview','1','app','identity','full','series',100,200)")
            db.execSQL("INSERT INTO candidates VALUES ('preview','1',0,'content://fixture')")
            db.execSQL("INSERT INTO images VALUES ('image','app','content://fixture','content://fixture',1280,720,'hash',100,'valid',170,NULL)")
            db.execSQL("INSERT INTO associations VALUES ('image','identity','series',120)")
        }
        migrations.runMigrationsAndValidate("rename", 3, true, ArtworkDatabase.MIGRATION_2_3).use { db ->
            for (table in listOf("tv_programs", "program_image_candidates", "program_images", "program_image_links")) {
                db.query("SELECT COUNT(*) FROM $table").use { assertTrue(it.moveToFirst()); assertEquals(1L, it.getLong(0)) }
            }
            db.query("SELECT i.fetchedAt,i.retryAt,l.confirmedAt FROM program_images i JOIN program_image_links l ON l.imageId=i.id").use {
                assertTrue(it.moveToFirst())
                assertEquals(listOf(100L, 170L, 120L), (0..2).map(it::getLong))
            }
            db.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
        }
    }
}
