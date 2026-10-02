package com.example.a62compass

import com.example.a62compass.util.CompassMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompassMathTest {

    @Test
    fun testHeadingNormalization() {
        assertEquals(0f, CompassMath.normalizeHeading(0f), 0.001f)
        assertEquals(180f, CompassMath.normalizeHeading(180f), 0.001f)
        assertEquals(90f, CompassMath.normalizeHeading(450f), 0.001f)
        assertEquals(270f, CompassMath.normalizeHeading(-90f), 0.001f)
        assertEquals(0f, CompassMath.normalizeHeading(-360f), 0.001f)
    }

    @Test
    fun testZeroDegreeHandling() {
        assertEquals(0f, CompassMath.normalizeHeading(0f), 0.001f)
        assertEquals("N", CompassMath.getCardinalDirection(0f))
        assertEquals("000°", CompassMath.formatHeadingDegrees(0f))
    }

    @Test
    fun test360To0Handling() {
        assertEquals(0f, CompassMath.normalizeHeading(360f), 0.001f)
        assertEquals(0f, CompassMath.normalizeHeading(720f), 0.001f)
        assertEquals("N", CompassMath.getCardinalDirection(360f))
        assertEquals("000°", CompassMath.formatHeadingDegrees(360f))
    }

    @Test
    fun testCardinalDirectionCalculations() {
        // Specific user test cases:
        // 0° → N
        assertEquals("N", CompassMath.getCardinalDirection(0f))
        // 45° → NE
        assertEquals("NE", CompassMath.getCardinalDirection(45f))
        // 90° → E
        assertEquals("E", CompassMath.getCardinalDirection(90f))
        // 135° → SE
        assertEquals("SE", CompassMath.getCardinalDirection(135f))
        // 180° → S
        assertEquals("S", CompassMath.getCardinalDirection(180f))
        // 225° → SW
        assertEquals("SW", CompassMath.getCardinalDirection(225f))
        // 270° → W
        assertEquals("W", CompassMath.getCardinalDirection(270f))
        // 315° → NW
        assertEquals("NW", CompassMath.getCardinalDirection(315f))
        // 359° → N
        assertEquals("N", CompassMath.getCardinalDirection(359f))
    }

    @Test
    fun testNEDirectionBoundaries() {
        assertEquals("NE", CompassMath.getCardinalDirection(22.5f))
        assertEquals("NE", CompassMath.getCardinalDirection(45f))
        assertEquals("NE", CompassMath.getCardinalDirection(67.4f))
    }

    @Test
    fun testSEDirectionBoundaries() {
        assertEquals("SE", CompassMath.getCardinalDirection(112.5f))
        assertEquals("SE", CompassMath.getCardinalDirection(135f))
        assertEquals("SE", CompassMath.getCardinalDirection(157.4f))
    }

    @Test
    fun testSWDirectionBoundaries() {
        assertEquals("SW", CompassMath.getCardinalDirection(202.5f))
        assertEquals("SW", CompassMath.getCardinalDirection(225f))
        assertEquals("SW", CompassMath.getCardinalDirection(247.4f))
    }

    @Test
    fun testNWDirectionBoundaries() {
        assertEquals("NW", CompassMath.getCardinalDirection(292.5f))
        assertEquals("NW", CompassMath.getCardinalDirection(315f))
        assertEquals("NW", CompassMath.getCardinalDirection(337.4f))
    }

    @Test
    fun testNorthBoundaryWrapAround() {
        assertEquals("N", CompassMath.getCardinalDirection(337.5f))
        assertEquals("N", CompassMath.getCardinalDirection(359.9f))
        assertEquals("N", CompassMath.getCardinalDirection(0f))
        assertEquals("N", CompassMath.getCardinalDirection(22.4f))
    }

    @Test
    fun testAngleWrapAroundSmoothing() {
        // Rotating from 359° to 1° should interpolate through 0°, not spin backward 358°
        val smoothed = CompassMath.smoothAngle(359f, 1f, 0.5f)
        // 359 + (2 * 0.5) = 360 = 0
        assertEquals(0f, smoothed, 0.01f)

        // Rotating from 1° to 359° should interpolate backwards through 0°
        val smoothedReverse = CompassMath.smoothAngle(1f, 359f, 0.5f)
        // 1 - (2 * 0.5) = 0
        assertEquals(0f, smoothedReverse, 0.01f)

        // Small step from 358 to 359
        val smoothedSmall = CompassMath.smoothAngle(358f, 360f, 0.5f)
        assertEquals(359f, smoothedSmall, 0.01f)
    }

    @Test
    fun testHeadingFormatting() {
        assertEquals("000°", CompassMath.formatHeadingDegrees(0f))
        assertEquals("005°", CompassMath.formatHeadingDegrees(5f))
        assertEquals("045°", CompassMath.formatHeadingDegrees(45f))
        assertEquals("090°", CompassMath.formatHeadingDegrees(90f))
        assertEquals("135°", CompassMath.formatHeadingDegrees(135f))
        assertEquals("180°", CompassMath.formatHeadingDegrees(180f))
        assertEquals("225°", CompassMath.formatHeadingDegrees(225f))
        assertEquals("270°", CompassMath.formatHeadingDegrees(270f))
        assertEquals("315°", CompassMath.formatHeadingDegrees(315f))
        assertEquals("359°", CompassMath.formatHeadingDegrees(359f))
        assertEquals("000°", CompassMath.formatHeadingDegrees(360f))
    }

    @Test
    fun testAngleDifference() {
        assertEquals(10f, CompassMath.getAngleDifference(20f, 10f), 0.001f)
        assertEquals(-10f, CompassMath.getAngleDifference(10f, 20f), 0.001f)
        // Across zero boundary
        assertEquals(2f, CompassMath.getAngleDifference(1f, 359f), 0.001f)
        assertEquals(-2f, CompassMath.getAngleDifference(359f, 1f), 0.001f)
    }

    @Test
    fun testHistorySerialization() {
        val originalList = listOf(
            com.example.a62compass.data.model.SavedHeading(
                id = 1L,
                heading = 284.5f,
                direction = "W",
                accuracy = "High",
                timestamp = 1700000000000L
            ),
            com.example.a62compass.data.model.SavedHeading(
                id = 2L,
                heading = 45.0f,
                direction = "NE",
                accuracy = "Medium",
                timestamp = 1700000010000L
            )
        )

        val json = com.example.a62compass.data.model.SavedHeading.listToJson(originalList)
        val deserializedList = com.example.a62compass.data.model.SavedHeading.listFromJson(json)

        assertEquals(2, deserializedList.size)
        assertEquals(1L, deserializedList[0].id)
        assertEquals(284.5f, deserializedList[0].heading, 0.01f)
        assertEquals("W", deserializedList[0].direction)
        assertEquals("High", deserializedList[0].accuracy)
        assertEquals(1700000000000L, deserializedList[0].timestamp)

        assertEquals(2L, deserializedList[1].id)
        assertEquals(45.0f, deserializedList[1].heading, 0.01f)
        assertEquals("NE", deserializedList[1].direction)
        assertEquals("Medium", deserializedList[1].accuracy)
        assertEquals(1700000010000L, deserializedList[1].timestamp)
    }
}
