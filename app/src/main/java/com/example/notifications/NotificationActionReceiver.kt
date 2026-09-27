package com.example.notifications

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.RemoteInput
import com.example.data.local.MessageEntity
import com.example.data.local.SalimDatabase
import com.example.telephony.SmsTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_INLINE_REPLY = "com.example.salim.ACTION_INLINE_REPLY"
        const val ACTION_MARK_READ = "com.example.salim.ACTION_MARK_READ"
        const val ACTION_COPY_OTP = "com.example.salim.ACTION_COPY_OTP"

        const val KEY_TEXT_REPLY = "key_text_reply"
        const val EXTRA_CONVERSATION_ID = "extra_conversation_id"
        const val EXTRA_ADDRESS = "extra_address"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_OTP_CODE = "extra_otp_code"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val db = SalimDatabase.getInstance(context)
        val conversationId = intent.getLongExtra(EXTRA_CONVERSATION_ID, -1L)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        when (action) {
            ACTION_INLINE_REPLY -> {
                val results = RemoteInput.getResultsFromIntent(intent) ?: return
                val replyText = results.getCharSequence(KEY_TEXT_REPLY)?.toString()?.trim() ?: return
                val address = intent.getStringExtra(EXTRA_ADDRESS) ?: return

                if (replyText.isNotEmpty() && conversationId != -1L) {
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val msg = MessageEntity(
                                conversationId = conversationId,
                                address = address,
                                body = replyText,
                                timestamp = System.currentTimeMillis(),
                                isOutgoing = true,
                                status = "SENDING",
                                isRead = true
                            )
                            val msgId = db.messageDao().insertMessage(msg)
                            db.conversationDao().updateLatestMessage(
                                id = conversationId,
                                snippet = replyText,
                                timestamp = msg.timestamp,
                                deliveryStatus = "SENDING"
                            )

                            // Send via SmsTransport
                            val transport = SmsTransport()
                            transport.sendTextMessage(context, conversationId, msgId, address, replyText)

                            if (notificationId != -1) {
                                NotificationCoordinator.dismissNotification(context, notificationId)
                            }
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }

            ACTION_MARK_READ -> {
                if (conversationId != -1L) {
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            db.conversationDao().markAllAsRead(conversationId)
                            db.messageDao().markMessagesAsReadForConversation(conversationId)
                            if (notificationId != -1) {
                                NotificationCoordinator.dismissNotification(context, notificationId)
                            }
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }

            ACTION_COPY_OTP -> {
                val code = intent.getStringExtra(EXTRA_OTP_CODE)
                if (!code.isNullOrBlank()) {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    val clip = ClipData.newPlainText("Salim Verification Code", code)
                    clipboard?.setPrimaryClip(clip)
                    Toast.makeText(context, "Code copied: $code", Toast.LENGTH_SHORT).show()
                    if (notificationId != -1) {
                        NotificationCoordinator.dismissNotification(context, notificationId)
                    }
                }
            }
        }
    }
}
