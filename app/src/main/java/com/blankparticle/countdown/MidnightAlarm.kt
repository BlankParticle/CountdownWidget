package com.blankparticle.countdown

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.LocalDate
import java.time.ZoneId

/**
 * Schedules the just-past-midnight widget refresh with AlarmManager.
 * WorkManager is unsuitable here: a widget-only app is never foregrounded,
 * so App Standby buckets defer its jobs by hours. While-idle alarms fire
 * on time regardless of the bucket.
 */
object MidnightAlarm {

    fun schedule(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        // A minute past local midnight so the date has definitely changed
        val trigger = LocalDate.now().plusDays(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant().toEpochMilli() + 60_000
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP, trigger, pendingIntent(context)
        )
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java)
            ?.cancel(pendingIntent(context))
    }

    private fun pendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context, 0,
            Intent(context, RefreshReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}
