package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {

    @Query("SELECT * FROM conversations WHERE isArchived = :archived ORDER BY isPinned DESC, lastMessageTimestamp DESC")
    fun getConversations(archived: Boolean = false): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations ORDER BY isPinned DESC, lastMessageTimestamp DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    fun getConversationById(id: Long): Flow<ConversationEntity?>

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    suspend fun getConversationByIdOnce(id: Long): ConversationEntity?

    @Query("SELECT * FROM conversations WHERE recipientAddress = :address LIMIT 1")
    suspend fun getConversationByAddress(address: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity): Long

    @Update
    suspend fun updateConversation(conversation: ConversationEntity)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversationById(id: Long)

    @Query("UPDATE conversations SET isPinned = :isPinned WHERE id = :id")
    suspend fun setPinned(id: Long, isPinned: Boolean)

    @Query("UPDATE conversations SET isArchived = :isArchived WHERE id = :id")
    suspend fun setArchived(id: Long, isArchived: Boolean)

    @Query("UPDATE conversations SET isMuted = :isMuted WHERE id = :id")
    suspend fun setMuted(id: Long, isMuted: Boolean)

    @Query("UPDATE conversations SET draftText = :draft WHERE id = :id")
    suspend fun updateDraft(id: Long, draft: String?)

    @Query("UPDATE conversations SET unreadCount = 0 WHERE id = :id")
    suspend fun markAllAsRead(id: Long)

    @Query("UPDATE conversations SET snippet = :snippet, lastMessageTimestamp = :timestamp, deliveryStatus = :deliveryStatus WHERE id = :id")
    suspend fun updateLatestMessage(id: Long, snippet: String, timestamp: Long, deliveryStatus: String)

    @Query("SELECT * FROM conversations WHERE recipientName LIKE '%' || :query || '%' OR recipientAddress LIKE '%' || :query || '%' OR snippet LIKE '%' || :query || '%'")
    fun searchConversations(query: String): Flow<List<ConversationEntity>>
}
