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
import androidx.glance.layout.defaultWeight
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.lrhsoft.shifterclone.MainActivity
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale

class MonthCalendarWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetDataLoader.load(context)
        val yearMonth = YearMonth.now()
        val today = LocalDate.now()
        val cells = monthCells(yearMonth)
        val monthName = yearMonth.month
            .getDisplayName(DateTextStyle.FULL, Locale("es", "ES"))
            .replaceFirstChar { it.uppercase() }

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(Color.White, Color(0xFF1E1E1E)))
                        .padding(8.dp)
                        .clickable(actionStartActivity<MainActivity>())
                ) {
                    Text(
                        text = "$monthName ${yearMonth.year}",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = ColorProvider(Color(0xFFD32F2F), Color(0xFFEF5350))
                        ),
                        modifier = GlanceModifier.fillMaxWidth().padding(bottom = 4.dp)
                    )

                    // Cabecera L M X J V S D
                    Row(modifier = GlanceModifier.fillMaxWidth()) {
                        listOf("L", "M", "X", "J", "V", "S", "D").forEach { d ->
                            Text(
                                text = d,
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    color = ColorProvider(Color.Gray, Color.LightGray)
                                ),
                                modifier = GlanceModifier.defaultWeight()
                            )
                        }
                    }

                    Spacer(GlanceModifier.height(2.dp))

                    cells.chunked(7).forEach { week ->
                        Row(modifier = GlanceModifier.fillMaxWidth()) {
                            week.forEach { date ->
                                MonthDayCell(
                                    date = date,
                                    currentMonth = yearMonth,
                                    today = today,
                                    snapshot = snapshot,
                                    modifier = GlanceModifier.defaultWeight()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthDayCell(
    date: LocalDate?,
    currentMonth: YearMonth,
    today: LocalDate,
    snapshot: WidgetSnapshot,
    modifier: GlanceModifier
) {
    if (date == null) {
        Spacer(modifier = modifier.height(28.dp))
        return
    }
    val inMonth = date.month == currentMonth.month
    val shifts = if (inMonth) snapshot.shiftsFor(date) else emptyList()
    val bg = when {
        !inMonth -> Color(0x00000000)
        shifts.isNotEmpty() -> Color(shifts.first().color)
        date == today -> Color(0x33D32F2F)
        else -> Color(0x00000000)
    }
    val textColor = when {
        !inMonth -> Color.Gray.copy(alpha = 0.4f)
        shifts.isNotEmpty() -> Color(shifts.first().textColor)
        else -> Color(0xFF212121)
    }
    val abbr = if (inMonth && shifts.isNotEmpty()) shifts.first().abbreviation else ""

    Column(
        modifier = modifier
            .padding(1.dp)
            .background(ColorProvider(bg, bg))
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = TextStyle(
                fontSize = 11.sp,
                fontWeight = if (date == today) FontWeight.Bold else FontWeight.Normal,
                color = ColorProvider(textColor, textColor),
                textAlign = TextAlign.Center
            )
        )
        if (abbr.isNotEmpty()) {
            Text(
                text = abbr,
                style = TextStyle(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(textColor, textColor),
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

class MonthCalendarWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MonthCalendarWidget()
}
