package com.blankparticle.countdown

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import java.time.LocalDate

class CountdownWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    companion object {
        val KEY_TARGET_EPOCH_DAY = longPreferencesKey("target_epoch_day")
        val KEY_CREATED_EPOCH_DAY = longPreferencesKey("created_epoch_day")
        val KEY_TITLE = stringPreferencesKey("title")
        val KEY_LAST_REFRESH_DAY = longPreferencesKey("last_refresh_day")
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)

        provideContent {
            WidgetContent(context, appWidgetId)
        }
    }

    @Composable
    private fun WidgetContent(context: Context, appWidgetId: Int) {
        val prefs = currentState<Preferences>()
        val targetEpochDay = prefs[KEY_TARGET_EPOCH_DAY]
        val createdEpochDay = prefs[KEY_CREATED_EPOCH_DAY]
        val title = prefs[KEY_TITLE]

        val size = LocalSize.current
        val density = context.resources.displayMetrics.density
        val sizePx = (minOf(size.width.value, size.height.value) * density)
            .toInt().coerceAtLeast(64)
        val today = LocalDate.now()

        val layers = remember(sizePx, targetEpochDay, createdEpochDay, title, today) {
            WidgetRenderer.render(context, sizePx, targetEpochDay, createdEpochDay, title)
        }

        val settingsIntent = Intent(context, ConfigActivity::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_CONFIGURE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            // Unique data URI so PendingIntents for different widgets don't collide
            data = Uri.parse("countdown://widget/$appWidgetId")
        }

        GlanceTheme {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .clickable(
                        onClick = actionStartActivity(settingsIntent),
                        rippleOverride = R.drawable.no_ripple
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(layers.shape),
                    contentDescription = "Countdown widget",
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.widgetBackground),
                    modifier = GlanceModifier.fillMaxSize()
                )
                Image(
                    provider = ImageProvider(layers.content),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurface),
                    modifier = GlanceModifier.fillMaxSize()
                )
                layers.progress?.let { progress ->
                    Image(
                        provider = ImageProvider(progress),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
                        modifier = GlanceModifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
