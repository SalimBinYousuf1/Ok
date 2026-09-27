package com.example.telephony

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.util.Log
import com.example.data.local.SalimDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsStatusReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsStatusReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val messageId = intent.getLongExtra(SmsTransport.EXTRA_MESSAGE_ID, -1L)
        val conversationId = intent.getLongExtra(SmsTransport.EXTRA_CONVERSATION_ID, -1L)

        if (messageId == -1L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = SalimDatabase.getInstance(context)
                when (action) {
                    SmsTransport.ACTION_SMS_SENT -> {
                        val isSuccess = (resultCode == Activity.RESULT_OK)
                        val status = if (isSuccess) "SENT" else "FAILED"
                        Log.d(TAG, "SMS Sent status for message $messageId: $status (code: $resultCode)")

                        db.messageDao().updateStatus(messageId, status)
                        if (conversationId != -1L) {
                            val msg = db.messageDao().getMessageById(messageId)
                            if (msg != null) {
                                db.conversationDao().updateLatestMessage(
                                    id = conversationId,
                                    snippet = msg.body.take(120),
                                    timestamp = msg.timestamp,
                                    deliveryStatus = status
                                )
                            }
                        }
                    }

                    SmsTransport.ACTION_SMS_DELIVERED -> {
                        val isDelivered = (resultCode == Activity.RESULT_OK)
                        val status = if (isDelivered) "DELIVERED" else "SENT"
                        Log.d(TAG, "SMS Delivery status for message $messageId: $status (code: $resultCode)")

                        db.messageDao().updateStatus(messageId, status)
                        if (conversationId != -1L) {
                            val msg = db.messageDao().getMessageById(messageId)
                            if (msg != null) {
                                db.conversationDao().updateLatestMessage(
                                    id = conversationId,
                                    snippet = msg.body.take(120),
                                    timestamp = msg.timestamp,
                                    deliveryStatus = status
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating SMS status", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
