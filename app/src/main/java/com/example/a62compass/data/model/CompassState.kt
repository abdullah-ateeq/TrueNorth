package com.example.a62compass.data.model

/**
 * State representing live compass sensor data and lock state.
 */
data class CompassState(
    val heading: Float = 0f,
    val direction: String = "N",
    val cardinalFullName: String = "NORTH",
    val sensorAvailable: Boolean = false,
    val accuracy: Int = 0,
    val accuracyLabel: String = "Unknown",
    val sensorTypeUsed: String = "None",
    val isCalibrating: Boolean = false,
    val isLocked: Boolean = false,
    val lockedHeading: Float? = null
)
