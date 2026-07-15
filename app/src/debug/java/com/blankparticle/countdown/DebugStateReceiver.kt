package com.blankparticle.countdown

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

/**
 * Debug-only: backdates the "created" day of every widget so the progress
 * ring shows partially elapsed time.
 *
 * adb shell am broadcast -a com.blankparticle.countdown.DEBUG_SET_ELAPSED \
 *   -n com.blankparticle.countdown/.DebugStateReceiver --el elapsed_days 30
 */
class DebugStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val elapsedDays = intent.getLongExtra("elapsed_days", 30L)
        runBlocking {
            val manager = GlanceAppWidgetManager(context)
            val widget = CountdownWidget()
            manager.getGlanceIds(CountdownWidget::class.java).forEach { glanceId ->
                updateAppWidgetState(context, glanceId) { prefs ->
                    prefs[CountdownWidget.KEY_CREATED_EPOCH_DAY] =
                        LocalDate.now().toEpochDay() - elapsedDays
                }
                widget.update(context, glanceId)
            }
        }
    }
}
