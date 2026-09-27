package com.example.telephony

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import com.example.data.local.ConversationEntity
import com.example.data.local.MessageEntity
import com.example.data.local.SalimDatabase
import com.example.notifications.NotificationCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) return

        val messages: Array<SmsMessage> = try {
            Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse SMS PDUs", e)
            return
        }

        if (messages.isEmpty()) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                processIncomingMessages(context, messages)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing incoming SMS", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun processIncomingMessages(context: Context, messages: Array<SmsMessage>) {
        val db = SalimDatabase.getInstance(context)
        val firstMessage = messages[0]
        val rawAddress = firstMessage.originatingAddress ?: "Unknown"
        val normalizedAddress = PhoneNumberUtils.normalizeNumber(rawAddress)
        val timestamp = firstMessage.timestampMillis

        // Combine multipart message parts into full text
        val bodyBuilder = StringBuilder()
        for (part in messages) {
            bodyBuilder.append(part.displayMessageBody ?: "")
        }
        val fullBody = bodyBuilder.toString()
        if (fullBody.isBlank()) return

        // Deduplication check
        val existing = db.messageDao().findDuplicate(normalizedAddress, fullBody, timestamp)
        if (existing != null) {
            Log.d(TAG, "Duplicate message detected, skipping insert")
            return
        }

        // Find or create conversation
        var conversation = db.conversationDao().getConversationByAddress(normalizedAddress)
        val conversationId: Long
        val contact = ContactResolver.resolveContact(context, normalizedAddress)

        if (conversation == null) {
            val newConversation = ConversationEntity(
                recipientAddress = normalizedAddress,
                recipientName = contact.name,
                snippet = fullBody.take(120),
                lastMessageTimestamp = timestamp,
                unreadCount = 1,
                deliveryStatus = "RECEIVED"
            )
            conversationId = db.conversationDao().insertConversation(newConversation)
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

        // Extract OTP if present
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

        // Write to platform SMS inbox so system database is in sync
        writeIncomingSmsToPlatform(context, normalizedAddress, fullBody, timestamp)

        // Check if conversation is muted
        if (conversation?.isMuted != true) {
            NotificationCoordinator.showIncomingMessageNotification(
                context = context,
                conversationId = conversationId,
                senderAddress = normalizedAddress,
                senderName = contact.name,
                body = fullBody,
                otpCode = otpCode
            )
        }
    }

    private fun writeIncomingSmsToPlatform(context: Context, address: String, body: String, timestamp: Long) {
        try {
            val values = ContentValues().apply {
                put(Telephony.Sms.ADDRESS, address)
                put(Telephony.Sms.BODY, body)
                put(Telephony.Sms.DATE, timestamp)
                put(Telephony.Sms.READ, 0)
                put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX)
            }
            context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)
        } catch (e: Exception) {
            Log.w(TAG, "Could not write to platform inbox", e)
        }
    }
}
