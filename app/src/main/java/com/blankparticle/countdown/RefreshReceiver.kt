package com.blankparticle.countdown

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Fired by the midnight alarm (explicit intent) and after boot, since
 * alarms don't survive a reboot. Refreshes every widget and schedules
 * the next midnight alarm.
 */
class RefreshReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                WidgetRefresher.refreshAll(context)
                MidnightAlarm.schedule(context)
            } finally {
                pending.finish()
            }
        }
    }
}
