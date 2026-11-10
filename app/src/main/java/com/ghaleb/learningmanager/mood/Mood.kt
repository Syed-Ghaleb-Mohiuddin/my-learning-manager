package com.ghaleb.learningmanager.mood

import androidx.annotation.DrawableRes
import com.ghaleb.learningmanager.R
import java.time.ZonedDateTime

enum class Mood(@DrawableRes val drawable: Int, val short: String, val line: String) {
    CALM(R.drawable.mood_calm, "All good", "Plenty of day left"),
    NUDGING(R.drawable.mood_nudging, "Session today?", "Got a minute to learn?"),
    ANNOYED(R.drawable.mood_annoyed, "Still waiting", "Still waiting on today"),
    ANGRY(R.drawable.mood_angry, "Today's slipping", "Today's slipping away"),
    FURIOUS(R.drawable.mood_furious, "Session. Now.", "Session. Right now."),
    PLEASED(R.drawable.mood_pleased, "Done today", "Today's done. Nice."),
    FREEZE(R.drawable.mood_freeze, "Frozen, not lost", "Yesterday froze. Today counts."),
}

/** Tune the ladder here. Dubai hour (0-23) from which each mood starts while today has no session. */
object MoodConfig {
    const val NUDGING_FROM = 12
    const val ANNOYED_FROM = 16
    const val ANGRY_FROM = 19
    const val FURIOUS_FROM = 21
}

object MoodCalculator {
    /** [now] must be Dubai time. Computed locally on every update so it escalates even offline. */
    fun compute(now: ZonedDateTime, todayDone: Boolean, yesterdayFrozen: Boolean): Mood {
        if (todayDone) return Mood.PLEASED
        val hour = now.hour
        return when {
            hour >= MoodConfig.FURIOUS_FROM -> Mood.FURIOUS
            hour >= MoodConfig.ANGRY_FROM -> Mood.ANGRY
            hour >= MoodConfig.ANNOYED_FROM -> Mood.ANNOYED
            hour >= MoodConfig.NUDGING_FROM -> Mood.NUDGING
            // Morning after a freeze: a gentle heads-up instead of plain calm.
            yesterdayFrozen -> Mood.FREEZE
            else -> Mood.CALM
        }
    }
}
