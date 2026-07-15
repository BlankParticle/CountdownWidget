package com.blankparticle.countdown

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private const val MILLIS_PER_DAY = 86_400_000L

/**
 * Widget settings: shown when a widget is added (APPWIDGET_CONFIGURE) and
 * when an existing widget is tapped. Shows the countdown and the chosen date;
 * the pencil opens a date picker.
 */
class ConfigActivity : ComponentActivity() {

    private val scope = MainScope()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        // Cancelled result by default: if the user backs out while adding,
        // the launcher removes the half-configured widget.
        setResult(
            RESULT_CANCELED,
            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        )

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val savedState = runBlocking {
            runCatching {
                val glanceId = GlanceAppWidgetManager(this@ConfigActivity)
                    .getGlanceIdBy(appWidgetId)
                getAppWidgetState(this@ConfigActivity, PreferencesGlanceStateDefinition, glanceId)
            }.getOrNull()
        }
        val savedEpochDay: Long? = savedState?.get(CountdownWidget.KEY_TARGET_EPOCH_DAY)
        val savedTitle: String = savedState?.get(CountdownWidget.KEY_TITLE) ?: ""

        setContent {
            val darkTheme = isSystemInDarkTheme()
            val colorScheme = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                    if (darkTheme) dynamicDarkColorScheme(this)
                    else dynamicLightColorScheme(this)
                darkTheme -> darkColorScheme()
                else -> lightColorScheme()
            }

            MaterialTheme(colorScheme = colorScheme) {
                var epochDay by remember { mutableStateOf(savedEpochDay) }
                // No date yet (widget just added): go straight to the picker
                var showPicker by remember { mutableStateOf(savedEpochDay == null) }

                Scaffold(
                    topBar = { CenterAlignedTopAppBar(title = { Text("Countdown") }) }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        epochDay?.let { day ->
                            val target = LocalDate.ofEpochDay(day)
                            val days = ChronoUnit.DAYS.between(LocalDate.now(), target)
                            val (big, label) = when {
                                days > 0 -> days.toString() to
                                        if (days == 1L) "day left" else "days left"
                                days == 0L -> "🎉" to "Today!"
                                else -> (-days).toString() to
                                        if (days == -1L) "day ago" else "days ago"
                            }

                            Text(
                                text = big,
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontSize = 88.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = target.format(
                                        DateTimeFormatter.ofPattern("d MMMM yyyy")
                                    ),
                                    style = MaterialTheme.typography.titleLarge
                                )
                                IconButton(onClick = { showPicker = true }) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = "Change date",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            var title by remember { mutableStateOf(savedTitle) }
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it.take(40) },
                                label = { Text("Title (optional)") },
                                supportingText = {
                                    Text("Shown on the widget instead of the date")
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            LaunchedEffect(title) {
                                if (title == savedTitle) return@LaunchedEffect
                                delay(500)
                                saveTitle(appWidgetId, title.trim())
                            }
                        }
                    }
                }

                if (showPicker) {
                    val pickerState = rememberDatePickerState(
                        initialSelectedDateMillis =
                            (epochDay ?: LocalDate.now().toEpochDay()) * MILLIS_PER_DAY
                    )
                    DatePickerDialog(
                        onDismissRequest = {
                            showPicker = false
                            if (epochDay == null) finish()
                        },
                        confirmButton = {
                            TextButton(
                                enabled = pickerState.selectedDateMillis != null,
                                onClick = {
                                    val millis = pickerState.selectedDateMillis
                                        ?: return@TextButton
                                    // DatePicker millis are midnight UTC of the picked day
                                    val day = millis / MILLIS_PER_DAY
                                    epochDay = day
                                    showPicker = false
                                    save(appWidgetId, day)
                                }
                            ) { Text("Set date") }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showPicker = false
                                if (epochDay == null) finish()
                            }) { Text("Cancel") }
                        }
                    ) {
                        DatePicker(state = pickerState, showModeToggle = false)
                    }
                }
            }
        }
    }

    private fun saveTitle(appWidgetId: Int, title: String) {
        scope.launch(Dispatchers.Default) {
            val context = this@ConfigActivity
            val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
            updateAppWidgetState(context, glanceId) { prefs ->
                if (title.isEmpty()) prefs.remove(CountdownWidget.KEY_TITLE)
                else prefs[CountdownWidget.KEY_TITLE] = title
            }
            CountdownWidget().update(context, glanceId)
        }
    }

    private fun save(appWidgetId: Int, epochDay: Long) {
        scope.launch(Dispatchers.Default) {
            val context = this@ConfigActivity
            val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
            updateAppWidgetState(context, glanceId) { prefs ->
                prefs[CountdownWidget.KEY_TARGET_EPOCH_DAY] = epochDay
                // Progress ring baseline: the day the countdown was (re)set
                prefs[CountdownWidget.KEY_CREATED_EPOCH_DAY] = LocalDate.now().toEpochDay()
            }
            CountdownWidget().update(context, glanceId)
            MidnightWorker.schedule(context)

            setResult(
                RESULT_OK,
                Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            )
        }
    }
}
