package com.ghaleb.learningmanager.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ghaleb.learningmanager.mood.Mood
import com.ghaleb.learningmanager.streak.DayState
import com.ghaleb.learningmanager.streak.DubaiTime
import com.ghaleb.learningmanager.streak.Streak
import com.ghaleb.learningmanager.streak.StreakDay
import com.ghaleb.learningmanager.ui.theme.AppTheme
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

private val CELL = 26.dp
private val GAP = 5.dp
private val MONTH_ROW = 16.dp
private val cellShape = RoundedCornerShape(6.dp)
private val timeFormat = DateTimeFormatter.ofPattern("d MMM, HH:mm")

@Composable
fun StreakScreen(streak: Streak?, mood: Mood, lastSync: Instant?, syncError: String?, onSync: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 20.dp),
    ) {
        Text("Streak", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(16.dp))
        if (streak != null) {
            if (streak.days.isNotEmpty()) {
                StreakGrid(streak.days)
                Spacer(Modifier.height(12.dp))
                Legend()
                Spacer(Modifier.height(20.dp))
            }
            SummaryCard(streak, mood)
        }
        Spacer(Modifier.height(12.dp))
        SyncLine(lastSync, syncError, onSync)
    }
}

private class WeekColumn(val days: List<StreakDay?>, val monthLabel: String?)

private fun toWeeks(days: List<StreakDay>): List<WeekColumn> {
    // Days start at the first ever session, which may be mid-week; pad so each column is Monday..Sunday.
    val lead = days.first().date.dayOfWeek.value - 1
    val padded: List<StreakDay?> = List(lead) { null } + days
    return padded.chunked(7).mapIndexed { i, week ->
        val real = week.filterNotNull()
        val labelDate = if (i == 0) real.firstOrNull()?.date else real.firstOrNull { it.date.dayOfMonth == 1 }?.date
        WeekColumn(week, labelDate?.month?.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault()))
    }
}

@Composable
private fun StreakGrid(days: List<StreakDay>) {
    val weeks = remember(days) { toWeeks(days) }
    val muted = AppTheme.colors.textMuted
    Row(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 20.dp, end = 8.dp), verticalArrangement = Arrangement.spacedBy(GAP)) {
            Spacer(Modifier.height(MONTH_ROW))
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { letter ->
                Box(Modifier.height(CELL), contentAlignment = Alignment.Center) {
                    Text(letter, style = MaterialTheme.typography.labelSmall, color = muted)
                }
            }
        }
        // Reversed so the newest week sits at the right edge and older weeks scroll in from the left.
        LazyRow(
            modifier = Modifier.weight(1f),
            reverseLayout = true,
            horizontalArrangement = Arrangement.spacedBy(GAP),
            contentPadding = PaddingValues(end = 20.dp),
        ) {
            items(weeks.asReversed()) { week ->
                Column(verticalArrangement = Arrangement.spacedBy(GAP)) {
                    Box(Modifier.height(MONTH_ROW).width(CELL)) {
                        week.monthLabel?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.labelSmall,
                                color = muted,
                                softWrap = false,
                                modifier = Modifier.wrapContentWidth(Alignment.Start, unbounded = true),
                            )
                        }
                    }
                    week.days.forEach { DayCell(it?.state) }
                }
            }
        }
    }
}

@Composable
private fun DayCell(state: DayState?) {
    val c = AppTheme.colors
    val filled = Modifier.size(CELL).clip(cellShape)
    when (state) {
        null -> Spacer(Modifier.size(CELL))
        DayState.SESSION -> Box(filled.background(c.session))
        DayState.FREEZE -> Box(filled.background(c.freeze))
        DayState.GAP -> Box(filled.background(c.gap))
        DayState.FUTURE -> Box(filled.background(c.future))
        DayState.TODAY_PENDING -> Box(Modifier.size(CELL).border(2.dp, c.session, cellShape))
    }
}

@Composable
private fun Legend() {
    val c = AppTheme.colors
    Row(
        Modifier.padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendItem(c.session, null, "session")
        LegendItem(c.freeze, null, "freeze")
        LegendItem(c.gap, null, "gap")
        LegendItem(Color.Transparent, c.session, "today")
    }
}

@Composable
private fun LegendItem(fill: Color, outline: Color?, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val shape = RoundedCornerShape(3.dp)
        var m = Modifier.size(12.dp).clip(shape).background(fill)
        if (outline != null) m = m.border(1.5.dp, outline, shape)
        Box(m)
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = AppTheme.colors.textMuted)
    }
}

@Composable
private fun SummaryCard(streak: Streak, mood: Mood) {
    val c = AppTheme.colors
    Row(
        Modifier.padding(horizontal = 20.dp).fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)).background(c.card).padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(mood.drawable), contentDescription = mood.line, modifier = Modifier.size(96.dp))
        Spacer(Modifier.width(16.dp))
        Column {
            if (streak.totalDaysShownUp == 0) {
                // Rule: never show a zero. Before the first session this is an invitation, not a count.
                Text("Day one", style = MaterialTheme.typography.headlineLarge, color = c.session)
                Text("starts today", style = MaterialTheme.typography.titleMedium)
            } else {
                Text("${streak.totalDaysShownUp}", style = MaterialTheme.typography.displayLarge, color = c.session)
                Text(
                    if (streak.totalDaysShownUp == 1) "day shown up" else "days shown up",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            streak.currentRun?.let { run ->
                Text(
                    "run: $run ${if (run == 1) "day" else "days"}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = c.textMuted,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(mood.line, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SyncLine(lastSync: Instant?, syncError: String?, onSync: () -> Unit) {
    val c = AppTheme.colors
    Row(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                lastSync?.let { "Synced " + it.atZone(DubaiTime.ZONE).format(timeFormat) } ?: "Not synced yet",
                style = MaterialTheme.typography.bodySmall,
                color = c.textMuted,
            )
            syncError?.let { Text("Sync failed: $it", style = MaterialTheme.typography.bodySmall, color = c.anger) }
        }
        TextButton(onClick = onSync) { Text("Sync now") }
    }
}
