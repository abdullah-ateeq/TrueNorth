package com.example.a62compass.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import com.example.a62compass.util.CompassMath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CompassSensorData(
    val heading: Float = 0f,
    val accuracy: Int = SensorManager.SENSOR_STATUS_UNRELIABLE,
    val accuracyLabel: String = "Unknown",
    val sensorAvailable: Boolean = false,
    val sensorTypeUsed: String = "None",
    val isCalibrating: Boolean = false
)

/**
 * Dedicated manager for device orientation sensors.
 *
 * Responsibilities:
 * - Check sensor availability (Rotation Vector preferred, Accelerometer + Magnetometer fallback)
 * - Register and unregister sensor listeners safely
 * - Remap coordinate axes based on Android display rotation
 * - Smooth azimuth calculations to prevent jumpiness
 * - Report sensor accuracy and calibration state
 */
class CompassSensorManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private val rotationVectorSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometerSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magneticSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val isRotationVectorAvailable = rotationVectorSensor != null
    private val isFallbackAvailable = accelerometerSensor != null && magneticSensor != null
    val isSensorAvailable: Boolean = isRotationVectorAvailable || isFallbackAvailable

    private val _sensorData = MutableStateFlow(
        CompassSensorData(
            sensorAvailable = isSensorAvailable,
            sensorTypeUsed = when {
                isRotationVectorAvailable -> "Rotation Vector"
                isFallbackAvailable -> "Accelerometer + Magnetometer"
                else -> "None"
            }
        )
    )
    val sensorData: StateFlow<CompassSensorData> = _sensorData.asStateFlow()

    private var isListening = false
    private var currentDisplayRotation = Surface.ROTATION_0

    // Temporary calculation buffers to avoid GC allocations during high-frequency sensor callbacks
    private val rotationMatrix = FloatArray(9)
    private val remappedMatrix = FloatArray(9)
    private val inclinationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    // Fallback buffers
    private val gravityValues = FloatArray(3)
    private val geomagneticValues = FloatArray(3)
    private var hasGravity = false
    private var hasGeomagnetic = false

    private var smoothedHeading = 0f
    private var isFirstHeading = true

    /**
     * Updates current display rotation (e.g. Surface.ROTATION_0, ROTATION_90, etc.)
     * so that the coordinate axes are remapped properly.
     */
    fun updateDisplayRotation(rotation: Int) {
        currentDisplayRotation = rotation
    }

    /**
     * Registers sensor listeners if sensors are available and not already listening.
     */
    fun startListening(displayRotation: Int = Surface.ROTATION_0) {
        if (isListening || sensorManager == null || !isSensorAvailable) return
        currentDisplayRotation = displayRotation

        if (isRotationVectorAvailable && rotationVectorSensor != null) {
            val registered = sensorManager.registerListener(
                this,
                rotationVectorSensor,
                SensorManager.SENSOR_DELAY_GAME
            )
            if (registered) {
                isListening = true
                return
            }
        }

        // Fallback to accelerometer + magnetometer
        if (isFallbackAvailable && accelerometerSensor != null && magneticSensor != null) {
            val accelRegistered = sensorManager.registerListener(
                this,
                accelerometerSensor,
                SensorManager.SENSOR_DELAY_GAME
            )
            val magRegistered = sensorManager.registerListener(
                this,
                magneticSensor,
                SensorManager.SENSOR_DELAY_GAME
            )
            if (accelRegistered && magRegistered) {
                isListening = true
            } else {
                sensorManager.unregisterListener(this)
                _sensorData.value = _sensorData.value.copy(sensorAvailable = false)
            }
        }
    }

    /**
     * Unregisters sensor listeners.
     */
    fun stopListening() {
        if (!isListening || sensorManager == null) return
        sensorManager.unregisterListener(this)
        isListening = false
        hasGravity = false
        hasGeomagnetic = false
        isFirstHeading = true
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                processRotationMatrix(rotationMatrix, "Rotation Vector", event.accuracy)
            }
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, gravityValues, 0, 3)
                hasGravity = true
                if (hasGeomagnetic) {
                    processFallback(event.accuracy)
                }
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, geomagneticValues, 0, 3)
                hasGeomagnetic = true
                if (hasGravity) {
                    processFallback(event.accuracy)
                }
            }
        }
    }

    private fun processFallback(accuracy: Int) {
        val success = SensorManager.getRotationMatrix(
            rotationMatrix,
            inclinationMatrix,
            gravityValues,
            geomagneticValues
        )
        if (success) {
            processRotationMatrix(rotationMatrix, "Accelerometer + Magnetometer", accuracy)
        }
    }

    private fun processRotationMatrix(rMatrix: FloatArray, sensorType: String, accuracy: Int) {
        // Remap coordinate system based on device screen rotation
        var axisX = SensorManager.AXIS_X
        var axisY = SensorManager.AXIS_Y

        when (currentDisplayRotation) {
            Surface.ROTATION_90 -> {
                axisX = SensorManager.AXIS_Y
                axisY = SensorManager.AXIS_MINUS_X
            }
            Surface.ROTATION_180 -> {
                axisX = SensorManager.AXIS_MINUS_X
                axisY = SensorManager.AXIS_MINUS_Y
            }
            Surface.ROTATION_270 -> {
                axisX = SensorManager.AXIS_MINUS_Y
                axisY = SensorManager.AXIS_X
            }
            Surface.ROTATION_0 -> {
                axisX = SensorManager.AXIS_X
                axisY = SensorManager.AXIS_Y
            }
        }

        if (SensorManager.remapCoordinateSystem(rMatrix, axisX, axisY, remappedMatrix)) {
            SensorManager.getOrientation(remappedMatrix, orientationAngles)
            val azimuthRad = orientationAngles[0]
            val azimuthDeg = Math.toDegrees(azimuthRad.toDouble()).toFloat()
            val normalizedAzimuth = CompassMath.normalizeHeading(azimuthDeg)

            if (isFirstHeading) {
                smoothedHeading = normalizedAzimuth
                isFirstHeading = false
            } else {
                // Apply low-pass smoothing with angle wrap-around
                smoothedHeading = CompassMath.smoothAngle(smoothedHeading, normalizedAzimuth, 0.20f)
            }

            val accuracyLabel = getAccuracyLabel(accuracy)
            val isCalibrating = accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE ||
                    accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW

            _sensorData.value = CompassSensorData(
                heading = smoothedHeading,
                accuracy = accuracy,
                accuracyLabel = accuracyLabel,
                sensorAvailable = true,
                sensorTypeUsed = sensorType,
                isCalibrating = isCalibrating
            )
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        val label = getAccuracyLabel(accuracy)
        val isCalibrating = accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE ||
                accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW
        _sensorData.value = _sensorData.value.copy(
            accuracy = accuracy,
            accuracyLabel = label,
            isCalibrating = isCalibrating
        )
    }

    private fun getAccuracyLabel(accuracy: Int): String {
        return when (accuracy) {
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "High"
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "Medium"
            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> "Low"
            SensorManager.SENSOR_STATUS_UNRELIABLE -> "Calibrating"
            else -> "Ready"
        }
    }
}
