package com.example.a62compass.data.model

enum class AppTheme {
    SYSTEM,
    LIGHT,
    DARK
}

enum class HeadingFormat {
    DEGREES_AND_DIRECTION, // e.g. 284° W
    DEGREES_ONLY,          // e.g. 284°
    DIRECTION_ONLY         // e.g. W
}

data class CompassSettings(
    val theme: AppTheme = AppTheme.SYSTEM,
    val dynamicColor: Boolean = true,
    val headingFormat: HeadingFormat = HeadingFormat.DEGREES_AND_DIRECTION,
    val hapticFeedback: Boolean = true,
    val soundEffects: Boolean = false,
    val showDegreeTicks: Boolean = true,
    val showAccuracy: Boolean = true
)
