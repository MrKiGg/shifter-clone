package com.lrhsoft.shifterclone.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.lrhsoft.shifterclone.MainActivity
import java.time.LocalDate
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale

class TodayWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetDataLoader.load(context)
        val today = LocalDate.now()
        val shifts = snapshot.shiftsFor(today)
        val dayName = today.dayOfWeek
            .getDisplayName(DateTextStyle.SHORT, Locale("es", "ES"))
            .replaceFirstChar { it.uppercase() }

        val bg = if (shifts.isNotEmpty()) Color(shifts.first().color) else Color(0xFF455A64)
        val fg = if (shifts.isNotEmpty()) Color(shifts.first().textColor) else Color.White
        val title = if (shifts.isEmpty()) {
            "Libre / sin turno"
        } else {
            shifts.joinToString(" · ") { "${it.abbreviation} ${it.name}" }
        }
        val schedule = shifts.mapNotNull { s ->
            if (s.startHour != null && s.endHour != null) {
                val sm = (s.startMinute ?: 0).toString().padStart(2, '0')
                val em = (s.endMinute ?: 0).toString().padStart(2, '0')
                "${s.startHour}:$sm–${s.endHour}:$em"
            } else null
        }.joinToString(" · ")

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(bg, bg))
                        .padding(12.dp)
                        .clickable(actionStartActivity<MainActivity>()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$dayName ${today.dayOfMonth}/${today.monthValue}",
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = ColorProvider(fg.copy(alpha = 0.85f), fg.copy(alpha = 0.85f))
                        )
                    )
                    Text(
                        text = title,
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(fg, fg)
                        )
                    )
                    if (schedule.isNotEmpty()) {
                        Text(
                            text = schedule,
                            style = TextStyle(
                                fontSize = 12.sp,
                                color = ColorProvider(fg.copy(alpha = 0.9f), fg.copy(alpha = 0.9f))
                            )
                        )
                    }
                }
            }
        }
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}
