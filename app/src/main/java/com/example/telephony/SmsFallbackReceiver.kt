package com.example.telephony

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import com.example.data.local.ConversationEntity
import com.example.data.local.MessageEntity
import com.example.data.local.SalimDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsFallbackReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsFallbackReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        // If we are already default SMS app, SMS_DELIVER handles this
        if (TelephonyManagerHelper.isDefaultSmsApp(context)) return

        val messages: Array<SmsMessage> = try {
            Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse SMS_RECEIVED PDUs", e)
            return
        }

        if (messages.isEmpty()) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = SalimDatabase.getInstance(context)
                val firstMessage = messages[0]
                val rawAddress = firstMessage.originatingAddress ?: "Unknown"
                val normalizedAddress = PhoneNumberUtils.normalizeNumber(rawAddress)
                val timestamp = firstMessage.timestampMillis

                val bodyBuilder = StringBuilder()
                for (part in messages) {
                    bodyBuilder.append(part.displayMessageBody ?: "")
                }
                val fullBody = bodyBuilder.toString()
                if (fullBody.isBlank()) return@launch

                val existing = db.messageDao().findDuplicate(normalizedAddress, fullBody, timestamp)
                if (existing != null) return@launch

                var conversation = db.conversationDao().getConversationByAddress(normalizedAddress)
                val conversationId: Long
                val contact = ContactResolver.resolveContact(context, normalizedAddress)

                if (conversation == null) {
                    val newConv = ConversationEntity(
                        recipientAddress = normalizedAddress,
                        recipientName = contact.name,
                        snippet = fullBody.take(120),
                        lastMessageTimestamp = timestamp,
                        unreadCount = 1,
                        deliveryStatus = "RECEIVED"
                    )
                    conversationId = db.conversationDao().insertConversation(newConv)
                } else {
                    conversationId = conversation.id
                    val updated = conversation.copy(
                        snippet = fullBody.take(120),
                        lastMessageTimestamp = timestamp,
                        unreadCount = conversation.unreadCount + 1,
                        deliveryStatus = "RECEIVED"
                    )
                    db.conversationDao().updateConversation(updated)
                }

                val otpCode = OtpDetector.extractOtp(fullBody)
                val messageEntity = MessageEntity(
                    conversationId = conversationId,
                    address = normalizedAddress,
                    body = fullBody,
                    timestamp = timestamp,
                    isOutgoing = false,
                    status = "RECEIVED",
                    isRead = false,
                    otpCode = otpCode
                )
                db.messageDao().insertMessage(messageEntity)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
