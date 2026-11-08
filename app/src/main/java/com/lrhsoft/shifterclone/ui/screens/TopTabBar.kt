package com.lrhsoft.shifterclone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun TopTabBar(
    selected: TopTab,
    currentMonth: YearMonth,
    year: Int,
    onSelect: (TopTab) -> Unit
) {
    val monthLabel = currentMonth.month
        .getDisplayName(TextStyle.FULL, Locale("es", "ES"))
        .uppercase(Locale("es", "ES"))
    val selectedColor = Color(0xFF00897B)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF37474F))
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(
            TopTab.MONTH to monthLabel,
            TopTab.YEAR to year.toString(),
            TopTab.SUMMARY to "RESUMEN"
        ).forEach { (tab, label) ->
            val isSelected = selected == tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(tab) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = label,
                        color = Color.White,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(3.dp)
                            .background(if (isSelected) selectedColor else Color.Transparent)
                    )
                }
            }
        }
    }
}
