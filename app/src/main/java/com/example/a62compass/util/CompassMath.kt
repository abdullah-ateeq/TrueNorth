package com.example.a62compass.util

import java.util.Locale
import kotlin.math.roundToInt

/**
 * Utility functions for heading normalization, cardinal direction determination,
 * angle smoothing with wrap-around support, and heading formatting.
 */
object CompassMath {

    /**
     * Normalizes an angle in degrees to the range [0.0, 360.0).
     */
    fun normalizeHeading(degrees: Float): Float {
        if (degrees.isNaN() || degrees.isInfinite()) return 0f
        var normalized = (degrees % 360f + 360f) % 360f
        if (normalized >= 360f) {
            normalized = 0f
        }
        return normalized
    }

    /**
     * Determines the closest 8-point cardinal/intercardinal direction.
     * Rules:
     * - 337.5°–360° / 0°–22.5°: N
     * - 22.5°–67.5°: NE
     * - 67.5°–112.5°: E
     * - 112.5°–157.5°: SE
     * - 157.5°–202.5°: S
     * - 202.5°–247.5°: SW
     * - 247.5°–292.5°: W
     * - 292.5°–337.5°: NW
     */
    fun getCardinalDirection(heading: Float): String {
        val norm = normalizeHeading(heading)
        return when {
            norm >= 337.5f || norm < 22.5f -> "N"
            norm < 67.5f -> "NE"
            norm < 112.5f -> "E"
            norm < 157.5f -> "SE"
            norm < 202.5f -> "S"
            norm < 247.5f -> "SW"
            norm < 292.5f -> "W"
            else -> "NW"
        }
    }

    /**
     * Returns full cardinal name (e.g. NORTH, EAST, NORTH-EAST).
     */
    fun getCardinalFullName(heading: Float): String {
        return when (getCardinalDirection(heading)) {
            "N" -> "NORTH"
            "NE" -> "NORTH-EAST"
            "E" -> "EAST"
            "SE" -> "SOUTH-EAST"
            "S" -> "SOUTH"
            "SW" -> "SOUTH-WEST"
            "W" -> "WEST"
            "NW" -> "NORTH-WEST"
            else -> "NORTH"
        }
    }

    /**
     * Formats heading as a 3-digit degree representation: 000°, 045°, 180°, etc.
     */
    fun formatHeadingDegrees(heading: Float): String {
        val norm = normalizeHeading(heading).roundToInt() % 360
        return String.format(Locale.US, "%03d°", norm)
    }

    /**
     * Reusable angle smoothing function handling 359° <-> 0° wrap-around
     * through the shortest rotational path.
     *
     * @param current Current smoothed angle in degrees
     * @param target Raw or incoming target angle in degrees
     * @param factor Interpolation factor between 0.0 (no movement) and 1.0 (instant snap)
     */
    fun smoothAngle(current: Float, target: Float, factor: Float = 0.15f): Float {
        val normalizedCurrent = normalizeHeading(current)
        val normalizedTarget = normalizeHeading(target)

        var diff = (normalizedTarget - normalizedCurrent) % 360f
        if (diff > 180f) diff -= 360f
        if (diff < -180f) diff += 360f

        val safeFactor = factor.coerceIn(0f, 1f)
        return normalizeHeading(normalizedCurrent + diff * safeFactor)
    }

    /**
     * Calculates the shortest angular difference in degrees from [current] to [target].
     * Result is in [-180.0, 180.0]. Positive means clockwise, negative means counter-clockwise.
     */
    fun getAngleDifference(target: Float, current: Float): Float {
        val normTarget = normalizeHeading(target)
        val normCurrent = normalizeHeading(current)
        var diff = (normTarget - normCurrent) % 360f
        if (diff > 180f) diff -= 360f
        if (diff < -180f) diff += 360f
        return diff
    }
}
