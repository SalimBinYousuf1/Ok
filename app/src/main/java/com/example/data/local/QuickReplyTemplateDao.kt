package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface QuickReplyTemplateDao {

    @Query("SELECT * FROM quick_reply_templates ORDER BY sortOrder ASC, id ASC")
    fun getAllTemplates(): Flow<List<QuickReplyTemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: QuickReplyTemplateEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(templates: List<QuickReplyTemplateEntity>)

    @Update
    suspend fun updateTemplate(template: QuickReplyTemplateEntity)

    @Query("DELETE FROM quick_reply_templates WHERE id = :id")
    suspend fun deleteTemplateById(id: Long)

    @Query("SELECT COUNT(*) FROM quick_reply_templates")
    suspend fun getTemplateCount(): Int
}
