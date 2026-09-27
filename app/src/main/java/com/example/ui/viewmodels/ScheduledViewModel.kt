package com.example.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SalimDatabase
import com.example.data.local.ScheduledMessageEntity
import com.example.data.repository.ScheduledRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ScheduledViewModel(application: Application) : AndroidViewModel(application) {

    private val db = SalimDatabase.getInstance(application)
    private val repository = ScheduledRepository(db.scheduledMessageDao())

    val scheduledMessages: StateFlow<List<ScheduledMessageEntity>> = repository
        .getAllScheduled()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun cancelScheduled(id: Long) {
        viewModelScope.launch {
            repository.cancelScheduled(getApplication(), id)
        }
    }

    fun deleteScheduled(id: Long) {
        viewModelScope.launch {
            repository.deleteScheduled(getApplication(), id)
        }
    }
}
