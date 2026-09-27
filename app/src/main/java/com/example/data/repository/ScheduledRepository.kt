package com.example.data.repository

import android.content.Context
import com.example.data.local.ScheduledMessageDao
import com.example.data.local.ScheduledMessageEntity
import com.example.telephony.ScheduledMessageManager
import kotlinx.coroutines.flow.Flow

class ScheduledRepository(
    private val scheduledMessageDao: ScheduledMessageDao
) {

    fun getAllScheduled(): Flow<List<ScheduledMessageEntity>> {
        return scheduledMessageDao.getAllScheduledMessages()
    }

    suspend fun scheduleMessage(
        context: Context,
        conversationId: Long,
        recipientAddress: String,
        recipientName: String,
        body: String,
        scheduledTimestamp: Long
    ): Long {
        val entity = ScheduledMessageEntity(
            conversationId = conversationId,
            recipientAddress = recipientAddress,
            recipientName = recipientName,
            body = body,
            scheduledTimestamp = scheduledTimestamp,
            status = "PENDING"
        )
        val id = scheduledMessageDao.insertScheduledMessage(entity)
        val scheduledWithId = entity.copy(id = id)
        ScheduledMessageManager.scheduleMessage(context, scheduledWithId)
        return id
    }

    suspend fun cancelScheduled(context: Context, id: Long) {
        ScheduledMessageManager.cancelScheduledMessage(context, id)
        scheduledMessageDao.updateStatus(id, "CANCELLED")
    }

    suspend fun deleteScheduled(context: Context, id: Long) {
        ScheduledMessageManager.cancelScheduledMessage(context, id)
        scheduledMessageDao.deleteScheduledMessage(id)
    }
}
