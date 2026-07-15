package com.blankparticle.countdown

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import java.time.LocalDate

object WidgetRefresher {

    suspend fun refreshAll(context: Context) {
        val widget = CountdownWidget()
        GlanceAppWidgetManager(context)
            .getGlanceIds(CountdownWidget::class.java)
            .forEach { glanceId ->
                // Touch per-widget state so the composition is guaranteed to
                // re-run: the current date isn't observable snapshot state, so
                // a live cached session would otherwise skip recomposition.
                updateAppWidgetState(context, glanceId) { prefs ->
                    prefs[CountdownWidget.KEY_LAST_REFRESH_DAY] =
                        LocalDate.now().toEpochDay()
                }
                widget.update(context, glanceId)
            }
    }
}
