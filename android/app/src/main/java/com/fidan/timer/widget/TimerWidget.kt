package com.fidan.timer.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.fidan.timer.MainActivity
import com.fidan.timer.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class TimerWidget : GlanceAppWidget() {
    
    companion object {
        private val countdownKey = stringPreferencesKey("countdown")
        private val isRunningKey = booleanPreferencesKey("isRunning")
        private val progressKey = floatPreferencesKey("progress")
    }
    
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            TimerWidgetContent()
        }
    }
    
    @Composable
    fun TimerWidgetContent() {
        val prefs = currentState<Preferences>()
        val countdown = prefs[countdownKey] ?: "25:00"
        val isRunning = prefs[isRunningKey] ?: false
        val progress = prefs[progressKey] ?: 0f
        
        GlanceTheme {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(Color(0xFF1B5E20)))
                    .cornerRadius(16.dp)
                    .clickable(actionRunCallback<OpenAppAction>()),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = GlanceModifier.fillMaxSize().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // App Name
                    Text(
                        text = "Fidan Focus",
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    
                    // Timer Display
                    Box(
                        modifier = GlanceModifier
                            .size(120.dp)
                            .background(
                                ColorProvider(Color(0x33FFFFFF))
                            )
                            .cornerRadius(60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Tree icon placeholder
                            Text(
                                text = getTreeEmoji(progress),
                                style = TextStyle(
                                    fontSize = 24.sp
                                )
                            )
                            
                            Spacer(modifier = GlanceModifier.height(4.dp))
                            
                            // Timer text
                            Text(
                                text = countdown,
                                style = TextStyle(
                                    color = ColorProvider(Color.White),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                    
                    Spacer(modifier = GlanceModifier.height(12.dp))
                    
                    // Control Buttons
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (!isRunning) {
                            Button(
                                text = "Start",
                                onClick = actionRunCallback<StartTimerAction>(),
                                modifier = GlanceModifier
                                    .width(80.dp)
                                    .height(36.dp)
                            )
                        } else {
                            Button(
                                text = "Pause",
                                onClick = actionRunCallback<PauseTimerAction>(),
                                modifier = GlanceModifier
                                    .width(80.dp)
                                    .height(36.dp)
                            )
                        }
                        
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        
                        Button(
                            text = "Reset",
                            onClick = actionRunCallback<ResetTimerAction>(),
                            modifier = GlanceModifier
                                .width(80.dp)
                                .height(36.dp)
                        )
                    }
                    
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    
                    // Progress bar
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(ColorProvider(Color(0x33FFFFFF)))
                            .cornerRadius(2.dp)
                    ) {
                        Box(
                            modifier = GlanceModifier
                                .fillMaxWidth(progress)
                                .height(4.dp)
                                .background(ColorProvider(Color(0xFF4CAF50)))
                                .cornerRadius(2.dp)
                        )
                    }
                }
            }
        }
    }
    
    private fun getTreeEmoji(progress: Float): String {
        return when {
            progress < 0.25f -> "🌱"
            progress < 0.5f -> "🌿"
            progress < 0.75f -> "🌳"
            progress < 1.0f -> "🌲"
            else -> "🌳"
        }
    }
}

class TimerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TimerWidget()
    
    companion object {
        const val ACTION_UPDATE_TIMER = "com.fidan.timer.widget.UPDATE_TIMER"
        const val EXTRA_COUNTDOWN = "countdown"
        const val EXTRA_IS_RUNNING = "isRunning"
        const val EXTRA_PROGRESS = "progress"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        if (intent.action == ACTION_UPDATE_TIMER) {
            val countdown = intent.getStringExtra(EXTRA_COUNTDOWN) ?: "25:00"
            val isRunning = intent.getBooleanExtra(EXTRA_IS_RUNNING, false)
            val progress = intent.getFloatExtra(EXTRA_PROGRESS, 0f)
            
            GlobalScope.launch(Dispatchers.IO) {
                val manager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, TimerWidgetReceiver::class.java)
                val widgetIds = manager.getAppWidgetIds(componentName)
                
                widgetIds.forEach { widgetId ->
                    updateAppWidgetState(context, GlanceId(widgetId)) { prefs ->
                        prefs[TimerWidget.countdownKey] = countdown
                        prefs[TimerWidget.isRunningKey] = isRunning
                        prefs[TimerWidget.progressKey] = progress
                    }
                    glanceAppWidget.update(context, GlanceId(widgetId))
                }
            }
        }
    }
}

class OpenAppAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(intent)
    }
}

class StartTimerAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = "com.fidan.timer.START"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}

class PauseTimerAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = "com.fidan.timer.PAUSE"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}

class ResetTimerAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = "com.fidan.timer.RESET"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}

// Legacy AppWidgetProvider for compatibility
class TimerWidgetProvider : AppWidgetProvider() {
    
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
    
    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val views = RemoteViews(context.packageName, R.layout.widget_timer).apply {
                setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            }
            
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}