package com.lrhsoft.shifterclone.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lrhsoft.shifterclone.model.DayAssignment
import com.lrhsoft.shifterclone.model.Shift
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun YearOverview(
    year: Int,
    assignments: Map<String, DayAssignment>,
    shifts: List<Shift>,
    onPreviousYear: () -> Unit,
    onNextYear: () -> Unit,
    onMonthClick: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val shiftMap = shifts.associateBy { it.id }
    val scroll = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .verticalScroll(scroll)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onPreviousYear,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("${year - 1}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            OutlinedButton(
                onClick = onNextYear,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("${year + 1}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        for (row in 0 until 6) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (col in 0..1) {
                    val month = row * 2 + col + 1
                    MiniMonth(
                        year = year,
                        month = month,
                        assignments = assignments,
                        shiftMap = shiftMap,
                        onClick = { onMonthClick(year, month) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun MiniMonth(
    year: Int,
    month: Int,
    assignments: Map<String, DayAssignment>,
    shiftMap: Map<String, Shift>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ym = YearMonth.of(year, month)
    val monthName = ym.month
        .getDisplayName(TextStyle.FULL, Locale("es", "ES"))
        .uppercase(Locale("es", "ES"))

    val first = ym.atDay(1)
    val startOffset = (first.dayOfWeek.value - 1) % 7
    val daysInMonth = ym.lengthOfMonth()
    val cells = mutableListOf<LocalDate?>()
    for (i in 0 until startOffset) cells.add(null)
    for (d in 1..daysInMonth) cells.add(ym.atDay(d))
    while (cells.size % 7 != 0) cells.add(null)
    val weeks = cells.chunked(7)
    val weekDays = listOf("L", "M", "X", "J", "V", "S", "D")

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(2.dp)
    ) {
        Text(
            text = monthName,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = Color(0xFF212121),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            textAlign = TextAlign.Center
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            weekDays.forEach { d ->
                Text(
                    text = d,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 9.sp,
                    color = Color(0xFF757575),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        weeks.forEach { week ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
            ) {
                week.forEach { date ->
                    val key = if (date != null) {
                        "${date.year}-${date.monthValue}-${date.dayOfMonth}"
                    } else null
                    val dayShifts = key?.let { assignments[it] }
                        ?.shiftIds
                        ?.mapNotNull { shiftMap[it] }
                        .orEmpty()
                    val bg = when {
                        date == null -> Color.Transparent
                        dayShifts.isNotEmpty() -> dayShifts.first().toComposeColor()
                        else -> Color.White
                    }
                    val borderC = if (date != null) Color(0xFFBDBDBD) else Color.Transparent

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .padding(0.5.dp)
                            .border(0.5.dp, borderC)
                            .background(bg),
                        contentAlignment = Alignment.Center
                    ) {
                        if (date != null) {
                            Text(
                                text = date.dayOfMonth.toString(),
                                fontSize = 8.sp,
                                color = when {
                                    dayShifts.isNotEmpty() -> dayShifts.first().toTextComposeColor()
                                    else -> Color(0xFF424242)
                                },
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
