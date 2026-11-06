package com.lrhsoft.shifterclone.widget

import android.content.Context
import androidx.compose.runtime.Composable
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
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.lrhsoft.shifterclone.MainActivity
import java.time.LocalDate
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale

class WeekWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetDataLoader.load(context)
        val today = LocalDate.now()
        val days = weekDaysAround(today)

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(Color.White))
                        .padding(8.dp)
                        .clickable(actionStartActivity<MainActivity>())
                ) {
                    Text(
                        text = "Esta semana",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = ColorProvider(Color(0xFFD32F2F))
                        ),
                        modifier = GlanceModifier.padding(bottom = 6.dp)
                    )

                    Row(modifier = GlanceModifier.fillMaxWidth()) {
                        days.forEach { date ->
                            WeekDayCell(
                                date = date,
                                today = today,
                                snapshot = snapshot,
                                modifier = GlanceModifier
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekDayCell(
    date: LocalDate,
    today: LocalDate,
    snapshot: WidgetSnapshot,
    modifier: GlanceModifier
) {
    val shifts = snapshot.shiftsFor(date)
    val bg = when {
        shifts.isNotEmpty() -> Color(shifts.first().color)
        date == today -> Color(0x33D32F2F)
        else -> Color(0x11000000)
    }
    val fg = if (shifts.isNotEmpty()) Color(shifts.first().textColor) else Color(0xFF212121)
    val dayLabel = date.dayOfWeek
        .getDisplayName(DateTextStyle.NARROW, Locale("es", "ES"))
        .uppercase(Locale("es", "ES"))
    val abbr = if (shifts.isEmpty()) "—" else shifts.joinToString("+") { it.abbreviation }

    Column(
        modifier = modifier
            .padding(horizontal = 2.dp)
            .background(ColorProvider(bg))
            .padding(vertical = 6.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = dayLabel,
            style = TextStyle(
                fontSize = 10.sp,
                color = ColorProvider(fg.copy(alpha = 0.8f)),
                textAlign = TextAlign.Center
            )
        )
        Text(
            text = date.dayOfMonth.toString(),
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = if (date == today) FontWeight.Bold else FontWeight.Normal,
                color = ColorProvider(fg),
                textAlign = TextAlign.Center
            )
        )
        Spacer(GlanceModifier.height(2.dp))
        Text(
            text = abbr,
            style = TextStyle(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = ColorProvider(fg),
                textAlign = TextAlign.Center
            )
        )
    }
}

class WeekWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeekWidget()
}
