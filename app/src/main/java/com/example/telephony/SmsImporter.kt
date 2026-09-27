package com.example.telephony

import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.example.data.local.ConversationDao
import com.example.data.local.ConversationEntity
import com.example.data.local.MessageDao
import com.example.data.local.MessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

sealed class ImportProgress {
    object Idle : ImportProgress()
    data class InProgress(val current: Int, val total: Int) : ImportProgress()
    data class Completed(val importedCount: Int) : ImportProgress()
    data class Error(val message: String) : ImportProgress()
}

class SmsImporter(
    private val context: Context,
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao
) {
    private val _progress = MutableStateFlow<ImportProgress>(ImportProgress.Idle)
    val progress = _progress.asStateFlow()

    suspend fun importPlatformSms(): Int = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            _progress.value = ImportProgress.Error("SMS permission not granted")
            return@withContext 0
        }

        try {
            val projection = arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.THREAD_ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.TYPE,
                Telephony.Sms.READ
            )
            val uri = Telephony.Sms.CONTENT_URI
            val cursor: Cursor? = context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${Telephony.Sms.DATE} DESC"
            )

            if (cursor == null) {
                _progress.value = ImportProgress.Completed(0)
                return@withContext 0
            }

            val total = cursor.count
            _progress.value = ImportProgress.InProgress(0, total)

            val idIdx = cursor.getColumnIndex(Telephony.Sms._ID)
            val threadIdIdx = cursor.getColumnIndex(Telephony.Sms.THREAD_ID)
            val addressIdx = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIdx = cursor.getColumnIndex(Telephony.Sms.BODY)
            val dateIdx = cursor.getColumnIndex(Telephony.Sms.DATE)
            val typeIdx = cursor.getColumnIndex(Telephony.Sms.TYPE)
            val readIdx = cursor.getColumnIndex(Telephony.Sms.READ)

            var imported = 0
            val conversationsCache = mutableMapOf<String, Long>()

            while (cursor.moveToNext()) {
                val telephonyId = if (idIdx != -1) cursor.getLong(idIdx) else 0L
                val threadId = if (threadIdIdx != -1) cursor.getLong(threadIdIdx) else 0L
                val address = if (addressIdx != -1) cursor.getString(addressIdx) ?: "" else ""
                val body = if (bodyIdx != -1) cursor.getString(bodyIdx) ?: "" else ""
                val date = if (dateIdx != -1) cursor.getLong(dateIdx) else System.currentTimeMillis()
                val type = if (typeIdx != -1) cursor.getInt(typeIdx) else Telephony.Sms.MESSAGE_TYPE_INBOX
                val read = if (readIdx != -1) cursor.getInt(readIdx) == 1 else true

                if (address.isBlank() || body.isBlank()) continue

                // Check if message already exists by telephony ID
                val existing = messageDao.getMessageByTelephonyId(telephonyId)
                if (existing != null) continue

                val normalizedAddr = PhoneNumberUtils.normalizeNumber(address)

                // Find or create conversation
                var conversationId = conversationsCache[normalizedAddr]
                if (conversationId == null) {
                    val existingConv = conversationDao.getConversationByAddress(normalizedAddr)
                    if (existingConv != null) {
                        conversationId = existingConv.id
                        conversationsCache[normalizedAddr] = conversationId
                    } else {
                        val contact = ContactResolver.resolveContact(context, normalizedAddr)
                        val newConv = ConversationEntity(
                            threadId = threadId,
                            recipientAddress = normalizedAddr,
                            recipientName = contact.name,
                            snippet = body.take(100),
                            lastMessageTimestamp = date,
                            unreadCount = if (read) 0 else 1
                        )
                        conversationId = conversationDao.insertConversation(newConv)
                        conversationsCache[normalizedAddr] = conversationId
                    }
                }

                val isOutgoing = (type == Telephony.Sms.MESSAGE_TYPE_SENT)
                val otp = if (!isOutgoing) OtpDetector.extractOtp(body) else null

                val messageEntity = MessageEntity(
                    conversationId = conversationId,
                    telephonyMessageId = telephonyId,
                    address = normalizedAddr,
                    body = body,
                    timestamp = date,
                    isOutgoing = isOutgoing,
                    status = if (isOutgoing) "SENT" else "RECEIVED",
                    isRead = read,
                    otpCode = otp
                )
                messageDao.insertMessage(messageEntity)
                imported++

                if (imported % 20 == 0) {
                    _progress.value = ImportProgress.InProgress(imported, total)
                }
            }
            cursor.close()

            _progress.value = ImportProgress.Completed(imported)
            imported
        } catch (e: Exception) {
            _progress.value = ImportProgress.Error(e.localizedMessage ?: "Failed to import messages")
            0
        }
    }
}
