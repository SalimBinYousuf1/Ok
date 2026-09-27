package com.example.data.repository

import android.content.Context
import com.example.data.local.ConversationDao
import com.example.data.local.ConversationEntity
import com.example.data.local.MessageDao
import com.example.telephony.ContactResolver
import com.example.telephony.PhoneNumberUtils
import kotlinx.coroutines.flow.Flow

class ConversationRepository(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao
) {

    fun getConversations(archived: Boolean = false): Flow<List<ConversationEntity>> {
        return conversationDao.getConversations(archived)
    }

    fun getAllConversations(): Flow<List<ConversationEntity>> {
        return conversationDao.getAllConversations()
    }

    fun getConversationById(id: Long): Flow<ConversationEntity?> {
        return conversationDao.getConversationById(id)
    }

    suspend fun getOrCreateConversation(context: Context, address: String, nameHint: String? = null): Long {
        val normalized = PhoneNumberUtils.normalizeNumber(address)
        val existing = conversationDao.getConversationByAddress(normalized)
        if (existing != null) {
            return existing.id
        }

        val contactName = if (!nameHint.isNullOrBlank()) {
            nameHint
        } else {
            ContactResolver.resolveContact(context, normalized).name
        }

        val newConv = ConversationEntity(
            recipientAddress = normalized,
            recipientName = contactName,
            snippet = "",
            lastMessageTimestamp = System.currentTimeMillis()
        )
        return conversationDao.insertConversation(newConv)
    }

    suspend fun setPinned(id: Long, isPinned: Boolean) {
        conversationDao.setPinned(id, isPinned)
    }

    suspend fun setArchived(id: Long, isArchived: Boolean) {
        conversationDao.setArchived(id, isArchived)
    }

    suspend fun setMuted(id: Long, isMuted: Boolean) {
        conversationDao.setMuted(id, isMuted)
    }

    suspend fun deleteConversation(id: Long) {
        messageDao.deleteMessagesForConversation(id)
        conversationDao.deleteConversationById(id)
    }

    suspend fun updateDraft(id: Long, draft: String?) {
        conversationDao.updateDraft(id, draft)
    }

    suspend fun markAllAsRead(id: Long) {
        conversationDao.markAllAsRead(id)
        messageDao.markMessagesAsReadForConversation(id)
    }

    fun searchConversations(query: String): Flow<List<ConversationEntity>> {
        return conversationDao.searchConversations(query)
    }
}
