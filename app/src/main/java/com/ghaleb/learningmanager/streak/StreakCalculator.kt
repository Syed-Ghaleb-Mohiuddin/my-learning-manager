package com.ghaleb.learningmanager.streak

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

enum class DayState { SESSION, FREEZE, GAP, TODAY_PENDING, FUTURE }

data class StreakDay(val date: LocalDate, val state: DayState)

data class Streak(
    /** From the first ever session to the Sunday of the current week. Empty before any session. */
    val days: List<StreakDay>,
    /** Headline number: distinct days with a session. Can only ever go up. */
    val totalDaysShownUp: Int,
    /** Current run of session/freeze days, or null when there is no live run. Never shown as 0. */
    val currentRun: Int?,
    val todayDone: Boolean,
) {
    companion object {
        val EMPTY = Streak(emptyList(), 0, null, false)
    }
}

/**
 * Pure streak maths over the set of Dubai dates that have a session. Nothing is stored.
 *
 * Weeks run Monday to Sunday. The first missed past day in each week is a freeze, later ones are gaps.
 * Today is never a miss while it is still today.
 */
object StreakCalculator {

    fun compute(sessionDates: Set<LocalDate>, today: LocalDate): Streak {
        // Rows dated after today can only come from a clock mismatch; ignore them rather than show a future session.
        val past = sessionDates.filterTo(HashSet()) { !it.isAfter(today) }
        val first = past.minOrNull() ?: return Streak.EMPTY
        val lastShown = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

        val weeksWithFreezeUsed = HashSet<LocalDate>()
        val days = ArrayList<StreakDay>()
        var d = first
        while (!d.isAfter(lastShown)) {
            val state = when {
                d in past -> DayState.SESSION
                d.isAfter(today) -> DayState.FUTURE
                d == today -> DayState.TODAY_PENDING
                weeksWithFreezeUsed.add(weekStart(d)) -> DayState.FREEZE
                else -> DayState.GAP
            }
            days += StreakDay(d, state)
            d = d.plusDays(1)
        }

        return Streak(
            days = days,
            totalDaysShownUp = past.size,
            currentRun = currentRun(days, today),
            todayDone = today in past,
        )
    }

    fun weekStart(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    private fun currentRun(days: List<StreakDay>, today: LocalDate): Int? {
        val byDate = days.associateBy({ it.date }, { it.state })
        // A pending today doesn't break the run; count back from yesterday instead.
        var d = if (byDate[today] == DayState.SESSION) today else today.minusDays(1)
        var length = 0
        var sessions = 0
        while (true) {
            when (byDate[d]) {
                DayState.SESSION -> { length++; sessions++ }
                DayState.FREEZE -> length++
                else -> break
            }
            d = d.minusDays(1)
        }
        // A run made only of freeze days isn't a run he did; hide it.
        return if (sessions > 0) length else null
    }
}
