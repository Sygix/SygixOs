/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.migration.Migration

@Entity(tableName = "programs", primaryKeys = ["tableName", "providerId"])
internal data class ArtworkProgram(
    val tableName: String,
    val providerId: String,
    val packageName: String,
    val imageFingerprint: String,
    val fullFingerprint: String,
    val sharedIdentity: String?,
    val confirmedAt: Long,
    val expiresAt: Long,
)

@Entity(tableName = "candidates", primaryKeys = ["tableName", "providerId", "rank"],
    foreignKeys = [ForeignKey(entity = ArtworkProgram::class, parentColumns = ["tableName", "providerId"], childColumns = ["tableName", "providerId"], onDelete = ForeignKey.CASCADE)])
internal data class ArtworkCandidate(val tableName: String, val providerId: String, val rank: Int, val uri: String)

@Entity(tableName = "images", indices = [Index(value = ["packageName", "uri"], unique = true)])
internal data class ArtworkImage(
    @PrimaryKey val id: String,
    val packageName: String,
    val uri: String,
    val diskKey: String,
    val width: Int,
    val height: Int,
    val hash: String,
    val fetchedAt: Long,
    val state: String,
    @androidx.room.ColumnInfo(defaultValue = "0") val retryAt: Long = 0,
    val failureCause: String? = null,
)

@Entity(tableName = "associations", primaryKeys = ["imageId", "identity"],
    foreignKeys = [ForeignKey(entity = ArtworkImage::class, parentColumns = ["id"], childColumns = ["imageId"], onDelete = ForeignKey.CASCADE)])
internal data class ArtworkAssociation(val imageId: String, val identity: String, val sharedIdentity: String?, val confirmedAt: Long)

@Dao
internal abstract class ArtworkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun program(value: ArtworkProgram)
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun candidates(values: List<ArtworkCandidate>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) abstract suspend fun insertImage(value: ArtworkImage)
    @Query("UPDATE images SET width=:width, height=:height, hash=:hash, fetchedAt=:fetchedAt, state=:state, retryAt=:retryAt, failureCause=:failureCause WHERE id=:id")
    abstract suspend fun updateImage(id: String, width: Int, height: Int, hash: String, fetchedAt: Long, state: String, retryAt: Long, failureCause: String?)
    @Transaction open suspend fun image(value: ArtworkImage) {
        insertImage(value)
        updateImage(value.id, value.width, value.height, value.hash, value.fetchedAt, value.state, value.retryAt, value.failureCause)
    }
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun associate(value: ArtworkAssociation)
    @Query("SELECT * FROM programs WHERE tableName=:table") abstract suspend fun programs(table: String): List<ArtworkProgram>
    @Query("SELECT * FROM candidates WHERE tableName=:table AND providerId=:id ORDER BY rank") abstract suspend fun candidates(table: String, id: String): List<ArtworkCandidate>
    @Query("SELECT * FROM images WHERE packageName=:app AND uri=:uri") abstract suspend fun image(app: String, uri: String): ArtworkImage?
    @Query("SELECT * FROM images") abstract suspend fun images(): List<ArtworkImage>
    @Query("SELECT * FROM images") abstract fun observeImages(): kotlinx.coroutines.flow.Flow<List<ArtworkImage>>
    @Query("SELECT * FROM associations WHERE imageId=:id") abstract suspend fun associations(id: String): List<ArtworkAssociation>
    @Query("DELETE FROM associations WHERE imageId=:id") abstract suspend fun clearAssociations(id: String)
    @Query("SELECT COUNT(DISTINCT COALESCE(a.sharedIdentity, a.identity)) FROM associations a JOIN images i ON a.imageId=i.id WHERE i.packageName=:app AND i.hash=:hash")
    abstract suspend fun identities(app: String, hash: String): Int
    @Query("SELECT * FROM images WHERE packageName=:app AND hash=:hash") abstract suspend fun matching(app: String, hash: String): List<ArtworkImage>
    @Query("DELETE FROM programs WHERE tableName=:table AND providerId NOT IN (:ids)") abstract suspend fun removeAbsent(table: String, ids: List<String>)
    @Query("DELETE FROM programs WHERE expiresAt<=:now") abstract suspend fun expirePrograms(now: Long)
    @Query("DELETE FROM associations WHERE identity NOT IN (SELECT imageFingerprint FROM programs) AND (sharedIdentity IS NULL OR sharedIdentity NOT IN (SELECT sharedIdentity FROM programs WHERE sharedIdentity IS NOT NULL))")
    abstract suspend fun removeOrphanAssociations()
    @Query("DELETE FROM images WHERE id=:id") abstract suspend fun deleteImage(id: String)
    @Transaction open suspend fun confirm(table: String, values: List<Pair<ArtworkProgram, List<ArtworkCandidate>>>) {
        removeAbsent(table, values.map { it.first.providerId })
        values.forEach { (program, candidates) -> program(program); candidates(candidates) }
    }
}

@Database(entities = [ArtworkProgram::class, ArtworkCandidate::class, ArtworkImage::class, ArtworkAssociation::class], version = 2, exportSchema = true)
internal abstract class ArtworkDatabase : RoomDatabase() {
    abstract fun artwork(): ArtworkDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE images ADD COLUMN retryAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE images ADD COLUMN failureCause TEXT")
            }
        }
        fun create(context: Context, name: String = "artwork.db"): ArtworkDatabase = Room.databaseBuilder(context, ArtworkDatabase::class.java, name)
            .addMigrations(MIGRATION_1_2)
            .fallbackToDestructiveMigration(dropAllTables = true).build()
    }
}
