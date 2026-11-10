package com.ghaleb.learningmanager.widget

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import com.ghaleb.learningmanager.mood.Mood
import com.ghaleb.learningmanager.mood.MoodCalculator
import com.ghaleb.learningmanager.repository
import com.ghaleb.learningmanager.settings
import com.ghaleb.learningmanager.streak.DayState
import com.ghaleb.learningmanager.streak.DubaiTime
import com.ghaleb.learningmanager.streak.StreakCalculator

/** What the widget draws. [week] is Monday..Sunday; null = before the first ever session. */
data class WidgetSnapshot(val mood: Mood, val week: List<DayState?>)

/**
 * The widget renders from its own Glance state, written here. Glance keeps a session alive between updates
 * and only recomposes it, so data loaded once in provideGlance would go stale; state written here doesn't.
 */
object WidgetUpdater {
    private val MOOD = stringPreferencesKey("mood")
    private val WEEK = stringPreferencesKey("week")

    suspend fun refresh(context: Context) {
        val ids = GlanceAppWidgetManager(context).getGlanceIds(LearningWidget::class.java)
        if (ids.isEmpty()) return
        val snapshot = snapshot(context)
        val widget = LearningWidget()
        for (id in ids) {
            write(context, id, snapshot)
            widget.update(context, id)
        }
    }

    suspend fun write(context: Context, id: GlanceId, snapshot: WidgetSnapshot) {
        updateAppWidgetState(context, id) { prefs ->
            prefs[MOOD] = snapshot.mood.name
            prefs[WEEK] = snapshot.week.joinToString(",") { it?.name ?: "-" }
        }
    }

    suspend fun snapshot(context: Context): WidgetSnapshot {
        val now = DubaiTime.now()
        val today = now.toLocalDate()
        val streak = context.repository.currentStreak(today)
        val byDate = streak.days.associate { it.date to it.state }
        val mood = context.settings.current().moodOverride
            ?: MoodCalculator.compute(now, streak.todayDone, byDate[today.minusDays(1)] == DayState.FREEZE)
        val monday = StreakCalculator.weekStart(today)
        val week = (0L..6L).map { offset ->
            val day = monday.plusDays(offset)
            byDate[day] ?: when {
                day.isAfter(today) -> DayState.FUTURE
                day == today -> DayState.TODAY_PENDING
                else -> null
            }
        }
        return WidgetSnapshot(mood, week)
    }

    fun read(prefs: Preferences): WidgetSnapshot {
        val mood = prefs[MOOD]?.let { name -> Mood.entries.firstOrNull { it.name == name } } ?: Mood.CALM
        val week = prefs[WEEK]?.split(",")?.map { name -> DayState.entries.firstOrNull { it.name == name } }
            ?: List(7) { null }
        return WidgetSnapshot(mood, week)
    }
}
