package com.example.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ConversationEntity
import com.example.data.local.MessageEntity
import com.example.data.local.QuickReplyTemplateEntity
import com.example.data.local.SalimDatabase
import com.example.data.repository.ConversationRepository
import com.example.data.repository.MessageRepository
import com.example.data.repository.ScheduledRepository
import com.example.data.repository.TemplateRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ConversationViewModel(
    application: Application,
    private val conversationId: Long
) : AndroidViewModel(application) {

    private val db = SalimDatabase.getInstance(application)
    private val conversationRepository = ConversationRepository(db.conversationDao(), db.messageDao())
    private val messageRepository = MessageRepository(db.messageDao(), db.conversationDao())
    private val scheduledRepository = ScheduledRepository(db.scheduledMessageDao())
    private val templateRepository = TemplateRepository(db.quickReplyTemplateDao())

    val conversation: StateFlow<ConversationEntity?> = conversationRepository
        .getConversationById(conversationId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val messages: StateFlow<List<MessageEntity>> = messageRepository
        .getMessagesForConversation(conversationId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val templates: StateFlow<List<QuickReplyTemplateEntity>> = templateRepository
        .getAllTemplates()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _draftText = MutableStateFlow("")
    val draftText: StateFlow<String> = _draftText.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var draftSaveJob: Job? = null

    init {
        // Mark conversation as read
        viewModelScope.launch {
            conversationRepository.markAllAsRead(conversationId)
            val current = db.conversationDao().getConversationByIdOnce(conversationId)
            current?.draftText?.let {
                _draftText.value = it
            }
        }
    }

    fun onDraftTextChanged(newText: String) {
        _draftText.value = newText
        draftSaveJob?.cancel()
        draftSaveJob = viewModelScope.launch {
            delay(500) // Debounce draft save
            conversationRepository.updateDraft(conversationId, newText.ifBlank { null })
        }
    }

    fun sendMessage(attachmentUri: String? = null, attachmentMimeType: String? = null) {
        val text = _draftText.value.trim()
        val conv = conversation.value ?: return
        if (text.isEmpty() && attachmentUri == null) return

        _draftText.value = ""
        viewModelScope.launch {
            val result = messageRepository.sendMessage(
                context = getApplication(),
                conversationId = conversationId,
                recipientAddress = conv.recipientAddress,
                body = text,
                attachmentUri = attachmentUri,
                attachmentMimeType = attachmentMimeType
            )
            if (result.isFailure) {
                _errorMessage.value = "Failed to send SMS. Check your signal or SIM."
            }
        }
    }

    fun retryMessage(messageId: Long) {
        viewModelScope.launch {
            val success = messageRepository.retryMessage(getApplication(), messageId)
            if (!success) {
                _errorMessage.value = "Failed to resend SMS."
            }
        }
    }

    fun toggleStarred(message: MessageEntity) {
        viewModelScope.launch {
            messageRepository.setStarred(message.id, !message.isStarred)
        }
    }

    fun deleteMessage(messageId: Long) {
        viewModelScope.launch {
            messageRepository.deleteMessage(messageId)
        }
    }

    fun scheduleMessage(scheduledTimestamp: Long, customText: String? = null) {
        val conv = conversation.value ?: return
        val text = customText ?: _draftText.value.trim()
        if (text.isEmpty()) return

        if (customText == null) {
            _draftText.value = ""
        }

        viewModelScope.launch {
            scheduledRepository.scheduleMessage(
                context = getApplication(),
                conversationId = conversationId,
                recipientAddress = conv.recipientAddress,
                recipientName = conv.recipientName,
                body = text,
                scheduledTimestamp = scheduledTimestamp
            )
        }
    }

    fun applyTemplate(template: QuickReplyTemplateEntity) {
        _draftText.value = template.content
        onDraftTextChanged(template.content)
    }

    fun clearError() {
        _errorMessage.value = null
    }

    class Factory(
        private val application: Application,
        private val conversationId: Long
    ) : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return ConversationViewModel(application, conversationId) as T
        }
    }
}

