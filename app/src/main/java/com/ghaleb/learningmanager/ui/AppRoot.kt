package com.ghaleb.learningmanager.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ghaleb.learningmanager.R
import com.ghaleb.learningmanager.mood.Mood
import com.ghaleb.learningmanager.mood.MoodCalculator
import com.ghaleb.learningmanager.repository
import com.ghaleb.learningmanager.settings
import com.ghaleb.learningmanager.streak.DayState
import com.ghaleb.learningmanager.streak.DubaiTime
import com.ghaleb.learningmanager.streak.StreakCalculator
import com.ghaleb.learningmanager.ui.theme.AppTheme
import kotlinx.coroutines.launch
import java.time.LocalDate

private enum class Tab(val label: String, @DrawableRes val icon: Int) {
    STREAK("Streak", R.drawable.ic_nav_streak),
    SUBJECTS("Subjects", R.drawable.ic_nav_subjects),
    DISSOLVED("Dissolved", R.drawable.ic_nav_dissolved),
    SETTINGS("Settings", R.drawable.ic_nav_settings),
}

@Composable
fun AppRoot(syncError: String?, onSync: () -> Unit) {
    val context = LocalContext.current
    val repository = context.repository
    val settingsStore = context.settings
    val scope = rememberCoroutineScope()
    val colors = AppTheme.colors

    val now by remember { DubaiTime.ticker() }.collectAsStateWithLifecycle(DubaiTime.now())
    // Null until Room answers, so the empty state doesn't flash before the cache loads.
    val dates by repository.sessionDates.collectAsStateWithLifecycle(initialValue = null as Set<LocalDate>?)
    val sessions by repository.sessions.collectAsStateWithLifecycle(emptyList())
    val mastery by repository.mastery.collectAsStateWithLifecycle(emptyList())
    val lastSync by repository.lastSyncedAt.collectAsStateWithLifecycle(null)
    val settings by settingsStore.settings.collectAsStateWithLifecycle(null)

    val today = now.toLocalDate()
    val streak = remember(dates, today) { dates?.let { StreakCalculator.compute(it, today) } }
    val mood = streak?.let { s ->
        val yesterdayFrozen = s.days.firstOrNull { it.date == today.minusDays(1) }?.state == DayState.FREEZE
        MoodCalculator.compute(now, s.todayDone, yesterdayFrozen)
    } ?: Mood.CALM

    var tab by rememberSaveable { mutableStateOf(Tab.STREAK) }

    Scaffold(
        containerColor = colors.bg,
        bottomBar = {
            NavigationBar(containerColor = colors.card) {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(painterResource(t.icon), contentDescription = null) },
                        label = { Text(t.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = colors.ink,
                            indicatorColor = colors.session,
                            selectedTextColor = colors.text,
                            unselectedIconColor = colors.textMuted,
                            unselectedTextColor = colors.textMuted,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                Tab.STREAK -> StreakScreen(streak, mood, lastSync, syncError, onSync)
                Tab.SUBJECTS -> SubjectsScreen(sessions, today)
                Tab.DISSOLVED -> DissolvedScreen(mastery)
                Tab.SETTINGS -> SettingsScreen(settings, lastSync, syncError, onSync)
            }
        }
    }

    val s = settings
    if (s != null && streak != null && s.milestoneEvery > 0) {
        val milestone = (streak.totalDaysShownUp / s.milestoneEvery) * s.milestoneEvery
        if (milestone > 0 && milestone > s.lastCelebrated) {
            Celebration(milestone) { scope.launch { settingsStore.setLastCelebrated(milestone) } }
        }
    }
}
