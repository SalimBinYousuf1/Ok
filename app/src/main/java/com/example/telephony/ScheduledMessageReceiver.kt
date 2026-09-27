package com.example.telephony

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.MessageEntity
import com.example.data.local.SalimDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScheduledMessageReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ScheduledMessageReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val scheduledId = intent.getLongExtra(ScheduledMessageManager.EXTRA_SCHEDULED_ID, -1L)
        if (scheduledId == -1L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = SalimDatabase.getInstance(context)
                val scheduled = db.scheduledMessageDao().getScheduledMessageById(scheduledId)
                if (scheduled != null && scheduled.status == "PENDING") {
                    Log.d(TAG, "Triggering scheduled message id: $scheduledId to ${scheduled.recipientAddress}")

                    // Insert message record
                    val message = MessageEntity(
                        conversationId = scheduled.conversationId,
                        address = scheduled.recipientAddress,
                        body = scheduled.body,
                        timestamp = System.currentTimeMillis(),
                        isOutgoing = true,
                        status = "SENDING",
                        isScheduled = true,
                        scheduledTime = scheduled.scheduledTimestamp
                    )
                    val messageId = db.messageDao().insertMessage(message)

                    // Update conversation
                    db.conversationDao().updateLatestMessage(
                        id = scheduled.conversationId,
                        snippet = scheduled.body.take(120),
                        timestamp = message.timestamp,
                        deliveryStatus = "SENDING"
                    )

                    // Send SMS
                    val transport = SmsTransport()
                    val result = transport.sendTextMessage(
                        context = context,
                        conversationId = scheduled.conversationId,
                        messageId = messageId,
                        recipientAddress = scheduled.recipientAddress,
                        body = scheduled.body
                    )

                    if (result.isSuccess) {
                        db.scheduledMessageDao().updateStatus(scheduledId, "SENT")
                    } else {
                        db.scheduledMessageDao().updateStatus(scheduledId, "FAILED")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error executing scheduled message alarm", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
