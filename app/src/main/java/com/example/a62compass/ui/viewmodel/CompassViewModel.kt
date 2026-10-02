package com.example.a62compass.ui.viewmodel

import android.app.Application
import android.view.Surface
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.a62compass.data.model.CompassSettings
import com.example.a62compass.data.model.CompassState
import com.example.a62compass.data.model.SavedHeading
import com.example.a62compass.data.repository.CompassRepository
import com.example.a62compass.sensor.CompassSensorManager
import com.example.a62compass.util.CompassMath
import com.example.a62compass.util.FeedbackManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CompassViewModel(application: Application) : AndroidViewModel(application) {

    val sensorManager = CompassSensorManager(application)
    private val repository = CompassRepository(application)
    private val feedbackManager = FeedbackManager(application)

    val settings: StateFlow<CompassSettings> = repository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CompassSettings()
        )

    private val isLockedFlow = MutableStateFlow(false)
    private val lockedHeadingFlow = MutableStateFlow<Float?>(null)

    val compassState: StateFlow<CompassState> = combine(
        sensorManager.sensorData,
        isLockedFlow,
        lockedHeadingFlow
    ) { sensorData, isLocked, lockedHeading ->
        val heading = sensorData.heading
        CompassState(
            heading = heading,
            direction = CompassMath.getCardinalDirection(heading),
            cardinalFullName = CompassMath.getCardinalFullName(heading),
            sensorAvailable = sensorData.sensorAvailable,
            accuracy = sensorData.accuracy,
            accuracyLabel = sensorData.accuracyLabel,
            sensorTypeUsed = sensorData.sensorTypeUsed,
            isCalibrating = sensorData.isCalibrating,
            isLocked = isLocked,
            lockedHeading = lockedHeading
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CompassState(
            sensorAvailable = sensorManager.isSensorAvailable,
            sensorTypeUsed = if (sensorManager.isSensorAvailable) "Detecting..." else "None"
        )
    )

    fun startListening(displayRotation: Int = Surface.ROTATION_0) {
        sensorManager.startListening(displayRotation)
    }

    fun stopListening() {
        sensorManager.stopListening()
    }

    fun updateDisplayRotation(rotation: Int) {
        sensorManager.updateDisplayRotation(rotation)
    }

    fun retrySensors(displayRotation: Int = Surface.ROTATION_0) {
        sensorManager.stopListening()
        sensorManager.startListening(displayRotation)
    }

    fun toggleLock() {
        val currentSettings = settings.value
        val currentState = compassState.value

        if (isLockedFlow.value) {
            clearLock()
        } else {
            isLockedFlow.value = true
            lockedHeadingFlow.value = currentState.heading
            feedbackManager.performHaptic(currentSettings.hapticFeedback)
            feedbackManager.playSubtleSound(currentSettings.soundEffects)
        }
    }

    fun clearLock() {
        val currentSettings = settings.value
        isLockedFlow.value = false
        lockedHeadingFlow.value = null
        feedbackManager.performHaptic(currentSettings.hapticFeedback)
    }

    fun saveCurrentHeading(onSaved: () -> Unit = {}) {
        val currentState = compassState.value
        if (!currentState.sensorAvailable) return

        val currentSettings = settings.value
        val entry = SavedHeading(
            id = System.currentTimeMillis(),
            heading = currentState.heading,
            direction = currentState.direction,
            accuracy = currentState.accuracyLabel,
            timestamp = System.currentTimeMillis()
        )

        viewModelScope.launch {
            repository.saveHeading(entry)
            feedbackManager.performHaptic(currentSettings.hapticFeedback)
            feedbackManager.playSubtleSound(currentSettings.soundEffects)
            onSaved()
        }
    }

    fun onCalibrationAction() {
        val currentSettings = settings.value
        feedbackManager.performHaptic(currentSettings.hapticFeedback)
    }

    override fun onCleared() {
        super.onCleared()
        sensorManager.stopListening()
        feedbackManager.release()
    }
}
