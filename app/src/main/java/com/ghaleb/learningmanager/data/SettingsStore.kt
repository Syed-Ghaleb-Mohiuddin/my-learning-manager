package com.ghaleb.learningmanager.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ghaleb.learningmanager.mood.Mood
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// Exactly one DataStore per file per process, so the delegate lives here and nowhere else.
val Context.appPrefs: DataStore<Preferences> by preferencesDataStore("prefs")

data class Settings(
    val reminderOn: Boolean = false,
    val reminderHour: Int = 19,
    val reminderMinute: Int = 0,
    val milestoneEvery: Int = 25,
    /** Forces the widget into one mood so every state can be checked without waiting. Null = live. */
    val moodOverride: Mood? = null,
    val lastCelebrated: Int = 0,
)

class SettingsStore(private val prefs: DataStore<Preferences>) {

    val settings: Flow<Settings> = prefs.data.map { p ->
        Settings(
            reminderOn = p[REMINDER_ON] ?: false,
            reminderHour = p[REMINDER_HOUR] ?: 19,
            reminderMinute = p[REMINDER_MINUTE] ?: 0,
            milestoneEvery = p[MILESTONE_EVERY] ?: 25,
            moodOverride = p[MOOD_OVERRIDE]?.let { name -> Mood.entries.firstOrNull { it.name == name } },
            lastCelebrated = p[LAST_CELEBRATED] ?: 0,
        )
    }

    suspend fun current(): Settings = settings.first()

    suspend fun setReminder(on: Boolean, hour: Int, minute: Int) = prefs.edit {
        it[REMINDER_ON] = on
        it[REMINDER_HOUR] = hour
        it[REMINDER_MINUTE] = minute
    }

    suspend fun setMilestoneEvery(days: Int) = prefs.edit { it[MILESTONE_EVERY] = days }

    suspend fun setMoodOverride(mood: Mood?) = prefs.edit {
        if (mood == null) it.remove(MOOD_OVERRIDE) else it[MOOD_OVERRIDE] = mood.name
    }

    suspend fun setLastCelebrated(total: Int) = prefs.edit { it[LAST_CELEBRATED] = total }

    private companion object {
        val REMINDER_ON = booleanPreferencesKey("reminder_on")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val MILESTONE_EVERY = intPreferencesKey("milestone_every")
        val MOOD_OVERRIDE = stringPreferencesKey("mood_override")
        val LAST_CELEBRATED = intPreferencesKey("last_celebrated")
    }
}
