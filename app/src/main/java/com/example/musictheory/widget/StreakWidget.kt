package com.example.musictheory.widget
import android.content.Context; import android.content.Intent
import androidx.glance.GlanceId; import androidx.glance.GlanceModifier
import androidx.glance.action.clickable; import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.background; import androidx.glance.layout.*
import androidx.glance.text.*; import androidx.glance.unit.ColorProvider
import com.example.musictheory.MainActivity; import com.example.musictheory.R
import com.example.musictheory.data.ProgressRepo; import com.example.musictheory.data.SettingsRepo
import kotlinx.coroutines.flow.first
class StreakWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val streak = SettingsRepo.streakFlow(context).first()
        val due = ProgressRepo.dueCount(context).first()
        provideContent {
            Box(GlanceModifier.fillMaxSize().background(ColorProvider(R.color.widget_bg, R.color.widget_bg))
                .cornerRadius(16.dp()).clickable(actionStartActivity(Intent(context, MainActivity::class.java)
                    .putExtra("route", "daily").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))).padding(12.dp)) {
                Column { Text("🔥 $streak-day streak", style = TextStyle(
                        color = ColorProvider(android.graphics.Color.WHITE), fontSize = 16.sp, fontWeight = FontWeight.Bold))
                    Text(if (due > 0) "Daily challenge + $due reviews due" else "Daily challenge — keep it going!",
                        style = TextStyle(color = ColorProvider(0xFFE0DEF4.toInt()), fontSize = 12.sp)) } } } }
}
class StreakWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = StreakWidget() }
suspend fun refreshWidget(context: Context) { runCatching { StreakWidget().updateAll(context) } }
