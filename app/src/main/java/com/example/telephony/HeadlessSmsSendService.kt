package com.example.telephony

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.telephony.TelephonyManager
import android.util.Log
import com.example.data.local.ConversationEntity
import com.example.data.local.MessageEntity
import com.example.data.local.SalimDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HeadlessSmsSendService : Service() {

    companion object {
        private const val TAG = "HeadlessSmsSendService"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        val action = intent.action
        if (TelephonyManager.ACTION_RESPOND_VIA_MESSAGE == action) {
            val uri: Uri? = intent.data
            val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
            val recipient = uri?.schemeSpecificPart ?: ""

            if (recipient.isNotBlank() && text.isNotBlank()) {
                val normalized = PhoneNumberUtils.normalizeNumber(recipient)
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = SalimDatabase.getInstance(applicationContext)
                        var conv = db.conversationDao().getConversationByAddress(normalized)
                        val convId = if (conv == null) {
                            val contact = ContactResolver.resolveContact(applicationContext, normalized)
                            val newConv = ConversationEntity(
                                recipientAddress = normalized,
                                recipientName = contact.name,
                                snippet = text,
                                lastMessageTimestamp = System.currentTimeMillis()
                            )
                            db.conversationDao().insertConversation(newConv)
                        } else {
                            conv.id
                        }

                        val msg = MessageEntity(
                            conversationId = convId,
                            address = normalized,
                            body = text,
                            timestamp = System.currentTimeMillis(),
                            isOutgoing = true,
                            status = "SENDING"
                        )
                        val msgId = db.messageDao().insertMessage(msg)

                        val transport = SmsTransport()
                        transport.sendTextMessage(applicationContext, convId, msgId, normalized, text)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in quick reply headless send", e)
                    } finally {
                        stopSelf(startId)
                    }
                }
                return START_REDELIVER_INTENT
            }
        }

        stopSelf(startId)
        return START_NOT_STICKY
    }
}
