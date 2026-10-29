/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import android.content.Context
import androidx.room.Dao
import androidx.room.ColumnInfo
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "tracking_account")
data class StoredTrackingAccount(
    @PrimaryKey val slot: Int = 1,
    val accountId: String,
    val name: String,
    val incarnation: String,
    val sessionHash: String,
    val revision: Long = 0,
    val snapshotAt: Long? = null,
    val failure: String? = null,
    val accessDenied: Boolean = false,
    val retryAt: Long = 0,
)

@Entity(tableName = "tracking_content", primaryKeys = ["incarnation", "key"])
data class StoredTrackingContent(
    val incarnation: String,
    val key: String,
    val parentKey: String,
    val payload: String,
    val revision: Long,
)

@Entity(tableName = "tracking_intent", primaryKeys = ["incarnation", "key"])
data class StoredTrackingIntent(
    val incarnation: String,
    val key: String,
    val revision: Long,
    val desiredState: Int,
    val previousState: Int,
    val possiblySent: Boolean = false,
    @ColumnInfo(defaultValue = "0") val undoHeld: Boolean = false,
)

@Entity(tableName = "tracking_reference", primaryKeys = ["incarnation", "key", "owner"])
data class StoredTrackingReference(val incarnation: String, val key: String, val owner: String)

@Entity(tableName = "tracking_undo", primaryKeys = ["incarnation", "key"])
data class StoredTrackingUndo(
    val incarnation: String,
    val key: String,
    val revision: Long,
    val remainingMillis: Long,
    val previousPayload: String,
    val previousIntentRevision: Long?,
    val previousDesiredState: Int?,
    val previousState: Int?,
    val previousPossiblySent: Boolean?,
)

@Entity(tableName = "tracking_reserve", primaryKeys = ["incarnation", "parentKey"])
data class StoredTrackingReserve(val incarnation: String, val parentKey: String, val complete: Boolean, val needsFill: Boolean,
    @ColumnInfo(defaultValue = "0") val requestedRevision: Long = 0)

@Dao
interface TrackingDao {
    @Query("SELECT * FROM tracking_account WHERE slot = 1")
    suspend fun account(): StoredTrackingAccount?

    @Query("SELECT * FROM tracking_account WHERE slot = 1")
    fun observeAccount(): Flow<StoredTrackingAccount?>

    @Query("SELECT * FROM tracking_content")
    fun observeContents(): Flow<List<StoredTrackingContent>>

    @Query("SELECT * FROM tracking_intent")
    fun observeIntents(): Flow<List<StoredTrackingIntent>>

    @Query("SELECT * FROM tracking_reserve")
    fun observeReserves(): Flow<List<StoredTrackingReserve>>

    @Query("SELECT * FROM tracking_undo")
    fun observeUndo(): Flow<List<StoredTrackingUndo>>

    @Query("SELECT * FROM tracking_content WHERE incarnation = :incarnation")
    suspend fun contents(incarnation: String): List<StoredTrackingContent>

    @Query("SELECT * FROM tracking_intent WHERE incarnation = :incarnation ORDER BY revision")
    suspend fun intents(incarnation: String): List<StoredTrackingIntent>

    @Query("SELECT * FROM tracking_reference WHERE incarnation = :incarnation")
    suspend fun references(incarnation: String): List<StoredTrackingReference>

    @Query("SELECT * FROM tracking_reserve WHERE incarnation = :incarnation")
    suspend fun reserves(incarnation: String): List<StoredTrackingReserve>

    @Query("SELECT * FROM tracking_undo WHERE incarnation = :incarnation ORDER BY revision")
    suspend fun undoContexts(incarnation: String): List<StoredTrackingUndo>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putAccount(value: StoredTrackingAccount)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putContents(values: List<StoredTrackingContent>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putIntent(value: StoredTrackingIntent)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putReference(value: StoredTrackingReference)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putReserve(value: StoredTrackingReserve)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putUndo(value: StoredTrackingUndo)

    @Query("DELETE FROM tracking_reserve WHERE incarnation = :incarnation AND parentKey = :parentKey")
    suspend fun deleteReserve(incarnation: String, parentKey: String)

    @Query("DELETE FROM tracking_undo WHERE incarnation = :incarnation AND `key` = :key AND revision = :revision")
    suspend fun deleteUndo(incarnation: String, key: String, revision: Long)

    @Query("DELETE FROM tracking_intent WHERE incarnation = :incarnation AND `key` = :key AND revision = :revision")
    suspend fun deleteIntent(incarnation: String, key: String, revision: Long)

    @Query("DELETE FROM tracking_content WHERE incarnation = :incarnation AND `key` = :key")
    suspend fun deleteContent(incarnation: String, key: String)

    @Query("DELETE FROM tracking_reference WHERE incarnation = :incarnation AND owner = :owner")
    suspend fun deleteReferences(incarnation: String, owner: String)

    @Query("DELETE FROM tracking_intent")
    suspend fun clearIntents()

    @Query("DELETE FROM tracking_content")
    suspend fun clearContents()

    @Query("DELETE FROM tracking_reference")
    suspend fun clearReferences()

    @Query("DELETE FROM tracking_reserve")
    suspend fun clearReserves()

    @Query("DELETE FROM tracking_undo")
    suspend fun clearUndo()

    @Query("DELETE FROM tracking_account")
    suspend fun clearAccount()
}

@Database(entities = [StoredTrackingAccount::class, StoredTrackingContent::class, StoredTrackingIntent::class,
    StoredTrackingReference::class, StoredTrackingReserve::class, StoredTrackingUndo::class], version = 3, exportSchema = true)
abstract class TrackingDatabase : RoomDatabase() {
    abstract fun tracking(): TrackingDao

    companion object {
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tracking_reserve ADD COLUMN requestedRevision INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tracking_intent ADD COLUMN undoHeld INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE TABLE IF NOT EXISTS tracking_undo (incarnation TEXT NOT NULL, `key` TEXT NOT NULL, revision INTEGER NOT NULL, remainingMillis INTEGER NOT NULL, previousPayload TEXT NOT NULL, previousIntentRevision INTEGER, previousDesiredState INTEGER, previousState INTEGER, previousPossiblySent INTEGER, PRIMARY KEY(incarnation, `key`))")
            }
        }

        fun open(context: Context): TrackingDatabase = Room.databaseBuilder(context.applicationContext,
            TrackingDatabase::class.java, context.noBackupFilesDir.resolve("tracking.db").absolutePath).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
    }
}
