package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["conversationId"]),
        Index(value = ["telephonyMessageId"], unique = false),
        Index(value = ["timestamp"]),
        Index(value = ["address", "timestamp"])
    ]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val conversationId: Long,
    val telephonyMessageId: Long? = null,
    val address: String,
    val body: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isOutgoing: Boolean,
    val status: String = "SENT", // PENDING, SENDING, SENT, DELIVERED, FAILED, RECEIVED
    val isStarred: Boolean = false,
    val isRead: Boolean = true,
    val attachmentUri: String? = null,
    val attachmentMimeType: String? = null,
    val isScheduled: Boolean = false,
    val scheduledTime: Long? = null,
    val otpCode: String? = null
)
