package com.example.a62compass.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.a62compass.data.model.SavedHeading
import com.example.a62compass.data.repository.CompassRepository
import com.example.a62compass.util.FeedbackManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CompassRepository(application)
    private val feedbackManager = FeedbackManager(application)

    val history: StateFlow<List<SavedHeading>> = repository.historyFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteHeading(id: Long) {
        viewModelScope.launch {
            repository.deleteHeading(id)
            feedbackManager.performHaptic(true)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            repository.clearHistory()
            feedbackManager.performHaptic(true)
        }
    }

    override fun onCleared() {
        super.onCleared()
        feedbackManager.release()
    }
}
