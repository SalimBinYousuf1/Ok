package com.example.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ConversationEntity
import com.example.data.local.MessageEntity
import com.example.data.local.SalimDatabase
import com.example.data.repository.ConversationRepository
import com.example.data.repository.MessageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val db = SalimDatabase.getInstance(application)
    private val conversationRepository = ConversationRepository(db.conversationDao(), db.messageDao())
    private val messageRepository = MessageRepository(db.messageDao(), db.conversationDao())

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val matchedConversations: StateFlow<List<ConversationEntity>> = _query
        .flatMapLatest { q ->
            if (q.isBlank()) flowOf(emptyList())
            else conversationRepository.searchConversations(q.trim())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val matchedMessages: StateFlow<List<MessageEntity>> = _query
        .flatMapLatest { q ->
            if (q.isBlank()) flowOf(emptyList())
            else messageRepository.searchMessages(q.trim())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setQuery(newQuery: String) {
        _query.value = newQuery
    }
}
