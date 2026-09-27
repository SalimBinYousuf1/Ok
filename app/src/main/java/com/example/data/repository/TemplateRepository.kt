package com.example.data.repository

import com.example.data.local.QuickReplyTemplateDao
import com.example.data.local.QuickReplyTemplateEntity
import kotlinx.coroutines.flow.Flow

class TemplateRepository(
    private val quickReplyTemplateDao: QuickReplyTemplateDao
) {

    fun getAllTemplates(): Flow<List<QuickReplyTemplateEntity>> {
        return quickReplyTemplateDao.getAllTemplates()
    }

    suspend fun insertTemplate(title: String, content: String): Long {
        return quickReplyTemplateDao.insertTemplate(
            QuickReplyTemplateEntity(title = title, content = content)
        )
    }

    suspend fun deleteTemplate(id: Long) {
        quickReplyTemplateDao.deleteTemplateById(id)
    }
}
