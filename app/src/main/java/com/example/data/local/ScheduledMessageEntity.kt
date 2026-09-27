package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "scheduled_messages",
    indices = [
        Index(value = ["scheduledTimestamp"]),
        Index(value = ["status"])
    ]
)
data class ScheduledMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val conversationId: Long,
    val recipientAddress: String,
    val recipientName: String,
    val body: String,
    val scheduledTimestamp: Long,
    val status: String = "PENDING", // PENDING, SENT, CANCELLED, FAILED
    val createdTimestamp: Long = System.currentTimeMillis()
)
