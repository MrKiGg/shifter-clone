package com.lrhsoft.shifterclone.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lrhsoft.shifterclone.data.ShiftRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = ShiftRepository(context)
                val notes = repo.notesFlow.first()
                val scheduler = AlarmScheduler(context)
                scheduler.rescheduleAll(notes.values)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
