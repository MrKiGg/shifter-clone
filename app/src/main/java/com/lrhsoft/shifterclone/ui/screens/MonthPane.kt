package com.lrhsoft.shifterclone.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun MonthPane(
    state: CalendarUiState,
    viewModel: CalendarViewModel,
    modifier: Modifier = Modifier
) {
    var dragAccum by remember { mutableFloatStateOf(0f) }
    var displayedMonth by remember { mutableStateOf(state.currentMonth) }
    val rotationY = remember { Animatable(0f) }
    val density = LocalDensity.current.density

    LaunchedEffect(state.currentMonth) {
        if (state.currentMonth == displayedMonth) return@LaunchedEffect
        val forward = state.currentMonth > displayedMonth
        rotationY.animateTo(if (forward) -90f else 90f, tween(180))
        displayedMonth = state.currentMonth
        rotationY.snapTo(if (forward) 90f else -90f)
        rotationY.animateTo(0f, tween(220))
    }
    LaunchedEffect(state.topTab) {
        if (state.topTab == TopTab.MONTH) {
            displayedMonth = state.currentMonth
            rotationY.snapTo(0f)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .pointerInput(state.currentMonth) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        when {
                            dragAccum < -80f -> viewModel.nextMonth()
                            dragAccum > 80f -> viewModel.previousMonth()
                        }
                        dragAccum = 0f
                    },
                    onDragCancel = { dragAccum = 0f },
                    onHorizontalDrag = { _, a -> dragAccum += a }
                )
            }
    ) {
        MonthHeader(
            yearMonth = displayedMonth,
            onPrevious = { viewModel.previousMonth() },
            onNext = { viewModel.nextMonth() }
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp)
                .graphicsLayer {
                    this.rotationY = rotationY.value
                    cameraDistance = 18f * density
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                    val absRot = kotlin.math.abs(rotationY.value)
                    alpha = 1f - (absRot / 90f) * 0.15f
                    scaleX = 1f - (absRot / 90f) * 0.04f
                    scaleY = 1f - (absRot / 90f) * 0.04f
                }
                .border(2.dp, Color.Black)
                .background(Color.White)
        ) {
            MonthGrid(
                modifier = Modifier.fillMaxSize(),
                yearMonth = displayedMonth,
                assignments = state.assignments,
                shifts = state.shifts,
                notes = state.notes,
                selectedRange = state.selectedRange,
                onDayClick = { y, m, d -> viewModel.onDayClicked(y, m, d) },
                onDayLongClick = { y, m, d -> viewModel.onDayLongPressed(y, m, d) }
            )
        }
    }
}
