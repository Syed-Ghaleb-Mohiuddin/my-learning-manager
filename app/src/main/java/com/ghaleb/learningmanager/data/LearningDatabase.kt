package com.ghaleb.learningmanager.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    /** ISO yyyy-MM-dd, Dubai-local. Kept as text so it sorts and compares correctly in SQL. */
    val date: String,
    val subject: String,
    val topics: List<String>,
    val summary: String?,
    val light: Boolean,
    val createdAt: String,
)

@Entity(tableName = "mastery", primaryKeys = ["subject", "topic"])
data class MasteryEntity(
    val subject: String,
    val topic: String,
    val level: String,
    val updatedAt: String,
)

class Converters {
    // Topics are kebab-case slugs, so a comma can never appear inside one.
    @TypeConverter
    fun fromTopics(topics: List<String>): String = topics.joinToString(",")

    @TypeConverter
    fun toTopics(value: String): List<String> = if (value.isEmpty()) emptyList() else value.split(",")
}

@Dao
abstract class LearningDao {
    @Query("SELECT * FROM sessions ORDER BY date, createdAt")
    abstract fun observeSessions(): Flow<List<SessionEntity>>

    @Query("SELECT DISTINCT date FROM sessions")
    abstract fun observeSessionDates(): Flow<List<String>>

    @Query("SELECT DISTINCT date FROM sessions")
    abstract suspend fun sessionDates(): List<String>

    @Query("SELECT * FROM mastery ORDER BY subject, topic")
    abstract fun observeMastery(): Flow<List<MasteryEntity>>

    @Query("DELETE FROM sessions")
    protected abstract suspend fun clearSessions()

    @Query("DELETE FROM mastery")
    protected abstract suspend fun clearMastery()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertSessions(rows: List<SessionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertMastery(rows: List<MasteryEntity>)

    /** Full replace, so rows deleted on the server (e.g. test data) disappear here too. */
    @Transaction
    open suspend fun replaceAll(sessions: List<SessionEntity>, mastery: List<MasteryEntity>) {
        clearSessions()
        clearMastery()
        insertSessions(sessions)
        insertMastery(mastery)
    }
}

@Database(entities = [SessionEntity::class, MasteryEntity::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class LearningDatabase : RoomDatabase() {
    abstract fun dao(): LearningDao

    companion object {
        fun create(context: Context): LearningDatabase =
            Room.databaseBuilder(context, LearningDatabase::class.java, "learning.db")
                // It's only a cache of Supabase; a schema change can just rebuild it on next sync.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
