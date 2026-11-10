package com.ghaleb.learningmanager.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ghaleb.learningmanager.LearningApp
import com.ghaleb.learningmanager.R
import com.ghaleb.learningmanager.mood.MoodCalculator
import com.ghaleb.learningmanager.repository
import com.ghaleb.learningmanager.settings
import com.ghaleb.learningmanager.streak.DubaiTime
import com.ghaleb.learningmanager.widget.ClaudeLauncher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Fires the daily reminder, and re-arms the alarm after boot, clock changes and app updates. */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as LearningApp
        val pending = goAsync()
        app.appScope.launch {
            try {
                val settings = context.settings.current()
                ReminderScheduler.schedule(context, settings)
                if (intent.action == ReminderScheduler.ACTION_FIRE) remindIfNeeded(context)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun remindIfNeeded(context: Context) {
        // Sync first so a session recorded on the laptop minutes ago doesn't trigger a nag. Bounded by goAsync's ~10s.
        withTimeoutOrNull(7_000) { context.repository.sync() }
        val streak = context.repository.currentStreak()
        if (streak.todayDone) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_HIGH),
        )
        val mood = MoodCalculator.compute(DubaiTime.now(), todayDone = false, yesterdayFrozen = false)
        val open = PendingIntent.getActivity(
            context, 0, ClaudeLauncher.intent(context),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(mood.line)
            .setContentText("Tap to open Claude and start today's session")
            .setColor(ContextCompat.getColor(context, R.color.mood_furious))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private companion object {
        const val CHANNEL = "reminder"
        const val NOTIFICATION_ID = 1
    }
}
