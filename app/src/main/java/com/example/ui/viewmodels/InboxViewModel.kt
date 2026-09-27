package com.example.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ConversationEntity
import com.example.data.local.SalimDatabase
import com.example.data.repository.ConversationRepository
import com.example.telephony.ImportProgress
import com.example.telephony.SmsImporter
import com.example.telephony.TelephonyManagerHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class InboxFilter {
    ALL, UNREAD, STARRED, ARCHIVED
}

class InboxViewModel(application: Application) : AndroidViewModel(application) {

    private val db = SalimDatabase.getInstance(application)
    private val conversationRepository = ConversationRepository(db.conversationDao(), db.messageDao())
    private val smsImporter = SmsImporter(application, db.conversationDao(), db.messageDao())

    private val _selectedFilter = MutableStateFlow(InboxFilter.ALL)
    val selectedFilter: StateFlow<InboxFilter> = _selectedFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isDefaultSms = MutableStateFlow(TelephonyManagerHelper.isDefaultSmsApp(application))
    val isDefaultSms: StateFlow<Boolean> = _isDefaultSms.asStateFlow()

    val importProgress: StateFlow<ImportProgress> = smsImporter.progress

    val conversations: StateFlow<List<ConversationEntity>> = combine(
        conversationRepository.getAllConversations(),
        _selectedFilter,
        _searchQuery
    ) { list, filter, query ->
        var filtered = when (filter) {
            InboxFilter.ALL -> list.filter { !it.isArchived }
            InboxFilter.UNREAD -> list.filter { !it.isArchived && it.unreadCount > 0 }
            InboxFilter.STARRED -> list.filter { !it.isArchived } // We will query starred messages or flagged conversations
            InboxFilter.ARCHIVED -> list.filter { it.isArchived }
        }

        if (query.isNotBlank()) {
            val q = query.lowercase().trim()
            filtered = filtered.filter {
                it.recipientName.lowercase().contains(q) ||
                        it.recipientAddress.contains(q) ||
                        it.snippet.lowercase().contains(q)
            }
        }
        filtered
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun refreshDefaultSmsStatus() {
        _isDefaultSms.value = TelephonyManagerHelper.isDefaultSmsApp(getApplication())
    }

    fun setFilter(filter: InboxFilter) {
        _selectedFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun togglePin(conversation: ConversationEntity) {
        viewModelScope.launch {
            conversationRepository.setPinned(conversation.id, !conversation.isPinned)
        }
    }

    fun toggleArchive(conversation: ConversationEntity) {
        viewModelScope.launch {
            conversationRepository.setArchived(conversation.id, !conversation.isArchived)
        }
    }

    fun deleteConversation(id: Long) {
        viewModelScope.launch {
            conversationRepository.deleteConversation(id)
        }
    }

    fun markAllAsRead(id: Long) {
        viewModelScope.launch {
            conversationRepository.markAllAsRead(id)
        }
    }

    fun importPlatformMessages() {
        viewModelScope.launch {
            smsImporter.importPlatformSms()
        }
    }
}
