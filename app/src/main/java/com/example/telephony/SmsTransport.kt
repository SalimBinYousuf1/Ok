package com.example.telephony

import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log

interface MessagingTransport {
    suspend fun sendTextMessage(
        context: Context,
        conversationId: Long,
        messageId: Long,
        recipientAddress: String,
        body: String
    ): Result<Unit>

    fun isAvailable(context: Context): Boolean
}

class SmsTransport : MessagingTransport {

    companion object {
        const val ACTION_SMS_SENT = "com.example.salim.ACTION_SMS_SENT"
        const val ACTION_SMS_DELIVERED = "com.example.salim.ACTION_SMS_DELIVERED"
        const val EXTRA_MESSAGE_ID = "extra_message_id"
        const val EXTRA_CONVERSATION_ID = "extra_conversation_id"
        const val EXTRA_ADDRESS = "extra_address"
        private const val TAG = "SmsTransport"
    }

    override fun isAvailable(context: Context): Boolean {
        return TelephonyManagerHelper.hasSimCard(context)
    }

    @Suppress("DEPRECATION")
    private fun getSmsManager(context: Context): SmsManager {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java) ?: SmsManager.getDefault()
        } else {
            SmsManager.getDefault()
        }
    }

    override suspend fun sendTextMessage(
        context: Context,
        conversationId: Long,
        messageId: Long,
        recipientAddress: String,
        body: String
    ): Result<Unit> {
        return try {
            val smsManager = getSmsManager(context)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

            val sentIntent = Intent(context, SmsStatusReceiver::class.java).apply {
                action = ACTION_SMS_SENT
                putExtra(EXTRA_MESSAGE_ID, messageId)
                putExtra(EXTRA_CONVERSATION_ID, conversationId)
                putExtra(EXTRA_ADDRESS, recipientAddress)
            }
            val sentPendingIntent = PendingIntent.getBroadcast(
                context,
                messageId.toInt(),
                sentIntent,
                flags
            )

            val deliveryIntent = Intent(context, SmsStatusReceiver::class.java).apply {
                action = ACTION_SMS_DELIVERED
                putExtra(EXTRA_MESSAGE_ID, messageId)
                putExtra(EXTRA_CONVERSATION_ID, conversationId)
                putExtra(EXTRA_ADDRESS, recipientAddress)
            }
            val deliveryPendingIntent = PendingIntent.getBroadcast(
                context,
                (messageId + 100000).toInt(),
                deliveryIntent,
                flags
            )

            val parts = smsManager.divideMessage(body)
            if (parts.size > 1) {
                val sentIntents = ArrayList<PendingIntent>().apply {
                    for (i in parts.indices) add(sentPendingIntent)
                }
                val deliveryIntents = ArrayList<PendingIntent>().apply {
                    for (i in parts.indices) add(deliveryPendingIntent)
                }
                smsManager.sendMultipartTextMessage(
                    recipientAddress,
                    null,
                    parts,
                    sentIntents,
                    deliveryIntents
                )
            } else {
                smsManager.sendTextMessage(
                    recipientAddress,
                    null,
                    body,
                    sentPendingIntent,
                    deliveryPendingIntent
                )
            }

            // Sync with Android Telephony Provider if permitted/default
            writeSentSmsToPlatform(context, recipientAddress, body)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending SMS to $recipientAddress", e)
            Result.failure(e)
        }
    }

    private fun writeSentSmsToPlatform(context: Context, address: String, body: String) {
        try {
            val values = ContentValues().apply {
                put(Telephony.Sms.ADDRESS, address)
                put(Telephony.Sms.BODY, body)
                put(Telephony.Sms.DATE, System.currentTimeMillis())
                put(Telephony.Sms.READ, 1)
                put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT)
            }
            context.contentResolver.insert(Telephony.Sms.Sent.CONTENT_URI, values)
        } catch (e: Exception) {
            Log.w(TAG, "Could not write sent SMS to platform provider", e)
        }
    }
}
