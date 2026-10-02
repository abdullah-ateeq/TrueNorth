package com.example.a62compass.ui.screens

import android.content.res.Configuration
import android.os.Build
import android.view.Surface
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.a62compass.data.model.CompassState
import com.example.a62compass.data.model.HeadingFormat
import com.example.a62compass.ui.components.CalibrationDialog
import com.example.a62compass.ui.components.CompassDial
import com.example.a62compass.ui.theme.LockedMarkerColor
import com.example.a62compass.ui.theme.NeedleNorth
import com.example.a62compass.ui.viewmodel.CompassViewModel
import com.example.a62compass.util.CompassMath
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun CompassScreen(
    viewModel: CompassViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.compassState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showCalibrationDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Determine Android display rotation for remapping sensor axes
    fun getCurrentDisplayRotation(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display?.rotation ?: Surface.ROTATION_0
        } else {
            @Suppress("DEPRECATION")
            val wm = context.getSystemService(android.content.Context.WINDOW_SERVICE) as? WindowManager
            @Suppress("DEPRECATION")
            wm?.defaultDisplay?.rotation ?: Surface.ROTATION_0
        }
    }

    // Lifecycle handling: stop sensor updates when screen/app is paused or destroyed
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    val rot = getCurrentDisplayRotation()
                    viewModel.startListening(rot)
                }
                Lifecycle.Event.ON_PAUSE -> {
                    viewModel.stopListening()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopListening()
        }
    }

    // Update display rotation when screen orientation changes
    LaunchedEffect(configuration.orientation) {
        viewModel.updateDisplayRotation(getCurrentDisplayRotation())
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!state.sensorAvailable) {
                CompassUnavailableView(
                    onRetry = {
                        viewModel.retrySensors(getCurrentDisplayRotation())
                    }
                )
            } else {
                if (isLandscape) {
                    CompassLandscapeLayout(
                        state = state,
                        headingFormat = settings.headingFormat,
                        showTicks = settings.showDegreeTicks,
                        showAccuracy = settings.showAccuracy,
                        onLockClick = { viewModel.toggleLock() },
                        onClearLock = { viewModel.clearLock() },
                        onSaveClick = {
                            viewModel.saveCurrentHeading {
                                coroutineScope.launch {
                                    val formatted = CompassMath.formatHeadingDegrees(state.heading)
                                    snackbarHostState.showSnackbar("Heading $formatted ${state.direction} saved")
                                }
                            }
                        },
                        onCalibrationClick = {
                            viewModel.onCalibrationAction()
                            showCalibrationDialog = true
                        }
                    )
                } else {
                    CompassPortraitLayout(
                        state = state,
                        headingFormat = settings.headingFormat,
                        showTicks = settings.showDegreeTicks,
                        showAccuracy = settings.showAccuracy,
                        onLockClick = { viewModel.toggleLock() },
                        onClearLock = { viewModel.clearLock() },
                        onSaveClick = {
                            viewModel.saveCurrentHeading {
                                coroutineScope.launch {
                                    val formatted = CompassMath.formatHeadingDegrees(state.heading)
                                    snackbarHostState.showSnackbar("Heading $formatted ${state.direction} saved")
                                }
                            }
                        },
                        onCalibrationClick = {
                            viewModel.onCalibrationAction()
                            showCalibrationDialog = true
                        }
                    )
                }
            }

            if (showCalibrationDialog) {
                CalibrationDialog(
                    accuracyLabel = state.accuracyLabel,
                    onDismiss = { showCalibrationDialog = false }
                )
            }
        }
    }
}

/**
 * Portrait Layout for Phone devices.
 */
@Composable
private fun CompassPortraitLayout(
    state: CompassState,
    headingFormat: HeadingFormat,
    showTicks: Boolean,
    showAccuracy: Boolean,
    onLockClick: () -> Unit,
    onClearLock: () -> Unit,
    onSaveClick: () -> Unit,
    onCalibrationClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Header
        CompassHeader()

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Large Heading and Cardinal Direction
        HeadingDisplay(
            state = state,
            format = headingFormat
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Compass Dial (Dominant element)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f),
            contentAlignment = Alignment.Center
        ) {
            CompassDial(
                heading = state.heading,
                isLocked = state.isLocked,
                lockedHeading = state.lockedHeading,
                showDegreeTicks = showTicks,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 4. Status & Calibration Bar
        StatusAndCalibrationSection(
            state = state,
            showAccuracy = showAccuracy,
            onCalibrationClick = onCalibrationClick
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 5. Main Action Buttons
        ActionButtons(
            state = state,
            onLockClick = onLockClick,
            onClearLock = onClearLock,
            onSaveClick = onSaveClick
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * Responsive Landscape/Tablet Layout.
 */
@Composable
private fun CompassLandscapeLayout(
    state: CompassState,
    headingFormat: HeadingFormat,
    showTicks: Boolean,
    showAccuracy: Boolean,
    onLockClick: () -> Unit,
    onClearLock: () -> Unit,
    onSaveClick: () -> Unit,
    onCalibrationClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Dial on left
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            CompassDial(
                heading = state.heading,
                isLocked = state.isLocked,
                lockedHeading = state.lockedHeading,
                showDegreeTicks = showTicks,
                modifier = Modifier.fillMaxHeight()
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Heading, Status, and Controls on right
        Column(
            modifier = Modifier
                .weight(1.1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CompassHeader()

            Spacer(modifier = Modifier.height(8.dp))

            HeadingDisplay(state = state, format = headingFormat)

            Spacer(modifier = Modifier.height(12.dp))

            StatusAndCalibrationSection(
                state = state,
                showAccuracy = showAccuracy,
                onCalibrationClick = onCalibrationClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            ActionButtons(
                state = state,
                onLockClick = onLockClick,
                onClearLock = onClearLock,
                onSaveClick = onSaveClick
            )
        }
    }
}

/**
 * Compact Header: "COMPASS", Subtitle: "Live magnetic heading".
 */
@Composable
private fun CompassHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "TRUENORTH",
            style = MaterialTheme.typography.titleMedium.copy(
                letterSpacing = 3.sp,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Live magnetic heading",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Prominent Heading Display with degree and cardinal directions.
 */
@Composable
private fun HeadingDisplay(
    state: CompassState,
    format: HeadingFormat
) {
    val formattedDegrees = CompassMath.formatHeadingDegrees(state.heading)
    val direction = state.direction
    val fullName = state.cardinalFullName

    val accessibilityDesc = "Current heading $formattedDegrees $fullName"

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.semantics { contentDescription = accessibilityDesc }
    ) {
        when (format) {
            HeadingFormat.DEGREES_AND_DIRECTION -> {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = formattedDegrees,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            letterSpacing = (-1).sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = direction,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                Text(
                    text = fullName,
                    style = MaterialTheme.typography.labelLarge.copy(
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HeadingFormat.DEGREES_ONLY -> {
                Text(
                    text = formattedDegrees,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            HeadingFormat.DIRECTION_ONLY -> {
                Text(
                    text = direction,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = fullName,
                    style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 2.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Locked status indicator
        if (state.isLocked && state.lockedHeading != null) {
            val lockedDegrees = CompassMath.formatHeadingDegrees(state.lockedHeading)
            val lockedDir = CompassMath.getCardinalDirection(state.lockedHeading)
            val diff = CompassMath.getAngleDifference(state.lockedHeading, state.heading).roundToInt()
            val diffStr = if (diff >= 0) "+$diff°" else "$diff°"

            Surface(
                color = LockedMarkerColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = LockedMarkerColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "LOCKED: $lockedDegrees $lockedDir ($diffStr)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = LockedMarkerColor
                    )
                }
            }
        }
    }
}

/**
 * Status, calibration banner, and accuracy chips.
 */
@Composable
private fun StatusAndCalibrationSection(
    state: CompassState,
    showAccuracy: Boolean,
    onCalibrationClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Magnetic heading label & accuracy status
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Magnetic heading",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (showAccuracy) {
                Text(
                    text = " • ",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Accuracy: ${state.accuracyLabel}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = when (state.accuracyLabel) {
                        "High" -> MaterialTheme.colorScheme.primary
                        "Medium" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        "Low", "Calibrating" -> NeedleNorth
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }

        // Calibration guidance banner if readings appear unstable or user taps
        if (state.isCalibrating) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .clickable { onCalibrationClick() }
                    .padding(horizontal = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Calibrate",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Calibration may improve accuracy. Tap for guide.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * Primary action buttons: Lock Heading, Save Heading, Calibration.
 */
@Composable
private fun ActionButtons(
    state: CompassState,
    onLockClick: () -> Unit,
    onClearLock: () -> Unit,
    onSaveClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Lock / Clear Lock Heading button
        if (state.isLocked) {
            OutlinedButton(
                onClick = onClearLock,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = LockedMarkerColor
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("CLEAR LOCK")
            }
        } else {
            OutlinedButton(
                onClick = onLockClick,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LockOpen,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("LOCK")
            }
        }

        // Save Heading Button
        Button(
            onClick = onSaveClick,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.BookmarkBorder,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("SAVE")
        }
    }
}

/**
 * Sensor Unavailable Error State.
 * Displayed when device does not have compatible rotation vector or accelerometer+magnetometer.
 */
@Composable
private fun CompassUnavailableView(
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.WarningAmber,
                contentDescription = "Sensor unavailable",
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "COMPASS UNAVAILABLE",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Your device does not provide compatible orientation sensors (Rotation Vector or Accelerometer & Magnetometer).",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("RETRY")
        }
    }
}
