package com.example.a62compass.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.a62compass.data.model.AppTheme
import com.example.a62compass.data.model.CompassSettings
import com.example.a62compass.data.model.HeadingFormat
import com.example.a62compass.data.repository.CompassRepository
import com.example.a62compass.util.FeedbackManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CompassRepository(application)
    private val feedbackManager = FeedbackManager(application)

    val settings: StateFlow<CompassSettings> = repository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CompassSettings()
        )

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            repository.updateTheme(theme)
            feedbackManager.performHaptic(settings.value.hapticFeedback)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateDynamicColor(enabled)
            feedbackManager.performHaptic(settings.value.hapticFeedback)
        }
    }

    fun setHeadingFormat(format: HeadingFormat) {
        viewModelScope.launch {
            repository.updateHeadingFormat(format)
            feedbackManager.performHaptic(settings.value.hapticFeedback)
        }
    }

    fun setHapticFeedback(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateHapticFeedback(enabled)
            feedbackManager.performHaptic(enabled)
        }
    }

    fun setSoundEffects(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSoundEffects(enabled)
            feedbackManager.performHaptic(settings.value.hapticFeedback)
            if (enabled) {
                feedbackManager.playSubtleSound(true)
            }
        }
    }

    fun setShowDegreeTicks(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateShowDegreeTicks(enabled)
            feedbackManager.performHaptic(settings.value.hapticFeedback)
        }
    }

    fun setShowAccuracy(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateShowAccuracy(enabled)
            feedbackManager.performHaptic(settings.value.hapticFeedback)
        }
    }

    fun resetSettings() {
        viewModelScope.launch {
            repository.resetSettings()
            feedbackManager.performHaptic(true)
        }
    }

    fun clearHistory() {
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
