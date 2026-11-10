package com.ghaleb.learningmanager.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.ghaleb.learningmanager.streak.DubaiTime
import com.ghaleb.learningmanager.streak.Streak
import com.ghaleb.learningmanager.streak.StreakCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.LocalDate

data class Session(
    val id: String,
    val date: LocalDate,
    val subject: String,
    val topics: List<String>,
    val summary: String?,
    val light: Boolean,
    val createdAt: Instant,
)

data class Mastery(val subject: String, val topic: String, val level: String, val updatedAt: Instant)

/**
 * Screens and the widget read only from the Room cache; [sync] is the only thing that touches the network.
 * [api] is null when local.properties had no Supabase config at build time.
 */
class LearningRepository(
    private val api: SupabaseApi?,
    private val dao: LearningDao,
    private val prefs: DataStore<Preferences>,
) {
    private val syncLock = Mutex()

    val sessions: Flow<List<Session>> = dao.observeSessions().map { rows -> rows.map { it.toModel() } }

    val sessionDates: Flow<Set<LocalDate>> =
        dao.observeSessionDates().map { dates -> dates.mapTo(HashSet(), LocalDate::parse) }

    val mastery: Flow<List<Mastery>> = dao.observeMastery().map { rows -> rows.map { it.toModel() } }

    val lastSyncedAt: Flow<Instant?> = prefs.data.map { p -> p[LAST_SYNC]?.let(Instant::ofEpochMilli) }

    /** One-shot streak for callers that aren't observing, such as the widget worker. */
    suspend fun currentStreak(today: LocalDate = DubaiTime.today()): Streak =
        StreakCalculator.compute(dao.sessionDates().mapTo(HashSet(), LocalDate::parse), today)

    suspend fun sync(): Result<Unit> = syncLock.withLock {
        runCatching {
            val client = api ?: error("Supabase URL or key missing from local.properties")
            val sessions = client.sessions().map { it.toEntity() }
            val mastery = client.mastery().map { it.toEntity() }
            dao.replaceAll(sessions, mastery)
            prefs.edit { it[LAST_SYNC] = System.currentTimeMillis() }
            Unit
        }
    }

    private companion object {
        val LAST_SYNC = longPreferencesKey("last_sync_ms")
    }
}

private fun SessionDto.toEntity() = SessionEntity(id, date, subject, topics, summary, light, createdAt)

private fun MasteryDto.toEntity() = MasteryEntity(subject, topic, level, updatedAt)

private fun SessionEntity.toModel() =
    Session(id, LocalDate.parse(date), subject, topics, summary, light, parseInstant(createdAt))

private fun MasteryEntity.toModel() = Mastery(subject, topic, level, parseInstant(updatedAt))

// Postgres timestamptz comes back like 2026-09-24T17:01:02.123456+00:00, which Instant.parse rejects on API 26.
private fun parseInstant(value: String): Instant =
    runCatching { java.time.OffsetDateTime.parse(value).toInstant() }.getOrElse { Instant.EPOCH }
