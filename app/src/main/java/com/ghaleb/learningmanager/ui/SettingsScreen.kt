package com.ghaleb.learningmanager.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ghaleb.learningmanager.data.Settings
import com.ghaleb.learningmanager.mood.Mood
import com.ghaleb.learningmanager.reminder.ReminderScheduler
import com.ghaleb.learningmanager.settings
import com.ghaleb.learningmanager.streak.DubaiTime
import com.ghaleb.learningmanager.ui.theme.AppTheme
import com.ghaleb.learningmanager.ui.theme.Condensed
import com.ghaleb.learningmanager.widget.WidgetUpdater
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.format.DateTimeFormatter

private const val MILESTONE_STEP = 5
private val syncFormat = DateTimeFormatter.ofPattern("d MMM, HH:mm")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(settings: Settings?, lastSync: Instant?, syncError: String?, onSync: () -> Unit) {
    if (settings == null) return
    val context = LocalContext.current
    val store = context.settings
    val scope = rememberCoroutineScope()
    val c = AppTheme.colors
    var pickingTime by remember { mutableStateOf(false) }
    // The reminder stays on whatever he answers; the receiver checks the permission before posting.
    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    fun saveReminder(on: Boolean, hour: Int, minute: Int) {
        scope.launch {
            store.setReminder(on, hour, minute)
            ReminderScheduler.schedule(context, store.current())
        }
    }

    fun setMood(mood: Mood?) {
        scope.launch {
            store.setMoodOverride(mood)
            WidgetUpdater.refresh(context.applicationContext)
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        SettingCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Daily reminder", style = MaterialTheme.typography.titleMedium)
                    Text("Only if today has no session yet", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
                }
                Switch(
                    checked = settings.reminderOn,
                    onCheckedChange = { on ->
                        if (on && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        saveReminder(on, settings.reminderHour, settings.reminderMinute)
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = c.session, checkedThumbColor = c.ink),
                )
            }
            if (settings.reminderOn) {
                Text(
                    "%02d:%02d".format(settings.reminderHour, settings.reminderMinute),
                    style = MaterialTheme.typography.displayMedium.copy(fontFamily = Condensed),
                    color = c.session,
                    modifier = Modifier.clickable { pickingTime = true },
                )
                Text("Tap the time to change it", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
            }
        }

        SettingCard {
            Text("Celebrate every", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledTonalButton(onClick = {
                    scope.launch { store.setMilestoneEvery((settings.milestoneEvery - MILESTONE_STEP).coerceAtLeast(MILESTONE_STEP)) }
                }) { Text("−") }
                Text(
                    "${settings.milestoneEvery} days",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                FilledTonalButton(onClick = {
                    scope.launch { store.setMilestoneEvery((settings.milestoneEvery + MILESTONE_STEP).coerceAtMost(500)) }
                }) { Text("+") }
            }
        }

        SettingCard {
            Text("Widget mood", style = MaterialTheme.typography.titleMedium)
            Text(
                "Forces the homescreen widget into one mood so you can check each look. Set back to Live when done.",
                style = MaterialTheme.typography.bodySmall,
                color = c.textMuted,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = settings.moodOverride == null, onClick = { setMood(null) }, label = { Text("Live") })
                Mood.entries.forEach { mood ->
                    FilterChip(
                        selected = settings.moodOverride == mood,
                        onClick = { setMood(mood) },
                        label = { Text(mood.name.lowercase()) },
                        leadingIcon = {
                            Image(painterResource(mood.drawable), contentDescription = null, modifier = Modifier.size(20.dp))
                        },
                    )
                }
            }
        }

        SettingCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Sync", style = MaterialTheme.typography.titleMedium)
                    Text(
                        lastSync?.let { "Last synced " + it.atZone(DubaiTime.ZONE).format(syncFormat) } ?: "Not synced yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textMuted,
                    )
                    syncError?.let { Text("Failed: $it", style = MaterialTheme.typography.bodySmall, color = c.anger) }
                }
                TextButton(onClick = onSync) { Text("Sync now") }
            }
        }

        SettingCard {
            Text("Widget going stale?", style = MaterialTheme.typography.titleMedium)
            Text(
                "Let the app run in the background so the widget keeps up. Find Learning in the list and allow it.",
                style = MaterialTheme.typography.bodySmall,
                color = c.textMuted,
            )
            TextButton(onClick = {
                context.startActivity(Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }) { Text("Open battery settings") }
        }

        Text(
            "Days are counted in Dubai time",
            style = MaterialTheme.typography.bodySmall,
            color = c.textMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (pickingTime) {
        TimeDialog(
            hour = settings.reminderHour,
            minute = settings.reminderMinute,
            onDismiss = { pickingTime = false },
            onPick = { h, m ->
                pickingTime = false
                saveReminder(true, h, m)
            },
        )
    }
}

@Composable
private fun SettingCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(AppTheme.colors.card).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(hour: Int, minute: Int, onDismiss: () -> Unit, onPick: (Int, Int) -> Unit) {
    val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onPick(state.hour, state.minute) }) { Text("Set") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = { TimePicker(state = state) },
    )
}
