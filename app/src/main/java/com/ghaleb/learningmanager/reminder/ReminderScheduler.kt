package com.ghaleb.learningmanager.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ghaleb.learningmanager.data.Settings
import com.ghaleb.learningmanager.streak.DubaiTime
import java.time.ZonedDateTime

object ReminderScheduler {
    const val ACTION_FIRE = "com.ghaleb.learningmanager.REMINDER"

    /** Replaces any pending reminder with the next one per [settings]. The time is Dubai time, like every date here. */
    fun schedule(context: Context, settings: Settings) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val pending = pendingIntent(context)
        alarms.cancel(pending)
        if (!settings.reminderOn) return

        val at = nextTrigger(settings.reminderHour, settings.reminderMinute).toInstant().toEpochMilli()
        val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()
        if (exactAllowed) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        } else {
            // Permission revoked by the user: still remind, just possibly a few minutes late.
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        }
    }

    fun nextTrigger(hour: Int, minute: Int, now: ZonedDateTime = DubaiTime.now()): ZonedDateTime {
        val today = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        return if (today.isAfter(now)) today else today.plusDays(1)
    }

    private fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, ReminderReceiver::class.java).setAction(ACTION_FIRE),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}
