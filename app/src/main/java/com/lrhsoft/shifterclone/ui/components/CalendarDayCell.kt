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
    shifts: List<Shift>,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(6.dp)

    val borderWidth = when {
        isRangeEndpoint -> 3.dp
        isInRange -> 2.dp
        isToday -> 2.dp
        else -> 0.dp
    }
    val borderColor = when {
        isRangeEndpoint -> Color(0xFFD32F2F)
        isInRange -> Color(0xFFD32F2F).copy(alpha = 0.7f)
        isToday -> MaterialTheme.colorScheme.primary
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .padding(2.dp)
            .clip(shape)
            .background(
                when {
                    !isCurrentMonth -> Color.Transparent
                    isInRange && shifts.isEmpty() -> Color(0xFFD32F2F).copy(alpha = 0.15f)
                    shifts.isNotEmpty() -> shifts.first().toComposeColor()
                    else -> MaterialTheme.colorScheme.surface
                }
            )
            .border(width = borderWidth, color = borderColor, shape = shape)
            .combinedClickable(
                enabled = dayNumber != null && isCurrentMonth,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (dayNumber != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp)
            ) {
                Text(
                    text = dayNumber.toString(),
                    fontSize = 12.sp,
                    fontWeight = if (isToday || isRangeEndpoint) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        !isCurrentMonth -> Color.Gray.copy(alpha = 0.4f)
                        shifts.isNotEmpty() -> shifts.first().toTextComposeColor()
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                if (shifts.isNotEmpty() && isCurrentMonth) {
                    shifts.take(2).forEach { shift ->
                        Text(
                            text = shift.abbreviation,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = shift.toTextComposeColor(),
                            maxLines = 1
                        )
                    }
                }

                if (isCurrentMonth && (hasNote || hasAlarm)) {
                    Row(
                        modifier = Modifier.padding(top = 1.dp),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(3.dp)
                    ) {
                        if (hasNote) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (shifts.isNotEmpty()) shifts.first().toTextComposeColor()
                                        else MaterialTheme.colorScheme.primary
                                    )
                            )
                        }
                        if (hasAlarm) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFC107))
                            )
                        }
                    }
                }
            }
        }
    }
}
