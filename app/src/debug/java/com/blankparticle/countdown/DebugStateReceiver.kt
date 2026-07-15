package com.blankparticle.countdown

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.MutablePreferences
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

/**
 * Debug-only helpers for exercising the widget from adb.
 *
 * Simulate N days passing (target/baseline shift 1 day closer each):
 *   adb shell am broadcast -a com.blankparticle.countdown.DEBUG_DAY_PASSED \
 *     -n com.blankparticle.countdown/.DebugStateReceiver --el days 1
 *
 * Set elapsed time for the progress ring:
 *   adb shell am broadcast -a com.blankparticle.countdown.DEBUG_SET_ELAPSED \
 *     -n com.blankparticle.countdown/.DebugStateReceiver --el elapsed_days 30
 *
 * Run the midnight refresh path (what the alarm triggers):
 *   adb shell am broadcast -a com.blankparticle.countdown.DEBUG_REFRESH \
 *     -n com.blankparticle.countdown/.DebugStateReceiver
 */
class DebugStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        runBlocking {
            when (intent.action) {
                "com.blankparticle.countdown.DEBUG_DAY_PASSED" -> {
                    val days = intent.getLongExtra("days", 1L)
                    updateAll(context) { prefs ->
                        prefs[CountdownWidget.KEY_TARGET_EPOCH_DAY]?.let {
                            prefs[CountdownWidget.KEY_TARGET_EPOCH_DAY] = it - days
                        }
                        prefs[CountdownWidget.KEY_CREATED_EPOCH_DAY]?.let {
                            prefs[CountdownWidget.KEY_CREATED_EPOCH_DAY] = it - days
                        }
                    }
                }
                "com.blankparticle.countdown.DEBUG_SET_ELAPSED" -> {
                    val elapsed = intent.getLongExtra("elapsed_days", 30L)
                    updateAll(context) { prefs ->
                        prefs[CountdownWidget.KEY_CREATED_EPOCH_DAY] =
                            LocalDate.now().toEpochDay() - elapsed
                    }
                }
            }
            // All actions end with the real refresh path (incl. DEBUG_REFRESH)
            WidgetRefresher.refreshAll(context)
            MidnightAlarm.schedule(context)
        }
    }

    private suspend fun updateAll(
        context: Context,
        block: (MutablePreferences) -> Unit
    ) {
        GlanceAppWidgetManager(context)
            .getGlanceIds(CountdownWidget::class.java)
            .forEach { glanceId ->
                updateAppWidgetState(context, glanceId) { prefs -> block(prefs) }
            }
    }
}
