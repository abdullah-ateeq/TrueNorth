package com.example.a62compass.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.a62compass.ui.theme.NeedleNorth
import com.example.a62compass.ui.theme.NeedlePivot
import com.example.a62compass.ui.theme.NeedleSouth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val ringAlpha = remember { Animatable(0f) }
    val ringScale = remember { Animatable(0.8f) }
    val needleAngle = remember { Animatable(-50f) }
    val ticksAlpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            ringAlpha.animateTo(1f, animationSpec = tween(600, easing = FastOutSlowInEasing))
        }
        launch {
            ringScale.animateTo(1f, animationSpec = tween(700, easing = FastOutSlowInEasing))
        }
        launch {
            delay(200)
            ticksAlpha.animateTo(1f, animationSpec = tween(600))
        }
        launch {
            delay(150)
            needleAngle.animateTo(0f, animationSpec = tween(850, easing = FastOutSlowInEasing))
        }
        launch {
            delay(400)
            textAlpha.animateTo(1f, animationSpec = tween(600))
        }

        delay(1400)
        onSplashFinished()
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val onSurface = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Canvas(modifier = Modifier.size(180.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = (size.width / 2f - 12f) * ringScale.value

                // 1. Compass ring
                drawCircle(
                    color = outlineVariant.copy(alpha = 0.5f * ringAlpha.value),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 2.5f)
                )

                // 2. Degree ticks
                if (ticksAlpha.value > 0f) {
                    for (deg in 0 until 360 step 30) {
                        val angleRad = Math.toRadians((deg - 90).toDouble())
                        val isMajor = deg % 90 == 0
                        val tickLen = if (isMajor) radius * 0.12f else radius * 0.07f

                        val outerX = center.x + radius * cos(angleRad).toFloat()
                        val outerY = center.y + radius * sin(angleRad).toFloat()
                        val innerX = center.x + (radius - tickLen) * cos(angleRad).toFloat()
                        val innerY = center.y + (radius - tickLen) * sin(angleRad).toFloat()

                        drawLine(
                            color = (if (deg == 0) NeedleNorth else onSurface).copy(
                                alpha = (if (isMajor) 0.8f else 0.4f) * ticksAlpha.value
                            ),
                            start = Offset(innerX, innerY),
                            end = Offset(outerX, outerY),
                            strokeWidth = if (isMajor) 2.5f else 1.5f,
                            cap = StrokeCap.Round
                        )
                    }
                }

                // 3. Settling needle
                rotate(degrees = needleAngle.value, pivot = center) {
                    val needleLength = radius * 0.62f
                    val needleWidth = radius * 0.12f

                    // North tip
                    val northPath = Path().apply {
                        moveTo(center.x, center.y - needleLength)
                        lineTo(center.x + needleWidth, center.y)
                        lineTo(center.x - needleWidth, center.y)
                        close()
                    }
                    drawPath(
                        path = northPath,
                        color = NeedleNorth.copy(alpha = ringAlpha.value),
                        style = Fill
                    )

                    // South tip
                    val southPath = Path().apply {
                        moveTo(center.x, center.y + needleLength)
                        lineTo(center.x + needleWidth, center.y)
                        lineTo(center.x - needleWidth, center.y)
                        close()
                    }
                    drawPath(
                        path = southPath,
                        color = NeedleSouth.copy(alpha = ringAlpha.value),
                        style = Fill
                    )

                    // Center pivot
                    drawCircle(
                        color = NeedlePivot.copy(alpha = ringAlpha.value),
                        radius = radius * 0.11f,
                        center = center,
                        style = Fill
                    )
                    drawCircle(
                        color = NeedleNorth.copy(alpha = ringAlpha.value),
                        radius = radius * 0.045f,
                        center = center,
                        style = Fill
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "TRUENORTH",
                style = MaterialTheme.typography.headlineMedium.copy(
                    letterSpacing = 4.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                ),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = textAlpha.value)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Live magnetic heading",
                style = MaterialTheme.typography.bodyMedium.copy(
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = textAlpha.value)
            )
        }
    }
}
