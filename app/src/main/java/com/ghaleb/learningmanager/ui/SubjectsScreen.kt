package com.ghaleb.learningmanager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ghaleb.learningmanager.data.Session
import com.ghaleb.learningmanager.ui.theme.AppTheme
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private val tileShape = RoundedCornerShape(20.dp)

@Composable
fun SubjectsScreen(sessions: List<Session>, today: LocalDate) {
    val bySubject = remember(sessions) { sessions.groupBy { it.subject } }
    val subjects = remember(bySubject) { KNOWN_SUBJECTS + (bySubject.keys - KNOWN_SUBJECTS.toSet()).sorted() }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text("Subjects", style = MaterialTheme.typography.headlineMedium)
        }
        items(subjects, key = { it }) { subject ->
            val last = bySubject[subject]?.maxOf { it.date }
            SubjectTile(subject, last, today, selected == subject) {
                selected = if (selected == subject) null else subject
            }
        }
        selected?.let { subject ->
            item(span = { GridItemSpan(maxLineSpan) }) { TopicList(subject, bySubject[subject].orEmpty()) }
        }
    }
}

@Composable
private fun SubjectTile(subject: String, last: LocalDate?, today: LocalDate, selected: Boolean, onClick: () -> Unit) {
    val c = AppTheme.colors
    val recent = last != null && ChronoUnit.DAYS.between(last, today) < 7
    val border = when {
        selected -> c.text
        recent -> c.session
        else -> Color.Transparent
    }
    Column(
        Modifier.fillMaxWidth().height(112.dp).clip(tileShape).background(c.card)
            .border(2.dp, border, tileShape).clickable(onClick = onClick).padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            subjectName(subject),
            style = MaterialTheme.typography.titleLarge,
            color = if (recent) c.text else c.textMuted,
        )
        Text(recency(last, today), style = MaterialTheme.typography.bodyMedium, color = if (recent) c.session else c.textMuted)
    }
}

@Composable
private fun TopicList(subject: String, sessions: List<Session>) {
    val c = AppTheme.colors
    val topics = remember(sessions) {
        sessions.sortedByDescending { it.createdAt }
            .flatMap { s -> s.topics.map { it to s.date } }
            .distinctBy { it.first }
            .take(12)
    }
    Column(
        Modifier.fillMaxWidth().clip(tileShape).background(c.card).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("${subjectName(subject)}: recent topics", style = MaterialTheme.typography.titleMedium)
        if (topics.isEmpty()) {
            Text("No sessions yet.", color = c.textMuted)
        }
        topics.forEach { (topic, date) ->
            Row(Modifier.fillMaxWidth()) {
                Text(topicWords(topic), modifier = Modifier.weight(1f))
                Spacer(Modifier.padding(start = 8.dp))
                Text(date.format(shortDate), color = c.textMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
