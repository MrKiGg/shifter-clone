package com.lrhsoft.shifterclone.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lrhsoft.shifterclone.model.Shift

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalendarDayCell(
    dayNumber: Int?,
    isToday: Boolean,
    isCurrentMonth: Boolean,
    isInRange: Boolean = false,
    isRangeEndpoint: Boolean = false,
    hasNote: Boolean = false,
    hasAlarm: Boolean = false,
    noteText: String? = null,
    shifts: List<Shift>,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(0.dp)

    val borderWidth = when {
        isRangeEndpoint -> 2.5.dp
        isInRange -> 1.5.dp
        isToday -> 2.dp
        else -> 0.5.dp
    }
    val borderColor = when {
        isRangeEndpoint -> Color(0xFFD32F2F)
        isInRange -> Color(0xFFD32F2F).copy(alpha = 0.65f)
        isToday -> Color(0xFFD32F2F)
        else -> Color(0xFFBDBDBD)
    }

    val contentColor = when {
        !isCurrentMonth -> Color.Gray.copy(alpha = 0.35f)
        isToday && shifts.isEmpty() -> Color(0xFFD32F2F)
        shifts.isNotEmpty() -> shifts.first().toTextComposeColor()
        else -> Color(0xFF212121)
    }

    Box(
        modifier = modifier
            .border(width = borderWidth, color = borderColor, shape = shape)
            .background(
                when {
                    !isCurrentMonth -> Color(0xFFF5F5F5)
                    isInRange && shifts.isEmpty() -> Color(0xFFD32F2F).copy(alpha = 0.12f)
                    shifts.isNotEmpty() -> shifts.first().toComposeColor()
                    else -> Color.White
                }
            )
            .combinedClickable(
                enabled = dayNumber != null && isCurrentMonth,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .fillMaxSize(),
        contentAlignment = Alignment.TopStart
    ) {
        if (dayNumber != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 3.dp, vertical = 2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dayNumber.toString(),
                        fontSize = 13.sp,
                        fontWeight = if (isToday || isRangeEndpoint) FontWeight.Bold else FontWeight.Medium,
                        color = contentColor,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Start
                    )
                    if (isCurrentMonth && hasAlarm) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFC107))
                        )
                    }
                }

                if (shifts.isNotEmpty() && isCurrentMonth) {
                    shifts.take(2).forEach { shift ->
                        Text(
                            text = shift.abbreviation,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = shift.toTextComposeColor(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                if (isCurrentMonth && !noteText.isNullOrBlank()) {
                    Text(
                        text = noteText,
                        fontSize = 9.sp,
                        color = contentColor.copy(alpha = 0.9f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 10.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 1.dp),
                        textAlign = TextAlign.Start
                    )
                } else if (isCurrentMonth && hasNote) {
                    Box(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(
                                if (shifts.isNotEmpty()) shifts.first().toTextComposeColor()
                                else MaterialTheme.colorScheme.primary
                            )
                            .align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}
