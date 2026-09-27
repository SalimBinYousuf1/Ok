package com.example.telephony

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.local.ScheduledMessageEntity

object ScheduledMessageManager {

    const val EXTRA_SCHEDULED_ID = "extra_scheduled_id"
    private const val TAG = "ScheduledMessageManager"

    fun scheduleMessage(context: Context, scheduled: ScheduledMessageEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ScheduledMessageReceiver::class.java).apply {
            putExtra(EXTRA_SCHEDULED_ID, scheduled.id)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            scheduled.id.toInt(),
            intent,
            flags
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    scheduled.scheduledTimestamp,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    scheduled.scheduledTimestamp,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled alarm set for message id: ${scheduled.id} at ${scheduled.scheduledTimestamp}")
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm permission not granted, falling back to inexact alarm", e)
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                scheduled.scheduledTimestamp,
                pendingIntent
            )
        }
    }

    fun cancelScheduledMessage(context: Context, scheduledId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ScheduledMessageReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            scheduledId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
