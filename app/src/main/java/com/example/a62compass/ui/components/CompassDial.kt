package com.example.a62compass.ui.components

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.a62compass.ui.theme.LockedMarkerColor
import com.example.a62compass.ui.theme.NeedleNorth
import com.example.a62compass.ui.theme.NeedlePivot
import com.example.a62compass.ui.theme.NeedleSouth
import com.example.a62compass.util.CompassMath
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Circular compass dial drawn on Compose Canvas.
 *
 * Features:
 * - Rotates dial opposite device heading (-heading) so geographic magnetic North aligns with real North.
 * - 360-degree ticks: Major every 30°, Minor every 5° or 10°.
 * - Cardinal directions: N, E, S, W (prominent, N accented), NE, SE, SW, NW (subtle).
 * - Two-sided needle: North accent, South muted, center pivot.
 * - Locked heading target marker when active.
 * - Top fixed reference lubber line indicating current heading.
 */
@Composable
fun CompassDial(
    heading: Float,
    isLocked: Boolean,
    lockedHeading: Float?,
    showDegreeTicks: Boolean,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val outlineVariantColor = MaterialTheme.colorScheme.outlineVariant
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant

    val cardinalPaint = remember(isDark, onSurfaceColor) {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = onSurfaceColor.toArgb()
        }
    }

    val northPaint = remember(isDark) {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = NeedleNorth.toArgb()
        }
    }

    val intercardinalPaint = remember(isDark, onSurfaceColor) {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            color = onSurfaceColor.copy(alpha = 0.65f).toArgb()
        }
    }

    val degreePaint = remember(isDark, onSurfaceColor) {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            color = onSurfaceColor.copy(alpha = 0.45f).toArgb()
        }
    }

    val textBounds = remember { Rect() }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(16.dp)
            .semantics {
                val dir = CompassMath.getCardinalDirection(heading)
                val formatted = CompassMath.formatHeadingDegrees(heading)
                contentDescription = "Compass dial showing $formatted $dir"
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = min(size.width, size.height) / 2f - 16f
            if (radius <= 0f) return@Canvas

            // 1. Draw outer decorative bezel and dial background
            drawCircle(
                color = surfaceVariantColor.copy(alpha = 0.35f),
                radius = radius,
                center = center
            )
            drawCircle(
                color = outlineVariantColor.copy(alpha = 0.6f),
                radius = radius,
                center = center,
                style = Stroke(width = 2.5f)
            )
            drawCircle(
                color = outlineVariantColor.copy(alpha = 0.3f),
                radius = radius - 30f,
                center = center,
                style = Stroke(width = 1f)
            )

            // 2. Rotate dial opposite the heading: -heading
            // So that 0° is North, and facing East (90°) places East at the top (under fixed marker)
            rotate(degrees = -heading, pivot = center) {
                // Draw 360-degree tick marks
                if (showDegreeTicks) {
                    val majorTickLen = radius * 0.08f
                    val mediumTickLen = radius * 0.05f
                    val minorTickLen = radius * 0.03f

                    for (deg in 0 until 360 step 5) {
                        val angleRad = Math.toRadians((deg - 90).toDouble())
                        val isMajor = deg % 30 == 0
                        val isMedium = deg % 10 == 0 && !isMajor
                        val tickLen = when {
                            isMajor -> majorTickLen
                            isMedium -> mediumTickLen
                            else -> minorTickLen
                        }

                        val outerX = center.x + radius * cos(angleRad).toFloat()
                        val outerY = center.y + radius * sin(angleRad).toFloat()
                        val innerX = center.x + (radius - tickLen) * cos(angleRad).toFloat()
                        val innerY = center.y + (radius - tickLen) * sin(angleRad).toFloat()

                        val strokeColor = when {
                            deg == 0 -> NeedleNorth
                            isMajor -> onSurfaceColor.copy(alpha = 0.85f)
                            isMedium -> onSurfaceColor.copy(alpha = 0.45f)
                            else -> onSurfaceColor.copy(alpha = 0.22f)
                        }
                        val strokeW = if (isMajor) 2.5f else if (isMedium) 1.5f else 1.0f

                        drawLine(
                            color = strokeColor,
                            start = Offset(innerX, innerY),
                            end = Offset(outerX, outerY),
                            strokeWidth = strokeW,
                            cap = StrokeCap.Round
                        )
                    }
                }

                // Draw Cardinal / Intercardinal Labels
                val labelRadius = radius * 0.76f
                cardinalPaint.textSize = radius * 0.12f
                northPaint.textSize = radius * 0.13f
                intercardinalPaint.textSize = radius * 0.085f
                degreePaint.textSize = radius * 0.065f

                val directions = listOf(
                    0 to ("N" to true),
                    45 to ("NE" to false),
                    90 to ("E" to true),
                    135 to ("SE" to false),
                    180 to ("S" to true),
                    225 to ("SW" to false),
                    270 to ("W" to true),
                    315 to ("NW" to false)
                )

                directions.forEach { (deg, info) ->
                    val (label, isCardinal) = info
                    val rad = Math.toRadians((deg - 90).toDouble())
                    val textX = center.x + labelRadius * cos(rad).toFloat()
                    val textY = center.y + labelRadius * sin(rad).toFloat()

                    val paint = when {
                        deg == 0 -> northPaint
                        isCardinal -> cardinalPaint
                        else -> intercardinalPaint
                    }

                    paint.getTextBounds(label, 0, label.length, textBounds)
                    val offsetY = textBounds.height() / 2f

                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        textX,
                        textY + offsetY,
                        paint
                    )
                }

                // Draw degree numbers at 30° increments (except at cardinal points)
                val degreeRadius = radius * 0.62f
                for (deg in 30 until 360 step 30) {
                    if (deg % 90 == 0) continue // Skip N, E, S, W numbers to avoid crowding
                    val rad = Math.toRadians((deg - 90).toDouble())
                    val textX = center.x + degreeRadius * cos(rad).toFloat()
                    val textY = center.y + degreeRadius * sin(rad).toFloat()
                    val text = deg.toString()
                    degreePaint.getTextBounds(text, 0, text.length, textBounds)
                    drawContext.canvas.nativeCanvas.drawText(
                        text,
                        textX,
                        textY + textBounds.height() / 2f,
                        degreePaint
                    )
                }

                // Draw Locked Heading Target Marker if locked
                if (isLocked && lockedHeading != null) {
                    val lockedRad = Math.toRadians((lockedHeading - 90).toDouble())
                    val markerOuterX = center.x + radius * cos(lockedRad).toFloat()
                    val markerOuterY = center.y + radius * sin(lockedRad).toFloat()

                    // Draw distinct amber indicator on the rim
                    drawCircle(
                        color = LockedMarkerColor,
                        radius = 7f,
                        center = Offset(markerOuterX, markerOuterY)
                    )

                    // Draw line from rim inward
                    val markerInnerX = center.x + (radius - 24f) * cos(lockedRad).toFloat()
                    val markerInnerY = center.y + (radius - 24f) * sin(lockedRad).toFloat()
                    drawLine(
                        color = LockedMarkerColor,
                        start = Offset(markerInnerX, markerInnerY),
                        end = Offset(markerOuterX, markerOuterY),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                }

                // Two-sided compass needle
                drawCompassNeedle(center, radius)
            }

            // 3. Top fixed heading index marker (Lubber line pointing down to current heading)
            drawTopIndexMarker(center, radius, primaryColor)
        }
    }
}

/**
 * Draws the elegant two-sided needle:
 * - North side: Strong accent color
 * - South side: Muted contrasting slate color
 * - Center: Circular pivot
 */
private fun DrawScope.drawCompassNeedle(center: Offset, radius: Float) {
    val needleLength = radius * 0.48f
    val needleWidth = radius * 0.075f

    // North half (points up towards 0° on the dial)
    val northPath = Path().apply {
        moveTo(center.x, center.y - needleLength)
        lineTo(center.x + needleWidth, center.y)
        lineTo(center.x - needleWidth, center.y)
        close()
    }
    drawPath(path = northPath, color = NeedleNorth, style = Fill)

    // South half (points down towards 180° on the dial)
    val southPath = Path().apply {
        moveTo(center.x, center.y + needleLength)
        lineTo(center.x + needleWidth, center.y)
        lineTo(center.x - needleWidth, center.y)
        close()
    }
    drawPath(path = southPath, color = NeedleSouth, style = Fill)

    // Needle subtle shading line down the center
    drawLine(
        color = Color.Black.copy(alpha = 0.2f),
        start = Offset(center.x, center.y - needleLength),
        end = Offset(center.x, center.y + needleLength),
        strokeWidth = 1.5f
    )

    // Center circular pivot
    val pivotRadius = radius * 0.08f
    drawCircle(
        color = NeedlePivot,
        radius = pivotRadius,
        center = center,
        style = Fill
    )
    drawCircle(
        color = Color.Black.copy(alpha = 0.4f),
        radius = pivotRadius,
        center = center,
        style = Stroke(width = 1.5f)
    )
    drawCircle(
        color = NeedleNorth,
        radius = pivotRadius * 0.35f,
        center = center,
        style = Fill
    )
}

/**
 * Top lubber line / reference marker at 12 o'clock showing the user's current facing heading.
 */
private fun DrawScope.drawTopIndexMarker(center: Offset, radius: Float, markerColor: Color) {
    val markerPath = Path().apply {
        val topY = center.y - radius - 10f
        moveTo(center.x, topY + 18f) // apex pointing down towards dial rim
        lineTo(center.x - 10f, topY)
        lineTo(center.x + 10f, topY)
        close()
    }
    drawPath(path = markerPath, color = markerColor, style = Fill)
}
