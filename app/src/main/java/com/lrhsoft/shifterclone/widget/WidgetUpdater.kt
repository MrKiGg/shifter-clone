package com.lrhsoft.shifterclone.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Actualiza todos los widgets de la app tras cambiar turnos/asignaciones.
 */
object WidgetUpdater {
    fun refreshAll(context: Context) {
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                MonthCalendarWidget().updateAll(appContext)
                TodayWidget().updateAll(appContext)
                WeekWidget().updateAll(appContext)
            } catch (_: Exception) {
                // Si no hay widgets instalados, updateAll puede fallar de forma benigna
            }
        }
    }
}
