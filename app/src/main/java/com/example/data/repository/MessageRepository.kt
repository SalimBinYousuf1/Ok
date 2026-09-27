package com.example.data.repository

import android.content.Context
import com.example.data.local.ConversationDao
import com.example.data.local.MessageDao
import com.example.data.local.MessageEntity
import com.example.telephony.MessagingTransport
import com.example.telephony.SmsTransport
import kotlinx.coroutines.flow.Flow

class MessageRepository(
    private val messageDao: MessageDao,
    private val conversationDao: ConversationDao,
    private val transport: MessagingTransport = SmsTransport()
) {

    fun getMessagesForConversation(conversationId: Long): Flow<List<MessageEntity>> {
        return messageDao.getMessagesForConversation(conversationId)
    }

    fun getStarredMessages(): Flow<List<MessageEntity>> {
        return messageDao.getStarredMessages()
    }

    fun searchMessages(query: String): Flow<List<MessageEntity>> {
        return messageDao.searchMessages(query)
    }

    suspend fun sendMessage(
        context: Context,
        conversationId: Long,
        recipientAddress: String,
        body: String,
        attachmentUri: String? = null,
        attachmentMimeType: String? = null
    ): Result<Long> {
        val timestamp = System.currentTimeMillis()
        val message = MessageEntity(
            conversationId = conversationId,
            address = recipientAddress,
            body = body,
            timestamp = timestamp,
            isOutgoing = true,
            status = "SENDING",
            attachmentUri = attachmentUri,
            attachmentMimeType = attachmentMimeType,
            isRead = true
        )
        val messageId = messageDao.insertMessage(message)

        // Update conversation snippet and timestamp
        conversationDao.updateLatestMessage(
            id = conversationId,
            snippet = body.take(120),
            timestamp = timestamp,
            deliveryStatus = "SENDING"
        )
        // Clear draft
        conversationDao.updateDraft(conversationId, null)

        val sendResult = transport.sendTextMessage(
            context = context,
            conversationId = conversationId,
            messageId = messageId,
            recipientAddress = recipientAddress,
            body = body
        )

        return if (sendResult.isSuccess) {
            Result.success(messageId)
        } else {
            messageDao.updateStatus(messageId, "FAILED")
            conversationDao.updateLatestMessage(
                id = conversationId,
                snippet = body.take(120),
                timestamp = timestamp,
                deliveryStatus = "FAILED"
            )
            Result.failure(sendResult.exceptionOrNull() ?: Exception("Send failed"))
        }
    }

    suspend fun retryMessage(context: Context, messageId: Long): Boolean {
        val msg = messageDao.getMessageById(messageId) ?: return false
        messageDao.updateStatus(messageId, "SENDING")
        conversationDao.updateLatestMessage(
            id = msg.conversationId,
            snippet = msg.body.take(120),
            timestamp = System.currentTimeMillis(),
            deliveryStatus = "SENDING"
        )
        val result = transport.sendTextMessage(
            context = context,
            conversationId = msg.conversationId,
            messageId = msg.id,
            recipientAddress = msg.address,
            body = msg.body
        )
        return result.isSuccess
    }

    suspend fun setStarred(messageId: Long, isStarred: Boolean) {
        messageDao.setStarred(messageId, isStarred)
    }

    suspend fun deleteMessage(messageId: Long) {
        val msg = messageDao.getMessageById(messageId)
        messageDao.deleteMessageById(messageId)
        if (msg != null) {
            val latest = messageDao.getLatestMessageForConversation(msg.conversationId)
            if (latest != null) {
                conversationDao.updateLatestMessage(
                    id = msg.conversationId,
                    snippet = latest.body.take(120),
                    timestamp = latest.timestamp,
                    deliveryStatus = latest.status
                )
            } else {
                conversationDao.updateLatestMessage(
                    id = msg.conversationId,
                    snippet = "",
                    timestamp = System.currentTimeMillis(),
                    deliveryStatus = "NONE"
                )
            }
        }
    }
}
