package com.ghaleb.learningmanager.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ghaleb.learningmanager.R
import com.ghaleb.learningmanager.data.Mastery
import com.ghaleb.learningmanager.ui.theme.AppTheme

private val pill = RoundedCornerShape(50)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DissolvedScreen(mastery: List<Mastery>) {
    val c = AppTheme.colors
    val dissolved = remember(mastery) { mastery.filter { it.level == "dissolved" }.sortedByDescending { it.updatedAt } }
    val newest = dissolved.firstOrNull()
    val bySubject = remember(dissolved) { dissolved.groupBy { it.subject }.toSortedMap() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Dissolved", style = MaterialTheme.typography.headlineMedium)
                Text("Things you've mastered", color = c.textMuted)
            }
            Image(painterResource(R.drawable.mood_pleased), contentDescription = null, modifier = Modifier.size(64.dp))
        }
        Spacer(Modifier.height(24.dp))
        if (dissolved.isEmpty()) {
            Text("Nothing here yet. Topics land here when you call them dissolved.", color = c.textMuted)
        }
        bySubject.forEach { (subject, items) ->
            Text(subjectName(subject), style = MaterialTheme.typography.labelLarge, color = c.textMuted)
            Spacer(Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items.forEach { TopicChip(it, isNewest = it == newest) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TopicChip(item: Mastery, isNewest: Boolean) {
    val c = AppTheme.colors
    Row(
        Modifier.clip(pill).background(c.card)
            .border(if (isNewest) 2.5.dp else 1.5.dp, c.session, pill)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isNewest) {
            Icon(painterResource(R.drawable.ic_nav_dissolved), contentDescription = "newest", tint = c.session, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(topicWords(item.topic), style = MaterialTheme.typography.bodyMedium)
    }
}
