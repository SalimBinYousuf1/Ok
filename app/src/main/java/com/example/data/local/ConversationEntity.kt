package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "conversations",
    indices = [
        Index(value = ["recipientAddress"], unique = true),
        Index(value = ["lastMessageTimestamp"]),
        Index(value = ["isPinned", "lastMessageTimestamp"])
    ]
)
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val threadId: Long = 0L,
    val recipientAddress: String,
    val recipientName: String,
    val snippet: String = "",
    val lastMessageTimestamp: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isMuted: Boolean = false,
    val draftText: String? = null,
    val deliveryStatus: String = "NONE", // NONE, SENDING, SENT, DELIVERED, FAILED
    val isGroup: Boolean = false
)
