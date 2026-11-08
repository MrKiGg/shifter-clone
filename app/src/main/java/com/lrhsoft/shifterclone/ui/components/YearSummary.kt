package com.lrhsoft.shifterclone.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lrhsoft.shifterclone.model.StatsSummary
import java.util.Locale

@Composable
fun YearSummary(
    year: Int,
    summary: StatsSummary,
    onPreviousYear: () -> Unit,
    onNextYear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
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
                Text("${year - 1}", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onNextYear,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("${year + 1}", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = "Resumen $year",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF212121)
        )
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                title = "Horas",
                value = String.format(Locale.getDefault(), "%.1f", summary.totalHours),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Días con turno",
                value = summary.workDays.toString(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Ganancias",
                value = String.format(Locale.getDefault(), "%.0f €", summary.totalEarnings),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = "Por turno",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Spacer(Modifier.height(8.dp))

        if (summary.byShift.isEmpty()) {
            Text(
                text = "No hay turnos registrados este año.",
                color = Color.Gray,
                modifier = Modifier.padding(vertical = 24.dp)
            )
        } else {
            summary.byShift.forEach { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(item.shift.color)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.shift.abbreviation,
                                color = Color(item.shift.textColor),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.shift.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "${item.count} día(s) · ${String.format(Locale.getDefault(), "%.1f", item.totalHours)} h",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                        if (item.totalEarnings > 0) {
                            Text(
                                text = String.format(Locale.getDefault(), "%.0f €", item.totalEarnings),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 11.sp, color = Color.Gray)
            Spacer(Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}
