package com.lrhsoft.shifterclone.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.lrhsoft.shifterclone.MainActivity

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Alarma Shifter"
        val body = intent.getStringExtra(EXTRA_BODY) ?: "Tienes un recordatorio"
        val noteKey = intent.getStringExtra(EXTRA_NOTE_KEY) ?: ""

        showNotification(context, title, body, noteKey)
    }

    private fun showNotification(context: Context, title: String, body: String, noteKey: String) {
        val channelId = "shifter_alarms"
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Alarmas de turnos y notas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Recordatorios de turnos y notas del calendario"
                enableVibration(true)
            }
            nm.createNotificationChannel(channel)
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NOTE_KEY, noteKey)
        }
        val pending = PendingIntent.getActivity(
            context,
            noteKey.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setSound(sound)
            .setVibrate(longArrayOf(0, 400, 200, 400))
            .setContentIntent(pending)
            .build()

        nm.notify(noteKey.hashCode(), notification)
    }

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_BODY = "body"
        const val EXTRA_NOTE_KEY = "note_key"
    }
}
