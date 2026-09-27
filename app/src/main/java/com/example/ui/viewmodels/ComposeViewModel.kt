package com.example.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SalimDatabase
import com.example.data.repository.ConversationRepository
import com.example.telephony.ContactInfo
import com.example.telephony.ContactResolver
import com.example.telephony.PhoneNumberUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ComposeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = SalimDatabase.getInstance(application)
    private val conversationRepository = ConversationRepository(db.conversationDao(), db.messageDao())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _suggestedContacts = MutableStateFlow<List<ContactInfo>>(emptyList())
    val suggestedContacts: StateFlow<List<ContactInfo>> = _suggestedContacts.asStateFlow()

    private val _createdConversationId = MutableStateFlow<Long?>(null)
    val createdConversationId: StateFlow<Long?> = _createdConversationId.asStateFlow()

    init {
        loadInitialContacts()
    }

    private fun loadInitialContacts() {
        viewModelScope.launch {
            val contacts = withContext(Dispatchers.IO) {
                ContactResolver.searchContacts(getApplication(), "")
            }
            _suggestedContacts.value = contacts
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            val contacts = withContext(Dispatchers.IO) {
                ContactResolver.searchContacts(getApplication(), query)
            }
            _suggestedContacts.value = contacts
        }
    }

    fun selectRecipient(number: String, nameHint: String? = null) {
        viewModelScope.launch {
            val normalized = PhoneNumberUtils.normalizeNumber(number)
            if (normalized.isNotBlank()) {
                val convId = conversationRepository.getOrCreateConversation(
                    context = getApplication(),
                    address = normalized,
                    nameHint = nameHint
                )
                _createdConversationId.value = convId
            }
        }
    }

    fun resetNav() {
        _createdConversationId.value = null
    }
}
